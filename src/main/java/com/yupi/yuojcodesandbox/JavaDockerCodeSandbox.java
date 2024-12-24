package com.yupi.yuojcodesandbox;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.dfa.WordTree;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.google.common.base.Stopwatch;
import com.yupi.yuojcodesandbox.model.ExecuteCodeRequest;
import com.yupi.yuojcodesandbox.model.ExecuteCodeResponse;
import com.yupi.yuojcodesandbox.model.Executemessage;
import com.yupi.yuojcodesandbox.model.JudgeInfo;
import com.yupi.yuojcodesandbox.utils.ProcessUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

//java原生实现的代码沙箱
public class JavaDockerCodeSandbox implements CodeSandbox {

    //全局代码存放路径
    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    //全局代码文件名
    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    //超时时间 5秒
    private static final long TIME_OUT = 5000L;

    //安全管理器class文件所在文件夹
    private static final  String SECURITY_MANAGER_PATH = "G:\\JavaProjects\\OJ\\yuoj-code-sandbox\\src\\main\\resources\\security";
    private static final  String SECURITY_MANAGER_CLASSNAME = "MySecurityManager";

    //判断是否初次运行,如果初次运行就需要拉取jdk镜像
    private static final Boolean FIRST_INIT = true;


    //测试代码
    public static void main(String[] args) {
        JavaDockerCodeSandbox javaNativeCodeSandbox = new JavaDockerCodeSandbox();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "1 3"));

        //使用resourceutil读取对应的文件内容 设置读取的编码格式
        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java",StandardCharsets.UTF_8);
        //String code = ResourceUtil.readStr("testCode/unsafeCode/ReadFileError.java",StandardCharsets.UTF_8);
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


        DockerClient dockerClient = DockerClientBuilder.getInstance().build();

        /*拉取jdk镜像放入容器,容器理解为一套环境,那么环境必须要有jdk才行*/
        String image = "openjdk:8-alpine";
        if(!FIRST_INIT){
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback(){
                @Override
                public void onNext(PullResponseItem item) {
                    System.out.println("下载镜像"+item.getStatus());
                    super.onNext(item);
                }
            };
            //awaitCompletion等待运行结束
            try {
                pullImageCmd.exec(pullImageResultCallback).awaitCompletion();
            } catch (InterruptedException e) {
                System.out.println("拉取镜像异常");
                throw new RuntimeException(e);
            }
            System.out.println("下载镜像完成");
        }


        /*创建容器-理解为创建一套运行的环境*/
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
        //创建容器时,绑定终端和容器的目录,第一个参数是本机代码存放目录,第二个参数是放入linux环境要存放的目录 作用是把本地文件同步到容器中,可以让容器访问 也可以叫挂载目录
        HostConfig hostConfig = new HostConfig();
        //设置容器内存 设置为100M
        hostConfig.withMemory(100*1000*1000L);
        //设置容器的cpu 设置为1核
        hostConfig.withCpuCount(1L);
        //配置linux的安全管理配置 这里放入命令
        //hostConfig.withSecurityOpts(Arrays.asList("seccomp=安全管理配置字符串"));

        /*这里这么做的目的是 因为这个目前是在windows开发,然后远程操作linux运行,然后代码会自动push到linux的文件夹,本质还是本地开发,所以使用file.seperator获取分隔符是没有意义的
         * 因为这里是将linux中项目代码存放目录中的用户运行代码tmpCode映射到一个专门存放用户代码的文件夹,如果不改的话会导致第一i给参数映射的文件夹是windows那么把这个文件夹拿到linux去找是找不到会报错的*/
        //这里改为linux代码位置
