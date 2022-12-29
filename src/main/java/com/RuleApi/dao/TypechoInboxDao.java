package com.RuleApi.dao;

import com.RuleApi.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoInboxDao
 * @author inbox
 * @date 2022/12/29
 */
@Mapper
public interface TypechoInboxDao {

    /**
     * [新增]
     **/
    int insert(TypechoInbox typechoInbox);

    /**
     * [更新]
     **/
    int update(TypechoInbox typechoInbox);

    /**
     * [删除]
     **/
    int delete(Object key);

    /**
     * [主键查询]
     **/
    TypechoInbox selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoInbox> selectList (TypechoInbox typechoInbox);

    /**
     * [分页条件查询]
     **/
    List<TypechoInbox> selectPage (@Param("typechoInbox") TypechoInbox typechoInbox, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(TypechoInbox typechoInbox);
}
