package org.reactivecommons.async.impl.sns.communications;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.reactivecommons.async.api.handlers.GenericHandler;
import org.reactivecommons.async.impl.model.SNSEventModel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@RequiredArgsConstructor
@Log
public class Listener {

  private final SqsAsyncClient client;

  public Mono<Void> listen(String queueName, GenericHandler<Mono, SNSEventModel> handler) {
    return getMessages(queueName)
        .flatMap(this::mapObject)
        .flatMap(tuple -> handleMessage(tuple, handler))
        .flatMap(message -> deleteMessage(queueName, message))
        .then();
  }

  public Mono<Message> handleMessage(Tuple2<SNSEventModel, Message> tuple,
      GenericHandler<Mono, SNSEventModel> f) {
    f.handle(tuple.getT1()).subscribe();
    Mono<Message> originalMessage = Mono.just(tuple.getT2());
    return originalMessage;
  }


  public Mono<Tuple2<SNSEventModel, Message>> mapObject(Message message) {
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      return Mono.zip(Mono.just(objectMapper.readValue(message.body(), SNSEventModel.class)),
          Mono.just(message));
    } catch (JsonProcessingException ex) {
      return Mono.error(ex);
    }
  }

  public Mono<DeleteMessageResponse> deleteMessage(String queueName, Message m) {
    DeleteMessageRequest deleteMessageRequest = getDeleteMessageRequest(queueName,
        m.receiptHandle());
    return Mono.fromFuture(client.deleteMessage(deleteMessageRequest))
        .doOnSuccess(response -> log.info("Deleted Message: " + response.hashCode()))
        .doOnError((e) -> {
          log.warning(e.getMessage());
        });

  }

  public Flux<Message> getMessages(String queueName) {
    return getReceiveMessageRequest(queueName)
        .flatMap((req) -> Mono.fromFuture(client.receiveMessage(req))
            .doOnSuccess(response -> log.info("Size: " + response.messages().size()))
            .doOnError((e) -> {
              System.out.println(e.getMessage());
            })
        )
        .flatMapMany((response) -> Flux.fromIterable(response.messages()));

  }

  public Mono<ReceiveMessageRequest> getReceiveMessageRequest(String name) {
    log.info("Getting messages from " + name);
    return Mono.just(
        ReceiveMessageRequest.builder().queueUrl(name).maxNumberOfMessages(10).waitTimeSeconds(20)
            .build());
  }

  public DeleteMessageRequest getDeleteMessageRequest(String queueName, String receiptHandle) {
    return DeleteMessageRequest.builder().queueUrl(queueName).receiptHandle(receiptHandle).build();
  }

  public Flux<Void> startListener(String queueName, GenericHandler<Mono, SNSEventModel> handler) {
    return listen(queueName, handler)
        .doOnSuccess((e) -> System.out.println("terminated " + queueName))
        .repeat();
  }

}