//        String linuxCodePathName = "/home/zeden/"  + GLOBAL_CODE_DIR_NAME;
//        String linuxUserCodeParentPath = linuxCodePathName + "/" + uid;
        hostConfig.setBinds(new Bind(userCodeParentPath,new Volume("/app")));
        // .withCmd("echo","Hello Docker") 配置创建容器前执行的命令
        // .withAttachStdin(true).withAttachStderr(true).withAttachStderr(true) 把docker和本地的终端获取链接,能获取输入输出
        // .withTty(true) 创建一个交互终端
        CreateContainerResponse createContainerResponse = containerCmd
                .withNetworkDisabled(true) //创建容器时设置网络容器为关闭
                .withReadonlyRootfs(true)
                .withHostConfig(hostConfig)
                .withAttachStdin(true)
                .withAttachStderr(true)
                .withAttachStderr(true)
                .withTty(true)
                .exec();
        System.out.println(createContainerResponse);
        String containerId = createContainerResponse.getId();

        /*启动容器 也是异步，可能容器还没启动就往下继续走了*/
        dockerClient.startContainerCmd(containerId).exec();

        //存放输出结果列表
        List<Executemessage> executemessageList = new ArrayList<>();
        /*执行命令，运行用户代码*/
        //docker exec container_name java -cp /app Main 1 3
        for (String inputArgs : inputList) {

            /*使用stopWatch计算程序运行的时间*/
            StopWatch stopwatch = new StopWatch();

            /*命令用空格分开一个个拼接，否则linux可能会认为没有空格是一个字符串*/
            String[] inputArgsArray = inputArgs.split(" ");
            String[] cmdArray = ArrayUtil.append(new String[]{"java", "-cp", "/app", "Main"}, inputArgsArray);
            //首先创建命令
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withCmd(cmdArray)
                    .withAttachStderr(true)
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .exec();
            System.out.println("创建执行命令: " + execCreateCmdResponse);

            //获取结果
            Executemessage executemessage = new Executemessage();
            final String[] message = {null};
            final String[] errorMessage = {null};
            long time = 0L;

            //默认运行超时，如果没超时的话下面执行完start后就会调用回调函数，然后再complete把变量变成false
            final boolean[] timeout = {true};
            String execId = execCreateCmdResponse.getId();
            //回调函数 获取运行结果
            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {

                @Override
                public void onComplete() {
                    //如果执行完成，表示没超时
                    timeout[0] =false;
                    super.onComplete();
                }

                @Override
                public void onNext(Frame frame) {
                    StreamType streamType = frame.getStreamType();
                    if (StreamType.STDERR.equals(streamType)) {
                        errorMessage[0] = new String(frame.getPayload());
                        System.out.println("输出错误结果: " + new String(frame.getPayload()));
                    } else {
                        message[0] = new String(frame.getPayload());
                        System.out.println("输出结果: " + new String(frame.getPayload()));
                    }
                    super.onNext(frame);
                }
            };

            final long[] maxmemory = {0L};

            /*获取占用的内存*/
            StatsCmd statsCmd = dockerClient.statsCmd(containerId);
            ResultCallback<Statistics> statisticsResultCallback = statsCmd.exec(new ResultCallback<Statistics>() {
                //获取占用内存
                @Override
                public void onNext(Statistics statistics) {
                    System.out.println("内存占用：" + statistics.getMemoryStats().getUsage());
                    maxmemory[0] = Math.max(statistics.getMemoryStats().getUsage(), maxmemory[0]);
                }
                @Override
                public void onStart(Closeable closeable) {

                }
                @Override
                public void onError(Throwable throwable) {

                }
                @Override
                public void onComplete() {

                }
                @Override
                public void close() throws IOException {

                }
            });
            statsCmd.exec(statisticsResultCallback);

            try {
                stopwatch.start();
                //执行命令 要传一个异步回调的函数 awaitCompletion等这个执行完再往下走 在这里设置执行时间，如果超时直接退出 达到超时控制效果
                dockerClient.execStartCmd(execId)
                        .exec(execStartResultCallback)
                        .awaitCompletion(TIME_OUT, TimeUnit.MICROSECONDS);
                stopwatch.stop();
                /*获取程序执行时间*/
                time = stopwatch.getLastTaskTimeMillis();
                statsCmd.close();

            } catch (InterruptedException e) {
                System.out.println("程序执行异常");
                throw new RuntimeException(e);
            }
            //给执行信息赋值
            executemessage.setMessage(message[0]);
            executemessage.setErrorMessage(errorMessage[0]);
            executemessage.setTime(time);
            executemessage.setMemory(maxmemory[0]);
            executemessageList.add(executemessage);
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
