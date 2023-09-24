package com.RuleApi.dao;

import com.RuleApi.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoContentsDao
 * @author buxia97
 * @date 2021/11/29
 */
@Mapper
public interface TypechoContentsDao {

    /**
     * [新增]
     **/
    int insert(TypechoContents typechoContents);

    /**
     * [批量新增]
     **/
    int batchInsert(List<TypechoContents> list);

    /**
     * [更新]
     **/
    int update(TypechoContents typechoContents);

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
    TypechoContents selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoContents> selectList (TypechoContents typechoContents);

    /**
     * [分页条件查询]
     **/
    List<TypechoContents> selectPage (@Param("typechoContents") TypechoContents typechoContents, @Param("page") Integer page, @Param("pageSize") Integer pageSize, @Param("searchKey") String searchKey, @Param("order") String order, @Param("random") Integer random);

    /**
     * [总量查询]
     **/
    int total(@Param("typechoContents") TypechoContents typechoContents, @Param("searchKey") String searchKey);

}
