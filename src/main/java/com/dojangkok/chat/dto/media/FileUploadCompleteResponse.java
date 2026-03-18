package com.dojangkok.chat.dto.media;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class FileUploadCompleteResponse {

    private final List<FileUploadCompleteItem> fileItems;

    @Getter
    @Builder
    public static class FileUploadCompleteItem {
        private final String fileAssetId;
        private final String fileKey;
        private final String fileType;
        private final String status;
        private final String presignedUrl;
    }
}
