package com.RuleApi.service;

import java.util.Map;
import java.util.List;
import com.RuleApi.entity.*;
import com.RuleApi.common.PageList;
/**
 * 业务层
 * TypechoPaykeyService
 * @author paykey
 * @date 2022/04/20
 */
public interface TypechoPaykeyService {

    /**
     * [新增]
     **/
    int insert(TypechoPaykey typechoPaykey);

    /**
     * [批量新增]
     **/
    int batchInsert(List<TypechoPaykey> list);

    /**
     * [更新]
     **/
    int update(TypechoPaykey typechoPaykey);

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
    TypechoPaykey selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoPaykey> selectList (TypechoPaykey typechoPaykey);

    /**
     * [分页条件查询]
     **/
    PageList<TypechoPaykey> selectPage (TypechoPaykey typechoPaykey, Integer page, Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(TypechoPaykey typechoPaykey);
}
