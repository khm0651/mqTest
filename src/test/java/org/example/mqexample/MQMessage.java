package org.example.mqexample;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.command.ActiveMQBytesMessage;
import org.apache.activemq.command.ActiveMQTopic;

@Slf4j
class MQMessage {
    private final static int DEFAULT_QOS = 1;
    private final static boolean DEFAULT_RETAINED = false;

    static Destination destination(String topic) {
        return new ActiveMQTopic(topic);
    }

    static Message toMqttClientReadableMessage(Object object) {
        ActiveMQBytesMessage activeMQBytesMessage = new ActiveMQBytesMessage();
        try {
            activeMQBytesMessage.setIntProperty("ActiveMQ.MQTT.QoS", DEFAULT_QOS);
            activeMQBytesMessage.setBooleanProperty("ActiveMQ.Retain", DEFAULT_RETAINED);
            activeMQBytesMessage.writeBytes(ObjectMapperHolder.get().writeValueAsBytes(object));
        } catch (JMSException | JsonProcessingException e) {
            log.error("failure to convert mqtt message.", e);
        }

        return activeMQBytesMessage;
    }
}
