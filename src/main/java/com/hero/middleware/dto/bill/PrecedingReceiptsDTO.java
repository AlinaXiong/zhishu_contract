package com.hero.middleware.dto.bill;

import lombok.Data;

import java.util.List;

@Data
public class PrecedingReceiptsDTO {
    private int total;
    private List<ReceiptsDTO> receipts;
}
