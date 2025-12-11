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
 * TypechoFieldsServiceImpl
 * @author buxia97
 * @date 2021/11/29
 */
@Service
public class FieldsServiceImpl implements FieldsService {

    @Autowired
	TypechoFieldsDao dao;

    @Override
    public int insert(TypechoFields typechoFields) {
        return dao.insert(typechoFields);
    }

    @Override
    public int batchInsert(List<TypechoFields> list) {
    	return dao.batchInsert(list);
    }

    @Override
    public int update(TypechoFields typechoFields) {
    	return dao.update(typechoFields);
    }

    @Override
    public int delete(Integer cid,String name) {
    	return dao.delete(cid,name);
    }

    @Override
    public int batchDelete(List<Object> keys) {
        return dao.batchDelete(keys);
    }

	@Override
	public List<TypechoFields> selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<TypechoFields> selectList(TypechoFields typechoFields) {
		return dao.selectList(typechoFields);
	}

	@Override
	public PageList<TypechoFields> selectPage(TypechoFields typechoFields, Integer offset, Integer pageSize) {
		PageList<TypechoFields> pageList = new PageList<>();

		int total = this.total(typechoFields);

		Integer totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<TypechoFields> list = dao.selectPage(typechoFields, page, pageSize);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(TypechoFields typechoFields) {
		return dao.total(typechoFields);
	}
}