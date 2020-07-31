package org.reactivecommons.async.impl.listeners;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.reactivecommons.api.domain.Command;
import org.reactivecommons.api.domain.DomainEvent;
import org.reactivecommons.async.api.handlers.registered.RegisteredCommandHandler;
import org.reactivecommons.async.impl.HandlerResolver;
import org.reactivecommons.async.impl.converters.MessageConverter;
import org.reactivecommons.async.impl.model.MessageSQS;
import org.reactivecommons.async.impl.sns.SNSEventModel;
import reactor.core.publisher.Mono;

public class ApplicationCommandListener extends GenericMessageListener {

  private final MessageConverter messageConverter;

  public ApplicationCommandListener(HandlerResolver handlers, MessageConverter messageConverter) {
    super(handlers);
    this.messageConverter = messageConverter;
  }

  private Mono<RegisteredCommandHandler> getHandler(SNSEventModel msj) {
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      Command command = objectMapper.readValue(msj.getMessage(), Command.class);
      String commandName = command.getName();
      RegisteredCommandHandler handler = handlers.getCommandHandler(commandName);
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
              Command<Object> command = messageConverter.readCommand(message, dataClass);
              return handler.getHandler()
                      .handle(command);
            });
  }

}
