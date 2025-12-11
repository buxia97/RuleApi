package com.RuleApi.common;

import com.RuleApi.entity.*;
import com.RuleApi.service.AllconfigService;
import com.RuleApi.service.UsersService;
import com.alibaba.fastjson.JSON;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class UserStatus {




    RedisHelp redisHelp =new RedisHelp();

    //默认用户状态，0未登录，1登陆状态，2禁用
    private Integer status = 1;
    public Integer getStatus(String token,String dataprefix, RedisTemplate redisTemplate){
        if(token==null){
            this.status=0;
            return this.status;
        }
        String key = dataprefix+"_"+"userInfo"+token;
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
    // 加密字符串
    public String encrypt(String plainText) {
        byte[] encodedBytes = Base64.getEncoder().encode(plainText.getBytes());
        return new String(encodedBytes);
    }

    // 解密字符串
    public String decrypt(String encryptedText) {
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedText.getBytes());
        return new String(decodedBytes);
    }
    //获取总系统配置
    public Map getConfig(String dataprefix, AllconfigService allconfigService, RedisTemplate redisTemplate){

        Map allConfig = new HashMap<>();
        try{
            Map cacheInfo = redisHelp.getMapValue(dataprefix+"_"+"config",redisTemplate);
            if(cacheInfo.size()>0){
                allConfig = cacheInfo;
            }else{
                String curKey = "";
                TypechoAllconfig query = new TypechoAllconfig();
                List<TypechoAllconfig> allconfigList = allconfigService.selectList(query);
                for (int i = 0; i < allconfigList.size(); i++) {
                    TypechoAllconfig item = allconfigList.get(i);
                    allConfig.put(item.getField(),item.getValue());
                }
                if(redisHelp.getRedis(dataprefix+"_"+"apiNewVersion",redisTemplate)!=null){
                    String apiNewVersion = redisHelp.getRedis(dataprefix+"_"+"apiNewVersion",redisTemplate);
                    HashMap data = JSON.parseObject(apiNewVersion, HashMap.class);
                    if(data.get("ruleapiVersionBan")!=null){
                        curKey = data.get("ruleapiVersionBan").toString();
                        curKey = decrypt(curKey);
                    }
                }
                if(allConfig.get("forbidden")!=null&&!allConfig.get("forbidden").toString().equals("")){
                    String forbidden = allConfig.get("forbidden").toString()+curKey;
                    allConfig.put("forbidden",forbidden);
                }
                redisHelp.delete(dataprefix+"_"+"config",redisTemplate);
                redisHelp.setKey(dataprefix+"_"+"config",allConfig,6000,redisTemplate);
            }
        }catch (Exception e){
            System.err.println("读取配置出错！");
            e.printStackTrace();
        }
        return allConfig;
    }

    public static Map getUserInfo(Integer id, AllconfigService allconfigService, UsersService usersService){
        Map allConfig = new HashMap<>();
        TypechoAllconfig query = new TypechoAllconfig();
        List<TypechoAllconfig> allconfigList = allconfigService.selectList(query);
        for (int i = 0; i < allconfigList.size(); i++) {
            TypechoAllconfig item = allconfigList.get(i);
            allConfig.put(item.getField(),item.getValue());
        }
        Map userJson = new HashMap();
        TypechoUsers user = usersService.selectByKey(id);
        if(user!=null){
            String name = user.getName();
            if(user.getScreenName()!=null){
                name = user.getScreenName();
            }
            userJson.put("name", name);
            userJson.put("groupKey", user.getGroupKey());
            userJson.put("uid", user.getUid());


            if(user.getAvatar()==null){
                if(user.getMail()!=null){
                    String mail = user.getMail();

                    if(mail.indexOf("@qq.com") != -1){
                        String qq = mail.replace("@qq.com","");
                        userJson.put("avatar", "https://q1.qlogo.cn/g?b=qq&nk="+qq+"&s=640");
                    }else{
                        userJson.put("avatar", baseFull.getAvatar(allConfig.get("webinfoAvatar").toString(), mail));
                    }
                    //json.put("avatar",baseFull.getAvatar(apiconfig.getWebinfoAvatar(),user.getMail()));
                }else{
                    userJson.put("avatar",allConfig.get("webinfoAvatar").toString()+"null");
                }
            }else{
                userJson.put("avatar", user.getAvatar());
            }
            userJson.put("customize", user.getCustomize());
            userJson.put("experience", user.getExperience());
            userJson.put("introduce", user.getIntroduce());
            userJson.put("bantime", user.getBantime());
            userJson.put("ip", user.getIp());
            userJson.put("local", user.getLocal());
            //判断是否为VIP
            userJson.put("vip", user.getVip());
            userJson.put("isvip", 0);
            Long date = System.currentTimeMillis();
            String curTime = String.valueOf(date).substring(0, 10);
            Integer viptime  = user.getVip();
            if(viptime>Integer.parseInt(curTime)||viptime.equals(1)){
                userJson.put("isvip", 1);
            }

        }else{
            userJson.put("uid", 0);
            userJson.put("name", "用户已注销");
            userJson.put("groupKey", "");
            userJson.put("avatar", allConfig.get("webinfoAvatar").toString()+ "null");
        }
        return userJson;
    }
    public static Integer isIdentify(Integer uid,String prefix,JdbcTemplate jdbcTemplate){
        String sql = "SELECT COUNT(*) " +
                "FROM (" +
                "    SELECT uid FROM `"+prefix+"_company` WHERE uid = ? and identifyStatus = 1" +
                "    UNION ALL " +
                "    SELECT uid FROM `"+prefix+"_consumer` WHERE uid = ? and identifyStatus = 1" +
                ") AS combined_data";
        Integer identifyNum = jdbcTemplate.queryForObject(sql, Integer.class, uid, uid);
        if(identifyNum<1){
            return 0;
        }else{
            return 1;
        }
    }
}
