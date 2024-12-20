package com.yupi.yuojcodesandbox.security;

public class TestSecuritymanager {

    public static void main(String[] args) {
        System.setSecurityManager(new MySecurityManager());
    }
}
