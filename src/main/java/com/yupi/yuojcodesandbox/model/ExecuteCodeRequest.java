package com.yupi.yuojcodesandbox.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder //构造器方法创建对象 插件提供的loombook
@NoArgsConstructor //无参
@AllArgsConstructor //有参
public class ExecuteCodeRequest {

    //输入用例
    private List<String> inputList;

    //要执行的代码
    private String code;

    //使用的编程语言
    private String language;
}
