package com.RuleApi.dao;

import com.RuleApi.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoEmailtemplateDao
 * @author buxia97
 * @date 2023/10/06
 */
@Mapper
public interface TypechoEmailtemplateDao {

    /**
     * [新增]
     **/
    int insert(TypechoEmailtemplate typechoEmailtemplate);

    /**
     * [批量新增]
     **/
    int batchInsert(List<TypechoEmailtemplate> list);

    /**
     * [更新]
     **/
    int update(TypechoEmailtemplate typechoEmailtemplate);

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
    TypechoEmailtemplate selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoEmailtemplate> selectList (TypechoEmailtemplate typechoEmailtemplate);

    /**
     * [分页条件查询]
     **/
    List<TypechoEmailtemplate> selectPage (@Param("typechoEmailtemplate") TypechoEmailtemplate typechoEmailtemplate, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(TypechoEmailtemplate typechoEmailtemplate);
}
