package com.yupi.yuojcodesandbox.model;

import lombok.Data;

//进程执行信息
@Data
public class Executemessage {

    private Integer exitValue;//状态码
    private String message; //执行结果信息
    private String errorMessage; //错误信息
    private Long time;  //运行时间
    private Long memory; //运行内存
}
