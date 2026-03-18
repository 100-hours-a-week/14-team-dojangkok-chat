package com.dojangkok.chat.controller;

import com.dojangkok.chat.auth.CurrentMemberId;
import com.dojangkok.chat.common.dto.DataResponseDto;
import com.dojangkok.chat.common.enums.Code;
import com.dojangkok.chat.dto.ai.AiChatRequest;
import com.dojangkok.chat.dto.ai.AiRoomResponse;
import com.dojangkok.chat.dto.ai.CreateAiRoomRequest;
import com.dojangkok.chat.dto.chatroom.ChatMessageListResponse;
import com.dojangkok.chat.service.AiChatRoomService;
import com.dojangkok.chat.service.AiChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Instant;

@RestController
@RequestMapping("/api/v3/ai-chat")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;
    private final AiChatRoomService aiChatRoomService;

    @PostMapping("/rooms")
    public DataResponseDto<AiRoomResponse> createRoom(
            @CurrentMemberId String userId,
            @RequestBody CreateAiRoomRequest request) {
        AiRoomResponse response = aiChatRoomService.getOrCreateAiChatRoom(userId, request.getEasyContractId());
        return new DataResponseDto<>(Code.SUCCESS, "AI 채팅방 생성에 성공하였습니다.", response);
    }

    @GetMapping("/rooms/{roomId}")
    public DataResponseDto<AiRoomResponse> getRoomDetail(
            @CurrentMemberId String userId,
            @PathVariable String roomId) {
        AiRoomResponse response = aiChatRoomService.getRoomDetail(userId, roomId);
        return new DataResponseDto<>(Code.SUCCESS, "AI 채팅방 상세 조회에 성공하였습니다.", response);
    }

    @GetMapping("/rooms/{roomId}/messages")
    public DataResponseDto<ChatMessageListResponse> getMessages(
            @CurrentMemberId String userId,
            @PathVariable String roomId,
            @RequestParam(required = false) Instant before,
            @RequestParam(defaultValue = "20") int size) {
        ChatMessageListResponse response = aiChatRoomService.getMessages(userId, roomId, before, size);
        return new DataResponseDto<>(Code.SUCCESS, "AI 채팅 메시지 조회에 성공하였습니다.", response);
    }

    @PostMapping(value = "/rooms/{roomId}/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(
            @CurrentMemberId String userId,
            @PathVariable String roomId,
            @Valid @RequestBody AiChatRequest request) {
        return aiChatService.streamAiResponse(userId, roomId, request.getMessage());
    }
}
