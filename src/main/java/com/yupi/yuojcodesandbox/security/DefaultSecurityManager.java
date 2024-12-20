package com.yupi.yuojcodesandbox.security;

import java.security.Permission;

/**
 * 默认的沙箱安全管理器
 */
public class DefaultSecurityManager extends SecurityManager{

    //默认放开所有权限
    @Override
    public void checkPermission(Permission perm) {
        System.out.println("默认不做任何限制");
        //打印权限
        System.out.println(perm);
    }

    @Override
    public void checkExec(String cmd) {
        super.checkExec(cmd);
    }

    @Override
    public void checkRead(String file, Object context) {
        super.checkRead(file, context);
    }

    @Override
    public void checkWrite(String file) {
        super.checkWrite(file);
    }

    @Override
    public void checkDelete(String file) {
        super.checkDelete(file);
    }

    @Override
    public void checkConnect(String host, int port) {
        super.checkConnect(host, port);
    }


}

