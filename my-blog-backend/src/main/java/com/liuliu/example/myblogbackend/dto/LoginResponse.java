package com.liuliu.example.myblogbackend.dto;

import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private UserInfo user;

    @Data
    public static class UserInfo {
        private Long id;
        private String email;
        private String nickname;
        private String avatar;
    }
}
