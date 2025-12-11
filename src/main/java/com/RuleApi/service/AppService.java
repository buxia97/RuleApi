package com.RuleApi.service;

import java.util.List;
import com.RuleApi.entity.*;
import com.RuleApi.common.PageList;

/**
 * 业务层
 * TypechoAppService
 * @author vips
 * @date 2023/06/09
 */
public interface AppService {

    /**
     * [新增]
     **/
    int insert(TypechoApp typechoApp);

    /**
     * [批量新增]
     **/
    int batchInsert(List<TypechoApp> list);

    /**
     * [更新]
     **/
    int update(TypechoApp typechoApp);

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
    TypechoApp selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoApp> selectList (TypechoApp typechoApp);

    /**
     * [分页条件查询]
     **/
    PageList<TypechoApp> selectPage (TypechoApp typechoApp, Integer page, Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(TypechoApp typechoApp);
}
