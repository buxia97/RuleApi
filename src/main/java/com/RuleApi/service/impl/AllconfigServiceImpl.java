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
 * TypechoAllconfigServiceImpl
 * @author config
 * @date 2024/12/16
 */
@Service
public class AllconfigServiceImpl implements AllconfigService {

    @Autowired
	TypechoAllconfigDao dao;

    @Override
    public int insert(TypechoAllconfig typechoAllconfig) {
        return dao.insert(typechoAllconfig);
    }

    @Override
    public int batchInsert(List<TypechoAllconfig> list) {
    	return dao.batchInsert(list);
    }

    @Override
    public int update(TypechoAllconfig typechoAllconfig) {
    	return dao.update(typechoAllconfig);
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
	public TypechoAllconfig selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<TypechoAllconfig> selectList(TypechoAllconfig typechoAllconfig) {
		return dao.selectList(typechoAllconfig);
	}

	@Override
	public PageList<TypechoAllconfig> selectPage(TypechoAllconfig typechoAllconfig, Integer offset, Integer pageSize) {
		PageList<TypechoAllconfig> pageList = new PageList<>();

		int total = this.total(typechoAllconfig);

		Integer totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<TypechoAllconfig> list = dao.selectPage(typechoAllconfig, page, pageSize);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(TypechoAllconfig typechoAllconfig) {
		return dao.total(typechoAllconfig);
	}
}