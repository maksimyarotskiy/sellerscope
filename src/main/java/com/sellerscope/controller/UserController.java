package com.sellerscope.controller;

import com.sellerscope.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

//    @PostMapping("/register")
//    public ResponseEntity<User> register(@RequestBody RegisterRequest request) {
//        User user = userService.register(request);
//        return ResponseEntity.ok(user);
//    }
}