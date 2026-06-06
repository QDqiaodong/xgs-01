package com.swapmarket.controller;

import com.swapmarket.common.Result;
import com.swapmarket.entity.User;
import com.swapmarket.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        Map<String, Object> result = userService.login(request.getUsername(), request.getPassword());
        if (result.isEmpty()) {
            return Result.error("用户名或密码错误");
        }
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }
}
