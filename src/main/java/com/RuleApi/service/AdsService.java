package com.RuleApi.service;

import java.util.List;
import com.RuleApi.entity.*;
import com.RuleApi.common.PageList;

/**
 * 业务层
 * TypechoAdsService
 * @author ads
 * @date 2022/09/06
 */
public interface AdsService {

    /**
     * [新增]
     **/
    int insert(TypechoAds typechoAds);
    /**
     * [更新]
     **/
    int update(TypechoAds typechoAds);

    /**
     * [删除]
     **/
    int delete(Object key);

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
    PageList<TypechoAds> selectPage (TypechoAds typechoAds, Integer page, Integer pageSize,String searchKey);

    /**
     * [总量查询]
     **/
    int total(TypechoAds typechoAds);
}
