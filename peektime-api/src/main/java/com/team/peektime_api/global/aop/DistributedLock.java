package com.team.peektime_api.global.aop;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {


    // 락 이름
    String key();


    // 락의 시간 단위
    TimeUnit timeUnit() default TimeUnit.SECONDS;


    /**
     * 락을 기다리는 시간 (default - 5s)
     * 락 획득을 위해 waitTime 만큼 대기한다.
     */
    long waitTime() default 5L;


    /**
     * 락 임대 시간 (defauly - 3s)
     * 락을 획득한 이후 leaseTime 이 지나면 락을 해제한다.
     */
    long leaseTime() default 3L;


    /**
     * 락 구간을 REQUIRES_NEW 트랜잭션으로 감쌀지 여부 (default - true)
     * LLM 호출처럼 오래 블로킹하는 구간은 false로 두어 DB 커넥션 점유를 피한다.
     */
    boolean transactional() default true;

}
