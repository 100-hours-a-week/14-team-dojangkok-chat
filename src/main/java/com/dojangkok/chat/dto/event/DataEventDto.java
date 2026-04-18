package com.dojangkok.chat.dto.event;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor
public class DataEventDto {

    private String type;
    private Instant eventTimestamp;

    // USER_UPDATED, USER_DELETED
    private String userId;
    private String nickname;
    private String profileImageUrl;

    // PROPERTY_UPDATED, PROPERTY_DELETED
    private String propertyId;
    private String title;
    private String imageUrl;
    private String dealStatus;
}
