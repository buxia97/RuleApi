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
 * TypechoViolationServiceImpl
 * @author buxia97
 * @date 2023/01/03
 */
@Service
public class TypechoViolationServiceImpl implements TypechoViolationService {

    @Autowired
	TypechoViolationDao dao;

    @Override
    public int insert(TypechoViolation typechoViolation) {
        return dao.insert(typechoViolation);
    }

    @Override
    public int batchInsert(List<TypechoViolation> list) {
    	return dao.batchInsert(list);
    }

    @Override
    public int update(TypechoViolation typechoViolation) {
    	return dao.update(typechoViolation);
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
	public TypechoViolation selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<TypechoViolation> selectList(TypechoViolation typechoViolation) {
		return dao.selectList(typechoViolation);
	}

	@Override
	public PageList<TypechoViolation> selectPage(TypechoViolation typechoViolation, Integer offset, Integer pageSize) {
		PageList<TypechoViolation> pageList = new PageList<>();

		int total = this.total(typechoViolation);

		Integer totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<TypechoViolation> list = dao.selectPage(typechoViolation, page, pageSize);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(TypechoViolation typechoViolation) {
		return dao.total(typechoViolation);
	}
}