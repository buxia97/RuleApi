package com.RuleApi.service.impl;

import com.RuleApi.entity.*;
import com.RuleApi.common.PageList;
import com.RuleApi.dao.*;
import com.RuleApi.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 业务层实现类
 * TypechoRelationshipsServiceImpl
 * @author buxia97
 * @date 2021/11/29
 */
@Service
public class RelationshipsServiceImpl implements RelationshipsService {

    @Autowired
	TypechoRelationshipsDao dao;

    @Override
    public int insert(TypechoRelationships typechoRelationships) {
        return dao.insert(typechoRelationships);
    }

    @Override
    public int batchInsert(List<TypechoRelationships> list) {
    	return dao.batchInsert(list);
    }

    @Override
    public int update(TypechoRelationships typechoRelationships) {
    	return dao.update(typechoRelationships);
    }

    @Override
    public int delete(Object key) {
    	return dao.delete(key);
    }

    @Override
    public int batchDelete(List<Object> keys) {
        return dao.batchDelete(keys);
    }

	@Override
	public List<TypechoRelationships> selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<TypechoRelationships> selectList(TypechoRelationships typechoRelationships) {
		return dao.selectList(typechoRelationships);
	}

	@Override
	public PageList<TypechoRelationships> selectPage(TypechoRelationships typechoRelationships, Integer offset, Integer pageSize,String order) {
		PageList<TypechoRelationships> pageList = new PageList<>();

		int total = this.total(typechoRelationships);

		Integer totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<TypechoRelationships> list = dao.selectPage(typechoRelationships, page, pageSize,order);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(TypechoRelationships typechoRelationships) {
		return dao.total(typechoRelationships);
	}
}