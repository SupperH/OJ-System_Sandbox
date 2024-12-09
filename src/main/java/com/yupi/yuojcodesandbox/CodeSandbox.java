package com.yupi.yuojcodesandbox;

import com.yupi.yuojcodesandbox.model.ExecuteCodeRequest;
import com.yupi.yuojcodesandbox.model.ExecuteCodeResponse;

//代码沙箱接口 用来执行代码的
/*为什么要定义接口：
* 如果之后不想用自己的沙箱 想用别人的沙箱 直接修改接口就行了 代码只调用接口
* 便于拓展提高通用性*/
public interface CodeSandbox {

    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}
