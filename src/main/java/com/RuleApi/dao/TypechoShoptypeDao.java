package com.RuleApi.dao;

import com.RuleApi.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoShoptypeDao
 * @author shoptype
 * @date 2023/07/10
 */
@Mapper
public interface TypechoShoptypeDao {

    /**
     * [新增]
     **/
    int insert(TypechoShoptype typechoShoptype);

    /**
     * [批量新增]
     **/
    int batchInsert(List<TypechoShoptype> list);

    /**
     * [更新]
     **/
    int update(TypechoShoptype typechoShoptype);

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
    TypechoShoptype selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoShoptype> selectList (TypechoShoptype typechoShoptype);

    /**
     * [分页条件查询]
     **/
    List<TypechoShoptype> selectPage (@Param("typechoShoptype") TypechoShoptype typechoShoptype, @Param("page") Integer page, @Param("pageSize") Integer pageSize, @Param("searchKey") String searchKey, @Param("order") String order);

    /**
     * [总量查询]
     **/
    int total(TypechoShoptype typechoShoptype);
}
