package com.lzb.core.util;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dalvik.system.DexFile;
import io.reactivex.Observable;

/**
 * author: Lzhb
 * created on: 2019/7/17 16:46
 * description:
 */

public class ClassUtils {
    public static Set<String> getFileNameByPackageName(Application context, final String packageName)
            throws PackageManager.NameNotFoundException, InterruptedException {
        final Set<String> classNames = new HashSet<>();
        List<String> paths = getSourcePaths(context);
        final CountDownLatch countDownLatch = new CountDownLatch(paths.size());
        ExecutorService executorService = Executors.newCachedThreadPool();
        Observable.fromIterable(paths).subscribe(s ->
                executorService.execute(() -> {
                    DexFile dexfile = null;
                    try {
                        dexfile = new DexFile(s);
                        Enumeration<String> dexEntries = dexfile.entries();
                        while (dexEntries.hasMoreElements()) {
                            String className = dexEntries.nextElement();
                            if (className.startsWith(packageName)) {
                                classNames.add(className);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (null != dexfile) {
                            try {
                                dexfile.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        countDownLatch.countDown();
                    }
                })
        );
        //等待执行完成
        countDownLatch.await();
        return classNames;
    }

    private static List<String> getSourcePaths(Application context)
            throws PackageManager.NameNotFoundException {
//        ApplicationInfo applicationInfo = context.getPackageManager()
//                .getApplicationInfo(context.getPackageName(), 0);
        List<String> sourcePaths = new ArrayList<>();
        sourcePaths.add(context.getPackageCodePath());
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            if (null != applicationInfo.splitSourceDirs) {
//                sourcePaths.addAll(Arrays.asList(applicationInfo.splitSourceDirs));
//            }
//        }
        return sourcePaths;
    }
}
