package com.RuleApi.service;

import java.util.Map;
import java.util.List;
import com.RuleApi.entity.*;
import com.RuleApi.common.PageList;

/**
 * 业务层
 * TypechoFanService
 * @author buxia97
 * @date 2023/01/03
 */
public interface TypechoFanService {

    /**
     * [新增]
     **/
    int insert(TypechoFan typechoFan);

    /**
     * [批量新增]
     **/
    int batchInsert(List<TypechoFan> list);

    /**
     * [更新]
     **/
    int update(TypechoFan typechoFan);

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
    TypechoFan selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoFan> selectList (TypechoFan typechoFan);

    /**
     * [分页条件查询]
     **/
    PageList<TypechoFan> selectPage (TypechoFan typechoFan, Integer page, Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(TypechoFan typechoFan);
}
