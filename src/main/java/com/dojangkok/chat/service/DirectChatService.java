package com.dojangkok.chat.service;

import com.dojangkok.chat.mq.config.RabbitMQChatConfig;
import com.dojangkok.chat.common.enums.Code;
import com.dojangkok.chat.common.exception.GeneralException;
import com.dojangkok.chat.domain.ChatMessage;
import com.dojangkok.chat.domain.ChatRoom;
import com.dojangkok.chat.dto.MessageContent;
import com.dojangkok.chat.dto.event.ChatMessageEvent;
import com.dojangkok.chat.dto.event.ChatNotificationEvent;
import com.dojangkok.chat.dto.chat.SendMessageRequest;
import com.dojangkok.chat.dto.chatroom.ChatMessageListResponse;
import com.dojangkok.chat.dto.chatroom.ChatMessageResponse;
import com.dojangkok.chat.dto.chatroom.ChatMessageSyncResponse;
import com.dojangkok.chat.mq.ChatNotificationProducer;
import com.dojangkok.chat.mapper.ChatMessageMapper;
import com.dojangkok.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final DirectChatRoomService directChatRoomService;
    private final DirectChatReadStatusService directChatReadStatusService;
    private final ChatMessageMapper chatMessageMapper;
    private final RabbitTemplate rabbitTemplate;
    private final ChatNotificationProducer chatNotificationProducer;

    public void sendMessage(String senderId, SendMessageRequest request) {
        ChatRoom room = directChatRoomService.getRoomByRoomId(request.getRoomId());

        MessageContent content = createContent(request);
        String preview = getPreviewText(request);

        ChatMessage message = ChatMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .roomId(request.getRoomId())
                .senderId(senderId)
                .contentType(request.getContentType())
                .content(content)
                .groupId(request.getGroupId())
                .createdAt(Instant.now())
                .build();

        chatMessageRepository.save(message);

        // 발신자 읽음 처리 (메시지를 보냈으면 해당 방의 모든 메시지를 읽은 것)
        directChatReadStatusService.markAsRead(senderId, request.getRoomId(), message.getMessageId());

        // ChatRoom lastMessage 업데이트
        directChatRoomService.updateLastMessage(request.getRoomId(), ChatRoom.LastMessage.builder()
                .content(preview)
                .contentType(request.getContentType())
                .senderId(senderId)
                .createdAt(message.getCreatedAt())
                .build());

        // 상대방 userId 찾기
        String targetUserId = room.getParticipants().stream()
                .filter(id -> !id.equals(senderId))
                .findFirst()
                .orElseThrow();

        // RabbitMQ Fanout Exchange에 publish
        ChatMessageEvent event = new ChatMessageEvent(
                message.getMessageId(),
                message.getRoomId(),
                message.getSenderId(),
                message.getContentType(),
                chatMessageMapper.toContentDto(message.getContent()),
                message.getGroupId(),
                message.getCreatedAt(),
                targetUserId
        );

        rabbitTemplate.convertAndSend(RabbitMQChatConfig.CHAT_FANOUT_EXCHANGE, "", event);
        log.info("메시지 발행: roomId={}, senderId={}, contentType={}", request.getRoomId(), senderId, request.getContentType());

        // 메인 서버로 채팅 알림 발행 (SSE 전달용)
        ChatRoom.ParticipantInfo senderProfile = room.getParticipantProfiles().stream()
                .filter(p -> p.getUserId().equals(senderId))
                .findFirst()
                .orElse(null);

        ChatNotificationEvent notificationEvent = ChatNotificationEvent.builder()
                .type("chat-message")
                .messageId(message.getMessageId())
                .roomId(message.getRoomId())
                .senderId(senderId)
                .senderNickname(senderProfile != null ? senderProfile.getNickname() : null)
                .senderProfileImageUrl(senderProfile != null ? senderProfile.getProfileImageUrl() : null)
                .targetMemberId(Long.parseLong(targetUserId))
                .contentType(message.getContentType())
                .preview(preview)
                .createdAt(message.getCreatedAt())
                .build();

        chatNotificationProducer.sendNotification(notificationEvent);
    }

    public ChatMessageListResponse getMessagesWithMarkRead(String userId, String roomId, Instant before, int size) {
        ChatRoom room = directChatRoomService.getRoomByRoomId(roomId);

        if (!room.getParticipants().contains(userId)) {
            throw new GeneralException(Code.CHAT_ROOM_ACCESS_DENIED);
        }

        List<ChatMessage> messages = getMessages(roomId, before, size);

        if (!messages.isEmpty()) {
            ChatMessage newest = messages.getFirst();
            directChatReadStatusService.markAsReadAndNotify(userId, roomId, newest.getMessageId());
        }

        List<ChatMessageResponse> messageResponses = messages.stream()
                .map(msg -> chatMessageMapper.toResponse(msg, userId))
                .toList();

        return chatMessageMapper.toListResponse(messageResponses, size);
    }

    public List<ChatMessage> getMessages(String roomId, Instant before, int size) {
        if (before == null) {
            return chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(
                    roomId, PageRequest.of(0, size));
        }
        return chatMessageRepository.findByRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                roomId, before, PageRequest.of(0, size));
    }

    public ChatMessageSyncResponse syncMessages(String userId, String roomId, String afterMessageId) {
        ChatRoom room = directChatRoomService.getRoomByRoomId(roomId);

        if (!room.getParticipants().contains(userId)) {
            throw new GeneralException(Code.CHAT_ROOM_ACCESS_DENIED);
        }

        // afterMessageId로 해당 메시지의 createdAt 조회
        ChatMessage afterMessage = chatMessageRepository.findByMessageId(afterMessageId)
                .orElseThrow(() -> new GeneralException(Code.CHAT_MESSAGE_NOT_FOUND));

        // 그 시점 이후 메시지를 오래된순(ASC)으로 조회 (최대 100건)
        List<ChatMessage> syncedMessages = chatMessageRepository
                .findByRoomIdAndCreatedAtAfterOrderByCreatedAtAsc(
                        roomId, afterMessage.getCreatedAt(), PageRequest.of(0, 100));

        List<ChatMessageResponse> responses = syncedMessages.stream()
                .map(msg -> chatMessageMapper.toResponse(msg, userId))
                .toList();

        log.info("메시지 동기화: roomId={}, afterMessageId={}, syncedCount={}",
                roomId, afterMessageId, responses.size());

        return ChatMessageSyncResponse.builder()
                .messages(responses)
                .syncedCount(responses.size())
                .build();
    }

    private MessageContent createContent(SendMessageRequest request) {
        return switch (request.getContentType()) {
            case "TEXT" -> new MessageContent.TextContent(request.getText());
            case "IMAGE" -> new MessageContent.ImageContent(
                    request.getUrl(),
                    request.getWidth(), request.getHeight(), request.getSize());
            case "VIDEO" -> new MessageContent.VideoContent(
                    request.getUrl(),
                    request.getDuration(), request.getWidth(), request.getHeight(), request.getSize());
            default -> throw new GeneralException(Code.CHAT_INVALID_CONTENT_TYPE);
        };
    }

    private String getPreviewText(SendMessageRequest request) {
        return switch (request.getContentType()) {
            case "TEXT" -> request.getText();
            case "IMAGE" -> "사진을 보냈습니다.";
            case "VIDEO" -> "동영상을 보냈습니다.";
            default -> "";
        };
    }
}
