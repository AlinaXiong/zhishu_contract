package com.hero.middleware.client.zhishu;

import com.hero.middleware.config.ZhishuApiConfig;
import com.hero.middleware.config.ZhishuHttpClientConfig;
import com.hero.middleware.dto.ApiLogEvent;
import com.hero.middleware.service.ApiLogService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZhishuApiClientTest {

    @Mock
    private ZhishuTokenManager zhishuTokenManager;

    @Mock
    private ApiLogService apiLogService;

    private HttpServer server;
    private CloseableHttpClient httpClient;
    private PoolingHttpClientConnectionManager connectionManager;
    private ZhishuApiClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

        ZhishuApiConfig config = new ZhishuApiConfig();
        config.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        config.setTimeout(30000);
        config.setConnectionRequestTimeout(5000);
        config.setConnectTimeout(5000);
        config.setMaxTotalConnections(30);
        config.setMaxConnectionsPerRoute(12);

        ZhishuHttpClientConfig httpClientConfig = new ZhishuHttpClientConfig();
        connectionManager = httpClientConfig.zhishuHttpConnectionManager(config);
        httpClient = httpClientConfig.zhishuHttpClient(connectionManager, config);
        client = new ZhishuApiClient();
        ReflectionTestUtils.setField(client, "zhishuApiConfig", config);
        ReflectionTestUtils.setField(client, "zhishuTokenManager", zhishuTokenManager);
        ReflectionTestUtils.setField(client, "apiLogService", apiLogService);
        ReflectionTestUtils.setField(client, "zhishuHttpClient", httpClient);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (httpClient != null) {
            httpClient.close();
        }
        if (connectionManager != null) {
            connectionManager.shutdown();
        }
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void connectionPoolUsesConfiguredLimits() {
        assertEquals(30, connectionManager.getMaxTotal());
        assertEquals(12, connectionManager.getDefaultMaxPerRoute());
    }

    @Test
    void doPostUsesPooledClientAndPreservesAuthorizationAndBody() {
        when(zhishuTokenManager.getAccessToken()).thenReturn("test-token");
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> contentType = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        server.createContext("/contracts", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            requestBody.set(readRequestBody(exchange));
            writeJsonResponse(exchange, "{\"code\":0,\"msg\":\"success\"}");
        });
        server.start();

        String response = client.doPost("/contracts", singletonMap("contract_number", "C-001"));

        assertEquals("{\"code\":0,\"msg\":\"success\"}", response);
        assertEquals("Bearer test-token", authorization.get());
        assertEquals("application/json", contentType.get());
        assertEquals("{\"contract_number\":\"C-001\"}", requestBody.get());
        verify(apiLogService).record(any(ApiLogEvent.class));
    }

    @Test
    void doGetPreservesQueryParameters() {
        when(zhishuTokenManager.getAccessToken()).thenReturn("test-token");
        AtomicReference<String> query = new AtomicReference<>();
        server.createContext("/process", exchange -> {
            query.set(exchange.getRequestURI().getRawQuery());
            writeJsonResponse(exchange, "{\"code\":0}");
        });
        server.start();

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("user_id_type", "user_id");
        String response = client.doGet("/process", params);

        assertEquals("{\"code\":0}", response);
        assertEquals("user_id_type=user_id", query.get());
        verify(apiLogService, times(1)).record(any(ApiLogEvent.class));
    }

    private Map<String, Object> singletonMap(String key, Object value) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(key, value);
        return values;
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[256];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private void writeJsonResponse(HttpExchange exchange, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }
}
