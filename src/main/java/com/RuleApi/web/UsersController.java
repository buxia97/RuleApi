package com.RuleApi.web;

import com.RuleApi.annotation.LoginRequired;
import com.RuleApi.common.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.RuleApi.entity.*;
import com.RuleApi.service.*;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.aliyun.dysmsapi20170525.Client;

import javax.imageio.ImageIO;
import javax.mail.MessagingException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

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
public class UsersController {

    @Autowired
    UsersService service;

    @Autowired
    private ContentsService contentsService;

    @Autowired
    private CommentsService commentsService;

    @Autowired
    private EmailtemplateService emailtemplateService;

    @Autowired
    private UserlogService userlogService;

    @Autowired
    private UserapiService userapiService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private PaylogService paylogService;

    @Autowired
    private AllconfigService allconfigService;

    @Autowired
    private InvitationService invitationService;

    @Autowired
    private DefaultKaptcha captchaProducer;

    @Autowired
    private InboxService inboxService;

    @Autowired
    private FanService fanService;

    @Autowired
    private ViolationService violationService;

    @Autowired
    private PushService pushService;


    @Autowired
    MailService MailService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;


    @Value("${mybatis.configuration.variables.prefix}")
    private String prefix;

    @Value("${webinfo.usertime}")
    private Integer usertime;

    @Value("${webinfo.userCache}")
    private Integer userCache;


    @Value("${web.prefix}")
    private String dataprefix;


    emailResult emailText = new emailResult();
    RedisHelp redisHelp = new RedisHelp();
    ResultAll Result = new ResultAll();
    baseFull baseFull = new baseFull();
    UserStatus UStatus = new UserStatus();
    HttpClient HttpClient = new HttpClient();
    PHPass phpass = new PHPass(8);
    EditFile editFile = new EditFile();

