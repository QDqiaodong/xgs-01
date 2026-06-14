package com.swapmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.swapmarket.dto.UserRegisterDTO;
import com.swapmarket.entity.User;
import com.swapmarket.mapper.UserMapper;
import com.swapmarket.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserVO register(UserRegisterDTO dto) {
        User existingUser = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (existingUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());

        userMapper.insert(user);

        return convertToVO(user);
    }

    public Map<String, Object> login(String username, String password) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));

        Map<String, Object> result = new HashMap<>();
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            user.setPassword(null);
            result.put("user", user);
            result.put("token", "token-" + user.getId() + "-" + System.currentTimeMillis());
        }
        return result;
    }

    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setCreditScore(user.getCreditScore());
        vo.setReviewCount(user.getReviewCount());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
