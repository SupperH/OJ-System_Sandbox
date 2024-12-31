package com.yupi.yuojcodesandbox.utils;

import cn.hutool.core.util.StrUtil;
import com.yupi.yuojcodesandbox.model.Executemessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.StopWatch;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

//进程工具类
public class ProcessUtils {

    /**
     * 执行进程并获取信息
     * @param runProcess
     * @param opName 操作
     * @return
     */
    public static Executemessage runProcessAndGetMessage(Process runProcess,String opName) {
        Executemessage executemessage = new Executemessage();

        try {

            /*使用StopWatch来计算运行时间 每一个case都要单独计算运行时间*/
            StopWatch stopWatch = new StopWatch();
            //开始计时
            stopWatch.start();

            //等待程序执行完成，然后得到一个状态码
            int exitValue = runProcess.waitFor();
            executemessage.setExitValue(exitValue);

            if (exitValue == 0) {
                System.out.println(opName+"成功");
                /*分批获取程序的输出，也就是获取控制台内容，然后使用bufferedReader包装成块读取内容*/
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                List<String> outputStrList = new ArrayList<>();
                String compileOutputLine;
                //会有很多行信息在控制台，每次只读取一行，用while循环 逐行读取
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    System.out.println(compileOutputLine);
                    outputStrList.add(compileOutputLine);
                }
                //拼接信息后面拼接换行符
                executemessage.setMessage(StringUtils.join(outputStrList,"\n"));
            } else {
                System.out.println(opName+"失败,错误码" + exitValue);
                StringBuilder sb = new StringBuilder();
                StringBuilder errorsb = new StringBuilder();


                /*分批获取程序的正常输出，也就是获取控制台内容，然后使用bufferedReader包装成块读取内容*/
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));


                List<String> outputStrList = new ArrayList<>();
                String compileOutputLine;
                //会有很多行信息在控制台，每次只读取一行，用while循环 逐行读取
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    System.out.println(compileOutputLine);
                    outputStrList.add(compileOutputLine);
                }
                //拼接信息后面拼接换行符
                executemessage.setMessage(StringUtils.join(outputStrList,"\n"));


                /*分批获取程序的错误输出，也就是获取控制台内容，然后使用bufferedReader包装成块读取内容*/
                BufferedReader errorBufferedReader = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));
                List<String> errorOutputStrList = new ArrayList<>();
                String errorCompileOutputLine;
                //会有很多行信息在控制台，每次只读取一行，用while循环 逐行读取
                while ((errorCompileOutputLine = errorBufferedReader.readLine()) != null) {
                    System.out.println(errorCompileOutputLine);
                    errorOutputStrList.add(errorCompileOutputLine);
                }
                //拼接信息后面拼接换行符
                executemessage.setErrorMessage(StringUtils.join(errorOutputStrList,"\n"));
            }

            //停止计时，并获取执行时间
            stopWatch.stop();
            executemessage.setTime(stopWatch.getLastTaskTimeMillis());
        }catch (Exception e){
            e.printStackTrace();
        }
        return executemessage;
    }

    /**
     * 交互式执行进程，也就是说不是读取main方法的arg数组，而是用户使用scanner输入参数
     * @param runProcess 运行进程
     * @param opName
     * @param args 运行需要的参数 ，用户scanner输入
     * @return
     */
    public static Executemessage runInteractProcessAndGetMessage(Process runProcess,String opName, String args) {
        Executemessage executemessage = new Executemessage();

        try {
            /*向控制台输入程序和从arg数组读取不同，这个方法又需要从终端读信息，还需要给终端写入信息所以需要input和output*/
            OutputStream outputStream = runProcess.getOutputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);

            /*这种方式，需要在末尾拼接回车符，否则系统会认为scanner没有按回车 一直卡死*/
            String[] s = args.split(" ");
            String join = StrUtil.join("\n", s) + "\n";
            outputStreamWriter.write(join);
            //flush 相当于按了回车，执行输入的参数
            outputStreamWriter.flush();

            /*分批获取程序的输出，也就是获取控制台内容，然后使用bufferedReader包装成块读取内容*/
            InputStream inputStream = runProcess.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String compileOutputLine;
            StringBuilder sb = new StringBuilder();
            //会有很多行信息在控制台，每次只读取一行，用while循环 逐行读取
            while ((compileOutputLine = bufferedReader.readLine()) != null) {
                System.out.println(compileOutputLine);
                sb.append(compileOutputLine);
            }

            executemessage.setMessage(sb.toString());

            //资源回收 关闭流  不然会卡住
            outputStreamWriter.close();
            outputStream.close();
            inputStream.close();
            runProcess.destroy();

        }catch (Exception e){
            e.printStackTrace();
        }
        return executemessage;
    }

}
