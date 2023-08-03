package com.RuleApi.service;

import java.util.Map;
import java.util.List;
import com.RuleApi.entity.*;
import com.RuleApi.common.PageList;

/**
 * 业务层
 * TypechoShoptypeService
 * @author shoptype
 * @date 2023/07/10
 */
public interface TypechoShoptypeService {

    /**
     * [新增]
     **/
    int insert(TypechoShoptype typechoShoptype);

    /**
     * [批量新增]
     **/
    int batchInsert(List<TypechoShoptype> list);

    /**
     * [更新]
     **/
    int update(TypechoShoptype typechoShoptype);

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
    TypechoShoptype selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoShoptype> selectList (TypechoShoptype typechoShoptype);

    /**
     * [分页条件查询]
     **/
    PageList<TypechoShoptype> selectPage (TypechoShoptype typechoShoptype, Integer page, Integer pageSize,String searchKey,String order);

    /**
     * [总量查询]
     **/
    int total(TypechoShoptype typechoShoptype);
}
