package org.example.mqexample;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.command.ActiveMQTopic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;

import java.time.OffsetDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@DisplayName("VirtualTopic 복제 큐 - 경쟁소비 시나리오")
class VirtualTopicQueueCompetingConsumersTests {

    @Autowired private JmsTemplate jmsTemplate;
    @Autowired private Server1 server1;
    @Autowired private Server2 server2;

    @Test
    @DisplayName("토픽 1건 발행 → VirtualTopic 복제 큐에서 컨슈머 2개 중 정확히 1곳만 수신")
    void case1() throws Exception {
        // given
        TransitionMessage msg = new TransitionMessage();
        msg.setAt(OffsetDateTime.now());
        msg.setAct("USER_INTERACTION");
        msg.setStatus("HOLD:INTERACTION");

        // when: 단일 토픽으로 발행
        jmsTemplate.send(new ActiveMQTopic("noti.robots.r1.transitions"),
                session -> MQMessage.toMqttClientReadableMessage(msg));

        // then: 둘 중 '정확히 1곳'만 수신
        boolean server1Ok = server1.getLatch().await(5, TimeUnit.SECONDS);
        boolean server2Ok = server2.getLatch().await(5, TimeUnit.SECONDS);

        // XOR: 하나만 true 여야 함
        assertThat(server1Ok ^ server2Ok).as("Exactly one listener must receive the message").isTrue();

        if (server1Ok) assertThat(server1.getLastStatus()).isEqualTo("HOLD:INTERACTION");
        if (server2Ok) assertThat(server2.getLastStatus()).isEqualTo("HOLD:INTERACTION");
    }

    @TestConfiguration
    static class ServerConfiguration {
        @Bean
        Server1 server1() { return new Server1(); }

        @Bean
        Server2 server2() { return new Server2(); }
    }

    @Slf4j
    static class Server1 {
        @Getter
        private final CountDownLatch latch = new CountDownLatch(1);
        @Getter private volatile String lastStatus;

        @JmsListener(destination = "consumers.rs.noti.robots.*.transitions")
        public void onMessage(@Payload TransitionMessage msg, @DestinationCapture String id) {
            lastStatus = msg.getStatus();
            log.info("[Server1-Queue] msg = {}, id = {}", msg, id);
            latch.countDown();
        }
    }

    @Slf4j
    static class Server2 {
        @Getter private final CountDownLatch latch = new CountDownLatch(1);
        @Getter private volatile String lastStatus;

        @JmsListener(destination = "consumers.rs.noti.robots.*.transitions")
        public void onMessage(@Payload TransitionMessage msg, @DestinationCapture String id) {
            lastStatus = msg.getStatus();
            log.info("[Server2-Queue] msg = {}, id = {}", msg, id);
            latch.countDown();
        }
    }
}
