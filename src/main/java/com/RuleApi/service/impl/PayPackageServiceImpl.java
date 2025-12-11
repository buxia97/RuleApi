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
 * TypechoPayPackageServiceImpl
 * @author pay
 * @date 2025/10/18
 */
@Service
public class PayPackageServiceImpl implements PayPackageService {

    @Autowired
	TypechoPayPackageDao dao;

    @Override
    public int insert(TypechoPayPackage typechoPayPackage) {
        return dao.insert(typechoPayPackage);
    }

    @Override
    public int batchInsert(List<TypechoPayPackage> list) {
    	return dao.batchInsert(list);
    }

    @Override
    public int update(TypechoPayPackage typechoPayPackage) {
    	return dao.update(typechoPayPackage);
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
	public TypechoPayPackage selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<TypechoPayPackage> selectList(TypechoPayPackage typechoPayPackage) {
		return dao.selectList(typechoPayPackage);
	}

	@Override
	public PageList<TypechoPayPackage> selectPage(TypechoPayPackage typechoPayPackage, Integer offset, Integer pageSize) {
		PageList<TypechoPayPackage> pageList = new PageList<>();

		int total = this.total(typechoPayPackage);

		Integer totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<TypechoPayPackage> list = dao.selectPage(typechoPayPackage, page, pageSize);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(TypechoPayPackage typechoPayPackage) {
		return dao.total(typechoPayPackage);
	}
}