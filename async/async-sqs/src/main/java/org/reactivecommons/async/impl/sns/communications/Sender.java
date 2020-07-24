package org.reactivecommons.async.impl.sns.communications;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.reactivecommons.async.impl.Headers;
import org.reactivecommons.async.impl.sns.config.SNSProps;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
@Data
@RequiredArgsConstructor
@Log
public class Sender {

  private final SnsAsyncClient client;
  private final String sourceApplication;
  private final SNSProps props;

  public <T> Mono<Void> publish(T message, String targetName) {
    return getPublishRequest(message, targetName)
        .flatMap( request -> Mono.fromFuture( client.publish(request) ))
        .doOnSuccess(response -> log.info(response.messageId()))
        .then();
  }

  private <T> Mono<PublishRequest> getPublishRequest(T message, String targetName) {
    try {
      PublishRequest request = PublishRequest.builder()
          .message( objectToJSON(message) )
          .messageAttributes( getMessageAttributes() )
          .topicArn( getTopicARN( targetName ) )
          .build();
      return Mono.just(request);
    } catch (JsonProcessingException e) {
      return Mono.error(e);
    }
  }

  private String getTopicARN(String topicName) {
    return props.getTopicPrefix() + ":" + topicName;
  }

  private <T> String objectToJSON(T message) throws JsonProcessingException {
    ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    String json = ow.writeValueAsString(message);
    return json;
  }

  // TODO add messageAttributes
  private Map<String, MessageAttributeValue> getMessageAttributes() {
    Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
    addAttribute(messageAttributes, Headers.SOURCE_APPLICATION, sourceApplication);
    addAttribute(messageAttributes, Headers.CORRELATION_ID, UUID.randomUUID().toString());
    addAttribute(messageAttributes, Headers.TIMESTAMP, new Date().toString());
    return messageAttributes;
  }

  private void addAttribute(Map<String, MessageAttributeValue> messageAttributes, final String attributeName, final String attributeValue) {
    MessageAttributeValue messageAttributeValue = MessageAttributeValue.builder()
        .dataType("String")
        .stringValue(attributeValue)
        .build();

    messageAttributes.put(attributeName, messageAttributeValue);
  }

}
