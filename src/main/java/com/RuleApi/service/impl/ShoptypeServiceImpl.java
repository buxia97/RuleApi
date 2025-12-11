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
 * TypechoShoptypeServiceImpl
 * @author shoptype
 * @date 2023/07/10
 */
@Service
public class ShoptypeServiceImpl implements ShoptypeService {

    @Autowired
	TypechoShoptypeDao dao;

    @Override
    public int insert(TypechoShoptype typechoShoptype) {
        return dao.insert(typechoShoptype);
    }

    @Override
    public int batchInsert(List<TypechoShoptype> list) {
    	return dao.batchInsert(list);
    }

    @Override
    public int update(TypechoShoptype typechoShoptype) {
    	return dao.update(typechoShoptype);
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
	public TypechoShoptype selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<TypechoShoptype> selectList(TypechoShoptype typechoShoptype) {
		return dao.selectList(typechoShoptype);
	}

	@Override
	public PageList<TypechoShoptype> selectPage(TypechoShoptype typechoShoptype, Integer offset, Integer pageSize, String searchKey, String order) {
		PageList<TypechoShoptype> pageList = new PageList<>();

		int total = this.total(typechoShoptype);

		Integer totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<TypechoShoptype> list = dao.selectPage(typechoShoptype, page, pageSize, searchKey,order);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(TypechoShoptype typechoShoptype) {
		return dao.total(typechoShoptype);
	}
}