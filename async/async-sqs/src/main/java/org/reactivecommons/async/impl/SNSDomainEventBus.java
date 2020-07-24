package org.reactivecommons.async.impl;

import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.reactivecommons.api.domain.DomainEvent;
import org.reactivecommons.api.domain.DomainEventBus;
import org.reactivecommons.async.impl.sns.Sender;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class SNSDomainEventBus implements DomainEventBus {

  private final Sender sender;
  private String topic = "DomainEvents";

  @Override
  public <T> Mono<Void> emit(DomainEvent<T> event) {
    return sender.publish(event, topic)
        .onErrorMap(err -> new RuntimeException("Event send failure: " + event.getName() + " Reason: "+ err.getMessage(), err));
  }

}