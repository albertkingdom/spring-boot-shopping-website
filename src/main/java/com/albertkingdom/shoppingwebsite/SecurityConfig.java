//package com.albertkingdom.shoppingwebsite;
//
//import org.springframework.http.HttpMethod;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
//
//
//@EnableWebSecurity
//public class SecurityConfig extends WebSecurityConfigurerAdapter {
//    @Override
//    protected void configure(HttpSecurity http) throws Exception {
//        //super.configure(http);
//        http
//                .authorizeRequests()
//                .antMatchers(HttpMethod.GET).permitAll()
//                .antMatchers(HttpMethod.POST).permitAll()
//                .anyRequest().authenticated()
//                .and()
//                .csrf().disable()
//                .formLogin(); //enable built-in login form
//    }
//}
