package com.dojangkok.chat.repository;

import com.dojangkok.chat.domain.ChatRoom;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 스냅샷 업데이트 Lost Update 문제 정량 측정 테스트.
 *
 * <h2>목적</h2>
 * save()(전체 document 교체)와 $set(부분 필드 업데이트)의
 * 동시성 안전성 차이를 정량적으로 비교한다.
 *
 * <h2>테스트 시나리오</h2>
 * 하나의 ChatRoom document에 대해 두 스레드가 동시에
 * 서로 다른 필드를 업데이트한다:
 * <ul>
 *   <li>Thread A: lastMessage 필드 업데이트</li>
 *   <li>Thread B: propertyTitle + propertyImageUrl 필드 업데이트</li>
 * </ul>
 * 이를 N회 반복하여 Lost Update 발생 횟수를 측정한다.
 *
 * <h2>실행 방법</h2>
 * <pre>
 * ./gradlew test --tests "com.dojangkok.chat.repository.SnapshotLostUpdateTest"
 * </pre>
 * Docker가 실행 중이어야 합니다 (Testcontainers가 MongoDB 컨테이너를 자동 생성).
 */
@DataMongoTest
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SnapshotLostUpdateTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private ChatRoomRepositoryCustomImpl chatRoomRepositoryCustom;

    private static final int ITERATIONS = 100;

    @BeforeEach
    void setUp() {
        chatRoomRepositoryCustom = new ChatRoomRepositoryCustomImpl(mongoTemplate);
        chatRoomRepository.deleteAll();
    }

    // ==========================================
    // 테스트 1: save() 방식 — Lost Update 발생률 측정
    // ==========================================
    @Test
    @Order(1)
    @DisplayName("[save() 방식] 동시 업데이트 시 Lost Update 발생률 측정")
    void testSaveMethod_LostUpdateRate() throws Exception {
        AtomicInteger lostUpdateCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        for (int i = 0; i < ITERATIONS; i++) {
            // 1. 초기 상태 세팅
            String roomId = "room-save-" + i;
            ChatRoom room = createTestRoom(roomId, "원본 메시지", "원본 매물");
            chatRoomRepository.save(room);

            String expectedMessage = "새 메시지-" + i;
            String expectedTitle = "새 매물-" + i;
            String expectedImageUrl = "새 이미지-" + i;

            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch go = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(2);

            // 2. Thread A: lastMessage 업데이트 (save 방식)
            executor.submit(() -> {
                try {
                    ready.countDown();
                    go.await(); // 동시 시작 보장
                    ChatRoom r = chatRoomRepository.findByRoomId(roomId).orElseThrow();
                    r.updateLastMessage(ChatRoom.LastMessage.builder()
                            .content(expectedMessage)
                            .contentType("TEXT")
                            .senderId("user-1")
                            .createdAt(Instant.now())
                            .build());
                    chatRoomRepository.save(r);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    done.countDown();
                }
            });

            // 3. Thread B: propertySnapshot 업데이트 (save 방식)
            executor.submit(() -> {
                try {
                    ready.countDown();
                    go.await(); // 동시 시작 보장
                    ChatRoom r = chatRoomRepository.findByRoomId(roomId).orElseThrow();
                    r.updatePropertySnapshot(expectedTitle, expectedImageUrl);
                    chatRoomRepository.save(r);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    done.countDown();
                }
            });

            // 4. 동시 시작 → 완료 대기
            ready.await();
            go.countDown();
            done.await();

            // 5. 검증: 두 필드 모두 최신 값인지 확인
            ChatRoom result = chatRoomRepository.findByRoomId(roomId).orElseThrow();

            boolean lastMessageLost = result.getLastMessage() == null
                    || !expectedMessage.equals(result.getLastMessage().getContent());
            boolean propertyLost = !expectedTitle.equals(result.getPropertyTitle());

            if (lastMessageLost || propertyLost) {
                lostUpdateCount.incrementAndGet();
            }
        }

        executor.shutdown();

        int lostCount = lostUpdateCount.get();
        double lostRate = (lostCount * 100.0) / ITERATIONS;

        System.out.println("==============================================");
        System.out.println("[save() 방식] Lost Update 측정 결과");
        System.out.println("==============================================");
        System.out.println("총 반복 횟수  : " + ITERATIONS);
        System.out.println("Lost Update  : " + lostCount + "건");
        System.out.printf("발생률        : %.1f%%\n", lostRate);
        System.out.println("==============================================");

        // save() 방식은 Lost Update가 발생할 것으로 예상
        // (CI에서 깨지지 않도록 assertion은 로그 확인용으로만 둠)
        if (lostCount > 0) {
            System.out.println("⚠️ Lost Update가 " + lostCount + "건 발생했습니다.");
            System.out.println("  → save()는 전체 document를 교체(replaceOne)하므로");
            System.out.println("    동시 수정 시 다른 필드의 변경이 유실됩니다.");
        }
    }

    // ==========================================
    // 테스트 2: $set 방식 — Lost Update 발생률 측정
    // ==========================================
    @Test
    @Order(2)
    @DisplayName("[$set 방식] 동시 업데이트 시 Lost Update 발생률 측정")
    void testSetMethod_LostUpdateRate() throws Exception {
        AtomicInteger lostUpdateCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        for (int i = 0; i < ITERATIONS; i++) {
            // 1. 초기 상태 세팅
            String roomId = "room-set-" + i;
            ChatRoom room = createTestRoom(roomId, "원본 메시지", "원본 매물");
            chatRoomRepository.save(room);

            String expectedMessage = "새 메시지-" + i;
            String expectedTitle = "새 매물-" + i;
            String expectedImageUrl = "새 이미지-" + i;

            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch go = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(2);

            // 2. Thread A: lastMessage 업데이트 ($set 방식)
            executor.submit(() -> {
                try {
                    ready.countDown();
                    go.await();
                    chatRoomRepositoryCustom.updateLastMessage(roomId,
                            ChatRoom.LastMessage.builder()
                                    .content(expectedMessage)
                                    .contentType("TEXT")
                                    .senderId("user-1")
                                    .createdAt(Instant.now())
                                    .build());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    done.countDown();
                }
            });

            // 3. Thread B: propertySnapshot 업데이트 ($set 방식)
            executor.submit(() -> {
                try {
                    ready.countDown();
                    go.await();
                    chatRoomRepositoryCustom.updatePropertySnapshot(
                            roomId, expectedTitle, expectedImageUrl, null);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    done.countDown();
                }
            });

            // 4. 동시 시작 → 완료 대기
            ready.await();
            go.countDown();
            done.await();

            // 5. 검증
            ChatRoom result = chatRoomRepository.findByRoomId(roomId).orElseThrow();

            boolean lastMessageLost = result.getLastMessage() == null
                    || !expectedMessage.equals(result.getLastMessage().getContent());
            boolean propertyLost = !expectedTitle.equals(result.getPropertyTitle());

            if (lastMessageLost || propertyLost) {
                lostUpdateCount.incrementAndGet();
            }
        }

        executor.shutdown();

        int lostCount = lostUpdateCount.get();
        double lostRate = (lostCount * 100.0) / ITERATIONS;

        System.out.println("==============================================");
        System.out.println("[$set 방식] Lost Update 측정 결과");
        System.out.println("==============================================");
        System.out.println("총 반복 횟수  : " + ITERATIONS);
        System.out.println("Lost Update  : " + lostCount + "건");
        System.out.printf("발생률        : %.1f%%\n", lostRate);
        System.out.println("==============================================");

        // $set 방식은 Lost Update가 0건이어야 함
        assertThat(lostCount)
                .as("$set 방식은 서로 다른 필드를 독립적으로 업데이트하므로 Lost Update가 0건이어야 합니다.")
                .isEqualTo(0);

        System.out.println("✅ Lost Update 0건 — $set 방식은 동시성 안전합니다.");
    }

    // ==========================================
    // 테스트 3: 3-way 동시 업데이트 (더 극단적인 시나리오)
    // ==========================================
    @Test
    @Order(3)
    @DisplayName("[save() 방식] 3개 스레드 동시 업데이트 — 더 높은 Lost Update율")
    void testSaveMethod_ThreeWayConcurrency() throws Exception {
        AtomicInteger lostUpdateCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(3);

        for (int i = 0; i < ITERATIONS; i++) {
            String roomId = "room-3way-" + i;
            ChatRoom room = createTestRoom(roomId, "원본", "원본 매물");
            chatRoomRepository.save(room);

            String expectedMessage = "msg-" + i;
            String expectedTitle = "title-" + i;
            String expectedNickname = "닉네임-" + i;

            CountDownLatch ready = new CountDownLatch(3);
            CountDownLatch go = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(3);

            // Thread A: lastMessage
            executor.submit(() -> {
                try {
                    ready.countDown();
                    go.await();
                    ChatRoom r = chatRoomRepository.findByRoomId(roomId).orElseThrow();
                    r.updateLastMessage(ChatRoom.LastMessage.builder()
                            .content(expectedMessage).contentType("TEXT")
                            .senderId("user-1").createdAt(Instant.now()).build());
                    chatRoomRepository.save(r);
                } catch (Exception e) { e.printStackTrace(); }
                finally { done.countDown(); }
            });

            // Thread B: propertySnapshot
            executor.submit(() -> {
                try {
                    ready.countDown();
                    go.await();
                    ChatRoom r = chatRoomRepository.findByRoomId(roomId).orElseThrow();
                    r.updatePropertySnapshot(expectedTitle, "img-" + roomId);
                    chatRoomRepository.save(r);
                } catch (Exception e) { e.printStackTrace(); }
                finally { done.countDown(); }
            });

            // Thread C: participantProfiles
            executor.submit(() -> {
                try {
                    ready.countDown();
                    go.await();
                    ChatRoom r = chatRoomRepository.findByRoomId(roomId).orElseThrow();
                    r.updateParticipantProfiles(List.of(
                            ChatRoom.ParticipantInfo.builder()
                                    .userId("user-1").nickname(expectedNickname)
                                    .profileImageUrl("profile-1").build(),
                            ChatRoom.ParticipantInfo.builder()
                                    .userId("user-2").nickname("상대방")
                                    .profileImageUrl("profile-2").build()
                    ));
                    chatRoomRepository.save(r);
                } catch (Exception e) { e.printStackTrace(); }
                finally { done.countDown(); }
            });

            ready.await();
            go.countDown();
            done.await();

            ChatRoom result = chatRoomRepository.findByRoomId(roomId).orElseThrow();

            boolean msgLost = result.getLastMessage() == null
                    || !expectedMessage.equals(result.getLastMessage().getContent());
            boolean propLost = !expectedTitle.equals(result.getPropertyTitle());
            boolean profileLost = result.getParticipantProfiles().stream()
                    .noneMatch(p -> expectedNickname.equals(p.getNickname()));

            if (msgLost || propLost || profileLost) {
                lostUpdateCount.incrementAndGet();
            }
        }

        executor.shutdown();

        int lostCount = lostUpdateCount.get();
        double lostRate = (lostCount * 100.0) / ITERATIONS;

        System.out.println("==============================================");
        System.out.println("[save() 3-way] Lost Update 측정 결과");
        System.out.println("==============================================");
        System.out.println("총 반복 횟수  : " + ITERATIONS);
        System.out.println("Lost Update  : " + lostCount + "건");
        System.out.printf("발생률        : %.1f%%\n", lostRate);
        System.out.println("==============================================");

        if (lostCount > 0) {
            System.out.println("⚠️ 3개 스레드 동시 save()에서 Lost Update " + lostCount + "건 발생");
        }
    }

    // ==========================================
    // 테스트 4: $set 3-way 동시 업데이트 — 0건 보장
    // ==========================================
    @Test
    @Order(4)
    @DisplayName("[$set 방식] 3개 스레드 동시 업데이트 — Lost Update 0건 보장")
    void testSetMethod_ThreeWayConcurrency() throws Exception {
        AtomicInteger lostUpdateCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(3);

        for (int i = 0; i < ITERATIONS; i++) {
            String roomId = "room-3way-set-" + i;
            ChatRoom room = createTestRoom(roomId, "원본", "원본 매물");
            chatRoomRepository.save(room);

            String expectedMessage = "msg-" + i;
            String expectedTitle = "title-" + i;
            String expectedNickname = "닉네임-" + i;

            CountDownLatch ready = new CountDownLatch(3);
            CountDownLatch go = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(3);

            // Thread A: lastMessage ($set)
            executor.submit(() -> {
                try {
                    ready.countDown();
                    go.await();
                    chatRoomRepositoryCustom.updateLastMessage(roomId,
                            ChatRoom.LastMessage.builder()
                                    .content(expectedMessage).contentType("TEXT")
                                    .senderId("user-1").createdAt(Instant.now()).build());
                } catch (Exception e) { e.printStackTrace(); }
                finally { done.countDown(); }
            });

            // Thread B: propertySnapshot ($set)
            executor.submit(() -> {
                try {
                    ready.countDown();
                    go.await();
                    chatRoomRepositoryCustom.updatePropertySnapshot(
                            roomId, expectedTitle, "img-" + roomId, null);
                } catch (Exception e) { e.printStackTrace(); }
                finally { done.countDown(); }
            });

            // Thread C: participantProfiles ($set)
            executor.submit(() -> {
                try {
                    ready.countDown();
                    go.await();
                    chatRoomRepositoryCustom.updateParticipantProfiles(roomId, List.of(
                            ChatRoom.ParticipantInfo.builder()
                                    .userId("user-1").nickname(expectedNickname)
                                    .profileImageUrl("profile-1").build(),
                            ChatRoom.ParticipantInfo.builder()
                                    .userId("user-2").nickname("상대방")
                                    .profileImageUrl("profile-2").build()
                    ), null);
                } catch (Exception e) { e.printStackTrace(); }
                finally { done.countDown(); }
            });

            ready.await();
            go.countDown();
            done.await();

            ChatRoom result = chatRoomRepository.findByRoomId(roomId).orElseThrow();

            boolean msgLost = result.getLastMessage() == null
                    || !expectedMessage.equals(result.getLastMessage().getContent());
            boolean propLost = !expectedTitle.equals(result.getPropertyTitle());
            boolean profileLost = result.getParticipantProfiles().stream()
                    .noneMatch(p -> expectedNickname.equals(p.getNickname()));

            if (msgLost || propLost || profileLost) {
                lostUpdateCount.incrementAndGet();
            }
        }

        executor.shutdown();

        int lostCount = lostUpdateCount.get();
        double lostRate = (lostCount * 100.0) / ITERATIONS;

        System.out.println("==============================================");
        System.out.println("[$set 3-way] Lost Update 측정 결과");
        System.out.println("==============================================");
        System.out.println("총 반복 횟수  : " + ITERATIONS);
        System.out.println("Lost Update  : " + lostCount + "건");
        System.out.printf("발생률        : %.1f%%\n", lostRate);
        System.out.println("==============================================");

        assertThat(lostCount)
                .as("$set 3-way 동시 업데이트에서도 Lost Update가 0건이어야 합니다.")
                .isEqualTo(0);

        System.out.println("✅ 3-way 동시 업데이트에서도 Lost Update 0건 — $set 방식 안전합니다.");
    }

    // ==========================================
    // Helper
    // ==========================================
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
