package org.reactivecommons.async.impl.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.reactivecommons.async.impl.config.props.AsyncProps;
import org.reactivecommons.async.impl.config.props.BrokerConfigProps;
import org.reactivecommons.async.impl.handlers.ApplicationCommandHandler;
import org.reactivecommons.async.impl.handlers.ApplicationEventHandler;
import org.reactivecommons.async.impl.sns.communications.Listener;
import org.reactivecommons.async.impl.sns.communications.Sender;
import org.reactivecommons.async.impl.sns.communications.TopologyCreator;
import org.springframework.beans.factory.annotation.Value;
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

    @Bean("evtListener")
    public Listener messageEventListener(SqsAsyncClient sqsClient, ApplicationEventHandler appEvtListener,
        BrokerConfigProps props, TopologyCreator topoloy) {
        final Listener listener = new Listener(sqsClient);
        String queueName = props.getEventsQueue();
        topoloy.createQueue(queueName).block();
        String queueUrl = topoloy.getQueueUrl(queueName).block();
        listener.startListener(queueUrl, appEvtListener::handle).subscribe();
        return listener;
    }

    @Bean("commandListener")
    public Listener messageCommandListener(SqsAsyncClient sqsClient, ApplicationCommandHandler appCmdListener, BrokerConfigProps props, TopologyCreator topoloy) {
        final Listener listener = new Listener(sqsClient);
        String queueName = props.getCommandsQueue();
        topoloy.createTopic(props.getAppName()+ props.getDirectMessagesExchangeName()).block();
        topoloy.createQueue(queueName).block();
        String queueUrl = topoloy.getQueueUrl(queueName).block();
        listener.startListener(queueUrl, appCmdListener::handle).subscribe();
        return listener;
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

    @Bean
    public TopologyCreator getTopology(SqsAsyncClient sqsAsyncClient, SnsAsyncClient snsAsyncClient) {
        return new TopologyCreator(snsAsyncClient, sqsAsyncClient,"","");
    }


}
