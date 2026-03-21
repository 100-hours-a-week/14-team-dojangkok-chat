package com.dojangkok.chat.service;

import com.dojangkok.chat.common.enums.Code;
import com.dojangkok.chat.common.exception.GeneralException;
import com.dojangkok.chat.domain.ChatMessage;
import com.dojangkok.chat.domain.ChatRoom;
import com.dojangkok.chat.dto.MessageContent;
import com.dojangkok.chat.repository.ChatMessageRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final DirectChatRoomService directChatRoomService;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    @Value("${ai.api.url}")
    private String aiApiUrl;

    @Value("${ai.api.secret-key}")
    private String aiApiSecretKey;

    private static final String AI_SENDER_ID = "AI_ASSISTANT";

    public Flux<String> streamAiResponse(String userId, String roomId, String message) {
        if (message == null || message.isBlank()) {
            throw new GeneralException(Code.BAD_REQUEST);
        }

        ChatRoom room = directChatRoomService.getRoomByRoomId(roomId);

        if (room.getEasyContractId() == null) {
            throw new GeneralException(Code.CHAT_ROOM_TYPE_MISMATCH);
        }

        ChatMessage userMessage = ChatMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .roomId(roomId)
                .senderId(userId)
                .contentType("TEXT")
                .content(new MessageContent.TextContent(message))
                .createdAt(Instant.now())
                .build();
        chatMessageRepository.save(userMessage);

        StringBuilder fullResponse = new StringBuilder();
        // cancel/complete/error 중 한 번만 저장되도록 보장
        AtomicBoolean saved = new AtomicBoolean(false);

        return webClientBuilder.build()
                .post()
                .uri(aiApiUrl + "/api/chat/" + room.getEasyContractId() + "/stream")
                .header("Authorization", "Bearer " + aiApiSecretKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("question", message))
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .filter(sse -> sse.data() != null)
                .map(sse -> {
                    String data = sse.data();
                    try {
                        JsonNode node = objectMapper.readTree(data);
                        String text = node.has("text") ? node.get("text").asText() : "";
                        boolean done = node.has("done") && node.get("done").asBoolean();
                        if (!done) {
                            fullResponse.append(text);
                        }
                    } catch (Exception e) {
                        log.warn("AI SSE 이벤트 파싱 실패: {}", data, e);
                    }
                    return data;
                })
                .doOnComplete(() -> {
                    saveAiResponse(roomId, fullResponse, saved, "완료");
                })
                .doOnCancel(() -> {
                    saveAiResponse(roomId, fullResponse, saved, "중단(사용자 이탈)");
                })
                .doOnError(e -> {
                    log.error("AI 스트리밍 에러: roomId={}", roomId, e);
                    saveAiResponse(roomId, fullResponse, saved, "에러");
                });
    }

    /**
     * AI 응답을 DB에 저장 (complete/cancel/error 중 한 번만 실행)
     */
    private void saveAiResponse(String roomId, StringBuilder fullResponse,
                                 AtomicBoolean saved, String reason) {
        if (!saved.compareAndSet(false, true)) {
            return; // 이미 저장됨
        }

        String content = fullResponse.toString();
        if (content.isBlank()) {
            log.info("AI 응답 {} → 저장할 내용 없음: roomId={}", reason, roomId);
            return;
        }

        try {
            ChatMessage aiMessage = ChatMessage.builder()
                    .messageId(UUID.randomUUID().toString())
                    .roomId(roomId)
                    .senderId(AI_SENDER_ID)
                    .contentType("TEXT")
                    .content(new MessageContent.TextContent(content))
                    .createdAt(Instant.now())
                    .build();
            chatMessageRepository.save(aiMessage);

            directChatRoomService.updateLastMessage(roomId, ChatRoom.LastMessage.builder()
                    .content(content)
                    .contentType("TEXT")
                    .senderId(AI_SENDER_ID)
                    .createdAt(aiMessage.getCreatedAt())
                    .build());

            log.info("AI 응답 저장 {} : roomId={}, length={}", reason, roomId, content.length());
        } catch (Exception e) {
            log.error("AI 응답 저장 실패: roomId={}, reason={}", roomId, reason, e);
        }
    }
}
