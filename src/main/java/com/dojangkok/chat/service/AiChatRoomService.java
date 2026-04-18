package com.dojangkok.chat.service;

import com.dojangkok.chat.common.enums.Code;
import com.dojangkok.chat.common.exception.GeneralException;
import com.dojangkok.chat.domain.ChatMessage;
import com.dojangkok.chat.domain.ChatRoom;
import com.dojangkok.chat.dto.ai.AiChatMessageListResponse;
import com.dojangkok.chat.dto.ai.AiChatMessageResponse;
import com.dojangkok.chat.dto.ai.AiRoomResponse;
import com.dojangkok.chat.mapper.AiChatRoomMapper;
import com.dojangkok.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatRoomService {

    private static final String AI_SENDER_ID = "AI_ASSISTANT";
    private static final String ROOM_TYPE_AI = "AI";

    private final ChatRoomRepository chatRoomRepository;
    private final DirectChatRoomService directChatRoomService;
    private final DirectChatService directChatService;
    private final AiChatRoomMapper aiChatRoomMapper;

    public AiRoomResponse getOrCreateAiChatRoom(String userId, String easyContractId) {
        try {
            ChatRoom room = chatRoomRepository
                    .findByTypeAndParticipantsContainingAndEasyContractId(ROOM_TYPE_AI, userId, easyContractId)
                    .orElseGet(() -> {
                        ChatRoom newRoom = ChatRoom.builder()
                                .roomId(UUID.randomUUID().toString())
                                .type(ROOM_TYPE_AI)
                                .participants(List.of(userId, AI_SENDER_ID))
                                .easyContractId(easyContractId)
                                .createdAt(Instant.now())
                                .build();
                        return chatRoomRepository.save(newRoom);
                    });
            return aiChatRoomMapper.toAiRoomResponse(room);
        } catch (DuplicateKeyException e) {
            log.info("AI 채팅방 동시 생성 감지 → 기존 방 반환: userId={}, easyContractId={}", userId, easyContractId);
            ChatRoom existingRoom = chatRoomRepository
                    .findByTypeAndParticipantsContainingAndEasyContractId(ROOM_TYPE_AI, userId, easyContractId)
                    .orElseThrow(() -> new GeneralException(Code.CHAT_ROOM_NOT_FOUND));
            return aiChatRoomMapper.toAiRoomResponse(existingRoom);
        }
    }

    public AiChatMessageListResponse getMessages(String userId, String roomId, Instant before, int size) {
        ChatRoom room = directChatRoomService.getRoomByRoomId(roomId);
        validateAiRoomAccess(room, userId);

        List<ChatMessage> messages = directChatService.getMessages(roomId, before, size);

        List<AiChatMessageResponse> messageResponses = messages.stream()
                .map(aiChatRoomMapper::toAiMessageResponse)
                .toList();

        return aiChatRoomMapper.toAiMessageListResponse(messageResponses, size);
    }

    private void validateAiRoomAccess(ChatRoom room, String userId) {
        if (!room.getType().equals(ROOM_TYPE_AI)) {
            throw new GeneralException(Code.CHAT_ROOM_TYPE_MISMATCH);
        }
        if (!room.getParticipants().contains(userId)) {
            throw new GeneralException(Code.CHAT_ROOM_ACCESS_DENIED);
        }
    }
}
