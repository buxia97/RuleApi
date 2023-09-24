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
 * TypechoContentsServiceImpl
 * @author buxia97
 * @date 2021/11/29
 */
@Service
public class TypechoContentsServiceImpl implements TypechoContentsService {

	@Autowired
	TypechoContentsDao dao;

	@Override
	public int insert(TypechoContents typechoContents) {
		return dao.insert(typechoContents);
	}

	@Override
	public int batchInsert(List<TypechoContents> list) {
		return dao.batchInsert(list);
	}

	@Override
	public int update(TypechoContents typechoContents) {
		return dao.update(typechoContents);
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
	public TypechoContents selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<TypechoContents> selectList(TypechoContents typechoContents) {
		return dao.selectList(typechoContents);
	}

	@Override
	public PageList<TypechoContents> selectPage(TypechoContents typechoContents, Integer offset, Integer pageSize ,String searchKey,String order,Integer random) {
		PageList<TypechoContents> pageList = new PageList<>();

		int total = this.total(typechoContents,searchKey);

		Integer totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<TypechoContents> list = dao.selectPage(typechoContents, page, pageSize,searchKey,order,random);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(TypechoContents typechoContents,String searchKey) {
		return dao.total(typechoContents,searchKey);
	}
}