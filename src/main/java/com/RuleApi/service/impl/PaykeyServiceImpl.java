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
 * TypechoPaykeyServiceImpl
 * @author paykey
 * @date 2022/04/20
 */
@Service
public class PaykeyServiceImpl implements PaykeyService {

    @Autowired
	TypechoPaykeyDao dao;

    @Override
    public int insert(TypechoPaykey typechoPaykey) {
        return dao.insert(typechoPaykey);
    }

    @Override
    public int batchInsert(List<TypechoPaykey> list) {
    	return dao.batchInsert(list);
    }

    @Override
    public int update(TypechoPaykey typechoPaykey) {
    	return dao.update(typechoPaykey);
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
	public TypechoPaykey selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<TypechoPaykey> selectList(TypechoPaykey typechoPaykey) {
		return dao.selectList(typechoPaykey);
	}

	@Override
	public PageList<TypechoPaykey> selectPage(TypechoPaykey typechoPaykey, Integer offset, Integer pageSize,String searchKey) {
		PageList<TypechoPaykey> pageList = new PageList<>();

		int total = this.total(typechoPaykey);

		Integer totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<TypechoPaykey> list = dao.selectPage(typechoPaykey, page, pageSize,searchKey);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(TypechoPaykey typechoPaykey) {
		return dao.total(typechoPaykey);
	}
}