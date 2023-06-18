package com.RuleApi.service.impl;

import com.RuleApi.common.PageList;
import com.RuleApi.dao.TypechoAppDao;
import com.RuleApi.entity.TypechoApp;
import com.RuleApi.service.TypechoAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 业务层实现类
 * TypechoAppServiceImpl
 * @author vips
 * @date 2023/06/09
 */
@Service
public class TypechoAppServiceImpl implements TypechoAppService {

    @Autowired
	TypechoAppDao dao;

    @Override
    public int insert(TypechoApp typechoApp) {
        return dao.insert(typechoApp);
    }

    @Override
    public int batchInsert(List<TypechoApp> list) {
    	return dao.batchInsert(list);
    }

    @Override
    public int update(TypechoApp typechoApp) {
    	return dao.update(typechoApp);
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
	public TypechoApp selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<TypechoApp> selectList(TypechoApp typechoApp) {
		return dao.selectList(typechoApp);
	}

	@Override
	public PageList<TypechoApp> selectPage(TypechoApp typechoApp, Integer offset, Integer pageSize) {
		PageList<TypechoApp> pageList = new PageList<>();

		int total = this.total(typechoApp);

		Integer totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<TypechoApp> list = dao.selectPage(typechoApp, page, pageSize);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(TypechoApp typechoApp) {
		return dao.total(typechoApp);
	}
}