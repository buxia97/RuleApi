package com.RuleApi.dao;

import com.RuleApi.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoViolationDao
 * @author buxia97
 * @date 2023/01/03
 */
@Mapper
public interface TypechoViolationDao {

    /**
     * [新增]
     **/
    int insert(TypechoViolation typechoViolation);

    /**
     * [批量新增]
     **/
    int batchInsert(List<TypechoViolation> list);

    /**
     * [更新]
     **/
    int update(TypechoViolation typechoViolation);

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
    TypechoViolation selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoViolation> selectList (TypechoViolation typechoViolation);

    /**
     * [分页条件查询]
     **/
    List<TypechoViolation> selectPage (@Param("typechoViolation") TypechoViolation typechoViolation, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(TypechoViolation typechoViolation);
}
