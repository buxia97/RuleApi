package com.RuleApi.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
@Component
public class UserStatus {
    RedisHelp redisHelp =new RedisHelp();

    @Value("${web.prefix}")
    private String dataprefix;
    //默认用户状态，0未登录，1登陆状态，2禁用
    private Integer status = 1;
    public Integer getStatus(String token, RedisTemplate redisTemplate){
        if(token==null){
            this.status=0;
            return this.status;
        }
        String key = this.dataprefix+"_"+"userInfo"+token;
        Map map =redisHelp.getMapValue(key,redisTemplate);
        if(map.size()==0){
            this.status=0;
            return this.status;
        }
//        Long date = System.currentTimeMillis();
//        Long old_date = (Long) redisHelp.getValue("userInfo"+token,"time",redisTemplate);
//        //清除上次数据
//        if(date - old_date > this.time){
//            redisHelp.delete("userInfo"+token,redisTemplate);
//            this.status=0;
//            return this.status;
//        }
        this.status=1;
        return this.status;
    }
}
