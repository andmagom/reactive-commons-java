package org.reactivecommons.async.impl.listeners;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.reactivecommons.api.domain.Command;
import org.reactivecommons.api.domain.DomainEvent;
import org.reactivecommons.async.api.handlers.registered.RegisteredCommandHandler;
import org.reactivecommons.async.api.handlers.registered.RegisteredEventListener;
import org.reactivecommons.async.impl.HandlerResolver;
import org.reactivecommons.async.impl.Handlers;
import org.reactivecommons.async.impl.sns.SNSEventModel;
import reactor.core.publisher.Mono;

public class ApplicationCommandListener extends GenericMessageListener {

  public ApplicationCommandListener(HandlerResolver handlers) {
    super(handlers);
  }

  private Mono<RegisteredCommandHandler> getHandler(SNSEventModel msj) {
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      DomainEvent event = objectMapper.readValue(msj.getMessage(), DomainEvent.class);
      String eventName = event.getName();
      RegisteredCommandHandler<Object> handler = handlers.getCommandHandler(eventName);
      return Mono.just(handler);
    } catch (JsonProcessingException e) {
      return Mono.error(e);
    }
  }

  public Mono handle(SNSEventModel msj) {
    return getHandler(msj)
        .map(handler -> {
          ObjectMapper mapper = new ObjectMapper();
          try {
            JsonNode json = mapper.readTree(msj.getMessage());
            String stringData = json.get("data").asText();
            Class dataClass = handler.getInputClass();
            return handler.getHandler()
                .handle(mapper.readValue(stringData, dataClass));
          } catch (JsonProcessingException e) {
            return Mono.error(e);
          }
        });
  }

}
