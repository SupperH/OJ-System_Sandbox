package com.yupi.yuojcodesandbox.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

//返回给前端的代码执行结果
@Data
@Builder //构造器方法创建对象 插件提供的
@NoArgsConstructor //无参
@AllArgsConstructor //有参
public class ExecuteCodeResponse {

    //输出用例
    private List<String> outPutList;

    //接口信息
    private String message;

    //执行状态
    private Integer status;

    //判题信息
    private JudgeInfo judgeInfo;
}
