package com.RuleApi.service;

import com.RuleApi.entity.*;

/**
 * 业务层
 * TypechoApiconfigService
 * @author apiconfig
 * @date 2022/04/28
 */
public interface ApiconfigService {

    /**
     * [新增]
     **/
    int insert(TypechoApiconfig typechoApiconfig);


    /**
     * [更新]
     **/
    int update(TypechoApiconfig typechoApiconfig);


    /**
     * [主键查询]
     **/
    TypechoApiconfig selectByKey(Object key);

}
