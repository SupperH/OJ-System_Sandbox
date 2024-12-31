package com.yupi.yuojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.dfa.WordTree;
import com.yupi.yuojcodesandbox.model.ExecuteCodeRequest;
import com.yupi.yuojcodesandbox.model.ExecuteCodeResponse;
import com.yupi.yuojcodesandbox.model.Executemessage;
import com.yupi.yuojcodesandbox.model.JudgeInfo;
import com.yupi.yuojcodesandbox.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/*沙箱的模板方法，定义成抽象类，后面可以让不同子类继承然后判断是否要重写里面方法该逻辑*/
@Slf4j
public abstract class JavaCodeSandboxTemplate implements CodeSandbox{

    //全局代码存放路径
    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    //全局代码文件名
    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    //实际让外层调用的方法
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {

        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();


        /*1.把用户代码保存为文件*/
        File userCodeFile = saveCodeToFile(code);

        /*2.编译代码，得到class文件*/
        Executemessage compileFileExecuteMessage = compileFile(userCodeFile);
        System.out.println(compileFileExecuteMessage);


        /*3.执行代码，获得执行结果列表*/
        List<Executemessage> executemessageList = runFile(userCodeFile, inputList);


        /*4.整理输出结果*/
        ExecuteCodeResponse outputResponse = getOutputResponse(executemessageList);


        /*5.删除文件*/
        boolean flg = deleteFile(userCodeFile);
        if(!flg){
            log.error("deleteFile error userCodeFilePath = {}",userCodeFile.getAbsolutePath());
        }

        return outputResponse;
    }

    /**
     * 1.把用户代码保存为文件
     * @param code 用户代码
     * @return
     */
    public File saveCodeToFile(String code){
        //把用户代码保存为文件
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

        return userCodeFile;
    }


    /**
     * 2.编译代码，得到class文件
     * @param userCodeFile 要编译的代码文件
     * @return
     */
    public Executemessage compileFile(File userCodeFile){
        /*设置编译代码的命令，javac 后面的文件动态拼接 用%s占位符*/
        String compileCmd = String.format("javac -encoding utf-8 %s",userCodeFile.getAbsolutePath());
        try {
            //编译代码，得到class文件 使用的是java类中的Process类的功能，这个类可以执行可以在控制台执行的命令
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            Executemessage executemessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");

            if(executemessage.getExitValue() != 0){
                throw new RuntimeException("编译错误");
            }
            return executemessage;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 3.执行文件，获得执行结果列表
     * @param userCodeFile
     * @param inputList
     * @return
     */
    public List<Executemessage> runFile(File userCodeFile,List<String> inputList){
        //获取路径
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();

        //存放输出结果列表
        List<Executemessage> executemessageList = new ArrayList<>();

        /*执行代码，得到输出结果*/
        //循环输入参数，拼接到执行命令中 inputList 是一个list集合，长度多少就应该有多少次输出，所以循环
        for(String inputArgs : inputList){
            /*%s占位符
             -Dfile.encoding=UTF-8 设置运行时编码格式为utf-8
             -Xmx256m 给进程分配256m的jvm空间*/
            //然后通过命令来让java允许时，启动我们定义的安全管理器 注意把安全管理器放到单独的文件夹里，然后通过命令的方式加载
            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s",
                    userCodeParentPath,inputArgs);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                Executemessage executemessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
                System.out.println(executemessage);
                executemessageList.add(executemessage);
            } catch (Exception e) {
                throw new RuntimeException("程序执行异常");
            }
        }
        return executemessageList;
    }

    /**
     * 4.获取输出结果
     * @param executemessageList
     * @return
     */
    public ExecuteCodeResponse getOutputResponse(List<Executemessage> executemessageList){
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

        return executeCodeResponse;
    }

    /**
     * 5.删除文件
     * @param userCodeFile
     * @return
     */
    public boolean deleteFile(File userCodeFile){

        /*文件清理， 上述结果结束后，为了防止内存资源和硬盘资源浪费，应该进行文件清理*/
        if(userCodeFile.getParentFile() !=null){
            //获取文件所在目录
            String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
            //这里用的是hutu工具包
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除"+(del?"成功":"失败"));
            return del;
        }
        return true;
    }


    //6.获取错误响应
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
