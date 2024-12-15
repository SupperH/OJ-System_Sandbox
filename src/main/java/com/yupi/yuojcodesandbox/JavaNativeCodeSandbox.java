package com.yupi.yuojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import com.yupi.yuojcodesandbox.model.ExecuteCodeRequest;
import com.yupi.yuojcodesandbox.model.ExecuteCodeResponse;
import com.yupi.yuojcodesandbox.model.Executemessage;
import com.yupi.yuojcodesandbox.model.JudgeInfo;
import com.yupi.yuojcodesandbox.utils.ProcessUtils;
import org.springframework.util.StopWatch;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
            return getErrorResponse(e);
        }

        //存放输出结果列表
        List<Executemessage> executemessageList = new ArrayList<>();

        /*执行代码，得到输出结果*/
        //循环输入参数，拼接到执行命令中 inputList 是一个list集合，长度多少就应该有多少次输出，所以循环
        for(String inputArgs : inputList){
            /*第一个占位符：路径 第二个占位符 执行代码需要的输入参数 -Dfile.encoding=UTF-8 设置运行时编码格式为utf-8*/
            String runCmd = String.format("java -Dfile.encoding=UTF-8 -cp %s Main %s",userCodeParentPath,inputArgs);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                Executemessage executemessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
                System.out.println(executemessage);
                executemessageList.add(executemessage);
            } catch (Exception e) {
                return getErrorResponse(e);
            }

        }

        /*收集整理输出结果*/
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();

        //循环去获取每一个case的执行结果
        List<String> outputList = new ArrayList<>();
        //存放运行时间最大值
        long maxTime = 0;
        for(Executemessage executemessage : executemessageList){
            String errorMessage = executemessage.getErrorMessage();
            //如果输出结果中错误信息不为空，说明这一个case报错，那么赋值错误信息并且结束循环
            if(StrUtil.isNotBlank(errorMessage)){
                executeCodeResponse.setMessage(errorMessage);
                //用户提交的代码执行中存在错误
                executeCodeResponse.setStatus(3);
                break;
            }
            outputList.add(executemessage.getMessage());

            /*执行时间，这里使用所有输出用例中的最大值，这里可以当作扩展，每个case都有自己单独的执行时间，其实就跟输出结果一样用list就行了*/
            Long time = executemessage.getTime();
            if(time !=null){
                //使用函数来获取最大值
                maxTime = Math.max(maxTime,time);
            }
        }
        /*如果正常运行完成，那么输出case的结果和输出结果列表的长度是一样的 因为上面for循环如果不结束循环会把每一个输出结果列表的值放入case执行结果中*/
        if(outputList.size() == executemessageList.size()){
            //正常运行完成
            executeCodeResponse.setStatus(1);
        }

        //赋值输出结果
        executeCodeResponse.setOutPutList(outputList);

        //判题详情信息不在代码沙箱服务赋值，在判题服务中进行赋值
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        //非常麻烦，这里暂时不做实现 要借助第三方库
//        judgeInfo.setMemory();
        executeCodeResponse.setJudgeInfo(judgeInfo);


        /*文件清理， 上述结果结束后，为了防止内存资源和硬盘资源浪费，应该进行文件清理*/
        if(userCodeFile.getParentFile() !=null){
            //这里用的是hutu工具包
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除"+(del?"成功":"失败"));
        }
        
        return executeCodeResponse;
    }

    //获取错误响应
    private ExecuteCodeResponse getErrorResponse(Throwable e){
        //如果程序报错，返回一个空的response，里面包含错误信息和状态码
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutPutList(new ArrayList<>());
        //存放程序错误信息
        executeCodeResponse.setMessage(e.getMessage());
        //表示代码沙箱程序错误
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setJudgeInfo(new JudgeInfo());

        return executeCodeResponse;
    }
}
