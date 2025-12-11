package com.RuleApi.service;

import java.util.List;
import com.RuleApi.entity.*;
import com.RuleApi.common.PageList;

/**
 * 业务层
 * TypechoFilesService
 * @author files
 * @date 2025/09/13
 */
public interface FilesService {

    /**
     * [新增]
     **/
    int insert(TypechoFiles typechoFiles);

    /**
     * [批量新增]
     **/
    int batchInsert(List<TypechoFiles> list);

    /**
     * [更新]
     **/
    int update(TypechoFiles typechoFiles);

    /**
     * [删除]
     **/
    int delete(Object key);

    /**
     * [批量删除]
     **/
    int batchDelete(List<Object> keys);

    /**
     * [主键查询]
     **/
    TypechoFiles selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoFiles> selectList (TypechoFiles typechoFiles);

    /**
     * [分页条件查询]
     **/
    PageList<TypechoFiles> selectPage (TypechoFiles typechoFiles, Integer page, Integer pageSize,String searchKey,String order);

    /**
     * [总量查询]
     **/
    int total(TypechoFiles typechoFiles,String searchKey);
}
