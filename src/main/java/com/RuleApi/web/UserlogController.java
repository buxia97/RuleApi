package com.RuleApi.web;

import com.RuleApi.annotation.LoginRequired;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 控制层
 * TypechoUserlogController
 * 同时负责用户的收藏，点赞，打赏和签到
 * @author buxia97
 * @date 2022/01/06
 */
@Controller
@RequestMapping(value = "/typechoUserlog")
public class UserlogController {

    @Autowired
    UserlogService service;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RelationshipsService relationshipsService;

    @Autowired
    private MetasService metasService;

    @Autowired
    private ContentsService contentsService;

    @Autowired
    private ShopService shopService;

    @Autowired
    private FieldsService fieldsService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private AppService appService;

    @Autowired
    private UsersService usersService;

    @Autowired
    private PaylogService paylogService;

    @Autowired
    private AllconfigService allconfigService;

    @Autowired
    private InboxService inboxService;

    @Autowired
    private SpaceService spaceService;

    @Autowired
    private PushService pushService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${web.prefix}")
    private String dataprefix;

    @Value("${mybatis.configuration.variables.prefix}")
    private String prefix;

    RedisHelp redisHelp =new RedisHelp();
    ResultAll Result = new ResultAll();
    HttpClient HttpClient = new HttpClient();
    UserStatus UStatus = new UserStatus();

    baseFull baseFull = new baseFull();
    /***
     * 查询用户是否收藏
     */
    @RequestMapping(value = "/isMark")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String isMark (@RequestParam(value = "cid", required = false,defaultValue = "0") String  cid,
                          @RequestParam(value = "type", required = false,defaultValue = "content") String  type,
                          @RequestParam(value = "token", required = false) String  token) {
        //只允许收藏文章、动态、帖子
        if(!type.equals("content")&&!type.equals("post")&&!type.equals("space")&&!type.equals("shop")){
            return Result.getResultJson(0,"参数错误",null);
        }
        String logType = "mark";
        if(type.equals("content")){
            TypechoContents contents = contentsService.selectByKey(cid);
            if(contents==null){
                return Result.getResultJson(0,"文章不存在",null);
            }
            logType = "mark";
        }
        if(type.equals("space")){
            TypechoSpace space = spaceService.selectByKey(cid);
            if(space==null){
                return Result.getResultJson(0,"动态不存在",null);
            }
            logType = "markSpace";
        }
        if(type.equals("shop")){
            TypechoShop shop = shopService.selectByKey(cid);
            if(shop==null){
                return Result.getResultJson(0,"商品不存在",null);
            }
            logType = "markShop";
        }
        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid =Integer.parseInt(map.get("uid").toString());
        TypechoUserlog userlog = new TypechoUserlog();
        userlog.setCid(Integer.parseInt(cid));
        userlog.setUid(uid);
        userlog.setType(logType);
        Integer isMark = service.total(userlog);
        Integer logid = -1;
        if(isMark>0){
            List<TypechoUserlog> loglist = service.selectList(userlog);
            logid = loglist.get(0).getId();
        }
        Map json = new HashMap();
        json.put("isMark",isMark);
        json.put("logid",logid);
        JSONObject response = new JSONObject();
        response.put("code" , 1);
        response.put("msg"  , "");
        response.put("data" , json);
        return response.toString();
    }
    /***
     * 查询用户收藏列表，文章、帖子、动态、商品
     */
    @RequestMapping(value = "/markList")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String markList (@RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                            @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit,
                            @RequestParam(value = "token", required = false) String  token) {

        if(limit>50){
            limit = 50;
        }
        Integer total = 0;
        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid =Integer.parseInt(map.get("uid").toString());

        TypechoUserlog query = new TypechoUserlog();
        query.setUid(uid);
        query.setType("mark");
        total = service.total(query);
        Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);

