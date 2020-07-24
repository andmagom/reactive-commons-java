package org.reactivecommons.async.impl.sns;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.reactivecommons.async.api.handlers.GenericHandler;
import org.reactivecommons.async.impl.sns.config.SNSProps;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@RequiredArgsConstructor
@Log
public class Listener {

  private final SqsAsyncClient client;
  private final SNSProps props;

  public Mono<Void> listen(String queueName, GenericHandler f) {
    return Mono.fromFuture(
        client.receiveMessage(request -> request
            .queueUrl(queueName)
            .waitTimeSeconds(20))
    )
        .doOnSuccess(response -> log.info("Size: " + response.messages().size()))
        .doOnError((e) -> {
          System.out.println(e.getMessage());
        })
        .flatMapMany(m -> Flux.fromIterable(m.messages()))
        .map(message -> message.body())
        .map(jsonString -> {
          ObjectMapper objectMapper = new ObjectMapper();
          try {
            return objectMapper.readValue(jsonString, SNSEventModel.class);
          } catch (JsonProcessingException ex) {
            return Flux.error(ex);
          }
        })
        .flatMap(event -> {
          SNSEventModel e = (SNSEventModel) event;
          return f.handle(e.getMessage());
        })
        .then();
  }

  /*
   client.deleteMessage((builder) -> builder
              .queueUrl(queueName)
              .receiptHandle(tuple.getT1().receiptHandle())
          );
   */

}
