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
                insert.setType(0);
                chatid = service.insert(insert);
            }
            JSONObject response = new JSONObject();
            response.put("code" , 1);
            response.put("data" , chatid);
            response.put("msg"  , "");
            return response.toString();
        }catch (Exception e){
            System.err.println(e);
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
                                @RequestParam(value = "type", required = false) Integer  type,
                                @RequestParam(value = "url", required = false) String  url) {
        try{
            if(chatid==null||msg==null||type==null){
                return Result.getResultJson(0,"参数不正确",null);
            }
            if(type<0){
                return Result.getResultJson(0,"参数不正确",null);
            }
            Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
            if(uStatus==0){
                return Result.getResultJson(0,"用户未登录或Token验证失败",null);
            }
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer uid =Integer.parseInt(map.get("uid").toString());
            //禁言判断

            String isSilence = redisHelp.getRedis(this.dataprefix+"_"+uid+"_silence",redisTemplate);
            if(isSilence!=null){
                return Result.getResultJson(0,"你已被禁言，请耐心等待",null);
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

            TypechoChat chat = service.selectByKey(chatid);
            if(chat==null){
                return Result.getResultJson(0,"聊天室不存在",null);
            }
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
                        return Result.getResultJson(0,"你已多次发送违禁词，被禁言一小时！",null);
                    }else{
                        redisHelp.setRedis(this.dataprefix+"_"+uid+"_isIntercept",frequency.toString(),600,redisTemplate);
                    }

                }
                return Result.getResultJson(0,"消息存在违禁词",null);
            }
            //违禁词拦截结束


            Long date = System.currentTimeMillis();
            String created = String.valueOf(date).substring(0,10);
            TypechoChatMsg msgbox = new TypechoChatMsg();
            msgbox.setCid(chatid);
            msgbox.setUid(uid);
            msgbox.setText(msg);
            msgbox.setCreated(Integer.parseInt(created));
            msgbox.setUrl(url);
            int rows = chatMsgService.insert(msgbox);
            JSONObject response = new JSONObject();
            response.put("code" , rows);
            response.put("msg"  , rows > 0 ? "发送成功" : "发送失败");
            return response.toString();
        }catch (Exception e){
            System.err.println(e);
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
                            @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit) {
        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }
        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid =Integer.parseInt(map.get("uid").toString());
        //查询uid时，同时查询toid
        TypechoChat query = new TypechoChat();
        query.setUid(uid);
        List jsonList = new ArrayList();
        List cacheList = redisHelp.getList(this.dataprefix+"_"+"myChat"+uid+"_"+page+"_"+limit,redisTemplate);
        Integer total = service.total(query);
        try{
            if(cacheList.size()>0){
                jsonList = cacheList;
            }else{
                TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);

                PageList<TypechoChat> pageList = service.selectPage(query, page, limit);
                List<TypechoChat> list = pageList.getList();
                if(list.size() < 1){
                    JSONObject noData = new JSONObject();
                    noData.put("code" , 0);
                    noData.put("msg"  , "");
                    noData.put("data" , new ArrayList());
                    noData.put("count", 0);
                    noData.put("total", total);
                    return noData.toString();
                }
                for (int i = 0; i < list.size(); i++) {
                    Map json = JSONObject.parseObject(JSONObject.toJSONString(list.get(i)), Map.class);
                    TypechoChat chat = list.get(i);
                    Integer userid = chat.getUid();
                    if(userid.equals(uid)){
                        userid = chat.getToid();
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
                    jsonList.add(json);
                }
                redisHelp.delete(this.dataprefix+"_"+"myChat"+uid+"_"+page+"_"+limit,redisTemplate);
                redisHelp.setList(this.dataprefix+"_"+"myChat"+uid+"_"+page+"_"+limit,jsonList,3,redisTemplate);
            }
        }catch (Exception e){
            System.err.println(e);
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
                    noData.put("code" , 0);
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
                    jsonList.add(json);
                }
                redisHelp.delete(this.dataprefix+"_"+"msgList_"+chatid+"_"+page+"_"+limit,redisTemplate);
                redisHelp.setList(this.dataprefix+"_"+"msgList_"+chatid+"_"+page+"_"+limit,jsonList,3,redisTemplate);
            }
        }catch (Exception e){
            System.err.println(e);
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
     * 参数请求报文:
     *
     * {
     *   "key":1
     * }
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
            JSONObject response = new JSONObject();
            response.put("code", rows > 0 ? 1 : 0);
            response.put("data", rows);
            response.put("msg", rows > 0 ? "操作成功" : "操作失败");
            return response.toString();

        } catch (Exception e) {
            System.err.println(e);
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }

    }

    /**
     * 参数请求报文:
     *
     * {
     *   "key":1
     * }
     */
    @RequestMapping(value = "/selectByKey")
    @ResponseBody
    public ApiResult selectByKey (@RequestBody Object key, HttpServletRequest request) {
        TypechoChat typechoChat = service.selectByKey(key);
        return new ApiResult<>(ResultCode.success.getCode(), typechoChat, ResultCode.success.getDescr(), request.getRequestURI());
    }

    /***
    * 参数请求报文:
    *
    * {
    *   "paramOne": 1,
    *   "paramTwo": "xxx"
    * }
    */
    @RequestMapping(value = "/selectList")
    @ResponseBody
    public ApiResult selectList (@RequestBody TypechoChat typechoChat, HttpServletRequest request) {
        List<TypechoChat> result = service.selectList(typechoChat);
        return new ApiResult<>(ResultCode.success.getCode(), result, ResultCode.success.getDescr(), request.getRequestURI());
    }

    /***
     * 参数请求报文:
     *
     * {
     *   "paramOne": 1,
     *   "paramTwo": "xxx"
     * }
     */
    @RequestMapping(value = "/selectPage")
    @ResponseBody
    public ApiResult selectPage (@RequestBody JSONObject object, HttpServletRequest request) {
        Integer page     = (Integer) object.getOrDefault("page"    , 1);
        Integer pageSize = (Integer) object.getOrDefault("pageSize", 15);

        // 剔除page, pageSize参数
        object.remove("page");
        object.remove("pageSize");

        TypechoChat typechoChat = object.toJavaObject(TypechoChat.class);
        PageList<TypechoChat> pageList = service.selectPage(typechoChat, page, pageSize);
        return new ApiResult<>(ResultCode.success.getCode(), pageList, ResultCode.success.getDescr(), request.getRequestURI());
    }

    /***
     * 表单查询请求
     * @param searchParams Bean对象JSON字符串
     * @param page         页码
     * @param limit        每页显示数量
     */
    @RequestMapping(value = "/formPage")
    @ResponseBody
    public String formPage (@RequestParam(value = "searchParams", required = false) String  searchParams,
                            @RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                            @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit) {
        TypechoChat query = new TypechoChat();
        if (StringUtils.isNotBlank(searchParams)) {
            JSONObject object = JSON.parseObject(searchParams);
            query = object.toJavaObject(TypechoChat.class);
        }

        PageList<TypechoChat> pageList = service.selectPage(query, page, limit);
        JSONObject response = new JSONObject();
        response.put("code" , 0);
        response.put("msg"  , "");
        response.put("data" , null != pageList.getList() ? pageList.getList() : new JSONArray());
        response.put("count", pageList.getTotalCount());
        return response.toString();
    }

    /***
     * 表单查询
     */
    @RequestMapping(value = "/formSelectByKey")
    @ResponseBody
    public TypechoChat formSelectByKey(@RequestParam(value = "key", required = false) String  key) {
        return service.selectByKey(key);
    }



    /***
     * 表单修改
     * @param params Bean对象JSON字符串
     */
    @RequestMapping(value = "/formUpdate")
    @ResponseBody
    public String formUpdate(@RequestParam(value = "params", required = false) String  params) {
        TypechoChat update = null;
        if (StringUtils.isNotBlank(params)) {
            JSONObject object = JSON.parseObject(params);
            update = object.toJavaObject(TypechoChat.class);
        }

        int rows = service.update(update);

        JSONObject response = new JSONObject();
        response.put("code" , rows);
        response.put("msg"  , rows > 0 ? "修改成功" : "修改失败");
        return response.toString();
    }

    /***
     * 表单删除
     */
    @RequestMapping(value = "/formDelete")
    @ResponseBody
    public int formDelete(@RequestParam(value = "key", required = false) String  key) {
        return service.delete(key);
    }
}
