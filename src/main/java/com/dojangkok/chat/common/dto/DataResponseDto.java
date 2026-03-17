package com.dojangkok.chat.common.dto;

import com.dojangkok.chat.common.enums.Code;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

@Getter
@JsonPropertyOrder({"code", "message", "data"})
public class DataResponseDto<T> extends ResponseDto {

    private final T data;

    public DataResponseDto(Code code, T data) {
        super(code, code.getMessage());
        this.data = data;
    }

    public DataResponseDto(Code code, String message, T data) {
        super(code, message);
        this.data = data;
    }
}
