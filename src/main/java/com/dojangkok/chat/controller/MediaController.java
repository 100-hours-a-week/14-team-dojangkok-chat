package com.dojangkok.chat.controller;

import com.dojangkok.chat.auth.CurrentMemberId;
import com.dojangkok.chat.common.dto.DataResponseDto;
import com.dojangkok.chat.common.enums.Code;
import com.dojangkok.chat.dto.media.MediaCompleteRequest;
import com.dojangkok.chat.dto.media.MediaUploadRequest;
import com.dojangkok.chat.dto.media.MediaCompleteResponse;
import com.dojangkok.chat.dto.media.MediaUploadResponse;
import com.dojangkok.chat.service.MediaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @PostMapping("/presigned-urls")
    public DataResponseDto<MediaUploadResponse> generatePresignedUrls(
            @CurrentMemberId String userId,
            @RequestBody @Valid MediaUploadRequest request) {

        MediaUploadResponse response = mediaService.generatePresignedUrls(userId, request);
        return new DataResponseDto<>(Code.SUCCESS, "Presigned URL 발급에 성공하였습니다.", response);
    }

    @PostMapping("/complete")
    public DataResponseDto<MediaCompleteResponse> completeFileUpload(
            @RequestBody MediaCompleteRequest request) {

        MediaCompleteResponse response = mediaService.completeFileUpload(request);
        return new DataResponseDto<>(Code.SUCCESS, "파일 업로드 완료 처리에 성공하였습니다.", response);
    }
}
