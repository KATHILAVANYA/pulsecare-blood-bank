package com.pulsecare.servlet;
import com.pulsecare.util.DB;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.servlet.*;
import java.io.IOException;
import java.sql.*;
import java.util.*;

@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try (Connection c = DB.getConnection()) {
            req.setAttribute("donorCount", scalar(c, "SELECT COUNT(*) FROM donors WHERE active_status=1"));
            req.setAttribute("totalUnits", scalar(c, "SELECT COALESCE(SUM(available_units),0) FROM blood_stock"));
            req.setAttribute("groupsInStock", scalar(c, "SELECT COUNT(*) FROM blood_stock WHERE available_units>0"));
            req.setAttribute("monthDonations", scalar(c, "SELECT COUNT(*) FROM donations WHERE status='ACCEPTED' AND donation_date >= DATE_FORMAT(CURRENT_DATE,'%Y-%m-01')"));
            List<Map<String,Object>> stock = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT blood_group,available_units,minimum_threshold FROM blood_stock ORDER BY FIELD(blood_group,'A+','A-','B+','B-','AB+','AB-','O+','O-')"); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) { Map<String,Object> m = new HashMap<>(); m.put("group",rs.getString(1)); m.put("units",rs.getInt(2)); m.put("threshold",rs.getInt(3)); stock.add(m); }
            }
            req.setAttribute("stock", stock);
            try (PreparedStatement ps = c.prepareStatement("SELECT blood_group,available_units,minimum_threshold FROM blood_stock WHERE available_units < minimum_threshold ORDER BY available_units"); ResultSet rs = ps.executeQuery()) {
                List<Map<String,Object>> low = new ArrayList<>();
                while(rs.next()){Map<String,Object> m=new HashMap<>();m.put("group",rs.getString(1));m.put("units",rs.getInt(2));m.put("threshold",rs.getInt(3));low.add(m);}
                req.setAttribute("lowStock",low);
            }
            try (PreparedStatement ps = c.prepareStatement("SELECT d.donation_id,d.donation_date,d.quantity,d.status,dr.full_name,dr.blood_group FROM donations d JOIN donors dr ON dr.donor_id=d.donor_id ORDER BY d.donation_id DESC LIMIT 8"); ResultSet rs = ps.executeQuery()) {
                List<Map<String,Object>> recent = new ArrayList<>();
                while(rs.next()){Map<String,Object> m=new HashMap<>();m.put("id",rs.getLong(1));m.put("date",rs.getTimestamp(2));m.put("quantity",rs.getInt(3));m.put("status",rs.getString(4));m.put("name",rs.getString(5));m.put("group",rs.getString(6));recent.add(m);}
                req.setAttribute("recent",recent);
            }
            req.getRequestDispatcher("/WEB-INF/views/dashboard.jsp").forward(req,resp);
        } catch(SQLException e){throw new ServletException("Could not load dashboard data.",e);}
    }
    private int scalar(Connection c,String sql)throws SQLException{try(PreparedStatement p=c.prepareStatement(sql);ResultSet r=p.executeQuery()){r.next();return r.getInt(1);}}
}
