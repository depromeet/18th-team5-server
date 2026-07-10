package com.team.peektime_api;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class PeektimeApiApplicationTests {

	// RedissonClient는 빈 생성 시점에 실제 Redis 접속을 시도하므로,
	// Redis 없는 환경에서도 컨텍스트 테스트가 돌도록 목으로 대체한다
	@MockitoBean
	private RedissonClient redissonClient;

	@Test
	void contextLoads() {
	}

}
