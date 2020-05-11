package org.reactivecommons.async.impl;

import lombok.AllArgsConstructor;
import org.reactivecommons.api.domain.Command;
import org.reactivecommons.async.api.AsyncQuery;
import org.reactivecommons.async.api.DirectAsyncGateway;
import org.reactivecommons.async.impl.sns.Sender;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class SNSDirectAsyncGateway implements DirectAsyncGateway {

  private Sender sender;

  @Override
  public <T> Mono<Void> sendCommand(Command<T> command, String targetName) {
    return sender.publish(command, targetName);
  }

  @Override
  public <T, R> Mono<R> requestReply(AsyncQuery<T> query, String targetName, Class<R> type) {
    return null;
  }
}
