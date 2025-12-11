package com.RuleApi.service;

import java.util.List;
import com.RuleApi.entity.*;
import com.RuleApi.common.PageList;

/**
 * 业务层
 * TypechoEmailtemplateService
 * @author buxia97
 * @date 2023/10/06
 */
public interface EmailtemplateService {

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
    int batchDelete(List<Object> keys);

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
    PageList<TypechoEmailtemplate> selectPage (TypechoEmailtemplate typechoEmailtemplate, Integer page, Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(TypechoEmailtemplate typechoEmailtemplate);
}
