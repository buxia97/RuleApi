package com.RuleApi.dao;

import com.RuleApi.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoAppDao
 * @author vips
 * @date 2023/06/09
 */
@Mapper
public interface TypechoAppDao {

    /**
     * [新增]
     **/
    int insert(TypechoApp typechoApp);

    /**
     * [批量新增]
     **/
    int batchInsert(List<TypechoApp> list);

    /**
     * [更新]
     **/
    int update(TypechoApp typechoApp);

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
    TypechoApp selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoApp> selectList (TypechoApp typechoApp);

    /**
     * [分页条件查询]
     **/
    List<TypechoApp> selectPage (@Param("typechoApp") TypechoApp typechoApp, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(TypechoApp typechoApp);
}
