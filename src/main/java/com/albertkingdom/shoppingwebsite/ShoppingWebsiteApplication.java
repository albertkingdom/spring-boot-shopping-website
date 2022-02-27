package com.albertkingdom.shoppingwebsite;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class ShoppingWebsiteApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShoppingWebsiteApplication.class, args);
	}
	@Value("${cloudinary.cloud_name}")
	private String cloudName;
	@Value("${cloudinary.api_key}")
	private String cloudApiKey;
	@Value("${cloudinary.api_secret}")
	private String cloudApiSecret;

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

	@Bean
	public Cloudinary cloudinaryConfig(){
		Cloudinary cloudinary = null;

		Map<String, String> config = new HashMap<>();
		config.put("cloud_name", cloudName);
		config.put("api_key", cloudApiKey);
		config.put("api_secret", cloudApiSecret);

		cloudinary = new Cloudinary(config);
		return cloudinary;



	}
}
