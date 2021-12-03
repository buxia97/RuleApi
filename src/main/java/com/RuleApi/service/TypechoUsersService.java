package com.RuleApi.service;

import java.util.Map;
import java.util.List;
import com.RuleApi.entity.*;
import com.RuleApi.common.PageList;

/**
 * 业务层
 * TypechoUsersService
 * @author buxia97
 * @date 2021/11/29
 */
public interface TypechoUsersService {

    /**
     * [新增]
     **/
    int insert(TypechoUsers typechoUsers);

    /**
     * [批量新增]
     **/
    int batchInsert(List<TypechoUsers> list);

    /**
     * [更新]
     **/
    int update(TypechoUsers typechoUsers);

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
    TypechoUsers selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoUsers> selectList (TypechoUsers typechoUsers);

    /**
     * [分页条件查询]
     **/
    PageList<TypechoUsers> selectPage (TypechoUsers typechoUsers, Integer page, Integer pageSize,String searchKey,String order);

    /**
     * [总量查询]
     **/
    int total(TypechoUsers typechoUsers);
}
