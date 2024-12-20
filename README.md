# 代码沙箱接口

## Java原生实现代码沙箱

原生：尽可能不借助第三方库和依赖，用最干净最原始的方式实现
代码沙箱需要: 接收代码 - 编译代码（javac） - 执行代码（java） java -cp . SimpleCompute 1 2

需要注意的是 如果是中文的输出 要关注输出的编码格式，通过chcp可以看到控制台的编码格式，默认是936 GBK编码 UTF-8是195001
但是这种只适用于自己的控制台，如果操作的电脑和环境不一样这种方法就无法通用，所以在javac编译的时候 加上-encoding utf-8参数   javac -encoding utf-8 .\SimpleCompute.java

实际OJ系统，对用户输入代码会有一定要求，便于统一处理，所以把用户输入代码的类名限制为Main，参考清华大学的oj系统

## 核心流程实现
用程序代替人工，用程序操作命令行，去编译执行代码
java进程执行管理类：Process

1.把用户代码保存为文件
2.编译代码，得到class文件
3.执行代码，得到输出结果
4.收集整理输出结果
5.文件清理
6.错误处理，提升程序健壮性

### 1.把用户代码保存为文件
 原理就是读取传入的代码转为字符串，写入指定文件中，这里需要注意的是，每次用户提交需要额外创建一个文件夹，因为用户不可能只提交一次代码，这样可以区分不同的提交
 可以参考JavaNativeCodeSandbox中的代码
 
### 2.编译代码，得到class文件
java执行程序：            Process exec = Runtime.getRuntime().exec(compileCmd);
java获取控制台输出          BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(compileProcess.getInputStream()));


### 3.执行程序
第一个占位符：路径 第二个占位符 执行代码需要的输入参数 -Dfile.encoding=UTF-8 设置运行时编码格式为utf-8   
String runCmd = String.format("java -Dfile.encoding=UTF-8 -cp %s Main %s",userCodeParentPath,inputArgs);

可以使用scanner和用户交互方式，让用户不断输入内容并获取输出 方法是ProcessUtils.runInteractProcessAndGetMessage

### 4.整理输出
获取程序执行时间，使用spring的StopWatch获取一段程序的执行时间 这里用最大值执行时间判断程序是否超时


### 5.文件清理
        if(userCodeFile.getParentFile() !=null){
            //这里用的是hutu工具包
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除"+(del?"成功":"失败"));
        }

### 6.错误处理，提升程序健壮性
创建错误处理方法，然后在catch处return

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


## 防止恶意代码
 从时间上攻击，比如无限睡眠
 从空间上攻击，比如无限循环占用内存不释放
 读取服务器文件导致文件泄露
 向服务器写文件，植入危险程序
 运行其他程序，比如木马程序

```java
 package com.yupi.yuojcodesandbox.unsafe;
import java.util.ArrayList;
import java.util.List;
//无限占用空间
public class MemoryError {

    public static void main(String[] args) throws InterruptedException {
        List<byte[]> bytes = new ArrayList<>();

        while(true){
            bytes.add(new byte[10000]);
        }
    }

}
```

实际运行中，会发现，内存占用到达一定空间后程序就会自动报错 oom 这是jvm的一个保护机制
JVisualJvm或Jconsole工具可以连接到jvm虚拟机上来可视化查看运行状态 jconsole在jdk安装bin目录下

```java
//文件泄露 读取服务器文件
public class Main {

    public static void main(String[] args) throws InterruptedException, IOException {
        String userDir = System.getProperty("user.dir");

        String filePath = userDir + File.separator + "src/main/resources/application.yml";
        List<String> allLines = Files.readAllLines(Paths.get(filePath));
        System.out.println(allLines);

    }

}
```

```java
//向服务器写文件，植入危险程序
public class WriteFileError {

    public static void main(String[] args) throws InterruptedException, IOException {
        String userDir = System.getProperty("user.dir");

        String filePath = userDir + File.separator + "src/main/resources/木马程序.bat";
        String errorProgram = "java -version 2>&1";
        Files.write(Paths.get(filePath), Arrays.asList(errorProgram));
        System.out.println("写入程序成功");
    }

}
```

```java
//运行其他程序，比如木马程序
public class RunFileError {

    public static void main(String[] args) throws InterruptedException, IOException {
        String userDir = System.getProperty("user.dir");

        String filePath = userDir + File.separator + "src/main/resources/木马程序.bat";
        Process proces = Runtime.getRuntime().exec(filePath);
        proces.waitFor();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(proces.getInputStream()));

        String compileOutputLine;
        while((compileOutputLine = bufferedReader.readLine()) !=null){
            System.out.println(compileOutputLine);
        }
        System.out.println("执行异常程序成功");
    }
}
```





### 解决方法
1） 超时控制 通过创建一个守护线程，超时后自动中断process

2） 限制给用户分配的资源 不能让每个java进程的执行占用JVM最大堆内存空间都和系统一致，应该小一点比如256MB
     在启动java时 可以指定内存大小 -Xmx256m（最大） -xms（最小）
     注意！ -Xmx参数，jvm的堆内存限制，不等同于系统实际占用的最大直言，可能会超出，如果需要更严格的内存限制，要在系统层面限制而不是jvm
     如果是linux，可以使用cgroup来实现对某个进程的cpu，内存等资源的分配

3） 限制代码 -黑白名单
     先定义一个黑白名单，比如哪些操作禁止 HuTool字典树工具类 WordTree，可以用更少的空间存储更多的敏感词汇，以及实现更高效的敏感词查找  **这个可以写在简历上**
 ![](G:\JavaProjects\OJ\yuoj-code-sandbox\src\image\WordTree原理.jpg)
    **缺点：**  无法遍历所有的黑名单，不同的编程语言，对应的领域，关键词都不一样，限制人工成本很大

4） 限制用户的操作权限（文件，网络，执行）
    java安全管理器来实现更严格的限制 security manager   是java提供的保护jvm java安全的机制，可以实现更严格的资源操作限制
    限制用户对文件，内存，cpu，网络等资源的操作和访问
    继承SecurityManager接口  实际情况下，我们只需要限制子程序的权限即可，不用限制开发者自己写的程序，也就是说限制的代码加在允许用户代码的时候即可
    在允许java程序时，指定安全管理器：
        java -Dfile.encoding=UTF-8 -cp %s;%s -Djava.security.manager=MySecurityManager Main
        **注意：SecurityManager类在jdk17以及被废弃，所以这个方法只适用于17以下的环境！！！！！！**
        **缺点：如果要做比较严格的权限限制，要自己判断哪些文件需要控制读写，粒度太细，难以精细化控制**

5） 运行环境隔离
    系统层面上，把用户程序封装到沙箱，和宿主机隔离开 使用Docker容器技术实现（底层使用cgroup，namespace等方式实现的）