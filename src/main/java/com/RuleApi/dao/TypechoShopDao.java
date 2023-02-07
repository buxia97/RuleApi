package com.RuleApi.dao;

import com.RuleApi.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoShopDao
 * @author buxia97
 * @date 2022/01/27
 */
@Mapper
public interface TypechoShopDao {

    /**
     * [新增]
     **/
    int insert(TypechoShop typechoShop);

    /**
     * [批量新增]
     **/
    int batchInsert(List<TypechoShop> list);

    /**
     * [更新]
     **/
    int update(TypechoShop typechoShop);

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
    TypechoShop selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoShop> selectList (TypechoShop typechoShop);

    /**
     * [分页条件查询]
     **/
    List<TypechoShop> selectPage (@Param("typechoShop") TypechoShop typechoShop, @Param("page") Integer page, @Param("pageSize") Integer pageSize, @Param("searchKey") String searchKey,@Param("order") String order);

    /**
     * [总量查询]
     **/
    int total(TypechoShop typechoShop);
}
