package com.pulsecare.servlet;
import com.pulsecare.util.DB;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.servlet.*;
import java.io.IOException;
import java.sql.*;
import java.util.*;

@WebServlet(urlPatterns={"/stock","/stock-adjust","/search-blood","/alerts","/reports"})
public class StockServlet extends HttpServlet {
    protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        String path=req.getServletPath();
        try(Connection c=DB.getConnection()){
            if("/search-blood".equals(path)){req.getRequestDispatcher("/WEB-INF/views/search-blood.jsp").forward(req,resp);return;}
            if("/reports".equals(path)){req.setAttribute("transactions",transactions(c));req.getRequestDispatcher("/WEB-INF/views/reports.jsp").forward(req,resp);return;}
            List<Map<String,Object>> rows=stock(c);req.setAttribute("stock",rows);
            if("/alerts".equals(path)){List<Map<String,Object>> low=new ArrayList<>();for(Map<String,Object>m:rows)if((int)m.get("units")<(int)m.get("threshold"))low.add(m);req.setAttribute("lowStock",low);req.getRequestDispatcher("/WEB-INF/views/alerts.jsp").forward(req,resp);}
            else req.getRequestDispatcher("/WEB-INF/views/stock.jsp").forward(req,resp);
        }catch(SQLException e){throw new ServletException("Could not load stock.",e);}
    }
    protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        String group=req.getParameter("bloodGroup"),reason=req.getParameter("reason"),kind=req.getParameter("kind");
        try(Connection c=DB.getConnection()){
            c.setAutoCommit(false);
            try{
                int qty=Integer.parseInt(req.getParameter("quantity"));int threshold=Integer.parseInt(req.getParameter("threshold")==null?"0":req.getParameter("threshold"));
                if(qty<0||threshold<0||!Arrays.asList("A+","A-","B+","B-","AB+","AB-","O+","O-").contains(group))throw new IllegalArgumentException();
                try(PreparedStatement lock=c.prepareStatement("SELECT available_units FROM blood_stock WHERE blood_group=? FOR UPDATE")){lock.setString(1,group);try(ResultSet r=lock.executeQuery()){if(!r.next())throw new SQLException("Missing stock row");int current=r.getInt(1);int next=current;
                    if("threshold".equals(kind)){try(PreparedStatement p=c.prepareStatement("UPDATE blood_stock SET minimum_threshold=? WHERE blood_group=?")){p.setInt(1,threshold);p.setString(2,group);p.executeUpdate();}}
                    else {String direction=req.getParameter("direction");next="remove".equals(direction)?current-qty:current+qty;if(next<0)throw new IllegalArgumentException();
                        try(PreparedStatement p=c.prepareStatement("UPDATE blood_stock SET available_units=? WHERE blood_group=?")){p.setInt(1,next);p.setString(2,group);p.executeUpdate();}
                        try(PreparedStatement p=c.prepareStatement("INSERT INTO stock_transactions(blood_group,transaction_type,quantity,reason,staff_user_id) VALUES(?,?,?,?,?)")){p.setString(1,group);p.setString(2,"ADJUSTMENT");p.setInt(3,"remove".equals(direction)?-qty:qty);p.setString(4,reason==null||reason.isBlank()?"Manual adjustment":reason);p.setLong(5,(Long)req.getSession().getAttribute("userId"));p.executeUpdate();}
                    }
                }}
                c.commit();resp.sendRedirect(req.getContextPath()+("/stock-adjust".equals(req.getServletPath())?"/stock":req.getServletPath()));
            }catch(Exception e){c.rollback();if(e instanceof IllegalArgumentException){resp.sendError(400,"Invalid quantity, threshold, or stock would become negative.");}else throw new SQLException("Stock update failed.", e);}
            finally{c.setAutoCommit(true);}
        }catch(SQLException e){throw new ServletException("Could not update stock.",e);}
    }
    private List<Map<String,Object>> stock(Connection c)throws SQLException{
        List<Map<String,Object>> a=new ArrayList<>();try(PreparedStatement p=c.prepareStatement("SELECT blood_group,available_units,minimum_threshold,updated_at FROM blood_stock ORDER BY FIELD(blood_group,'A+','A-','B+','B-','AB+','AB-','O+','O-')");ResultSet r=p.executeQuery()){while(r.next()){Map<String,Object>m=new HashMap<>();m.put("group",r.getString(1));m.put("units",r.getInt(2));m.put("threshold",r.getInt(3));m.put("updated",r.getTimestamp(4));a.add(m);}}return a;
    }
    private List<Map<String,Object>> transactions(Connection c)throws SQLException{
        List<Map<String,Object>> a=new ArrayList<>();try(PreparedStatement p=c.prepareStatement("SELECT transaction_id,blood_group,transaction_type,quantity,reason,transaction_date FROM stock_transactions ORDER BY transaction_id DESC LIMIT 100");ResultSet r=p.executeQuery()){while(r.next()){Map<String,Object>m=new HashMap<>();m.put("id",r.getLong(1));m.put("group",r.getString(2));m.put("type",r.getString(3));m.put("qty",r.getInt(4));m.put("reason",r.getString(5));m.put("date",r.getTimestamp(6));a.add(m);}}return a;
    }
}
