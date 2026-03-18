package com.dojangkok.chat.dto.ai;

import lombok.Getter;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.annotations.NotNull;

@Getter
@NoArgsConstructor
public class AiChatRequest {

    @NotNull
    private String message;
}
