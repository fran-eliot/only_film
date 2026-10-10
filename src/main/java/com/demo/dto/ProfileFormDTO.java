package com.demo.dto;

import com.demo.model.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileFormDTO {
    private String username;
    private String email;
    private String password;
    private String imageUrl;

    public static ProfileFormDTO from(User user) {
        ProfileFormDTO dto = new ProfileFormDTO();
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setImageUrl(user.getImageUrl());
        return dto;
    }

    public User toUser() {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        return user;
    }
}
