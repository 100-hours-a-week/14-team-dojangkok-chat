package com.dojangkok.chat.common.dto;

import com.dojangkok.chat.common.enums.Code;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonPropertyOrder({"code", "message"})
public class ResponseDto {

    private final Code code;
    private final String message;

}
