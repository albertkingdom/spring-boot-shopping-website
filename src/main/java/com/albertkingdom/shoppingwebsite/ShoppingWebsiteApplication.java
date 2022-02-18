package com.albertkingdom.shoppingwebsite;

import com.albertkingdom.shoppingwebsite.model.User;
import com.albertkingdom.shoppingwebsite.sevice.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;

@SpringBootApplication
public class ShoppingWebsiteApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShoppingWebsiteApplication.class, args);
	}
//	@Bean
//	public WebMvcConfigurer corsConfigurer() {
//		return new WebMvcConfigurer() {
//			@Override
//			public void addCorsMappings(CorsRegistry registry) {
//				registry.addMapping("/api/products").allowedOrigins("http://localhost:8080");
//			}
//		};
//	}
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
//	@Bean
//	CommandLineRunner run(UserService userService) {
//		return args -> {
//
//			//userService.saveUser(new User(null,"test6@gmail.com", "test666", "test6", new ArrayList<>()));
//
//			//userService.addRoleToUser("test6@gmail.com", "ROLE_USER");
//
//		};
//	}
}
