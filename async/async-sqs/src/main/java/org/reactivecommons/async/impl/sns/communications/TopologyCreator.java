package org.reactivecommons.async.impl.sns.communications;

import lombok.extern.java.Log;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.*;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.*;
import software.amazon.awssdk.services.sqs.model.AddPermissionRequest;

import java.util.concurrent.CompletableFuture;

@Log
public class TopologyCreator {

    private final SnsAsyncClient topicClient;
    private final SqsAsyncClient queueClient;

    public TopologyCreator(SnsAsyncClient topicClient,SqsAsyncClient queueClient){
        this.topicClient = topicClient;
        this.queueClient = queueClient;
    }

    public Mono<String> declareTopic(String name){
        return listTopics()
                .map((topic)->topic.topicArn())
                .filter((topic)->topic.contains(":"+name))
                .switchIfEmpty(createTopic(name))
                .single();
    }

    private Mono<ListTopicsRequest> getListTopicRequest(){
        return Mono.just(ListTopicsRequest.builder().build());
    }

    public Flux<Topic> listTopics(){
        return getListTopicRequest()
                .flatMap(request -> Mono.fromFuture(topicClient.listTopics(request)))
                .flatMapMany((response)->Flux.fromIterable(response.topics()));
    }

    private Mono<CreateTopicRequest> getCreateTopicRequest(String name){
        return Mono.just(CreateTopicRequest.builder().name(name).build());
    }

    public Mono<String> createTopic(String name){
        return getCreateTopicRequest(name)
                .flatMap(request->Mono.fromFuture(topicClient.createTopic(request)))
                .map(CreateTopicResponse::toString)
                .doOnNext((response) -> log.fine(response))
                .doOnError((e) -> log.severe(e.toString()));
    }


    private Mono<CreateQueueRequest> createQueueRequest(String name){
        return Mono.just(CreateQueueRequest.builder().queueName(name).build());
    }

    public Mono<String> createQueue(String name){
        return createQueueRequest(name)
                .flatMap(request->Mono.fromFuture(queueClient.createQueue(request)))
                .map(CreateQueueResponse::toString)
                .doOnNext((response) -> log.fine(response))
                .doOnError((e) -> log.severe(e.toString()));
    }


    public Mono<String> getTopicArn(String name) {
        return listTopics()
                .map(Topic::topicArn)
                .filter((topic) -> topic.contains(":" + name))
                .single();
    }

    private Mono<GetQueueUrlRequest> getQueueUrlRequest(String queueName){
        return Mono.just(GetQueueUrlRequest.builder().queueName(queueName).build());
    }

    public Mono<String> getQueueUrl(String name){
        return getQueueUrlRequest(name)
                .flatMap((request)->Mono.fromFuture(queueClient.getQueueUrl(request)))
                .map(GetQueueUrlResponse::queueUrl);
    }

    public Mono<String> bind(String queueName,String topicName){
        return getQueueUrl(queueName)
                .flatMap(this::getQueueArn)
                .zipWith(getTopicArn(topicName))
                .flatMap((a)->getSubscribeRequest(a.getT1(),a.getT2()))
                .flatMap(request->Mono.fromFuture(topicClient.subscribe(request)))
                .map(SubscribeResponse::subscriptionArn)
                .onErrorMap(TopologyDefException::new);
    }


    private Mono<SubscribeRequest> getSubscribeRequest(String queueUrl,String topicArn) {
        SubscribeRequest subscribeRequest = SubscribeRequest.builder()
                .protocol("sqs")
                .endpoint(queueUrl)
                .topicArn(topicArn)
                .returnSubscriptionArn(true)
                .build();
        return Mono.just(subscribeRequest);
    }

    private Mono<GetQueueAttributesRequest> getQueueAttributesRequest(String queueUrl) {
        GetQueueAttributesRequest subscribeRequest = GetQueueAttributesRequest.builder()
                .queueUrl(queueUrl)
                .attributeNamesWithStrings("All")
                .build();
        return Mono.just(subscribeRequest);
    }

    public Mono<String> getQueueArn(String queueUrl) {
        return getQueueAttributesRequest(queueUrl)
                .flatMap(request -> Mono.fromFuture(queueClient.getQueueAttributes(request)))
                .map((response)->response.attributesAsStrings().get("QueueArn"));

    }

    private Mono<AddPermissionRequest> getAddPermisionRequest(String queueArn) {
        AddPermissionRequest subscribeRequest = AddPermissionRequest.builder()
                .queueUrl(queueArn)
                .actions("*")
                .build();
        return Mono.just(subscribeRequest);
    }


    public static class TopologyDefException extends RuntimeException {
        public TopologyDefException(Throwable cause) {
            super(cause);
        }
    }
}
