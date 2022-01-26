package com.albertkingdom.shoppingwebsite.Interceptor;

import com.albertkingdom.shoppingwebsite.model.User;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TestInterceptor implements HandlerInterceptor {
    /*
    if request have cookie in header, we can use it to identify user
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        User existingUser = (User) request.getSession().getAttribute("user"); // find the corresponding user object
        System.out.println("TestInterceptor "+existingUser);

        if (existingUser != null) {
            System.out.println("TestInterceptor "+existingUser + ", email:" + existingUser.getEmail());

        }
        return true;
    }
}
