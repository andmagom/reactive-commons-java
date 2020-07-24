package org.reactivecommons.async.impl.sns.config;

import lombok.Data;
import software.amazon.awssdk.regions.Region;

@Data
public class SNSProps {
  private final Region region;
  private final String topicPrefix;
  private final String nameProject;
}
