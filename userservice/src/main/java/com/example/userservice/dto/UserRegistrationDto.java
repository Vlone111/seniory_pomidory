package com.example.userservice.dto;

import lombok.Data;

@Data
public class UserRegistrationDto {
    private String email;
    private String firstname;
    private String lastname;
    private String password;
    private String role; // "user" или "owner" - роль, которую хочет получить пользователь при регистрации
}
