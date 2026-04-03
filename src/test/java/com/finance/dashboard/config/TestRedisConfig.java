package com.finance.dashboard.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * In-process Redis substitute for integration tests.
 *
 * <p>Uses a shared {@link ConcurrentHashMap} as backing store so that all operations
 * (get, set, setIfAbsent, delete) behave consistently across the test suite without
 * requiring a real Redis instance.
 *
 * <p>Why {@code @SuppressWarnings("unchecked")} on the bean methods?
 * Mockito's {@code mock(Class)} and {@code when(...).thenReturn(...)} are inherently
 * untyped at the Java generics level — {@code ValueOperations<String,String>} erases
 * to {@code ValueOperations} at runtime.  The compiler issues unchecked-conversion
 * warnings that cannot be satisfied without suppression; this is a well-known,
 * unavoidable limitation of Mockito + Java generics and does not affect correctness.
 */
@TestConfiguration
public class TestRedisConfig {

    /**
     * Shared backing store for all mocked Redis operations.
     * Tests run in a single JVM thread; ConcurrentHashMap ensures safe concurrent access
     * for the (rare) cases where async audit operations write during a test.
     */
    private final Map<String, String> redisStore = new ConcurrentHashMap<>();

    @Bean
    @Primary
    @SuppressWarnings("unchecked") // Mockito generic mock - see class Javadoc
    public StringRedisTemplate stringRedisTemplate() {
        StringRedisTemplate template = Mockito.mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = Mockito.mock(ValueOperations.class);

        when(template.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(1L);
        when(template.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        Mockito.doAnswer(inv -> {
            redisStore.put(inv.getArgument(0, String.class), inv.getArgument(1, String.class));
            return null;
        }).when(valueOps).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        when(valueOps.get(anyString()))
                .thenAnswer(inv -> redisStore.get(inv.getArgument(0, String.class)));

        when(template.delete(anyString())).thenAnswer(inv -> {
            redisStore.remove(inv.getArgument(0, String.class));
            return true;
        });

        return template;
    }

    @Bean
    @Primary
    @SuppressWarnings("unchecked") // Mockito generic mock - see class Javadoc
    public RedisTemplate<String, String> redisTemplate() {
        RedisTemplate<String, String> template = Mockito.mock(RedisTemplate.class);
        ValueOperations<String, String> valueOps = Mockito.mock(ValueOperations.class);

        when(template.opsForValue()).thenReturn(valueOps);

        // set — unconditional write
        Mockito.doAnswer(inv -> {
            redisStore.put(inv.getArgument(0, String.class), inv.getArgument(1, String.class));
            return null;
        }).when(valueOps).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        // setIfAbsent(key, value, Duration) — used by RecordService for idempotency (SETNX semantics)
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenAnswer(inv -> {
                    String key = inv.getArgument(0, String.class);
                    String val = inv.getArgument(1, String.class);
                    // putIfAbsent returns null if the key was absent → insertion succeeded
                    return redisStore.putIfAbsent(key, val) == null;
                });

        // setIfAbsent(key, value, long, TimeUnit) — fallback overload
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenAnswer(inv -> {
                    String key = inv.getArgument(0, String.class);
                    String val = inv.getArgument(1, String.class);
                    return redisStore.putIfAbsent(key, val) == null;
                });

        // get
        when(valueOps.get(anyString()))
                .thenAnswer(inv -> redisStore.get(inv.getArgument(0, String.class)));

        // delete — used by refresh token rotation and idempotency cleanup
        when(template.delete(anyString())).thenAnswer(inv -> {
            redisStore.remove(inv.getArgument(0, String.class));
            return true;
        });

        return template;
    }

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return Mockito.mock(RedisConnectionFactory.class);
    }
}
