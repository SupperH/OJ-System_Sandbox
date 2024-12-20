package com.yupi.yuojcodesandbox.unsafe;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

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
