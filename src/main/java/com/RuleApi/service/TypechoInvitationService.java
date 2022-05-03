package com.RuleApi.service;

import java.util.Map;
import java.util.List;
import com.RuleApi.entity.*;
import com.RuleApi.common.PageList;

/**
 * 业务层
 * TypechoInvitationService
 * @author invitation
 * @date 2022/05/03
 */
public interface TypechoInvitationService {

    /**
     * [新增]
     **/
    int insert(TypechoInvitation typechoInvitation);

    /**
     * [批量新增]
     **/
    int batchInsert(List<TypechoInvitation> list);

    /**
     * [更新]
     **/
    int update(TypechoInvitation typechoInvitation);

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
    TypechoInvitation selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoInvitation> selectList (TypechoInvitation typechoInvitation);

    /**
     * [分页条件查询]
     **/
    PageList<TypechoInvitation> selectPage (TypechoInvitation typechoInvitation, Integer page, Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(TypechoInvitation typechoInvitation);
}
