package com.yupi.yuojcodesandbox.utils;

import com.yupi.yuojcodesandbox.model.Executemessage;

import java.io.BufferedReader;
import java.io.InputStreamReader;

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
            //等待程序执行完成，然后得到一个状态码
            int exitValue = runProcess.waitFor();
            executemessage.setExitValue(exitValue);

            if (exitValue == 0) {
                System.out.println(opName+"成功");
                StringBuilder sb = new StringBuilder();

                /*分批获取程序的输出，也就是获取控制台内容，然后使用bufferedReader包装成块读取内容*/
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                String compileOutputLine;
                //会有很多行信息在控制台，每次只读取一行，用while循环 逐行读取
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    System.out.println(compileOutputLine);
                    sb.append(compileOutputLine);
                }

                executemessage.setMessage(sb.toString());
            } else {
                System.out.println(opName+"失败,错误码" + exitValue);
                StringBuilder sb = new StringBuilder();
                StringBuilder errorsb = new StringBuilder();


                /*分批获取程序的正常输出，也就是获取控制台内容，然后使用bufferedReader包装成块读取内容*/
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                String compileOutputLine;
                //会有很多行信息在控制台，每次只读取一行，用while循环 逐行读取
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    System.out.println(compileOutputLine);
                    sb.append(compileOutputLine);
                }

                /*分批获取程序的错误输出，也就是获取控制台内容，然后使用bufferedReader包装成块读取内容*/
                BufferedReader errorBufferedReader = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));
                String errorCompileOutputLine;
                //会有很多行信息在控制台，每次只读取一行，用while循环 逐行读取
                while ((errorCompileOutputLine = errorBufferedReader.readLine()) != null) {
                    System.out.println(errorCompileOutputLine);
                    errorsb.append(errorCompileOutputLine);
                }
                executemessage.setErrorMessage(errorsb.toString());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return executemessage;
    }
}
