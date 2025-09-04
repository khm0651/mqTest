package org.example.mqexample;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.command.ActiveMQBytesMessage;
import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.util.ByteSequence;
import org.springframework.core.MethodParameter;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;

import jakarta.validation.*;
import java.util.Set;

@Slf4j
public class PayloadArgumentResolver implements HandlerMethodArgumentResolver {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public PayloadArgumentResolver() {
        this.objectMapper = ObjectMapperHolder.get();
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }


    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasMethodAnnotation(JmsListener.class) && parameter.hasParameterAnnotation(Payload.class);
    }

    @SneakyThrows
    @Override
    public Object resolveArgument(MethodParameter parameter, Message<?> message) {
        ActiveMQMessage activeMQMessage = toActiveMqMessage(message);
        Class<?> parameterType = parameter.getParameterType();

        if (activeMQMessage instanceof ActiveMQBytesMessage) {
            ActiveMQBytesMessage bytesMessage = (ActiveMQBytesMessage) activeMQMessage;
            ByteSequence content = bytesMessage.getContent();

            Object object = objectMapper.readValue(content.getData(), parameterType);
            Set<ConstraintViolation<Object>> violations = validator.validate(object);
            for (ConstraintViolation<Object> violation : violations) {
                log.error(violation.getMessage());
            }
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException("invalid.", violations);
            }
            return object;
        } else {
            log.error("not expected message type: {}", activeMQMessage.getClass());
            throw new MessageConversionException("not supported message type");
        }
    }

    private ActiveMQMessage toActiveMqMessage(Message<?> message) {
        return MessageUtil.toActiveMqMessage(message);
    }
}
