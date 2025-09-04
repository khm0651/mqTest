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
class SingleServerTopicAndVirtualQueueTests {

    @Autowired private JmsTemplate jmsTemplate;
    @Autowired private Server1 server1;

    @Test
    @DisplayName("단일 서버에서 토픽으로 발행한 1건을 Topic 리스너와 VirtualTopic 큐 리스너가 모두 수신해야 한다")
    void case1() throws Exception {
        // given
        TransitionMessage msg = new TransitionMessage();
        msg.setAt(OffsetDateTime.now());
        msg.setAct("USER_INTERACTION");
        msg.setStatus("HOLD:INTERACTION");

        // when: 단일 토픽으로 발행
        jmsTemplate.send(new ActiveMQTopic("noti.robots.r1.transitions"),
                session -> MQMessage.toMqttClientReadableMessage(msg));

        // then: 두 리스너 모두 1건씩 수신
        boolean server1QueueOk = server1.getQueueLatch().await(5, TimeUnit.SECONDS);
        boolean server1TopicOk = server1.getTopicLatch().await(5, TimeUnit.SECONDS);

        assertThat(server1QueueOk).as("[Server1-Queue] should receive").isTrue();
        assertThat(server1TopicOk).as("[Server1-Topic] should receive").isTrue();

        assertThat(server1.getQueueLastStatus()).isEqualTo("HOLD:INTERACTION");
        assertThat(server1.getTopicLastStatus()).isEqualTo("HOLD:INTERACTION");
    }

    @TestConfiguration
    static class ServerConfiguration {
        @Bean
        Server1 server1() { return new Server1(); }
    }

    @Slf4j
    static class Server1 {
        @Getter
        private final CountDownLatch queueLatch = new CountDownLatch(1);
        @Getter private volatile String queueLastStatus;

        @Getter private final CountDownLatch topicLatch = new CountDownLatch(1);
        @Getter private volatile String topicLastStatus;

        @JmsListener(destination = "consumers.rs.noti.robots.*.transitions")
        public void onMessageQueue(@Payload TransitionMessage msg, @DestinationCapture String id) {
            queueLastStatus = msg.getStatus();
            log.info("[Server1-Queue] msg = {}, id = {}", msg, id);
            queueLatch.countDown();
        }

        @JmsListener(destination = "noti.robots.*.transitions", containerFactory = "jmsTopicFactory")
        public void onMessageTopic(@Payload TransitionMessage msg, @DestinationCapture String id) {
            topicLastStatus = msg.getStatus();
            log.info("[Server1-Topic] msg = {}, id = {}", msg, id);
            topicLatch.countDown();
        }
    }
}
