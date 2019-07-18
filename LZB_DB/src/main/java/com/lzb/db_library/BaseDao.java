package com.lzb.db_library;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;


/**
 * description:
 */

public class BaseDao<T> implements IBaseDao<T> {
    private static final String TAG = "BaseDao";
    private SQLiteDatabase sqLiteDatabase;
    private Class<T> entityClass;
    private boolean isInit = false;
    private Map<String, Field> cacheMap;
    private String tableName;

    boolean init(SQLiteDatabase sqLiteDatabase, Class<T> entityClass) {
        this.sqLiteDatabase = sqLiteDatabase;
        this.entityClass = entityClass;
        if (!isInit) {
            if (!sqLiteDatabase.isOpen()) return false;
            //获取表名
            Table table = entityClass.getAnnotation(Table.class);
            tableName = (table != null ?
                    ((TextUtils.isEmpty(table.value()) && table.value().equalsIgnoreCase("null"))
                            ? entityClass.getName() : table.value())
                    : entityClass.getName());
            String sql = createTable();
            Log.i(TAG, "init: sql = " + sql);
            if (TextUtils.isEmpty(sql)) return false;
            sqLiteDatabase.execSQL(sql);
            cacheMap = new HashMap<>();
            initCacheMap(tableName);
            isInit = true;
        }
        return isInit;
    }

    private void initCacheMap(String tableName) {
        String sql = "select * from " + tableName + " limit 1,0";//返回空表
        Cursor cursor = sqLiteDatabase.rawQuery(sql, null);
        String[] columnNames = cursor.getColumnNames();
        Field[] columnFields = entityClass.getDeclaredFields();
        Observable.fromArray(columnNames)
                .subscribe(name ->
                        Observable.fromArray(columnFields)
                                .filter(field -> {
                                    field.setAccessible(true);//设置属性的操作权限
                                    Column column = field.getAnnotation(Column.class);
                                    String fieldName = appendColumn(column, field);
                                    return fieldName.equals(name);
                                }).subscribe(field -> cacheMap.put(name, field))
                );
    }

    /**
     * 拼接sql语句
     *
     * @return 返回sql语句
     */
    private String createTable() {
        if (TextUtils.isEmpty(tableName)) return "";
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("create table if not exists " + tableName + " (");
        //获取类中所有的属性
        Field[] fields = entityClass.getDeclaredFields();
        Observable.fromArray(fields).subscribe(field -> getColumn(field, stringBuffer));
        if (stringBuffer.charAt(stringBuffer.length() - 1) == ',') {//将最后拼接的","去掉
            stringBuffer.deleteCharAt(stringBuffer.length() - 1);
        }
        stringBuffer.append(")");
        return stringBuffer.toString();
    }

    /**
     * 获取sql中的字段
     *
     * @param field        类中的字段
     * @param stringBuffer 拼接
     */
    private void getColumn(Field field, StringBuffer stringBuffer) {
        Observable.just(field)
                .map(f -> f.getType())
                .subscribe(aClass -> {
                    // 获取属性上的注解
                    Column column = field.getAnnotation(Column.class);
                    if (aClass == String.class) {
                        stringBuffer.append(appendColumn(column, field, "TEXT"));
                    } else if (aClass == Integer.class) {
                        stringBuffer.append(appendColumn(column, field, "INTEGER"));
                    } else if (aClass == Double.class) {
                        stringBuffer.append(appendColumn(column, field, "DOUBLE"));
                    } else if (aClass == Long.class) {
                        stringBuffer.append(appendColumn(column, field, "BIGINT"));
                    } else if (aClass == byte[].class) {
                        stringBuffer.append(appendColumn(column, field, "BLOB"));
                    } else {
                        // TODO　其他类型
                    }
                });
    }

    /**
     * 拼接字段
     *
     * @param column 注解
     * @param field  字段
     * @param type   类型
     * @return sql字段
     */
    private String appendColumn(Column column, Field field, String type) {
        return appendColumn(column, field) + " " + type + ",";
    }

    private String appendColumn(Column column, Field field) {
        return ((column != null) ?
                ((TextUtils.isEmpty(column.value()) && column.value().equalsIgnoreCase("null")) ?
                        field.getName() : column.value())
                : field.getName());
    }

