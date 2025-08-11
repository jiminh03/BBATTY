package com.ssafy.chat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
	"spring.data.redis.host=localhost",
	"spring.data.redis.port=6379",
	"spring.data.redis.repositories.enabled=false"
})
class ChatApplicationTests {

	@Test
	void contextLoads() {
	}

}