        List jsonList = new ArrayList();
        List cacheList = redisHelp.getList(this.dataprefix+"_"+"markList_"+page+"_"+limit+"_"+uid,redisTemplate);
        try{
            if(cacheList.size()>0){
                jsonList = cacheList;
            }else {
                PageList<TypechoUserlog> pageList = service.selectPage(query, page, limit);
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
                    Integer cid = list.get(i).getCid();

                    TypechoContents typechoContents = contentsService.selectByKey(cid);
                    Map json = baseFull.getContentListInfo(typechoContents,fieldsService,relationshipsService,metasService,usersService,apiconfig);
                    if(json!=null){
                        jsonList.add(json);
                    }
                }
                redisHelp.delete(this.dataprefix+"_"+"markList_"+page+"_"+limit+"_"+uid, redisTemplate);
                redisHelp.setList(this.dataprefix+"_"+"markList_"+page+"_"+limit+"_"+uid, jsonList, 5, redisTemplate);
            }
        }catch (Exception e){
            if(cacheList.size()>0){
                jsonList = cacheList;
            }
        }
        JSONObject response = new JSONObject();
        response.put("code" , 1);
        response.put("msg"  , "");
        response.put("data" , null != jsonList ? jsonList : new JSONArray());
        response.put("count", jsonList.size());
        response.put("total", total);
        return response.toString();
    }
    /***
     * 查询用户打赏历史
     * @param page         页码
     * @param limit        每页显示数量
     */
    @RequestMapping(value = "/rewardList")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String rewardList (@RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                            @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit,
                            @RequestParam(value = "token", required = false) String  token) {
        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid =Integer.parseInt(map.get("uid").toString());
        if(limit>50){
            limit = 50;
        }
        Integer total = 0;
        TypechoUserlog query = new TypechoUserlog();
        query.setUid(uid);
        query.setType("reward");
        total = service.total(query);
        PageList<TypechoUserlog> pageList = service.selectPage(query, page, limit);
        JSONObject response = new JSONObject();
        response.put("code" , 1);
        response.put("msg"  , "");
        response.put("data" , null != pageList.getList() ? pageList.getList() : new JSONArray());
        response.put("count", pageList.getList());
        response.put("total", total);
        return response.toString();
    }
    /***
     * 添加log
     * @param params Bean对象JSON字符串
     */
    @RequestMapping(value = "/addLog")
    @ResponseBody
    public String addLog(@RequestParam(value = "params", required = false) String  params,@RequestParam(value = "token", required = false) String  token,HttpServletRequest request) {
        try {
            Map jsonToMap =null;
            TypechoUserlog insert = null;
            String  agent =  request.getHeader("User-Agent");
            String  ip = baseFull.getIpAddr(request);

            //生成随机积分
            Random r = new Random();

            String clock = "";
            String type = "";
            JSONObject clockData = new JSONObject();
            if (StringUtils.isNotBlank(params)) {


                jsonToMap =  JSONObject.parseObject(JSON.parseObject(params).toString());

                //生成typecho数据库格式的修改时间戳
                Long date = System.currentTimeMillis();
                String userTime = String.valueOf(date).substring(0,10);
                jsonToMap.put("created",userTime);
                type = jsonToMap.get("type").toString();
                //只有喜欢操作不需要登陆拦截
                if(!type.equals("likes")){
                    Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
                    if(uStatus==0){
                        return Result.getResultJson(0,"请先登录哦",null);
                    }
                    String isRepeated = redisHelp.getRedis(token+"_isRepeated",redisTemplate);
                    if(isRepeated==null){
                        redisHelp.setRedis(token+"_isRepeated","1",5,redisTemplate);
                    }else{
                        return Result.getResultJson(0,"你的操作太频繁了",null);
                    }
                }
                Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
                Integer uid = 0;
                if(map.get("uid")!=null){
                    uid =Integer.parseInt(map.get("uid").toString());
                    jsonToMap.put("uid",uid);
                }


                //mark为收藏，reward为打赏，likes为奖励，clock为签到
                if(!type.equals("mark")&&!type.equals("reward")&&!type.equals("likes")&&!type.equals("clock")){
                    return Result.getResultJson(0,"错误的字段类型",null);
                }
                //如果是点赞，那么每天只能一次
                if(type.equals("likes")){
                    String cid = jsonToMap.get("cid").toString();
                    String isLikes = redisHelp.getRedis(this.dataprefix+"_"+"userlikes"+"_"+ip+"_"+agent+"_"+cid,redisTemplate);
                    if(isLikes!=null){
                        return Result.getResultJson(0,"距离上次操作不到24小时！",null);
                    }
                    //添加点赞量
                    TypechoContents contensjson = contentsService.selectByKey(cid);
                    Integer likes = contensjson.getLikes();
                    likes = likes + 1;
                    TypechoContents toContents = new TypechoContents();
                    toContents.setCid(Integer.parseInt(cid));
                    toContents.setLikes(likes);
                    contentsService.update(toContents);

                    redisHelp.setRedis(this.dataprefix+"_"+"userlikes"+"_"+ip+"_"+agent+"_"+cid,"yes",86400,redisTemplate);
                }
                //签到，每天一次
                if(type.equals("clock")){
                    Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
                    Integer clockMax = 1;
                    if(apiconfig.get("clock")!=null){
                        clockMax = Integer.parseInt(apiconfig.get("clock").toString());
                    }
                    int award = 0;
                    if (clockMax > 0){
                        award = r.nextInt(clockMax) + 1;
                    }
                    Integer addExp = 0;
                    if(apiconfig.get("clockExp")!=null){
                        addExp = Integer.parseInt(apiconfig.get("clockExp").toString());
                    }
                    Integer clockPoints = 0;
                    if(apiconfig.get("clockPoints")!=null){
                        clockPoints = Integer.parseInt(apiconfig.get("clockPoints").toString());
                    }
                    TypechoUserlog log = new TypechoUserlog();
                    log.setType("clock");
                    log.setUid(uid);

                    List<TypechoUserlog> info = service.selectList(log);

                    //获取上次时间
                    if (info.size()>0){
                        Integer time = info.get(0).getCreated();
                        String oldStamp = time+"000";
                        SimpleDateFormat sdf=new SimpleDateFormat("yyyyMMdd");
                        String oldtime = sdf.format(new Date(Long.parseLong(oldStamp)));
                        Integer old = Integer.parseInt(oldtime);
                        //获取本次时间
                        Long curStamp = System.currentTimeMillis();  //获取当前时间戳
                        String curtime = sdf.format(new Date(Long.parseLong(String.valueOf(curStamp))));
                        Integer cur = Integer.parseInt(curtime);
                        if(old>=cur){
                            return Result.getResultJson(0,"你已经签到过了哦",null);
                        }
                    }


                    TypechoUsers user = usersService.selectByKey(uid);
                    TypechoUsers newUser = new TypechoUsers();
                    newUser.setUid(uid);
                    if(award > 0){
                        Integer account = user.getAssets();
                        Integer Assets = account + award;
                        newUser.setAssets(Assets);
                    }
                    Integer oldExperience = 0;
                    if(user.getExperience()!=null){
                        oldExperience = user.getExperience();
                    }
                    Integer newExperience = oldExperience + addExp;
                    Integer oldPoints = 0;
                    if(user.getPoints()!=null){
                        oldPoints = user.getPoints();
                    }
                    Integer newPoints = oldPoints +  clockPoints;
                    newUser.setExperience(newExperience);
                    newUser.setPoints(newPoints);
                    usersService.update(newUser);
                    jsonToMap.put("num",award);
                    //clock = "，获得"+award+"积分奖励！";
                    clockData.put("award" , award);
                    clockData.put("addExp" , addExp);
                    clockData.put("clockPoints" , clockPoints);


                    //生成签到收益日志
                    TypechoPaylog paylog = new TypechoPaylog();
                    paylog.setStatus(1);
                    paylog.setCreated(Integer.parseInt(userTime));
                    paylog.setUid(uid);
                    paylog.setOutTradeNo(userTime+"clock");
                    paylog.setTotalAmount(award+"");
                    paylog.setPaytype("clock");
                    paylog.setSubject("签到奖励");
                    paylogService.insert(paylog);

                    jsonToMap.put("toid",uid);

                    redisHelp.delete(this.dataprefix+"_"+"userData_"+uid,redisTemplate);
                }
                //收藏，只能一次
                if(type.equals("mark")){
                    if(jsonToMap.get("cid")==null){
                        return Result.getResultJson(0,"参数不正确",null);
                    }
                    Integer cid = Integer.parseInt(jsonToMap.get("cid").toString());
                    TypechoUserlog log = new TypechoUserlog();
                    log.setType("mark");
                    log.setUid(uid);
                    log.setCid(cid);
                    List<TypechoUserlog> info = service.selectList(log);
                    if(info.size()>0){
                        return Result.getResultJson(0,"已在你的收藏中！",null);
                    }
                }
                //打赏，要扣余额
                if(type.equals("reward")){

                    if(jsonToMap.get("num")==null){
                        return Result.getResultJson(0,"参数不正确",null);
                    }
                    Integer num = Integer.parseInt(jsonToMap.get("num").toString());
                    if(num<=0){
                        return Result.getResultJson(0,"参数不正确",null);
                    }
                    TypechoUsers user = usersService.selectByKey(uid);
                    Integer account = user.getAssets();
                    if(num>account){
                        return Result.getResultJson(0,"积分不足！",null);
                    }
                    Integer Assets = account - num;

                    //获取作者信息
                    Integer cid = Integer.parseInt(jsonToMap.get("cid").toString());
                    TypechoContents curContents = contentsService.selectByKey(cid);
                    Integer authorid = curContents.getAuthorId();
                    //生成打赏者资产日志（如果是自己打赏自己，就不生成）
                    if(!uid.equals(authorid)){
                        TypechoPaylog paylog = new TypechoPaylog();
                        paylog.setStatus(1);
                        paylog.setCreated(Integer.parseInt(userTime));
                        paylog.setUid(uid);
                        paylog.setOutTradeNo(userTime+"toReward");
                        paylog.setTotalAmount("-"+num);
                        paylog.setPaytype("toReward");
                        paylog.setSubject("打赏作品");
                        paylogService.insert(paylog);
                    }else{
                        return Result.getResultJson(0,"你不可以打赏自己的作品！",null);
                    }
                    //扣除自己的积分
                    TypechoUsers newUser = new TypechoUsers();
                    newUser.setUid(uid);
                    newUser.setAssets(Assets);
                    usersService.update(newUser);

                    //给文章的作者增加积分

                    TypechoUsers toUser = usersService.selectByKey(authorid);
                    Integer toAssets = toUser.getAssets();
                    Integer curAssets = toAssets + num;
                    toUser.setAssets(curAssets);
                    usersService.update(toUser);

                    jsonToMap.put("toid",authorid);

                    if(!uid.equals(authorid)) {
                        //生成作者资产日志
                        TypechoPaylog paylogB = new TypechoPaylog();
                        paylogB.setStatus(1);
                        paylogB.setCreated(Integer.parseInt(userTime));
                        paylogB.setUid(authorid);
                        paylogB.setOutTradeNo(userTime + "reward");
                        paylogB.setTotalAmount(num.toString());
                        paylogB.setPaytype("reward");
                        paylogB.setSubject("来自用户ID" + uid + "打赏");
                        paylogService.insert(paylogB);
                        //发送消息通知
                        String created = String.valueOf(date).substring(0,10);
                        TypechoInbox inbox = new TypechoInbox();
                        inbox.setUid(uid);
                        inbox.setTouid(authorid);
                        inbox.setType("finance");
                        inbox.setText("打赏了你的文章【"+curContents.getTitle()+"】");
                        inbox.setValue(curContents.getCid());
                        inbox.setCreated(Integer.parseInt(created));
                        inboxService.insert(inbox);
                    }

                }
                insert = JSON.parseObject(JSON.toJSONString(jsonToMap), TypechoUserlog.class);
            }

            int rows = service.insert(insert);

            JSONObject response = new JSONObject();
            response.put("code" , rows);
            if(type.equals("clock")){
                response.put("clockData" , clockData);

            }
            response.put("msg"  , rows > 0 ? "操作成功"+clock : "操作失败");
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }

    }

    /***
     * 表单删除
     */
    @RequestMapping(value = "/removeLog")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String removeLog(@RequestParam(value = "key", required = false) String  key,@RequestParam(value = "token", required = false) String  token) {
        //验证用户权限
        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid =Integer.parseInt(map.get("uid").toString());
        String group = map.get("group").toString();


        TypechoUserlog info = service.selectByKey(key);
        Integer userId = info.getUid();
        String type = info.getType();
        if(!group.equals("administrator")){
            if(!userId.equals(uid)){
                return Result.getResultJson(0,"你无权进行此操作",null);
            }
            if(!type.equals("mark")){
                return Result.getResultJson(0,"该类型数据不允许删除",null);
            }
        }


        Integer rows =  service.delete(key);
        JSONObject response = new JSONObject();
        response.put("code" , rows);
        response.put("msg"  , rows > 0 ? "操作成功" : "操作失败");
        return response.toString();

    }
    /***
     * 查询用户购买订单
     */
    @RequestMapping(value = "/orderList")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String orderList (@RequestParam(value = "token", required = false) String  token) {

        String page = "1";
        String limit = "60";
        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid =Integer.parseInt(map.get("uid").toString());
        Integer total = 0;
        TypechoUserlog query = new TypechoUserlog();
        query.setUid(uid);
        query.setType("buy");
        total = service.total(query);
        List jsonList = new ArrayList();
        List cacheList = redisHelp.getList(this.dataprefix+"_"+"orderList_"+page+"_"+limit+"_"+uid,redisTemplate);
        try{
            if(cacheList.size()>0){
                jsonList = cacheList;
            }else {
                PageList<TypechoUserlog> pageList = service.selectPage(query, Integer.parseInt(page), Integer.parseInt(limit));
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
                    Integer cid = list.get(i).getCid();
                    Map json = JSONObject.parseObject(JSONObject.toJSONString(list.get(i)), Map.class);
                    //这里cid是商品id
                    TypechoShop shop = shopService.selectByKey(cid);
                    if(shop!=null){
                        Map shopInfo = JSONObject.parseObject(JSONObject.toJSONString(shop), Map.class);
                        json.put("shopInfo",shopInfo);
                    }


                    //获取商家邮箱
                    Integer merid = shop.getUid();
                    TypechoUsers merchant = usersService.selectByKey(merid);
                    String merchantEmail = merchant.getMail();
                    if(merchantEmail==null){
                        json.put("merchantEmail",null);
                    }else{
                        json.put("merchantEmail",merchantEmail);
                    }



                    jsonList.add(json);


                }
                redisHelp.delete(this.dataprefix+"_"+"orderList_"+page+"_"+limit+"_"+uid, redisTemplate);
                redisHelp.setList(this.dataprefix+"_"+"orderList_"+page+"_"+limit+"_"+uid, jsonList, 5, redisTemplate);
            }
        }catch (Exception e){
            if(cacheList.size()>0){
                jsonList = cacheList;
            }
        }
        JSONObject response = new JSONObject();
        response.put("code" , 1);
        response.put("msg"  , "");
        response.put("data" , null != jsonList ? jsonList : new JSONArray());
        response.put("count", jsonList.size());
        response.put("total", total);
        return response.toString();
    }
    /***
     * 查询售出订单列表
     */
    @RequestMapping(value = "/orderSellList")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String orderSellList (@RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                                 @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit,
                                 @RequestParam(value = "token", required = false) String  token) {
        if(limit>50){
            limit = 50;
        }
        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid =Integer.parseInt(map.get("uid").toString());
        Integer total = 0;
        TypechoUserlog query = new TypechoUserlog();
        query.setToid(uid);
        query.setType("buy");
        total = service.total(query);
        List jsonList = new ArrayList();
        List cacheList = redisHelp.getList(this.dataprefix+"_"+"orderSellList_"+page+"_"+limit+"_"+uid,redisTemplate);
        try{
            if(cacheList.size()>0){
                jsonList = cacheList;
            }else {
                PageList<TypechoUserlog> pageList = service.selectPage(query, page, limit);
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
                    Integer cid = list.get(i).getCid();
                    Integer touid = list.get(i).getUid();
                    Map json = JSONObject.parseObject(JSONObject.toJSONString(list.get(i)), Map.class);
                    //这里cid是商品id
                    TypechoShop shop = shopService.selectByKey(cid);
                    if(shop!=null){
                        Map shopInfo = JSONObject.parseObject(JSONObject.toJSONString(shop), Map.class);

                        json.put("shopInfo",shopInfo);
                    }

                    //获取用户地址

                    TypechoUsers user = usersService.selectByKey(touid);
                    String address = user.getAddress();
                    String userEmail = user.getMail();
                    if(address==null){
                        json.put("address",null);
                    }else{
                        json.put("address",address);
                    }
                    if(userEmail==null){
                        json.put("userEmail",null);
                    }else{
                        json.put("userEmail",userEmail);
                    }
                    jsonList.add(json);


                }
                redisHelp.delete(this.dataprefix+"_"+"orderSellList_"+page+"_"+limit+"_"+uid, redisTemplate);
                redisHelp.setList(this.dataprefix+"_"+"orderSellList_"+page+"_"+limit+"_"+uid, jsonList, 5, redisTemplate);

            }
        }catch (Exception e){
            if(cacheList.size()>0){
                jsonList = cacheList;
            }
        }
        JSONObject response = new JSONObject();
        response.put("code" , 1);
        response.put("msg"  , "");
        response.put("data" , null != jsonList ? jsonList : new JSONArray());
        response.put("count", jsonList.size());
        response.put("total", total);
        return response.toString();
    }
    /***
     * 发起广告
     */
    @RequestMapping(value = "/adsGift")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String adsGift(@RequestParam(value = "token", required = false) String  token,
                          @RequestParam(value = "appkey", required = false) String  appkey) {
        try{
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            Integer uid =Integer.parseInt(map.get("uid").toString());
            Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
            if(apiconfig.get("banRobots").toString().equals("1")) {
                //登录情况下，刷数据攻击拦截
                String isSilence = redisHelp.getRedis(this.dataprefix+"_"+uid+"_silence",redisTemplate);
                if(isSilence!=null){
                    return Result.getResultJson(0,"你已被禁言，请耐心等待",null);
                }
                String isRepeated = redisHelp.getRedis(this.dataprefix+"_"+uid+"_isRepeated",redisTemplate);
                if(isRepeated==null){
                    redisHelp.setRedis(this.dataprefix+"_"+uid+"_isRepeated","1",3,redisTemplate);
                }else{
                    Integer frequency = Integer.parseInt(isRepeated) + 1;
                    if(frequency==3){
                        securityService.safetyMessage("用户ID："+uid+"，在激励视频奖励接口疑似存在攻击行为，请及时确认处理。","system");
                        redisHelp.setRedis(this.dataprefix+"_"+uid+"_silence","1",Integer.parseInt(apiconfig.get("silenceTime").toString()),redisTemplate);
                        return Result.getResultJson(0,"你的请求存在恶意行为，已暂时禁止操作！",null);
                    }else{
                        redisHelp.setRedis(this.dataprefix+"_"+uid+"_isRepeated",frequency.toString(),3,redisTemplate);
                    }
                    return Result.getResultJson(0,"你的操作太频繁了",null);
                }
            }
            //攻击拦截结束
            String regISsendCode = redisHelp.getRedis(this.dataprefix + "_" + "adsGift_"+uid, redisTemplate);
            if(regISsendCode==null){
                redisHelp.setRedis(this.dataprefix + "_" + "adsGift_"+uid, "data", 20, redisTemplate);
            }else{
                return Result.getResultJson(0, "不要恶意跳过激励视频哦！", null);
            }
            TypechoApp app = appService.selectByKey(appkey);
            if(app==null){
                return Result.getResultJson(0,"应用不存在或密钥错误",null);
            }
            //获取今日已发起广告
            Integer adsNum = Integer.parseInt(apiconfig.get("adsGiftNum").toString());

            System.out.println("SELECT COUNT(*) FROM `"+prefix+"_userlog` WHERE type = 'adsGift' and uid = "+uid+" and DATE(FROM_UNIXTIME(created)) = CURDATE();");
            Integer oldAdsNum = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `"+prefix+"_userlog` WHERE type = 'adsGift' and uid = "+uid+" and DATE(FROM_UNIXTIME(created)) = CURDATE();", Integer.class);
            if(oldAdsNum >=  adsNum){
                return Result.getResultJson(0,"今日奖励获取次数已用完",null);
            }
            //增加广告日志
            Long date = System.currentTimeMillis();
            String userTime = String.valueOf(date).substring(0,10);
            TypechoUserlog log = new TypechoUserlog();
            log.setType("adsGift");
            log.setUid(uid);
            log.setCreated(Integer.parseInt(userTime));
            //cid用于状态，没有回调过就是0
            log.setCid(0);
            service.insert(log);
            Integer logid = log.getId();

            Map json = new HashMap<String, String>();
            json.put("adpid", app.getAdpid());
            json.put("logid", logid);
            JSONObject response = new JSONObject();
            response.put("code", 1);
            response.put("msg", "");
            response.put("data", json);
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }


    }
    /***
     * 广告回调(旧前端回调)
     */
    @RequestMapping(value = "/adsGiftNotify")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String adsGiftNotify(@RequestParam(value = "token", required = false) String  token,
                                @RequestParam(value = "logid", required = false) String  logid) {
        try{
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            Integer uid =Integer.parseInt(map.get("uid").toString());
            Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
            Integer adsVideoType = Integer.parseInt(apiconfig.get("adsVideoType").toString());
            if(!adsVideoType.equals(0)){
                System.out.println("未开启该回调渠道！");
                return Result.getResultJson(0,"未开启该回调渠道！",null);
            }
            if(apiconfig.get("banRobots").toString().equals("1")) {
                //登录情况下，刷数据攻击拦截
                String isSilence = redisHelp.getRedis(this.dataprefix+"_"+uid+"_silence",redisTemplate);
                if(isSilence!=null){
                    return Result.getResultJson(0,"你已被禁言，请耐心等待",null);
                }
                String isRepeated = redisHelp.getRedis(this.dataprefix+"_"+uid+"_isRepeated",redisTemplate);
                if(isRepeated==null){
                    redisHelp.setRedis(this.dataprefix+"_"+uid+"_isRepeated","1",3,redisTemplate);
                }else{
                    Integer frequency = Integer.parseInt(isRepeated) + 1;
                    if(frequency==3){
                        securityService.safetyMessage("用户ID："+uid+"，在激励视频回调疑似存在攻击行为，请及时确认处理。","system");
                        redisHelp.setRedis(this.dataprefix+"_"+uid+"_silence","1",Integer.parseInt(apiconfig.get("silenceTime").toString()),redisTemplate);
                        return Result.getResultJson(0,"你的请求存在恶意行为，已暂时禁止操作！",null);
                    }else{
                        redisHelp.setRedis(this.dataprefix+"_"+uid+"_isRepeated",frequency.toString(),3,redisTemplate);
                    }
                    return Result.getResultJson(0,"你的操作太频繁了",null);
                }
            }
            //攻击拦截结束
            TypechoUserlog log = service.selectByKey(logid);
            if(log==null){
                return Result.getResultJson(0,"请先发起激励视频",null);
            }
            if(!log.getCid().equals(0)){
                return Result.getResultJson(0,"不要重复请求回调",null);
            }
            //修改状态
            TypechoUserlog newLog = new TypechoUserlog();
            newLog.setId(log.getId());
            newLog.setCid(1);
            service.update(newLog);
            //加积分
            Random r = new Random();
            Integer award = Integer.parseInt(apiconfig.get("adsGiftAward").toString());

            TypechoUsers user = usersService.selectByKey(uid);
            TypechoUsers newUser = new TypechoUsers();
            newUser.setUid(uid);
            if(award > 0){
                Integer account = user.getAssets();
                Integer Assets = account + award;
                newUser.setAssets(Assets);
            }
            usersService.update(newUser);
            Long date = System.currentTimeMillis();
            String userTime = String.valueOf(date).substring(0,10);
            TypechoPaylog paylog = new TypechoPaylog();
            paylog.setStatus(1);
            paylog.setCreated(Integer.parseInt(userTime));
            paylog.setUid(uid);
            paylog.setOutTradeNo(userTime+"adsGift");
            paylog.setTotalAmount(award.toString());
            paylog.setPaytype("adsGift");
            paylog.setSubject("广告奖励");
            paylogService.insert(paylog);
            Map json = new HashMap<String, String>();
            json.put("logid", logid);
            json.put("award", award);
            JSONObject response = new JSONObject();
            response.put("code", 1);
            response.put("msg", "");
            response.put("data", json);
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }
    }

    /***
     * 广告回调（服务端模式）
     */
    @RequestMapping(value = "/adsServerNotify")
    @ResponseBody
    @LoginRequired(purview = "-1")
    public String adsServerNotify(@RequestParam(value = "adpid", required = false) String  adpid,
                                @RequestParam(value = "provider", required = false) String  provider,
                                  @RequestParam(value = "sign", required = false) String  sign,
                                  @RequestParam(value = "trans_id", required = false) String  trans_id,
                                  @RequestParam(value = "user_id", required = false) String  user_id,
                                  @RequestParam(value = "extra", required = false) String  extra) {
        try{

            Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
            Integer adsVideoType = Integer.parseInt(apiconfig.get("adsVideoType").toString());

            if(!adsVideoType.equals(1)){
                System.out.println("未开启该回调渠道！");
                JSONObject response = new JSONObject();
                response.put("isValid", false);
                return response.toString();
            }
            String adsSecuritykey = apiconfig.get("adsSecuritykey").toString();
            String curSign = baseFull.getSHA256StrJava(adsSecuritykey+":"+trans_id);
            if(!curSign.equals(sign)){
                System.out.println("ads签名校验失败，无法发放奖励！");
                JSONObject response = new JSONObject();
                response.put("isValid", false);
                return response.toString();
            }
            //获取今日已发起广告
            Integer adsNum = Integer.parseInt(apiconfig.get("adsGiftNum").toString());
            System.out.println("SELECT COUNT(*) FROM `"+prefix+"_userlog` WHERE type = 'adsGift' and uid = "+user_id+" and DATE(FROM_UNIXTIME(created)) = CURDATE();");
            Integer oldAdsNum = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `"+prefix+"_userlog` WHERE type = 'adsGift' and uid = "+user_id+" and DATE(FROM_UNIXTIME(created)) = CURDATE();", Integer.class);
            if(oldAdsNum >=  adsNum){
                return Result.getResultJson(0,"今日奖励获取次数已用完",null);
            }
            //加积分
            Random r = new Random();
            Integer award = Integer.parseInt(apiconfig.get("adsGiftAward").toString());
            TypechoUsers user = usersService.selectByKey(user_id);
            TypechoUsers newUser = new TypechoUsers();
            newUser.setUid(Integer.parseInt(user_id));
            if(award > 0){
                Integer account = user.getAssets();
                Integer Assets = account + award;
                newUser.setAssets(Assets);
            }
            usersService.update(newUser);
            Long date = System.currentTimeMillis();
            String userTime = String.valueOf(date).substring(0,10);
            TypechoPaylog paylog = new TypechoPaylog();
            paylog.setStatus(1);
            paylog.setCreated(Integer.parseInt(userTime));
            paylog.setUid(Integer.parseInt(user_id));
            paylog.setOutTradeNo(userTime+"adsGift");
            paylog.setTotalAmount(award.toString());
            paylog.setPaytype("adsGift");
            paylog.setSubject("广告奖励");
            paylogService.insert(paylog);
            JSONObject response = new JSONObject();
            response.put("isValid", true);
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            JSONObject response = new JSONObject();
            response.put("isValid", false);
            return response.toString();
        }
    }
    /***
     * 查询商品是否已经购买过
     */
    @RequestMapping(value = "/dataClean")
    @ResponseBody
    @LoginRequired(purview = "2")
    public String dataClean(@RequestParam(value = "clean", required = false) Integer  clean,
                            @RequestParam(value = "token", required = false) String  token) {
        try {
            //1是清理用户签到，2是清理用户资产日志，3是清理用户订单数据，4是清理无效卡密
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            String group = map.get("group").toString();
            if (!group.equals("administrator")) {
                return Result.getResultJson(0, "你没有操作权限", null);
            }
            Long date = System.currentTimeMillis();
            String curTime = String.valueOf(date).substring(0,10);
            Integer cleanTime = Integer.parseInt(curTime) - 2592000;

            Integer cleanUserTime = Integer.parseInt(curTime) - 31556926;
            //用户签到清理
            if(clean.equals(1)){
                jdbcTemplate.execute("DELETE FROM "+this.prefix+"_userlog WHERE type='clock' and  created < "+cleanTime+";");
            }
            //用户资产记录清理
            if(clean.equals(2)){
                jdbcTemplate.execute("DELETE FROM "+this.prefix+"_paylog WHERE created < "+cleanTime+";");
            }
            //用户订单清理
            if(clean.equals(3)){
                jdbcTemplate.execute("DELETE FROM "+this.prefix+"_userlog WHERE type='buy' and created < "+cleanTime+";");
            }
            //充值码清理
            if(clean.equals(4)){
                jdbcTemplate.execute("DELETE FROM "+this.prefix+"_paykey WHERE status=1 ;");
            }
            //邀请码清理
            if(clean.equals(5)){
                jdbcTemplate.execute("DELETE FROM "+this.prefix+"_invitation WHERE status=1 ;");
            }
            //不活跃用户清理
            if(clean.equals(6)){
                jdbcTemplate.execute("DELETE FROM "+this.prefix+"_user WHERE activated < "+cleanUserTime+";");
            }
            //未支付订单清理
            if(clean.equals(7)){
                jdbcTemplate.execute("DELETE FROM "+this.prefix+"_paylog WHERE status=0;");
            }
            //广告发起日志清理
            if(clean.equals(8)){
                jdbcTemplate.execute("DELETE FROM "+this.prefix+"_userlog WHERE type='adsGift' and created < "+cleanTime+";");
            }
            JSONObject response = new JSONObject();
            response.put("code" , 1);
            response.put("msg"  , "清理成功");
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }

    }

    /***
     * 拉黑
     */
    @RequestMapping("/ban")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String ban (@RequestParam(value = "id", required = false) Integer id,
                       @RequestParam(value = "token", required = false) String  token,
                       @RequestParam(value = "type", required = false,defaultValue = "banUser") String type) {
        try {
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer uid =Integer.parseInt(map.get("uid").toString());
            //可以拉黑的类型包括文章，帖子，动态，用户，拉黑用户代表拉黑全部
            if(!type.equals("banUser")&&!type.equals("banContent")&&!type.equals("banSpace")&&!type.equals("banPost")){
                return Result.getResultJson(0, "参数错误！", null);
            }
            if(type.equals("banUser")){
                if(id.equals(uid)){
                    return Result.getResultJson(0, "你不能拉黑你自己！", null);
                }
                TypechoUsers users = usersService.selectByKey(id);
                if(users==null){
                    return Result.getResultJson(0, "用户不存在！", null);
                }
            }
            if(type.equals("banContent")){
                TypechoContents contents = contentsService.selectByKey(id);
                if(contents==null){
                    return Result.getResultJson(0, "文章不存在！", null);
                }
                if(uid.equals(contents.getAuthorId())){
                    return Result.getResultJson(0, "你不能拉黑你自己的内容！", null);
                }
            }
            if(type.equals("banSpace")){
                TypechoSpace space = spaceService.selectByKey(id);
                if(space==null){
                    return Result.getResultJson(0, "动态不存在！", null);
                }
                if(uid.equals(space.getUid())){
                    return Result.getResultJson(0, "你不能拉黑你自己的内容！", null);
                }
            }
            Long date = System.currentTimeMillis();
            String userTime = String.valueOf(date).substring(0, 10);
            TypechoUserlog userlog = new TypechoUserlog();
            userlog.setUid(uid);
            userlog.setType(type);
            userlog.setCid(-1);
            userlog.setNum(id);
            List<TypechoUserlog> list = service.selectList(userlog);
            if(list.size()>0){
                return Result.getResultJson(0, "你已经执行过相同操作！", null);
            }
            userlog.setNum(id);
            userlog.setToid(0);
            userlog.setCreated(Integer.parseInt(userTime));
            Integer rows = service.insert(userlog);
            JSONObject response = new JSONObject();
            if(rows>0){
                if(type.equals("banUser")){
                    redisHelp.delete(this.dataprefix+"_"+"userInfo_"+id,redisTemplate);
                }
                redisHelp.delete(this.dataprefix+"_"+"myBanLog_"+uid,redisTemplate);
                redisHelp.deleteKeysWithPattern("*"+this.dataprefix+"_myBanList_"+uid+"*",redisTemplate,this.dataprefix);
            }

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
     * 拉黑记录
     */
    @RequestMapping("/myBanLog")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String myBanLog ( @RequestParam(value = "token", required = false) String  token) {
        try {

            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer uid =Integer.parseInt(map.get("uid").toString());

            Map myBanLog = new HashMap<String, String>();
            Map cacheInfo = redisHelp.getMapValue(this.dataprefix+"_"+"myBanLog_"+uid,redisTemplate);
            if(cacheInfo.size()>0){
                myBanLog = cacheInfo;
            }else{
                //获取自己拉黑的情况
                TypechoUserlog mylog = new TypechoUserlog();
                mylog.setUid(uid);
                mylog.setType("banUser");
                List<TypechoUserlog> banUser = service.selectList(mylog);
                List banUserList = new ArrayList<>();
                for (int i = 0; i < banUser.size(); i++) {
                    Map json = JSONObject.parseObject(JSONObject.toJSONString(banUser.get(i)), Map.class);
                    banUserList.add(json);
                }
                mylog.setType("banContent");
                List<TypechoUserlog> banContent = service.selectList(mylog);
                List banContentList = new ArrayList<>();
                for (int i = 0; i < banContent.size(); i++) {
                    Map json = JSONObject.parseObject(JSONObject.toJSONString(banContent.get(i)), Map.class);
                    banContentList.add(json);
                }
                mylog.setType("banSpace");
                List<TypechoUserlog> banSpace = service.selectList(mylog);
                List banSpaceList = new ArrayList<>();
                for (int i = 0; i < banSpace.size(); i++) {
                    Map json = JSONObject.parseObject(JSONObject.toJSONString(banSpace.get(i)), Map.class);
                    banSpaceList.add(json);
                }
                mylog.setType("banPost");
                List<TypechoUserlog> banPost = service.selectList(mylog);
                List banPostList = new ArrayList<>();
                for (int i = 0; i < banPost.size(); i++) {
                    Map json = JSONObject.parseObject(JSONObject.toJSONString(banPost.get(i)), Map.class);
                    banPostList.add(json);
                }
                //获取自己被拉黑的情况
                TypechoUserlog userlog = new TypechoUserlog();
                userlog.setType("banUser");
                userlog.setNum(uid);
                List<TypechoUserlog> banMyUser = service.selectList(userlog);
                List banMyUserList = new ArrayList<>();
                for (int i = 0; i < banMyUser.size(); i++) {
                    Map json = JSONObject.parseObject(JSONObject.toJSONString(banMyUser.get(i)), Map.class);
                    banMyUserList.add(json);
                }
                myBanLog.put("banUserList",banUserList);
                myBanLog.put("banContentList",banContentList);
                myBanLog.put("banSpaceList",banSpaceList);
                myBanLog.put("banPostList",banPostList);
                myBanLog.put("banMyUserList",banMyUserList);
                redisHelp.delete(this.dataprefix+"_"+"myBanLog_"+uid,redisTemplate);
                redisHelp.setKey(this.dataprefix+"_"+"myBanLog_"+uid,myBanLog,1200,redisTemplate);
            }
            JSONObject response = new JSONObject();
            response.put("code", 1);
            response.put("msg", "");
            response.put("data", myBanLog);

            return response.toString();

        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }
    }

    /***
     * 表单删除
     */
    @RequestMapping(value = "/removeBan")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String removeBan(@RequestParam(value = "key", required = false) String  key,
                            @RequestParam(value = "token", required = false) String  token) {
        try {
            //验证用户权限
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer uid =Integer.parseInt(map.get("uid").toString());
            String group = map.get("group").toString();
            TypechoUserlog info = service.selectByKey(key);
            Integer userId = info.getUid();
            if(!group.equals("administrator")){
                if(!userId.equals(uid)){
                    return Result.getResultJson(0,"你无权进行此操作",null);
                }
            }
            Integer rows =  service.delete(key);
            if(rows>0){
                redisHelp.delete(this.dataprefix+"_"+"myBanLog_"+uid,redisTemplate);
                redisHelp.deleteKeysWithPattern("*"+this.dataprefix+"_myBanList_"+uid+"*",redisTemplate,this.dataprefix);
            }
            JSONObject response = new JSONObject();
            response.put("code" , rows);
            response.put("msg"  , rows > 0 ? "操作成功" : "操作失败");
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }

    }
}
