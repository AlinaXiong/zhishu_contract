package com.hero.middleware.client.yuecai.response;

import lombok.Data;

import java.util.List;

@Data
public class MasterDataRes {
    private int totalPages;
    private int totalElements;
    private int numberOfElements;
    private int size;
    private int number;
    private boolean empty;
    private List<Object> content;
}
