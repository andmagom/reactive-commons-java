package org.reactivecommons.async.impl.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.reactivecommons.api.domain.DomainEventBus;
import org.reactivecommons.async.impl.SNSDomainEventBus;
import org.reactivecommons.async.impl.config.props.AsyncProps;
import org.reactivecommons.async.impl.config.props.BrokerConfigProps;
import org.reactivecommons.async.impl.converters.MessageConverter;
import org.reactivecommons.async.impl.converters.json.JacksonMessageConverter;
import org.reactivecommons.async.impl.listeners.ApplicationCommandListener;
import org.reactivecommons.async.impl.listeners.ApplicationEventListener;
import org.reactivecommons.async.impl.sns.Listener;
import org.reactivecommons.async.impl.sns.communications.Sender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;


@Log
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({
        AWSProperties.class,
        AsyncProps.class
})
@Import({BrokerConfigProps.class, MessageListenersConfig.class})
public class AWSConfig {

    private final AsyncProps asyncProps;

    @Value("${spring.application.name}")
    private String appName;

    @Bean
    public Sender messageSender(SnsAsyncClient client, AWSProperties awsProperties) {
        final Sender sender = new Sender(client, appName, awsProperties.getPrefixARN() );
        return sender;
    }

    @Bean("eventListener")
    public Listener messageEventListener(SqsAsyncClient sqsClient, ApplicationEventListener appEvtListener) {
        final Listener listener = new Listener(sqsClient);
        listener.startListener("queueCommand", appEvtListener::handle);
        return listener;
    }

    @Bean("commandListener")
    public Listener messageCommandListener(SqsAsyncClient sqsClient, ApplicationCommandListener appCmdListener) {
        final Listener listener = new Listener(sqsClient);
        listener.startListener("queueCommand", appCmdListener::handle);
        return listener;
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageConverter messageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        return new JacksonMessageConverter(mapper);
    }


    private DomainEventBus domainEventBus(Sender sender, BrokerConfigProps props) {
        final String exchangeName = props.getDomainEventsExchangeName();
        return new SNSDomainEventBus(sender, exchangeName);
    }

    @Bean
    public SqsAsyncClient getSQSAsyncClient(AWSProperties awsProperties) {
        Region region = Region.of(awsProperties.getRegion());
        return SqsAsyncClient.builder()
            .region(region)
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
    }

    @Bean
    public SnsAsyncClient getSNSAsyncClient(AWSProperties awsProperties) {
        Region region = Region.of(awsProperties.getRegion());
        return SnsAsyncClient.builder()
            .region(region)
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
    }


}
