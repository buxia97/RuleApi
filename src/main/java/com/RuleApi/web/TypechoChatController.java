package com.RuleApi.web;

import com.RuleApi.common.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.RuleApi.entity.*;
import com.RuleApi.service.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 控制层
 * TypechoChatController
 * @author buxia97
 * @date 2023/01/10
 */
@Controller
@RequestMapping(value = "/typechoChat")
public class TypechoChatController {

    @Autowired
    TypechoChatService service;

    @Autowired
    TypechoChatMsgService chatMsgService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private TypechoApiconfigService apiconfigService;

    @Autowired
    private TypechoUsersService usersService;

    @Value("${web.prefix}")
    private String dataprefix;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private PushService pushService;

    RedisHelp redisHelp =new RedisHelp();
    ResultAll Result = new ResultAll();
    UserStatus UStatus = new UserStatus();
    baseFull baseFull = new baseFull();
    EditFile editFile = new EditFile();

    /***
     * 获取私聊聊天室（没有则自动新增）
     */
    @RequestMapping(value = "/getPrivateChat")
    @ResponseBody
    public String getPrivateChat(@RequestParam(value = "token", required = false) String  token,
                            @RequestParam(value = "touid", required = false) Integer  touid) {
        try{
            if(touid==null||touid<1){
                return Result.getResultJson(0,"参数不正确",null);
            }
            Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);

            if(uStatus==0){
                return Result.getResultJson(0,"用户未登录或Token验证失败",null);
            }
            Integer chatid = null;
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer uid =Integer.parseInt(map.get("uid").toString());
            //登录情况下，刷聊天数据
            String isSilence = redisHelp.getRedis(this.dataprefix+"_"+uid+"_silence",redisTemplate);
            if(isSilence!=null){
                return Result.getResultJson(0,"你的操作太频繁了，请稍后再试",null);
            }
            String isRepeated = redisHelp.getRedis(this.dataprefix+"_"+uid+"_getChat",redisTemplate);
            if(isRepeated==null){
                redisHelp.setRedis(this.dataprefix+"_"+uid+"_getChat","2",1,redisTemplate);
            }else{
                Integer frequency = Integer.parseInt(isRepeated) + 1;
                if(frequency==4){
                    securityService.safetyMessage("用户ID："+uid+"，在聊天发起接口疑似存在攻击行为，请及时确认处理。","system");
                    redisHelp.setRedis(this.dataprefix+"_"+uid+"_silence","1",900,redisTemplate);
                    return Result.getResultJson(0,"你的操作过于频繁，已被禁用15分钟聊天室！",null);
                }else{
                    redisHelp.setRedis(this.dataprefix+"_"+uid+"_isSendMsg",frequency.toString(),5,redisTemplate);
                }
            }
            //攻击拦截结束

            //判断是否有聊天室存在(自己发起的聊天)
            TypechoChat chat = new TypechoChat();
            chat.setUid(uid);
            chat.setToid(touid);
            List<TypechoChat> list = service.selectList(chat);
            if(list.size()>0){
                chatid = list.get(0).getId();
            }else{
                //判断对方发起的聊天
                chat.setUid(touid);
                chat.setToid(uid);
                list = service.selectList(chat);
                if(list.size()>0){
                    chatid = list.get(0).getId();
                }
            }
            //如果未聊天过，则创建聊天室
            if(chatid==null){
                TypechoChat insert = new TypechoChat();
                insert.setUid(uid);
                insert.setToid(touid);
                Long date = System.currentTimeMillis();
                String created = String.valueOf(date).substring(0,10);
                insert.setCreated(Integer.parseInt(created));
                insert.setLastTime(Integer.parseInt(created));
                insert.setType(0);
                service.insert(insert);
                chatid = insert.getId();
            }
            JSONObject response = new JSONObject();
            response.put("code" , 1);
            response.put("data" , chatid);
            response.put("msg"  , "");
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }

    }
    /***
     * 发送消息
     */
    @RequestMapping(value = "/sendMsg")
    @ResponseBody
    public String sendMsg(@RequestParam(value = "token", required = false) String  token,
                                 @RequestParam(value = "chatid", required = false) Integer  chatid,
                                 @RequestParam(value = "msg", required = false) String  msg,
                                @RequestParam(value = "type", required = false, defaultValue = "0") Integer  type,
                                @RequestParam(value = "url", required = false) String  url) {
        try{
            if(chatid==null||type==null){
                return Result.getResultJson(0,"参数不正确",null);
            }
            if(type<0){
                return Result.getResultJson(0,"参数不正确",null);
            }
            if(type.equals(0)){
                if(msg.length()<1){
                    return Result.getResultJson(0,"消息内容不能为空",null);
                }
            }
            Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
            if(uStatus==0){
                return Result.getResultJson(0,"用户未登录或Token验证失败",null);
            }
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer uid =Integer.parseInt(map.get("uid").toString());
            String group = map.get("group").toString();
            //禁言判断

            String isSilence = redisHelp.getRedis(this.dataprefix+"_"+uid+"_silence",redisTemplate);
            if(isSilence!=null){
                return Result.getResultJson(0,"你的操作太频繁了，请稍后再试",null);
            }

            //登录情况下，刷数据攻击拦截
            String isRepeated = redisHelp.getRedis(this.dataprefix+"_"+uid+"_isSendMsg",redisTemplate);
            if(isRepeated==null){
                redisHelp.setRedis(this.dataprefix+"_"+uid+"_isSendMsg","1",1,redisTemplate);
            }else{
                Integer frequency = Integer.parseInt(isRepeated) + 1;
                if(frequency==4){
                    securityService.safetyMessage("用户ID："+uid+"，在聊天发送消息接口疑似存在攻击行为，请及时确认处理。","system");
                    redisHelp.setRedis(this.dataprefix+"_"+uid+"_silence","1",600,redisTemplate);
                    return Result.getResultJson(0,"你的发言过于频繁，已被禁言十分钟！",null);
                }else{
                    redisHelp.setRedis(this.dataprefix+"_"+uid+"_isSendMsg",frequency.toString(),5,redisTemplate);
                }
                return Result.getResultJson(0,"你的操作太频繁了",null);
            }

            //攻击拦截结束
            if(baseFull.haveCode(msg).equals(1)){
                return Result.getResultJson(0,"消息内容存在敏感代码",null);
            }
            TypechoChat chat = service.selectByKey(chatid);
            if(chat==null){
                return Result.getResultJson(0,"聊天室不存在",null);
            }
            if(!chat.getBan().equals(0)){

                if(group.equals("administrator")&&group.equals("editor")){
                    if(chat.getType().equals(0)){
                        return Result.getResultJson(0,"该聊天室已开启屏蔽机制",null);
                    }else{
                        return Result.getResultJson(0,"管理员已开启禁言",null);
                    }
                }

            }
            if(type.equals(0)){
                if(msg.length()>1500){
                    return Result.getResultJson(0,"最大发言字数不超过1500",null);
                }
                //违禁词拦截
                TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);
                String forbidden = apiconfig.getForbidden();
                Integer intercept = 0;
                if(forbidden!=null){
                    if(forbidden.indexOf(",") != -1){
                        String[] strarray=forbidden.split(",");
                        for (int i = 0; i < strarray.length; i++){
                            String str = strarray[i];
                            if(msg.indexOf(str) != -1){
                                intercept = 1;
                            }
                        }
                    }else{
                        if(msg.indexOf(forbidden) != -1){
                            intercept = 1;
                        }
                    }
                }
                if(intercept.equals(1)){
                    //以十分钟为检测周期，违禁一次刷新一次，等于4次则禁言
                    String isIntercept = redisHelp.getRedis(this.dataprefix+"_"+uid+"_isIntercept",redisTemplate);
                    if(isIntercept==null){
                        redisHelp.setRedis(this.dataprefix+"_"+uid+"_isIntercept","1",600,redisTemplate);
                    }else{
                        Integer frequency = Integer.parseInt(isIntercept) + 1;
                        if(frequency==4){
                            securityService.safetyMessage("用户ID："+uid+"，在聊天发送消息接口多次触发违禁，请及时确认处理。","system");
                            redisHelp.setRedis(this.dataprefix+"_"+uid+"_silence","1",3600,redisTemplate);
                            return Result.getResultJson(0,"您多次发送违禁内容，已被限制功能1小时",null);
                        }else{
                            redisHelp.setRedis(this.dataprefix+"_"+uid+"_isIntercept",frequency.toString(),600,redisTemplate);
                        }

                    }
                    return Result.getResultJson(0,"消息存在违禁词",null);
                }
                //违禁词拦截结束
            }


            Long date = System.currentTimeMillis();
            String created = String.valueOf(date).substring(0,10);
            TypechoChatMsg msgbox = new TypechoChatMsg();
            msgbox.setCid(chatid);
            msgbox.setUid(uid);
            msgbox.setText(msg);
            msgbox.setCreated(Integer.parseInt(created));
            msgbox.setUrl(url);
            msgbox.setType(type);
            int rows = chatMsgService.insert(msgbox);
            //更新聊天室最后消息时间
            TypechoChat newChat = new TypechoChat();
            newChat.setId(chatid);
            newChat.setLastTime(Integer.parseInt(created));
            service.update(newChat);
            JSONObject response = new JSONObject();
            response.put("code" , rows);
            response.put("msg"  , rows > 0 ? "发送成功" : "发送失败");
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }


    }
    /***
     * 我参与的私聊
     */
    @RequestMapping(value = "/myChat")
    @ResponseBody
    public String myCaht (@RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                            @RequestParam(value = "token", required = false) String  token,
                            @RequestParam(value = "order", required = false) String  order,
                            @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit) {
        if(limit>50){
            limit = 50;
        }
        if(order==null){
            order = "lastTime";
        }
        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }
        Map map = redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);

        Integer uid =Integer.parseInt(map.get("uid").toString());
        //查询uid时，同时查询toid
        TypechoChat query = new TypechoChat();
        query.setUid(uid);
        query.setType(0);
        List jsonList = new ArrayList();
        List cacheList = redisHelp.getList(this.dataprefix+"_"+"myChat"+uid+"_"+page+"_"+limit,redisTemplate);
        Integer total = service.total(query);
        try{
            if(cacheList.size()>0){
                jsonList = cacheList;
            }else{
                TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);

                PageList<TypechoChat> pageList = service.selectPage(query, page, limit,order,null);
                List<TypechoChat> list = pageList.getList();
                if(list.size() < 1){
                    JSONObject noData = new JSONObject();
                    noData.put("code" , 1);
                    noData.put("msg"  , "");
                    noData.put("data" , new ArrayList());
                    noData.put("count", 0);
                    noData.put("total", total);
                    return noData.toString();
                }
                for (int i = 0; i < list.size(); i++) {
                    Map json = JSONObject.parseObject(JSONObject.toJSONString(list.get(i)), Map.class);


                    TypechoChat chat = list.get(i);

                    //获取最新聊天消息
                    Integer chatid = chat.getId();
                    TypechoChatMsg msg = new TypechoChatMsg();
                    msg.setCid(chatid);
                    List<TypechoChatMsg> msgList = chatMsgService.selectList(msg);
                    if(msgList.size()>0) {
                        Map lastMsg = JSONObject.parseObject(JSONObject.toJSONString(msgList.get(0)), Map.class);
                        json.put("lastMsg",lastMsg);
                    }
                    Integer msgNum = chatMsgService.total(msg);
                    json.put("msgNum",msgNum);

                    Integer userid = chat.getUid();
                    if(userid.equals(uid)){
                        userid = chat.getToid();
                    }else{
                        userid = chat.getUid();
                    }
                    TypechoUsers user = usersService.selectByKey(userid);
                    //获取用户信息
                    Map userJson = new HashMap();
                    if(user!=null){
                        String name = user.getName();
                        if(user.getScreenName()!=null){
                            name = user.getScreenName();
                        }
                        userJson.put("name", name);
                        userJson.put("groupKey", user.getGroupKey());

                        if(user.getAvatar()==null){
                            if(user.getMail()!=null){
                                String mail = user.getMail();
                                if(mail.indexOf("@qq.com") != -1){
                                    String qq = mail.replace("@qq.com","");
                                    userJson.put("avatar", "https://q1.qlogo.cn/g?b=qq&nk="+qq+"&s=640");
                                }else{
                                    userJson.put("avatar", baseFull.getAvatar(apiconfig.getWebinfoAvatar(), mail));
                                }
                                //json.put("avatar",baseFull.getAvatar(apiconfig.getWebinfoAvatar(),user.getMail()));
                            }else{
                                userJson.put("avatar",apiconfig.getWebinfoAvatar()+"null");
                            }
                        }else{
                            userJson.put("avatar", user.getAvatar());
                        }
                        userJson.put("uid", user.getUid());
                        userJson.put("customize", user.getCustomize());
                        userJson.put("experience",user.getExperience());
                        userJson.put("introduce", user.getIntroduce());
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
                        userJson.put("name", "用户已注销");
                        userJson.put("groupKey", "");
                        userJson.put("avatar", apiconfig.getWebinfoAvatar() + "null");
                    }
                    json.put("userJson",userJson);
                    jsonList.add(json);
                }
                redisHelp.delete(this.dataprefix+"_"+"myChat"+uid+"_"+page+"_"+limit,redisTemplate);
                redisHelp.setList(this.dataprefix+"_"+"myChat"+uid+"_"+page+"_"+limit,jsonList,3,redisTemplate);
            }
        }catch (Exception e){
            e.printStackTrace();
            if(cacheList.size()>0){
                jsonList = cacheList;
            }
        }
        JSONObject response = new JSONObject();
        response.put("code" , 1);
        response.put("msg"  , "");
        response.put("data" , jsonList);
        response.put("count", jsonList.size());
        response.put("total", total);
        return response.toString();

    }

    /***
     * 聊天消息
     */
    @RequestMapping(value = "/msgList")
    @ResponseBody
    public String msgList ( @RequestParam(value = "chatid", required = false) Integer  chatid,
                            @RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                            @RequestParam(value = "token", required = false) String  token,
                            @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit) {
        if(chatid==null){
            return Result.getResultJson(0,"参数不正确",null);
        }
        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }
        TypechoChat chat = service.selectByKey(chatid);
        if(chat==null){
            return Result.getResultJson(0,"聊天室不存在",null);
        }
        if(limit>300){
            limit = 300;
        }

        TypechoChatMsg query = new TypechoChatMsg();
        query.setCid(chatid);
        List jsonList = new ArrayList();
        List cacheList = redisHelp.getList(this.dataprefix+"_"+"msgList_"+chatid+"_"+page+"_"+limit,redisTemplate);
        Integer total = chatMsgService.total(query);
        try{
            if(cacheList.size()>0){
                jsonList = cacheList;
            }else{
                TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);

                PageList<TypechoChatMsg> pageList = chatMsgService.selectPage(query, page, limit);
                List<TypechoChatMsg> list = pageList.getList();
                if(list.size() < 1){
                    JSONObject noData = new JSONObject();
                    noData.put("code" , 1);
                    noData.put("msg"  , "");
                    noData.put("data" , new ArrayList());
                    noData.put("count", 0);
                    noData.put("total", total);
                    return noData.toString();
                }
                for (int i = 0; i < list.size(); i++) {
                    Map json = JSONObject.parseObject(JSONObject.toJSONString(list.get(i)), Map.class);
                    TypechoChatMsg msg = list.get(i);
                    Integer userid = msg.getUid();
                    TypechoUsers user = usersService.selectByKey(userid);
                    //获取用户信息
                    Map userJson = new HashMap();
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
                                    userJson.put("avatar", baseFull.getAvatar(apiconfig.getWebinfoAvatar(), mail));
                                }
                                //json.put("avatar",baseFull.getAvatar(apiconfig.getWebinfoAvatar(),user.getMail()));
                            }else{
                                userJson.put("avatar",apiconfig.getWebinfoAvatar()+"null");
                            }
                        }else{
                            userJson.put("avatar", user.getAvatar());
                        }
                        userJson.put("customize", user.getCustomize());
                        userJson.put("introduce", user.getIntroduce());
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
                        userJson.put("name", "用户已注销");
                        userJson.put("groupKey", "");
                        userJson.put("avatar", apiconfig.getWebinfoAvatar() + "null");
                    }
                    json.put("userJson",userJson);
                    //获取最新消息


                    jsonList.add(json);
                }
                redisHelp.delete(this.dataprefix+"_"+"msgList_"+chatid+"_"+page+"_"+limit,redisTemplate);
                redisHelp.setList(this.dataprefix+"_"+"msgList_"+chatid+"_"+page+"_"+limit,jsonList,3,redisTemplate);
            }
        }catch (Exception e){
            e.printStackTrace();
            if(cacheList.size()>0){
                jsonList = cacheList;
            }
        }
        JSONObject response = new JSONObject();
        response.put("code" , 1);
        response.put("msg"  , "");
        response.put("data" , jsonList);
        response.put("count", jsonList.size());
        response.put("total", total);
        return response.toString();
    }

    /**
     * 删除聊天室
     */
    @RequestMapping(value = "/deleteChat")
    @ResponseBody
    public String deleteChat (@RequestParam(value = "chatid", required = false) Integer  chatid,
                             @RequestParam(value = "token", required = false) String  token) {
        if(chatid==null){
            return Result.getResultJson(0,"参数不正确",null);
        }
        try {
            Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
            if(uStatus==0){
                return Result.getResultJson(0,"用户未登录或Token验证失败",null);
            }
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            String group = map.get("group").toString();
            if(!group.equals("administrator")&&!group.equals("editor")){
                return Result.getResultJson(0,"你没有操作权限",null);
            }
            Integer logUid =Integer.parseInt(map.get("uid").toString());
            TypechoChat chat = service.selectByKey(chatid);
            if(chat==null){
                return Result.getResultJson(0,"聊天室不存在",null);
            }
            //删除聊天室全部消息
            chatMsgService.delete(chatid);
            //删除聊天室
            int rows = service.delete(chatid);
            editFile.setLog("管理员"+logUid+"请求删除（清空聊天室）："+chatid);
            JSONObject response = new JSONObject();
            response.put("code", rows > 0 ? 1 : 0);
            response.put("data", rows);
            response.put("msg", rows > 0 ? "操作成功" : "操作失败");
            return response.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }

    }
    /**
     * 删除聊天消息
     */
    @RequestMapping(value = "/deleteMsg")
    @ResponseBody
    public String deleteMsg (@RequestParam(value = "msgid", required = false) Integer  msgid,
                              @RequestParam(value = "token", required = false) String  token) {
        if(msgid==null){
            return Result.getResultJson(0,"参数不正确",null);
        }
        try {
            Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
            if(uStatus==0){
                return Result.getResultJson(0,"用户未登录或Token验证失败",null);
            }
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            String group = map.get("group").toString();
            if(!group.equals("administrator")&&!group.equals("editor")){
                return Result.getResultJson(0,"你没有操作权限",null);
            }
            Integer logUid =Integer.parseInt(map.get("uid").toString());
            TypechoChatMsg msg = chatMsgService.selectByKey(msgid);
            if(msg==null){
                return Result.getResultJson(0,"聊天消息不存在",null);
            }
            //删除消息
            int rows = chatMsgService.deleteMsg(msgid);
            editFile.setLog("管理员"+logUid+"请求删除聊天消息："+msgid);
            JSONObject response = new JSONObject();
            response.put("code", rows > 0 ? 1 : 0);
            response.put("data", rows);
            response.put("msg", rows > 0 ? "操作成功" : "操作失败");
            return response.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }

    }

    /***
     * 管理员创建群聊
     */
    @RequestMapping(value = "/createGroup")
    @ResponseBody
    public String createChat(@RequestParam(value = "name", required = false) String  name,
                             @RequestParam(value = "pic", required = false) String  pic,
                             @RequestParam(value = "token", required = false) String  token) {

        if(name.length()<1||pic.length()<1){
            return Result.getResultJson(0,"必须设置群聊图片和名称",null);
        }
        try{
            Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
            if(uStatus==0){
                return Result.getResultJson(0,"用户未登录或Token验证失败",null);
            }
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            String group = map.get("group").toString();
            if(!group.equals("administrator")&&!group.equals("editor")){
                return Result.getResultJson(0,"你没有操作权限",null);
            }
            Long date = System.currentTimeMillis();
            String created = String.valueOf(date).substring(0,10);
            Integer uid =Integer.parseInt(map.get("uid").toString());
            TypechoChat chat = new TypechoChat();
            chat.setName(name);
            chat.setPic(pic);
            chat.setUid(uid);
            chat.setType(1);
            chat.setCreated(Integer.parseInt(created));
            chat.setLastTime(Integer.parseInt(created));
            int rows = service.insert(chat);
            editFile.setLog("管理员"+uid+"请求创建聊天室");
            JSONObject response = new JSONObject();
            response.put("code", rows > 0 ? 1 : 0);
            response.put("data", rows);
            response.put("msg", rows > 0 ? "创建成功" : "创建失败");
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }
    }
    /***
     * 管理员编辑群聊
     */
    @RequestMapping(value = "/editGroup")
    @ResponseBody
    public String editGroup(@RequestParam(value = "name", required = false) String  name,
                             @RequestParam(value = "id", required = false) Integer  id,
                             @RequestParam(value = "pic", required = false) String  pic,
                             @RequestParam(value = "token", required = false) String  token) {

        try{
            Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
            if(uStatus==0){
                return Result.getResultJson(0,"用户未登录或Token验证失败",null);
            }
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            String group = map.get("group").toString();
            if(!group.equals("administrator")&&!group.equals("editor")){
                return Result.getResultJson(0,"你没有操作权限",null);
            }
            TypechoChat oldChat = service.selectByKey(id);
            if(oldChat == null){
                return Result.getResultJson(0,"群聊不存在",null);
            }
            Integer uid =Integer.parseInt(map.get("uid").toString());
            TypechoChat chat = new TypechoChat();
            chat.setId(id);
            chat.setName(name);
            chat.setPic(pic);

            int rows = service.update(chat);
            editFile.setLog("管理员"+uid+"请求修改聊天室");
            JSONObject response = new JSONObject();
            response.put("code", rows > 0 ? 1 : 0);
            response.put("data", rows);
            response.put("msg", rows > 0 ? "修改成功" : "修改失败");
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }
    }
    /***
     * 屏蔽和全群禁言
     */
    @RequestMapping(value = "/banChat")
    @ResponseBody
    public String banChat(@RequestParam(value = "id", required = false) Integer  id,
                            @RequestParam(value = "token", required = false) String  token,
                          @RequestParam(value = "type", required = false, defaultValue = "1") Integer  type) {
        try {
            Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
            if(uStatus==0){
                return Result.getResultJson(0,"用户未登录或Token验证失败",null);
            }

            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            String group = map.get("group").toString();
            Integer uid =Integer.parseInt(map.get("uid").toString());
            TypechoChat oldChat = service.selectByKey(id);
            if(oldChat == null){
                return Result.getResultJson(0,"聊天室不存在",null);
            }
            if(oldChat.getType().equals(1)){
                if(!group.equals("administrator")&&!group.equals("editor")){
                    return Result.getResultJson(0,"你没有操作权限",null);
                }
//                if(!oldChat.getBan().equals(0)){
//                    return Result.getResultJson(0,"该群聊已被全体禁言",null);
//                }
            }else{
                if(oldChat.getUid().equals(uid)&&oldChat.getToid().equals(uid)){
                    return Result.getResultJson(0,"你没有操作权限",null);
                }
                if(!oldChat.getBan().equals(0)){
                    if(!oldChat.getBan().equals(uid)){
                        return Result.getResultJson(0,"你没有操作权限",null);
                    }

                }

            }
            TypechoChat chat = new TypechoChat();
            chat.setId(id);
            if(type.equals(1)){
                chat.setBan(uid);
            }else{
                chat.setBan(0);
            }

            int rows = service.update(chat);
            //发送系统消息
            Long date = System.currentTimeMillis();
            String created = String.valueOf(date).substring(0,10);
            TypechoChatMsg msgbox = new TypechoChatMsg();
            msgbox.setCid(id);
            msgbox.setUid(uid);
            if(type.equals(1)) {
                msgbox.setText("ban");
            }else{
                msgbox.setText("noban");
            }
            msgbox.setCreated(Integer.parseInt(created));
            msgbox.setType(4);
            chatMsgService.insert(msgbox);
            editFile.setLog("用户"+uid+"请求屏蔽or禁言聊天室");
            JSONObject response = new JSONObject();
            response.put("code", rows > 0 ? 1 : 0);
            response.put("data", rows);
            response.put("msg", rows > 0 ? "操作成功" : "操作失败");
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }



    }
    /***
     * 群聊信息
     */
    @RequestMapping(value = "/groupInfo")
    @ResponseBody
    public String groupInfo(@RequestParam(value = "id", required = false) Integer  id) {

        try{
            Map groupInfoJson = new HashMap<String, String>();
            Map cacheInfo = redisHelp.getMapValue(this.dataprefix+"_"+"groupInfoJson_"+id,redisTemplate);

            if(cacheInfo.size()>0){
                groupInfoJson = cacheInfo;
            }else{
                TypechoChat chat;
                chat = service.selectByKey(id);
                TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);
                //获取创建人信息
                Integer userid = chat.getUid();
                Map userJson = UserStatus.getUserInfo(userid,apiconfigService,usersService);
                groupInfoJson.put("userJson",userJson);
                groupInfoJson = JSONObject.parseObject(JSONObject.toJSONString(chat), Map.class);
                redisHelp.delete(this.dataprefix+"_"+"groupInfoJson_"+id,redisTemplate);
                redisHelp.setKey(this.dataprefix+"_"+"groupInfoJson_"+id,groupInfoJson,5,redisTemplate);
            }

            JSONObject response = new JSONObject();

            response.put("code", 1);
            response.put("msg", "");
            response.put("data", groupInfoJson);

            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            JSONObject response = new JSONObject();
            response.put("code", 1);
            response.put("msg", "");
            response.put("data", null);

            return response.toString();
        }
    }

    /***
     * 全部聊天
     */
    @RequestMapping(value = "/allChat")
    @ResponseBody
    public String allGroup (@RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                            @RequestParam(value = "order", required = false, defaultValue = "created") String  order,
                            @RequestParam(value = "type", required = false, defaultValue = "1") Integer  type,
                            @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit,
                            @RequestParam(value = "searchKey"        , required = false, defaultValue = "") String searchKey,
                            @RequestParam(value = "token", required = false) String  token) {
        if(limit>50){
            limit = 50;
        }
        TypechoChat query = new TypechoChat();
        query.setType(1);
        //管理员可以查看所有聊天，普通用户只能查看群聊
        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        if(uStatus==0){
            query.setType(1);
        }else{
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            String group = map.get("group").toString();
            if(group.equals("administrator")||group.equals("editor")){
                query.setType(type);
            }

        }

        List jsonList = new ArrayList();
        List cacheList = redisHelp.getList(this.dataprefix+"_"+"allGroup_"+page+"_"+limit,redisTemplate);
        Integer total = service.total(query);
        try{
            if(cacheList.size()>0){
                jsonList = cacheList;
            }else{
                TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);

                PageList<TypechoChat> pageList = service.selectPage(query, page, limit,order,searchKey);
                List<TypechoChat> list = pageList.getList();
                if(list.size() < 1){
                    JSONObject noData = new JSONObject();
                    noData.put("code" , 1);
                    noData.put("msg"  , "");
                    noData.put("data" , new ArrayList());
                    noData.put("count", 0);
                    noData.put("total", total);
                    return noData.toString();
                }
                for (int i = 0; i < list.size(); i++) {
                    Map json = JSONObject.parseObject(JSONObject.toJSONString(list.get(i)), Map.class);
                    TypechoChat chat = list.get(i);

                    //获取最新聊天消息
                    Integer chatid = chat.getId();
                    TypechoChatMsg msg = new TypechoChatMsg();
                    msg.setCid(chatid);
                    List<TypechoChatMsg> msgList = chatMsgService.selectList(msg);
                    if(msgList.size()>0) {
                        Integer msgUid = msgList.get(0).getUid();
                        TypechoUsers msgUser = usersService.selectByKey(msgUid);

                        Map lastMsg = JSONObject.parseObject(JSONObject.toJSONString(msgList.get(0)), Map.class);
                        if(msgUser.getScreenName()!=null){
                            lastMsg.put("name",msgUser.getScreenName());
                        }else{
                            lastMsg.put("name",msgUser.getName());
                        }
                        json.put("lastMsg",lastMsg);
                    }
                    Integer msgNum = chatMsgService.total(msg);
                    json.put("msgNum",msgNum);

                    if(type.equals(0)){
                        //获取聊天发起人信息
                        Integer userid = chat.getUid();
                        Integer toUserid = chat.getToid();
                        TypechoUsers user = usersService.selectByKey(userid);
                        TypechoUsers toUser = usersService.selectByKey(toUserid);
                        //获取用户信息
                        Map userJson = new HashMap();
                        if(user!=null){
                            String name = user.getName();
                            if(user.getScreenName()!=null){
                                name = user.getScreenName();
                            }
                            String toName = toUser.getName();
                            if(toUser.getScreenName()!=null){
                                toName = toUser.getScreenName();
                            }
                            userJson.put("name", name);
                            userJson.put("toName", toName);
                            userJson.put("groupKey", user.getGroupKey());

                            if(user.getAvatar()==null){
                                if(user.getMail()!=null){
                                    String mail = user.getMail();
                                    if(mail.indexOf("@qq.com") != -1){
                                        String qq = mail.replace("@qq.com","");
                                        userJson.put("avatar", "https://q1.qlogo.cn/g?b=qq&nk="+qq+"&s=640");
                                    }else{
                                        userJson.put("avatar", baseFull.getAvatar(apiconfig.getWebinfoAvatar(), mail));
                                    }
                                    //json.put("avatar",baseFull.getAvatar(apiconfig.getWebinfoAvatar(),user.getMail()));
                                }else{
                                    userJson.put("avatar",apiconfig.getWebinfoAvatar()+"null");
                                }
                            }else{
                                userJson.put("avatar", user.getAvatar());
                            }
                            userJson.put("uid", user.getUid());
                            userJson.put("touid", toUser.getUid());
                            userJson.put("customize", user.getCustomize());
                            userJson.put("experience",user.getExperience());
                            userJson.put("introduce", user.getIntroduce());
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
                            userJson.put("name", "用户已注销");
                            userJson.put("groupKey", "");
                            userJson.put("avatar", apiconfig.getWebinfoAvatar() + "null");
                        }
                        json.put("userJson",userJson);
                    }

                    jsonList.add(json);
                }
                redisHelp.delete(this.dataprefix+"_"+"allGroup_"+page+"_"+limit,redisTemplate);
                redisHelp.setList(this.dataprefix+"_"+"allGroup_"+page+"_"+limit,jsonList,5,redisTemplate);
            }
        }catch (Exception e){
            e.printStackTrace();
            if(cacheList.size()>0){
                jsonList = cacheList;
            }
        }
        JSONObject response = new JSONObject();
        response.put("code" , 1);
        response.put("msg"  , "");
        response.put("data" , jsonList);
        response.put("count", jsonList.size());
        response.put("total", total);
        return response.toString();

    }



}
