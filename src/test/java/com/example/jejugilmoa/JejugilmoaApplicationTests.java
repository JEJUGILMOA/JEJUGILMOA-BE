package com.example.jejugilmoa;

import com.example.jejugilmoa.global.init.DataInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class JejugilmoaApplicationTests {

    @MockitoBean
    DataInitializer dataInitializer;

	@Test
	void contextLoads() {
	}

}
