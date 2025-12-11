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
 * TypechoInvitationServiceImpl
 * @author invitation
 * @date 2022/05/03
 */
@Service
public class InvitationServiceImpl implements InvitationService {

    @Autowired
	TypechoInvitationDao dao;

    @Override
    public int insert(TypechoInvitation typechoInvitation) {
        return dao.insert(typechoInvitation);
    }

    @Override
    public int batchInsert(List<TypechoInvitation> list) {
    	return dao.batchInsert(list);
    }

    @Override
    public int update(TypechoInvitation typechoInvitation) {
    	return dao.update(typechoInvitation);
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
	public TypechoInvitation selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<TypechoInvitation> selectList(TypechoInvitation typechoInvitation) {
		return dao.selectList(typechoInvitation);
	}

	@Override
	public PageList<TypechoInvitation> selectPage(TypechoInvitation typechoInvitation, Integer offset, Integer pageSize) {
		PageList<TypechoInvitation> pageList = new PageList<>();

		int total = this.total(typechoInvitation);

		Integer totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<TypechoInvitation> list = dao.selectPage(typechoInvitation, page, pageSize);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(TypechoInvitation typechoInvitation) {
		return dao.total(typechoInvitation);
	}
}