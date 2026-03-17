package com.dojangkok.chat.dto.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CachedPropertyInfo {

    private String propertyId;
    private String title;
    private String imageUrl;
    private Long priceMain;
    private Integer priceMonthly;
    private String rentType;
    private String dealStatus;
}
