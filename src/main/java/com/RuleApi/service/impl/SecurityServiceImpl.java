package com.RuleApi.service.impl;

import com.RuleApi.entity.*;
import com.RuleApi.common.PageList;
import com.RuleApi.dao.*;
import com.RuleApi.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SecurityServiceImpl  implements SecurityService {
    @Autowired
    private TypechoInboxService inboxService;
    @Autowired
    private TypechoUsersService usersService;

    @Override
    public void safetyMessage(String msg,String type){
        //向所有管理员发送警告
        try{
            TypechoUsers user = new TypechoUsers();
            user.setGroupKey("administrator");
            List<TypechoUsers> userList = usersService.selectList(user);
            for (int i = 0; i < userList.size(); i++) {
                TypechoInbox inbox = new TypechoInbox();
                Integer uid = userList.get(i).getUid();
                Long date = System.currentTimeMillis();
                String created = String.valueOf(date).substring(0,10);
                TypechoInbox insert = new TypechoInbox();
                insert.setUid(uid);
                insert.setTouid(uid);
                insert.setType(type);
                insert.setText(msg);
                insert.setCreated(Integer.parseInt(created));
                inboxService.insert(insert);
            }
            System.err.println("有用户存在违规行为，已向所有管理员发送警告");
        }catch (Exception e){
            System.err.println(e);
        }
    }
}
