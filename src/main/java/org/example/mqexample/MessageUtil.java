package org.example.mqexample;

import org.apache.activemq.command.ActiveMQMessage;
import org.springframework.messaging.Message;

import java.lang.reflect.Field;

class MessageUtil {

    static ActiveMQMessage toActiveMqMessage(Message<?> message) {
        try {
            Field field = message.getClass().getDeclaredField("message");
            field.setAccessible(true);
            return (ActiveMQMessage) field.get(message);
        } catch (Exception e) {
            throw new IllegalArgumentException("Can not convert ActiveMqMessage");
        }
    }

}
