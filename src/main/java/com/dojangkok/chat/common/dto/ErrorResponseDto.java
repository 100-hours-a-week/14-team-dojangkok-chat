package com.dojangkok.chat.common.dto;

import com.dojangkok.chat.common.enums.Code;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"code", "message", "data"})
public class ErrorResponseDto extends ResponseDto {

    private final Object data;

    public ErrorResponseDto(Code code) {
        super(code, code.getMessage());
        this.data = null;
    }

    public ErrorResponseDto(Code code, String message) {
        super(code, message);
        this.data = null;
    }

    public ErrorResponseDto(Code code, Object data) {
        super(code, code.getMessage());
        this.data = data;
    }
}
