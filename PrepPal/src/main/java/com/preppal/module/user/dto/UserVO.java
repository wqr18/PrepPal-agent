package com.preppal.module.user.dto;

import com.preppal.module.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {

    private Long id;
    private String nickname;
    private String avatarUrl;

    public static UserVO from(User user) {
        return new UserVO(user.getId(), user.getNickname(), user.getAvatarUrl());
    }
}
