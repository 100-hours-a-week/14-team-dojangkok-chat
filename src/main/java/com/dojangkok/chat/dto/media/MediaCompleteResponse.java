package com.dojangkok.chat.dto.media;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MediaCompleteResponse {

    private final List<MediaCompleteItem> fileItems;

    @Getter
    @Builder
    public static class MediaCompleteItem {
        private final String fileKey;
        private final String downloadUrl;
        private final String contentType;
        private final long fileSize;
    }
}
