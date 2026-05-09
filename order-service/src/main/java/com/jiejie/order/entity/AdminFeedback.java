package com.jiejie.order.entity;

import lombok.Data;

import java.util.Date;

@Data
public class AdminFeedback {
    private Long id;
    private Long reporterId;
    private String content;
    private Integer status;
    private Date createTime;
}
