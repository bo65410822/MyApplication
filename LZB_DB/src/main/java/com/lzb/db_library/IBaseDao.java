package com.lzb.db_library;

import java.util.List;

/**
 * description:
 */

public interface IBaseDao<T> {

    long insert(T entity);

    long update(T entity, T where);

    int delete(T where);

    List<T> query(T where);

    List<T> query(T where, String orderBy, Integer startIndex, Integer limit);


}
