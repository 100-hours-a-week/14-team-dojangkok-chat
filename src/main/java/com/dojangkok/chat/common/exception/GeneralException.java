package com.dojangkok.chat.common.exception;

import com.dojangkok.chat.common.enums.Code;
import lombok.Getter;

@Getter
public class GeneralException extends RuntimeException {

    private final Code code;
    private final Object data;

    public GeneralException(Code code) {
        super(code.getMessage());
        this.code = code;
        this.data = null;
    }

    public GeneralException(Code code, Object data) {
        super(code.getMessage());
        this.code = code;
        this.data = data;
    }
}
