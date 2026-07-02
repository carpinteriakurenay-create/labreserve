package com.labreserve;

import com.labreserve.support.RedisTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(RedisTestConfig.class)
class LabReserveApplicationTests {

    @Test
    void contextLoads() {
    }
}
