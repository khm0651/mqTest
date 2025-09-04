package org.example.mqexample;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
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
@DisplayName("Topic 팬아웃 시나리오 — 동일 메시지를 모든 구독자가 수신")
class TopicFanoutTwoListenersTests {

    @Autowired private JmsTemplate jmsTemplate;
    @Autowired private Server1 server1;
    @Autowired private Server2 server2;

    @Test
    @DisplayName("토픽 1건 발행 → 리스너 2개가 모두 수신해야 한다")
    void case1() throws Exception {
        // given
        TransitionMessage msg = new TransitionMessage();
        msg.setAt(OffsetDateTime.now());
        msg.setAct("USER_INTERACTION");
        msg.setStatus("HOLD:INTERACTION");

        // when: 토픽으로 발행 (패턴 noti.robots.*.transitions 매칭)
        jmsTemplate.send(MQMessage.destination("noti.robots.r1.transitions"), session -> MQMessage.toMqttClientReadableMessage(msg));

        // then: 두 리스너 모두 1건씩 수신
        boolean server1Ok = server1.getLatch().await(5, TimeUnit.SECONDS);
        boolean server2Ok = server2.getLatch().await(5, TimeUnit.SECONDS);

        assertThat(server1Ok).as("[Server1-Topic] should receive").isTrue();
        assertThat(server2Ok).as("[Server2-Topic] should receive").isTrue();

        assertThat(server1.getLastStatus()).isEqualTo("HOLD:INTERACTION");
        assertThat(server2.getLastStatus()).isEqualTo("HOLD:INTERACTION");
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

        @JmsListener(destination = "noti.robots.*.transitions", containerFactory = "jmsTopicFactory")
        public void onMessage(@Payload TransitionMessage msg, @DestinationCapture String id) {
            lastStatus = msg.getStatus();
            log.info("[Server1-Topic] msg = {}, id = {}", msg, id);
            latch.countDown();
        }
    }

    @Slf4j
    static class Server2 {
        @Getter private final CountDownLatch latch = new CountDownLatch(1);
        @Getter private volatile String lastStatus;

        @JmsListener(destination = "noti.robots.*.transitions", containerFactory = "jmsTopicFactory")
        public void onMessage(@Payload TransitionMessage msg, @DestinationCapture String id) {
            lastStatus = msg.getStatus();
            log.info("[Server2-Topic] msg = {}, id = {}", msg, id);
            latch.countDown();
        }
    }
}
