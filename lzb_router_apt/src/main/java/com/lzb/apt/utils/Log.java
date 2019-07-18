package com.lzb.apt.utils;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;

/**
 * author: Lzhb
 * created on: 2019/7/17 9:54
 * description:
 */

public class Log {

    private Messager messager;

    private Log(Messager messager) {
        this.messager = messager;
    }

    public static Log newLog(Messager messager) {
        return new Log(messager);
    }

    public void i(String msg) {
        messager.printMessage(Diagnostic.Kind.NOTE, msg);
    }
}
