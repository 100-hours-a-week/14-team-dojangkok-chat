package com.dojangkok.chat.repository;

import com.dojangkok.chat.domain.ChatRoom;
import com.mongodb.event.CommandListener;
import com.mongodb.event.CommandStartedEvent;
import org.bson.BsonDocument;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MongoDB Compass로 Lost Update 결과를 직접 확인하기 위한 테스트.
 *
 * <h2>사전 준비</h2>
 * <ol>
 *   <li>로컬에서 MongoDB가 실행 중이어야 합니다 (localhost:27017)</li>
 *   <li>MongoDB Compass를 열고 mongodb://localhost:27017 에 연결</li>
 * </ol>
 *
 * <h2>실행 방법</h2>
 * <pre>
 * ./gradlew test --tests "com.dojangkok.chat.repository.SnapshotLostUpdateCompassTest" --info
 * </pre>
 *
 * <h2>Compass에서 확인할 내용</h2>
 * <ol>
 *   <li>dojangkok_test 데이터베이스 → chat_rooms 컬렉션 클릭</li>
 *   <li>Filter: { "roomId": /^room-save/ } → save() 방식 결과 확인</li>
 *   <li>Filter: { "roomId": /^room-set/ } → $set 방식 결과 확인</li>
 *   <li>save() 방식의 document에서 lastMessage나 propertyTitle이 "원본" 값으로 남아있는 것 = Lost Update 증거</li>
 *   <li>$set 방식의 document는 모든 필드가 "새" 값으로 정상 반영됨</li>
 * </ol>
 *
 * <h2>테스트 종료 후</h2>
 * 테스트가 끝나면 10분간 대기합니다. 이 시간 동안 Compass에서 데이터를 확인하세요.
 * 확인이 끝나면 Ctrl+C로 테스트를 종료하거나, 10분 후 자동 종료됩니다.
 * 데이터는 자동 삭제되지 않으므로, 나중에 수동으로 dojangkok_test DB를 drop하면 됩니다.
 */
