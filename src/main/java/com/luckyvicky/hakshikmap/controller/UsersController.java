package com.luckyvicky.hakshikmap.controller;

import com.luckyvicky.hakshikmap.dto.UserRes;
import com.luckyvicky.hakshikmap.dto.UserVoteCountRes;
import com.luckyvicky.hakshikmap.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor // 생성자 자동으로 만들어 줌
@CrossOrigin(origins = {"http://localhost:3000",
                         "http://172.20.87.64:3000"})
@RequestMapping("/api/user")
public class UsersController {
    private final UserService userService;

    @PostMapping
    public UserRes create() {
        return userService.create();
    }

    @GetMapping("/vote")
    public UserVoteCountRes getUserVoteCount(
            @RequestHeader("user-token") String token
    ) {
        return userService.getUserVoteCount(token);
    }
}