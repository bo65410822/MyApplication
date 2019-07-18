package com.lzb.apt.utils;

import java.util.Collection;
import java.util.Map;

/**
 * author: Lzhb
 * created on: 2019/7/17 10:02
 * description:
 */

public class Utils {

    public static boolean isEmpty(CharSequence s) {
        return s == null || s.length() == 0;
    }

    public static boolean isEmpty(Collection<?> coll) {
        return coll == null || coll.isEmpty();
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

}
