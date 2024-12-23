package com.yupi.yuojcodesandbox.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.LogContainerResultCallback;

import java.util.List;

public class DockerDemo {
    public static void main(String[] args) throws InterruptedException {
        // Docker 远程服务器地址
        String dockerHost = "tcp://192.168.158.128:2375"; // 确保远程服务器开放了 2375 端口
        // 配置 Docker 客户端
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHost) // 设置 Docker 守护程序地址
                .build();
        // 获取 Docker 客户端
        DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();

        /* 测试连接*/
//
//        PingCmd pingCmd = dockerClient.pingCmd();
//        pingCmd.exec();

        /*拉取镜像*/
        String image = "nginx:latest";
//        PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
//        PullImageResultCallback pullImageResultCallback = new PullImageResultCallback(){
//            @Override
//            public void onNext(PullResponseItem item) {
//                System.out.println("下载镜像"+item.getStatus());
//                super.onNext(item);
//            }
//        };
//        //awaitCompletion等待运行结束
//        pullImageCmd.exec(pullImageResultCallback).awaitCompletion();
//        System.out.println("下载完成");

        /*创建容器-理解为创建一套运行的环境*/
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
        // .withCmd("echo","Hello Docker") 配置创建容器前执行的命令
        CreateContainerResponse createContainerResponse = containerCmd.withCmd("echo","Hello Docker").exec();
        System.out.println(createContainerResponse);
        String containerId = createContainerResponse.getId();

        /*查看容器状态*/
        ListContainersCmd listContainersCmd = dockerClient.listContainersCmd();
        //withShowAll 显示所有容器
        List<Container> exec = listContainersCmd.withShowAll(true).exec();
        for (Container container : exec) {
            System.out.println(container);
        }

        /*启动容器 也是异步，可能容器还没启动就往下继续走了*/
        dockerClient.startContainerCmd(containerId).exec();

        /*查看日志，异步读取分批读取，因为日志很多不可能等待读完*/
        LogContainerResultCallback logContainerResultCallback = new LogContainerResultCallback(){
            @Override
            public void onNext(Frame item) {
                System.out.println("日志" + item.getPayload());
                super.onNext(item);
            }
        };
        //withStdOut(true).withStdErr(true) 输出正常日志和错误日志 awaitCompletion 阻塞等待运行结束
        dockerClient.logContainerCmd(containerId).withStdOut(true).withStdErr(true).exec(logContainerResultCallback).awaitCompletion();

        /*删除容器 withForce:强制删除 -f命令*/
//        dockerClient.removeContainerCmd(containerId).withForce(true).exec();

        /*删除镜像*/
//        dockerClient.removeImageCmd(image).exec();

    }
}
