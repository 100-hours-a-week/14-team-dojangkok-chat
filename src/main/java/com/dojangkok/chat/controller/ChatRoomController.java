package com.dojangkok.chat.controller;

import com.dojangkok.chat.auth.CurrentMemberId;
import com.dojangkok.chat.common.dto.DataResponseDto;
import com.dojangkok.chat.common.enums.Code;
import com.dojangkok.chat.dto.chatroom.CreateRoomRequest;
import com.dojangkok.chat.dto.chatroom.CreateRoomResponse;
import com.dojangkok.chat.dto.chatroom.ChatMessageListResponse;
import com.dojangkok.chat.dto.chatroom.ChatMessageSyncResponse;
import com.dojangkok.chat.dto.chatroom.ChatRoomDetailResponse;
import com.dojangkok.chat.dto.chatroom.ChatRoomListResponse;
import com.dojangkok.chat.service.DirectChatService;
import com.dojangkok.chat.service.DirectChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/chat/v3/direct-chat/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final DirectChatRoomService directChatRoomService;
    private final DirectChatService directChatService;

    @PostMapping
    public DataResponseDto<CreateRoomResponse> createRoom(
            @CurrentMemberId String userId,
            @RequestBody CreateRoomRequest request) {
        CreateRoomResponse response = directChatRoomService.getOrCreateDirectChatRoom(userId, request);
        return new DataResponseDto<>(Code.SUCCESS, "채팅방 생성에 성공하였습니다.", response);
    }

    @GetMapping
    public DataResponseDto<ChatRoomListResponse> getMyRooms(
            @CurrentMemberId String userId) {
        ChatRoomListResponse response = directChatRoomService.getMyRooms(userId);
        return new DataResponseDto<>(Code.SUCCESS, "채팅방 목록 조회에 성공하였습니다.", response);
    }

    @GetMapping("/{roomId}")
    public DataResponseDto<ChatRoomDetailResponse> getRoomDetail(
            @CurrentMemberId String userId,
            @PathVariable String roomId,
            @RequestParam(defaultValue = "false") boolean legacy) {
        ChatRoomDetailResponse response = directChatRoomService.getRoomDetail(userId, roomId, legacy);
        return new DataResponseDto<>(Code.SUCCESS, "채팅방 상세 조회에 성공하였습니다.", response);
    }

    @GetMapping("/{roomId}/messages")
    public DataResponseDto<ChatMessageListResponse> getMessages(
            @CurrentMemberId String userId,
            @PathVariable String roomId,
            @RequestParam(required = false) Instant before,
            @RequestParam(defaultValue = "20") int size) {
        ChatMessageListResponse response = directChatService.getMessagesWithMarkRead(userId, roomId, before, size);
        return new DataResponseDto<>(Code.SUCCESS, "메시지 조회에 성공하였습니다.", response);
    }

    @GetMapping("/{roomId}/messages/sync")
    public DataResponseDto<ChatMessageSyncResponse> syncMessages(
            @CurrentMemberId String userId,
            @PathVariable String roomId,
            @RequestParam String after) {
        ChatMessageSyncResponse response = directChatService.syncMessages(userId, roomId, after);
        return new DataResponseDto<>(Code.SUCCESS, "메시지 동기화에 성공하였습니다.", response);
    }
}
