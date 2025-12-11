package com.RuleApi.service.impl;

import com.RuleApi.entity.*;
import com.RuleApi.dao.*;
import com.RuleApi.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 业务层实现类
 * TypechoApiconfigServiceImpl
 * @author apiconfig
 * @date 2022/04/28
 */
@Service
public class ApiconfigServiceImpl implements ApiconfigService {

    @Autowired
	TypechoApiconfigDao dao;

    @Override
    public int insert(TypechoApiconfig typechoApiconfig) {
        return dao.insert(typechoApiconfig);
    }


    @Override
    public int update(TypechoApiconfig typechoApiconfig) {
    	return dao.update(typechoApiconfig);
    }


	@Override
	public TypechoApiconfig selectByKey(Object key) {
		return dao.selectByKey(key);
	}

}