package com.pulsecare.filter;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.*;
import java.io.IOException;

@WebFilter(urlPatterns = {"/dashboard", "/donors", "/stock", "/search-blood", "/donations", "/receipts", "/alerts", "/reports", "/stock-adjust", "/blood-availability"})
public class AuthFilter implements Filter {
    @Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            res.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        res.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        chain.doFilter(request, response);
    }
}
