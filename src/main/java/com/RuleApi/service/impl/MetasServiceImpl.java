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
 * TypechoMetasServiceImpl
 * @author buxia97
 * @date 2021/11/29
 */
@Service
public class MetasServiceImpl implements MetasService {

    @Autowired
	TypechoMetasDao dao;

    @Override
    public int insert(TypechoMetas typechoMetas) {
        return dao.insert(typechoMetas);
    }

    @Override
    public int batchInsert(List<TypechoMetas> list) {
    	return dao.batchInsert(list);
    }

    @Override
    public int update(TypechoMetas typechoMetas) {
    	return dao.update(typechoMetas);
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
	public TypechoMetas selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public TypechoMetas selectBySlug(Object slug) {
		return dao.selectBySlug(slug);
	}

	@Override
	public List<TypechoMetas> selectList(TypechoMetas typechoMetas) {
		return dao.selectList(typechoMetas);
	}

	@Override
	public PageList<TypechoMetas> selectPage(TypechoMetas typechoMetas, Integer offset, Integer pageSize, String searchKey, String order) {
		PageList<TypechoMetas> pageList = new PageList<>();

		int total = this.total(typechoMetas);

		Integer totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<TypechoMetas> list = dao.selectPage(typechoMetas, page, pageSize,searchKey,order);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(TypechoMetas typechoMetas) {
		return dao.total(typechoMetas);
	}
}