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
 * TypechoChatMsgServiceImpl
 * @author buxia97
 * @date 2023/01/11
 */
@Service
public class TypechoChatMsgServiceImpl implements TypechoChatMsgService {

    @Autowired
	TypechoChatMsgDao dao;

    @Override
    public int insert(TypechoChatMsg typechoChatMsg) {
        return dao.insert(typechoChatMsg);
    }

    @Override
    public int batchInsert(List<TypechoChatMsg> list) {
    	return dao.batchInsert(list);
    }

    @Override
    public int update(TypechoChatMsg typechoChatMsg) {
    	return dao.update(typechoChatMsg);
    }

    @Override
    public int delete(Object key) {
    	return dao.delete(key);
    }

	@Override
	public int deleteMsg(Object key) {
		return dao.deleteMsg(key);
	}

    @Override
    public int batchDelete(List<Object> keys) {
        return dao.batchDelete(keys);
    }

	@Override
	public TypechoChatMsg selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<TypechoChatMsg> selectList(TypechoChatMsg typechoChatMsg) {
		return dao.selectList(typechoChatMsg);
	}

	@Override
	public PageList<TypechoChatMsg> selectPage(TypechoChatMsg typechoChatMsg, Integer offset, Integer pageSize) {
		PageList<TypechoChatMsg> pageList = new PageList<>();

		int total = this.total(typechoChatMsg);

		Integer totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<TypechoChatMsg> list = dao.selectPage(typechoChatMsg, page, pageSize);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(TypechoChatMsg typechoChatMsg) {
		return dao.total(typechoChatMsg);
	}
}