package com.microsoft.conference.common.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.enodeframework.kafka.KafkaMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.adapter.RetryingMessageListenerAdapter;
import org.springframework.retry.support.RetryTemplate;

import java.util.HashMap;
import java.util.Map;

import static com.microsoft.conference.common.QueueProperties.DEFAULT_CONSUMER_GROUP0;
import static com.microsoft.conference.common.QueueProperties.KAFKA_SERVER;

@Configuration
public class KafkaEventConfig {

    @Value("${spring.enode.mq.topic.event}")
    private String eventTopic;

    @Value("${spring.enode.mq.topic.command}")
    private String commandTopic;

    @Value("${spring.enode.mq.topic.application}")
    private String applicationTopic;

    @Value("${spring.enode.mq.topic.exception}")
    private String exceptionTopic;

    @Autowired
    @Qualifier("kafkaCommandListener")
    private KafkaMessageListener kafkaCommandListener;

    @Autowired
    @Qualifier("kafkaDomainEventListener")
    private KafkaMessageListener kafkaDomainEventListener;

    @Autowired
    @Qualifier("kafkaApplicationMessageListener")
    private KafkaMessageListener kafkaApplicationMessageListener;

    @Autowired
    @Qualifier("kafkaPublishableExceptionListener")
    private KafkaMessageListener kafkaPublishableExceptionListener;

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_SERVER);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, DEFAULT_CONSUMER_GROUP0);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "100");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "15000");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public RetryTemplate retryTemplate() {
        return new RetryTemplate();
    }

    @Bean
    public KafkaMessageListenerContainer<String, String> domainEventListenerContainer(RetryTemplate retryTemplate) {
        ContainerProperties properties = new ContainerProperties(eventTopic);
        properties.setGroupId(DEFAULT_CONSUMER_GROUP0);
        RetryingMessageListenerAdapter<String, String> listenerAdapter = new RetryingMessageListenerAdapter<>(kafkaDomainEventListener, retryTemplate);
        properties.setMessageListener(listenerAdapter);
        properties.setMissingTopicsFatal(false);
        properties.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return new KafkaMessageListenerContainer<>(consumerFactory(), properties);
    }

    @Bean
    public KafkaMessageListenerContainer<String, String> commandListenerContainer(RetryTemplate retryTemplate) {
        ContainerProperties properties = new ContainerProperties(commandTopic);
        properties.setGroupId(DEFAULT_CONSUMER_GROUP0);
        RetryingMessageListenerAdapter<String, String> listenerAdapter = new RetryingMessageListenerAdapter<>(kafkaCommandListener, retryTemplate);
        properties.setMessageListener(listenerAdapter);
        properties.setMissingTopicsFatal(false);
        properties.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return new KafkaMessageListenerContainer<>(consumerFactory(), properties);
    }

    @Bean
    public KafkaMessageListenerContainer<String, String> applicationMessageListenerContainer(RetryTemplate retryTemplate) {
        ContainerProperties properties = new ContainerProperties(applicationTopic);
        properties.setGroupId(DEFAULT_CONSUMER_GROUP0);
        RetryingMessageListenerAdapter<String, String> listenerAdapter = new RetryingMessageListenerAdapter<>(kafkaApplicationMessageListener, retryTemplate);
        properties.setMessageListener(listenerAdapter);
        properties.setMissingTopicsFatal(false);
        properties.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return new KafkaMessageListenerContainer<>(consumerFactory(), properties);
    }

    @Bean
    public KafkaMessageListenerContainer<String, String> publishableExceptionListenerContainer(RetryTemplate retryTemplate) {
        ContainerProperties properties = new ContainerProperties(exceptionTopic);
        properties.setGroupId(DEFAULT_CONSUMER_GROUP0);
        RetryingMessageListenerAdapter<String, String> listenerAdapter = new RetryingMessageListenerAdapter<>(kafkaPublishableExceptionListener, retryTemplate);
        properties.setMessageListener(listenerAdapter);
        properties.setMissingTopicsFatal(false);
        properties.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return new KafkaMessageListenerContainer<>(consumerFactory(), properties);
    }
}
