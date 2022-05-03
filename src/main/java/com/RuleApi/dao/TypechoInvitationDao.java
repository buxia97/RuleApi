package com.RuleApi.dao;

import com.RuleApi.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * dao层接口
 * TypechoInvitationDao
 * @author invitation
 * @date 2022/05/03
 */
@Mapper
public interface TypechoInvitationDao {

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
    int batchDelete(List<Object> list);

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
    List<TypechoInvitation> selectPage (@Param("typechoInvitation") TypechoInvitation typechoInvitation, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(TypechoInvitation typechoInvitation);
}
