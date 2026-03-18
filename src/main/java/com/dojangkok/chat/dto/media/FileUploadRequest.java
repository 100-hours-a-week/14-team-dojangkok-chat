package com.dojangkok.chat.dto.media;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class FileUploadRequest {

    @NotNull
    private String roomId;

    @Valid
    @NotEmpty(message = "업로드할 파일 정보가 필요합니다.")
    @Size(max = 10, message = "한 번에 최대 10개의 파일만 업로드할 수 있습니다.")
    private List<FileUploadItemRequest> fileItems;

    @Getter
    @NoArgsConstructor
    public static class FileUploadItemRequest {

        @NotNull(message = "파일 타입은 필수입니다.")
        private String fileType;

        @NotBlank(message = "파일명은 필수입니다.")
        private String fileName;

        @NotBlank(message = "컨텐츠 타입은 필수입니다.")
        private String contentType;

        @NotNull(message = "파일 용량은 필수입니다.")
        @Positive(message = "파일 용량은 0보다 커야 합니다.")
        private Long sizeBytes;
    }
}
