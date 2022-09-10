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
 * TypechoAdsServiceImpl
 * @author ads
 * @date 2022/09/06
 */
@Service
public class TypechoAdsServiceImpl implements TypechoAdsService {

    @Autowired
	TypechoAdsDao dao;

    @Override
    public int insert(TypechoAds typechoAds) {
        return dao.insert(typechoAds);
    }


    @Override
    public int update(TypechoAds typechoAds) {
    	return dao.update(typechoAds);
    }

    @Override
    public int delete(Object key) {
    	return dao.delete(key);
    }


	@Override
	public TypechoAds selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<TypechoAds> selectList(TypechoAds typechoAds) {
		return dao.selectList(typechoAds);
	}

	@Override
	public PageList<TypechoAds> selectPage(TypechoAds typechoAds, Integer offset, Integer pageSize,String searchKey) {
		PageList<TypechoAds> pageList = new PageList<>();

		int total = this.total(typechoAds);

		Integer totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<TypechoAds> list = dao.selectPage(typechoAds, page, pageSize,searchKey);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(TypechoAds typechoAds) {
		return dao.total(typechoAds);
	}
}