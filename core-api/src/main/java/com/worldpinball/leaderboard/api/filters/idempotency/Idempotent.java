package com.worldpinball.leaderboard.api.filters.idempotency;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    String headerName() default "X-Idempotency-Key";
    int expirationMinutes() default 60;
    boolean requireHeader() default false;
}
