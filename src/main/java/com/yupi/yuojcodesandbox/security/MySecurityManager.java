package com.yupi.yuojcodesandbox.security;

import java.security.Permission;

/**
 * 默认的沙箱安全管理器
 */
public class MySecurityManager extends SecurityManager{

    //默认放开所有权限
    @Override
    public void checkPermission(Permission perm) {
    }

    //检测程序是否课执行文件
    @Override
    public void checkExec(String cmd) {
        throw new SecurityException("checkExec权限不足"+cmd);

    }

    //检测程序是否允许读取文件
    @Override
    public void checkRead(String file, Object context) {
        throw new SecurityException("checkRead权限不足"+file);
    }

    //检测程序是否允许写入文件
    @Override
    public void checkWrite(String file) {
        throw new SecurityException("checkWrite权限不足"+file);
    }

    //检测程序是否允许删除文件
    @Override
    public void checkDelete(String file) {
        throw new SecurityException("checkDelete权限不足"+file);
    }

    //检测程序是否允许连接网络
    @Override
    public void checkConnect(String host, int port) {
        throw new SecurityException("checkConnect权限不足"+host + ":" + port);
    }


}

