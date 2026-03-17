package com.dojangkok.chat.repository;

import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
@RequiredArgsConstructor
public class ChatMessageRepositoryCustomImpl implements ChatMessageRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public long countUnreadMessages(String roomId, String userId, Instant after) {
        /*
         * 1. match: roomId 일치 + senderId != userId + createdAt > after
         * 2. group: groupId 기준으로 묶음 (groupId가 null이면 messageId로 개별 카운트)
         * 3. count: 그룹 수 = 안 읽은 메시지 수
         *
         * groupId가 null인 메시지(텍스트, 단일 미디어)는 각각의 messageId로 그룹핑되어 개별 1건.
         * groupId가 같은 메시지(다중 미디어 묶음)는 하나로 그룹핑되어 1건.
         */
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(
                        Criteria.where("roomId").is(roomId)
                                .and("senderId").ne(userId)
                                .and("createdAt").gt(after)
                ),
                // groupId가 null이면 messageId를 사용하여 개별 카운트
                Aggregation.project()
                        .and("messageId").as("messageId")
                        .and("groupId").as("groupId")
                        .andExpression("ifNull(groupId, messageId)").as("groupKey"),
                Aggregation.group("groupKey"),
                Aggregation.count().as("total")
        );

        AggregationResults<Document> results = mongoTemplate.aggregate(
                aggregation, "chat_messages", Document.class);

        Document result = results.getUniqueMappedResult();
        if (result == null) return 0;
        return result.getInteger("total", 0);
    }
}
