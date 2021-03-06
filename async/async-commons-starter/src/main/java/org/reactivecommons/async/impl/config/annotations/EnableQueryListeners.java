package org.reactivecommons.async.impl.config.annotations;

import org.reactivecommons.async.impl.config.QueryListenerConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;


@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
@Import(QueryListenerConfig.class)
@Configuration
public @interface EnableQueryListeners {
}



