package org.reactivecommons.async.impl.listeners;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.java.Log;
import org.reactivecommons.api.domain.DomainEvent;
import org.reactivecommons.async.api.handlers.registered.RegisteredEventListener;
import org.reactivecommons.async.impl.HandlerResolver;
import org.reactivecommons.async.impl.converters.MessageConverter;
import org.reactivecommons.async.impl.model.MessageSQS;
import org.reactivecommons.async.impl.sns.SNSEventModel;
import reactor.core.publisher.Mono;

@Log
public class ApplicationEventListener extends GenericMessageListener {

  private final MessageConverter messageConverter;

  public ApplicationEventListener(HandlerResolver handlers, MessageConverter messageConverter) {
    super(handlers);
    this.messageConverter = messageConverter;
  }

  private Mono<RegisteredEventListener> getHandler(SNSEventModel msj) {
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      DomainEvent event = objectMapper.readValue(msj.getMessage(), DomainEvent.class);
      String eventName = event.getName();
      RegisteredEventListener handler = handlers.getEventListener(eventName);
      return Mono.just(handler);
    } catch (JsonProcessingException e) {
      return Mono.error(e);
    }
  }

  public Mono handle(SNSEventModel msj) {
    return getHandler(msj)
        .flatMap(handler -> {
          Class dataClass = handler.getInputClass();
          MessageSQS message = new MessageSQS(msj.getMessage());
          DomainEvent<Object> domainEvent = messageConverter.readDomainEvent(message, dataClass);
          return handler.getHandler()
              .handle(domainEvent);
        });
  }

}
