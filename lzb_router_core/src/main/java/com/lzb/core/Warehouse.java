package com.lzb.core;

import com.lzb.annotation.RouteMeta;

import java.util.HashMap;
import java.util.Map;

/**
 * author: Lzhb
 * created on: 2019/7/17 17:08
 * description:
 */

class Warehouse {
    // root 映射表 保存分组信息
    static Map<String, Class<? extends IRouteGroup>> groupsIndex = new HashMap<>();

    // group 映射表 保存组中的所有数据
    static Map<String, RouteMeta> routes = new HashMap<>();

//    // group 映射表 保存组中的所有数据
//    static Map<Class, IService> services = new HashMap<>();
}
