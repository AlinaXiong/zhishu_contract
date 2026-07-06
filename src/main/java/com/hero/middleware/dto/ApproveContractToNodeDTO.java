package com.hero.middleware.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Collection;

@Data
public class ApproveContractToNodeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Collection<String> contractNumbers;

    private String nodeName;
}
