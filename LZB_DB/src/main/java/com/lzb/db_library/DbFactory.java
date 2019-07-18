package com.lzb.db_library;

import android.database.sqlite.SQLiteDatabase;

/**
 * description:
 */

public class DbFactory {

    private SQLiteDatabase sqLiteDatabase;

    private volatile static DbFactory instance;

    private DbFactory(String path) {
        sqLiteDatabase = SQLiteDatabase.openOrCreateDatabase(path, null);
    }

    public static DbFactory getInstance(String path) {
        if (instance == null) {
            synchronized (DbFactory.class) {
                if (instance == null) {
                    instance = new DbFactory(path);
                }
            }
        }
        return instance;
    }

    public <T extends BaseDao<M>, M> T getBaseDao(Class<T> daoClass, Class<M> entityClass) {
        BaseDao baseDao = null;
        try {
            baseDao = daoClass.newInstance();
            baseDao.init(sqLiteDatabase, entityClass);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return (T) baseDao;
    }
}
