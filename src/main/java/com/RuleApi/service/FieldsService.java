package com.RuleApi.service;

import java.util.List;
import com.RuleApi.entity.*;
import com.RuleApi.common.PageList;

/**
 * 业务层
 * TypechoFieldsService
 * @author buxia97
 * @date 2021/11/29
 */
public interface FieldsService {

    /**
     * [新增]
     **/
    int insert(TypechoFields typechoFields);

    /**
     * [批量新增]
     **/
    int batchInsert(List<TypechoFields> list);

    /**
     * [更新]
     **/
    int update(TypechoFields typechoFields);

    /**
     * [删除]
     **/
    int delete(Integer cid, String name);

    /**
     * [批量删除]
     **/
    int batchDelete(List<Object> keys);

    /**
     * [主键查询]
     **/
    List<TypechoFields> selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoFields> selectList (TypechoFields typechoFields);

    /**
     * [分页条件查询]
     **/
    PageList<TypechoFields> selectPage (TypechoFields typechoFields, Integer page, Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(TypechoFields typechoFields);
}
