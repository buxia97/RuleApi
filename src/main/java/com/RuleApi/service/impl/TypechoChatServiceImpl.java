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
 * TypechoChatServiceImpl
 * @author buxia97
 * @date 2023/01/10
 */
@Service
public class TypechoChatServiceImpl implements TypechoChatService {

    @Autowired
	TypechoChatDao dao;

    @Override
    public int insert(TypechoChat typechoChat) {
        return dao.insert(typechoChat);
    }

    @Override
    public int batchInsert(List<TypechoChat> list) {
    	return dao.batchInsert(list);
    }

    @Override
    public int update(TypechoChat typechoChat) {
    	return dao.update(typechoChat);
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
	public TypechoChat selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<TypechoChat> selectList(TypechoChat typechoChat) {
		return dao.selectList(typechoChat);
	}

	@Override
	public PageList<TypechoChat> selectPage(TypechoChat typechoChat, Integer offset, Integer pageSize) {
		PageList<TypechoChat> pageList = new PageList<>();

		int total = this.total(typechoChat);

		Integer totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<TypechoChat> list = dao.selectPage(typechoChat, page, pageSize);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(TypechoChat typechoChat) {
		return dao.total(typechoChat);
	}
}