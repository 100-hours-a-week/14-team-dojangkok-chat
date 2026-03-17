package com.dojangkok.chat.service;

import com.dojangkok.chat.domain.FileAsset;
import com.dojangkok.chat.dto.media.MediaCompleteRequest;
import com.dojangkok.chat.dto.media.MediaUploadRequest;
import com.dojangkok.chat.dto.media.MediaCompleteResponse;
import com.dojangkok.chat.dto.media.MediaUploadResponse;
import com.dojangkok.chat.repository.FileAssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaService {

    private static final long MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024;   // 10MB
    private static final long MAX_VIDEO_SIZE_BYTES = 100 * 1024 * 1024;  // 100MB

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );
    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of(
            "video/mp4", "video/quicktime", "video/webm"
    );

    private final S3Service s3Service;
    private final FileAssetRepository fileAssetRepository;

    public MediaUploadResponse generatePresignedUrls(String userId, MediaUploadRequest request) {
        // 전체 검증
        for (MediaUploadRequest.MediaUploadItemRequest item : request.getFileItems()) {
            String contentType = item.getContentType().toLowerCase();

            if (!isAllowedContentType(contentType)) {
                throw new IllegalArgumentException(
                        "허용되지 않는 파일 형식입니다: " + item.getFileName() + " (" + item.getContentType() + ")");
            }

            long maxSize = getMaxSizeForContentType(contentType);
            if (item.getSizeBytes() > maxSize) {
                throw new IllegalArgumentException(
                        "파일 용량이 초과되었습니다: " + item.getFileName() + " (" + item.getSizeBytes() + " bytes)");
            }
        }

        // 검증 통과 후 일괄 발급
        List<MediaUploadResponse.MediaUploadItem> items = new ArrayList<>();

        for (MediaUploadRequest.MediaUploadItemRequest item : request.getFileItems()) {
            String extension = extractExtension(item.getFileName());
            String mediaPrefix = getMediaPrefix(item.getContentType().toLowerCase());
            String fileKey = mediaPrefix + UUID.randomUUID() + extension;

            String uploadUrl = s3Service.generatePresignedUploadUrl(fileKey, item.getContentType());

            // FileAsset UPLOADING 저장
            FileAsset fileAsset = fileAssetRepository.save(FileAsset.builder()
                    .fileKey(fileKey)
                    .roomId(request.getRoomId())
                    .uploaderId(userId)
                    .contentType(item.getContentType())
                    .originalFilename(item.getFileName())
                    .build());

            items.add(MediaUploadResponse.MediaUploadItem.builder()
                    .fileAssetId(fileAsset.getId())
                    .fileKey(fileKey)
                    .uploadUrl(uploadUrl)
                    .build());
        }

        log.info("Presigned URL 발급 완료: {}개, roomId={}", items.size(), request.getRoomId());
        return MediaUploadResponse.builder().fileItems(items).build();
    }

    public MediaCompleteResponse completeFileUpload(MediaCompleteRequest request) {
        List<String> fileAssetIds = request.getFileAssetIds();

        // FileAsset ID로 조회
        List<FileAsset> fileAssets = fileAssetRepository.findAllById(fileAssetIds);
        Map<String, FileAsset> fileAssetMap = fileAssets.stream()
                .collect(Collectors.toMap(FileAsset::getId, Function.identity()));

        // 존재 여부 검증
        for (String fileAssetId : fileAssetIds) {
            if (!fileAssetMap.containsKey(fileAssetId)) {
                throw new IllegalArgumentException("존재하지 않는 파일입니다: fileAssetId=" + fileAssetId);
            }
        }

        // Virtual Thread 기반 S3 HEAD 병렬 처리
        List<FileAsset> assetsNeedingHead = fileAssetIds.stream()
                .map(fileAssetMap::get)
                .filter(fa -> fa.getStatus() != FileAsset.FileAssetStatus.COMPLETED)
                .toList();

        Map<String, Optional<HeadObjectResponse>> headResponses;
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Map<String, Future<Optional<HeadObjectResponse>>> futures = assetsNeedingHead.stream()
                    .collect(Collectors.toMap(
                            FileAsset::getId,
                            fa -> executor.submit(() -> s3Service.getObjectMetadata(fa.getFileKey()))
                    ));

            headResponses = futures.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> {
                                try {
                                    return entry.getValue().get();
                                } catch (Exception e) {
                                    log.error("S3 HEAD 요청 실패: fileAssetId={}", entry.getKey(), e);
                                    return Optional.empty();
                                }
                            }
                    ));
        }

        // 검증 수행
        List<MediaCompleteResponse.MediaCompleteItem> results = new ArrayList<>();

        for (String fileAssetId : fileAssetIds) {
            FileAsset fileAsset = fileAssetMap.get(fileAssetId);

            // 이미 완료된 경우
            if (fileAsset.getStatus() == FileAsset.FileAssetStatus.COMPLETED) {
                String downloadUrl = s3Service.generatePresignedDownloadUrl(fileAsset.getFileKey());
                results.add(MediaCompleteResponse.MediaCompleteItem.builder()
                        .fileKey(fileAsset.getFileKey())
                        .downloadUrl(downloadUrl)
                        .contentType(fileAsset.getContentType())
                        .fileSize(fileAsset.getFileSize())
                        .build());
                continue;
            }

            Optional<HeadObjectResponse> headResponse = headResponses.get(fileAssetId);

            // S3에 파일이 없는 경우
            if (headResponse == null || headResponse.isEmpty()) {
                rollbackAll(fileAssets);
                throw new IllegalArgumentException("S3에 파일이 업로드되지 않았습니다: fileAssetId=" + fileAssetId);
            }

            HeadObjectResponse head = headResponse.get();
            long actualSize = head.contentLength();
            String actualContentType = head.contentType();

            // 용량 정책 검증
            long maxSize = getMaxSizeForContentType(actualContentType.toLowerCase());
            if (actualSize > maxSize) {
                rollbackAll(fileAssets);
                throw new IllegalArgumentException("파일 용량이 초과되었습니다: fileAssetId=" + fileAssetId + ", size=" + actualSize);
            }

            // Content-Type 위변조 검증
            if (!fileAsset.getContentType().equalsIgnoreCase(actualContentType)) {
                rollbackAll(fileAssets);
                throw new IllegalArgumentException("파일 형식이 일치하지 않습니다: fileAssetId=" + fileAssetId
                        + ", declared=" + fileAsset.getContentType() + ", actual=" + actualContentType);
            }

            // 검증 통과 — COMPLETED 처리 + Presigned Download URL 발급
            fileAsset.markCompleted(actualSize);
            String downloadUrl = s3Service.generatePresignedDownloadUrl(fileAsset.getFileKey());

            results.add(MediaCompleteResponse.MediaCompleteItem.builder()
                    .fileKey(fileAsset.getFileKey())
                    .downloadUrl(downloadUrl)
                    .contentType(fileAsset.getContentType())
                    .fileSize(actualSize)
                    .build());
        }

        // 일괄 저장
        fileAssetRepository.saveAll(fileAssets);
        log.info("파일 업로드 완료 검증 성공: {}개", results.size());

        return MediaCompleteResponse.builder().fileItems(results).build();
    }

    private void rollbackAll(List<FileAsset> fileAssets) {
        for (FileAsset fa : fileAssets) {
            if (fa.getStatus() != FileAsset.FileAssetStatus.COMPLETED) {
                s3Service.deleteObject(fa.getFileKey());
                fa.markExpired();
            }
        }
        fileAssetRepository.saveAll(fileAssets);
    }

    private boolean isAllowedContentType(String contentType) {
        return ALLOWED_IMAGE_TYPES.contains(contentType) || ALLOWED_VIDEO_TYPES.contains(contentType);
    }

    private long getMaxSizeForContentType(String contentType) {
        if (ALLOWED_VIDEO_TYPES.contains(contentType)) {
            return MAX_VIDEO_SIZE_BYTES;
        }
        return MAX_IMAGE_SIZE_BYTES;
    }

    private String extractExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex >= 0 ? fileName.substring(dotIndex) : "";
    }

    private String getMediaPrefix(String contentType) {
        if (ALLOWED_VIDEO_TYPES.contains(contentType)) {
            return "video/chat/";
        }
        return "image/chat/";
    }
}
