package com.jiejie.common;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer code;
    private String msg;
    private Object data;

    public static Result success(Object data) { return new Result(200, "成功", data); }
    public static Result error(String msg) { return new Result(500, msg, null); }
    public static Result error(Integer code, String msg) { return new Result(code, msg, null); }
}
