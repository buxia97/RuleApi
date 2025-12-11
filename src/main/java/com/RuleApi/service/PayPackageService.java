package com.RuleApi.service;

import java.util.List;
import com.RuleApi.entity.*;
import com.RuleApi.common.PageList;

/**
 * 业务层
 * TypechoPayPackageService
 * @author pay
 * @date 2025/10/18
 */
public interface PayPackageService {

    /**
     * [新增]
     **/
    int insert(TypechoPayPackage typechoPayPackage);

    /**
     * [批量新增]
     **/
    int batchInsert(List<TypechoPayPackage> list);

    /**
     * [更新]
     **/
    int update(TypechoPayPackage typechoPayPackage);

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
    TypechoPayPackage selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoPayPackage> selectList (TypechoPayPackage typechoPayPackage);

    /**
     * [分页条件查询]
     **/
    PageList<TypechoPayPackage> selectPage (TypechoPayPackage typechoPayPackage, Integer page, Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(TypechoPayPackage typechoPayPackage);
}
