package com.albertkingdom.shoppingwebsite.controller;

import com.albertkingdom.shoppingwebsite.Exception.InvalidRequestException;
import com.albertkingdom.shoppingwebsite.model.CustomResponse;
import com.albertkingdom.shoppingwebsite.model.User;
import com.albertkingdom.shoppingwebsite.repository.UserRepository;
import com.albertkingdom.shoppingwebsite.sevice.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.ArrayList;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserServiceImpl userServiceImpl;

    @RequestMapping(value = "/api/register", method = RequestMethod.POST)
    public ResponseEntity<CustomResponse> register(@Valid @RequestBody User user, BindingResult bindingResult) {
        if (bindingResult.hasErrors()){
            throw new InvalidRequestException("Invalid parameter", bindingResult);
        }

        /* create a user and add default role as "ROLE_USER"
         */
        userServiceImpl.saveUser(user);

        userServiceImpl.addRoleToUser(user.getEmail(), "ROLE_USER");

        CustomResponse resultResponse = new CustomResponse("register success", null);
        return new ResponseEntity<>(resultResponse, HttpStatus.OK);
    }

    @RequestMapping(value = "/api/login", method = RequestMethod.POST)
    public ResponseEntity<CustomResponse> login(@RequestBody User user, HttpSession session) {
        //User existingUser = userRepository.findByEmailAndPassword(user.getEmail(), user.getPassword());
        User existingUser = userServiceImpl.getUserByEmailAndPassword(user.getEmail(), user.getPassword());

//        if (existingUser != null) {
//            session.setAttribute("user", existingUser);
//            CustomResponse resultResponse = new CustomResponse("login success", existingUser.getName());
//            return new ResponseEntity<>(resultResponse, HttpStatus.OK);
//        }
//        CustomResponse resultResponse = new CustomResponse("login failed", null);
//        return new ResponseEntity<>(resultResponse, HttpStatus.BAD_REQUEST);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    // todo: if use jwt solution, no need to logout in backend
    @RequestMapping("/api/logout")
    @GetMapping
    public String logout(HttpSession session) {
        session.removeAttribute("user");
        return "you have log out";
    }

    @RequestMapping("/api/user/all")
    @GetMapping
    public ResponseEntity<?> getAllUser() {
        return new ResponseEntity<>(userServiceImpl.getAllUsers(), HttpStatus.OK);
    }
}
