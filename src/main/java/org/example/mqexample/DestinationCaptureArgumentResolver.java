package org.example.mqexample;

import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.command.ActiveMQMessage;
import org.springframework.core.MethodParameter;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public class DestinationCaptureArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasMethodAnnotation(JmsListener.class) && parameter.hasParameterAnnotation(DestinationCapture.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, Message<?> message) {
        ActiveMQMessage activeMQMessage = toActiveMqMessage(message);

        JmsListener jmsListener = parameter.getMethodAnnotation(JmsListener.class);
        String listenerDestination = Objects.requireNonNull(jmsListener).destination();
        String physicalDestination = getPhysicalDestination(activeMQMessage);

        // destination 의 경로 길이를 originalDestination 의 경로 길이와 맞춘다.
        // // 더 길다는 거는 virtual topic consumer 를 나타내는 prefix 일 것. 그것을 잘라내는게 목적.
        // // '.'을 세면 길이를 알 수 있다.
        String[] physicalDestinationPaths = physicalDestination.split("\\.");
        int physicalDestinationPathsSize = physicalDestinationPaths.length;

        String[] listenerDestinationPaths = listenerDestination.split("\\.");
        int listenerDestinationPathsSize = listenerDestinationPaths.length;
        int different = listenerDestinationPathsSize - physicalDestinationPathsSize;
        List<String> pathVariableList = new ArrayList<>();
        for (int i = 0; i < physicalDestinationPathsSize; i++) {
            if (!physicalDestinationPaths[i].equals(listenerDestinationPaths[i + different])) {
                if (!listenerDestinationPaths[i + different].equals("*")) {
                    log.error("failure to assume for resolving argument. physicalDestination: {}, listenerDestination: {}", physicalDestination, listenerDestination);
                    throw new RuntimeException("failure to assume for resolving argument.");
                }
                pathVariableList.add(physicalDestinationPaths[i]);
            }
        }

        DestinationCapture parameterAnnotation = parameter.getParameterAnnotation(DestinationCapture.class);
        assert parameterAnnotation != null;
        int index = parameterAnnotation.value();
        try {
            return pathVariableList.get(parameterAnnotation.value());
        } catch (IndexOutOfBoundsException e) {
            log.error("captured {} path(s) but required the path of {} index.", pathVariableList.size(), index, e);
            throw new MessageConversionException("failure to capture because invalid index.");
        }
    }

    private String getPhysicalDestination(ActiveMQMessage activeMQMessage) {
        ActiveMQDestination destination = activeMQMessage.getDestination();
        if (destination.isQueue()) {
            return activeMQMessage.getOriginalDestination().getPhysicalName();
        }
        return destination.getPhysicalName();
    }

    private ActiveMQMessage toActiveMqMessage(Message<?> message) {
        return MessageUtil.toActiveMqMessage(message);
    }
}
