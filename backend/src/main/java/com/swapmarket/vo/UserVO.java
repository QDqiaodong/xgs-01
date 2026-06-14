package com.swapmarket.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private Double creditScore;
    private Integer reviewCount;
    private LocalDateTime createTime;
}
