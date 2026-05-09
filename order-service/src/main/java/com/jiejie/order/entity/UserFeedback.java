package com.jiejie.order.entity;

import lombok.Data;

import java.util.Date;

@Data
public class UserFeedback {
    private Long id;
    private Long userId;
    private String content;
    private String replyContent;
    private Integer status;
    private Date createTime;
}
