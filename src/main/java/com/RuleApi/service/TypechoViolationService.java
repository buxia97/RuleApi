package com.RuleApi.service;

import java.util.Map;
import java.util.List;
import com.RuleApi.entity.*;
import com.RuleApi.common.PageList;

/**
 * 业务层
 * TypechoViolationService
 * @author buxia97
 * @date 2023/01/03
 */
public interface TypechoViolationService {

    /**
     * [新增]
     **/
    int insert(TypechoViolation typechoViolation);

    /**
     * [批量新增]
     **/
    int batchInsert(List<TypechoViolation> list);

    /**
     * [更新]
     **/
    int update(TypechoViolation typechoViolation);

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
    TypechoViolation selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoViolation> selectList (TypechoViolation typechoViolation);

    /**
     * [分页条件查询]
     **/
    PageList<TypechoViolation> selectPage (TypechoViolation typechoViolation, Integer page, Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(TypechoViolation typechoViolation);
}
