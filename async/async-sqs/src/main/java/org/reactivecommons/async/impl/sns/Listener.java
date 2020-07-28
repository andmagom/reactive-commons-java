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
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@RequiredArgsConstructor
@Log
public class Listener {

  private final SqsAsyncClient client;
  private final SNSProps props;

  public Mono<Void> listen(String queueName, GenericHandler<Mono, SNSEventModel> f) {
    return getMessages(queueName)
        .flatMap(message -> {
          ObjectMapper objectMapper = new ObjectMapper();
          try {
            return Mono.zip(Mono.just(objectMapper.readValue(message.body(), SNSEventModel.class)),Mono.just(message));
          } catch (JsonProcessingException ex) {
            return Flux.error(ex);
          }
        })
        .flatMap((tuple)->Mono.zip(Mono.just(tuple.getT2()),f.handle(tuple.getT1())))
        .flatMap((a)->{
            DeleteMessageRequest deleteMessageRequest = getdeleteMessageRequest(queueName,a.getT1().receiptHandle());
            Mono.fromFuture(client.deleteMessage(deleteMessageRequest)).subscribe((res)->{
                System.out.println("Respuesta: "+res.toString());
            });
            System.out.println("Mensaje eliminado: "+a.getT1().receiptHandle());
            return a.getT2();
        }).then();
  }

  public Flux<Message> getMessages(String queueName){

      return getReceiveMessageRequest(queueName)
            .flatMap((req)->Mono.fromFuture(client.receiveMessage(req))
                    .doOnSuccess(response -> log.info("Size: " + response.messages().size()))
                    .doOnError((e) -> {
                        System.out.println(e.getMessage());
                    })
            )
            .flatMapMany((response)->Flux.fromIterable(response.messages()));

  }

  public Mono<ReceiveMessageRequest> getReceiveMessageRequest(String name){
      return Mono.just(ReceiveMessageRequest.builder().queueUrl(name).maxNumberOfMessages(10).waitTimeSeconds(20).build());
  }

  public DeleteMessageRequest getdeleteMessageRequest(String queueName, String receiptHandle){
      return DeleteMessageRequest.builder().queueUrl(queueName).receiptHandle(receiptHandle).build();
  }

  /*
   client.deleteMessage((builder) -> builder
              .queueUrl(queueName)
              .receiptHandle(tuple.getT1().receiptHandle())
          );
   */

}
