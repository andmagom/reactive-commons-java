package org.reactivecommons.async.impl.sns.config;

import lombok.Builder;
import lombok.Data;
import software.amazon.awssdk.regions.Region;

@Data
@Builder(toBuilder = true)
public class SNSProps {
  private final Region region;
  private final String topicPrefix;
}
