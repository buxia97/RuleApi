package com.RuleApi.service;

import java.util.List;
import com.RuleApi.entity.*;
import com.RuleApi.common.PageList;

/**
 * 业务层
 * TypechoSpaceService
 * @author buxia97
 * @date 2023/02/05
 */
public interface SpaceService {

    /**
     * [新增]
     **/
    int insert(TypechoSpace typechoSpace);

    /**
     * [批量新增]
     **/
    int batchInsert(List<TypechoSpace> list);

    /**
     * [更新]
     **/
    int update(TypechoSpace typechoSpace);

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
    TypechoSpace selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoSpace> selectList (TypechoSpace typechoSpace);

    /**
     * [分页条件查询]
     **/
    PageList<TypechoSpace> selectPage (TypechoSpace typechoSpace, Integer page, Integer pageSize,String order,String searchKey,Integer isReply);

    /**
     * [总量查询]
     **/
    int total(TypechoSpace typechoSpace,String searchKey);
}