@DataMongoTest
@TestPropertySource(properties = {
        "spring.data.mongodb.uri=mongodb://localhost:27017",
        "spring.data.mongodb.database=dojangkok_test"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Import(SnapshotLostUpdateCompassTest.MongoCommandCounterConfig.class)
class SnapshotLostUpdateCompassTest {

    // ================================================================
    //  CommandListener — DB 호출 실측
    // ================================================================

    static class MongoCommandCounter implements CommandListener {
        private static final String TARGET = "chat_rooms";
        private static final Set<String> READ_CMDS = Set.of("find");
        private static final Set<String> WRITE_CMDS = Set.of("update", "insert");

        private final AtomicInteger readCount = new AtomicInteger(0);
        private final AtomicInteger writeCount = new AtomicInteger(0);
        private final AtomicInteger replaceCount = new AtomicInteger(0);
        private final AtomicInteger setUpdateCount = new AtomicInteger(0);

        @Override
        public void commandStarted(CommandStartedEvent event) {
            String cmd = event.getCommandName();
            BsonDocument doc = event.getCommand();
            String col = null;
            if (doc.containsKey(cmd)) {
                try { col = doc.getString(cmd).getValue(); } catch (Exception ignored) {}
            }
            if (!TARGET.equals(col)) return;

            if (READ_CMDS.contains(cmd)) {
                readCount.incrementAndGet();
            } else if (WRITE_CMDS.contains(cmd)) {
                writeCount.incrementAndGet();
                if ("update".equals(cmd) && doc.containsKey("updates")) {
                    try {
                        BsonDocument first = doc.getArray("updates").get(0).asDocument();
                        BsonDocument u = first.getDocument("u");
                        if (u.containsKey("$set")) setUpdateCount.incrementAndGet();
                        else replaceCount.incrementAndGet();
                    } catch (Exception ignored) {}
                }
            }
        }

        public void reset() {
            readCount.set(0); writeCount.set(0);
            replaceCount.set(0); setUpdateCount.set(0);
        }

        public int getReadCount() { return readCount.get(); }
        public int getWriteCount() { return writeCount.get(); }
        public int getReplaceCount() { return replaceCount.get(); }
        public int getSetUpdateCount() { return setUpdateCount.get(); }
        public int getTotalCount() { return readCount.get() + writeCount.get(); }
    }

    static final MongoCommandCounter commandCounter = new MongoCommandCounter();

    @TestConfiguration
    static class MongoCommandCounterConfig {
        @Bean
        public MongoClientSettingsBuilderCustomizer commandCounterCustomizer() {
            return builder -> builder.addCommandListener(commandCounter);
        }
    }

    // ================================================================
    //  테스트 인프라
    // ================================================================

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private ChatRoomRepositoryCustomImpl chatRoomRepositoryCustom;

    private static final int ITERATIONS = 10; // Compass 확인용이므로 10건이면 충분
    private static final int DETAIL_LOG_COUNT = 5;

    @BeforeAll
    static void printInstructions() {
        System.out.println();
        System.out.println("  ╔══════════════════════════════════════════════════════════╗");
        System.out.println("  ║  MongoDB Compass 확인용 테스트                           ║");
        System.out.println("  ╠══════════════════════════════════════════════════════════╣");
        System.out.println("  ║                                                          ║");
        System.out.println("  ║  1. MongoDB Compass를 열어주세요                         ║");
        System.out.println("  ║  2. mongodb://localhost:27017 에 연결                    ║");
        System.out.println("  ║  3. 테스트가 끝나면 dojangkok_test DB를 확인             ║");
        System.out.println("  ║                                                          ║");
        System.out.println("  ║  확인 포인트:                                            ║");
        System.out.println("  ║  • chat_rooms 컬렉션에서 roomId로 필터링                 ║");
        System.out.println("  ║  • { roomId: /^room-save/ } → save() 결과               ║");
        System.out.println("  ║  • { roomId: /^room-set/ }  → $set 결과                 ║");
        System.out.println("  ║  • save() 쪽에서 lastMessage나 propertyTitle이           ║");
        System.out.println("  ║    \"원본\" 값으로 남아있으면 = Lost Update 증거            ║");
        System.out.println("  ║                                                          ║");
        System.out.println("  ╚══════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    @BeforeEach
    void setUp() {
        chatRoomRepositoryCustom = new ChatRoomRepositoryCustomImpl(mongoTemplate);
        // 기존 테스트 데이터만 삭제 (이전 실행 결과 정리)
        chatRoomRepository.deleteAll();
        commandCounter.reset();
    }

    // ================================================================
    //  테스트 1: save() 방식 — Lost Update 발생
    // ================================================================
    @Test
    @Order(1)
    @DisplayName("[save() 방식] Compass에서 Lost Update 확인")
    void testSaveMethod_ForCompass() throws Exception {
        System.out.println();
        System.out.println("[save() 방식] 테스트 시작 — 2개 스레드가 동시에 save() 호출");
        System.out.println("  Thread A: lastMessage 업데이트 (read → modify → save)");
        System.out.println("  Thread B: propertyTitle 업데이트 (read → modify → save)");
        System.out.println();

        AtomicInteger lostTotal = new AtomicInteger(0);
        AtomicInteger msgLostTotal = new AtomicInteger(0);
        AtomicInteger propLostTotal = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        commandCounter.reset();

        for (int i = 0; i < ITERATIONS; i++) {
            String roomId = "room-save-" + i;
            chatRoomRepository.save(createTestRoom(roomId, "원본 메시지", "원본 매물"));

            String expectedMsg = "새 메시지-" + i;
            String expectedTitle = "새 매물-" + i;

            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch go = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(2);

            executor.submit(() -> {
                try {
                    ready.countDown(); go.await();
                    ChatRoom r = chatRoomRepository.findByRoomId(roomId).orElseThrow();
                    r.updateLastMessage(ChatRoom.LastMessage.builder()
                            .content(expectedMsg).contentType("TEXT")
                            .senderId("user-1").createdAt(Instant.now()).build());
                    chatRoomRepository.save(r);
                } catch (Exception e) { e.printStackTrace(); }
                finally { done.countDown(); }
            });

            executor.submit(() -> {
                try {
                    ready.countDown(); go.await();
                    ChatRoom r = chatRoomRepository.findByRoomId(roomId).orElseThrow();
                    r.updatePropertySnapshot(expectedTitle, "새이미지-" + i);
                    chatRoomRepository.save(r);
                } catch (Exception e) { e.printStackTrace(); }
                finally { done.countDown(); }
            });

            ready.await();
            go.countDown();
            done.await();

            // DB에서 다시 읽어서 검증
            ChatRoom result = chatRoomRepository.findByRoomId(roomId).orElseThrow();
            String actualMsg = result.getLastMessage() != null ? result.getLastMessage().getContent() : "null";
            String actualTitle = result.getPropertyTitle();

            boolean msgLost = !expectedMsg.equals(actualMsg);
            boolean propLost = !expectedTitle.equals(actualTitle);
            List<String> lostFields = new ArrayList<>();
            if (msgLost) { lostFields.add("lastMessage"); msgLostTotal.incrementAndGet(); }
            if (propLost) { lostFields.add("propertyTitle"); propLostTotal.incrementAndGet(); }
            boolean isLost = !lostFields.isEmpty();
            if (isLost) lostTotal.incrementAndGet();

            // 모든 iteration 상세 로그 (10건이므로 전부 출력)
            System.out.printf("  #%d 검증 (roomId=%s)%n", i + 1, roomId);
            System.out.printf("    expected: lastMessage=\"%s\", propertyTitle=\"%s\"%n", expectedMsg, expectedTitle);
            System.out.printf("    actual  : lastMessage=\"%s\", propertyTitle=\"%s\"%n", actualMsg, actualTitle);
            if (isLost) {
                System.out.printf("    → ✗ LOST — %s 유실%n", String.join(", ", lostFields));
            } else {
                System.out.println("    → ✓ OK");
            }
            System.out.println();
        }

        executor.shutdown();

        printSummary("save()", lostTotal.get(), msgLostTotal.get(), propLostTotal.get());

        System.out.println("  💡 Compass에서 확인:");
        System.out.println("     DB: dojangkok_test → Collection: chat_rooms");
        System.out.println("     Filter: { \"roomId\": { \"$regex\": \"^room-save\" } }");
        System.out.println("     → lastMessage.content이 \"원본 메시지\"로 남아있거나");
        System.out.println("       propertyTitle이 \"원본 매물\"로 남아있는 document = Lost Update 증거");
        System.out.println();
    }

    // ================================================================
    //  테스트 2: $set 방식 — Lost Update 0건
    // ================================================================
    @Test
    @Order(2)
    @DisplayName("[$set 방식] Compass에서 모든 필드 정상 반영 확인")
    void testSetMethod_ForCompass() throws Exception {
        System.out.println();
        System.out.println("[$set 방식] 테스트 시작 — 2개 스레드가 동시에 $set 호출");
        System.out.println("  Thread A: lastMessage 업데이트 (updateOne + $set)");
        System.out.println("  Thread B: propertyTitle 업데이트 (updateOne + $set)");
        System.out.println();

        AtomicInteger lostTotal = new AtomicInteger(0);
        AtomicInteger msgLostTotal = new AtomicInteger(0);
        AtomicInteger propLostTotal = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        commandCounter.reset();

        for (int i = 0; i < ITERATIONS; i++) {
            String roomId = "room-set-" + i;
            chatRoomRepository.save(createTestRoom(roomId, "원본 메시지", "원본 매물"));

            String expectedMsg = "새 메시지-" + i;
            String expectedTitle = "새 매물-" + i;

            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch go = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(2);

            executor.submit(() -> {
                try {
                    ready.countDown(); go.await();
                    chatRoomRepositoryCustom.updateLastMessage(roomId,
                            ChatRoom.LastMessage.builder()
                                    .content(expectedMsg).contentType("TEXT")
                                    .senderId("user-1").createdAt(Instant.now()).build());
                } catch (Exception e) { e.printStackTrace(); }
                finally { done.countDown(); }
            });

            executor.submit(() -> {
                try {
                    ready.countDown(); go.await();
                    chatRoomRepositoryCustom.updatePropertySnapshot(
                            roomId, expectedTitle, "새이미지-" + i, null);
                } catch (Exception e) { e.printStackTrace(); }
                finally { done.countDown(); }
            });

            ready.await();
            go.countDown();
            done.await();

            ChatRoom result = chatRoomRepository.findByRoomId(roomId).orElseThrow();
            String actualMsg = result.getLastMessage() != null ? result.getLastMessage().getContent() : "null";
            String actualTitle = result.getPropertyTitle();

            boolean msgLost = !expectedMsg.equals(actualMsg);
            boolean propLost = !expectedTitle.equals(actualTitle);
            List<String> lostFields = new ArrayList<>();
            if (msgLost) { lostFields.add("lastMessage"); msgLostTotal.incrementAndGet(); }
            if (propLost) { lostFields.add("propertyTitle"); propLostTotal.incrementAndGet(); }
            boolean isLost = !lostFields.isEmpty();
            if (isLost) lostTotal.incrementAndGet();

            System.out.printf("  #%d 검증 (roomId=%s)%n", i + 1, roomId);
            System.out.printf("    expected: lastMessage=\"%s\", propertyTitle=\"%s\"%n", expectedMsg, expectedTitle);
            System.out.printf("    actual  : lastMessage=\"%s\", propertyTitle=\"%s\"%n", actualMsg, actualTitle);
            if (isLost) {
                System.out.printf("    → ✗ LOST — %s 유실%n", String.join(", ", lostFields));
            } else {
                System.out.println("    → ✓ OK");
            }
            System.out.println();
        }

        executor.shutdown();

        printSummary("$set", lostTotal.get(), msgLostTotal.get(), propLostTotal.get());

        System.out.println("  💡 Compass에서 확인:");
        System.out.println("     DB: dojangkok_test → Collection: chat_rooms");
        System.out.println("     Filter: { \"roomId\": { \"$regex\": \"^room-set\" } }");
        System.out.println("     → 모든 document의 lastMessage.content과 propertyTitle이");
        System.out.println("       \"새 메시지-N\", \"새 매물-N\" 형태로 정상 반영되어 있어야 함");
        System.out.println();
    }

    // ================================================================
    //  테스트 3: Compass 확인 대기 (10분)
    // ================================================================
    @Test
    @Order(3)
    @DisplayName("[대기] MongoDB Compass로 결과를 확인하세요 (10분 대기)")
    void waitForCompassInspection() throws Exception {
        int waitMinutes = 10;

        System.out.println();
        System.out.println("  ══════════════════════════════════════════════════════");
        System.out.println("  🔍 MongoDB Compass로 결과를 확인하세요!");
        System.out.println("  ══════════════════════════════════════════════════════");
        System.out.println();
        System.out.println("  접속 정보: mongodb://localhost:27017");
        System.out.println("  데이터베이스: dojangkok_test");
        System.out.println("  컬렉션: chat_rooms");
        System.out.println();
        System.out.println("  ▶ 확인 방법:");
        System.out.println();
        System.out.println("  1) save() 결과 확인 — Lost Update가 발생한 document 찾기");
        System.out.println("     Filter: { \"roomId\": { \"$regex\": \"^room-save\" } }");
        System.out.println("     → lastMessage.content이 \"원본 메시지\"로 남아있거나");
        System.out.println("       propertyTitle이 \"원본 매물\"로 남아있는 document 확인");
        System.out.println();
        System.out.println("  2) $set 결과 확인 — 모든 필드가 정상 반영됨");
        System.out.println("     Filter: { \"roomId\": { \"$regex\": \"^room-set\" } }");
        System.out.println("     → 모든 document의 값이 \"새 메시지-N\", \"새 매물-N\"으로 정상 반영");
        System.out.println();
        System.out.println("  3) Lost Update document만 필터링 (가장 직관적)");
        System.out.println("     Filter: { \"propertyTitle\": \"원본 매물\" }");
        System.out.println("     → 이 필터에 걸리는 document = propertyTitle이 유실된 것");
        System.out.println();
        System.out.println("     Filter: { \"lastMessage.content\": \"원본 메시지\" }");
        System.out.println("     → 이 필터에 걸리는 document = lastMessage가 유실된 것");
        System.out.println();
        System.out.printf("  ⏳ %d분간 대기합니다. 확인이 끝나면 Ctrl+C로 종료하세요.%n", waitMinutes);
        System.out.println("  ══════════════════════════════════════════════════════");
        System.out.println();

        for (int remaining = waitMinutes * 60; remaining > 0; remaining--) {
            if (remaining % 60 == 0) {
                System.out.printf("  ⏳ 남은 시간: %d분%n", remaining / 60);
            }
            Thread.sleep(1000);
        }

        System.out.println();
        System.out.println("  ⏰ 대기 시간이 종료되었습니다.");
        System.out.println("  데이터는 dojangkok_test DB에 그대로 남아있습니다.");
        System.out.println("  정리하려면: MongoDB shell에서 use dojangkok_test → db.dropDatabase()");
        System.out.println();
    }

    // ================================================================
    //  로그 출력
    // ================================================================

    private void printSummary(String method, int lostCount, int msgLost, int propLost) {
        double lostRate = (lostCount * 100.0) / ITERATIONS;

        int measuredRead = commandCounter.getReadCount();
        int measuredWrite = commandCounter.getWriteCount();
        int measuredReplace = commandCounter.getReplaceCount();
        int measuredSetUpdate = commandCounter.getSetUpdateCount();

        System.out.println("  ──────────────────────────────────────────────");
        System.out.printf("  [%s] 최종 집계%n", method);
        System.out.println("  ──────────────────────────────────────────────");
        System.out.printf("  총 반복       : %d회%n", ITERATIONS);
        System.out.printf("  Lost Update   : %d건 / %d회 (%.1f%%)%n", lostCount, ITERATIONS, lostRate);
        System.out.println();
        System.out.println("  필드별 유실:");
        System.out.printf("    lastMessage    : %d건%n", msgLost);
        System.out.printf("    propertyTitle  : %d건%n", propLost);
        System.out.println();
        System.out.println("  DB 호출 수 (CommandListener 실측):");
        System.out.printf("    find (READ)         : %d회%n", measuredRead);
        System.out.printf("    update/insert (WRITE): %d회%n", measuredWrite);
        if (measuredReplace > 0) {
            System.out.printf("      └ replaceOne      : %d회 (전체 document 교체)%n", measuredReplace);
        }
        if (measuredSetUpdate > 0) {
            System.out.printf("      └ $set updateOne  : %d회 (변경 필드만 수정)%n", measuredSetUpdate);
        }
        System.out.printf("    합계                : %d회%n", measuredRead + measuredWrite);

        if (lostCount > 0) {
            System.out.println();
            System.out.println("  ⚠️ save()는 read→modify→replaceOne 패턴이므로");
            System.out.println("     동시 수정 시 나중에 save()한 스레드가 먼저 save()한 스레드의 변경을 덮어씁니다.");
        } else {
            System.out.println();
            System.out.println("  ✅ $set은 변경 필드만 원자적으로 수정하므로 동시 수정에서도 유실 0건입니다.");
        }

        System.out.println("  ──────────────────────────────────────────────");
        System.out.println();
    }

    // ================================================================
    //  Helper
    // ================================================================

    private ChatRoom createTestRoom(String roomId, String messageContent, String propertyTitle) {
        return ChatRoom.builder()
                .roomId(roomId)
                .type("DIRECT")
                .participants(List.of("user-1", "user-2"))
                .propertyId("prop-" + roomId)
                .propertyTitle(propertyTitle)
                .propertyImageUrl("img-original")
                .participantProfiles(List.of(
                        ChatRoom.ParticipantInfo.builder()
                                .userId("user-1").nickname("원본닉네임1")
                                .profileImageUrl("profile-1").build(),
                        ChatRoom.ParticipantInfo.builder()
                                .userId("user-2").nickname("원본닉네임2")
                                .profileImageUrl("profile-2").build()
                ))
                .lastMessage(ChatRoom.LastMessage.builder()
                        .content(messageContent).contentType("TEXT")
                        .senderId("user-1").createdAt(Instant.now()).build())
                .createdAt(Instant.now())
                .build();
    }
}
