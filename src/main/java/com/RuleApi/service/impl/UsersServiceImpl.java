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
 * TypechoUsersServiceImpl
 * @author buxia97
 * @date 2021/11/29
 */
@Service
public class UsersServiceImpl implements UsersService {

    @Autowired
	TypechoUsersDao dao;

    @Override
    public int insert(TypechoUsers typechoUsers) {
        return dao.insert(typechoUsers);
    }

    @Override
    public int batchInsert(List<TypechoUsers> list) {
    	return dao.batchInsert(list);
    }

    @Override
    public int update(TypechoUsers typechoUsers) {
    	return dao.update(typechoUsers);
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
	public TypechoUsers selectByKey(Object key) {
		return dao.selectByKey(key);
	}


	@Override
	public List<TypechoUsers> selectList(TypechoUsers typechoUsers) {
		return dao.selectList(typechoUsers);
	}

	@Override
	public PageList<TypechoUsers> selectPage(TypechoUsers typechoUsers, Integer offset, Integer pageSize ,String searchKey,String order) {
		PageList<TypechoUsers> pageList = new PageList<>();

		int total = this.total(typechoUsers,searchKey);

		Integer totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<TypechoUsers> list = dao.selectPage(typechoUsers, page, pageSize,searchKey,order);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setSearchKey(searchKey);
		pageList.setOrder(order);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(TypechoUsers typechoUsers,String searchKey) {
		return dao.total(typechoUsers,searchKey);
	}
}