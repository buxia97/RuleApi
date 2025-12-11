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
 * TypechoInboxServiceImpl
 * @author inbox
 * @date 2022/12/29
 */
@Service
public class InboxServiceImpl implements InboxService {

    @Autowired
	TypechoInboxDao dao;

    @Override
    public int insert(TypechoInbox typechoInbox) {
        return dao.insert(typechoInbox);
    }

    @Override
    public int update(TypechoInbox typechoInbox) {
    	return dao.update(typechoInbox);
    }

    @Override
    public int delete(Object key) {
    	return dao.delete(key);
    }

	@Override
	public TypechoInbox selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<TypechoInbox> selectList(TypechoInbox typechoInbox) {
		return dao.selectList(typechoInbox);
	}

	@Override
	public PageList<TypechoInbox> selectPage(TypechoInbox typechoInbox, Integer offset, Integer pageSize) {
		PageList<TypechoInbox> pageList = new PageList<>();

		int total = this.total(typechoInbox);

		Integer totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<TypechoInbox> list = dao.selectPage(typechoInbox, page, pageSize);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(TypechoInbox typechoInbox) {
		return dao.total(typechoInbox);
	}
}