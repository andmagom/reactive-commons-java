package org.reactivecommons.async.impl.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.reactivecommons.async.impl.HandlerResolver;
import org.reactivecommons.async.impl.sns.SNSEventModel;
import reactor.core.publisher.Mono;

@Log
@RequiredArgsConstructor
public abstract class GenericMessageListener {

  protected final HandlerResolver handlers;
  public abstract Mono handle(SNSEventModel msj);

}


