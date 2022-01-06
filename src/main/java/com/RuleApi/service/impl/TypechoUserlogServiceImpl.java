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
 * TypechoUserlogServiceImpl
 * @author buxia97
 * @date 2022/01/06
 */
@Service
public class TypechoUserlogServiceImpl implements TypechoUserlogService {

    @Autowired
	TypechoUserlogDao dao;

    @Override
    public int insert(TypechoUserlog typechoUserlog) {
        return dao.insert(typechoUserlog);
    }

    @Override
    public int batchInsert(List<TypechoUserlog> list) {
    	return dao.batchInsert(list);
    }

    @Override
    public int update(TypechoUserlog typechoUserlog) {
    	return dao.update(typechoUserlog);
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
	public TypechoUserlog selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<TypechoUserlog> selectList(TypechoUserlog typechoUserlog) {
		return dao.selectList(typechoUserlog);
	}

	@Override
	public PageList<TypechoUserlog> selectPage(TypechoUserlog typechoUserlog, Integer offset, Integer pageSize) {
		PageList<TypechoUserlog> pageList = new PageList<>();

		int total = this.total(typechoUserlog);

		Integer totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<TypechoUserlog> list = dao.selectPage(typechoUserlog, page, pageSize);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(TypechoUserlog typechoUserlog) {
		return dao.total(typechoUserlog);
	}
}