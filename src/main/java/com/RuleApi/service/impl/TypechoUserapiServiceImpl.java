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
 * TypechoUserapiServiceImpl
 * @author buxia97
 * @date 2022/01/10
 */
@Service
public class TypechoUserapiServiceImpl implements TypechoUserapiService {

	@Autowired
	TypechoUserapiDao dao;

	@Override
	public int insert(TypechoUserapi typechoUserapi) {
		return dao.insert(typechoUserapi);
	}

	@Override
	public int batchInsert(List<TypechoUserapi> list) {
		return dao.batchInsert(list);
	}

	@Override
	public int update(TypechoUserapi typechoUserapi) {
		return dao.update(typechoUserapi);
	}

	@Override
	public int delete(Object key) {
		return dao.delete(key);
	}

	@Override
	public int deleteUserAll(Object key) {
		return dao.deleteUserAll(key);
	}

	@Override
	public int batchDelete(List<Object> keys) {
		return dao.batchDelete(keys);
	}

	@Override
	public TypechoUserapi selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<TypechoUserapi> selectList(TypechoUserapi typechoUserapi) {
		return dao.selectList(typechoUserapi);
	}

	@Override
	public PageList<TypechoUserapi> selectPage(TypechoUserapi typechoUserapi, Integer offset, Integer pageSize) {
		PageList<TypechoUserapi> pageList = new PageList<>();

		int total = this.total(typechoUserapi);

		Integer totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<TypechoUserapi> list = dao.selectPage(typechoUserapi, page, pageSize);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(TypechoUserapi typechoUserapi) {
		return dao.total(typechoUserapi);
	}
}