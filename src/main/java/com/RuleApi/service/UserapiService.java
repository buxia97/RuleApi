package com.RuleApi.service;

import java.util.List;
import com.RuleApi.entity.*;
import com.RuleApi.common.PageList;

/**
 * 业务层
 * TypechoUserapiService
 * @author buxia97
 * @date 2022/01/10
 */
public interface UserapiService {

    /**
     * [新增]
     **/
    int insert(TypechoUserapi typechoUserapi);

    /**
     * [批量新增]
     **/
    int batchInsert(List<TypechoUserapi> list);

    /**
     * [更新]
     **/
    int update(TypechoUserapi typechoUserapi);

    /**
     * [删除]
     **/
    int delete(Object key);

    /**
     * [删除]
     **/
    int deleteUserAll(Object key);

    /**
     * [批量删除]
     **/
    int batchDelete(List<Object> keys);

    /**
     * [主键查询]
     **/
    TypechoUserapi selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoUserapi> selectList (TypechoUserapi typechoUserapi);

    /**
     * [分页条件查询]
     **/
    PageList<TypechoUserapi> selectPage (TypechoUserapi typechoUserapi, Integer page, Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(TypechoUserapi typechoUserapi);
}
