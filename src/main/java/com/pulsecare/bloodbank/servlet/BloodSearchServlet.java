package com.pulsecare.servlet;
import com.pulsecare.util.DB;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.servlet.*;
import java.io.IOException;
import java.sql.*;

@WebServlet("/blood-availability")
public class BloodSearchServlet extends HttpServlet {
    protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        String group=req.getParameter("bloodGroup");int qty;
        try{qty=Integer.parseInt(req.getParameter("quantity"));if(qty<1||qty>100)throw new NumberFormatException();}
        catch(NumberFormatException e){req.setAttribute("error","Enter a requested quantity between 1 and 100.");req.getRequestDispatcher("/WEB-INF/views/search-blood.jsp").forward(req,resp);return;}
        try(Connection c=DB.getConnection();PreparedStatement p=c.prepareStatement("SELECT available_units,minimum_threshold,updated_at FROM blood_stock WHERE blood_group=?")){
            p.setString(1,group);try(ResultSet r=p.executeQuery()){if(!r.next()){req.setAttribute("error","Select a valid blood group.");}else{req.setAttribute("group",group);req.setAttribute("units",r.getInt(1));req.setAttribute("threshold",r.getInt(2));req.setAttribute("updated",r.getTimestamp(3));req.setAttribute("requested",qty);req.setAttribute("available",r.getInt(1)>=qty);}}
            req.getRequestDispatcher("/WEB-INF/views/search-blood.jsp").forward(req,resp);
        }catch(SQLException e){throw new ServletException("Blood search failed.",e);}
    }
}
