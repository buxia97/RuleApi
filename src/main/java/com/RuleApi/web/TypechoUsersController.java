package com.RuleApi.web;

import com.RuleApi.common.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.RuleApi.entity.*;
import com.RuleApi.service.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.springframework.stereotype.Component;


/**
 * 控制层
 * TypechoUsersController
 * @author buxia97
 * @date 2021/11/29
 */
@Component
@Controller
@RequestMapping(value = "/typechoUsers")
public class TypechoUsersController {

    @Autowired
    TypechoUsersService service;

    @Autowired
    private TypechoContentsService contentsService;


    @Autowired
    private TypechoCommentsService commentsService;

    @Autowired
    private TypechoUserlogService userlogService;

    @Autowired
    private TypechoUserapiService userapiService;

    @Autowired
    private TypechoPaylogService paylogService;

    @Autowired
    private TypechoApiconfigService apiconfigService;

    @Autowired
    private TypechoInvitationService invitationService;

    @Autowired
    MailService MailService;

    @Autowired
    private RedisTemplate redisTemplate;



    @Value("${webinfo.usertime}")
    private Integer usertime;

    @Value("${webinfo.userCache}")
    private Integer userCache;


    @Value("${web.prefix}")
    private String dataprefix;



    RedisHelp redisHelp = new RedisHelp();
    ResultAll Result = new ResultAll();
    baseFull baseFull = new baseFull();
    UserStatus UStatus = new UserStatus();
    HttpClient HttpClient = new HttpClient();
    PHPass phpass = new PHPass(8);

    /***
     * 用户查询
     * @param searchParams Bean对象JSON字符串
     * @param page         页码
     * @param limit        每页显示数量
     */
    @RequestMapping(value = "/userList")
    @ResponseBody
    public String userList(@RequestParam(value = "searchParams", required = false) String searchParams,
                           @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                           @RequestParam(value = "searchKey", required = false, defaultValue = "") String searchKey,
                           @RequestParam(value = "order", required = false, defaultValue = "") String order,
                           @RequestParam(value = "limit", required = false, defaultValue = "15") Integer limit) {
        TypechoUsers query = new TypechoUsers();
        if (StringUtils.isNotBlank(searchParams)) {
            JSONObject object = JSON.parseObject(searchParams);
            object.remove("password");
            query = object.toJavaObject(TypechoUsers.class);
        }
        if(limit>50){
            limit = 50;
        }
        List jsonList = new ArrayList();

        List cacheList = redisHelp.getList(this.dataprefix + "_" + "userList_" + page + "_" + limit + "_" + searchParams + "_" + order + "_" + searchKey, redisTemplate);
        try {
            if (cacheList.size() > 0) {
                jsonList = cacheList;
            } else {
                TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);
                PageList<TypechoUsers> pageList = service.selectPage(query, page, limit, searchKey, order);
                List list = pageList.getList();
                for (int i = 0; i < list.size(); i++) {
                    Map json = JSONObject.parseObject(JSONObject.toJSONString(list.get(i)), Map.class);
                    //获取用户等级
                    Integer uid = Integer.parseInt(json.get("uid").toString());
                    TypechoComments comments = new TypechoComments();
                    comments.setAuthorId(uid);
                    Integer lv = commentsService.total(comments);
                    json.put("lv", baseFull.getLv(lv));

                    json.remove("password");
                    json.remove("address");
                    json.remove("pay");
                    json.remove("assets");
                    if (json.get("mail") != null) {
                        json.put("avatar", baseFull.getAvatar(apiconfig.getWebinfoAvatar(), json.get("mail").toString()));
                    } else {
                        json.put("avatar", apiconfig.getWebinfoAvatar() + "null");
                    }


                    jsonList.add(json);

                }
                redisHelp.delete(this.dataprefix + "_" + "userList_" + page + "_" + limit + "_" + searchParams + "_" + order + "_" + searchKey, redisTemplate);
                redisHelp.setList(this.dataprefix + "_" + "userList_" + page + "_" + limit + "_" + searchParams + "_" + order + "_" + searchKey, jsonList, this.userCache, redisTemplate);
            }
        } catch (Exception e) {

            if (cacheList.size() > 0) {
                jsonList = cacheList;
            }

        }

        JSONObject response = new JSONObject();

        response.put("code", 1);
        response.put("msg", "");
        response.put("data", null != jsonList ? jsonList : new JSONArray());
        response.put("count", jsonList.size());

