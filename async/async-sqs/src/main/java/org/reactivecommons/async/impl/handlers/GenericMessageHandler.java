package org.reactivecommons.async.impl.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.reactivecommons.async.impl.HandlerResolver;
import org.reactivecommons.async.impl.model.SNSEventModel;
import reactor.core.publisher.Mono;

@Log
@RequiredArgsConstructor
public abstract class GenericMessageHandler {

  protected final HandlerResolver handlers;
  public abstract Mono handle(SNSEventModel msj);

}


