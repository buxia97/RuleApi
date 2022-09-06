package com.RuleApi.dao;

import com.RuleApi.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoAdsDao
 * @author ads
 * @date 2022/09/06
 */
@Mapper
public interface TypechoAdsDao {

    /**
     * [新增]
     **/
    int insert(TypechoAds typechoAds);

    /**
     * [批量新增]
     **/
    int batchInsert(List<TypechoAds> list);

    /**
     * [更新]
     **/
    int update(TypechoAds typechoAds);

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
    TypechoAds selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoAds> selectList (TypechoAds typechoAds);

    /**
     * [分页条件查询]
     **/
    List<TypechoAds> selectPage (@Param("typechoAds") TypechoAds typechoAds, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(TypechoAds typechoAds);
}
