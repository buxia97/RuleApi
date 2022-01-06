package com.RuleApi.service;

import java.util.Map;
import java.util.List;
import com.RuleApi.entity.*;
import com.RuleApi.common.PageList;

/**
 * 业务层
 * TypechoUserlogService
 * @author buxia97
 * @date 2022/01/06
 */
public interface TypechoUserlogService {

    /**
     * [新增]
     **/
    int insert(TypechoUserlog typechoUserlog);

    /**
     * [批量新增]
     **/
    int batchInsert(List<TypechoUserlog> list);

    /**
     * [更新]
     **/
    int update(TypechoUserlog typechoUserlog);

    /**
     * [删除]
     **/
    int delete(Object key);

    /**
     * [批量删除]
     **/
    int batchDelete(List<Object> keys);

    /**
     * [主键查询]
     **/
    TypechoUserlog selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoUserlog> selectList (TypechoUserlog typechoUserlog);

    /**
     * [分页条件查询]
     **/
    PageList<TypechoUserlog> selectPage (TypechoUserlog typechoUserlog, Integer page, Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(TypechoUserlog typechoUserlog);
}
