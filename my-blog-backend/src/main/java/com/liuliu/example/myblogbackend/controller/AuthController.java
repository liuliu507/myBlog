package com.liuliu.example.myblogbackend.controller;

import com.liuliu.example.myblogbackend.common.Result;
import com.liuliu.example.myblogbackend.dto.*;
import com.liuliu.example.myblogbackend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterRequest req){
        return Result.success(userService.register(req));
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return Result.success(userService.login(req));
    }

    @GetMapping("/me")
    public Result<LoginResponse.UserInfo> me(@RequestAttribute("userId") Long userId) {
        return Result.success(userService.getUserInfo(userId));
    }

    @PostMapping("/send-reset-code")
    public Result<Void> sendResetCode(@Valid @RequestBody SendResetCodeRequest req) {
        userService.sendResetCode(req.getEmail());
        return Result.success();
    }

    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        userService.resetPassword(req);
        return Result.success();
    }
}
