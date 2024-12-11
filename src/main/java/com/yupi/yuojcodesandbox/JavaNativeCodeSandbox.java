package com.yupi.yuojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import com.yupi.yuojcodesandbox.model.ExecuteCodeRequest;
import com.yupi.yuojcodesandbox.model.ExecuteCodeResponse;
import com.yupi.yuojcodesandbox.model.Executemessage;
import com.yupi.yuojcodesandbox.utils.ProcessUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

//java原生实现的代码沙箱
public class JavaNativeCodeSandbox implements CodeSandbox {

    //全局代码存放路径
    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    //全局代码文件名
    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    //测试代码
    public static void main(String[] args) {
        JavaNativeCodeSandbox javaNativeCodeSandbox = new JavaNativeCodeSandbox();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "1 3"));

        //使用resourceutil读取对应的文件内容 设置读取的编码格式
        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java",StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandbox.executeCode(executeCodeRequest);

        System.out.println(executeCodeResponse);
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

        //获取用户工作目录，也就是根目录
        String userDir = System.getProperty("user.dir");

        //文件夹为tmpCode 同时拼接路径 注意路径中的\用File.separator 因为linux和windows的\不一样 所以用这个
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;

        //判断全局代码目录是否存在,没有就新建
        if(!FileUtil.exist(globalCodePathName)){
            FileUtil.mkdir(globalCodePathName);
        }

        //把用户的代码隔离存放，每次提交的代码都用不同的路径 因为每个人不可能只提交一次代码，所以这么做可以区分提交
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();

        //把用户的代码写入代码文件（因为本系统定死提交的文件类名叫做Main所以直接写死，然后设置utf-8编码）
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code,userCodePath, StandardCharsets.UTF_8);

        /*设置编译代码的命令，javac 后面的文件动态拼接 用%s占位符*/
        String compileCmd = String.format("javac -encoding utf-8 %s",userCodeFile.getAbsolutePath());

        try {
            //编译代码，得到class文件 使用的是java类中的Process类的功能，这个类可以执行可以在控制台执行的命令
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            Executemessage executemessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
            System.out.println(executemessage);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        /*执行代码，得到输出结果*/
        //循环输入参数，拼接到执行命令中 inputList 是一个list集合，长度多少就应该有多少次输出，所以循环
        for(String inputArgs : inputList){
            /*第一个占位符：路径 第二个占位符 执行代码需要的输入参数 -Dfile.encoding=UTF-8 设置运行时编码格式为utf-8*/
            String runCmd = String.format("java -Dfile.encoding=UTF-8 -cp %s Main %s",userCodeParentPath,inputArgs);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                Executemessage executemessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
                System.out.println(executemessage);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }


        return null;
    }
}
