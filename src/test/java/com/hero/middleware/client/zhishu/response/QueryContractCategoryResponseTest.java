package com.hero.middleware.client.zhishu.response;

import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class QueryContractCategoryResponseTest {

    @Test
    void parsesCategoryResourcesFromActualBodyShape() {
        String body = "{"
                + "\"msg\":\"success\","
                + "\"code\":0,"
                + "\"data\":{\"categoryResources\":[{"
                + "\"number\":\"009\","
                + "\"name\":\"Anchor\","
                + "\"id\":\"1121078071424713033\","
                + "\"abbreviation\":\"ZB\","
                + "\"children\":[{\"number\":\"009001\",\"name\":\"Brokerage\",\"id\":\"1\",\"abbreviation\":\"ZBJJ\"}]"
                + "}]}"
                + "}";

        QueryContractCategoryResponse response = JSON.parseObject(body, QueryContractCategoryResponse.class);

        assertEquals(Integer.valueOf(0), response.getCode());
        assertNotNull(response.getData().getCategoryResources());
        assertEquals("009", response.getData().getCategoryResources().get(0).getNumber());
        assertEquals("ZBJJ", response.getData().getCategoryResources().get(0)
                .getChildren().get(0).getAbbreviation());
    }
}
