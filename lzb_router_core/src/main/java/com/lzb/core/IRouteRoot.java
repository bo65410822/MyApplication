package com.lzb.core;

import java.util.Map;

/**
 * author: Lzhb
 * created on: 2019/7/17 13:23
 * description:
 */

public interface IRouteRoot {
    /**
     * @param routes input
     */
    void loadInto(Map<String, Class<? extends IRouteGroup>> routes);
}
