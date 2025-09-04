package org.example.mqexample;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
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
class MqTopicTests {

    @Autowired private JmsTemplate jmsTemplate;
    @Autowired private TopicServer1Listener server1;
    @Autowired private TopicServer2Listener server2;

    @Test
    @DisplayName("jmsTopicFactory로 설정하여 토픽으로 한경우 모든 서버를 같은 토픽 메시지를 받아야한다.")
    void bothServersReceiveSameTopicMessage() throws Exception {
        // given
        TransitionMessage msg = new TransitionMessage();
        msg.setAt(OffsetDateTime.now());
        msg.setAct("USER_INTERACTION");
        msg.setStatus("HOLD:INTERACTION");

        // when: 토픽으로 발행 (패턴 noti.robots.*.transitions 매칭)
        jmsTemplate.send(MQMessage.destination("noti.robots.r1.transitions"), session -> MQMessage.toMqttClientReadableMessage(msg));

        // then: 두 리스너 모두 1건씩 수신
        boolean aOk = server1.getLatch().await(5, TimeUnit.SECONDS);
        boolean bOk = server2.getLatch().await(5, TimeUnit.SECONDS);

        assertThat(aOk).as("ServerA should receive").isTrue();
        assertThat(bOk).as("ServerB should receive").isTrue();

        assertThat(server1.getLastStatus()).isEqualTo("HOLD:INTERACTION");
        assertThat(server2.getLastStatus()).isEqualTo("HOLD:INTERACTION");
    }

    @TestConfiguration
    static class TopicServer {
        @Bean
        TopicServer1Listener topicServer1Listener() { return new TopicServer1Listener(); }

        @Bean
        TopicServer2Listener topicServer2Listener() { return new TopicServer2Listener(); }
    }

    @Slf4j
    static class TopicServer1Listener {
        @Getter
        private final CountDownLatch latch = new CountDownLatch(1);
        @Getter private volatile String lastStatus;

        @JmsListener(destination = "noti.robots.*.transitions", containerFactory = "jmsTopicFactory")
        public void onMessage(@Payload TransitionMessage msg, @DestinationCapture String id) {
            lastStatus = msg.getStatus();
            log.info("[TopicServer1] msg = {}, id = {}", msg, id);
            latch.countDown();
        }
    }

    @Slf4j
    static class TopicServer2Listener {
        @Getter private final CountDownLatch latch = new CountDownLatch(1);
        @Getter private volatile String lastStatus;

        @JmsListener(destination = "noti.robots.*.transitions", containerFactory = "jmsTopicFactory")
        public void onMessage(@Payload TransitionMessage msg, @DestinationCapture String id) {
            lastStatus = msg.getStatus();
            log.info("[TopicServer2] msg = {}, id = {}", msg, id);
            latch.countDown();
        }
    }
}
