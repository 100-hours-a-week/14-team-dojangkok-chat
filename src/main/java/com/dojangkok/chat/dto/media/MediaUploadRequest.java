package com.dojangkok.chat.dto.media;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class MediaUploadRequest {

    @NotNull
    private String roomId;

    @NotEmpty
    @Size(max = 10, message = "한 번에 최대 10개의 파일만 업로드할 수 있습니다.")
    private List<MediaUploadItemRequest> fileItems;

    @Getter
    @NoArgsConstructor
    public static class MediaUploadItemRequest {
        private String fileType;
        private String fileName;
        private String contentType;
        private long sizeBytes;
    }
}
