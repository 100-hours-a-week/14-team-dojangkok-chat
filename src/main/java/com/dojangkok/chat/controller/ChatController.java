package com.dojangkok.chat.controller;

import com.dojangkok.chat.dto.chat.ReadStatusRequest;
import com.dojangkok.chat.dto.chat.SendMessageRequest;
import com.dojangkok.chat.service.DirectChatService;
import com.dojangkok.chat.service.DirectChatReadStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final DirectChatService directChatService;
    private final DirectChatReadStatusService directChatReadStatusService;

    @MessageMapping("/chat.send")
    public void sendMessage(SendMessageRequest request, Principal principal) {
        String senderId = principal.getName();
        log.info("메시지 수신: senderId={}, roomId={}, contentType={}", senderId, request.getRoomId(), request.getContentType());
        directChatService.sendMessage(senderId, request);
    }

    @MessageMapping("/chat.read")
    public void markAsRead(ReadStatusRequest request, Principal principal) {
        String userId = principal.getName();
        log.info("읽음 처리: userId={}, roomId={}", userId, request.getRoomId());
        directChatReadStatusService.markAsReadAndNotify(userId, request.getRoomId(), request.getLastReadMessageId());
    }
}
