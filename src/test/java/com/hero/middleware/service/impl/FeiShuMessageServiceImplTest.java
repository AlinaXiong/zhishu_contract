package com.hero.middleware.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hero.middleware.client.feishu.FeiShuApiClient;
import com.hero.middleware.client.feishu.request.FeiShuMessageSendRequest;
import com.hero.middleware.client.feishu.response.FeiShuMessageSendResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;

@Slf4j
@SpringBootTest
public class FeiShuMessageServiceImplTest {
    @Autowired
    private FeiShuApiClient feiShuApiClient;

    @Test
    public void sendMessageTest(){
        FeiShuMessageSendRequest request = new FeiShuMessageSendRequest();
        request.setReceiveId("b9a947f8");
        request.setMsgType("interactive");
        request.setContent(JSON.toJSONString(buildMessageCard("测试发送时间，卡片形式")));
        FeiShuMessageSendResponse feiShuMessageSendResponse = feiShuApiClient.sendMessage(request);
        System.out.println(JSONObject.toJSON(feiShuMessageSendResponse));
    }

    private JSONObject buildMessageCard(String content) {
        JSONObject card = new JSONObject();

        JSONObject text = new JSONObject();
        text.put("tag", "plain_text");
        text.put("content", content);

        JSONObject element = new JSONObject();
        element.put("tag", "div");
        element.put("text", text);

        JSONArray elements = new JSONArray();
        elements.add(element);
        card.put("elements", elements);
        return card;
    }
}
