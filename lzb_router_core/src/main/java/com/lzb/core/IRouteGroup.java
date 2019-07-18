package com.lzb.core;

import com.lzb.annotation.RouteMeta;

import java.util.Map;

/**
 * author: Lzhb
 * created on: 2019/7/17 13:23
 * description:
 */

public interface IRouteGroup {
    void loadInto(Map<String, RouteMeta> atlas);
}
