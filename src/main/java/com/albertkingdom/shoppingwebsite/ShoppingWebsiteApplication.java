package com.albertkingdom.shoppingwebsite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
}
