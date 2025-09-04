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
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;

import java.time.OffsetDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
class MqQueueTests {

    @Autowired private JmsTemplate jmsTemplate;
    @Autowired private QueueServer1Listener server1;
    @Autowired private QueueServer2Listener server2;

    @Test
    @DisplayName("jmsTopicFactory로 설정하여 토픽으로 한경우 모든 서버를 같은 토픽 메시지를 받아야한다.")
    void onlyOneServerGetsIt_onSharedTopicSubscription() throws Exception {
        // given
        TransitionMessage msg = new TransitionMessage();
        msg.setAt(OffsetDateTime.now());
        msg.setAct("USER_INTERACTION");
        msg.setStatus("HOLD:INTERACTION");

        // when: 단일 토픽으로 발행
        jmsTemplate.send(new ActiveMQTopic("noti.robots.r1.transitions"),
                session -> MQMessage.toMqttClientReadableMessage(msg));

        // then: 둘 중 '정확히 1곳'만 수신
        boolean aOk = server1.getLatch().await(5, TimeUnit.SECONDS);
        boolean bOk = server2.getLatch().await(5, TimeUnit.SECONDS);

        // XOR: 하나만 true 여야 함
        assertThat(aOk ^ bOk).as("Exactly one listener must receive the message").isTrue();

        if (aOk) assertThat(server1.getLastStatus()).isEqualTo("HOLD:INTERACTION");
        if (bOk) assertThat(server2.getLastStatus()).isEqualTo("HOLD:INTERACTION");
    }

    @TestConfiguration
    static class QueueServer {
        @Bean
        QueueServer1Listener queueServer1Listener() { return new QueueServer1Listener(); }

        @Bean
        QueueServer2Listener queueServer2Listener() { return new QueueServer2Listener(); }
    }

    @Slf4j
    static class QueueServer1Listener {
        @Getter
        private final CountDownLatch latch = new CountDownLatch(1);
        @Getter private volatile String lastStatus;

        @JmsListener(destination = "consumers.rs.noti.robots.*.transitions")
        public void onMessage(@Payload TransitionMessage msg, @DestinationCapture String id) {
            lastStatus = msg.getStatus();
            log.info("[QueueServer1] msg = {}, id = {}", msg, id);
            latch.countDown();
        }
    }

    @Slf4j
    static class QueueServer2Listener {
        @Getter private final CountDownLatch latch = new CountDownLatch(1);
        @Getter private volatile String lastStatus;

        @JmsListener(destination = "consumers.rs.noti.robots.*.transitions")
        public void onMessage(@Payload TransitionMessage msg, @DestinationCapture String id) {
            lastStatus = msg.getStatus();
            log.info("[QueueServer2] msg = {}, id = {}", msg, id);
            latch.countDown();
        }
    }
}
