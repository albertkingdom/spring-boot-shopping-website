package com.albertkingdom.shoppingwebsite.Interceptor;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// todo: if apply jwt, no need to add interceptor
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //registry.addInterceptor(new LoginInterceptor()).addPathPatterns("/api/products"); // add interceptor to specific path
        //registry.addInterceptor(new TestInterceptor()).addPathPatterns("/api/*");

    }
}
