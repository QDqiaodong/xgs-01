package com.swapmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.swapmarket.entity.User;
import com.swapmarket.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;

    public Map<String, Object> login(String username, String password) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getPassword, password));
        
        Map<String, Object> result = new HashMap<>();
        if (user != null) {
            user.setPassword(null);
            result.put("user", user);
            result.put("token", "token-" + user.getId() + "-" + System.currentTimeMillis());
        }
        return result;
    }

    public User getById(Long id) {
        return userMapper.selectById(id);
    }
}
