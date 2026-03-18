package com.dojangkok.chat.common.exception;

import com.dojangkok.chat.common.dto.ErrorResponseDto;
import com.dojangkok.chat.common.enums.Code;
import lombok.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GeneralExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler
    public ResponseEntity<ErrorResponseDto> generalExceptionHandler(GeneralException e) {
        HttpStatusCode status = HttpStatusCode.valueOf(e.getCode().getStatus());
        ErrorResponseDto body = new ErrorResponseDto(e.getCode(), e.getData());
        return ResponseEntity
                .status(status)
                .body(body);
    }

    @Override
    @NonNull
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            @NonNull MethodArgumentNotValidException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request
    ) {
        // 가장 첫 필드 에러 메시지로 통일
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("요청 값이 올바르지 않습니다.");

        ErrorResponseDto body = new ErrorResponseDto(Code.BAD_REQUEST, message);
        return ResponseEntity.status(Code.BAD_REQUEST.getStatus()).body(body);
    }
}
