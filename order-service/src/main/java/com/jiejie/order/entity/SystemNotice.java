package com.jiejie.order.entity;

import lombok.Data;

import java.util.Date;

@Data
public class SystemNotice {
    private Long id;
    private String title;
    private String content;
    private Integer status;
    private Date createTime;
}
