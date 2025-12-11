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
 * TypechoEmailtemplateServiceImpl
 * @author buxia97
 * @date 2023/10/06
 */
@Service
public class EmailtemplateServiceImpl implements EmailtemplateService {

    @Autowired
	TypechoEmailtemplateDao dao;

    @Override
    public int insert(TypechoEmailtemplate typechoEmailtemplate) {
        return dao.insert(typechoEmailtemplate);
    }

    @Override
    public int batchInsert(List<TypechoEmailtemplate> list) {
    	return dao.batchInsert(list);
    }

    @Override
    public int update(TypechoEmailtemplate typechoEmailtemplate) {
    	return dao.update(typechoEmailtemplate);
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
	public TypechoEmailtemplate selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<TypechoEmailtemplate> selectList(TypechoEmailtemplate typechoEmailtemplate) {
		return dao.selectList(typechoEmailtemplate);
	}

	@Override
	public PageList<TypechoEmailtemplate> selectPage(TypechoEmailtemplate typechoEmailtemplate, Integer offset, Integer pageSize) {
		PageList<TypechoEmailtemplate> pageList = new PageList<>();

		int total = this.total(typechoEmailtemplate);

		Integer totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<TypechoEmailtemplate> list = dao.selectPage(typechoEmailtemplate, page, pageSize);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(TypechoEmailtemplate typechoEmailtemplate) {
		return dao.total(typechoEmailtemplate);
	}
}