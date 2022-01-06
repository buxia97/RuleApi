package com.RuleApi.dao;

import com.RuleApi.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoUserlogDao
 * @author buxia97
 * @date 2022/01/06
 */
@Mapper
public interface TypechoUserlogDao {

    /**
     * [新增]
     **/
    int insert(TypechoUserlog typechoUserlog);

    /**
     * [批量新增]
     **/
    int batchInsert(List<TypechoUserlog> list);

    /**
     * [更新]
     **/
    int update(TypechoUserlog typechoUserlog);

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
    TypechoUserlog selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoUserlog> selectList (TypechoUserlog typechoUserlog);

    /**
     * [分页条件查询]
     **/
    List<TypechoUserlog> selectPage (@Param("typechoUserlog") TypechoUserlog typechoUserlog, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(TypechoUserlog typechoUserlog);
}
