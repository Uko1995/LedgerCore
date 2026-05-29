package com.example.LedgerCore;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.example.LedgerCore.repository.ReservationRepository;
import static org.mockito.Mockito.mock;

@SpringBootTest
@Import(DemoApplicationTests.TestMockConfig.class)
class DemoApplicationTests {

	@Test
	void contextLoads() {
	}

	@TestConfiguration
	static class TestMockConfig {
		@Bean
		ReservationRepository reservationRepository() {
			return mock(ReservationRepository.class);
		}
	}
}
