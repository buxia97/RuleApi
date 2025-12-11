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
 * TypechoFanServiceImpl
 * @author buxia97
 * @date 2023/01/03
 */
@Service
public class FanServiceImpl implements FanService {

    @Autowired
	TypechoFanDao dao;

    @Override
    public int insert(TypechoFan typechoFan) {
        return dao.insert(typechoFan);
    }

    @Override
    public int batchInsert(List<TypechoFan> list) {
    	return dao.batchInsert(list);
    }

    @Override
    public int update(TypechoFan typechoFan) {
    	return dao.update(typechoFan);
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
	public TypechoFan selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<TypechoFan> selectList(TypechoFan typechoFan) {
		return dao.selectList(typechoFan);
	}

	@Override
	public PageList<TypechoFan> selectPage(TypechoFan typechoFan, Integer offset, Integer pageSize) {
		PageList<TypechoFan> pageList = new PageList<>();

		int total = this.total(typechoFan);

		Integer totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<TypechoFan> list = dao.selectPage(typechoFan, page, pageSize);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public PageList<TypechoFan> selectUserPage(TypechoFan typechoFan, Integer offset, Integer pageSize) {
		PageList<TypechoFan> pageList = new PageList<>();

		int total = this.total(typechoFan);

		Integer totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<TypechoFan> list = dao.selectPage(typechoFan, page, pageSize);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(TypechoFan typechoFan) {
		return dao.total(typechoFan);
	}
}