package com.dojangkok.chat.controller;

import com.dojangkok.chat.auth.CurrentMemberId;
import com.dojangkok.chat.common.dto.DataResponseDto;
import com.dojangkok.chat.common.enums.Code;
import com.dojangkok.chat.dto.media.FileUploadCompleteRequest;
import com.dojangkok.chat.dto.media.FileUploadCompleteResponse;
import com.dojangkok.chat.dto.media.FileUploadRequest;
import com.dojangkok.chat.dto.media.FileUploadResponse;
import com.dojangkok.chat.service.FileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat/v3/direct-chat/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/presigned-urls")
    public DataResponseDto<FileUploadResponse> generatePresignedUrls(
            @CurrentMemberId String userId,
            @RequestBody @Valid FileUploadRequest request) {

        FileUploadResponse response = fileService.generatePresignedUrls(userId, request);
        return new DataResponseDto<>(Code.SUCCESS, "Presigned URL 발급에 성공하였습니다.", response);
    }

    @PostMapping("/complete")
    public DataResponseDto<FileUploadCompleteResponse> completeFileUpload(
            @RequestBody FileUploadCompleteRequest request) {

        FileUploadCompleteResponse response = fileService.completeFileUpload(request);
        return new DataResponseDto<>(Code.SUCCESS, "파일 업로드 완료 처리에 성공하였습니다.", response);
    }
}
