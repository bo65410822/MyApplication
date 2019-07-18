package com.lzb.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * author: Lzhb
 * created on: 2019/7/16 16:21
 * description:
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Router {
    /**
     * 路径
     */
    String path();

    /**
     * 分组
     */
    String group() default "";
}
