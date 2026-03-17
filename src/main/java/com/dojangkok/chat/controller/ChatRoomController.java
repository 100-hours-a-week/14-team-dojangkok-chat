package com.dojangkok.chat.controller;

import com.dojangkok.chat.auth.CurrentMemberId;
import com.dojangkok.chat.common.dto.DataResponseDto;
import com.dojangkok.chat.common.enums.Code;
import com.dojangkok.chat.dto.chatroom.CreateRoomRequest;
import com.dojangkok.chat.dto.chatroom.CreateRoomResponse;
import com.dojangkok.chat.dto.chatroom.ChatMessageListResponse;
import com.dojangkok.chat.dto.chatroom.ChatRoomDetailResponse;
import com.dojangkok.chat.dto.chatroom.ChatRoomListResponse;
import com.dojangkok.chat.service.ChatMessageService;
import com.dojangkok.chat.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v3/chat/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    @PostMapping
    public DataResponseDto<CreateRoomResponse> createRoom(
            @CurrentMemberId String userId,
            @RequestBody CreateRoomRequest request) {
        CreateRoomResponse response = chatRoomService.createDirectRoom(userId, request);
        return new DataResponseDto<>(Code.SUCCESS, "채팅방 생성에 성공하였습니다.", response);
    }

    @GetMapping
    public DataResponseDto<ChatRoomListResponse> getMyRooms(
            @CurrentMemberId String userId) {
        ChatRoomListResponse response = chatRoomService.getMyRooms(userId);
        return new DataResponseDto<>(Code.SUCCESS, "채팅방 목록 조회에 성공하였습니다.", response);
    }

    @GetMapping("/{roomId}")
    public DataResponseDto<ChatRoomDetailResponse> getRoomDetail(
            @CurrentMemberId String userId,
            @PathVariable String roomId) {
        ChatRoomDetailResponse response = chatRoomService.getRoomDetail(userId, roomId);
        return new DataResponseDto<>(Code.SUCCESS, "채팅방 상세 조회에 성공하였습니다.", response);
    }

    @GetMapping("/{roomId}/messages")
    public DataResponseDto<ChatMessageListResponse> getMessages(
            @CurrentMemberId String userId,
            @PathVariable String roomId,
            @RequestParam(required = false) Instant before,
            @RequestParam(defaultValue = "20") int size) {
        ChatMessageListResponse response = chatMessageService.getMessagesWithMarkRead(userId, roomId, before, size);
        return new DataResponseDto<>(Code.SUCCESS, "메시지 조회에 성공하였습니다.", response);
    }
}
