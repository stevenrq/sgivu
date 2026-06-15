package com.sgivu.purchasesale.config;

import com.sgivu.purchasesale.dto.DashboardSummaryResponse;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

/**
 * Cache Redis para agregados del dashboard.
 *
 * <p>Cache {@code dashboard-summary}: TTL 60s. Namespace {@code sgivu:cache:purchase-sale:} aislado
 * del namespace de sesiones del gateway ({@code spring:session:sgivu-gateway}). Serialización JSON
 * (Jackson 3 + JSR-310 integrado) para evitar problemas de classloader con DevTools. Se invalida
 * con {@code @CacheEvict} en create/update/delete de contratos.
 */
@Configuration
@EnableCaching
public class CacheConfig {

  public static final String DASHBOARD_SUMMARY_CACHE = "dashboard-summary";

  @Bean
  CacheManager cacheManager(RedisConnectionFactory factory) {
    JacksonJsonRedisSerializer<DashboardSummaryResponse> jsonSerializer =
        new JacksonJsonRedisSerializer<>(DashboardSummaryResponse.class);

    RedisCacheConfiguration config =
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofSeconds(60))
            .prefixCacheNameWith("sgivu:cache:purchase-sale:")
            .disableCachingNullValues()
            .serializeValuesWith(SerializationPair.fromSerializer(jsonSerializer));

    return RedisCacheManager.builder(factory)
        .withCacheConfiguration(DASHBOARD_SUMMARY_CACHE, config)
        .build();
  }
}
