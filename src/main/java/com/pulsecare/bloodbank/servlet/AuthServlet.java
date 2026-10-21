package com.pulsecare.servlet;

import com.pulsecare.util.DB;
import com.pulsecare.util.PasswordUtil;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.servlet.ServletException;
import java.io.IOException;
import java.sql.*;

@WebServlet(urlPatterns = {"/", "/login", "/register", "/logout"})
public class AuthServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/logout".equals(path)) {
            HttpSession s = req.getSession(false);
            if (s != null) s.invalidate();
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        if ("/".equals(path) && req.getSession(false) != null && req.getSession(false).getAttribute("userId") != null) {
            resp.sendRedirect(req.getContextPath() + "/dashboard"); return;
        }
        req.getRequestDispatcher("/WEB-INF/views/" + ("/register".equals(path) ? "register.jsp" : "login.jsp")).forward(req, resp);
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String path = req.getServletPath();
        if ("/register".equals(path)) register(req, resp); else login(req, resp);
    }

    private void login(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String email = value(req, "email").toLowerCase();
        String password = req.getParameter("password");
        try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement(
                "SELECT user_id,full_name,email,password_hash,role FROM users WHERE email=? AND active_status=1")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && PasswordUtil.verify(password == null ? "" : password, rs.getString("password_hash"))) {
                    HttpSession old = req.getSession(false); if (old != null) old.invalidate();
                    HttpSession s = req.getSession(true); s.setMaxInactiveInterval(30 * 60);
                    s.setAttribute("userId", rs.getLong("user_id")); s.setAttribute("fullName", rs.getString("full_name"));
                    s.setAttribute("email", rs.getString("email")); s.setAttribute("role", rs.getString("role"));
                    resp.sendRedirect(req.getContextPath() + "/dashboard"); return;
                }
            }
            req.setAttribute("error", "Invalid email or password.");
            req.getRequestDispatcher("/WEB-INF/login.jsp").forward(req, resp);
        } catch (SQLException e) { throw new ServletException("Database connection failed. Check DB_URL, DB_USER, DB_PASSWORD and schema.sql.", e); }
    }

    private void register(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String name = value(req, "fullName"), email = value(req, "email").toLowerCase(), password = req.getParameter("password");
        if (name.isBlank() || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") || password == null || password.length() < 8) {
            req.setAttribute("error", "Enter a name, valid email, and password with at least 8 characters.");
            req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp); return;
        }
        try (Connection c = DB.getConnection()) {
            c.setAutoCommit(false);
            try {
                String role;
                try (PreparedStatement count = c.prepareStatement("SELECT COUNT(*) FROM users"); ResultSet rs = count.executeQuery()) {
                    rs.next(); role = rs.getLong(1) == 0 ? "ADMIN" : "STAFF";
                }
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO users(full_name,email,password_hash,role) VALUES(?,?,?,?)")) {
                    ps.setString(1, name); ps.setString(2, email); ps.setString(3, PasswordUtil.hash(password)); ps.setString(4, role); ps.executeUpdate();
                }
                c.commit();
                req.setAttribute("success", "Account created. You can now sign in.");
                req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
            } catch (SQLException e) { c.rollback(); req.setAttribute("error", "Could not create account. The email may already be registered."); req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp); }
            finally { c.setAutoCommit(true); }
        } catch (SQLException e) { throw new ServletException("Database connection failed.", e); }
    }
    private String value(HttpServletRequest r, String k) { String v = r.getParameter(k); return v == null ? "" : v.trim(); }
}
