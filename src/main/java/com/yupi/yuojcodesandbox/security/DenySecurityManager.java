package com.yupi.yuojcodesandbox.security;

import java.security.Permission;

/**
 * 默认的沙箱安全管理器
 */
public class DenySecurityManager extends SecurityManager{

    //检查所有权限，如果有不合规的直接报错
    @Override
    public void checkPermission(Permission perm) {
        throw new SecurityException("权限不足"+perm.getActions());
    }
}

