package com.albertkingdom.shoppingwebsite.controller;

import com.albertkingdom.shoppingwebsite.model.CustomResponse;
import com.albertkingdom.shoppingwebsite.model.User;
import com.albertkingdom.shoppingwebsite.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @RequestMapping(value = "/api/register", method = RequestMethod.POST)
    public ResponseEntity<CustomResponse> register(@RequestBody User user) {
        userRepository.save(user);
        CustomResponse resultResponse = new CustomResponse("register success", null);
        return new ResponseEntity<>(resultResponse, HttpStatus.OK);
    }

    @RequestMapping(value = "/api/login", method = RequestMethod.POST)
    public ResponseEntity<CustomResponse> login(@RequestBody User user, HttpSession session) {
        User existingUser = userRepository.findByEmailAndPassword(user.getEmail(), user.getPassword());
        if (existingUser != null) {
            session.setAttribute("user", existingUser);
            CustomResponse resultResponse = new CustomResponse("login success", existingUser.getName());
            return new ResponseEntity<>(resultResponse, HttpStatus.OK);
        }
        CustomResponse resultResponse = new CustomResponse("login failed", null);
        return new ResponseEntity<>(resultResponse, HttpStatus.BAD_REQUEST);
    }

    @RequestMapping("/api/logout")
    @GetMapping
    public String logout(HttpSession session) {
        session.removeAttribute("user");
        return "you have log out";
    }


}
