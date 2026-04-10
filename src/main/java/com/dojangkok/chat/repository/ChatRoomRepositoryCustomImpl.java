package com.dojangkok.chat.repository;

import com.dojangkok.chat.domain.ChatRoom;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ChatRoomRepositoryCustomImpl implements ChatRoomRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public void updateParticipantProfiles(String roomId,
                                          List<ChatRoom.ParticipantInfo> profiles,
                                          Instant eventTimestamp) {
        Query query = new Query(Criteria.where("roomId").is(roomId));
        Update update = new Update()
                .set("participantProfiles", profiles);

        if (eventTimestamp != null) {
            update.set("lastEventTimestamp", eventTimestamp);
        }

        UpdateResult result = mongoTemplate.updateFirst(query, update, ChatRoom.class);
        result.getModifiedCount();
    }

    @Override
    public void updatePropertySnapshot(String roomId,
                                       String propertyTitle,
                                       String propertyImageUrl,
                                       Instant eventTimestamp) {
        Query query = new Query(Criteria.where("roomId").is(roomId));
        Update update = new Update()
                .set("propertyTitle", propertyTitle)
                .set("propertyImageUrl", propertyImageUrl);

        if (eventTimestamp != null) {
            update.set("lastEventTimestamp", eventTimestamp);
        }

        UpdateResult result = mongoTemplate.updateFirst(query, update, ChatRoom.class);
        result.getModifiedCount();
    }

    @Override
    public void updateLastMessage(String roomId, ChatRoom.LastMessage lastMessage) {
        Query query = new Query(Criteria.where("roomId").is(roomId));
        Update update = new Update()
                .set("lastMessage", lastMessage);

        UpdateResult result = mongoTemplate.updateFirst(query, update, ChatRoom.class);
        result.getModifiedCount();
    }
}
