package com.dojangkok.chat.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum Code {

    // 성공 응답
    SUCCESS(HttpStatus.OK.value(), "SUCCESS", "요청이 성공하였습니다."),
    CREATED_SUCCESS(HttpStatus.CREATED.value(), "CREATED_SUCCESS", "요청이 성공하였습니다."),
    ACCEPTED_SUCCESS(HttpStatus.ACCEPTED.value(), "ACCEPTED_SUCCESS", "요청이 성공적으로 접수되었습니다."),

    // 공통 오류
    BAD_REQUEST(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", "잘못된 요청입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_ERROR", "서버 오류가 발생했습니다."),

    // 인증/인가 오류
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "MEMBER_NOT_FOUND", "존재하지 않는 회원입니다."),
    NICKNAME_CONFLICT(HttpStatus.CONFLICT.value(), "NICKNAME_CONFLICT", "이미 존재하는 닉네임입니다."),
    NICKNAME_TOO_LONG(HttpStatus.UNPROCESSABLE_ENTITY.value(), "NICKNAME_TITLE_TOO_LONG", "닉네임의 최대 길이를 초과하였습니다."),
    INVALID_EXCHANGE_CODE(HttpStatus.BAD_REQUEST.value(), "INVALID_EXCHANGE_CODE", "유효하지 않거나 만료된 교환 코드입니다."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED.value(), "INVALID_ACCESS_TOKEN", "유효하지 않은 access token입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED.value(), "INVALID_REFRESH_TOKEN", "유효하지 않은 refresh token입니다."),
    TOKEN_REUSE_DETECTED(HttpStatus.UNAUTHORIZED.value(), "TOKEN_REUSE_DETECTED", "토큰 재사용이 감지되었습니다. 다시 로그인해주세요."),

    // 파일 관련 오류
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "FILE_NOT_FOUND", "업로드된 파일을 찾을 수 없습니다."),
    FILE_ASSET_NOT_COMPLETED(HttpStatus.CONFLICT.value(), "FILE_ASSET_NOT_COMPLETED", "업로드가 완료되지 않은 파일이 포함되어 있습니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY.value(), "FILE_SIZE_EXCEEDED", "파일 용량이 제한을 초과하였습니다."),
    FILE_COUNT_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY.value(), "FILE_COUNT_EXCEEDED", "최대 허용 파일 개수(50개)를 초과하였습니다."),
    FILE_CONTENT_TYPE_NOT_ALLOWED(HttpStatus.UNPROCESSABLE_ENTITY.value(), "FILE_CONTENT_TYPE_NOT_ALLOWED", "허용되지 않는 파일 형식입니다."),
    FILE_SIZE_MISMATCH(HttpStatus.CONFLICT.value(), "FILE_SIZE_MISMATCH", "파일 용량이 신고된 값과 일치하지 않습니다."),
    FILE_CONTENT_TYPE_MISMATCH(HttpStatus.CONFLICT.value(), "FILE_CONTENT_TYPE_MISMATCH", "파일 형식이 신고된 값과 일치하지 않습니다."),
    FILE_UPLOAD_NOT_COMPLETED(HttpStatus.CONFLICT.value(), "FILE_UPLOAD_NOT_COMPLETED", "S3에 파일이 업로드되지 않았습니다."),

    // 라이프스타일 관련 오류
    LIFESTYLE_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "LIFESTYLE_NOT_FOUND", "라이프스타일 정보를 찾을 수 없습니다."),
    LIFESTYLE_VERSION_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "LIFESTYLE_VERSION_NOT_FOUND", "해당 라이프스타일 버전이 존재하지 않습니다."),

    // 체크리스트 관련 오류
    CHECKLIST_TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "CHECKLIST_TEMPLATE_NOT_FOUND", "체크리스트 템플릿이 존재하지 않습니다."),
    CHECKLIST_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "CHECKLIST_NOT_FOUND", "해당 체크리스트가 존재하지 않습니다."),
    CHECKLIST_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "CHECKLIST_ITEM_NOT_FOUND", "해당 체크리스트 항목이 존재하지 않습니다."),

    // 집 노트 관련 오류
    HOME_NOTE_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "HOME_NOTE_NOT_FOUND", "해당 집 노트가 존재하지 않습니다."),
    HOME_NOTE_ACCESS_DENIED(HttpStatus.FORBIDDEN.value(), "HOME_NOTE_ACCESS_DENIED", "해당 집 노트에 대한 권한이 없습니다."),
    HOME_NOTE_TITLE_EMPTY(HttpStatus.BAD_REQUEST.value(), "HOME_NOTE_TITLE_EMPTY", "집 노트의 제목이 비어있습니다."),
    HOME_NOTE_TITLE_TOO_LONG(HttpStatus.UNPROCESSABLE_ENTITY.value(), "HOME_NOTE_TITLE_TOO_LONG", "집 노트 제목의 최대 길이를 초과하였습니다."),
    HOME_NOTE_FILE_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "HOME_NOTE_FILE_NOT_FOUND", "해당 미디어가 존재하지 않습니다."),
    HOME_NOTE_FILE_RELATION_CONFLICT(HttpStatus.CONFLICT.value(), "HOME_NOTE_FILE_RELATION_CONFLICT", "해당 미디어가 집 노트에 포함되어 있지 않습니다."),
    HOME_NOTE_ITEMS_TOO_MANY(HttpStatus.UNPROCESSABLE_ENTITY.value(), "HOME_NOTE_ITEMS_TOO_MANY", "최대 첨부 가능한 이미지 개수를 초과하였습니다."),

    // 쉬운 계약서 관련 오류
    EASY_CONTRACT_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "EASY_CONTRACT_NOT_FOUND", "해당 쉬운 계약서가 존재하지 않습니다."),
    EASY_CONTRACT_ACCESS_DENIED(HttpStatus.FORBIDDEN.value(), "EASY_CONTRACT_ACCESS_DENIED", "해당 쉬운 계약서에 대한 권한이 없습니다."),
    EASY_CONTRACT_TITLE_EMPTY(HttpStatus.BAD_REQUEST.value(), "EASY_CONTRACT_TITLE_EMPTY", "쉬운 계약서의 제목이 비어있습니다."),
    EASY_CONTRACT_TITLE_TOO_LONG(HttpStatus.UNPROCESSABLE_ENTITY.value(), "EASY_CONTRACT_TITLE_TOO_LONG", "쉬운 계약서 제목의 최대 길이를 초과하였습니다."),
    EASY_CONTRACT_FILE_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "EASY_CONTRACT_FILE_NOT_FOUND", "해당 파일이 존재하지 않습니다."),
    EASY_CONTRACT_FILE_RELATION_CONFLICT(HttpStatus.CONFLICT.value(), "EASY_CONTRACT_FILE_RELATION_CONFLICT", "해당 파일이 쉬운 계약서에 포함되어 있지 않습니다."),
    EASY_CONTRACT_CANCEL_NOT_ALLOWED(HttpStatus.CONFLICT.value(), "EASY_CONTRACT_CANCEL_NOT_ALLOWED", "이미 취소되었거나 작업이 완료되어 취소할 수 없습니다."),

    // 매물 게시글 관련 오류
    PROPERTY_POST_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "PROPERTY_POST_NOT_FOUND", "해당 매물 게시글이 존재하지 않습니다."),
    PROPERTY_POST_FORBIDDEN(HttpStatus.FORBIDDEN.value(), "PROPERTY_POST_FORBIDDEN", "해당 매물 게시글에 대한 권한이 없습니다."),
    PROPERTY_POST_UPDATE_NOT_ALLOWED(HttpStatus.CONFLICT.value(), "PROPERTY_POST_UPDATE_NOT_ALLOWED", "현재 상태에서는 게시글을 수정할 수 없습니다."),
    PROPERTY_POST_FILE_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "PROPERTY_POST_FILE_NOT_FOUND", "해당 이미지가 존재하지 않습니다."),
    PROPERTY_POST_FILE_RELATION_CONFLICT(HttpStatus.CONFLICT.value(), "PROPERTY_POST_FILE_RELATION_CONFLICT", "해당 이미지가 매물 게시글에 포함되어 있지 않습니다."),
    PROPERTY_POST_DELETED(HttpStatus.GONE.value(), "PROPERTY_POST_DELETED", "삭제된 매물 게시글입니다."),
    TITLE_INVALID(HttpStatus.BAD_REQUEST.value(), "TITLE_INVALID", "제목 형식이 올바르지 않습니다."),
    PRICE_INVALID(HttpStatus.BAD_REQUEST.value(), "PRICE_INVALID", "가격 정보가 올바르지 않습니다."),
    AREA_INVALID(HttpStatus.BAD_REQUEST.value(), "AREA_INVALID", "면적 정보가 올바르지 않습니다."),
    FLOOR_INVALID(HttpStatus.BAD_REQUEST.value(), "FLOOR_INVALID", "층수 정보가 올바르지 않습니다."),
    DEAL_STATUS_INVALID(HttpStatus.BAD_REQUEST.value(), "DEAL_STATUS_INVALID", "deal_status 값이 올바르지 않습니다."),
    POST_VISIBILITY_INVALID(HttpStatus.BAD_REQUEST.value(), "POST_VISIBILITY_INVALID", "is_hidden 값이 올바르지 않습니다."),
    BOOKMARK_ALREADY_EXISTS(HttpStatus.CONFLICT.value(), "BOOKMARK_ALREADY_EXISTS", "이미 스크랩된 매물입니다."),
    BOOKMARK_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "BOOKMARK_NOT_FOUND", "스크랩된 매물이 아닙니다."),
    SORT_INVALID(HttpStatus.BAD_REQUEST.value(), "SORT_INVALID", "정렬 조건이 올바르지 않습니다."),
    PRICE_RANGE_INVALID(HttpStatus.BAD_REQUEST.value(), "PRICE_RANGE_INVALID", "가격 범위가 올바르지 않습니다."),
    FILTER_INVALID(HttpStatus.BAD_REQUEST.value(), "FILTER_INVALID", "필터 값이 올바르지 않습니다."),

    // AI 서비스 관련 오류
    AI_SERVICE_ERROR(HttpStatus.BAD_GATEWAY.value(), "AI_SERVICE_ERROR", "AI 서비스에서 오류가 발생하였습니다."),
    AI_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE.value(), "AI_SERVICE_UNAVAILABLE", "AI 서비스에 연결할 수 없습니다.");

    private final int status;
    private final String code;
    private final String message;
}