    private ContentValues getContentValues(Map<String, String> map) {
        ContentValues contentValues = new ContentValues();
        Observable.fromIterable(map.keySet())
                .subscribe(s -> contentValues.put(s, map.get(s)));
        return contentValues;
    }

    private List<T> getList(Cursor cursor, T t) {
        List list = new ArrayList();
        try {
            final Object item = t.getClass().newInstance();
            while (cursor.moveToNext()) {
                Observable.fromIterable(cacheMap.entrySet())
                        .subscribe(entry -> {
                            //取列名
                            String columnName = entry.getKey();
                            //然后以列名拿到列名在游标中的位置
                            Integer columnIndex = cursor.getColumnIndex(columnName);
                            Field field = entry.getValue();
                            Class type = field.getType();
                            //将查到的数据设置为对应JavaBean属性的值
                            if (columnIndex != -1) {
                                if (type == String.class) {
                                    field.set(item, cursor.getString(columnIndex));
                                } else if (type == Double.class) {
                                    field.set(item, cursor.getDouble(columnIndex));
                                } else if (type == Integer.class) {
                                    field.set(item, cursor.getInt(columnIndex));
                                } else if (type == Long.class) {
                                    field.set(item, cursor.getLong(columnIndex));
                                } else if (type == byte[].class) {
                                    field.set(item, cursor.getBlob(columnIndex));
                                } else {
                                    // TODO　其他类型
                                }
                            }
                        });
                list.add(item);
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        cursor.close();
        return list;
    }
    private Map<String, String> getValues(T entity) {
        Map<String, String> map = new HashMap<>();
        Observable.fromIterable(cacheMap.values())
                .subscribe(field -> {
                    field.setAccessible(true);
                    //获取成员变量的值
                    Object o = field.get(entity);
                    String value = o.toString();
                    //获取表的字段
                    Column column = field.getAnnotation(Column.class);
                    String key = appendColumn(column, field);
                    map.put(key, value);
                });
        return map;
    }

    @Override
    public long insert(T entity) {
        Map<String, String> map = getValues(entity);
        ContentValues contentValues = getContentValues(map);
        return sqLiteDatabase.insert(tableName, null, contentValues);
    }

    @Override
    public long update(T update, T where) {
        Map<String, String> map = getValues(update);
        ContentValues contentValues = getContentValues(map);
        Map<String, String> wheres = getValues(where);
        Where wh = new Where(wheres);
        return sqLiteDatabase.update(tableName, contentValues, wh.whereClause, wh.whereArgs);
    }

    @Override
    public int delete(T where) {
        Map<String, String> wheres = getValues(where);
        Where wh = new Where(wheres);
        return sqLiteDatabase.delete(tableName, wh.whereClause, wh.whereArgs);
    }

    @Override
    public List<T> query(T where) {
        return query(where, null, null, null);
    }

    @Override
    public List<T> query(T where, String orderBy, Integer startIndex, Integer limit) {
        Map<String, String> wheres = getValues(where);
        Where wh = new Where(wheres);
        String limitStr = null;
        if (startIndex != null && limit != null) {
            limitStr = startIndex + " , " + limit;
        }
        Cursor cursor = sqLiteDatabase.query(tableName, null, wh.whereClause, wh.whereArgs,
                null, null, orderBy, limitStr);
        List<T> result = getList(cursor, where);
        return result;
    }


    private class Where {
        String whereClause; //sql中的where条件
        String[] whereArgs; //where条件的值

        Where(Map<String, String> whereClause) {
            List<String> list = new ArrayList<>();//whereArgs里面的内容存入list
            StringBuffer stringBuffer = new StringBuffer();
            if (whereClause != null) {
                stringBuffer.append("1+1");
                Observable.fromIterable(whereClause.keySet())
                        .map(key -> new String[]{key, whereClause.get(key)})
                        .filter(strings -> strings[1] != null)
                        .subscribe(strings -> {
                            stringBuffer.append(" and " + strings[0] + "=?");
                            list.add(strings[1]);
                        });
            }
            this.whereClause = stringBuffer.toString();
            this.whereArgs = (String[]) list.toArray();
        }
    }
}
