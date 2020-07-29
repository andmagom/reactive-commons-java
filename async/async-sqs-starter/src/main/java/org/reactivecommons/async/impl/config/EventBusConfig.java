package org.reactivecommons.async.impl.config;

import org.reactivecommons.api.domain.DomainEventBus;
import org.reactivecommons.async.impl.SNSDomainEventBus;
import org.reactivecommons.async.impl.config.props.BrokerConfigProps;
import org.reactivecommons.async.impl.sns.communications.Sender;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static reactor.rabbitmq.ExchangeSpecification.exchange;

@Configuration
@Import(AWSConfig.class)
public class EventBusConfig {

    @Bean
    public DomainEventBus domainEventBus(Sender sender, BrokerConfigProps props) {
        final String exchangeName = props.getDomainEventsExchangeName();
        return new SNSDomainEventBus(sender, exchangeName);
    }
}
