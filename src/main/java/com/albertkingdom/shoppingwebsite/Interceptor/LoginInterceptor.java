package com.albertkingdom.shoppingwebsite.Interceptor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;


public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if (request.getSession().getAttribute("user") == null && request.getMethod().equalsIgnoreCase("post")) {


            response.getWriter().write("{ \"error_description\": \"Invalid Value\"}");
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(400);
            return false;
        }
        return true;
    }
}
