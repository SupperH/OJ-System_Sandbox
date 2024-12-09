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


