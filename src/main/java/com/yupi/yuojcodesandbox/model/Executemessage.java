package com.yupi.yuojcodesandbox.model;

import lombok.Data;

//进程执行信息
@Data
public class Executemessage {

    private Integer exitValue;
    private String message;
    private String errorMessage;
}
