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
 * TypechoShopServiceImpl
 * @author buxia97
 * @date 2022/01/27
 */
@Service
public class TypechoShopServiceImpl implements TypechoShopService {

	@Autowired
	TypechoShopDao dao;

	@Override
	public int insert(TypechoShop typechoShop) {
		return dao.insert(typechoShop);
	}

	@Override
	public int batchInsert(List<TypechoShop> list) {
		return dao.batchInsert(list);
	}

	@Override
	public int update(TypechoShop typechoShop) {
		return dao.update(typechoShop);
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
	public TypechoShop selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<TypechoShop> selectList(TypechoShop typechoShop) {
		return dao.selectList(typechoShop);
	}

	@Override
	public PageList<TypechoShop> selectPage(TypechoShop typechoShop, Integer offset, Integer pageSize,String searchKey,String order) {
		PageList<TypechoShop> pageList = new PageList<>();

		int total = this.total(typechoShop,searchKey);

		Integer totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<TypechoShop> list = dao.selectPage(typechoShop, page, pageSize,searchKey,order);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(TypechoShop typechoShop,String searchKey) {
		return dao.total(typechoShop,searchKey);
	}
}