        return response.toString();
    }

    /***
     * 用户数据
     */
    @RequestMapping(value = "/userData")
    @ResponseBody
    public String userData(@RequestParam(value = "token", required = false) String token) {
        Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
        if (uStatus == 0) {
            return Result.getResultJson(0, "用户未登录或Token验证失败", null);
        }
        Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
        Integer uid = Integer.parseInt(map.get("uid").toString());
        //用户文章数量
        TypechoContents contents = new TypechoContents();
        contents.setType("post");
        contents.setStatus("publish");
        contents.setAuthorId(uid);
        Integer contentsNum = contentsService.total(contents);
        //用户评论数量
        TypechoComments comments = new TypechoComments();
        comments.setAuthorId(uid);
        Integer commentsNum = commentsService.total(comments);
        //用户资产和创建时间
        TypechoUsers user = service.selectByKey(uid);
        Integer assets = user.getAssets();
        Integer created = user.getCreated();
        //是否签到
        TypechoUserlog log = new TypechoUserlog();
        log.setType("clock");
        log.setUid(uid);
        List<TypechoUserlog> info = userlogService.selectList(log);
        Integer isClock = 0;
        //获取上次时间
        if (info.size() > 0) {
            Integer time = info.get(0).getCreated();
            String oldStamp = time + "000";
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            String oldtime = sdf.format(new Date(Long.parseLong(oldStamp)));
            Integer old = Integer.parseInt(oldtime);
            //获取本次时间
            Long curStamp = System.currentTimeMillis();  //获取当前时间戳
            String curtime = sdf.format(new Date(Long.parseLong(String.valueOf(curStamp))));
            Integer cur = Integer.parseInt(curtime);
            if (old >= cur) {
                isClock = 1;
            }
        }

        Map json = new HashMap();
        json.put("contentsNum", contentsNum);
        json.put("commentsNum", commentsNum);
        json.put("assets", assets);
        json.put("created", created);
        json.put("isClock", isClock);

        JSONObject response = new JSONObject();

        response.put("code", 1);
        response.put("msg", "");
        response.put("data", json);

        return response.toString();
    }

    /***
     * 表单查询
     */
    @RequestMapping(value = "/userInfo")
    @ResponseBody
    public String userInfo(@RequestParam(value = "key", required = false) String key) {
        try {
            TypechoUsers info = service.selectByKey(key);
            Map json = JSONObject.parseObject(JSONObject.toJSONString(info), Map.class);
            //获取用户等级
            Integer uid = Integer.parseInt(key);
            if (uid < 1) {
                return Result.getResultJson(0, "请传入正确的参数", null);
            }
            TypechoComments comments = new TypechoComments();
            comments.setAuthorId(uid);
            Integer lv = commentsService.total(comments);
            json.put("lv", baseFull.getLv(lv));
            //判断是否为VIP
            json.put("isvip", 0);
            Long date = System.currentTimeMillis();
            String curTime = String.valueOf(date).substring(0, 10);
            Integer viptime  = info.getVip();
            if(viptime>Integer.parseInt(curTime)||viptime.equals(1)){
                json.put("isvip", 1);
            }
            json.remove("password");
            json.remove("address");
            json.remove("pay");
            json.remove("assets");
            TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);
            if (json.get("mail") != null) {
                json.put("avatar", baseFull.getAvatar(apiconfig.getWebinfoAvatar(), json.get("mail").toString()));

            } else {
                json.put("avatar", apiconfig.getWebinfoAvatar() + "null");
            }
            JSONObject response = new JSONObject();

            response.put("code", 1);
            response.put("msg", "");
            response.put("data", json);

            return response.toString();
        } catch (Exception e) {
            JSONObject response = new JSONObject();

            response.put("code", 0);
            response.put("msg", "用户信息获取失败");
            response.put("data", null);

            return response.toString();
        }

    }

    /***
     * 登陆
     * @param params Bean对象JSON字符串
     */
    @RequestMapping(value = "/userLogin")
    @ResponseBody
    public String userLogin(@RequestParam(value = "params", required = false) String params) {
        Map jsonToMap = null;
        String oldpw = null;
        try {
            if (StringUtils.isNotBlank(params)) {
                jsonToMap = JSONObject.parseObject(JSON.parseObject(params).toString());
                if (jsonToMap.get("name") == null || jsonToMap.get("password") == null) {
                    return Result.getResultJson(0, "请输入正确的参数", null);
                }
                oldpw = jsonToMap.get("password").toString();
            } else {
                return Result.getResultJson(0, "请输入正确的参数", null);
            }
            jsonToMap.remove("password");
            TypechoUsers Users = JSON.parseObject(JSON.toJSONString(jsonToMap), TypechoUsers.class);
            List<TypechoUsers> rows = service.selectList(Users);
            if (rows.size() > 0) {
                //查询出用户信息后，通过接口验证用户密码
                String newpw = rows.get(0).getPassword();
                //通过内置验证
                boolean isPass = phpass.CheckPassword(oldpw, newpw);

                if (!isPass) {
                    return Result.getResultJson(0, "用户密码错误", null);
                }
                //内置验证结束
                Long date = System.currentTimeMillis();
                String Token = date + jsonToMap.get("name").toString();
                jsonToMap.put("uid", rows.get(0).getUid());
                //生成唯一性token用于验证
                jsonToMap.put("token", jsonToMap.get("name").toString() + DigestUtils.md5DigestAsHex(Token.getBytes()));
                jsonToMap.put("time", date);
                jsonToMap.put("group", rows.get(0).getGroupKey());
                jsonToMap.put("mail", rows.get(0).getMail());
                jsonToMap.put("url", rows.get(0).getUrl());
                jsonToMap.put("screenName", rows.get(0).getScreenName());
                jsonToMap.put("customize", rows.get(0).getCustomize());
                jsonToMap.put("introduce", rows.get(0).getIntroduce());
                //判断是否为VIP
                jsonToMap.put("vip", rows.get(0).getVip());
                jsonToMap.put("isvip", 0);
                String curTime = String.valueOf(date).substring(0, 10);
                Integer viptime  = rows.get(0).getVip();
                if(viptime>Integer.parseInt(curTime)||viptime.equals(1)){
                    jsonToMap.put("isvip", 1);
                }
                //获取用户等级
                Integer uid = rows.get(0).getUid();
                TypechoComments comments = new TypechoComments();
                comments.setAuthorId(uid);
                Integer lv = commentsService.total(comments);
                jsonToMap.put("lv", baseFull.getLv(lv));
                TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);
                if (rows.get(0).getMail() != null) {
                    jsonToMap.put("avatar", baseFull.getAvatar(apiconfig.getWebinfoAvatar(), rows.get(0).getMail()));
                } else {
                    jsonToMap.put("avatar", apiconfig.getWebinfoAvatar() + "null");
                }
                //更新用户登录时间和第一次登陆时间（满足typecho要求）
                String userTime = String.valueOf(date).substring(0, 10);
                Map updateLogin = new HashMap<String, String>();
                updateLogin.put("uid", rows.get(0).getUid());
                updateLogin.put("logged", userTime);
                if (rows.get(0).getLogged() == 0) {
                    updateLogin.put("activated", userTime);
                }
                TypechoUsers updateuser = JSON.parseObject(JSON.toJSONString(updateLogin), TypechoUsers.class);
                service.update(updateuser);


                //删除之前的token后，存入redis(防止积累导致内存溢出，超时时间默认是24小时)
                String oldToken = redisHelp.getRedis(this.dataprefix + "_" + "userkey" + jsonToMap.get("name").toString(), redisTemplate);
                if (oldToken != null) {
                    redisHelp.delete(this.dataprefix + "_" + "userInfo" + oldToken, redisTemplate);
                }
                //redisHelp.deleteByPrex("userInfo"+jsonToMap.get("name").toString()+":*",redisTemplate);
                redisHelp.setRedis(this.dataprefix + "_" + "userkey" + jsonToMap.get("name").toString(), jsonToMap.get("token").toString(), this.usertime, redisTemplate);
                redisHelp.setKey(this.dataprefix + "_" + "userInfo" + jsonToMap.get("name").toString() + DigestUtils.md5DigestAsHex(Token.getBytes()), jsonToMap, this.usertime, redisTemplate);

            }
            return Result.getResultJson(rows.size() > 0 ? 1 : 0, rows.size() > 0 ? "登录成功" : "用户名或密码错误", jsonToMap);
        } catch (Exception e) {
            JSONObject response = new JSONObject();

            response.put("code", 0);
            response.put("msg", "登陆失败，请联系管理员");
            response.put("data", null);

            return response.toString();
        }
    }

    /***
     * 社会化登陆
     * @param params Bean对象JSON字符串
     */
    @RequestMapping(value = "/apiLogin")
    @ResponseBody
    public String apiLogin(@RequestParam(value = "params", required = false) String params) {


        Map jsonToMap = null;
        String oldpw = null;
        try {
            if (StringUtils.isNotBlank(params)) {
                jsonToMap = JSONObject.parseObject(JSON.parseObject(params).toString());
            } else {
                return Result.getResultJson(0, "请输入正确的参数", null);
            }
            TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);
            Integer isInvite = apiconfig.getIsInvite();
            //如果是微信，则走两步判断，是小程序还是APP
            if(jsonToMap.get("appLoginType").toString().equals("weixin")){
                if(jsonToMap.get("type").toString().equals("applets")){
                    //如果是小程序，走官方接口获取accessToken和openid
                    if (jsonToMap.get("js_code") == null) {
                        return Result.getResultJson(0, "APP配置异常，请检查相关设置", null);
                    }
                    String js_code = jsonToMap.get("js_code").toString();

                    String requestUrl = "https://api.weixin.qq.com/sns/jscode2session?appid="+apiconfig.getAppletsAppid()+"&secret="+apiconfig.getAppletsSecret()+"&js_code="+js_code+"&grant_type=authorization_code";
                    String res = HttpClient.doGet(requestUrl);
                    if(res==null){
                        return Result.getResultJson(0, "接口配置异常，请检查相关设置", null);
                    }

                    HashMap data = JSON.parseObject(res, HashMap.class);
                    if(data.get("unionid")==null){
                        return Result.getResultJson(0, "接口配置异常，请检查相关设置", null);
                    }
                    jsonToMap.put("accessToken",data.get("unionid"));
                    jsonToMap.put("openId",data.get("openid"));
                }else {
                    if (jsonToMap.get("accessToken") == null) {
                        return Result.getResultJson(0, "登录配置异常，请检查相关设置", null);
                    }
                }
            }else{
                if (jsonToMap.get("accessToken") == null) {
                    return Result.getResultJson(0, "登录配置异常，请检查相关设置", null);
                }
            }
            TypechoUserapi userapi = JSON.parseObject(JSON.toJSONString(jsonToMap), TypechoUserapi.class);
            String accessToken = userapi.getAccessToken();
            String loginType = userapi.getAppLoginType();
            TypechoUserapi isApi = new TypechoUserapi();
            isApi.setAccessToken(accessToken);
            isApi.setAppLoginType(loginType);
            List<TypechoUserapi> apiList = userapiService.selectList(isApi);
            //大于0则走向登陆，小于0则进行注册
            if (apiList.size() > 0) {

                TypechoUserapi apiInfo = apiList.get(0);
                TypechoUsers user = service.selectByKey(apiInfo.getUid().toString());
                Long date = System.currentTimeMillis();
                String Token = date + user.getName();
                jsonToMap.put("uid", user.getUid());

                //生成唯一性token用于验证
                jsonToMap.put("name", user.getName());
                jsonToMap.put("token", user.getName() + DigestUtils.md5DigestAsHex(Token.getBytes()));
                jsonToMap.put("time", date);
                jsonToMap.put("group", user.getGroupKey());
                jsonToMap.put("mail", user.getMail());
                jsonToMap.put("url", user.getUrl());
                jsonToMap.put("screenName", user.getScreenName());
                jsonToMap.put("customize", user.getCustomize());
                jsonToMap.put("introduce", user.getIntroduce());
                //判断是否为VIP
                jsonToMap.put("vip", user.getVip());
                jsonToMap.put("isvip", 0);
                String curTime = String.valueOf(date).substring(0, 10);
                Integer viptime  = user.getVip();
                if(viptime>Integer.parseInt(curTime)||viptime.equals(1)){
                    jsonToMap.put("isvip", 1);
                }

                if (user.getMail() != null) {
                    jsonToMap.put("avatar", baseFull.getAvatar(apiconfig.getWebinfoAvatar(), user.getMail()));
                } else {
                    jsonToMap.put("avatar", apiconfig.getWebinfoAvatar() + "null");
                }
                //获取用户等级
                Integer uid = user.getUid();
                TypechoComments comments = new TypechoComments();
                comments.setAuthorId(uid);
                Integer lv = commentsService.total(comments);
                jsonToMap.put("lv", baseFull.getLv(lv));
                //更新用户登录时间和第一次登陆时间（满足typecho要求）
                String userTime = String.valueOf(date).substring(0, 10);
                Map updateLogin = new HashMap<String, String>();
                updateLogin.put("uid", user.getUid());
                updateLogin.put("logged", userTime);
                if (user.getLogged() == 0) {
                    updateLogin.put("activated", userTime);
                }
                TypechoUsers updateuser = JSON.parseObject(JSON.toJSONString(updateLogin), TypechoUsers.class);
                Integer rows = service.update(updateuser);

                //删除之前的token后，存入redis(防止积累导致内存溢出，超时时间默认是24小时)
                String oldToken = redisHelp.getRedis(this.dataprefix + "_" + "userkey" + jsonToMap.get("name").toString(), redisTemplate);
                if (oldToken != null) {
                    redisHelp.delete(this.dataprefix + "_" + "userInfo" + oldToken, redisTemplate);
                }
                redisHelp.setRedis(this.dataprefix + "_" + "userkey" + jsonToMap.get("name").toString(), jsonToMap.get("token").toString(), this.usertime, redisTemplate);
                redisHelp.setKey(this.dataprefix + "_" + "userInfo" + jsonToMap.get("name").toString() + DigestUtils.md5DigestAsHex(Token.getBytes()), jsonToMap, this.usertime, redisTemplate);

                return Result.getResultJson(rows > 0 ? 1 : 0, rows > 0 ? "登录成功" : "登陆失败", jsonToMap);

            } else {
                //注册
                if(isInvite.equals(1)){
                    return Result.getResultJson(0, "当前注册需要邀请码，请采用普通方式注册！", null);
                }
                TypechoUsers regUser = new TypechoUsers();
                String name = baseFull.createRandomStr(5) + baseFull.createRandomStr(4);
                String p = baseFull.createRandomStr(9);
                String passwd = phpass.HashPassword(p);
                Long date = System.currentTimeMillis();
                String userTime = String.valueOf(date).substring(0, 10);
                regUser.setName(name);
                regUser.setCreated(Integer.parseInt(userTime));
                regUser.setGroupKey("subscriber");
                regUser.setScreenName(userapi.getNickName());
                regUser.setPassword(passwd.replaceAll("(\\\r\\\n|\\\r|\\\n|\\\n\\\r)", ""));
                Integer to = service.insert(regUser);
                //注册完成后，增加绑定
                Integer uid = regUser.getUid();
                userapi.setUid(uid);
                int rows = userapiService.insert(userapi);
                //返回token
                Long regdate = System.currentTimeMillis();
                String Token = regdate + name;
                jsonToMap.put("uid", uid);
                //生成唯一性token用于验证
                jsonToMap.put("name", name);
                jsonToMap.put("token", name + DigestUtils.md5DigestAsHex(Token.getBytes()));
                jsonToMap.put("time", regdate);
                jsonToMap.put("group", "contributor");
                jsonToMap.put("mail", "");
                jsonToMap.put("url", "");
                jsonToMap.put("screenName", userapi.getNickName());
                jsonToMap.put("avatar", apiconfig.getWebinfoAvatar() + "null");
                jsonToMap.put("lv", 0);
                jsonToMap.put("customize", "");
                //VIP
                jsonToMap.put("vip", 0);
                jsonToMap.put("isvip", 0);

                //删除之前的token后，存入redis(防止积累导致内存溢出，超时时间默认是24小时)
                String oldToken = redisHelp.getRedis(this.dataprefix + "_" + "userkey" + name, redisTemplate);
                if (oldToken != null) {
                    redisHelp.delete(this.dataprefix + "_" + "userInfo" + oldToken, redisTemplate);
                }
                redisHelp.setRedis(this.dataprefix + "_" + "userkey" + jsonToMap.get("name").toString(), jsonToMap.get("token").toString(), this.usertime, redisTemplate);
                redisHelp.setKey(this.dataprefix + "_" + "userInfo" + jsonToMap.get("name").toString() + DigestUtils.md5DigestAsHex(Token.getBytes()), jsonToMap, this.usertime, redisTemplate);

                return Result.getResultJson(rows > 0 ? 1 : 0, rows > 0 ? "登录成功" : "登陆失败", jsonToMap);

            }
        } catch (Exception e) {
            System.out.println(e);
            JSONObject response = new JSONObject();

            response.put("code", 0);
            response.put("msg", "登陆失败，请联系管理员");
            response.put("data", null);

            return response.toString();
        }

    }

    /***
     * 社会化登陆绑定
     * @param params Bean对象JSON字符串
     */
    @RequestMapping(value = "/apiBind")
    @ResponseBody
    public String apiBind(@RequestParam(value = "params", required = false) String params, @RequestParam(value = "token", required = false) String token) {

        Map jsonToMap = null;
        String oldpw = null;
        try {
            Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
            if (uStatus == 0) {
                return Result.getResultJson(0, "用户未登录或Token验证失败", null);
            }
            if (StringUtils.isNotBlank(params)) {
                jsonToMap = JSONObject.parseObject(JSON.parseObject(params).toString());
            } else {
                return Result.getResultJson(0, "请输入正确的参数", null);
            }
            //如果是微信，则走两步判断，是小程序还是APP
            if(jsonToMap.get("appLoginType").toString().equals("weixin")){
                if(jsonToMap.get("type").toString().equals("applets")){
                    //如果是小程序，走官方接口获取accessToken和openid
                    if (jsonToMap.get("js_code") == null) {
                        return Result.getResultJson(0, "APP配置异常，请检查相关设置", null);
                    }
                    String js_code = jsonToMap.get("js_code").toString();
                    TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);
                    String requestUrl = "https://api.weixin.qq.com/sns/jscode2session?appid="+apiconfig.getAppletsAppid()+"&secret="+apiconfig.getAppletsSecret()+"&js_code="+js_code+"&grant_type=authorization_code";
                    String res = HttpClient.doGet(requestUrl);
                    if(res==null){
                        return Result.getResultJson(0, "接口配置异常，请检查相关设置", null);
                    }

                    HashMap data = JSON.parseObject(res, HashMap.class);
                    if(data.get("unionid")==null){
                        return Result.getResultJson(0, "接口配置异常，请检查相关设置", null);
                    }
                    jsonToMap.put("accessToken",data.get("unionid"));
                    jsonToMap.put("openId",data.get("openid"));
                }else {
                    if (jsonToMap.get("accessToken") == null) {
                        return Result.getResultJson(0, "登录配置异常，请检查相关设置", null);
                    }
                }
            }else{
                if (jsonToMap.get("accessToken") == null) {
                    return Result.getResultJson(0, "登录配置异常，请检查相关设置", null);
                }
            }

            TypechoUserapi userapi = JSON.parseObject(JSON.toJSONString(jsonToMap), TypechoUserapi.class);
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            Integer uid = Integer.parseInt(map.get("uid").toString());
            userapi.setUid(uid);
            String accessToken = userapi.getAccessToken();
            String loginType = userapi.getAppLoginType();
            TypechoUserapi isApi = new TypechoUserapi();
            isApi.setAccessToken(accessToken);
            isApi.setAppLoginType(loginType);
            List<TypechoUserapi> apiBind = userapiService.selectList(isApi);
            if (apiBind.size() > 0) {
                //如果已经绑定，删除之前的绑定
                Integer id = apiBind.get(0).getId();
                userapiService.delete(id);
            }
            int rows = userapiService.insert(userapi);
            JSONObject response = new JSONObject();
            response.put("code", rows > 0 ? 1 : 0);
            response.put("data", rows);
            response.put("msg", rows > 0 ? "绑定成功" : "绑定失败");
            return response.toString();
        } catch (Exception e) {
            System.out.println(e);
            JSONObject response = new JSONObject();

            response.put("code", 0);
            response.put("msg", "未知错误，请联系管理员");
            response.put("data", null);

            return response.toString();
        }

    }


    /**
     * 用户绑定查询
     */
    @RequestMapping(value = "/userBindStatus")
    @ResponseBody
    public String userBindStatus(@RequestParam(value = "token", required = false) String token) {

        JSONObject response = new JSONObject();
        try {
            Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
            if (uStatus == 0) {
                return Result.getResultJson(0, "用户未登录或Token验证失败", null);
            }
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            Integer uid = Integer.parseInt(map.get("uid").toString());
            TypechoUserapi userapi = new TypechoUserapi();
            userapi.setUid(uid);
            userapi.setAppLoginType("qq");
            Integer qqBind = userapiService.total(userapi);
            userapi.setAppLoginType("weixin");
            Integer weixinBind = userapiService.total(userapi);
            userapi.setAppLoginType("sinaweibo");
            Integer weiboBind = userapiService.total(userapi);
            Map jsonToMap = new HashMap();

            jsonToMap.put("qqBind", qqBind);
            jsonToMap.put("weixinBind", weixinBind);
            jsonToMap.put("weiboBind", weiboBind);

            response.put("code", 1);
            response.put("data", jsonToMap);
            response.put("msg", "");
            return response.toString();
        } catch (Exception e) {
            System.out.println(e);
            response.put("code", 0);
            response.put("data", "");
            response.put("msg", "数据异常");
            return response.toString();
        }

    }

    /***
     * 注册用户
     * @param params Bean对象JSON字符串
     */
    @RequestMapping(value = "/userRegister")
    @ResponseBody
    public String userRegister(@RequestParam(value = "params", required = false) String params) {
        TypechoUsers insert = null;
        Map jsonToMap = null;
        try{
            if (StringUtils.isNotBlank(params)) {
                jsonToMap = JSONObject.parseObject(JSON.parseObject(params).toString());
                //在之前需要做判断，验证用户名或者邮箱在数据库中是否存在
                //判断是否开启邮箱验证
                TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);
                Integer isEmail = apiconfig.getIsEmail();
                Integer isInvite = apiconfig.getIsInvite();
                //验证是否存在相同用户名或者邮箱
                TypechoUsers toKey = new TypechoUsers();
                if(isEmail.equals(1)) {

                    toKey.setMail(jsonToMap.get("mail").toString());
                    List isMail = service.selectList(toKey);
                    if (isMail.size() > 0) {
                        return Result.getResultJson(0, "该邮箱已注册", null);
                    }
                }
                toKey.setMail(null);
                toKey.setName(jsonToMap.get("name").toString());
                List isName = service.selectList(toKey);
                if (isName.size() > 0) {
                    return Result.getResultJson(0, "该用户名已注册", null);
                }
                //验证邮箱验证码
                if(isEmail.equals(1)){
                    String email = jsonToMap.get("mail").toString();
                    String code = jsonToMap.get("code").toString();
                    String cur_code = redisHelp.getRedis(this.dataprefix + "_" + "sendCode" + email, redisTemplate);
                    if (cur_code == null) {
                        return Result.getResultJson(0, "请先发送验证码", null);
                    }
                    if (!cur_code.equals(code)) {
                        return Result.getResultJson(0, "验证码不正确", null);
                    }
                }
                //验证邀请码
                if(isInvite.equals(1)){
                    if(jsonToMap.get("inviteCode")==null){
                        return Result.getResultJson(0, "请输入邀请码", null);
                    }
                    TypechoInvitation invitation = new TypechoInvitation();
                    invitation.setCode(jsonToMap.get("inviteCode").toString());
                    List<TypechoInvitation> invite = invitationService.selectList(invitation);
                    if(invite.size()<1){
                        return Result.getResultJson(0, "错误的邀请码", null);
                    }else{
                        TypechoInvitation cur = invite.get(0);
                        cur.setStatus(1);
                        invitationService.update(cur);
                    }

                }
                String p = jsonToMap.get("password").toString();
                String passwd = phpass.HashPassword(p);
                Long date = System.currentTimeMillis();
                String userTime = String.valueOf(date).substring(0, 10);
                jsonToMap.put("created", userTime);
                jsonToMap.put("group", "contributor");

                jsonToMap.put("password", passwd);
                //jsonToMap.remove("introduce");
                jsonToMap.remove("assets");
                jsonToMap.remove("customize");
                jsonToMap.remove("vip");
            }
            insert = JSON.parseObject(JSON.toJSONString(jsonToMap), TypechoUsers.class);
            int rows = service.insert(insert);

            JSONObject response = new JSONObject();
            response.put("code", rows > 0 ? 1 : 0);
            response.put("data", rows);
            response.put("msg", rows > 0 ? "注册成功" : "注册失败");
            return response.toString();
        }catch (Exception e){
            return Result.getResultJson(0, "参数错误", null);
        }


    }

    /**
     * 登陆后操作的邮箱验证
     */
    @RequestMapping(value = "/SendCode")
    @ResponseBody
    public String SendCode(@RequestParam(value = "params", required = false) String params, HttpServletRequest request) throws MessagingException {
        try{
            Map jsonToMap = null;

            TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);
            Integer isEmail = apiconfig.getIsEmail();
            if(isEmail.equals(0)){
                return Result.getResultJson(0, "邮箱验证已经关闭", null);
            }
            String  agent =  request.getHeader("User-Agent");
            String  ip = baseFull.getIpAddr(request);

            String iSsendCode = redisHelp.getRedis(this.dataprefix + "_" + "iSsendCode_"+agent+"_"+ip, redisTemplate);
            if(iSsendCode==null){
                redisHelp.setRedis(this.dataprefix + "_" + "iSsendCode_"+agent+"_"+ip, "data", 30, redisTemplate);
            }else{
                return Result.getResultJson(0, "你的操作太频繁了", null);
            }
            if (StringUtils.isNotBlank(params)) {
                jsonToMap = JSONObject.parseObject(JSON.parseObject(params).toString());
                Map keyName = new HashMap<String, String>();
                keyName.put("name", jsonToMap.get("name").toString());
                TypechoUsers toKey1 = JSON.parseObject(JSON.toJSONString(keyName), TypechoUsers.class);
                List<TypechoUsers> isName = service.selectList(toKey1);


                if (isName.size() > 0) {
                    //生成六位随机验证码
                    Random random = new Random();
                    String code = "";
                    for (int i = 0; i < 6; i++) {
                        code += random.nextInt(10);
                    }
                    //存入redis并发送邮件
                    String name = isName.get(0).getName();
                    String email = isName.get(0).getMail();
                    redisHelp.delete(this.dataprefix + "_" + "sendCode" + name, redisTemplate);
                    redisHelp.setRedis(this.dataprefix + "_" + "sendCode" + name, code, 1800, redisTemplate);
                    MailService.send("你本次的验证码为" + code, "<!DOCTYPE html><html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" /><title></title><meta charset=\"utf-8\" /><style>*{padding:0px;margin:0px;box-sizing:border-box;}html{box-sizing:border-box;}body{font-size:15px;background:#fff}.main{margin:20px auto;max-width:500px;border:solid 1px #2299dd;overflow:hidden;}.main h1{display:block;width:100%;background:#2299dd;font-size:18px;color:#fff;text-align:center;padding:15px;}.text{padding:30px;}.text p{margin:10px 0px;line-height:25px;}.text p span{color:#2299dd;font-weight:bold;font-size:22px;margin-left:5px;}</style></head><body><div class=\"main\"><h1>用户验证码</h1><div class=\"text\"><p>用户 " + name + "，你本次的验证码为<span>" + code + "</span>。</p><p>出于安全原因，该验证码将于30分钟后失效。请勿将验证码透露给他人。</p></div></div></body></html>",
                            new String[]{email}, new String[]{});
                    return Result.getResultJson(1, "邮件发送成功", null);
                } else {
                    return Result.getResultJson(0, "该用户不存在", null);
                }

            } else {
                return Result.getResultJson(0, "参数错误", null);
            }
        }catch (Exception e){
            return Result.getResultJson(0, "不正确的邮箱发信配置", null);
        }



    }

    /**
     * 注册邮箱验证
     */
    @RequestMapping(value = "/RegSendCode")
    @ResponseBody
    public String RegSendCode(@RequestParam(value = "params", required = false) String params, HttpServletRequest request) throws MessagingException {
        try{
            Map jsonToMap = null;
            TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);
            Integer isEmail = apiconfig.getIsEmail();
            if(isEmail.equals(0)){
                return Result.getResultJson(0, "邮箱验证已经关闭", null);
            }
            String  agent =  request.getHeader("User-Agent");
            String  ip = baseFull.getIpAddr(request);

            String regISsendCode = redisHelp.getRedis(this.dataprefix + "_" + "regISsendCode_"+agent+"_"+ip, redisTemplate);
            if(regISsendCode==null){
                redisHelp.setRedis(this.dataprefix + "_" + "regISsendCode_"+agent+"_"+ip, "data", 30, redisTemplate);
            }else{
                return Result.getResultJson(0, "你的操作太频繁了", null);
            }
            if (StringUtils.isNotBlank(params)) {
                jsonToMap = JSONObject.parseObject(JSON.parseObject(params).toString());
                String email = jsonToMap.get("mail").toString();
                if (!baseFull.isEmail(email)) {
                    return Result.getResultJson(0, "请输入正确的邮箱", null);
                }
                //判断邮箱是否寻找
                Map keyMail = new HashMap<String, String>();
                keyMail.put("mail", jsonToMap.get("mail").toString());
                TypechoUsers toKey1 = JSON.parseObject(JSON.toJSONString(keyMail), TypechoUsers.class);
                List<TypechoUsers> isName = service.selectList(toKey1);
                if (isName.size() > 0) {
                    return Result.getResultJson(0, "该邮箱已被注册", null);
                }

                //生成六位随机验证码
                Random random = new Random();
                String code = "";
                for (int i = 0; i < 6; i++) {
                    code += random.nextInt(10);
                }
                //存入redis并发送邮件
                redisHelp.delete(this.dataprefix + "_" + "sendCode" + email, redisTemplate);
                redisHelp.setRedis(this.dataprefix + "_" + "sendCode" + email, code, 1800, redisTemplate);
                MailService.send("你本次的验证码为" + code, "<!DOCTYPE html><html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" /><title></title><meta charset=\"utf-8\" /><style>*{padding:0px;margin:0px;box-sizing:border-box;}html{box-sizing:border-box;}body{font-size:15px;background:#fff}.main{margin:20px auto;max-width:500px;border:solid 1px #2299dd;overflow:hidden;}.main h1{display:block;width:100%;background:#2299dd;font-size:18px;color:#fff;text-align:center;padding:15px;}.text{padding:30px;}.text p{margin:10px 0px;line-height:25px;}.text p span{color:#2299dd;font-weight:bold;font-size:22px;margin-left:5px;}</style></head><body><div class=\"main\"><h1>用户验证码</h1><div class=\"text\"><p>你本次的验证码为<span>" + code + "</span>。</p><p>出于安全原因，该验证码将于30分钟后失效。请勿将验证码透露给他人。</p></div></div></body></html>",
                        new String[]{email}, new String[]{});
                return Result.getResultJson(1, "邮件发送成功", null);
            } else {
                return Result.getResultJson(0, "参数错误", null);
            }
        }catch (Exception e){
            return Result.getResultJson(0, "不正确的邮箱发信配置", null);
        }


    }

    /***
     * 找回密码
     * @param params Bean对象JSON字符串
     */
    @RequestMapping(value = "/userFoget")
    @ResponseBody
    public String userFoget(@RequestParam(value = "params", required = false) String params) {
        try {
            TypechoUsers update = null;
            Map jsonToMap = null;
            if (StringUtils.isNotBlank(params)) {
                TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);
                Integer isEmail = apiconfig.getIsEmail();
                if(isEmail.equals(0)){
                    return Result.getResultJson(0, "邮箱验证已经关闭，请联系管理员找回密码", null);
                }
                jsonToMap = JSONObject.parseObject(JSON.parseObject(params).toString());
                String code = jsonToMap.get("code").toString();
                String name = jsonToMap.get("name").toString();
                //从redis获取验证码
                String sendCode = null;
                if (redisHelp.getRedis(this.dataprefix + "_" + "sendCode" + name, redisTemplate) != null) {
                    sendCode = redisHelp.getRedis(this.dataprefix + "_" + "sendCode" + name, redisTemplate);
                } else {
                    return Result.getResultJson(0, "验证码已超时或未发送", null);
                }
                if (!sendCode.equals(code)) {
                    return Result.getResultJson(0, "验证码不正确", null);
                }
                redisHelp.delete(this.dataprefix + "_" + "sendCode" + name, redisTemplate);
                String p = jsonToMap.get("password").toString();
                String passwd = phpass.HashPassword(p);
                jsonToMap.put("password", passwd);
                jsonToMap.remove("code");

                Map keyName = new HashMap<String, String>();
                keyName.put("name", jsonToMap.get("name").toString());
                TypechoUsers toKey1 = JSON.parseObject(JSON.toJSONString(keyName), TypechoUsers.class);
                List<TypechoUsers> isName = service.selectList(toKey1);
                if (isName.size() == 0) {
                    return Result.getResultJson(0, "用户不存在", null);
                }

                Map updateMap = new HashMap<String, String>();
                updateMap.put("uid", isName.get(0).getUid().toString());
                updateMap.put("name", jsonToMap.get("name").toString());
                updateMap.put("password", jsonToMap.get("password").toString());

                update = JSON.parseObject(JSON.toJSONString(updateMap), TypechoUsers.class);
            }

            int rows = service.update(update);

            JSONObject response = new JSONObject();
            response.put("code", rows > 0 ? 1 : 0);
            response.put("data", rows);
            response.put("msg", rows > 0 ? "操作成功" : "操作失败");
            return response.toString();
        } catch (Exception e) {
            System.out.println(e);
            return Result.getResultJson(0, "操作失败", null);
        }

    }

    /***
     * 用户修改
     * @param params Bean对象JSON字符串
     */
    @RequestMapping(value = "/userEdit")
    @ResponseBody
    public String userEdit(@RequestParam(value = "params", required = false) String params, @RequestParam(value = "token", required = false) String token) {
        try {
            TypechoUsers update = null;
            Map jsonToMap = null;
            String code = "";
            Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
            if (uStatus == 0) {
                return Result.getResultJson(0, "用户未登录或Token验证失败", null);
            }
            if (StringUtils.isNotBlank(params)) {

                jsonToMap = JSONObject.parseObject(JSON.parseObject(params).toString());
                //根据验证码判断是否要修改邮箱
                if (jsonToMap.get("code") != null && jsonToMap.get("mail") != null) {
                    TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);
                    Integer isEmail = apiconfig.getIsEmail();
                    if(isEmail.equals(1)){
                        String email = jsonToMap.get("mail").toString();
                        if (redisHelp.getRedis(this.dataprefix + "_" + "sendCode" + email, redisTemplate) != null) {
                            String sendCode = redisHelp.getRedis(this.dataprefix + "_" + "sendCode" + email, redisTemplate);
                            code = jsonToMap.get("code").toString();
                            if (!sendCode.equals(code)) {
                                return Result.getResultJson(0, "验证码不正确", null);
                            }
                        } else {
                            return Result.getResultJson(0, "验证码不正确或已失效", null);
                        }
                    }

                } else {
                    jsonToMap.remove("mail");
                }
                jsonToMap.remove("code");
                if (jsonToMap.get("password") != null) {
                    String p = jsonToMap.get("password").toString();
                    String passwd = phpass.HashPassword(p);
                    jsonToMap.put("password", passwd);
                }
                Map keyName = new HashMap<String, String>();
                keyName.put("name", jsonToMap.get("name").toString());
                TypechoUsers toKey1 = JSON.parseObject(JSON.toJSONString(keyName), TypechoUsers.class);
                List<TypechoUsers> isName = service.selectList(toKey1);
                if (isName.size() == 0) {
                    return Result.getResultJson(0, "用户不存在", null);
                }


                jsonToMap.remove("name");
                jsonToMap.remove("group");
                //部分字段不允许修改
                jsonToMap.remove("customize");
                jsonToMap.remove("created");
                jsonToMap.remove("activated");
                jsonToMap.remove("logged");
                jsonToMap.remove("authCode");
                //jsonToMap.remove("introduce");
                jsonToMap.remove("assets");
                update = JSON.parseObject(JSON.toJSONString(jsonToMap), TypechoUsers.class);
            }

            int rows = service.update(update);

            if (rows > 0 && jsonToMap.get("password") != null) {
                //执行成功后，如果密码发生了改变，需要重新登陆
                redisHelp.delete(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            }
            if (rows > 0 && jsonToMap.get("mail") != null) {
                //执行成功后，如果邮箱发生了改变，则重新登陆
                redisHelp.delete(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            }
            JSONObject response = new JSONObject();
            response.put("code", rows > 0 ? 1 : 0);
            response.put("data", rows);
            response.put("msg", rows > 0 ? "操作成功" : "操作失败");
            return response.toString();
        } catch (Exception e) {
            System.out.println(e);
            return Result.getResultJson(0, "操作失败", null);
        }

    }

    /***
     * 用户修改（管理员）
     * @param params Bean对象JSON字符串
     */
    @RequestMapping(value = "/manageUserEdit")
    @ResponseBody
    public String manageUserEdit(@RequestParam(value = "params", required = false) String params, @RequestParam(value = "token", required = false) String token) {
        try {
            TypechoUsers update = null;
            Map jsonToMap = null;
            String code = "";
            Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
            if (uStatus == 0) {
                return Result.getResultJson(0, "用户未登录或Token验证失败", null);
            }
            String name = "";
            if (StringUtils.isNotBlank(params)) {

                jsonToMap = JSONObject.parseObject(JSON.parseObject(params).toString());
                name = jsonToMap.get("name").toString();
                if (jsonToMap.get("password") != null) {
                    String p = jsonToMap.get("password").toString();
                    String passwd = phpass.HashPassword(p);
                    jsonToMap.put("password", passwd);
                }
                Map keyName = new HashMap<String, String>();
                keyName.put("name", jsonToMap.get("name").toString());
                TypechoUsers toKey1 = JSON.parseObject(JSON.toJSONString(keyName), TypechoUsers.class);
                List<TypechoUsers> isName = service.selectList(toKey1);
                if (isName.size() == 0) {
                    return Result.getResultJson(0, "用户不存在", null);
                }


                jsonToMap.remove("name");
                jsonToMap.remove("group");
                //部分字段不允许修改

                jsonToMap.remove("created");
                jsonToMap.remove("activated");
                jsonToMap.remove("logged");
                jsonToMap.remove("authCode");
                //jsonToMap.remove("introduce");
                jsonToMap.remove("assets");
                update = JSON.parseObject(JSON.toJSONString(jsonToMap), TypechoUsers.class);
            }

            int rows = service.update(update);
            //修改后让用户强制重新登陆

            String oldToken = null;
            if (redisHelp.getRedis(this.dataprefix + "_" + "userkey" + name, redisTemplate) != null) {
                oldToken = redisHelp.getRedis(this.dataprefix + "_" + "userkey" + name, redisTemplate);
            }
            if (oldToken != null) {
                redisHelp.delete(this.dataprefix + "_" + "userInfo" + oldToken, redisTemplate);
                redisHelp.delete(this.dataprefix + "_" + "userkey" + name, redisTemplate);
            }
            JSONObject response = new JSONObject();
            response.put("code", rows > 0 ? 1 : 0);
            response.put("data", rows);
            response.put("msg", rows > 0 ? "操作成功" : "操作失败");
            return response.toString();
        } catch (Exception e) {
            System.out.println(e);
            return Result.getResultJson(0, "操作失败", null);
        }

    }

    /***
     * 用户状态检测
     *
     */
    @RequestMapping(value = "/userStatus")
    @ResponseBody
    public String userStatus(@RequestParam(value = "token", required = false) String token) {
        Map jsonToMap = null;
        Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);

        if (uStatus == 0) {
            return Result.getResultJson(0, "用户未登录或Token验证失败", null);
        } else {
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            Integer uid = Integer.parseInt(map.get("uid").toString());
            TypechoUsers users = service.selectByKey(uid);
            Map json = JSONObject.parseObject(JSONObject.toJSONString(users), Map.class);
            TypechoComments comments = new TypechoComments();
            comments.setAuthorId(uid);
            Integer lv = commentsService.total(comments);
            json.remove("password");
            //判断是否为VIP
            json.put("isvip", 0);
            Long date = System.currentTimeMillis();
            String curTime = String.valueOf(date).substring(0, 10);
            Integer viptime  = users.getVip();
            if(viptime>Integer.parseInt(curTime)||viptime.equals(1)){
                json.put("isvip", 1);
            }
            json.put("lv", baseFull.getLv(lv));
            JSONObject response = new JSONObject();

            response.put("code", 1);
            response.put("msg", "");
            response.put("data", json);

            return response.toString();
        }
    }

    /***
     * 用户删除
     */
    @RequestMapping(value = "/userDelete")
    @ResponseBody
    public String userDelete(@RequestParam(value = "key", required = false) String key, @RequestParam(value = "token", required = false) String token) {
        try {
            Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
            if (uStatus == 0) {
                return Result.getResultJson(0, "用户未登录或Token验证失败", null);
            }
            //String group = (String) redisHelp.getValue("userInfo"+token,"group",redisTemplate);
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            String group = map.get("group").toString();
            if (!group.equals("administrator")) {
                return Result.getResultJson(0, "你没有操作权限", null);
            }
            int rows = service.delete(key);
            JSONObject response = new JSONObject();
            response.put("code", rows > 0 ? 1 : 0);
            response.put("data", rows);
            response.put("msg", rows > 0 ? "操作成功" : "操作失败");
            return response.toString();
        } catch (Exception e) {
            System.out.println(e);
            return Result.getResultJson(0, "操作失败", null);
        }

    }

    /***
     * 发起提现
     */
    @RequestMapping(value = "/userWithdraw")
    @ResponseBody
    public String userWithdraw(@RequestParam(value = "num", required = false) Integer num, @RequestParam(value = "token", required = false) String token) {
        try {
            Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
            if (uStatus == 0) {
                return Result.getResultJson(0, "用户未登录或Token验证失败", null);
            }
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            Integer uid = Integer.parseInt(map.get("uid").toString());
            //查询用户是否设置pay
            TypechoUsers user = service.selectByKey(uid);
            if (user.getPay() == null) {
                return Result.getResultJson(0, "请先设置收款信息", null);
            }
            Long date = System.currentTimeMillis();
            String userTime = String.valueOf(date).substring(0, 10);
            TypechoUserlog userlog = new TypechoUserlog();
            userlog.setUid(uid);
            userlog.setType("withdraw");
            userlog.setCid(-1);

            List<TypechoUserlog> list = userlogService.selectList(userlog);
            if(list.size()>0){
                return Result.getResultJson(0, "您有正在审核的申请", null);
            }
            userlog.setNum(num);
            userlog.setToid(uid);
            userlog.setCreated(Integer.parseInt(userTime));
            Integer rows = userlogService.insert(userlog);
            JSONObject response = new JSONObject();
            response.put("code", rows > 0 ? 1 : 0);
            response.put("data", rows);
            response.put("msg", rows > 0 ? "操作成功" : "操作失败");
            return response.toString();
        } catch (Exception e) {
            System.out.println(e);
            return Result.getResultJson(0, "操作失败", null);
        }

    }

    /***
     * 提现列表
     */
    @RequestMapping(value = "/withdrawList")
    @ResponseBody
    public String withdrawList(@RequestParam(value = "searchParams", required = false) String searchParams,
                               @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                               @RequestParam(value = "limit", required = false, defaultValue = "15") Integer limit,
                               @RequestParam(value = "token", required = false) String token) {
        Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
        if (uStatus == 0) {
            return Result.getResultJson(0, "用户未登录或Token验证失败", null);
        }
        if(limit>50){
            limit = 50;
        }
        TypechoUserlog query = new TypechoUserlog();
        if (StringUtils.isNotBlank(searchParams)) {

            JSONObject object = JSON.parseObject(searchParams);
            query.setType("withdraw");
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            Integer uid = Integer.parseInt(map.get("uid").toString());
            String group = map.get("group").toString();
            //不是管理员就只能看自己的提现记录
            if (!group.equals("administrator")) {
                object.put("uid", uid);

            }
            if (object.get("uid") != null) {

                query.setUid(Integer.parseInt(object.get("uid").toString()));
            }
            if (object.get("cid") != null) {

                query.setCid(Integer.parseInt(object.get("cid").toString()));
            }


        }

        PageList<TypechoUserlog> pageList = userlogService.selectPage(query, page, limit);
        List jsonList = new ArrayList();
        List<TypechoUserlog> list = pageList.getList();
        for (int i = 0; i < list.size(); i++) {
            Map json = JSONObject.parseObject(JSONObject.toJSONString(list.get(i)), Map.class);
            Integer uuid = list.get(i).getUid();
            TypechoUsers userinfo = service.selectByKey(uuid);
            String pay = userinfo.getPay();
            json.put("pay", pay);
            jsonList.add(json);
        }
        JSONObject response = new JSONObject();
        response.put("code", 1);
        response.put("msg", "");
        response.put("data", null != jsonList ? jsonList : new JSONArray());
        response.put("count", jsonList.size());
        return response.toString();
    }

    /***
     * 提现审核
     */
    @RequestMapping(value = "/withdrawStatus")
    @ResponseBody
    public String withdrawStatus(@RequestParam(value = "key", required = false) Integer key, @RequestParam(value = "type", required = false) Integer type, @RequestParam(value = "token", required = false) String token) {
        try {
            Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
            if (uStatus == 0) {
                return Result.getResultJson(0, "用户未登录或Token验证失败", null);
            }
            //String group = (String) redisHelp.getValue("userInfo"+token,"group",redisTemplate);
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            String group = map.get("group").toString();
            if (!group.equals("administrator")) {
                return Result.getResultJson(0, "你没有操作权限", null);
            }

            TypechoUserlog userlog = userlogService.selectByKey(key);
            //审核通过，则开始扣费和改状态
            if (type.equals(1)) {
                Integer num = userlog.getNum();
                Integer uid = userlog.getUid();
                TypechoUsers user = service.selectByKey(uid);
                Integer oldAssets = user.getAssets();
                if(oldAssets<num){
                    return Result.getResultJson(0, "该用户资产已不足以用于提现！", null);
                }
                Integer assets = oldAssets - num;
                user.setAssets(assets);
                service.update(user);
                userlog.setCid(0);
            } else {
                userlog.setCid(-2);
            }
            Integer rows = userlogService.update(userlog);
            JSONObject response = new JSONObject();
            response.put("code", rows > 0 ? 1 : 0);
            response.put("data", rows);
            response.put("msg", rows > 0 ? "操作成功" : "操作失败");
            return response.toString();
        } catch (Exception e) {
            System.out.println(e);
            return Result.getResultJson(0, "操作失败", null);
        }

    }

    /***
     * 管理员手动充扣
     */
    @RequestMapping(value = "/userRecharge")
    @ResponseBody
    public String userRecharge(@RequestParam(value = "key", required = false) Integer key, @RequestParam(value = "num", required = false) Integer num, @RequestParam(value = "type", required = false) Integer type, @RequestParam(value = "token", required = false) String token) {
        Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
        if (uStatus == 0) {
            return Result.getResultJson(0, "用户未登录或Token验证失败", null);
        }
        //String group = (String) redisHelp.getValue("userInfo"+token,"group",redisTemplate);
        Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
        String group = map.get("group").toString();
        if (!group.equals("administrator")) {
            return Result.getResultJson(0, "你没有操作权限", null);
        }
        TypechoUsers user = service.selectByKey(key);
        Integer oldAssets = user.getAssets();
        if (num <= 0) {
            return Result.getResultJson(0, "金额不正确", null);
        }
        Integer assets;
        //生成系统对用户资产操作的日志
        Long date = System.currentTimeMillis();
        String userTime = String.valueOf(date).substring(0,10);
        TypechoPaylog paylog = new TypechoPaylog();
        paylog.setStatus(1);
        paylog.setCreated(Integer.parseInt(userTime));
        paylog.setUid(key);
        paylog.setOutTradeNo(userTime+"system");
        paylog.setPaytype("system");
        //0是充值，1是扣款
        if (type.equals(0)) {
            assets = oldAssets + num;
            paylog.setTotalAmount(num+"");
            paylog.setSubject("系统充值");
        } else {
            assets = oldAssets - num;
            paylog.setTotalAmount("-"+num);
            paylog.setSubject("系统扣款");
        }
        paylogService.insert(paylog);
        user.setAssets(assets);
        Integer rows = service.update(user);
        JSONObject response = new JSONObject();
        response.put("code", rows > 0 ? 1 : 0);
        response.put("data", rows);
        response.put("msg", rows > 0 ? "操作成功" : "操作失败");
        return response.toString();
    }

    /**
     * 扫码登陆-生成二维码
     **/
    @RequestMapping(value = "/getScan")
    @ResponseBody
    public void getScan(@RequestParam(value = "codeContent", required = false) String codeContent, HttpServletResponse response) {
        redisHelp.setRedis(codeContent, "nodata", 90, redisTemplate);
        JSONObject res = new JSONObject();
        res.put("type", "Scan");
        res.put("data", codeContent);
        try {
            /*
             * 调用工具类生成二维码并输出到输出流中
             */
            QRCodeUtil.createCodeToOutputStream(res.toString(), response.getOutputStream());
            System.out.println("成功生成二维码!");
        } catch (IOException e) {
            System.out.println("发生错误");
        }
    }

    /**
     * 扫码登陆-获取扫码状态
     **/
    @RequestMapping(value = "/getScanStatus")
    @ResponseBody
    public String getScanStatus(@RequestParam(value = "codeContent", required = false) String codeContent) {
        String value = redisHelp.getRedis(codeContent, redisTemplate);
        if (value == null) {
            return Result.getResultJson(-1, "二维码已过期", null);
        }
        if (value.equals("nodata")) {
            return Result.getResultJson(0, "未扫码", null);
        }
        String token = value;
        Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
        Integer uid = Integer.parseInt(map.get("uid").toString());
        TypechoUsers users = service.selectByKey(uid);
        Map json = JSONObject.parseObject(JSONObject.toJSONString(users), Map.class);
        TypechoComments comments = new TypechoComments();
        comments.setAuthorId(uid);
        Integer lv = commentsService.total(comments);
        json.remove("password");
        json.put("lv", baseFull.getLv(lv));
        json.put("token", token);
        TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);
        if (json.get("mail") != null) {
            json.put("avatar", baseFull.getAvatar(apiconfig.getWebinfoAvatar(), json.get("mail").toString()));

        } else {
            json.put("avatar", apiconfig.getWebinfoAvatar() + "null");
        }
        //判断是否为VIP
        json.put("vip", users.getVip());
        json.put("isvip", 0);
        Long date = System.currentTimeMillis();
        String curTime = String.valueOf(date).substring(0, 10);
        Integer viptime  = users.getVip();
        if(viptime>Integer.parseInt(curTime)||viptime.equals(1)){
            json.put("isvip", 1);
        }
        JSONObject response = new JSONObject();

        response.put("code", 1);
        response.put("msg", "");
        response.put("data", json);

        return response.toString();
    }
    /**
     * 扫码登陆-app载入token
     **/
    @RequestMapping(value = "/setScan")
    @ResponseBody
    public String setScan(@RequestParam(value = "codeContent", required = false) String codeContent, @RequestParam(value = "token", required = false) String token) {
        try {
            String value = redisHelp.getRedis(codeContent, redisTemplate);
            if (value == null) {
                return Result.getResultJson(0, "二维码已过期", null);
            }
            redisHelp.setRedis(codeContent, token, 90, redisTemplate);
            return Result.getResultJson(1, "操作成功！", null);
        }catch (Exception e){
            return Result.getResultJson(0, "请求异常", null);
        }

    }
    /***
     * 注册系统配置信息
     */
    @RequestMapping(value = "/regConfig")
    @ResponseBody
    public String regConfig() {
        JSONObject data = new JSONObject();
        TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);
        data.put("isEmail",apiconfig.getIsEmail());
        data.put("isInvite",apiconfig.getIsInvite());
        JSONObject response = new JSONObject();
        response.put("code" , 1);
        response.put("data" , data);
        response.put("msg"  , "");
        return response.toString();
    }

    /**
     * 创建邀请码
     * **/
    @RequestMapping(value = "/madeInvitation")
    @ResponseBody
    public String madeInvitation(@RequestParam(value = "num", required = false) Integer  num,@RequestParam(value = "token", required = false) String  token) {
        try{
            Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
            if (uStatus == 0) {
                return Result.getResultJson(0, "用户未登录或Token验证失败", null);
            }
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            String group = map.get("group").toString();;
            Integer uid =Integer.parseInt(map.get("uid").toString());
            if (!group.equals("administrator")) {
                return Result.getResultJson(0, "你没有操作权限", null);
            }
            if(num>100){
                num = 100;
            }

            Long date = System.currentTimeMillis();
            String curTime = String.valueOf(date).substring(0, 10);
            //循环生成卡密
            for (int i = 0; i < num; i++) {
                TypechoInvitation invitation = new TypechoInvitation();
                String code = baseFull.createRandomStr(8);
                invitation.setCode(code);
                invitation.setStatus(0);
                invitation.setCreated(Integer.parseInt(curTime));
                invitation.setUid(uid);
                invitationService.insert(invitation);
            }
            JSONObject response = new JSONObject();
            response.put("code" , 1);
            response.put("msg"  , "生成邀请码成功");
            return response.toString();
        }catch (Exception e){
            JSONObject response = new JSONObject();
            response.put("code" , 1);
            response.put("msg"  , "生成邀请码失败");
            return response.toString();
        }
    }

    /***
     * 邀请码列表
     *
     */
    @RequestMapping(value = "/invitationList")
    @ResponseBody
    public String invitationList (@RequestParam(value = "searchParams", required = false) String  searchParams,
                                @RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                                @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit,
                                @RequestParam(value = "token", required = false) String  token) {
        Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
        if (uStatus == 0) {
            return Result.getResultJson(0, "用户未登录或Token验证失败", null);
        }
        Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
        String group = map.get("group").toString();
        if (!group.equals("administrator")) {
            return Result.getResultJson(0, "你没有操作权限", null);
        }
        TypechoInvitation query = new TypechoInvitation();
        if (StringUtils.isNotBlank(searchParams)) {
            JSONObject object = JSON.parseObject(searchParams);
            query = object.toJavaObject(TypechoInvitation.class);
        }

        PageList<TypechoInvitation> pageList = invitationService.selectPage(query, page, limit);
        JSONObject response = new JSONObject();
        response.put("code" , 1);
        response.put("msg"  , "");
        response.put("data" , null != pageList.getList() ? pageList.getList() : new JSONArray());
        response.put("count", pageList.getTotalCount());
        return response.toString();
    }
    /***
     * 导出邀请码
     *
     */
    @RequestMapping(value = "/invitationExcel")
    @ResponseBody
    public void invitationExcel(@RequestParam(value = "limit" , required = false, defaultValue = "15") Integer limit,@RequestParam(value = "token", required = false) String  token,HttpServletResponse response) throws IOException {
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet("邀请码列表");

        Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
        if (uStatus == 0) {
            response.setContentType("application/octet-stream");
            response.setHeader("Content-disposition", "attachment;filename=nodata.xls");
            response.flushBuffer();
            workbook.write(response.getOutputStream());
        }
        Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
        String group = map.get("group").toString();
        if (!group.equals("administrator")) {
            response.setContentType("application/octet-stream");
            response.setHeader("Content-disposition", "attachment;filename=nodata.xls");
            response.flushBuffer();
            workbook.write(response.getOutputStream());
        }
        TypechoInvitation query = new TypechoInvitation();
        PageList<TypechoInvitation> pageList = invitationService.selectPage(query, 1, limit);
        List<TypechoInvitation> list = pageList.getList();




        String fileName = "InvitationExcel"  + ".xls";//设置要导出的文件的名字
        //新增数据行，并且设置单元格数据

        int rowNum = 1;

        String[] headers = { "ID", "邀请码", "创建人"};
        //headers表示excel表中第一行的表头

        HSSFRow row = sheet.createRow(0);
        //在excel表中添加表头

        for(int i=0;i<headers.length;i++){
            HSSFCell cell = row.createCell(i);
            HSSFRichTextString text = new HSSFRichTextString(headers[i]);
            cell.setCellValue(text);
        }
        for (TypechoInvitation Invitation : list) {
            HSSFRow row1 = sheet.createRow(rowNum);
            row1.createCell(0).setCellValue(Invitation.getId());
            row1.createCell(1).setCellValue(Invitation.getCode());
            row1.createCell(2).setCellValue(Invitation.getUid());
            rowNum++;
        }

        response.setContentType("application/octet-stream");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName);
        response.flushBuffer();
        workbook.write(response.getOutputStream());
    }
}