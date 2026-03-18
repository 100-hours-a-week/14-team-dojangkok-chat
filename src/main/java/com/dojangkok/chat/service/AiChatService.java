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
                    ChatMessage aiMessage = ChatMessage.builder()
                            .messageId(UUID.randomUUID().toString())
                            .roomId(roomId)
                            .senderId(AI_SENDER_ID)
                            .contentType("TEXT")
                            .content(new MessageContent.TextContent(fullResponse.toString()))
                            .createdAt(Instant.now())
                            .build();
                    chatMessageRepository.save(aiMessage);

                    directChatRoomService.updateLastMessage(roomId, ChatRoom.LastMessage.builder()
                            .content(fullResponse.toString())
                            .contentType("TEXT")
                            .senderId(AI_SENDER_ID)
                            .createdAt(aiMessage.getCreatedAt())
                            .build());

                    log.info("AI 응답 저장 완료: roomId={}", roomId);
                })
                .doOnError(e -> log.error("AI 스트리밍 에러: roomId={}", roomId, e));
    }
}
