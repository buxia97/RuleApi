package com.RuleApi.service;

import java.util.Map;
import java.util.List;
import com.RuleApi.entity.*;
import com.RuleApi.common.PageList;

/**
 * 业务层
 * TypechoInboxService
 * @author inbox
 * @date 2022/12/29
 */
public interface TypechoInboxService {

    /**
     * [新增]
     **/
    int insert(TypechoInbox typechoInbox);


    /**
     * [更新]
     **/
    int update(TypechoInbox typechoInbox);

    /**
     * [删除]
     **/
    int delete(Object key);

    /**
     * [主键查询]
     **/
    TypechoInbox selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoInbox> selectList (TypechoInbox typechoInbox);

    /**
     * [分页条件查询]
     **/
    PageList<TypechoInbox> selectPage (TypechoInbox typechoInbox, Integer page, Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(TypechoInbox typechoInbox);
}
