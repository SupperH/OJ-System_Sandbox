package com.yupi.yuojcodesandbox.controller;

import com.yupi.yuojcodesandbox.JavaNativeCodeSandbox;
import com.yupi.yuojcodesandbox.model.ExecuteCodeRequest;
import com.yupi.yuojcodesandbox.model.ExecuteCodeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController("/")
public class MainController {

    //传递的请求头，安全性校验
    private static final String AUTH_REQUEST_HEADER = "auth";
    //传递的密钥
    private static final String AUTH_REQUEST_SECRET = "secretKey";

    @Resource
    private JavaNativeCodeSandbox javaNativeCodeSandbox;

    @GetMapping("/health")
    public String healthCheck(){
        return "ok";
    }

    /**
     * 执行代码 以json方式接收参数
     * @param executeCodeRequest
     * @return
     */
    @PostMapping("/executeCode")
    ExecuteCodeResponse executeCode(@RequestBody ExecuteCodeRequest executeCodeRequest, HttpServletRequest request,
                                    HttpServletResponse response){

        //获取调用方的请求头然后进行安全性校验
        String authHeader = request.getHeader(AUTH_REQUEST_HEADER);
        //如果传递的请求头和约定的密钥不一致 就说明调用方不明，返回错误状态码和null
        if(!AUTH_REQUEST_SECRET.equals(authHeader)){
            response.setStatus(403);
            return null;

        }

        if(executeCodeRequest == null){
            throw new RuntimeException("请求参数为空");
        }
        return javaNativeCodeSandbox.executeCode(executeCodeRequest);
    }
}
