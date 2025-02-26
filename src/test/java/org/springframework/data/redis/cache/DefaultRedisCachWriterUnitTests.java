/*
 * Copyright 2017-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.redis.cache;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.types.Expiration;

/**
 * Unit tests for {@link DefaultRedisCacheWriter}
 *
 * @author John Blum
 */
@ExtendWith(MockitoExtension.class)
class DefaultRedisCacheWriterUnitTests {

	@Mock private CacheStatisticsCollector mockCacheStatisticsCollector = mock(CacheStatisticsCollector.class);

	@Mock private RedisConnection mockConnection;

	@Mock(strictness = Mock.Strictness.LENIENT) private RedisConnectionFactory mockConnectionFactory;

	@Mock private ReactiveRedisConnection mockReactiveConnection;

	@Mock(strictness = Mock.Strictness.LENIENT) private TestReactiveRedisConnectionFactory mockReactiveConnectionFactory;

	@BeforeEach
	void setup() {
		doReturn(this.mockConnection).when(this.mockConnectionFactory).getConnection();
		doReturn(this.mockConnection).when(this.mockReactiveConnectionFactory).getConnection();
		doReturn(this.mockReactiveConnection).when(this.mockReactiveConnectionFactory).getReactiveConnection();
	}

	private RedisCacheWriter newRedisCacheWriter() {
		return spy(new DefaultRedisCacheWriter(this.mockConnectionFactory, mock(BatchStrategy.class))
				.withStatisticsCollector(this.mockCacheStatisticsCollector));
	}

	private RedisCacheWriter newReactiveRedisCacheWriter() {
		return spy(new DefaultRedisCacheWriter(this.mockReactiveConnectionFactory, Duration.ZERO, mock(BatchStrategy.class))
				.withStatisticsCollector(this.mockCacheStatisticsCollector));
	}

	@Test // GH-2351
	void getWithNonNullTtl() {

		byte[] key = "TestKey".getBytes();
		byte[] value = "TestValue".getBytes();

		Duration ttl = Duration.ofSeconds(15);
		Expiration expiration = Expiration.from(ttl);

		RedisStringCommands mockStringCommands = mock(RedisStringCommands.class);

		doReturn(mockStringCommands).when(this.mockConnection).stringCommands();
		doReturn(value).when(mockStringCommands).getEx(any(), any());

		RedisCacheWriter cacheWriter = newRedisCacheWriter();

		assertThat(cacheWriter.get("TestCache", key, ttl)).isEqualTo(value);

		verify(this.mockConnection).stringCommands();
		verify(mockStringCommands).getEx(eq(key), eq(expiration));
		verify(this.mockConnection).close();
		verifyNoMoreInteractions(this.mockConnection, mockStringCommands);
	}

	@Test // GH-2351
	void getWithNullTtl() {

		byte[] key = "TestKey".getBytes();
		byte[] value = "TestValue".getBytes();

		RedisStringCommands mockStringCommands = mock(RedisStringCommands.class);

		doReturn(mockStringCommands).when(this.mockConnection).stringCommands();
		doReturn(value).when(mockStringCommands).get(any());

		RedisCacheWriter cacheWriter = newRedisCacheWriter();

		assertThat(cacheWriter.get("TestCache", key, null)).isEqualTo(value);

		verify(this.mockConnection).stringCommands();
		verify(mockStringCommands).get(eq(key));
		verify(this.mockConnection).close();
		verifyNoMoreInteractions(this.mockConnection, mockStringCommands);
	}

	interface TestReactiveRedisConnectionFactory extends ReactiveRedisConnectionFactory, RedisConnectionFactory {}

}
