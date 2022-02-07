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
 * TypechoPaylogServiceImpl
 * @author buxia97
 * @date 2022/02/07
 */
@Service
public class TypechoPaylogServiceImpl implements TypechoPaylogService {

    @Autowired
	TypechoPaylogDao dao;

    @Override
    public int insert(TypechoPaylog typechoPaylog) {
        return dao.insert(typechoPaylog);
    }

    @Override
    public int batchInsert(List<TypechoPaylog> list) {
    	return dao.batchInsert(list);
    }

    @Override
    public int update(TypechoPaylog typechoPaylog) {
    	return dao.update(typechoPaylog);
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
	public TypechoPaylog selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<TypechoPaylog> selectList(TypechoPaylog typechoPaylog) {
		return dao.selectList(typechoPaylog);
	}

	@Override
	public PageList<TypechoPaylog> selectPage(TypechoPaylog typechoPaylog, Integer offset, Integer pageSize) {
		PageList<TypechoPaylog> pageList = new PageList<>();

		int total = this.total(typechoPaylog);

		Integer totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<TypechoPaylog> list = dao.selectPage(typechoPaylog, page, pageSize);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(TypechoPaylog typechoPaylog) {
		return dao.total(typechoPaylog);
	}
}