    /***
     * 用户查询
     * @param searchParams Bean对象JSON字符串
     * @param page         页码
     * @param limit        每页显示数量
     */
    @RequestMapping(value = "/userList")
    @ResponseBody
    @LoginRequired(purview = "-1")
    public String userList(@RequestParam(value = "searchParams", required = false) String searchParams,
                           @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                           @RequestParam(value = "searchKey", required = false, defaultValue = "") String searchKey,
                           @RequestParam(value = "order", required = false, defaultValue = "") String order,
                           @RequestParam(value = "limit", required = false, defaultValue = "15") Integer limit,
                           @RequestParam(value = "token", required = false, defaultValue = "") String token) {
        TypechoUsers query = new TypechoUsers();
        String sqlParams = "null";
        if(limit>50){
            limit = 50;
        }
        //如果开启全局登录，则必须登录才能得到数据
        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
        if(apiconfig.get("isLogin").toString().equals("1")){
            if(uStatus==0){
                return Result.getResultJson(0,"用户未登录或Token验证失败",null);
            }
        }
        //验证结束
        Integer total = 0;
        if (StringUtils.isNotBlank(searchParams)) {
            JSONObject object = JSON.parseObject(searchParams);
            object.remove("password");
            query = object.toJavaObject(TypechoUsers.class);
            Map paramsJson = JSONObject.parseObject(JSONObject.toJSONString(query), Map.class);
            sqlParams = paramsJson.toString();
        }
        total = service.total(query,searchKey);
        List jsonList = new ArrayList();
        List cacheList = new ArrayList();
        //如果是管理员，则不缓存且显示用户资产
        Integer isAdmin = 0;
        Map map = new HashMap();
        Integer logUid = 0;
        String group = "";
        if (!uStatus.equals(0)) {
            map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            logUid =Integer.parseInt(map.get("uid").toString());
            group = map.get("group").toString();
            if (group.equals("administrator")||group.equals("editor")) {
                isAdmin = 1;
            }
        }
        if(isAdmin.equals(0)){
            cacheList = redisHelp.getList(this.dataprefix + "_" + "userList_" + page + "_" + limit + "_" + sqlParams + "_" + order + "_" + searchKey, redisTemplate);
        }

        try {
            if (cacheList.size() > 0) {
                jsonList = cacheList;
            } else {
                PageList<TypechoUsers> pageList = service.selectPage(query, page, limit, searchKey, order);
                List<TypechoUsers> list = pageList.getList();
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
                    TypechoUsers userInfo = list.get(i);
                    //获取用户等级
                    Integer uid = Integer.parseInt(json.get("uid").toString());
//                    TypechoComments comments = new TypechoComments();
//                    comments.setAuthorId(uid);
//                    Integer lv = commentsService.total(comments,null);
//                    json.put("lv", baseFull.getLv(lv));

                    json.remove("password");
                    json.remove("address");
                    json.remove("authCode");
                    json.remove("pay");
                    if (!group.equals("administrator")) {
                        json.remove("assets");
                    }
                    if(json.get("avatar")==null){
                        if (json.get("mail") != null) {

                            String mail = json.get("mail").toString();

                            if(mail.indexOf("@qq.com") != -1){
                                String qq = mail.replace("@qq.com","");
                                json.put("avatar", "https://q1.qlogo.cn/g?b=qq&nk="+qq+"&s=640");
                            }else{
                                json.put("avatar", baseFull.getAvatar(apiconfig.get("webinfoAvatar").toString(), mail));
                            }
                        } else {
                            json.put("avatar", apiconfig.get("webinfoAvatar").toString() + "null");
                        }
                    }else{

                    }
                    json.put("isvip", 0);
                    Long date = System.currentTimeMillis();
                    String curTime = String.valueOf(date).substring(0, 10);
                    Integer viptime  = userInfo.getVip();
                    if(viptime>Integer.parseInt(curTime)){
                        json.put("isvip", 1);
                    }
                    if(viptime.equals(1)){
                        //永久VIP
                        json.put("isvip", 2);
                    }

                    json.remove("mail");
                    json.remove("phone");
                    if (uStatus != 0) {
                        TypechoFan fan = new TypechoFan();
                        fan.setUid(logUid);
                        fan.setTouid(uid);
                        Integer isFollow = fanService.total(fan);
                        json.put("isFollow",isFollow);
                    }else{
                        json.put("isFollow",0);
                    }

                    jsonList.add(json);

                }
                redisHelp.delete(this.dataprefix + "_" + "userList_" + page + "_" + limit + "_" + sqlParams + "_" + order + "_" + searchKey, redisTemplate);
                redisHelp.setList(this.dataprefix + "_" + "userList_" + page + "_" + limit + "_" + sqlParams + "_" + order + "_" + searchKey, jsonList, this.userCache, redisTemplate);
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
        response.put("total", total);
        return response.toString();
    }

    /***
     * 用户数据
     */
    @RequestMapping(value = "/userData")
    @ResponseBody
    @LoginRequired(purview = "-1")
    public String userData(@RequestParam(value = "token", required = false) String token,
                           @RequestParam(value = "uid", required = false,defaultValue = "0") Integer uid) {
        Map json = new HashMap();
        try {

            Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
            if (uStatus == 0) {
                if(uid.equals(0)){
                    return Result.getResultJson(0,"参数不正确",null);
                }
            }else{
                if(uid.equals(0)){
                    Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
                    if(uid.equals(0)){
                        uid = Integer.parseInt(map.get("uid").toString());
                    }
                }
            }

            Map cacheInfo = redisHelp.getMapValue(this.dataprefix+"_"+"userData_"+uid,redisTemplate);
            if(cacheInfo.size()>0){
                json = cacheInfo;
            }else{
                //用户文章数量
                TypechoContents contents = new TypechoContents();
                contents.setType("post");
                contents.setStatus("publish");
                contents.setAuthorId(uid);
                Integer contentsNum = contentsService.total(contents,null);
                //用户评论数量
                TypechoComments comments = new TypechoComments();
                comments.setAuthorId(uid);
                Integer commentsNum = commentsService.total(comments,null);
                //用户资产和创建时间
                TypechoUsers user = service.selectByKey(uid);
                if(user==null){
                    return Result.getResultJson(0,"用户不存在",null);
                }
                Integer assets = user.getAssets();
                Integer points = user.getPoints();
                Integer created = user.getCreated();
                Integer experience = user.getExperience();
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
                //用户粉丝数量
                TypechoFan fan = new TypechoFan();
                fan.setTouid(uid);
                Integer fanNum = fanService.total(fan);
                //用户关注数量
                TypechoFan follow = new TypechoFan();
                follow.setUid(uid);
                Integer followNum = fanService.total(follow);

                String isSilence = redisHelp.getRedis(this.dataprefix+"_"+uid+"_silence",redisTemplate);
                if(isSilence!=null){
                    json.put("systemBan", 1);
                }else{
                    json.put("systemBan", 0);
                }
                TypechoInbox inbox = new TypechoInbox();
                inbox.setUid(uid);
                inbox.setType("selfDelete");
                List<TypechoInbox> list = inboxService.selectList(inbox);
                if(list.size()>0){
                    json.put("systemBan", 2);
                }
                json.put("contentsNum", contentsNum);
                json.put("commentsNum", commentsNum);
                json.put("assets", assets);
                json.put("points", points);
                json.put("created", created);
                json.put("experience", experience);
                json.put("isClock", isClock);
                json.put("fanNum", fanNum);
                json.put("followNum", followNum);
                redisHelp.delete(this.dataprefix+"_"+"userData_"+uid,redisTemplate);
                redisHelp.setKey(this.dataprefix+"_"+"userData_"+uid,json,5,redisTemplate);
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        JSONObject response = new JSONObject();
        response.put("code", 1);
        response.put("msg", "");
        response.put("data", json);

        return response.toString();
    }

    /***
     * 用户信息
     */
    @RequestMapping(value = "/userInfo")
    @ResponseBody
    @LoginRequired(purview = "-2")
    public String userInfo(@RequestParam(value = "key", required = false) String key,@RequestParam(value = "token", required = false, defaultValue = "") String token) {
        try {
            Map json = new HashMap();

            String group = "";
            Integer logUid = 0;
            Integer isVip = 0;
            Map map  = new HashMap();
            Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
            if (!uStatus.equals(0)) {
                map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
                group =map.get("group").toString();
                logUid =Integer.parseInt(map.get("uid").toString());
                isVip =Integer.parseInt(map.get("isvip").toString());
            }
            Map cacheInfo = redisHelp.getMapValue(this.dataprefix+"_"+"userInfo_"+key,redisTemplate);
            if(cacheInfo.size()>0){
                json = cacheInfo;
            }else{
                TypechoUsers info = service.selectByKey(key);
                if(info==null){
                    return Result.getResultJson(0, "请传入正确的参数", null);
                }
                json = JSONObject.parseObject(JSONObject.toJSONString(info), Map.class);
                //获取用户等级

                Integer uid = Integer.parseInt(key);

//                TypechoComments comments = new TypechoComments();
//                comments.setAuthorId(uid);
//                Integer lv = commentsService.total(comments,null);
//                json.put("lv", baseFull.getLv(lv));
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
                json.remove("clientId");
                json.remove("pay");
                if(map.size()>0){
                    if (!group.equals("administrator")) {
                        json.remove("assets");
                    }
                }else{
                    json.remove("assets");
                }


                Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
                if(json.get("avatar")==null){
                    if (json.get("mail") != null) {
                        String mail = json.get("mail").toString();

                        if(mail.indexOf("@qq.com") != -1){
                            String qq = mail.replace("@qq.com","");
                            json.put("avatar", "https://q1.qlogo.cn/g?b=qq&nk="+qq+"&s=640");
                        }else{
                            json.put("avatar", baseFull.getAvatar(apiconfig.get("webinfoAvatar").toString(), mail));
                        }
                        //json.put("avatar", baseFull.getAvatar(apiconfig.get("webinfoAvatar").toString(), json.get("mail").toString()));

                    } else {
                        json.put("avatar", apiconfig.get("webinfoAvatar").toString() + "null");
                    }
                }
                String curGroup = info.getGroupKey();
                if (!group.equals("administrator")&&isVip.equals(0)) {
                    json.remove("mail");
                    json.remove("phone");
                }
                //再次判断，如果用户是管理员，只有管理员能看管理员
                if(curGroup.equals("administrator")){
                    if (!group.equals("administrator")){
                        json.remove("mail");
                        json.remove("phone");
                    }
                }
                if (uStatus != 0) {
                    TypechoFan fan = new TypechoFan();
                    fan.setUid(logUid);
                    fan.setTouid(uid);
                    Integer isFollow = fanService.total(fan);
                    json.put("isFollow",isFollow);
                }else{
                    json.put("isFollow",0);
                }
                //获取被拉黑和拉黑情况
                Integer isBan = 0;
                Integer isBanlogid = 0;
                if (uStatus != 0) {
                    TypechoUserlog userlog = new TypechoUserlog();
                    userlog.setType("banUser");
                    userlog.setUid(Integer.parseInt(key));
                    userlog.setNum(logUid);
                    List<TypechoUserlog> ban = userlogService.selectList(userlog);
                    if(ban.size()>0){
                        //对方把我拉黑
                        isBan = 1;
                        isBanlogid = ban.get(0).getId();
                    }
                    userlog.setUid(logUid);
                    userlog.setNum(Integer.parseInt(key));
                    ban = userlogService.selectList(userlog);
                    if(ban.size()>0){
                        //我把对方拉黑
                        isBan = 2;
                        isBanlogid = ban.get(0).getId();
                    }
                }
                json.put("isBan",isBan);
                json.put("isBanlogid",isBanlogid);
                redisHelp.delete(this.dataprefix+"_"+"userInfo_"+key,redisTemplate);
                redisHelp.setKey(this.dataprefix+"_"+"userInfo_"+key,json,this.userCache,redisTemplate);

            }
            JSONObject response = new JSONObject();
            response.put("code", 1);
            response.put("msg", "");
            response.put("data", json);
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
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
    @LoginRequired(purview = "-2")
    public String userLogin(@RequestParam(value = "params", required = false) String params,HttpServletRequest request) {
        Map jsonToMap = null;
        String oldpw = null;
        try {
            //未登录情况下，撞库类攻击拦截
            Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
            String  ip = baseFull.getIpAddr(request);
            if(apiconfig.get("banRobots").toString().equals("1")) {

                String isSilence = redisHelp.getRedis(ip+"_silence",redisTemplate);
                if(isSilence!=null){
                    return Result.getResultJson(0,"你已被禁止请求，请耐心等待",null);
                }
                String isRepeated = redisHelp.getRedis(ip+"_isOperation",redisTemplate);
                if(isRepeated==null){
                    redisHelp.setRedis(ip+"_isOperation","1",2,redisTemplate);
                }else{
                    Integer frequency = Integer.parseInt(isRepeated) + 1;
                    if(frequency==4){
                        securityService.safetyMessage("IP："+ip+"，在登录接口疑似存在攻击行为，请及时确认处理。","system");
                        redisHelp.setRedis(ip+"_silence","1",Integer.parseInt(apiconfig.get("silenceTime").toString()),redisTemplate);
                        return Result.getResultJson(0,"你的请求存在恶意行为，10分钟内禁止操作！",null);
                    }
                    redisHelp.setRedis(ip+"_isOperation",frequency.toString(),2,redisTemplate);
                    return Result.getResultJson(0,"你的操作太频繁了",null);
                }
            }
            //攻击拦截结束
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
            String name = jsonToMap.get("name").toString();

            TypechoUsers Users = new TypechoUsers();
            //支持邮箱登录
            if (!baseFull.isEmail(name)) {
                Users.setName(name);
            }else{
                Users.setMail(name);
            }
            List<TypechoUsers> rows = service.selectList(Users);
            if (rows.size() < 0) {
                if (name.matches("[1-9]\\d*")) {
                    //如果是整数，尝试手机号登录
                    TypechoUsers phoneUser = new TypechoUsers();
                    phoneUser.setPhone(name);
                    rows = service.selectList(phoneUser);
                }
            }
            if (rows.size() > 0) {
                //判断用户是否被封禁
                Integer bantime = rows.get(0).getBantime();
                if(bantime.equals(1)){
                    return Result.getResultJson(0, "你的账号已被永久封禁，如有疑问请联系管理员", null);
                }else{
                    Long date = System.currentTimeMillis();
                    Integer curtime = Integer.parseInt(String.valueOf(date).substring(0,10));
                    if(bantime > curtime){
                        return Result.getResultJson(0, "你的账号被暂时封禁，请耐心等待解封。", null);
                    }
                }
                //查询出用户信息后，通过接口验证用户密码
                String newpw = rows.get(0).getPassword();
                //通过内置验证
                boolean isPass = phpass.CheckPassword(oldpw, newpw);

                if (!isPass) {
                    return Result.getResultJson(0, "用户密码错误", null);
                }
                //内置验证结束
                Long date = System.currentTimeMillis();
                String randStr = baseFull.createRandomStr(6);
                String Token = date + jsonToMap.get("name").toString()+randStr;
                jsonToMap.put("uid", rows.get(0).getUid());
                //生成唯一性token用于验证
                jsonToMap.put("token", jsonToMap.get("name").toString() + DigestUtils.md5DigestAsHex(Token.getBytes()));
                jsonToMap.put("time", date);
                jsonToMap.put("group", rows.get(0).getGroupKey());
                jsonToMap.put("url", rows.get(0).getUrl());
                jsonToMap.put("screenName", rows.get(0).getScreenName());
                jsonToMap.put("customize", rows.get(0).getCustomize());
                jsonToMap.put("introduce", rows.get(0).getIntroduce());
                jsonToMap.put("experience", rows.get(0).getExperience());
                //判断是否为VIP
                jsonToMap.put("vip", rows.get(0).getVip());
                jsonToMap.put("isvip", 0);
                String curTime = String.valueOf(date).substring(0, 10);
                Integer viptime  = rows.get(0).getVip();
                if(viptime>Integer.parseInt(curTime)||viptime.equals(1)){
                    jsonToMap.put("isvip", 1);
                }
                //获取用户等级
  //              Integer uid = rows.get(0).getUid();
//                TypechoComments comments = new TypechoComments();
//                comments.setAuthorId(uid);
//                Integer lv = commentsService.total(comments,null);
//                jsonToMap.put("lv", baseFull.getLv(lv));

                if(rows.get(0).getAvatar()!=null){
                    jsonToMap.put("avatar",rows.get(0).getAvatar());
                }else{
                    if (rows.get(0).getMail() != null) {
                        if(rows.get(0).getMail().indexOf("@qq.com") != -1){
                            String qq = rows.get(0).getMail().replace("@qq.com","");
                            jsonToMap.put("avatar", "https://q1.qlogo.cn/g?b=qq&nk="+qq+"&s=640");
                        }else{
                            jsonToMap.put("avatar", baseFull.getAvatar(apiconfig.get("webinfoAvatar").toString(), rows.get(0).getMail()));
                        }
                    } else {
                        jsonToMap.put("avatar", apiconfig.get("webinfoAvatar").toString() + "null");
                    }
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
                updateuser.setIp(ip);
                updateuser.setLocal(baseFull.getLocal(ip));
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
            e.printStackTrace();
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
    @LoginRequired(purview = "-2")
    public String apiLogin(@RequestParam(value = "params", required = false) String params,HttpServletRequest request) {


        Map jsonToMap = null;
        String oldpw = null;
        try {
            if (StringUtils.isNotBlank(params)) {
                jsonToMap = JSONObject.parseObject(JSON.parseObject(params).toString());
            } else {
                return Result.getResultJson(0, "请输入正确的参数", null);
            }
            String  ip = baseFull.getIpAddr(request);
            Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
            Integer isInvite = Integer.parseInt(apiconfig.get("isInvite").toString());
            //如果是微信，则走两步判断，是小程序还是APP
            if(jsonToMap.get("appLoginType").toString().equals("weixin")){

                //走官方接口获取accessToken和openid
                if (jsonToMap.get("js_code") == null) {
                    return Result.getResultJson(0, "APP配置异常，js_code参数不存在", null);
                }
                String js_code = jsonToMap.get("js_code").toString();
                if(jsonToMap.get("type").toString().equals("applets")){
                    String requestUrl = "https://api.weixin.qq.com/sns/jscode2session?appid="+apiconfig.get("appletsAppid").toString()+"&secret="+apiconfig.get("appletsSecret").toString()+"&js_code="+js_code+"&grant_type=authorization_code";
                    String res = HttpClient.doGet(requestUrl);
                    if(res==null){
                        return Result.getResultJson(0, "接口配置异常，微信官方接口请求失败", null);
                    }
                    System.out.println("微信登录小程序接口返回"+res);
                    HashMap data = JSON.parseObject(res, HashMap.class);
                    if(data.get("openid")==null){
                        return Result.getResultJson(0, "接口配置异常，小程序openid获取失败", null);
                    }
                    jsonToMap.put("accessToken",data.get("openid"));
                    jsonToMap.put("openId",data.get("openid"));
                }else{
                    String requestUrl = "https://api.weixin.qq.com/sns/oauth2/access_token?appid="+apiconfig.get("wxAppId").toString()+"&secret="+apiconfig.get("wxAppSecret").toString()+"&code="+js_code+"&grant_type=authorization_code";
                    String res = HttpClient.doGet(requestUrl);
                    System.out.println(res);
                    if(res==null){
                        return Result.getResultJson(0, "接口配置异常，微信官方接口请求失败", null);
                    }

                    HashMap data = JSON.parseObject(res, HashMap.class);
                    if(data.get("openid")==null){
                        return Result.getResultJson(0, "接口配置异常，openid获取失败", null);
                    }
                    jsonToMap.put("accessToken",data.get("openid"));
                    jsonToMap.put("openId",data.get("openid"));
                }


            }

            //QQ也要走两步判断
            if(jsonToMap.get("appLoginType").toString().equals("qq")){


                if(jsonToMap.get("type").toString().equals("applets")){
                    if (jsonToMap.get("js_code") == null) {
                        return Result.getResultJson(0, "APP配置异常，js_code参数不存在", null);
                    }
                    String js_code = jsonToMap.get("js_code").toString();
                    //如果是小程序，走官方接口获取accessToken和openid
                    String requestUrl = "https://api.q.qq.com/sns/jscode2session?appid="+apiconfig.get("qqAppletsAppid").toString()+"&secret="+apiconfig.get("qqAppletsSecret").toString()+"&js_code="+js_code+"&grant_type=authorization_code";
                    String res = HttpClient.doGet(requestUrl);
                    System.out.println("QQ接口返回"+res);
                    if(res==null){
                        return Result.getResultJson(0, "接口配置异常，QQ官方接口请求失败", null);
                    }

                    HashMap data = JSON.parseObject(res, HashMap.class);
                    if(data.get("openid")==null){
                        return Result.getResultJson(0, "接口配置异常，openid获取失败", null);
                    }
                    jsonToMap.put("accessToken",data.get("openid"));
                    jsonToMap.put("openId",data.get("openid"));
                }else {
                    if (jsonToMap.get("accessToken") == null) {
                        return Result.getResultJson(0, "登录配置异常，accessToken参数不存在", null);
                    }
                    jsonToMap.put("accessToken",jsonToMap.get("openId"));
                    jsonToMap.put("openId",jsonToMap.get("openId"));
                }
            }else{
                if (jsonToMap.get("accessToken") == null) {
                    return Result.getResultJson(0, "登录配置异常，accessToken参数不存在", null);
                }
            }
            if(jsonToMap.get("appLoginType").toString().equals("apple")){
                System.out.println("Apple登录全部参数："+params);
                if (jsonToMap.get("accessToken") == null) {
                    return Result.getResultJson(0, "登录配置异常，accessToken参数不存在", null);
                }


                String identityToken = jsonToMap.get("accessToken").toString();
                String appleSub = AppleLoginUtil.verifyAndGetSub(identityToken);
                if(appleSub == null){
                    return Result.getResultJson(0, "Apple登录验证失败，请重试", null);
                }
                // 把 sub 当成 openId 绑定
                jsonToMap.put("openId", appleSub);
                jsonToMap.put("accessToken", appleSub);
            }
            TypechoUserapi userapi = JSON.parseObject(JSON.toJSONString(jsonToMap), TypechoUserapi.class);
            String openid = userapi.getOpenId();
            String loginType = userapi.getAppLoginType();
            TypechoUserapi isApi = new TypechoUserapi();
            isApi.setOpenId(openid);
            isApi.setAppLoginType(loginType);
            List<TypechoUserapi> apiList = userapiService.selectList(isApi);
            //大于0则走向登陆，小于0则进行注册
            if (apiList.size() > 0) {

                TypechoUserapi apiInfo = apiList.get(0);
                TypechoUsers user = service.selectByKey(apiInfo.getUid());
                if(user==null){
                    return Result.getResultJson(0, "用户不存在", null);
                }
                //判断用户是否被封禁
                Integer bantime = user.getBantime();
                if(bantime.equals(1)){
                    return Result.getResultJson(0, "你的账号已被永久封禁，如有疑问请联系管理员", null);
                }else{
                    Long date = System.currentTimeMillis();
                    Integer curtime = Integer.parseInt(String.valueOf(date).substring(0,10));
                    if(bantime > curtime){
                        return Result.getResultJson(0, "你的账号被暂时封禁，请耐心等待解封。", null);
                    }
                }
                Long date = System.currentTimeMillis();
                String randStr = baseFull.createRandomStr(6);
                String Token = date + user.getName()+randStr;
                jsonToMap.put("uid", user.getUid());

                //生成唯一性token用于验证
                jsonToMap.put("name", user.getName());
                jsonToMap.put("token", user.getName() + DigestUtils.md5DigestAsHex(Token.getBytes()));
                jsonToMap.put("time", date);
                jsonToMap.put("group", user.getGroupKey());
                jsonToMap.put("url", user.getUrl());
                jsonToMap.put("screenName", user.getScreenName());
                jsonToMap.put("customize", user.getCustomize());
                jsonToMap.put("introduce", user.getIntroduce());
                jsonToMap.put("experience", user.getExperience());
                //判断是否为VIP
                jsonToMap.put("vip", user.getVip());
                jsonToMap.put("isvip", 0);
                String curTime = String.valueOf(date).substring(0, 10);
                Integer viptime  = user.getVip();
                if(viptime>Integer.parseInt(curTime)||viptime.equals(1)){
                    jsonToMap.put("isvip", 1);
                }
                if(user.getAvatar()!=null){
                    jsonToMap.put("avatar", user.getAvatar());
                }else{
                    if (user.getMail() != null) {
                        if(user.getMail().indexOf("@qq.com") != -1){
                            String qq = user.getMail().replace("@qq.com","");
                            jsonToMap.put("avatar", "https://q1.qlogo.cn/g?b=qq&nk="+qq+"&s=640");
                        }else{
                            jsonToMap.put("avatar", baseFull.getAvatar(apiconfig.get("webinfoAvatar").toString(), user.getMail()));
                        }
                    } else {
                        jsonToMap.put("avatar", apiconfig.get("webinfoAvatar").toString() + "null");
                    }
                }

                //获取用户等级
//                Integer uid = user.getUid();
//                TypechoComments comments = new TypechoComments();
//                comments.setAuthorId(uid);
//                Integer lv = commentsService.total(comments,null);
//                jsonToMap.put("lv", baseFull.getLv(lv));
                //更新用户登录时间和第一次登陆时间（满足typecho要求）
                String userTime = String.valueOf(date).substring(0, 10);
                TypechoUsers updateuser = new TypechoUsers();
                updateuser.setUid(user.getUid());
                updateuser.setLogged(Integer.parseInt(userTime));
                updateuser.setIp(ip);
                updateuser.setLocal(baseFull.getLocal(ip));
                if (user.getLogged() == 0) {
                    updateuser.setActivated(Integer.parseInt(userTime));
                }

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

//                if (jsonToMap.get("headImgUrl") != null) {
//
//                }
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
                regUser.setIp(ip);
                regUser.setLocal(baseFull.getLocal(ip));
                String headImgUrl = apiconfig.get("webinfoAvatar").toString() + "null";
                if (jsonToMap.get("headImgUrl") != null) {
                    headImgUrl = jsonToMap.get("headImgUrl").toString();
                    //QQ的接口头像要处理(垃圾腾讯突然修改了返回格式)
                    if(jsonToMap.get("appLoginType").toString().equals("qq")){
                        headImgUrl = headImgUrl.replace("http://","https://");
                        headImgUrl = headImgUrl.replace("&amp;","&");
                    }
                    regUser.setAvatar(headImgUrl);
                }
                service.insert(regUser);
                //注册完成后，增加绑定
                Integer uid = regUser.getUid();
                userapi.setUid(uid);
                int rows = userapiService.insert(userapi);
                //返回token
                String randStr = baseFull.createRandomStr(6);
                Long regdate = System.currentTimeMillis();
                String Token = regdate + name + randStr;
                jsonToMap.put("uid", uid);
                //生成唯一性token用于验证
                jsonToMap.put("name", name);
                jsonToMap.put("token", name + DigestUtils.md5DigestAsHex(Token.getBytes()));
                jsonToMap.put("time", regdate);
                jsonToMap.put("group", "contributor");
                jsonToMap.put("groupKey", "contributor");
                jsonToMap.put("url", "");
                jsonToMap.put("screenName", userapi.getNickName());
                jsonToMap.put("avatar", headImgUrl);
//                jsonToMap.put("lv", 0);
                jsonToMap.put("customize", "");
                jsonToMap.put("experience", 0);
                //VIP
                jsonToMap.put("vip", 0);
                jsonToMap.put("isvip", 0);
                jsonToMap.put("noPassWord", 1);

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
            e.printStackTrace();
            JSONObject response = new JSONObject();

            response.put("code", 0);
            response.put("msg", "登陆失败，请联系管理员");
            response.put("data", null);

            return response.toString();
        }

    }
    /***
     * 快捷登录
     */
    @RequestMapping(value = "/phoneLogin")
    @ResponseBody
    @LoginRequired(purview = "-2")
    public String phoneLogin(@RequestParam(value = "phone", required = false) String phone,
                             @RequestParam(value = "code", required = false) String code,
                            HttpServletRequest request) {
        Map jsonToMap = new HashMap();
        try{
            if(phone==null||code==null){
                return Result.getResultJson(0, "参数错误", null);
            }
            String sendCode = "";
            if (redisHelp.getRedis(this.dataprefix + "_" + "sendSMS" + phone, redisTemplate) != null) {
                sendCode = redisHelp.getRedis(this.dataprefix + "_" + "sendSMS" + phone, redisTemplate);
                if (!sendCode.equals(code)) {
                    return Result.getResultJson(0, "验证码不正确", null);
                }
            } else {
                return Result.getResultJson(0, "验证码不正确或已失效", null);
            }
            //未登录情况下，撞库类攻击拦截
            Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
            String ip = baseFull.getIpAddr(request);
            if(apiconfig.get("banRobots").toString().equals("1")) {

                String isSilence = redisHelp.getRedis(ip + "_silence", redisTemplate);
                if (isSilence != null) {
                    return Result.getResultJson(0, "你已被禁止请求，请耐心等待", null);
                }
                String isRepeated = redisHelp.getRedis(ip + "_isOperation", redisTemplate);
                if (isRepeated == null) {
                    redisHelp.setRedis(ip + "_isOperation", "1", 2, redisTemplate);
                } else {
                    Integer frequency = Integer.parseInt(isRepeated) + 1;
                    if (frequency == 4) {
                        securityService.safetyMessage("IP：" + ip + "，在登录接口疑似存在攻击行为，请及时确认处理。", "system");
                        redisHelp.setRedis(ip + "_silence", "1", Integer.parseInt(apiconfig.get("silenceTime").toString()), redisTemplate);
                        return Result.getResultJson(0, "你的请求存在恶意行为，10分钟内禁止操作！", null);
                    }
                    redisHelp.setRedis(ip + "_isOperation", frequency.toString(), 2, redisTemplate);
                    return Result.getResultJson(0, "你的操作太频繁了", null);
                }
            }
            //攻击拦截结束
            Integer isInvite = Integer.parseInt(apiconfig.get("isInvite").toString());

            //判断是否存在用户，存在就登录，不存在就注册
            TypechoUsers users = new TypechoUsers();
            users.setPhone(phone);
            List<TypechoUsers> list = service.selectList(users);
            if(list.size() > 0){

                TypechoUsers user = list.get(0);
                //判断用户是否被封禁
                Integer bantime = user.getBantime();
                if(bantime.equals(1)){
                    return Result.getResultJson(0, "你的账号已被永久封禁，如有疑问请联系管理员", null);
                }else{
                    Long date = System.currentTimeMillis();
                    Integer curtime = Integer.parseInt(String.valueOf(date).substring(0,10));
                    if(bantime > curtime){
                        return Result.getResultJson(0, "你的账号被暂时封禁，请耐心等待解封。", null);
                    }
                }
                Long date = System.currentTimeMillis();
                String randStr = baseFull.createRandomStr(6);
                String Token = date + user.getName()+randStr;
                jsonToMap.put("uid", user.getUid());

                //生成唯一性token用于验证

                jsonToMap.put("name", user.getName());
                jsonToMap.put("token", user.getName() + DigestUtils.md5DigestAsHex(Token.getBytes()));
                jsonToMap.put("time", date);
                jsonToMap.put("group", user.getGroupKey());
                jsonToMap.put("url", user.getUrl());
                jsonToMap.put("screenName", user.getScreenName());
                jsonToMap.put("customize", user.getCustomize());
                jsonToMap.put("introduce", user.getIntroduce());
                jsonToMap.put("experience", user.getExperience());
                //判断是否为VIP
                jsonToMap.put("vip", user.getVip());
                jsonToMap.put("isvip", 0);
                String curTime = String.valueOf(date).substring(0, 10);
                Integer viptime  = user.getVip();
                if(viptime>Integer.parseInt(curTime)||viptime.equals(1)){
                    jsonToMap.put("isvip", 1);
                }
                if(user.getAvatar()!=null){
                    jsonToMap.put("avatar", user.getAvatar());
                }else{
                    if (user.getMail() != null) {
                        if(user.getMail().indexOf("@qq.com") != -1){
                            String qq = user.getMail().replace("@qq.com","");
                            jsonToMap.put("avatar", "https://q1.qlogo.cn/g?b=qq&nk="+qq+"&s=640");
                        }else{
                            jsonToMap.put("avatar", baseFull.getAvatar(apiconfig.get("webinfoAvatar").toString(), user.getMail()));
                        }
                    } else {
                        jsonToMap.put("avatar", apiconfig.get("webinfoAvatar").toString() + "null");
                    }
                }

                //获取用户等级
//                Integer uid = user.getUid();
//                TypechoComments comments = new TypechoComments();
//                comments.setAuthorId(uid);
//                Integer lv = commentsService.total(comments,null);
//                jsonToMap.put("lv", baseFull.getLv(lv));
                //更新用户登录时间和第一次登陆时间（满足typecho要求）
                String userTime = String.valueOf(date).substring(0, 10);
                TypechoUsers updateuser = new TypechoUsers();
                updateuser.setUid(user.getUid());
                updateuser.setLogged(Integer.parseInt(userTime));
                updateuser.setIp(ip);
                updateuser.setLocal(baseFull.getLocal(ip));
                if (user.getLogged() == 0) {
                    updateuser.setActivated(Integer.parseInt(userTime));
                }

                Integer rows = service.update(updateuser);

                //删除之前的token后，存入redis(防止积累导致内存溢出，超时时间默认是24小时)
                String oldToken = redisHelp.getRedis(this.dataprefix + "_" + "userkey" + jsonToMap.get("name").toString(), redisTemplate);
                if (oldToken != null) {
                    redisHelp.delete(this.dataprefix + "_" + "userInfo" + oldToken, redisTemplate);
                }
                redisHelp.setRedis(this.dataprefix + "_" + "userkey" + jsonToMap.get("name").toString(), jsonToMap.get("token").toString(), this.usertime, redisTemplate);
                redisHelp.setKey(this.dataprefix + "_" + "userInfo" + jsonToMap.get("name").toString() + DigestUtils.md5DigestAsHex(Token.getBytes()), jsonToMap, this.usertime, redisTemplate);

                return Result.getResultJson(rows > 0 ? 1 : 0, rows > 0 ? "登录成功" : "登陆失败", jsonToMap);
            }else{
                //验证邀请码
                if(isInvite.equals(1)){
                    return Result.getResultJson(0, "当前注册需要邀请码，请采用其它方式注册", null);

                }
                TypechoUsers regUser = new TypechoUsers();
                String p = baseFull.createRandomStr(9);
                String passwd = phpass.HashPassword(p);
                Long date = System.currentTimeMillis();
                String userTime = String.valueOf(date).substring(0, 10);
                String name = baseFull.createRandomStr(5) + baseFull.createRandomStr(4);
                regUser.setName(name);
                regUser.setCreated(Integer.parseInt(userTime));
                regUser.setGroupKey("subscriber");
                regUser.setScreenName(null);
                regUser.setPassword(passwd.replaceAll("(\\\r\\\n|\\\r|\\\n|\\\n\\\r)", ""));
                regUser.setIp(ip);
                regUser.setLocal(baseFull.getLocal(ip));
                regUser.setPhone(phone);
                int rows = service.insert(regUser);
                //返回token
                Long regdate = System.currentTimeMillis();
                String Token = regdate + name;
                jsonToMap.put("uid", regUser.getUid());
                //生成唯一性token用于验证
                jsonToMap.put("name", name);
                jsonToMap.put("token", name + DigestUtils.md5DigestAsHex(Token.getBytes()));
                jsonToMap.put("time", regdate);
                jsonToMap.put("group", "contributor");
                jsonToMap.put("groupKey", "contributor");
                jsonToMap.put("url", "");
                jsonToMap.put("screenName", "");
                jsonToMap.put("avatar", apiconfig.get("webinfoAvatar").toString() + "null");
//                jsonToMap.put("lv", 0);
                jsonToMap.put("customize", "");
                jsonToMap.put("experience", 0);
                //VIP
                jsonToMap.put("vip", 0);
                jsonToMap.put("isvip", 0);
                //未设置的信息
                jsonToMap.put("noPassWord", 1);
                //删除之前的token后，存入redis(防止积累导致内存溢出，超时时间默认是24小时)
                String oldToken = redisHelp.getRedis(this.dataprefix + "_" + "userkey" + name, redisTemplate);
                if (oldToken != null) {
                    redisHelp.delete(this.dataprefix + "_" + "userInfo" + oldToken, redisTemplate);
                }
                redisHelp.setRedis(this.dataprefix + "_" + "userkey" + jsonToMap.get("name").toString(), jsonToMap.get("token").toString(), this.usertime, redisTemplate);
                redisHelp.setKey(this.dataprefix + "_" + "userInfo" + jsonToMap.get("name").toString() + DigestUtils.md5DigestAsHex(Token.getBytes()), jsonToMap, this.usertime, redisTemplate);

                return Result.getResultJson(rows > 0 ? 1 : 0, rows > 0 ? "登录成功" : "登陆失败", jsonToMap);

            }
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }
    }
    /***
     * 社会化登陆绑定
     * @param params Bean对象JSON字符串
     */
    @RequestMapping(value = "/apiBind")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String apiBind(@RequestParam(value = "params", required = false) String params,
                          @RequestParam(value = "token", required = false) String token) {

        Map jsonToMap = null;
        String oldpw = null;
        try {
            if (StringUtils.isNotBlank(params)) {
                jsonToMap = JSONObject.parseObject(JSON.parseObject(params).toString());
            } else {
                return Result.getResultJson(0, "请输入正确的参数", null);
            }
            Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
            //如果是微信，则走两步判断，是小程序还是APP
            if(jsonToMap.get("appLoginType").toString().equals("weixin")){

                //走官方接口获取accessToken和openid
                if (jsonToMap.get("js_code") == null) {
                    return Result.getResultJson(0, "APP配置异常，js_code参数不存在", null);
                }
                String js_code = jsonToMap.get("js_code").toString();
                if(jsonToMap.get("type").toString().equals("applets")){
                    String requestUrl = "https://api.weixin.qq.com/sns/jscode2session?appid="+apiconfig.get("appletsAppid").toString()+"&secret="+apiconfig.get("appletsSecret").toString()+"&js_code="+js_code+"&grant_type=authorization_code";
                    String res = HttpClient.doGet(requestUrl);
                    if(res==null){
                        return Result.getResultJson(0, "接口配置异常，微信官方接口请求失败", null);
                    }
                    HashMap data = JSON.parseObject(res, HashMap.class);
                    if(data.get("openid")==null){
                        return Result.getResultJson(0, "接口配置异常，小程序openid获取失败，错误码"+data.get("errcode").toString(), null);
                    }
                    jsonToMap.put("accessToken",data.get("openid"));
                    jsonToMap.put("openId",data.get("openid"));
                }else{
                    String requestUrl = "https://api.weixin.qq.com/sns/oauth2/access_token?appid="+apiconfig.get("wxAppId").toString()+"&secret="+apiconfig.get("wxAppSecret").toString()+"&code="+js_code+"&grant_type=authorization_code";
                    String res = HttpClient.doGet(requestUrl);
                    if(res==null){
                        return Result.getResultJson(0, "接口配置异常，微信官方接口请求失败", null);
                    }
                    HashMap data = JSON.parseObject(res, HashMap.class);
                    if(data.get("openid")==null){
                        return Result.getResultJson(0, "接口配置异常，openid获取失败，错误码"+data.get("errcode").toString(), null);
                    }
                    jsonToMap.put("accessToken",data.get("openid"));
                    jsonToMap.put("openId",data.get("openid"));
                }


            }
            //QQ也要走两步判断
            if(jsonToMap.get("appLoginType").toString().equals("qq")){
                if(jsonToMap.get("type").toString().equals("applets")){
                    //如果是小程序，走官方接口获取accessToken和openid
                    if (jsonToMap.get("js_code") == null) {
                        return Result.getResultJson(0, "APP配置异常，请检查相关设置", null);
                    }
                    String js_code = jsonToMap.get("js_code").toString();
                    String requestUrl = "https://api.q.qq.com/sns/jscode2session?appid="+apiconfig.get("qqAppletsAppid").toString()+"&secret="+apiconfig.get("qqAppletsSecret").toString()+"&js_code="+js_code+"&grant_type=authorization_code";
                    String res = HttpClient.doGet(requestUrl);
                    if(res==null){
                        return Result.getResultJson(0, "接口配置异常，请检查相关设置", null);
                    }

                    HashMap data = JSON.parseObject(res, HashMap.class);
                    if(data.get("unionid")==null){
                        return Result.getResultJson(0, "接口配置异常，请检查相关设置", null);
                    }
                    jsonToMap.put("accessToken",data.get("openid"));
                    jsonToMap.put("openId",data.get("openid"));
                }else {
                    if (jsonToMap.get("accessToken") == null) {
                        return Result.getResultJson(0, "登录配置异常，请检查相关设置", null);
                    }
                    jsonToMap.put("accessToken",jsonToMap.get("openId"));
                    jsonToMap.put("openId",jsonToMap.get("openId"));
                }
            }else{
                if (jsonToMap.get("accessToken") == null) {
                    return Result.getResultJson(0, "登录配置异常，请检查相关设置", null);
                }
            }
            if(jsonToMap.get("appLoginType").toString().equals("apple")){
                System.out.println("Apple登录全部参数："+params);
                if (jsonToMap.get("accessToken") == null) {
                    return Result.getResultJson(0, "登录配置异常，accessToken参数不存在", null);
                }
                String identityToken = jsonToMap.get("accessToken").toString();
                String appleSub = AppleLoginUtil.verifyAndGetSub(identityToken);
                if(appleSub == null){
                    return Result.getResultJson(0, "Apple登录验证失败，请重试", null);
                }
                // 把 sub 当成 openId 绑定
                jsonToMap.put("openId", appleSub);
                jsonToMap.put("accessToken", appleSub);
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
                return Result.getResultJson(0, "已经被其他用户绑定了，请直接登录", null);
            }
            isApi.setUid(uid);
            int rows = userapiService.insert(userapi);
            JSONObject response = new JSONObject();
            response.put("code", rows > 0 ? 1 : 0);
            response.put("data", rows);
            response.put("msg", rows > 0 ? "绑定成功" : "绑定失败");
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            JSONObject response = new JSONObject();

            response.put("code", 0);
            response.put("msg", "未知错误，请联系管理员");
            response.put("data", null);

            return response.toString();
        }

    }

    /***
     * 社会化登陆解除绑定
     */
    @RequestMapping(value = "/apiBindDelete")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String apiBindDelete(@RequestParam(value = "appLoginType", required = false) String appLoginType,
                          @RequestParam(value = "token", required = false) String token) {
        try {
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            Integer uid = Integer.parseInt(map.get("uid").toString());
            TypechoUserapi isApi = new TypechoUserapi();
            isApi.setAppLoginType(appLoginType);
            isApi.setUid(uid);
            List<TypechoUserapi> apiBind = userapiService.selectList(isApi);
            if (apiBind.size() > 0) {
                //如果已经绑定，删除之前的绑定
                Integer id = apiBind.get(0).getId();
                int rows = userapiService.delete(id);
                JSONObject response = new JSONObject();
                response.put("code", rows > 0 ? 1 : 0);
                response.put("data", rows);
                response.put("msg", rows > 0 ? "操作成功" : "操作失败");
                return response.toString();
            }else{
                return Result.getResultJson(0, "你还未绑定该渠道", null);
            }
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }

    }


    /**
     * 用户绑定查询
     */
    @RequestMapping(value = "/userBindStatus")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String userBindStatus(@RequestParam(value = "token", required = false) String token) {

        JSONObject response = new JSONObject();
        try {
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
            userapi.setAppLoginType("apple");
            Integer appleBind = userapiService.total(userapi);
            Map jsonToMap = new HashMap();

            jsonToMap.put("qqBind", qqBind);
            jsonToMap.put("weixinBind", weixinBind);
            jsonToMap.put("weiboBind", weiboBind);
            jsonToMap.put("appleBind", appleBind);

            response.put("code", 1);
            response.put("data", jsonToMap);
            response.put("msg", "");
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
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
    @LoginRequired(purview = "-2")
    public String userRegister(@RequestParam(value = "params", required = false) String params,HttpServletRequest request) {
        TypechoUsers insert = null;
        Map jsonToMap = null;
        try{
            //未登录情况下，撞库类攻击拦截
            Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
            String  ip = baseFull.getIpAddr(request);
            if(apiconfig.get("banRobots").toString().equals("1")) {

                String isSilence = redisHelp.getRedis(ip+"_silence",redisTemplate);
                if(isSilence!=null){
                    return Result.getResultJson(0,"你已被禁止请求，请耐心等待",null);
                }
                String isRepeated = redisHelp.getRedis(ip+"_isOperation",redisTemplate);
                if(isRepeated==null){
                    redisHelp.setRedis(ip+"_isOperation","1",3,redisTemplate);
                }else{
                    Integer frequency = Integer.parseInt(isRepeated) + 1;
                    if(frequency==3){
                        securityService.safetyMessage("IP："+ip+"，在注册接口疑似存在攻击行为，请及时确认处理。","system");
                        redisHelp.setRedis(ip+"_silence","1",Integer.parseInt(apiconfig.get("silenceTime").toString()),redisTemplate);
                        return Result.getResultJson(0,"你的请求存在恶意行为，10分钟内禁止操作！",null);
                    }
                    redisHelp.setRedis(ip+"_isOperation",frequency.toString(),3,redisTemplate);
                    return Result.getResultJson(0,"你的操作太频繁了",null);
                }
            }

            //攻击拦截结束
            Integer isUserInvite = 0;
            Integer inviteUserID = 0;
            Integer inviteUserAssets = 0;
            if (StringUtils.isNotBlank(params)) {
                jsonToMap = JSONObject.parseObject(JSON.parseObject(params).toString());
                jsonToMap.remove("invitationCode");
                //在之前需要做判断，验证用户名或者邮箱在数据库中是否存在
                //判断是否开启邮箱验证
                Integer isEmail = Integer.parseInt(apiconfig.get("isEmail").toString());
                Integer isInvite = Integer.parseInt(apiconfig.get("isInvite").toString());
                //验证是否存在相同用户名或者邮箱
                TypechoUsers toKey = new TypechoUsers();
                if(isEmail>0) {

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
                if(isEmail>0){
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
                    //优先验证用户邀请码
                    TypechoUsers InvitationUser = new TypechoUsers();
                    InvitationUser.setInvitationCode(jsonToMap.get("inviteCode").toString());
                    List<TypechoUsers> invitationList = service.selectList(InvitationUser);
                    if(invitationList.size()>0){
                        TypechoUsers InvitationData = invitationList.get(0);
                        inviteUserID = InvitationData.getUid();
                        isUserInvite = 1;
//                        inviteUserAssets = InvitationData.getAssets();
                        jsonToMap.put("invitationUser", inviteUserID);

                    }else{
                        //再验证平台邀请码
                        TypechoInvitation invitation = new TypechoInvitation();
                        invitation.setCode(jsonToMap.get("inviteCode").toString());
                        invitation.setStatus(0);
                        List<TypechoInvitation> invite = invitationService.selectList(invitation);
                        if(invite.size()<1){
                            return Result.getResultJson(0, "错误的邀请码", null);
                        }else{
                            TypechoInvitation cur = invite.get(0);
                            cur.setStatus(1);
                            invitationService.update(cur);
                        }
                    }
                }
                //验证用户名是否违禁
                String userName = jsonToMap.get("name").toString();
                String forbidden = "";
                if(apiconfig.get("forbidden")!=null){
                    forbidden = apiconfig.get("forbidden").toString();
                }
                Integer isForbidden = baseFull.getForbidden(forbidden,userName);
                if(isForbidden.equals(1)){
                    return Result.getResultJson(0, "用户名包含违规词语", null);
                }


                String p = jsonToMap.get("password").toString();
                String passwd = phpass.HashPassword(p);
                Long date = System.currentTimeMillis();
                String userTime = String.valueOf(date).substring(0, 10);
                jsonToMap.put("created", userTime);
                jsonToMap.put("group", "contributor");
                jsonToMap.put("groupKey", "contributor");
                jsonToMap.put("screenName", userName);
                jsonToMap.put("password", passwd);
                //jsonToMap.remove("introduce");
                jsonToMap.remove("assets");
                jsonToMap.remove("customize");
                jsonToMap.remove("vip");
                jsonToMap.remove("posttime");
                jsonToMap.remove("points");
                jsonToMap.remove("experience");
                jsonToMap.remove("honor");
            }else{
                return Result.getResultJson(0, "参数错误", null);
            }


            //判断是否传入邀请码
            if(jsonToMap.get("inviteCode")!=null){
                //优先验证用户邀请码是否对应用户
                TypechoUsers InvitationUser = new TypechoUsers();
                InvitationUser.setInvitationCode(jsonToMap.get("inviteCode").toString());
                List<TypechoUsers> invitationList = service.selectList(InvitationUser);
                if(invitationList.size()>0){

                    TypechoUsers InvitationData = invitationList.get(0);
                    inviteUserID = InvitationData.getUid();
                    jsonToMap.put("invitationUser", inviteUserID);
                    inviteUserAssets = InvitationData.getAssets();
                    //判断有没有开启邀请返利
                    Integer rebateLevel = Integer.parseInt(apiconfig.get("rebateLevel").toString());
                    if(rebateLevel.equals(1)||rebateLevel.equals(3)){
                        //获取固定奖励数量
                        Integer rebateNum = Integer.parseInt(apiconfig.get("rebateNum").toString());
                        inviteUserAssets = inviteUserAssets + rebateNum;
                        TypechoUsers updateUser = new TypechoUsers();
                        updateUser.setUid(inviteUserID);
                        updateUser.setAssets(inviteUserAssets);
                        service.update(updateUser);

                        //生成财务日志
                        Long date = System.currentTimeMillis();
                        String created = String.valueOf(date).substring(0,10);
                        TypechoPaylog paylog = new TypechoPaylog();
                        paylog.setStatus(1);
                        paylog.setCreated(Integer.parseInt(created));
                        paylog.setUid(inviteUserID);
                        paylog.setOutTradeNo(created+"rebate");
                        paylog.setTotalAmount(""+rebateNum);
                        paylog.setPaytype("rebate");
                        paylog.setSubject("邀请注册固定奖励");
                        paylogService.insert(paylog);
                    }

                }

            }
            insert = JSON.parseObject(JSON.toJSONString(jsonToMap), TypechoUsers.class);
            int rows = service.insert(insert);

            JSONObject response = new JSONObject();
            response.put("code", rows > 0 ? 1 : 0);
            response.put("data", rows);
            response.put("msg", rows > 0 ? "注册成功" : "注册失败");
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0, "接口请求异常，请联系管理员", null);
        }


    }

    /**
     * 登陆后操作的邮箱验证
     */
    @RequestMapping(value = "/SendCode")
    @ResponseBody
    public String SendCode(@RequestParam(value = "params", required = false) String params,
                           @RequestParam(value = "verifyCode", required = false) String verifyCode,
                           HttpServletRequest request) throws MessagingException {
        try{
            Map jsonToMap = null;


            String  agent =  request.getHeader("User-Agent");
            String  ip = baseFull.getIpAddr(request);
            //刷邮件攻击拦截
            Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
            if(Integer.parseInt(apiconfig.get("verifyLevel").toString())>1) {
                if (StringUtils.isEmpty(verifyCode)) {
                    return Result.getResultJson(0,"图片验证码不能为空",null);
                }
                String kaptchaCode = redisHelp.getRedis(this.dataprefix+"_"+ip+"_verifyCode",redisTemplate);
                if (StringUtils.isEmpty(kaptchaCode) || !verifyCode.equals(kaptchaCode)) {
                    return Result.getResultJson(0,"图片验证码错误",null);
                }
            }

            if(apiconfig.get("banRobots").toString().equals("1")) {
                String isSilence = redisHelp.getRedis(ip+"_silence",redisTemplate);
                if(isSilence!=null){
                    return Result.getResultJson(0,"你已被暂时禁止请求，请耐心等待",null);
                }
                String isRepeated = redisHelp.getRedis(ip+"_isOperation",redisTemplate);
                if(isRepeated==null){
                    redisHelp.setRedis(ip+"_isOperation","1",2,redisTemplate);
                }else{
                    Integer frequency = Integer.parseInt(isRepeated) + 1;
                    if(frequency==3){
                        securityService.safetyMessage("IP："+ip+"，在邮箱发信疑似存在攻击行为，请及时确认处理。","system");
                        redisHelp.setRedis(ip+"_silence","1",Integer.parseInt(apiconfig.get("silenceTime").toString()),redisTemplate);
                        return Result.getResultJson(0,"你的请求存在恶意行为，30分钟内禁止操作！",null);
                    }
                    redisHelp.setRedis(ip+"_isOperation",frequency.toString(),3,redisTemplate);
                    return Result.getResultJson(0,"你的操作太频繁了",null);
                }
            }

            //攻击拦截结束

            //邮件每天最多发送五次
            String sendCode = redisHelp.getRedis(this.dataprefix+"_"+ip+"_sendCode",redisTemplate);
            if(sendCode==null){
                redisHelp.setRedis(this.dataprefix+"_"+ip+"_sendCode","1",86400,redisTemplate);
            }else{
                Integer send_Code = Integer.parseInt(sendCode) + 1;
                if(send_Code > 5){
                    return Result.getResultJson(0,"你已超过最大发送限制，请您24小时后再操作",null);
                }else{
                    redisHelp.setRedis(this.dataprefix+"_"+ip+"_sendCode",send_Code.toString(),86400,redisTemplate);
                }
            }
            //限制结束

            //邮件59秒只能发送一次
            String iSsendCode = redisHelp.getRedis(this.dataprefix + "_" + "iSsendCode_"+agent+"_"+ip, redisTemplate);
            if(iSsendCode==null){
                redisHelp.setRedis(this.dataprefix + "_" + "iSsendCode_"+agent+"_"+ip, "data", 59, redisTemplate);
            }else{
                return Result.getResultJson(0, "你的操作太频繁了", null);
            }
            Integer isEmail = Integer.parseInt(apiconfig.get("isEmail").toString());
            if(isEmail.equals(0)){
                return Result.getResultJson(0, "邮箱验证已经关闭", null);
            }

            if (StringUtils.isNotBlank(params)) {
                jsonToMap = JSONObject.parseObject(JSON.parseObject(params).toString());
                if(jsonToMap.get("name")==null){
                    return Result.getResultJson(0, "参数不正确", null);
                }
                TypechoUsers curUser = new TypechoUsers();
                //判断用户是否提交了邮箱，并验证是否注册
                String name = jsonToMap.get("name").toString();
                String EMAIL_REGEX = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
                Pattern pattern = Pattern.compile(EMAIL_REGEX);
                if (!pattern.matcher(name).matches()) {
                    curUser.setName(name);
                }else{
                    curUser.setMail(name);
                }
                List<TypechoUsers> isName = service.selectList(curUser);
                if (isName.size() > 0) {
                    //生成六位随机验证码
                    Random random = new Random();
                    String code = "";
                    for (int i = 0; i < 6; i++) {
                        code += random.nextInt(10);
                    }
                    //存入redis并发送邮件
                    String userName = isName.get(0).getName();
                    String email = isName.get(0).getMail();
                    redisHelp.delete(this.dataprefix + "_" + "sendCode" + userName, redisTemplate);
                    redisHelp.setRedis(this.dataprefix + "_" + "sendCode" + userName, code, 1800, redisTemplate);
                    TypechoEmailtemplate emailtemplate = emailtemplateService.selectByKey(1);
                    if(emailtemplate==null){
                        return Result.getResultJson(1, "邮件模板不存在", null);
                    }

                    MailService.send("你本次的验证码为" + code,emailText.getVerifyEmail(emailtemplate,name,code),
                            new String[]{email}, new String[]{},apiconfig);
                    return Result.getResultJson(1, "邮件发送成功", null);
                } else {
                    return Result.getResultJson(0, "该用户不存在", null);
                }

            } else {
                return Result.getResultJson(0, "参数错误", null);
            }
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0, "不正确的邮箱发信配置", null);
        }



    }

    /**
     * 注册邮箱验证
     */
    @RequestMapping(value = "/RegSendCode")
    @ResponseBody
    public String RegSendCode(@RequestParam(value = "params", required = false) String params,
                              @RequestParam(value = "verifyCode", required = false) String verifyCode,
                              HttpServletRequest request) throws MessagingException {
        try{
            String  ip = baseFull.getIpAddr(request);
            String  agent =  request.getHeader("User-Agent");
            Map jsonToMap = null;
            Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
            Integer isEmail = Integer.parseInt(apiconfig.get("isEmail").toString());
            if(isEmail.equals(0)){
                return Result.getResultJson(0, "邮箱验证已经关闭", null);
            }
            if(Integer.parseInt(apiconfig.get("verifyLevel").toString())>1) {
                if (StringUtils.isEmpty(verifyCode)) {
                    return Result.getResultJson(0,"图片验证码不能为空",null);
                }
                String kaptchaCode = redisHelp.getRedis(this.dataprefix+"_"+ip+"_verifyCode",redisTemplate);
                if (StringUtils.isEmpty(kaptchaCode) || !verifyCode.equals(kaptchaCode)) {
                    return Result.getResultJson(0,"图片验证码错误",null);
                }
            }
            //刷邮件攻击拦截
            if(apiconfig.get("banRobots").toString().equals("1")) {
                String isSilence = redisHelp.getRedis(ip+"_silence",redisTemplate);
                if(isSilence!=null){
                    return Result.getResultJson(0,"你已被暂时禁止请求，请耐心等待",null);
                }
                String isRepeated = redisHelp.getRedis(ip+"_isOperation",redisTemplate);
                if(isRepeated==null){
                    redisHelp.setRedis(ip+"_isOperation","1",2,redisTemplate);
                }else{
                    Integer frequency = Integer.parseInt(isRepeated) + 1;
                    if(frequency==3){
                        securityService.safetyMessage("IP："+ip+"，在邮箱发信疑似存在攻击行为，请及时确认处理。","system");
                        redisHelp.setRedis(ip+"_silence","1",Integer.parseInt(apiconfig.get("silenceTime").toString()),redisTemplate);
                        return Result.getResultJson(0,"你的请求存在恶意行为，30分钟内禁止操作！",null);
                    }
                    redisHelp.setRedis(ip+"_isOperation",frequency.toString(),3,redisTemplate);
                    return Result.getResultJson(0,"你的操作太频繁了",null);
                }
            }

            //攻击拦截结束
            String regISsendCode = redisHelp.getRedis(this.dataprefix + "_" + "regISsendCode_"+agent+"_"+ip, redisTemplate);
            if(regISsendCode==null){
                redisHelp.setRedis(this.dataprefix + "_" + "regISsendCode_"+agent+"_"+ip, "data", 59, redisTemplate);
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
                TypechoEmailtemplate emailtemplate = emailtemplateService.selectByKey(1);
                if(emailtemplate==null){
                    return Result.getResultJson(1, "邮件模板不存在", null);
                }
                MailService.send("你本次的验证码为" + code,
                        emailText.getVerifyEmail(emailtemplate,null,code),
                        new String[]{email}, new String[]{},apiconfig);
                return Result.getResultJson(1, "邮件发送成功", null);
            } else {
                return Result.getResultJson(0, "参数错误", null);
            }
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0, "不正确的邮箱发信配置", null);
        }


    }

    /**
     * 手机验证
     */
    public static Client createClient(String accessKeyId, String accessKeySecret,String endpoint) throws Exception {
        com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config()
                // 必填，您的 AccessKey ID
                .setAccessKeyId(accessKeyId)
                // 必填，您的 AccessKey Secret
                .setAccessKeySecret(accessKeySecret);
        // 访问的域名
        config.endpoint = endpoint;
        return new Client(config);
    }
    @RequestMapping(value = "/sendSMS")
    @ResponseBody
    public String sendSMS(@RequestParam(value = "phone", required = false) String phone,
                          @RequestParam(value = "area", required = false ,defaultValue = "+86") String area,
                          @RequestParam(value = "verifyCode", required = false) String verifyCode,
                          HttpServletRequest request,
                          HttpSession session) throws MessagingException {
        try{
            Map jsonToMap = null;
            String  agent =  request.getHeader("User-Agent");
            String  ip = baseFull.getIpAddr(request);
            Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
            Integer isPhone = Integer.parseInt(apiconfig.get("isPhone").toString());
            if(isPhone.equals(0)){
                return Result.getResultJson(0, "短信服务已关闭", null);
            }
            if(Integer.parseInt(apiconfig.get("verifyLevel").toString())>1) {
                if (StringUtils.isEmpty(verifyCode)) {
                    return Result.getResultJson(0,"图片验证码不能为空",null);
                }
                String kaptchaCode = redisHelp.getRedis(this.dataprefix+"_"+ip+"_verifyCode",redisTemplate);
                if (StringUtils.isEmpty(kaptchaCode) || !verifyCode.equals(kaptchaCode)) {
                    return Result.getResultJson(0,"图片验证码错误",null);
                }
            }

            //刷短信攻击拦截
            if(apiconfig.get("banRobots").toString().equals("1")) {
                String isSilence = redisHelp.getRedis(ip + "_silence", redisTemplate);
                if (isSilence != null) {
                    return Result.getResultJson(0, "你已被暂时禁止请求，请耐心等待", null);
                }
                String isRepeated = redisHelp.getRedis(ip + "_isOperation", redisTemplate);
                if (isRepeated == null) {
                    redisHelp.setRedis(ip + "_isOperation", "1", 2, redisTemplate);
                } else {
                    Integer frequency = Integer.parseInt(isRepeated) + 1;
                    if (frequency == 3) {
                        securityService.safetyMessage("IP：" + ip + "，在短信发信疑似存在攻击行为，请及时确认处理。", "system");
                        redisHelp.setRedis(ip + "_silence", "1", Integer.parseInt(apiconfig.get("silenceTime").toString()), redisTemplate);
                        return Result.getResultJson(0, "你的请求存在恶意行为，30分钟内禁止操作！", null);
                    }
                    redisHelp.setRedis(ip + "_isOperation", frequency.toString(), 3, redisTemplate);
                    return Result.getResultJson(0, "你的操作太频繁了", null);
                }
            }
            //攻击拦截结束
            String regISsendCode = redisHelp.getRedis(this.dataprefix + "_" + "regISsendSMS_"+agent+"_"+ip, redisTemplate);
            if(regISsendCode==null){
                redisHelp.setRedis(this.dataprefix + "_" + "regISsendSMS_"+agent+"_"+ip, "data", 59, redisTemplate);
            }else{
                return Result.getResultJson(0, "你的操作太频繁了", null);
            }
            // 替换成自己的
            Integer status = securityService.sendSMSCode(phone,area);
            if(status==1){
                return Result.getResultJson(1, "发送成功", null);
            }else {
                return Result.getResultJson(0, "短信发送失败，请检查接口配置", null);
            }
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0, "不正确的短信发信配置", null);
        }
    }

    /***
     * 找回密码
     * @param params Bean对象JSON字符串
     */
    @RequestMapping(value = "/userFoget")
    @ResponseBody
    @LoginRequired(purview = "-2")
    public String userFoget(@RequestParam(value = "params", required = false) String params) {
        try {
            TypechoUsers update = null;
            Map jsonToMap = null;
            if (StringUtils.isNotBlank(params)) {
                jsonToMap = JSONObject.parseObject(JSON.parseObject(params).toString());
                Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
                Integer isEmail = Integer.parseInt(apiconfig.get("isEmail").toString());
                if(isEmail.equals(0)){
                    return Result.getResultJson(0, "邮箱验证已经关闭，请联系管理员找回密码", null);
                }
                if(jsonToMap.get("code")==null||jsonToMap.get("name")==null){
                    return Result.getResultJson(0, "参数错误", null);
                }

                String code = jsonToMap.get("code").toString();
                String name = jsonToMap.get("name").toString();
                //先验证并获取用户
                TypechoUsers curUser = new TypechoUsers();
                //判断用户是否提交了邮箱，并验证是否注册
                String EMAIL_REGEX = "^[\\w.-]+@([\\w-]+\\.)+[\\w-]{2,}$";
                Pattern pattern = Pattern.compile(EMAIL_REGEX);
                if (!pattern.matcher(name).matches()) {
                    curUser.setName(name);
                }else{
                    curUser.setMail(name);
                }
                List<TypechoUsers> isName = service.selectList(curUser);
                if (isName.size() > 0) {
                    String userName = isName.get(0).getName();
                    //从redis获取验证码
                    String sendCode = null;
                    if (redisHelp.getRedis(this.dataprefix + "_" + "sendCode" + userName, redisTemplate) != null) {
                        sendCode = redisHelp.getRedis(this.dataprefix + "_" + "sendCode" + userName, redisTemplate);
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

                    Map updateMap = new HashMap<String, String>();
                    updateMap.put("uid", isName.get(0).getUid().toString());
                    updateMap.put("name", jsonToMap.get("name").toString());
                    updateMap.put("password", jsonToMap.get("password").toString());

                    update = JSON.parseObject(JSON.toJSONString(updateMap), TypechoUsers.class);
                }else{
                    return Result.getResultJson(0, "用户不存在", null);
                }

            }else{
                return Result.getResultJson(0, "参数错误", null);
            }
            int rows = service.update(update);
            JSONObject response = new JSONObject();
            response.put("code", rows > 0 ? 1 : 0);
            response.put("data", rows);
            response.put("msg", rows > 0 ? "操作成功" : "操作失败");
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(0, "操作失败", null);
        }

    }
    /***
     * 用户clientId修改，用于推送
     */
    @RequestMapping(value = "/setClientId")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String setClientId(@RequestParam(value = "clientId", required = false) String clientId, @RequestParam(value = "token", required = false) String token) {
        try {
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            String uid = map.get("uid").toString();
            TypechoUsers user = new TypechoUsers();
            user.setUid(Integer.parseInt(uid));
            user.setClientId(clientId);
            int rows = service.update(user);
            JSONObject response = new JSONObject();
            response.put("code", rows > 0 ? 1 : 0);
            response.put("data", rows);
            response.put("msg", rows > 0 ? "操作成功" : "操作失败");
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(0, "操作失败", null);
        }
    }
    /***
     * 用户修改
     * @param params Bean对象JSON字符串
     */
    @RequestMapping(value = "/userEdit")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String userEdit(@RequestParam(value = "params", required = false) String params,
                           @RequestParam(value = "token", required = false) String token) {
        try {
            TypechoUsers update = null;
            Map jsonToMap = null;
            String code = "";

            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            String uid = map.get("uid").toString();
            TypechoUsers user = new TypechoUsers();
            Integer isForbidden = 0;
            if (StringUtils.isNotBlank(params)) {
                Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
                jsonToMap = JSONObject.parseObject(JSON.parseObject(params).toString());
                //根据验证码判断是否要修改邮箱
                if (jsonToMap.get("code") != null && jsonToMap.get("mail") != null) {

                    Integer isEmail = Integer.parseInt(apiconfig.get("isEmail").toString());
                    if(isEmail>0){
                        String email = jsonToMap.get("mail").toString();
                        //判断邮箱是否已被其它用户绑定
                        user.setMail(email);
                        List<TypechoUsers> ulist = service.selectList(user);
                        if(ulist.size() > 0){
                            String oldEmail = ulist.get(0).getMail();
                            if(oldEmail.equals(email)){
                                return Result.getResultJson(0, "该邮箱已被绑定", null);
                            }
                        }
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
                //根据验证码判断是否要修改手机号
                if (jsonToMap.get("code") != null && jsonToMap.get("phone") != null) {
                    String phone = jsonToMap.get("phone").toString();
                    //判断手机号是否已被其它用户绑定
                    user.setMail(phone);
                    List<TypechoUsers> ulist = service.selectList(user);
                    if(ulist.size() > 0){
                        String oldPhone = ulist.get(0).getPhone();
                        if(oldPhone.equals(phone)){
                            return Result.getResultJson(0, "该手机号已被绑定", null);
                        }
                    }
                    if (redisHelp.getRedis(this.dataprefix + "_" + "sendSMS" + phone, redisTemplate) != null) {
                        String sendCode = redisHelp.getRedis(this.dataprefix + "_" + "sendSMS" + phone, redisTemplate);
                        code = jsonToMap.get("code").toString();
                        if (!sendCode.equals(code)) {
                            return Result.getResultJson(0, "验证码不正确", null);
                        }
                    } else {
                        return Result.getResultJson(0, "验证码不正确或已失效", null);
                    }
                } else {
                    jsonToMap.remove("phone");
                }
                jsonToMap.remove("code");
                if (jsonToMap.get("password") != null) {
                    String p = jsonToMap.get("password").toString();
                    String passwd = phpass.HashPassword(p);
                    jsonToMap.put("password", passwd);
                }

                if (jsonToMap.get("uid") == null) {
                    return Result.getResultJson(0, "用户不存在", null);
                }
                Integer userID = Integer.parseInt(jsonToMap.get("uid").toString());
                user = service.selectByKey(userID);
                if(user==null){
                    return Result.getResultJson(0, "用户不存在", null);
                }
                String forbidden = "";
                if(apiconfig.get("forbidden")!=null){
                    forbidden = apiconfig.get("forbidden").toString();
                }
                if(jsonToMap.get("introduce") != null){
                    String introduce = jsonToMap.get("introduce").toString();
                    Integer isIntroForbidden = baseFull.getForbidden(forbidden,introduce);
                    if(isIntroForbidden.equals(1)){
                        isForbidden = 1;
                        jsonToMap.remove("introduce");
                    }
                    //腾讯云内容违规检测
                    if(apiconfig.get("cmsSwitch").toString().equals("1")|apiconfig.get("cmsSwitch").toString().equals("3")){
                        try {
                            String setText = baseFull.htmlToText(introduce);
                            setText = baseFull.encrypt(setText);
                            Map violationData = securityService.textViolation(setText);
                            String Suggestion = violationData.get("Suggestion").toString();
                            if(Suggestion.equals("Block")){
                                Set<String> allKeywords = new HashSet<>();
                                Object keywordObj = violationData.get("Keywords");
                                if (keywordObj instanceof List) {
                                    List<?> keywords = (List<?>) keywordObj;
                                    for (Object word : keywords) {
                                        if (word != null) {
                                            allKeywords.add(word.toString());
                                        }
                                    }
                                }
                                String textBlockTips = "内容涉及违规，违规内容：" + String.join("，", allKeywords);
                                return Result.getResultJson(0,textBlockTips,null);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                jsonToMap.remove("name");
                jsonToMap.remove("group");
                //部分字段不允许修改
                jsonToMap.remove("customize");
                jsonToMap.remove("created");
                jsonToMap.remove("activated");
                jsonToMap.remove("logged");
                jsonToMap.remove("authCode");
                jsonToMap.remove("bantime");
                jsonToMap.remove("posttime");
                //jsonToMap.remove("introduce");

                jsonToMap.remove("assets");
                jsonToMap.remove("experience");
                jsonToMap.remove("vip");
                jsonToMap.remove("points");
                if(jsonToMap.get("screenName")!=null){
                    //验证用户名是否违禁
                    String screenName = jsonToMap.get("screenName").toString();

                    Integer isNameForbidden = baseFull.getForbidden(forbidden,screenName);
                    if(isNameForbidden.equals(1)){
                        return Result.getResultJson(0, "用户名包含违规词语", null);
                    }
                    user.setScreenName(screenName);
                    List<TypechoUsers> userlist = service.selectList(user);
                    if(userlist.size() > 0){
                        Integer myuid = Integer.parseInt(uid);
                        if(!userlist.get(0).getUid().equals(myuid)){
                            return Result.getResultJson(0, "该昵称已被占用！", null);
                        }
                    }
                    //腾讯云内容违规检测
                    if(apiconfig.get("cmsSwitch").toString().equals("1")|apiconfig.get("cmsSwitch").toString().equals("3")){
                        try{
                            String setText = baseFull.htmlToText(screenName);
                            setText = baseFull.encrypt(setText);
                            Map violationData = securityService.textViolation(setText);
                            String Suggestion = violationData.get("Suggestion").toString();
                            if(Suggestion.equals("Block")){
                                Set<String> allKeywords = new HashSet<>();
                                Object keywordObj = violationData.get("Keywords");
                                if (keywordObj instanceof List) {
                                    List<?> keywords = (List<?>) keywordObj;
                                    for (Object word : keywords) {
                                        if (word != null) {
                                            allKeywords.add(word.toString());
                                        }
                                    }
                                }
                                String textBlockTips = "昵称内容涉及违规，违规内容：" + String.join("，", allKeywords);
                                return Result.getResultJson(0,textBlockTips,null);
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }

                update = JSON.parseObject(JSON.toJSONString(jsonToMap), TypechoUsers.class);
            }else{
                return Result.getResultJson(0, "参数不正确", null);
            }
            update.setUid(Integer.parseInt(uid));
            int rows = service.update(update);

            if (rows > 0 && jsonToMap.get("password") != null) {
                //执行成功后，如果密码发生了改变，需要重新登陆
                redisHelp.delete(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            }
            if (rows > 0 && jsonToMap.get("mail") != null) {
                //执行成功后，如果邮箱发生了改变，则重新登陆
                redisHelp.delete(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            }
            String responseText = "操作成功";
            if(isForbidden.equals(1)){
                responseText = "简介存在违禁词，该字段未修改。";
            }
            JSONObject response = new JSONObject();
            response.put("code", rows > 0 ? 1 : 0);
            response.put("data", rows);
            response.put("msg", rows > 0 ? responseText : "操作失败");
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(0, "接口请求异常，请联系管理员", null);
        }

    }

    /***
     * 用户修改（管理员）
     * @param params Bean对象JSON字符串
     */
    @RequestMapping(value = "/manageUserEdit")
    @ResponseBody
    @LoginRequired(purview = "1")
    public String manageUserEdit(@RequestParam(value = "params", required = false) String params, @RequestParam(value = "token", required = false) String token) {
        try {
            TypechoUsers update = null;
            Map jsonToMap = null;
            String code = "";
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
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
                if(jsonToMap.get("group")!=null){
                    String groupText = jsonToMap.get("group").toString();
                    if(!groupText.equals("administrator")&&!groupText.equals("editor")&&!groupText.equals("contributor")&&!groupText.equals("subscriber")&&!groupText.equals("visitor")){
                        return Result.getResultJson(0, "用户组不正确", null);
                    }
                    jsonToMap.put("groupKey",groupText);
                }

                //部分字段不允许修改

                jsonToMap.remove("created");
                jsonToMap.remove("activated");
                jsonToMap.remove("logged");
                jsonToMap.remove("authCode");
                //jsonToMap.remove("introduce");
                jsonToMap.remove("assets");
                update = JSON.parseObject(JSON.toJSONString(jsonToMap), TypechoUsers.class);
                if (jsonToMap.get("customize") == null) {
                    update.setCustomize("");
                }
            }

            int rows = service.update(update);
            //如果修改了密码、权限、头衔，则让用户强制重新登陆
            if(update.getGroupKey()!=null||update.getExperience()!=null||update.getScreenName()!=null||update.getMail()!=null||update.getPassword()!=null){
                String oldToken = null;
                if (redisHelp.getRedis(this.dataprefix + "_" + "userkey" + name, redisTemplate) != null) {
                    oldToken = redisHelp.getRedis(this.dataprefix + "_" + "userkey" + name, redisTemplate);
                }
                if (oldToken != null) {
                    redisHelp.delete(this.dataprefix + "_" + "userInfo" + oldToken, redisTemplate);
                    redisHelp.delete(this.dataprefix + "_" + "userkey" + name, redisTemplate);
                }
            }
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
     * 用户状态检测
     *
     */
    @RequestMapping(value = "/userStatus")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String userStatus(@RequestParam(value = "token", required = false) String token) {
        Map jsonToMap = null;
        Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
        Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
        Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
        Integer uid = Integer.parseInt(map.get("uid").toString());
        TypechoUsers users = service.selectByKey(uid);
        //判断用户是否被封禁
        Integer bantime = users.getBantime();
        if(bantime.equals(1)){
            return Result.getResultJson(0, "你的账号已被永久封禁，如有疑问请联系管理员", null);
        }else{
            Long date = System.currentTimeMillis();
            Integer curtime = Integer.parseInt(String.valueOf(date).substring(0,10));
            if(bantime > curtime){
                return Result.getResultJson(0, "你的账号被暂时封禁，请耐心等待解封。", null);
            }
        }
        Map json = JSONObject.parseObject(JSONObject.toJSONString(users), Map.class);
//            TypechoComments comments = new TypechoComments();
//            comments.setAuthorId(uid);
//            Integer lv = commentsService.total(comments,null);
        if(json.get("avatar")==null){
            if (json.get("mail") != null) {
                String mail = json.get("mail").toString();

                if(mail.indexOf("@qq.com") != -1){
                    String qq = mail.replace("@qq.com","");
                    json.put("avatar", "https://q1.qlogo.cn/g?b=qq&nk="+qq+"&s=640");
                }else{
                    json.put("avatar", baseFull.getAvatar(apiconfig.get("webinfoAvatar").toString(), mail));
                }
                //json.put("avatar", baseFull.getAvatar(apiconfig.get("webinfoAvatar").toString(), json.get("mail").toString()));

            } else {
                json.put("avatar", apiconfig.get("webinfoAvatar").toString() + "null");
            }
        }
        json.remove("password");
        json.remove("clientId");
        //判断是否为VIP
        json.put("isvip", 0);
        Long date = System.currentTimeMillis();
        String curTime = String.valueOf(date).substring(0, 10);
        Integer viptime  = users.getVip();
        if(viptime>Integer.parseInt(curTime)||viptime.equals(1)){
            json.put("isvip", 1);
        }
//            json.put("lv", baseFull.getLv(lv));
        JSONObject response = new JSONObject();

        response.put("code", 1);
        response.put("msg", "");
        response.put("data", json);

        return response.toString();

    }

    /***
     * 用户删除
     */
    @RequestMapping(value = "/userDelete")
    @ResponseBody
    @LoginRequired(purview = "2")
    public String userDelete(@RequestParam(value = "key", required = false) String key, @RequestParam(value = "token", required = false) String token) {
        try {
            //String group = (String) redisHelp.getValue("userInfo"+token,"group",redisTemplate);
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            Integer logUid =Integer.parseInt(map.get("uid").toString());
            TypechoUsers users = service.selectByKey(key);
            if(users==null){
                return Result.getResultJson(0, "该用户不存在", null);
            }
            if(users.getUid().equals(logUid)){
                return Result.getResultJson(0, "你不可以删除你自己", null);
            }
            //删除关联的信息
            TypechoUserapi userapi = new TypechoUserapi();
            userapi.setUid(Integer.parseInt(key));
            Integer isApi = userapiService.total(userapi);
            if(isApi > 0){
                userapiService.deleteUserAll(key);
            }
            //删除用户登录状态
            String oldToken = redisHelp.getRedis(this.dataprefix + "_" + "userkey" + users.getName(), redisTemplate);
            if (oldToken != null) {
                redisHelp.delete(this.dataprefix + "_" + "userInfo" + oldToken, redisTemplate);
                redisHelp.delete(this.dataprefix + "_" + "userkey" + users.getName(), redisTemplate);
            }
            int rows = service.delete(key);
            editFile.setLog("管理员"+logUid+"删除了用户"+key);
            JSONObject response = new JSONObject();
            response.put("code", rows > 0 ? 1 : 0);
            response.put("data", rows);
            response.put("msg", rows > 0 ? "操作成功" : "操作失败");
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(0, "操作失败", null);
        }

    }

    /***
     * 发起提现
     */
    @RequestMapping(value = "/userWithdraw")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String userWithdraw(@RequestParam(value = "num", required = false) Integer num, @RequestParam(value = "token", required = false) String token) {
        try {
            if(num==null){
                return Result.getResultJson(0, "参数错误", null);
            }
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            Integer uid = Integer.parseInt(map.get("uid").toString());
            //查询用户是否设置pay
            if(num < 1){
                return Result.getResultJson(0, "参数错误", null);
            }
            TypechoUsers user = service.selectByKey(uid);
            if (user.getPay() == null) {
                return Result.getResultJson(0, "请先设置收款信息", null);
            }
            Integer Assets = user.getAssets();
            if(num > Assets){
                return Result.getResultJson(0, "你的余额不足", null);
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
            e.printStackTrace();
            return Result.getResultJson(0, "操作失败", null);
        }

    }

    /***
     * 提现列表
     */
    @RequestMapping(value = "/withdrawList")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String withdrawList(@RequestParam(value = "searchParams", required = false) String searchParams,
                               @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                               @RequestParam(value = "limit", required = false, defaultValue = "15") Integer limit,
                               @RequestParam(value = "token", required = false) String token) {
        if(limit>50){
            limit = 50;
        }
        Integer total = 0;
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
            total = userlogService.total(query);

        }

        PageList<TypechoUserlog> pageList = userlogService.selectPage(query, page, limit);
        List jsonList = new ArrayList();
        List<TypechoUserlog> list = pageList.getList();
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
        response.put("total", total);
        return response.toString();
    }

    /***
     * 提现审核
     */
    @RequestMapping(value = "/withdrawStatus")
    @ResponseBody
    @LoginRequired(purview = "2")
    public String withdrawStatus(@RequestParam(value = "key", required = false) Integer key, @RequestParam(value = "type", required = false) Integer type, @RequestParam(value = "token", required = false) String token) {
        try {
            //String group = (String) redisHelp.getValue("userInfo"+token,"group",redisTemplate);
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            Integer loguid =Integer.parseInt(map.get("uid").toString());
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
                //添加财务记录
                Long date = System.currentTimeMillis();
                String curTime = String.valueOf(date).substring(0, 10);
                TypechoPaylog paylog = new TypechoPaylog();
                paylog.setStatus(1);
                paylog.setCreated(Integer.parseInt(curTime));
                paylog.setUid(uid);
                paylog.setOutTradeNo(curTime+"withdraw");
                paylog.setTotalAmount("-"+num);
                paylog.setPaytype("withdraw");
                paylog.setSubject("申请提现");
                paylogService.insert(paylog);
                //发送消息通知
                String created = String.valueOf(date).substring(0,10);
                TypechoInbox inbox = new TypechoInbox();
                inbox.setUid(loguid);
                inbox.setTouid(uid);
                inbox.setType("finance");
                inbox.setText("你的提现审核已经审核通过");
                inbox.setValue(0);
                inbox.setCreated(Integer.parseInt(created));
                inboxService.insert(inbox);
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
            e.printStackTrace();
            return Result.getResultJson(0, "操作失败", null);
        }

    }

    /***
     * 管理员手动充扣
     */
    @RequestMapping(value = "/userRecharge")
    @ResponseBody
    @LoginRequired(purview = "2")
    public String userRecharge(@RequestParam(value = "key", required = false) Integer key,
                               @RequestParam(value = "num", required = false) Integer num,
                               @RequestParam(value = "type", required = false) Integer type,
                               @RequestParam(value = "rechargeType", required = false, defaultValue = "0") Integer rechargeType,
                               @RequestParam(value = "token", required = false) String token) {
        //String group = (String) redisHelp.getValue("userInfo"+token,"group",redisTemplate);
        Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
        TypechoUsers user = service.selectByKey(key);
        Integer oldAssets = user.getAssets();
        Integer oldPoints = user.getPoints();
        if (num <= 0) {
            return Result.getResultJson(0, "金额不正确", null);
        }
        Integer assets = 0;
        Integer points = 0;
        //生成系统对用户资产操作的日志
        Long date = System.currentTimeMillis();
        String userTime = String.valueOf(date).substring(0,10);
        TypechoPaylog paylog = new TypechoPaylog();
        paylog.setStatus(1);
        paylog.setCreated(Integer.parseInt(userTime));
        paylog.setUid(key);
        paylog.setOutTradeNo(userTime+"system");
        paylog.setPaytype("system");
        //rechargeType 0是充值资产，1是充值积分
        if(rechargeType.equals(0)){
            //0是充值，1是扣款
            if (type.equals(0)) {
                assets = oldAssets + num;
                paylog.setTotalAmount(num+"");
                paylog.setSubject("系统充值资产");
            } else {
                assets = oldAssets - num;
                paylog.setTotalAmount("-"+num);
                paylog.setSubject("系统扣除资产");
            }
        }else{
            //0是充值，1是扣款
            if (type.equals(0)) {
                points = oldPoints + num;
                paylog.setTotalAmount(num+"");
                paylog.setSubject("系统充值积分");
            } else {
                points = oldPoints - num;
                paylog.setTotalAmount("-"+num);
                paylog.setSubject("系统扣除积分");
            }
        }

        paylogService.insert(paylog);
        TypechoUsers update = new TypechoUsers();
        update.setUid(user.getUid());
        if(rechargeType.equals(0)){
            update.setAssets(assets);
        }else{
            update.setPoints(points);
        }
        Integer rows = service.update(update);
        JSONObject response = new JSONObject();
        response.put("code", rows > 0 ? 1 : 0);
        response.put("data", rows);
        response.put("msg", rows > 0 ? "操作成功" : "操作失败");
        return response.toString();
    }
    /**
     * 退出登录
     * **/
    @RequestMapping(value = "/signOut")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String signOut(@RequestParam(value = "token", required = false) String  token) {
        try {
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            String name = map.get("name").toString();
            redisHelp.delete(this.dataprefix + "_" + "userkey" + name, redisTemplate);
            redisHelp.delete(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            return Result.getResultJson(1, "退出成功", null);
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0, "退出失败", null);
        }

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
            e.printStackTrace();
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
//        TypechoComments comments = new TypechoComments();
//        comments.setAuthorId(uid);
//        Integer lv = commentsService.total(comments,null);
        json.remove("password");
        json.remove("groupKey");
//        json.put("lv", baseFull.getLv(lv));
        json.put("token", token);

        json.put("group", users.getGroupKey());
        Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);

        if(users.getAvatar()==null){
            if (json.get("mail") != null) {
                String mail = json.get("mail").toString();

                if(mail.indexOf("@qq.com") != -1){
                    String qq = mail.replace("@qq.com","");
                    json.put("avatar", "https://q1.qlogo.cn/g?b=qq&nk="+qq+"&s=640");
                }else{
                    json.put("avatar", baseFull.getAvatar(apiconfig.get("webinfoAvatar").toString(), mail));
                }
                //json.put("avatar", baseFull.getAvatar(apiconfig.get("webinfoAvatar").toString(), json.get("mail").toString()));

            } else {
                json.put("avatar", apiconfig.get("webinfoAvatar").toString() + "null");
            }
        }else{
            json.put("avatar", users.getAvatar());
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
            e.printStackTrace();
            return Result.getResultJson(0, "请求异常", null);
        }

    }
    /***
     * 注册系统配置信息
     */
    @RequestMapping(value = "/regConfig")
    @ResponseBody
    @LoginRequired(purview = "-1")
    public String regConfig() {
        JSONObject data = new JSONObject();
        Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
        data.put("isEmail",Integer.parseInt(apiconfig.get("isEmail").toString()));
        data.put("isInvite",Integer.parseInt(apiconfig.get("isInvite").toString()));
        data.put("isPhone",Integer.parseInt(apiconfig.get("isPhone").toString()));
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
    @LoginRequired(purview = "2")
    public String madeInvitation(@RequestParam(value = "num", required = false) Integer  num,@RequestParam(value = "token", required = false) String  token) {
        try{
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            Integer uid =Integer.parseInt(map.get("uid").toString());
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
            e.printStackTrace();
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
    @LoginRequired(purview = "2")
    public String invitationList (@RequestParam(value = "searchParams", required = false) String  searchParams,
                                  @RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                                  @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit,
                                  @RequestParam(value = "token", required = false) String  token) {
        Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);

        Integer total = 0;
        TypechoInvitation query = new TypechoInvitation();
        if (StringUtils.isNotBlank(searchParams)) {
            JSONObject object = JSON.parseObject(searchParams);
            query = object.toJavaObject(TypechoInvitation.class);
            total = invitationService.total(query);
        }

        PageList<TypechoInvitation> pageList = invitationService.selectPage(query, page, limit);
        JSONObject response = new JSONObject();
        response.put("code" , 1);
        response.put("msg"  , "");
        response.put("data" , null != pageList.getList() ? pageList.getList() : new JSONArray());
        response.put("count", pageList.getTotalCount());
        response.put("total", total);
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
        query.setStatus(0);
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
    /***
     * 用户收件箱
     *
     */
    @RequestMapping(value = "/inbox")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String inbox (@RequestParam(value = "token", required = false) String  token,
                         @RequestParam(value = "type", required = false , defaultValue = "comment") String  type,
                         @RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                         @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit) {
        if(limit>50){
            limit = 50;
        }
        TypechoInbox query = new TypechoInbox();
        List jsonList = new ArrayList();
        Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
        Integer uid =Integer.parseInt(map.get("uid").toString());
        List cacheList = redisHelp.getList(this.dataprefix+"_"+"inbox_"+page+"_"+limit+"_"+type+"_"+uid,redisTemplate);
        query.setType(type);
        query.setTouid(uid);
        Integer total = inboxService.total(query);
        Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
        try{
            if(cacheList.size()>0){
                jsonList = cacheList;
            }else{
                PageList<TypechoInbox> pageList = inboxService.selectPage(query, page, limit);
                List<TypechoInbox> list = pageList.getList();
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
                    TypechoInbox inbox = list.get(i);
                    Integer userid = inbox.getUid();
                    Map userJson = new HashMap();
                    //如果是未注册游客评论
                    if(userid.equals(0)){
                        if(inbox.getType().equals("comment")){
                            //获取评论信息
                            Integer coid = inbox.getCid();
                            TypechoComments comments = commentsService.selectByKey(coid);
                            if(comments==null){
                                userJson = UserStatus.getUserInfo(userid,allconfigService,service);
                            }else{
                                userJson.put("avatar","");
                                userJson.put("uid",0);
                                userJson.put("name",comments.getAuthor());
                                String avatar = "";
                                if(comments.getMail()!=null&&comments.getMail()!=""){
                                    String mail = comments.getMail();
                                    if(mail.indexOf("@qq.com") != -1){
                                        String qq = mail.replace("@qq.com","");
                                        avatar = "https://q1.qlogo.cn/g?b=qq&nk="+qq+"&s=640";
                                    }else{
                                        avatar = baseFull.getAvatar(apiconfig.get("webinfoAvatar").toString(), comments.getMail());
                                    }
                                    //avatar = baseFull.getAvatar(apiconfig.get("webinfoAvatar").toString(), author.getMail());
                                }
                                userJson.put("avatar",avatar);
                            }
                        }
                    }else{
                        //获取用户信息
                        userJson = UserStatus.getUserInfo(userid,allconfigService,service);
                    }
                    //获取用户等级
//                    TypechoComments comments = new TypechoComments();
//                    comments.setAuthorId(userid);
//                    Integer lv = commentsService.total(comments,null);
//                    userJson.put("lv", baseFull.getLv(lv));
                    json.put("userJson",userJson);
                    if(inbox.getType().equals("comment")){
                        TypechoContents contentsInfo = contentsService.selectByKey(inbox.getValue());
                        if(contentsInfo!=null){
                            json.put("contenTitle",contentsInfo.getTitle());
                            //加入文章数据
                            Map contentsJson = new HashMap();
                            contentsJson.put("cid",contentsInfo.getCid());
                            contentsJson.put("slug",contentsInfo.getSlug());
                            contentsJson.put("title",contentsInfo.getTitle());
                            contentsJson.put("type",contentsInfo.getType());
                            json.put("contentsInfo",contentsJson);
                        }else{
                            json.put("contenTitle","文章已删除");
                        }
                    }
                    jsonList.add(json);
                }
                redisHelp.delete(this.dataprefix+"_"+"inbox_"+page+"_"+limit+"_"+type+"_"+uid,redisTemplate);
                redisHelp.setList(this.dataprefix+"_"+"inbox_"+page+"_"+limit+"_"+type+"_"+uid,jsonList,3,redisTemplate);
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
     * 获取用户未读消息数量
     *
     */
    @RequestMapping(value = "/unreadNum")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String unreadNum (@RequestParam(value = "token", required = false) String  token) {
        TypechoInbox query = new TypechoInbox();

        Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
        Integer uid =Integer.parseInt(map.get("uid").toString());
        Map data = new HashMap();
        Map cacheInfo = redisHelp.getMapValue(this.dataprefix+"_"+"unreadNum_"+uid,redisTemplate);
        if(cacheInfo.size()>0){
            data = cacheInfo;
        }else{
            query.setTouid(uid);
            query.setIsread(0);
            //评论comment，财务finance，系统system，聊天chat，粉丝fan
            Integer total = inboxService.total(query);
            query.setType("comment");
            Integer comment = inboxService.total(query);
            query.setType("finance");
            Integer finance = inboxService.total(query);
            query.setType("system");
            Integer system = inboxService.total(query);
            query.setType("fan");
            Integer fan = inboxService.total(query);

            String unReadMsg = redisHelp.getRedis(this.dataprefix+"_unReadMsg_"+uid,redisTemplate);
            Integer chat = 0;
            if(unReadMsg!=null){
                chat = Integer.parseInt(unReadMsg);
            }
            total = total + chat;
            data.put("total",total);
            data.put("comment",comment);
            data.put("finance",finance);
            data.put("system",system);
            data.put("chat",chat);
            data.put("fan",fan);
            redisHelp.delete(this.dataprefix+"_"+"unreadNum_"+uid,redisTemplate);
            redisHelp.setKey(this.dataprefix+"_"+"unreadNum_"+uid,data,30,redisTemplate);
        }
        JSONObject response = new JSONObject();
        response.put("code" , 1);
        response.put("msg"  , "");
        response.put("data" ,data);
        return response.toString();
    }
    /***
     * 将所有消息已读
     *
     */
    @RequestMapping(value = "/setRead")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String setRead (@RequestParam(value = "token", required = false) String  token,
                           @RequestParam(value = "type", required = false,defaultValue = "all") String  type) {
        TypechoInbox query = new TypechoInbox();
        try {
            if(!type.equals("all")&&!type.equals("comment")&&!type.equals("finance")&&!type.equals("chat")&&!type.equals("fan")&&!type.equals("system")){
                return Result.getResultJson(0, "参数错误", null);
            }
            //评论comment，财务finance，系统system，聊天chat，粉丝fan
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            Integer uid =Integer.parseInt(map.get("uid").toString());
            if(type.equals("all")){
                redisHelp.deleteKeysWithPattern("*"+this.dataprefix+"_"+"myChat_"+uid+"*",redisTemplate,this.dataprefix);
                jdbcTemplate.execute("UPDATE "+this.prefix+"_inbox SET isread = 1 WHERE touid="+uid+";");
                //聊天消息归零
                jdbcTemplate.execute("UPDATE "+this.prefix+"_chat SET otherUnRead = 0 WHERE toid="+uid+" and type = 0;");
                jdbcTemplate.execute("UPDATE "+this.prefix+"_chat SET myUnRead = 0 WHERE uid="+uid+" and type = 0;");
                redisHelp.setRedis(this.dataprefix+"_unReadMsg_"+uid,"0",600,redisTemplate);
            }else{
                if(type.equals("comment")){
                    //要同时去除文章和帖子评论的消息数量
                    jdbcTemplate.execute("UPDATE "+this.prefix+"_inbox SET isread = 1 WHERE touid ="+uid+" AND type = 'comment' ;");
                    jdbcTemplate.execute("UPDATE "+this.prefix+"_inbox SET isread = 1 WHERE touid ="+uid+" AND type = 'postComment' ;");
                }else{
                    jdbcTemplate.execute("UPDATE "+this.prefix+"_inbox SET isread = 1 WHERE touid ="+uid+" AND type = '"+type+"' ;");

                }
            }
            redisHelp.delete(this.dataprefix+"_"+"unreadNum_"+uid,redisTemplate);
            return Result.getResultJson(1, "操作成功", null);
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0, "操作失败", null);
        }
    }
    /***
     * 向指定用户发送消息
     */
    @RequestMapping(value = "/sendUser")
    @ResponseBody
    @LoginRequired(purview = "2")
    public String sendUser(@RequestParam(value = "token", required = false) String  token,
                           @RequestParam(value = "uid", required = false, defaultValue = "1") Integer uid,
                           @RequestParam(value = "text", required = false, defaultValue = "1") String text) {
        try{

            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            String group = map.get("group").toString();

            if(text.length()<1){
                return Result.getResultJson(0, "发送内容不能为空", null);
            }
            TypechoUsers user = service.selectByKey(uid);
            if(user==null){
                return Result.getResultJson(0, "该用户不存在", null);
            }else{
                //如果用户存在客户端id，则发送app通知
                if(user.getClientId()!=null){
                    Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
                    String title = apiconfig.get("webinfoTitle").toString();
                    try {
                        pushService.sendPushMsg(user.getClientId(),title+"系统消息",text,"payload","system",apiconfig);
                    }catch (Exception e){
                        System.err.println("通知发送失败："+e);
                    }

                }
            }
            Integer muid =Integer.parseInt(map.get("uid").toString());

            //普通用户最大邮件限制
            if(!group.equals("administrator")&&!group.equals("editor")){
                String sendUser = redisHelp.getRedis(this.dataprefix+"_"+muid+"_sendUser",redisTemplate);
                if(sendUser==null){
                    redisHelp.setRedis(this.dataprefix+"_"+muid+"_sendUser","1",86400,redisTemplate);
                }else{
                    Integer send_User = Integer.parseInt(sendUser) + 1;
                    if(send_User > 4){
                        return Result.getResultJson(0,"你已超过最大邮件限制，请您24小时后再操作",null);
                    }else{
                        redisHelp.setRedis(this.dataprefix+"_"+muid+"_sendUser",send_User.toString(),86400,redisTemplate);
                    }
                }
            }
            //限制结束

            Long date = System.currentTimeMillis();
            String created = String.valueOf(date).substring(0,10);
            TypechoInbox insert = new TypechoInbox();
            insert.setUid(muid);
            insert.setTouid(uid);
            insert.setType("system");
            insert.setText(text);
            insert.setCreated(Integer.parseInt(created));
            int rows = inboxService.insert(insert);

            JSONObject response = new JSONObject();
            response.put("code" , rows);
            response.put("msg"  , rows > 0 ? "发送成功" : "发送失败");
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0, "发送失败", null);
        }

    }
    /***
     * 关注和取消关注
     */
    @RequestMapping(value = "/follow")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String follow(@RequestParam(value = "token", required = false) String  token,
                         @RequestParam(value = "touid", required = false, defaultValue = "1") Integer touid,
                         @RequestParam(value = "type", required = false, defaultValue = "1") Integer type) {
        try{
            if(touid==0||touid==null||type==null){
                return Result.getResultJson(0, "参数不正确", null);
            }
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            Integer uid =Integer.parseInt(map.get("uid").toString());
            //登录情况下，刷数据攻击拦截
            Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
            if(apiconfig.get("banRobots").toString().equals("1")) {
                String isSilence = redisHelp.getRedis(this.dataprefix+"_"+uid+"_silence",redisTemplate);
                if(isSilence!=null){
                    return Result.getResultJson(0,"你已被禁止请求，请耐心等待",null);
                }
                String isRepeated = redisHelp.getRedis(this.dataprefix+"_"+uid+"_isRepeated",redisTemplate);
                if(isRepeated==null){
                    redisHelp.setRedis(this.dataprefix+"_"+uid+"_isRepeated","1",1,redisTemplate);
                }else{
                    Integer frequency = Integer.parseInt(isRepeated) + 1;
                    if(frequency==1){
                        securityService.safetyMessage("用户ID："+uid+"，在关注接口疑似存在攻击行为，请及时确认处理。","system");
                        redisHelp.setRedis(this.dataprefix+"_"+uid+"_silence","1",Integer.parseInt(apiconfig.get("silenceTime").toString()),redisTemplate);
                        return Result.getResultJson(0,"你的请求存在恶意行为，10分钟内禁止操作！",null);
                    }else{
                        redisHelp.setRedis(this.dataprefix+"_"+uid+"_isRepeated",frequency.toString(),3,redisTemplate);
                    }
                    return Result.getResultJson(0,"你的操作太频繁了",null);
                }
            }

            //攻击拦截结束

            if(uid.equals(touid)){
                return Result.getResultJson(0, "你不可以关注自己", null);
            }
            TypechoFan fan = new TypechoFan();
            fan.setTouid(touid);
            fan.setUid(uid);
            Integer isFan = fanService.total(fan);
            //1是请求关注，0是取消关注
            if(type.equals(1)){
                if(isFan > 0){
                    return Result.getResultJson(0, "你已经关注过了", null);
                }
                Long date = System.currentTimeMillis();
                String created = String.valueOf(date).substring(0,10);
                fan.setCreated(Integer.parseInt(created));
                int rows = fanService.insert(fan);
                if(rows > 0){
                    //发送消息
                    TypechoInbox insert = new TypechoInbox();
                    insert.setUid(uid);
                    insert.setTouid(touid);
                    insert.setType("fan");
                    insert.setText("关注了你。");
                    insert.setCreated(Integer.parseInt(created));
                    inboxService.insert(insert);
                }
                JSONObject response = new JSONObject();
                response.put("code" , rows);
                response.put("msg"  , rows > 0 ? "关注成功" : "关注失败");
                return response.toString();
            }else{
                if(isFan < 1){
                    return Result.getResultJson(0, "你还未关注Ta", null);
                }
                int rows = 0;
                List<TypechoFan> fanlist = fanService.selectList(fan);
                if(fanlist.size()>0){
                    TypechoFan oldFan = fanlist.get(0);
                    Integer id = oldFan.getId();
                    rows = fanService.delete(id);
                }

                JSONObject response = new JSONObject();
                response.put("code" , rows);
                response.put("msg"  , rows > 0 ? "取消关注成功" : "取消关注失败");
                return response.toString();
            }

        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0, "接口异常，请联系管理员", null);
        }
    }
    /***
     * 关注状态
     */
    @RequestMapping(value = "/isFollow")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String isFollow(@RequestParam(value = "token", required = false) String  token,
                           @RequestParam(value = "touid", required = false, defaultValue = "1") Integer touid) {
        if(touid==0||touid==null){
            return Result.getResultJson(0, "参数不正确", null);
        }
        Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
        Integer uid =Integer.parseInt(map.get("uid").toString());
        TypechoFan fan = new TypechoFan();
        fan.setTouid(touid);
        fan.setUid(uid);
        Integer isFan = fanService.total(fan);
        if(isFan > 0){
            return Result.getResultJson(1, "已关注", null);
        }else{
            return Result.getResultJson(0, "未关注", null);
        }
    }
    /***
     * Ta关注的人
     */
    @RequestMapping(value = "/followList")
    @ResponseBody
    @LoginRequired(purview = "-1")
    public String followList(@RequestParam(value = "uid", required = false) Integer  uid,
                             @RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                             @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit) {
        if(limit>50){
            limit = 50;
        }
        TypechoFan query = new TypechoFan();
        List jsonList = new ArrayList();
        List cacheList = redisHelp.getList(this.dataprefix+"_"+"followList_"+page+"_"+limit+"_"+uid,redisTemplate);
        query.setUid(uid);
        Integer total = fanService.total(query);
        try{
            if(cacheList.size()>0){
                jsonList = cacheList;
            }else{

                Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);

                PageList<TypechoFan> pageList = fanService.selectPage(query, page, limit);
                List<TypechoFan> list = pageList.getList();
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
                    TypechoFan fan = list.get(i);
                    Integer userid = fan.getTouid();
                    TypechoUsers user = service.selectByKey(userid);
                    //获取用户信息
                    Map userJson = UserStatus.getUserInfo(userid,allconfigService,service);
                    //获取用户等级
//                    TypechoComments comments = new TypechoComments();
//                    comments.setAuthorId(userid);
//                    Integer lv = commentsService.total(comments,null);
//                    userJson.put("lv", baseFull.getLv(lv));
                    json.put("userJson",userJson);
                    jsonList.add(json);
                }
                redisHelp.delete(this.dataprefix+"_"+"followList_"+page+"_"+limit+"_"+uid,redisTemplate);
                redisHelp.setList(this.dataprefix+"_"+"followList_"+page+"_"+limit+"_"+uid,jsonList,3,redisTemplate);
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
     * 关注Ta的人
     */
    @RequestMapping(value = "/fanList")
    @ResponseBody
    @LoginRequired(purview = "-1")
    public String fanList(@RequestParam(value = "touid", required = false) Integer  touid,
                          @RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                          @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit) {
        if(limit>50){
            limit = 50;
        }
        TypechoFan query = new TypechoFan();
        List jsonList = new ArrayList();
        List cacheList = redisHelp.getList(this.dataprefix+"_"+"fanList_"+page+"_"+limit+"_"+touid,redisTemplate);
        query.setTouid(touid);
        Integer total = fanService.total(query);
        try{
            if(cacheList.size()>0){
                jsonList = cacheList;
            }else{

                Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);

                PageList<TypechoFan> pageList = fanService.selectPage(query, page, limit);
                List<TypechoFan> list = pageList.getList();
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
                    TypechoFan fan = list.get(i);
                    Integer userid = fan.getUid();
                    //获取用户信息
                    Map userJson = UserStatus.getUserInfo(userid,allconfigService,service);
                    //获取用户等级
//                    TypechoComments comments = new TypechoComments();
//                    comments.setAuthorId(userid);
//                    Integer lv = commentsService.total(comments,null);
//                    userJson.put("lv", baseFull.getLv(lv));
                    json.put("userJson",userJson);
                    jsonList.add(json);
                }
                redisHelp.delete(this.dataprefix+"_"+"fanList_"+page+"_"+limit+"_"+touid,redisTemplate);
                redisHelp.setList(this.dataprefix+"_"+"fanList_"+page+"_"+limit+"_"+touid,jsonList,3,redisTemplate);
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
     * 我拉黑的人
     */
    @RequestMapping(value = "/myBanList")
    @ResponseBody
    @LoginRequired(purview = "-1")
    public String myBanList(@RequestParam(value = "token", required = false) String  token,
                          @RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                          @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit) {
        if(limit>50){
            limit = 50;
        }
        TypechoUserlog query = new TypechoUserlog();
        List jsonList = new ArrayList();
        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid =Integer.parseInt(map.get("uid").toString());
        List cacheList = redisHelp.getList(this.dataprefix+"_"+"myBanList_"+uid+"_"+page+"_"+limit,redisTemplate);
        query.setUid(uid);
        query.setType("banUser");
        Integer total = userlogService.total(query);
        try{
            if(cacheList.size()>0){
                jsonList = cacheList;
            }else{


                PageList<TypechoUserlog> pageList = userlogService.selectPage(query, page, limit);
                List<TypechoUserlog> list = pageList.getList();
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
                    TypechoUserlog log = list.get(i);
                    Integer userid = log.getNum();
                    //获取用户信息
                    Map userJson = UserStatus.getUserInfo(userid,allconfigService,service);
                    //获取用户等级
                    json.put("userJson",userJson);
                    jsonList.add(json);
                }
                redisHelp.delete(this.dataprefix+"_"+"myBanList_"+uid+"_"+page+"_"+limit,redisTemplate);
                redisHelp.setList(this.dataprefix+"_"+"myBanList_"+uid+"_"+page+"_"+limit,jsonList,3,redisTemplate);
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
     * 管理员封禁指定用户
     */
    @RequestMapping(value = "/banUser")
    @ResponseBody
    @LoginRequired(purview = "1")
    public String sendUser(@RequestParam(value = "token", required = false) String  token,
                           @RequestParam(value = "uid", required = false) Integer uid,
                           @RequestParam(value = "time", required = false) Integer time,
                           @RequestParam(value = "type", required = false) String type,
                           @RequestParam(value = "text", required = false) String text){
        try {
            //防止重复提交
            String isRepeated = redisHelp.getRedis(token+"_isRepeated",redisTemplate);
            if(isRepeated==null){
                redisHelp.setRedis(token+"_isRepeated","1",5,redisTemplate);
            }else{
                return Result.getResultJson(0,"你的操作太频繁了",null);
            }


            if(time<0||time==null){
                return Result.getResultJson(0, "参数错误", null);
            }

            //处理类型（manager管理员操作，system系统自动）
            if(!type.equals("manager")&&!type.equals("system")){
                return Result.getResultJson(0, "参数错误", null);
            }
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            Integer userid =Integer.parseInt(map.get("uid").toString());

            //判断用户是否存在
            TypechoUsers olduser = service.selectByKey(uid);
            TypechoUsers users = new TypechoUsers();
            if(olduser==null){
                return Result.getResultJson(0, "该用户不存在", null);
            }
            if(olduser.getGroupKey().equals("administrator")){
                return Result.getResultJson(0, "该用户组无法封禁", null);
            }
            Integer updatetime = 0;
            Long date = System.currentTimeMillis();
            Integer curtime = Integer.parseInt(String.valueOf(date).substring(0,10));
            Integer oldtime = olduser.getBantime();
            //如果用户已经被封禁，则继续累加时间，如果没有在封禁状态，则从当前时间开始计算。
            if(oldtime > curtime){
                updatetime = oldtime;
            }else{
                updatetime = curtime;
            }
            updatetime = updatetime + time;
            users.setUid(uid);
            //如果time等于0则设置为当前时间，则等于解除封禁
            if(time.equals(0)){
                users.setBantime(curtime);
            }else{
                users.setBantime(updatetime);
            }
            int rows = service.update(users);
            //添加违规记录
            TypechoViolation violation = new TypechoViolation();
            violation.setUid(uid);
            violation.setType(type);
            violation.setText(text);
            violation.setHandler(userid);
            violation.setCreated(curtime);
            violationService.insert(violation);
            //删除用户登录状态
            String oldToken = redisHelp.getRedis(this.dataprefix + "_" + "userkey" + olduser.getName(), redisTemplate);
            if (oldToken != null) {
                redisHelp.delete(this.dataprefix + "_" + "userInfo" + oldToken, redisTemplate);
                redisHelp.delete(this.dataprefix + "_" + "userkey" + olduser.getName(), redisTemplate);
            }

            editFile.setLog("管理员"+userid+"请求封禁用户"+uid);
            JSONObject response = new JSONObject();
            response.put("code" , rows);
            response.put("msg"  , rows > 0 ? "操作成功" : "操作失败");
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0, "接口异常，请联系管理员", null);
        }
    }
    /***
     * 解封指定用户
     */
    @RequestMapping(value = "/unblockUser")
    @ResponseBody
    @LoginRequired(purview = "2")
    public String sendUser(@RequestParam(value = "token", required = false) String  token,
                           @RequestParam(value = "uid", required = false) Integer uid){
        try{

            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            Integer userid =Integer.parseInt(map.get("uid").toString());

            TypechoUsers users = service.selectByKey(uid);
            if(users==null){
                return Result.getResultJson(0, "用户不存在", null);
            }
            Long date = System.currentTimeMillis();
            Integer curtime = Integer.parseInt(String.valueOf(date).substring(0,10));
            Integer oldtime = users.getBantime();
            if(oldtime < curtime){
                return Result.getResultJson(0, "用户未被封禁", null);
            }
            TypechoUsers update = new TypechoUsers();
            update.setBantime(curtime);
            update.setUid(uid);
            int rows = service.update(update);

            editFile.setLog("管理员"+userid+"请求解封用户"+uid);

            JSONObject response = new JSONObject();
            response.put("code" ,rows > 0 ? 1: 0 );
            response.put("data" , rows);
            response.put("msg"  , rows > 0 ? "操作成功" : "操作失败");
            return response.toString();

        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0, "接口请求异常，请联系管理员", null);
        }

    }
    /***
     * 封禁记录
     */
    @RequestMapping(value = "/violationList")
    @ResponseBody
    @LoginRequired(purview = "-1")
    public String violationList (@RequestParam(value = "searchParams", required = false) String  searchParams,
                                 @RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                                 @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit) {
        TypechoViolation query = new TypechoViolation();
        Integer total = 0;
        List jsonList = new ArrayList();
        List cacheList = redisHelp.getList(this.dataprefix+"_"+"violationList_"+page+"_"+limit+"_"+searchParams,redisTemplate);
        if (StringUtils.isNotBlank(searchParams)) {
            JSONObject object = JSON.parseObject(searchParams);
            query = object.toJavaObject(TypechoViolation.class);
        }
        total = violationService.total(query);
        try {
            if (cacheList.size() > 0) {
                jsonList = cacheList;
            } else {
                PageList<TypechoViolation> pageList = violationService.selectPage(query, page, limit);
                List<TypechoViolation> list = pageList.getList();
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
                    TypechoViolation violation = list.get(i);
                    Integer userid = violation.getUid();
                    //获取用户信息
                    Map userJson = UserStatus.getUserInfo(userid,allconfigService,service);
                    //获取用户等级
//                    TypechoComments comments = new TypechoComments();
//                    comments.setAuthorId(userid);
//                    Integer lv = commentsService.total(comments,null);
//                    userJson.put("lv", baseFull.getLv(lv));
                    json.put("userJson",userJson);
                    jsonList.add(json);
                }

                redisHelp.delete(this.dataprefix+"_"+"violationList_"+page+"_"+limit+"_"+searchParams,redisTemplate);
                redisHelp.setList(this.dataprefix+"_"+"violationList_"+page+"_"+limit+"_"+searchParams,jsonList,30,redisTemplate);
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
     * 用户数据清理
     */
    @RequestMapping(value = "/userClean")
    @ResponseBody
    @LoginRequired(purview = "2")
    public String dataClean(@RequestParam(value = "clean", required = false) Integer  clean,
                            @RequestParam(value = "token", required = false) String  token,
                            @RequestParam(value = "uid", required = false) Integer  uid) {
        try {
            //1是清理用户签到，2是清理用户资产日志，3是清理用户订单数据，4是清理无效卡密
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            Integer logUid =Integer.parseInt(map.get("uid").toString());

            TypechoUsers users = service.selectByKey(uid);
            if(users==null){
                return Result.getResultJson(0, "该用户不存在", null);
            }
            if(users.getGroupKey().equals("administrator")){
                return Result.getResultJson(0, "不允许删除管理员的文章", null);
            }
            String text = "文章数据";
            //清除该用户所有文章
            if(clean.equals(1)){
                jdbcTemplate.execute("DELETE FROM "+this.prefix+"_contents WHERE authorId = "+uid+";");
            }
            //清除该用户所有评论
            if(clean.equals(2)){
                jdbcTemplate.execute("DELETE FROM "+this.prefix+"_comments WHERE authorId = "+uid+";");
                text = "评论数据";
            }
            //清除该用户所有动态
            if(clean.equals(3)){
                jdbcTemplate.execute("DELETE FROM "+this.prefix+"_space WHERE uid = "+uid+";");
                text = "动态数据";
            }
            //清除该用户所有商品
            if(clean.equals(4)){
                jdbcTemplate.execute("DELETE FROM "+this.prefix+"_shop WHERE uid = "+uid+";");
                text = "商品数据";
            }
            //清除该用户签到记录
            if(clean.equals(5)){
                jdbcTemplate.execute("DELETE FROM "+this.prefix+"_userlog WHERE type='clock' and uid = "+uid+";");
                text = "日志数据";
            }
            //清除该用户所有帖子
            if(clean.equals(6)){
                jdbcTemplate.execute("DELETE FROM "+this.prefix+"_forum WHERE authorId = "+uid+";");
                text = "帖子数据";
            }
            //清除该用户所有帖子评论
            if(clean.equals(7)){
                jdbcTemplate.execute("DELETE FROM "+this.prefix+"_forum_comment WHERE uid = "+uid+";");
                text = "帖子数据";
            }
            securityService.safetyMessage("管理员："+logUid+"，清除了用户"+uid+"所有"+text,"system");
            JSONObject response = new JSONObject();
            response.put("code" , 1);
            response.put("msg"  , "清理成功，缓存刷新后将自动生效");
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            JSONObject response = new JSONObject();
            response.put("code" , 0);
            response.put("msg"  , "接口请求异常，请联系管理员");
            return response.toString();
        }

    }
    /***
     * 限制和解除限制（普通功能限制，不记录数据库）
     */
    @RequestMapping(value = "/restrict")
    @ResponseBody
    @LoginRequired(purview = "2")
    public String restrict(@RequestParam(value = "token", required = false) String  token,
                           @RequestParam(value = "uid", required = false) Integer  uid,
                           @RequestParam(value = "type", required = false, defaultValue = "0") Integer  type) {
        try {

            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);

            Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
            if(type.equals(1)){
                redisHelp.setRedis(this.dataprefix+"_"+uid+"_silence","1",Integer.parseInt(apiconfig.get("silenceTime").toString()),redisTemplate);
            }else{
                String isSilence = redisHelp.getRedis(this.dataprefix+"_"+uid+"_silence",redisTemplate);
                if(isSilence==null){
                    return Result.getResultJson(0,"用户状态正常，无需操作",null);
                }
                redisHelp.delete(this.dataprefix+"_"+uid+"_silence", redisTemplate);
            }
            JSONObject response = new JSONObject();
            response.put("code" , 1);
            response.put("msg"  , "操作成功");
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            JSONObject response = new JSONObject();
            response.put("code" , 0);
            response.put("msg"  , "接口请求异常，请联系管理员");
            return response.toString();
        }
    }
    @RequestMapping(value = "/giftVIP")
    @ResponseBody
    @LoginRequired(purview = "2")
    public String giftVIP(@RequestParam(value = "token", required = false) String  token,
                           @RequestParam(value = "uid", required = false) Integer  uid,
                           @RequestParam(value = "day", required = false, defaultValue = "1") Integer  day) {
        try{

            if(day < 1){
                return Result.getResultJson(0,"参数错误！",null);
            }
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            Integer logUid =Integer.parseInt(map.get("uid").toString());
            Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
            Long date = System.currentTimeMillis();
            String curTime = String.valueOf(date).substring(0, 10);
            Integer days = 86400;
            TypechoUsers users = service.selectByKey(uid);
            //判断用户是否为VIP，决定是续期还是从当前时间开始计算
            Integer vip = users.getVip();
            //默认是从当前时间开始相加
            Integer vipTime = 0;
            if(vip.equals(1)){
                return Result.getResultJson(0,"用户已经是永久VIP，无需续期",null);
            }
            //如果已经是vip，走续期逻辑。
            if(vip > Integer.parseInt(curTime)){
                vipTime = vip + days*day;
            }else{
                //如果不是或者已过期
                vipTime = Integer.parseInt(curTime) + days*day;
            }
            Integer vipPrice = Integer.parseInt(apiconfig.get("vipPrice").toString());
            Integer AllPrice = day * vipPrice;
            TypechoUsers newUser = new TypechoUsers();
            newUser.setUid(uid);
            newUser.setVip(vipTime);
            int rows =  service.update(newUser);
            String created = String.valueOf(date).substring(0,10);
            TypechoPaylog paylog = new TypechoPaylog();
            paylog.setStatus(1);
            paylog.setCreated(Integer.parseInt(created));
            paylog.setUid(uid);
            paylog.setOutTradeNo(created+"buyvip");
            paylog.setTotalAmount("-"+AllPrice);
            paylog.setPaytype("buyvip");
            paylog.setSubject("管理员赠送VIP");
            paylogService.insert(paylog);
            editFile.setLog("管理员"+uid+"为用户"+uid+"开通VIP"+day+"天");
            JSONObject response = new JSONObject();
            response.put("code" , rows);
            response.put("msg"  , rows > 0 ? "开通VIP成功" : "操作失败");
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            JSONObject response = new JSONObject();
            response.put("code" , 0);
            response.put("msg"  , "接口请求异常，请联系管理员");
            return response.toString();
        }


    }
    //申请注销
    @RequestMapping(value = "/selfDelete")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String selfDelete(@RequestParam(value = "token", required = false) String  token) {
        try {
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            Integer uid =Integer.parseInt(map.get("uid").toString());
            TypechoUsers user = service.selectByKey(uid);
            if(user==null){
                return Result.getResultJson(0, "用户不存在", null);
            }
            //int rows = service.delete(uid);
            //删除关联的绑定信息
//            TypechoUserapi userapi = new TypechoUserapi();
//            userapi.setUid(uid);
//            Integer isApi = userapiService.total(userapi);
//            if(isApi > 0){
//                userapiService.deleteUserAll(uid);
//            }

            //发送消息通知
            Long date = System.currentTimeMillis();
            String created = String.valueOf(date).substring(0,10);
            TypechoInbox inbox = new TypechoInbox();
            inbox.setUid(uid);
            inbox.setType("selfDelete");
            List<TypechoInbox> list = inboxService.selectList(inbox);
            if(list.size()>0){
                return Result.getResultJson(0, "你已经提交过申请！", null);
            }
            inbox.setTouid(0);
            inbox.setText("申请注销账户");
            inbox.setValue(0);
            inbox.setCreated(Integer.parseInt(created));
            int rows = inboxService.insert(inbox);
            //删除用户登录状态
//            String oldToken = redisHelp.getRedis(this.dataprefix + "_" + "userkey" + user.getName(), redisTemplate);
//            if (oldToken != null) {
//                redisHelp.delete(this.dataprefix + "_" + "userInfo" + oldToken, redisTemplate);
//                redisHelp.delete(this.dataprefix + "_" + "userkey" + user.getName(), redisTemplate);
//            }
            editFile.setLog("用户"+uid+"申请注销账户");

            JSONObject response = new JSONObject();
            response.put("code" ,rows > 0 ? 1: 0 );
            response.put("data" , rows);
            response.put("msg"  , rows > 0 ? "申请成功！" : "操作失败");
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }
    }

    //管理员审核
    @RequestMapping(value = "/selfDeleteOk")
    @ResponseBody
    @LoginRequired(purview = "2")
    public String selfDelete(@RequestParam(value = "token", required = false) String  token,
                             @RequestParam(value = "type", required = false) Integer  type,
                             @RequestParam(value = "uid", required = false) Integer uid) {
        try{

            int rows = 0;
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            Integer logUid =Integer.parseInt(map.get("uid").toString());

            TypechoUsers user = service.selectByKey(uid);
            if(user==null){
                return Result.getResultJson(0, "用户不存在", null);
            }
            Long date = System.currentTimeMillis();
            String created = String.valueOf(date).substring(0,10);
            //修改审核
            TypechoInbox inbox = new TypechoInbox();
            inbox.setUid(uid);
            inbox.setType("selfDelete");
            List<TypechoInbox> list = inboxService.selectList(inbox);
            if(list.size()>0){
                Integer inboxId = list.get(0).getId();
                if(type.equals(0)){
                    //拒接则发消息
                    rows = inboxService.delete(inboxId);
                    TypechoInbox insert = new TypechoInbox();
                    insert.setUid(logUid);
                    insert.setTouid(uid);
                    insert.setType("system");
                    insert.setText("你的注销申请已被拒绝");
                    insert.setCreated(Integer.parseInt(created));
                    inboxService.insert(insert);
                }else{
                    //审核通过就直接删除
                    rows = inboxService.delete(inboxId);
                }

            }else{
                return Result.getResultJson(0, "该用户未提交申请", null);
            }
            if(type.equals(1)){
                //删除关联的绑定信息
                TypechoUserapi userapi = new TypechoUserapi();
                userapi.setUid(uid);
                Integer isApi = userapiService.total(userapi);
                if(isApi > 0){
                    userapiService.deleteUserAll(uid);
                }
                //删除用户登录状态
                String oldToken = redisHelp.getRedis(this.dataprefix + "_" + "userkey" + user.getName(), redisTemplate);
                if (oldToken != null) {
                    redisHelp.delete(this.dataprefix + "_" + "userInfo" + oldToken, redisTemplate);
                    redisHelp.delete(this.dataprefix + "_" + "userkey" + user.getName(), redisTemplate);
                }
                rows = service.delete(uid);
            }
            //操作完了清理列表缓存
            if(rows > 0){
                redisHelp.deleteKeysWithPattern("*"+this.dataprefix+"_selfDeleteList_*",redisTemplate,this.dataprefix);
            }
            editFile.setLog("管理员"+logUid+"审核了申请注销申请。");
            JSONObject response = new JSONObject();
            response.put("code" ,rows > 0 ? 1: 0 );
            response.put("data" , rows);
            response.put("msg"  , rows > 0 ? "操作成功！" : "操作失败");
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }

    }


    //审核列表
    @RequestMapping(value = "/selfDeleteList")
    @ResponseBody
    @LoginRequired(purview = "2")
    public String selfDeleteList (@RequestParam(value = "token", required = false) String  token,
                                  @RequestParam(value = "searchParams", required = false) String  searchParams,
                                 @RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                                 @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit) {

        TypechoInbox query = new TypechoInbox();
        Integer total = 0;
        List jsonList = new ArrayList();
        if (StringUtils.isNotBlank(searchParams)) {
            JSONObject object = JSON.parseObject(searchParams);
            query = object.toJavaObject(TypechoInbox.class);
        }
        query.setType("selfDelete");
        List cacheList = redisHelp.getList(this.dataprefix+"_"+"selfDeleteList_"+page+"_"+limit,redisTemplate);
        total = inboxService.total(query);
        try {
            if (cacheList.size() > 0) {
                jsonList = cacheList;
            } else {
                PageList<TypechoInbox> pageList = inboxService.selectPage(query, page, limit);
                List<TypechoInbox> list = pageList.getList();
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
                    TypechoInbox violation = list.get(i);
                    Integer userid = violation.getUid();
                    //获取用户信息
                    Map userJson = UserStatus.getUserInfo(userid,allconfigService,service);
                    json.put("userJson",userJson);
                    jsonList.add(json);
                }

                redisHelp.delete(this.dataprefix+"_"+"selfDeleteList_"+page+"_"+limit,redisTemplate);
                redisHelp.setList(this.dataprefix+"_"+"selfDeleteList_"+page+"_"+limit,jsonList,30,redisTemplate);
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
     * 生成验证码
     */
    @RequestMapping(value = "/getKaptcha")
    @ResponseBody
    public void getKaptcha(HttpServletRequest request, HttpServletResponse httpServletResponse) throws Exception {
        byte[] captchaOutputStream;
        String  ip = baseFull.getIpAddr(request);
        ByteArrayOutputStream imgOutputStream = new ByteArrayOutputStream();
        try {
            //生产验证码字符串并保存到session中
            String verifyCode = captchaProducer.createText();
            redisHelp.setRedis(this.dataprefix+"_"+ip+"_verifyCode",verifyCode,60,redisTemplate);
            BufferedImage challenge = captchaProducer.createImage(verifyCode);
            ImageIO.write(challenge, "jpg", imgOutputStream);
        } catch (IllegalArgumentException e) {
            httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        captchaOutputStream = imgOutputStream.toByteArray();
        httpServletResponse.setHeader("Cache-Control", "no-store");
        httpServletResponse.setHeader("Pragma", "no-cache");
        httpServletResponse.setDateHeader("Expires", 0);
        httpServletResponse.setContentType("image/jpeg");
        ServletOutputStream responseOutputStream = httpServletResponse.getOutputStream();
        responseOutputStream.write(captchaOutputStream);
        responseOutputStream.flush();
        responseOutputStream.close();
    }

    /***
     * 验证图片验证码
     */
    @RequestMapping("/verifyKaptcha")
    @ResponseBody
    public String verifyKaptcha(@RequestParam(value = "code", required = false) String  code,
                                HttpServletRequest request) {
        String  ip = baseFull.getIpAddr(request);
        if (StringUtils.isEmpty(code)) {
            return Result.getResultJson(0,"验证码不能为空",null);
        }
        String kaptchaCode = redisHelp.getRedis(this.dataprefix+"_"+ip+"_verifyCode",redisTemplate);
        if (StringUtils.isEmpty(kaptchaCode) || !code.equals(kaptchaCode)) {
            return Result.getResultJson(0,"验证码错误",null);
        }
        return Result.getResultJson(1,"验证成功",null);
    }
    /***
     * 获取用户邀请码
     */
    @RequestMapping("/getInvitationCode")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String getInvitationCode(@RequestParam(value = "token", required = false) String  token) {
        try{
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            Integer uid =Integer.parseInt(map.get("uid").toString());
            TypechoUsers user = service.selectByKey(uid);
            if(user==null){
                return Result.getResultJson(0, "用户信息不存在", null);
            }
            String invitationCode = "";
            if(user.getInvitationCode()==null||user.getInvitationCode()==""||user.getInvitationCode().length()<1){
                //生成邀请码字符串
                String key = baseFull.createRandomStr(8);
                TypechoUsers newUser = new TypechoUsers();
                newUser.setUid(uid);
                newUser.setInvitationCode(key);
                service.update(newUser);
                invitationCode = key;
            }else{
                invitationCode = user.getInvitationCode();
            }
            JSONObject response = new JSONObject();

            response.put("code", 1);
            response.put("msg", "");
            response.put("data", invitationCode);
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }
    }

    /***
     * 提交举报信息
     */
    @RequestMapping("/report")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String report(@RequestParam(value = "token", required = false) String  token,
                         @RequestParam(value = "text", required = false) String  text,
                         @RequestParam(value = "verifyCode", required = false) String verifyCode,
                         HttpServletRequest request) {
        try{
            String  ip = baseFull.getIpAddr(request);
            Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
            if(Integer.parseInt(apiconfig.get("verifyLevel").toString())>1) {
                if (StringUtils.isEmpty(verifyCode)) {
                    return Result.getResultJson(0,"图片验证码不能为空",null);
                }
                String kaptchaCode = redisHelp.getRedis(this.dataprefix+"_"+ip+"_verifyCode",redisTemplate);
                if (StringUtils.isEmpty(kaptchaCode) || !verifyCode.equals(kaptchaCode)) {
                    return Result.getResultJson(0,"图片验证码错误",null);
                }
            }
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            Integer uid =Integer.parseInt(map.get("uid").toString());
            Long date = System.currentTimeMillis();
            //发送消息通知
            String created = String.valueOf(date).substring(0,10);
            TypechoInbox inbox = new TypechoInbox();
            inbox.setUid(uid);
            inbox.setTouid(0);
            inbox.setType("report");
            inbox.setText(text);
            //-1代表被拒绝， 0代表未处理，1代表已处理
            inbox.setValue(0);
            inbox.setCreated(Integer.parseInt(created));
            int rows = inboxService.insert(inbox);
            if(rows>1){
                redisHelp.deleteKeysWithPattern("*"+this.dataprefix+"_reportList_*",redisTemplate,this.dataprefix);
            }
            editFile.setLog("用户"+uid+"提交了举报信息。");
            JSONObject response = new JSONObject();
            response.put("code" ,rows > 0 ? 1: 0 );
            response.put("data" , rows);
            response.put("msg"  , rows > 0 ? "操作成功！" : "操作失败");
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }

    }

    /***
     * 举报列表
     */
    @RequestMapping("/reportList")
    @ResponseBody
    @LoginRequired(purview = "1")
    public String reportList (@RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                              @RequestParam(value = "type", required = false,defaultValue = "1") Integer type,
                         @RequestParam(value = "limit", required = false, defaultValue = "15") Integer limit) {
        if(limit>50){
            limit = 50;
        }
        TypechoInbox query = new TypechoInbox();
        query.setValue(type);
        query.setType("report");
        List jsonList = new ArrayList();
        List cacheList = redisHelp.getList(this.dataprefix+"_"+"reportList_"+type+"_"+page+"_"+limit,redisTemplate);
        Integer total = inboxService.total(query);
        try{
            if(cacheList.size()>0){
                jsonList = cacheList;
            }else{
                PageList<TypechoInbox> pageList = inboxService.selectPage(query, page, limit);
                List<TypechoInbox> list = pageList.getList();
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
                    TypechoInbox inbox = list.get(i);
                    Integer userid = inbox.getUid();
                    Map userJson = UserStatus.getUserInfo(userid,allconfigService,service);
                    json.put("userJson",userJson);
                    jsonList.add(json);
                }
                redisHelp.delete(this.dataprefix+"_"+"reportList_"+type+"_"+page+"_"+limit,redisTemplate);
                redisHelp.setList(this.dataprefix+"_"+"reportList_"+type+"_"+page+"_"+limit,jsonList,60,redisTemplate);
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
     * 举报审核
     */
    @RequestMapping("/reportAudit")
    @ResponseBody
    @LoginRequired(purview = "1")
    public String reportAudit (@RequestParam(value = "id", required = false) Integer id,
                               @RequestParam(value = "token", required = false) String  token,
                               @RequestParam(value = "type", required = false,defaultValue = "1") Integer type,
                               @RequestParam(value = "remarks", required = false) String remarks) {

        try{
            if(!type.equals(-1)&&!type.equals(1)){
                return Result.getResultJson(0,"参数不正确",null);
            }
            TypechoInbox inbox = inboxService.selectByKey(id);
            if(inbox==null){
                return Result.getResultJson(0,"举报信息不存在",null);
            }
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            Integer uid =Integer.parseInt(map.get("uid").toString());
            TypechoInbox auditQuery = new TypechoInbox();
            auditQuery.setId(id);
            auditQuery.setValue(type);
            int rows = inboxService.update(auditQuery);
            if(rows > 0){
                redisHelp.deleteKeysWithPattern("*"+this.dataprefix+"_reportList_*",redisTemplate,this.dataprefix);
                //给用户发送消息
                Long date = System.currentTimeMillis();
                String created = String.valueOf(date).substring(0,10);
                TypechoInbox insert = new TypechoInbox();
                insert.setUid(uid);
                insert.setTouid(inbox.getUid());
                insert.setType("system");
                if(type.equals(-1)){
                    insert.setText("您的举报内容经审核后状态未不通过，理由为："+remarks);
                    insert.setCreated(Integer.parseInt(created));

                }else{
                    insert.setText("您的举报内容审核通过。");
                    insert.setCreated(Integer.parseInt(created));
                }
                inboxService.insert(insert);
                return Result.getResultJson(1,"操作成功",null);
            }else{
                return Result.getResultJson(0,"操作失败",null);
            }
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }
    }
}