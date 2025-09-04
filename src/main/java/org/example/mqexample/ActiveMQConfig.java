package org.example.mqexample;

import jakarta.annotation.PostConstruct;
import jakarta.jms.ConnectionFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQPrefetchPolicy;
import org.apache.activemq.RedeliveryPolicy;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.JmsListenerAnnotationBeanPostProcessor;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ActiveMQConfig {

    private final JmsListenerAnnotationBeanPostProcessor processor;
    private final CachingConnectionFactory jmsConnectionFactory;
    private final BeanFactory beanFactory;

    private final Integer prefetchSizeTopic = 10;

    private final Integer prefetchSizeQueue = 10;

    @PostConstruct
    public void configModify() {
        List<HandlerMethodArgumentResolver> resolvers =
                List.of(new DestinationCaptureArgumentResolver(), new PayloadArgumentResolver());
        DefaultMessageHandlerMethodFactory factory = new DefaultMessageHandlerMethodFactory();
        factory.setBeanFactory(beanFactory);
        factory.setCustomArgumentResolvers(resolvers);
        factory.afterPropertiesSet();
        processor.setMessageHandlerMethodFactory(factory);

        // 일단 retry 안 하게.
        RedeliveryPolicy redeliveryPolicy = new RedeliveryPolicy();
        redeliveryPolicy.setMaximumRedeliveries(0);
        ActiveMQConnectionFactory activeMQConnectionFactory =
                (ActiveMQConnectionFactory) jmsConnectionFactory.getTargetConnectionFactory();
        assert activeMQConnectionFactory != null;
        activeMQConnectionFactory.getRedeliveryPolicy().setMaximumRedeliveries(0);

        // Prefetch 정책을 설정합니다.
        ActiveMQPrefetchPolicy prefetchPolicy = activeMQConnectionFactory.getPrefetchPolicy();
        prefetchPolicy.setQueuePrefetch(prefetchSizeQueue);
        prefetchPolicy.setTopicPrefetch(prefetchSizeTopic);
    }

    @Bean
    public JmsListenerContainerFactory<?> jmsTopicFactory(
            @Qualifier("jmsConnectionFactory") ConnectionFactory activeMQConnectionFactory,
            DefaultJmsListenerContainerFactoryConfigurer configurer) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        configurer.configure(factory, activeMQConnectionFactory);
        factory.setPubSubDomain(true);
        factory.setExceptionListener(ex -> log.error("An exception occurred during processing.", ex));
        factory.setErrorHandler(t -> log.error("An error occurred.", t));
        return factory;
    }
}