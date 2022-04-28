package com.RuleApi.service;

import java.util.Map;
import java.util.List;
import com.RuleApi.entity.*;
import com.RuleApi.common.PageList;
/**
 * 业务层
 * TypechoApiconfigService
 * @author apiconfig
 * @date 2022/04/28
 */
public interface TypechoApiconfigService {

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
