package com.dojangkok.chat.dto.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CachedUserProfile {

    private String userId;
    private String nickname;
    private String profileImageUrl;
}
