package com.hero.middleware.client.yuecai;

import com.hero.middleware.config.YuecaiApiConfig;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class YuecaiApiClientTest {

    private final AtomicInteger tokenRequestCount = new AtomicInteger();
    private HttpServer server;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/oauth/oauth/token", exchange -> {
            tokenRequestCount.incrementAndGet();
            byte[] response = "{\"access_token\":\"cached-token\",\"expires_in\":3600}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void reusesAccessTokenUntilItExpires() {
        YuecaiApiClient client = new YuecaiApiClient();
        YuecaiApiConfig config = new YuecaiApiConfig();
        config.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        config.setGrantType("client_credentials");
        config.setClientId("test-client");
        config.setClientSecret("test-secret");
        config.setTimeout(5_000);
        ReflectionTestUtils.setField(client, "yuecaiApiConfig", config);

        assertEquals("cached-token", client.getAccessToken());
        assertEquals("cached-token", client.getAccessToken());
        assertEquals(1, tokenRequestCount.get());
    }
}
