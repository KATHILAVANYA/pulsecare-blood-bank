package com.pulsecare.servlet;
import com.pulsecare.util.DB;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.servlet.*;
import java.io.IOException;
import java.sql.*;
import java.util.*;

@WebServlet("/donors")
public class DonorServlet extends HttpServlet {
    private static final String[] GROUPS={"A+","A-","B+","B-","AB+","AB-","O+","O-"};
    protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        String q=req.getParameter("q"), group=req.getParameter("group");
        List<Map<String,Object>> donors=new ArrayList<>();
        String sql="SELECT donor_id,full_name,email,phone,blood_group,registration_date,last_donation_date,eligibility_status FROM donors WHERE active_status=1 AND (? IS NULL OR full_name LIKE ? OR email LIKE ? OR phone LIKE ? OR CAST(donor_id AS CHAR)=?) AND (? IS NULL OR blood_group=?) ORDER BY donor_id DESC";
        try(Connection c=DB.getConnection();PreparedStatement p=c.prepareStatement(sql)){
            String query=(q==null||q.isBlank())?null:q.trim(); String g=(group==null||group.isBlank())?null:group;
            p.setString(1,query);p.setString(2,query==null?null:"%"+query+"%");p.setString(3,query==null?null:"%"+query+"%");p.setString(4,query==null?null:"%"+query+"%");p.setString(5,query);p.setString(6,g);p.setString(7,g);
            try(ResultSet r=p.executeQuery()){while(r.next()){Map<String,Object>m=new HashMap<>();m.put("id",r.getLong(1));m.put("name",r.getString(2));m.put("email",r.getString(3));m.put("phone",r.getString(4));m.put("group",r.getString(5));m.put("registered",r.getDate(6));m.put("last",r.getDate(7));m.put("eligibility",r.getString(8));donors.add(m);}}
            req.setAttribute("donors",donors);req.setAttribute("groups",GROUPS);req.setAttribute("q",q);req.setAttribute("selectedGroup",group);
            req.getRequestDispatcher("/WEB-INF/views/donors.jsp").forward(req,resp);
        }catch(SQLException e){throw new ServletException("Could not load donors.",e);}
    }
    protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        String action=req.getParameter("action");
        try(Connection c=DB.getConnection()){
            if("deactivate".equals(action)){
                try(PreparedStatement p=c.prepareStatement("UPDATE donors SET active_status=0 WHERE donor_id=?")){p.setLong(1,Long.parseLong(req.getParameter("id")));p.executeUpdate();}
            }else{
                String name=req.getParameter("fullName"),email=req.getParameter("email"),phone=req.getParameter("phone"),group=req.getParameter("bloodGroup"),dob=req.getParameter("dob"),gender=req.getParameter("gender"),address=req.getParameter("address");
                if(name==null||name.isBlank()||phone==null||phone.isBlank()||!Arrays.asList(GROUPS).contains(group)){resp.sendError(400,"Name, phone, and valid blood group are required.");return;}
                String sql="INSERT INTO donors(full_name,email,phone,date_of_birth,gender,blood_group,address) VALUES(?,?,?,?,?,?,?)";
                try(PreparedStatement p=c.prepareStatement(sql)){
                    p.setString(1,name.trim());p.setString(2,(email==null||email.isBlank())?null:email.trim());p.setString(3,phone.trim());
                    if(dob==null||dob.isBlank())p.setNull(4,Types.DATE);else p.setDate(4,Date.valueOf(dob));
                    p.setString(5,gender);p.setString(6,group);p.setString(7,address);p.executeUpdate();
                }
            }
            resp.sendRedirect(req.getContextPath()+"/donors");
        }catch(IllegalArgumentException e){resp.sendError(400,"Invalid input.");}
        catch(SQLException e){if("23000".equals(e.getSQLState())){resp.sendError(400,"Duplicate donor email or invalid donor data.");}else throw new ServletException("Could not save donor.",e);}
    }
}
