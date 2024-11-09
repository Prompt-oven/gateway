package com.promptoven.gateway.repo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

	@Value("${spring.data.redis.host}")
	String host;
	@Value("${spring.data.redis.port}")
	int port;

	@Bean
	public RedisConnectionFactory redisConnectionFactory() {
		RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(host, port);
		LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfig);
		factory.afterPropertiesSet();
		return factory;
	}

	@Bean
	public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
		RedisTemplate<String, String> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new StringRedisSerializer());
		template.setHashKeySerializer(new StringRedisSerializer());
		template.setHashValueSerializer(new StringRedisSerializer());
		template.afterPropertiesSet();
		return template;
	}
}