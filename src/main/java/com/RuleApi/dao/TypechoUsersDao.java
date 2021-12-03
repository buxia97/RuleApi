package com.RuleApi.dao;

import com.RuleApi.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoUsersDao
 * @author buxia97
 * @date 2021/11/29
 */
@Mapper
public interface TypechoUsersDao {

    /**
     * [新增]
     **/
    int insert(TypechoUsers typechoUsers);

    /**
     * [批量新增]
     **/
    int batchInsert(List<TypechoUsers> list);

    /**
     * [更新]
     **/
    int update(TypechoUsers typechoUsers);

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
    TypechoUsers selectByKey(Object key);



    /**
     * [条件查询]
     **/
    List<TypechoUsers> selectList (TypechoUsers typechoUsers);

    /**
     * [分页条件查询]
     **/
    List<TypechoUsers> selectPage (@Param("typechoUsers") TypechoUsers typechoUsers, @Param("page") Integer page, @Param("pageSize") Integer pageSize, @Param("searchKey") String searchKey, @Param("order") String order);

    /**
     * [总量查询]
     **/
    int total(TypechoUsers typechoUsers);
}
