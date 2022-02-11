package com.RuleApi.dao;

import com.RuleApi.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoCommentsDao
 * @author buxia97
 * @date 2021/11/29
 */
@Mapper
public interface TypechoCommentsDao {

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
    int batchDelete(List<Object> list);

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
    List<TypechoComments> selectPage (@Param("typechoComments") TypechoComments typechoComments, @Param("page") Integer page, @Param("pageSize") Integer pageSize, @Param("searchKey") String searchKey, @Param("order") String order);

    /**
     * [总量查询]
     **/
    int total(TypechoComments typechoComments);
}
