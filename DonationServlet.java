package com.pulsecare.servlet;
import com.pulsecare.util.DB;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.servlet.*;
import java.io.IOException;
import java.sql.*;
import java.util.*;

@WebServlet(urlPatterns={"/donations","/receipts"})
public class DonationServlet extends HttpServlet {
    protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        String path=req.getServletPath();
        try(Connection c=DB.getConnection()){
            if("/receipts".equals(path)){
                String id=req.getParameter("id");
                if(id!=null){try(PreparedStatement p=c.prepareStatement("SELECT r.receipt_number,r.generated_at,d.donation_id,d.donation_date,d.quantity,d.collection_centre,dr.donor_id,dr.full_name,dr.blood_group,u.full_name AS staff FROM donation_receipts r JOIN donations d ON d.donation_id=r.donation_id JOIN donors dr ON dr.donor_id=d.donor_id JOIN users u ON u.user_id=d.staff_user_id WHERE r.donation_id=?")){p.setLong(1,Long.parseLong(id));try(ResultSet r=p.executeQuery()){if(r.next()){Map<String,Object>m=new HashMap<>();m.put("receipt",r.getString(1));m.put("generated",r.getTimestamp(2));m.put("donationId",r.getLong(3));m.put("date",r.getTimestamp(4));m.put("quantity",r.getInt(5));m.put("centre",r.getString(6));m.put("donorId",r.getLong(7));m.put("name",r.getString(8));m.put("group",r.getString(9));m.put("staff",r.getString(10));req.setAttribute("receipt",m);}}}
                    if(req.getAttribute("receipt")==null){resp.sendError(404,"Receipt not found.");return;}
                    req.getRequestDispatcher("/WEB-INF/views/receipt.jsp").forward(req,resp);return;
                }
                req.setAttribute("donations",list(c));req.getRequestDispatcher("/WEB-INF/views/receipts.jsp").forward(req,resp);return;
            }
            req.setAttribute("donors",donors(c));req.setAttribute("donations",list(c));req.getRequestDispatcher("/WEB-INF/views/donations.jsp").forward(req,resp);
        }catch(SQLException e){throw new ServletException("Could not load donations.",e);}
    }
    protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        String donorId=req.getParameter("donorId"),quantity=req.getParameter("quantity"),centre=req.getParameter("centre"),remarks=req.getParameter("remarks"),token=req.getParameter("requestToken");
        try(Connection c=DB.getConnection()){
            c.setAutoCommit(false);
            try{
                long did=Long.parseLong(donorId);int qty=Integer.parseInt(quantity);
                if(qty<1||qty>5||centre==null||centre.isBlank()||token==null||token.isBlank())throw new IllegalArgumentException();
                try(PreparedStatement existing=c.prepareStatement("SELECT donation_id FROM donations WHERE request_token=?")){existing.setString(1,token);try(ResultSet r=existing.executeQuery()){if(r.next()){c.rollback();resp.sendRedirect(req.getContextPath()+"/receipts?id="+r.getLong(1));return;}}}
                String group;
                try(PreparedStatement p=c.prepareStatement("SELECT blood_group FROM donors WHERE donor_id=? AND active_status=1")){p.setLong(1,did);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException();group=r.getString(1);}}
                long donationId;
                try(PreparedStatement p=c.prepareStatement("INSERT INTO donations(donor_id,quantity,collection_centre,staff_user_id,status,remarks,request_token) VALUES(?,?,?,?,'ACCEPTED',?,?)",Statement.RETURN_GENERATED_KEYS)){
                    p.setLong(1,did);p.setInt(2,qty);p.setString(3,centre.trim());p.setLong(4,(Long)req.getSession().getAttribute("userId"));p.setString(5,remarks);p.setString(6,token);p.executeUpdate();try(ResultSet k=p.getGeneratedKeys()){k.next();donationId=k.getLong(1);}
                }
                try(PreparedStatement p=c.prepareStatement("UPDATE blood_stock SET available_units=available_units+? WHERE blood_group=?")){p.setInt(1,qty);p.setString(2,group);p.executeUpdate();}
                try(PreparedStatement p=c.prepareStatement("INSERT INTO stock_transactions(blood_group,transaction_type,quantity,reference_id,reason,staff_user_id) VALUES(?,'DONATION',?,?,?,?)")){p.setString(1,group);p.setInt(2,qty);p.setLong(3,donationId);p.setString(4,"Accepted donation #"+donationId);p.setLong(5,(Long)req.getSession().getAttribute("userId"));p.executeUpdate();}
                try(PreparedStatement p=c.prepareStatement("UPDATE donors SET last_donation_date=CURRENT_DATE WHERE donor_id=?")){p.setLong(1,did);p.executeUpdate();}
                try(PreparedStatement p=c.prepareStatement("INSERT INTO donation_receipts(receipt_number,donation_id) VALUES(?,?)")){p.setString(1,"PCR-"+System.currentTimeMillis()+"-"+donationId);p.setLong(2,donationId);p.executeUpdate();}
                c.commit();resp.sendRedirect(req.getContextPath()+"/receipts?id="+donationId);
            }catch(Exception e){c.rollback();if(e instanceof IllegalArgumentException){resp.sendError(400,"Check donor, quantity (1–5 units), centre, and form token.");}else throw new SQLException("Donation processing failed.", e);}
            finally{c.setAutoCommit(true);}
        }catch(SQLException e){throw new ServletException("Donation could not be recorded.",e);}
    }
    private List<Map<String,Object>> donors(Connection c)throws SQLException{List<Map<String,Object>>a=new ArrayList<>();try(PreparedStatement p=c.prepareStatement("SELECT donor_id,full_name,blood_group FROM donors WHERE active_status=1 ORDER BY full_name");ResultSet r=p.executeQuery()){while(r.next()){Map<String,Object>m=new HashMap<>();m.put("id",r.getLong(1));m.put("name",r.getString(2));m.put("group",r.getString(3));a.add(m);}}return a;}
    private List<Map<String,Object>> list(Connection c)throws SQLException{List<Map<String,Object>>a=new ArrayList<>();try(PreparedStatement p=c.prepareStatement("SELECT d.donation_id,d.donation_date,d.quantity,d.status,dr.donor_id,dr.full_name,dr.blood_group,d.collection_centre,r.receipt_number FROM donations d JOIN donors dr ON dr.donor_id=d.donor_id LEFT JOIN donation_receipts r ON r.donation_id=d.donation_id ORDER BY d.donation_id DESC LIMIT 200");ResultSet rs=p.executeQuery()){while(rs.next()){Map<String,Object>m=new HashMap<>();m.put("id",rs.getLong(1));m.put("date",rs.getTimestamp(2));m.put("quantity",rs.getInt(3));m.put("status",rs.getString(4));m.put("donorId",rs.getLong(5));m.put("name",rs.getString(6));m.put("group",rs.getString(7));m.put("centre",rs.getString(8));m.put("receipt",rs.getString(9));a.add(m);}}return a;}
}
