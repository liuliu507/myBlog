package com.liuliu.example.myblogbackend.dto;

import lombok.Data;

@Data
public class UserVO {
    private Long id;
    private String email;
    private String nickname;
    private String avatar;
}
