package com.albertkingdom.shoppingwebsite;


import com.albertkingdom.shoppingwebsite.filter.CustomAuthorizationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Autowired
    private final UserDetailsService userDetailsService;
    @Autowired
    private CustomAuthorizationFilter customAuthorizationFilter;

    public SecurityConfig(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    private static final String[] AUTH_WHITELIST = {
            // -- Swagger UI v2
            "/v2/api-docs",
            "/swagger-resources",
            "/swagger-resources/**",
            "/configuration/ui",
            "/configuration/security",
            "/swagger-ui.html",
            "/webjars/**",
            // -- Swagger UI v3 (OpenAPI)
            "/v3/api-docs/**",
            "/swagger-ui/**",
            // other public endpoints of your API may be appended to this array
            "/api/login",
            "/api/register",
            "/api/refreshToken",
            "/api/order/pay/confirm"
    };
    @Override
    protected void configure(HttpSecurity http) throws Exception {

        //CustomAuthenticationFilter customAuthenticationFilter = new CustomAuthenticationFilter(authenticationManagerBean());
        //customAuthenticationFilter.setFilterProcessesUrl("/api/login");
        http.csrf().disable()
                .cors() //allow cors option preflight from browser
                .and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        http.authorizeRequests()
                .antMatchers(AUTH_WHITELIST).permitAll()
                .antMatchers(HttpMethod.GET,"/api/products/**").permitAll()
                .antMatchers(HttpMethod.POST,"/api/products/**").hasAnyAuthority("ROLE_ADMIN")
                .antMatchers(HttpMethod.DELETE,"/api/products/**").hasAnyAuthority("ROLE_ADMIN")
                .antMatchers(HttpMethod.PUT,"/api/products/**").hasAnyAuthority("ROLE_ADMIN")
                .antMatchers(HttpMethod.POST,"/api/order/**").hasAnyAuthority("ROLE_USER") // create order
                .antMatchers("/api/order/**").hasAnyAuthority("ROLE_ADMIN")  //allow user logged in and with role as "role_admin"
                .anyRequest().authenticated(); // deny all access without authenticated


        //http.addFilter(customAuthenticationFilter);
        http.addFilterBefore(customAuthorizationFilter, UsernamePasswordAuthenticationFilter.class);
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        // 1. set password encode method
        // 2. set spring security how to get login user information
        auth.userDetailsService(userDetailsService).passwordEncoder(new BCryptPasswordEncoder());
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
}
