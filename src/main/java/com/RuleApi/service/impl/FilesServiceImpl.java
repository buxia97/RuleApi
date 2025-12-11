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
 * TypechoFilesServiceImpl
 * @author files
 * @date 2025/09/13
 */
@Service
public class FilesServiceImpl implements FilesService {

    @Autowired
	TypechoFilesDao dao;

    @Override
    public int insert(TypechoFiles typechoFiles) {
        return dao.insert(typechoFiles);
    }

    @Override
    public int batchInsert(List<TypechoFiles> list) {
    	return dao.batchInsert(list);
    }

    @Override
    public int update(TypechoFiles typechoFiles) {
    	return dao.update(typechoFiles);
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
	public TypechoFiles selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<TypechoFiles> selectList(TypechoFiles typechoFiles) {
		return dao.selectList(typechoFiles);
	}

	@Override
	public PageList<TypechoFiles> selectPage(TypechoFiles typechoFiles, Integer offset, Integer pageSize,String searchKey,String order) {
		PageList<TypechoFiles> pageList = new PageList<>();

		int total = this.total(typechoFiles,searchKey);

		Integer totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<TypechoFiles> list = dao.selectPage(typechoFiles, page, pageSize,searchKey,order);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(TypechoFiles typechoFiles,String searchKey) {
		return dao.total(typechoFiles,searchKey);
	}
}