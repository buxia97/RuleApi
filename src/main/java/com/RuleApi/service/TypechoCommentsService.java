package com.RuleApi.service;

import java.util.Map;
import java.util.List;
import com.RuleApi.entity.*;
import com.RuleApi.common.PageList;

/**
 * 业务层
 * TypechoCommentsService
 * @author buxia97
 * @date 2021/11/29
 */
public interface TypechoCommentsService {

    /**
     * [新增]
     **/
    int insert(TypechoComments typechoComments);

    /**
     * [批量新增]
     **/
    int batchInsert(List<TypechoComments> list);

    /**
     * [更新]
     **/
    int update(TypechoComments typechoComments);

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
    TypechoComments selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoComments> selectList (TypechoComments typechoComments);

    /**
     * [分页条件查询]
     **/
    PageList<TypechoComments> selectPage (TypechoComments typechoComments, Integer page, Integer pageSize,String searchKey,String order);

    /**
     * [总量查询]
     **/
    int total(TypechoComments typechoComments);
}
