package com.dojangkok.chat.dto.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ChatMessageEvent.class, name = "MESSAGE"),
        @JsonSubTypes.Type(value = ChatReadEvent.class, name = "READ")
})
public sealed interface ChatEvent permits ChatMessageEvent, ChatReadEvent {

    String targetUserId();
}
