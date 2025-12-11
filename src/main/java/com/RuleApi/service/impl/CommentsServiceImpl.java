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
 * TypechoCommentsServiceImpl
 * @author buxia97
 * @date 2021/11/29
 */
@Service
public class CommentsServiceImpl implements CommentsService {

    @Autowired
	TypechoCommentsDao dao;

    @Override
    public int insert(TypechoComments typechoComments) {
        return dao.insert(typechoComments);
    }

    @Override
    public int batchInsert(List<TypechoComments> list) {
    	return dao.batchInsert(list);
    }

    @Override
    public int update(TypechoComments typechoComments) {
    	return dao.update(typechoComments);
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
	public TypechoComments selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<TypechoComments> selectList(TypechoComments typechoComments) {
		return dao.selectList(typechoComments);
	}

	@Override
	public PageList<TypechoComments> selectPage(TypechoComments typechoComments, Integer offset, Integer pageSize,String searchKey,String order) {
		PageList<TypechoComments> pageList = new PageList<>();

		int total = this.total(typechoComments,searchKey);

		Integer totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<TypechoComments> list = dao.selectPage(typechoComments, page, pageSize,searchKey,order);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(TypechoComments typechoComments,String searchKey) {
		return dao.total(typechoComments,searchKey);
	}
}