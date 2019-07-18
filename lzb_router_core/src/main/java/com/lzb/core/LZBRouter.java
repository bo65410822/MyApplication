package com.lzb.core;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import com.lzb.core.util.ClassUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;


/**
 * author: Lzhb
 * created on: 2019/7/17 16:36
 * description:
 */

public final class LZBRouter {
    private static final String ROUTE_ROOT_PACKAGE = "com.lzb.router";
    private static final String SDK_NAME = "LZBRouter";
    private static final String SEPARATOR = "$$";
    private static final String SUFFIX_ROOT = "Root";
    private static LZBRouter instance;
    private Context mContext;

    private LZBRouter() {
    }

    public static LZBRouter getInstance() {
        if (null == instance) {
            synchronized (LZBRouter.class) {
                if (null == instance) {
                    instance = new LZBRouter();
                }
            }
        }
        return instance;
    }

    public void init(Application application) {
        mContext = application;
        try {
            loadInfo();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadInfo() throws PackageManager.NameNotFoundException,
            InterruptedException, ClassNotFoundException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException, InstantiationException {
        //获得所有 apt生成的路由类的全类名 (路由表)
        Set<String> routerSet =
                ClassUtils.getFileNameByPackageName((Application) mContext, ROUTE_ROOT_PACKAGE);
        for (String className : routerSet) {
            Log.e("-------", "loadInfo: className = " + className);
            if (className.startsWith(ROUTE_ROOT_PACKAGE + "." + SDK_NAME + SEPARATOR +
                    SUFFIX_ROOT)) {
                ((IRouteRoot) Class.forName(className).getConstructor().newInstance())
                        .loadInto(Warehouse.groupsIndex);
            }
        }
    }

    public void navigation(String path, Context context) {
        String group = path.substring(1, path.indexOf("/", 1));
        Class<? extends IRouteGroup> aClass = Warehouse.groupsIndex.get(group);
        IRouteGroup routeGroup = null;
        try {
            routeGroup = aClass.getConstructor().newInstance();
            routeGroup.loadInto(Warehouse.routes);
            context.startActivity(new Intent(context, Warehouse.routes.get(group).getDestination()));
        } catch (Exception e) {
            Log.e("---------", "navigation: e = " + e.getMessage());
            e.printStackTrace();
        }
    }
}
