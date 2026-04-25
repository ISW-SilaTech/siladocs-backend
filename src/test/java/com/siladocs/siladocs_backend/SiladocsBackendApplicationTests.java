package com.siladocs.siladocs_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
	"blockchain.fabric.api.url=http://localhost:8000",
	"blockchain.fabric.api.timeout.connect=10000",
	"blockchain.fabric.api.timeout.read=30000"
})
class SiladocsBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
