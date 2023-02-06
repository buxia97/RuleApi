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
 * TypechoSpaceServiceImpl
 * @author buxia97
 * @date 2023/02/05
 */
@Service
public class TypechoSpaceServiceImpl implements TypechoSpaceService {

    @Autowired
	TypechoSpaceDao dao;

    @Override
    public int insert(TypechoSpace typechoSpace) {
        return dao.insert(typechoSpace);
    }

    @Override
    public int batchInsert(List<TypechoSpace> list) {
    	return dao.batchInsert(list);
    }

    @Override
    public int update(TypechoSpace typechoSpace) {
    	return dao.update(typechoSpace);
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
	public TypechoSpace selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<TypechoSpace> selectList(TypechoSpace typechoSpace) {
		return dao.selectList(typechoSpace);
	}

	@Override
	public PageList<TypechoSpace> selectPage(TypechoSpace typechoSpace, Integer offset, Integer pageSize,String order,String searchKey) {
		PageList<TypechoSpace> pageList = new PageList<>();

		int total = this.total(typechoSpace);

		Integer totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<TypechoSpace> list = dao.selectPage(typechoSpace, page, pageSize,order,searchKey);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(TypechoSpace typechoSpace) {
		return dao.total(typechoSpace);
	}
}