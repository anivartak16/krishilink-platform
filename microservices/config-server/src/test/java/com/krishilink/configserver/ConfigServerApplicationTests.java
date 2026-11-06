package com.krishilink.configserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "eureka.client.register-with-eureka=false",
    "eureka.client.fetch-registry=false",
    "spring.cloud.config.server.native.search-locations=classpath:/config"
})
class ConfigServerApplicationTests {
    @Test
    void contextLoads() {}
}
