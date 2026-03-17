package com.dojangkok.chat.dto.media;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MediaUploadResponse {

    private final List<MediaUploadItem> fileItems;

    @Getter
    @Builder
    public static class MediaUploadItem {
        private final String fileAssetId;
        private final String fileKey;
        private final String uploadUrl;
    }
}
