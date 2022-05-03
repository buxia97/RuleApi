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
public class TypechoUserlogController {

    @Autowired
    TypechoUserlogService service;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private TypechoRelationshipsService relationshipsService;

    @Autowired
    private TypechoMetasService metasService;

    @Autowired
    private TypechoContentsService contentsService;

    @Autowired
    private TypechoShopService shopService;

    @Autowired
    private TypechoFieldsService fieldsService;

    @Autowired
    private TypechoUsersService usersService;

    @Autowired
    private TypechoPaylogService paylogService;

    @Autowired
    private TypechoApiconfigService apiconfigService;

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
    public String isMark (@RequestParam(value = "cid", required = false) String  cid,
                            @RequestParam(value = "token", required = false) String  token) {
        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }
        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid =Integer.parseInt(map.get("uid").toString());
        TypechoUserlog userlog = new TypechoUserlog();
        userlog.setCid(Integer.parseInt(cid));
        userlog.setUid(uid);
        userlog.setType("mark");
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
     * 查询用户收藏列表
     * @param page         页码
     * @param limit        每页显示数量
     */
    @RequestMapping(value = "/markList")
    @ResponseBody
    public String markList (@RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                            @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit,
                            @RequestParam(value = "token", required = false) String  token) {


        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }
        if(limit>50){
            limit = 50;
        }
        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid =Integer.parseInt(map.get("uid").toString());

        TypechoUserlog query = new TypechoUserlog();
        query.setUid(uid);
        query.setType("mark");

        List jsonList = new ArrayList();
        List cacheList = redisHelp.getList(this.dataprefix+"_"+"markList_"+page+"_"+limit+"_"+uid,redisTemplate);
        try{
            if(cacheList.size()>0){
                jsonList = cacheList;
            }else {
                PageList<TypechoUserlog> pageList = service.selectPage(query, page, limit);
                List<TypechoUserlog> list = pageList.getList();
                for (int i = 0; i < list.size(); i++) {
                    Integer cid = list.get(i).getCid();

                    TypechoContents typechoContents = contentsService.selectByKey(cid);
                    Map contentsInfo = JSONObject.parseObject(JSONObject.toJSONString(typechoContents), Map.class);
                    //只有开放状态文章允许加入
                    String status = contentsInfo.get("status").toString();
                    String ctype = contentsInfo.get("type").toString();
                    //应该判断类型和发布状态，而不是直接判断状态
                    if (status.equals("publish") && ctype.equals("post")) {
                        //处理文章内容为简介

                        String text = contentsInfo.get("text").toString();
                        List imgList = baseFull.getImageSrc(text);
                        text = text.replaceAll("(\\\r\\\n|\\\r|\\\n|\\\n\\\r)", "");
                        text = text.replaceAll("\\s*", "");
                        text = text.replaceAll("</?[^>]+>", "");
                        //去掉文章开头的图片插入
                        text=text.replaceAll("((https?|http):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)","");
                        text=text.replaceAll("((!\\[)[\\s\\S]+?(\\]\\[)[\\s\\S]+?(\\]))", "");
                        text=text.replaceAll("((!\\[)[\\s\\S]+?(\\]))", "");
                        text=text.replaceAll("\\(", "");
                        text=text.replaceAll("\\)", "");
                        contentsInfo.put("text", text.length() > 200 ? text.substring(0, 200) : text);
                        contentsInfo.put("images", imgList);
                        //加入自定义字段，分类和标签
                        //加入自定义字段信息，这里取消注释即可开启，但是数据库查询会消耗性能
                        List<TypechoFields> fields = fieldsService.selectByKey(cid);
                        contentsInfo.put("fields", fields);

                        List<TypechoRelationships> relationships = relationshipsService.selectByKey(cid);

                        List metas = new ArrayList();
                        List tags = new ArrayList();
                        for (int j = 0; j < relationships.size(); j++) {
                            Map info = JSONObject.parseObject(JSONObject.toJSONString(relationships.get(j)), Map.class);
                            if (info != null) {
                                String mid = info.get("mid").toString();

                                TypechoMetas metasList = metasService.selectByKey(mid);
                                Map metasInfo = JSONObject.parseObject(JSONObject.toJSONString(metasList), Map.class);
                                String type = metasInfo.get("type").toString();
                                if (type.equals("category")) {
                                    metas.add(metasInfo);
                                }
                                if (type.equals("tag")) {
                                    tags.add(metasInfo);
                                }
                            }

                        }

                        contentsInfo.remove("password");
                        contentsInfo.put("category", metas);
                        contentsInfo.put("tag", tags);
                        contentsInfo.put("logid", list.get(i).getId());
                        jsonList.add(contentsInfo);
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
        return response.toString();
    }
    /***
     * 查询用户打赏历史
     * @param page         页码
     * @param limit        每页显示数量
     */
    @RequestMapping(value = "/rewardList")
    @ResponseBody
    public String rewardList (@RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                            @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit,
                            @RequestParam(value = "token", required = false) String  token) {
        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }
        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid =Integer.parseInt(map.get("uid").toString());
        if(limit>50){
            limit = 50;
        }
        TypechoUserlog query = new TypechoUserlog();
        query.setUid(uid);
        query.setType("reward");
        PageList<TypechoUserlog> pageList = service.selectPage(query, page, limit);


        JSONObject response = new JSONObject();
        response.put("code" , 1);
        response.put("msg"  , "");
        response.put("data" , null != pageList.getList() ? pageList.getList() : new JSONArray());
        response.put("count", pageList.getList());
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

            if (StringUtils.isNotBlank(params)) {


                jsonToMap =  JSONObject.parseObject(JSON.parseObject(params).toString());

                //生成typecho数据库格式的修改时间戳
                Long date = System.currentTimeMillis();
                String userTime = String.valueOf(date).substring(0,10);
                jsonToMap.put("created",userTime);
                String type = jsonToMap.get("type").toString();
                //只有喜欢操作不需要登陆拦截
                if(!type.equals("likes")){
                    Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
                    if(uStatus==0){
                        return Result.getResultJson(0,"请先登录哦",null);
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
                    TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);
                    Integer clockMax = apiconfig.getClock();
                    if (clockMax < 1){
                        return Result.getResultJson(0,"签到功能已关闭",null);
                    }
                    int award = r.nextInt(clockMax) + 1;
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
                    Integer account = user.getAssets();
                    Integer Assets = account + award;

                    TypechoUsers newUser = new TypechoUsers();
                    newUser.setUid(uid);
                    newUser.setAssets(Assets);

                    usersService.update(newUser);
                    jsonToMap.put("num",award);
                    clock = "，获得"+award+"积分奖励！";

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
                    //扣除自己的积分
                    TypechoUsers newUser = new TypechoUsers();
                    newUser.setUid(uid);
                    newUser.setAssets(Assets);
                    usersService.update(newUser);
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
                    }


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
                    }

                }
                insert = JSON.parseObject(JSON.toJSONString(jsonToMap), TypechoUserlog.class);
            }

            int rows = service.insert(insert);

            JSONObject response = new JSONObject();
            response.put("code" , rows);
            response.put("msg"  , rows > 0 ? "操作成功"+clock : "操作失败");
            return response.toString();
        }catch (Exception e){
            System.out.println(e);
            return Result.getResultJson(0,"操作失败",null);
        }

    }

    /***
     * 表单删除
     */
    @RequestMapping(value = "/removeLog")
    @ResponseBody
    public String removeLog(@RequestParam(value = "key", required = false) String  key,@RequestParam(value = "token", required = false) String  token) {
        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }
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
    public String orderList (@RequestParam(value = "token", required = false) String  token) {

        String page = "1";
        String limit = "60";
        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }
        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid =Integer.parseInt(map.get("uid").toString());

        TypechoUserlog query = new TypechoUserlog();
        query.setUid(uid);
        query.setType("buy");

        List jsonList = new ArrayList();
        List cacheList = redisHelp.getList(this.dataprefix+"_"+"orderList_"+page+"_"+limit+"_"+uid,redisTemplate);
        try{
            if(cacheList.size()>0){
                jsonList = cacheList;
            }else {
                PageList<TypechoUserlog> pageList = service.selectPage(query, Integer.parseInt(page), Integer.parseInt(limit));
                List<TypechoUserlog> list = pageList.getList();
                for (int i = 0; i < list.size(); i++) {
                    Integer cid = list.get(i).getCid();
                    //这里cid是商品id
                    TypechoShop shop = shopService.selectByKey(cid);
                    Map shopInfo = JSONObject.parseObject(JSONObject.toJSONString(shop), Map.class);
                    Map json = JSONObject.parseObject(JSONObject.toJSONString(list.get(i)), Map.class);
                    json.put("shopInfo",shopInfo);
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
        return response.toString();
    }
    /***
     * 查询售出订单列表
     */
    @RequestMapping(value = "/orderSellList")
    @ResponseBody
    public String orderSellList (@RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                                 @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit,
                                 @RequestParam(value = "token", required = false) String  token) {

        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }
        if(limit>50){
            limit = 50;
        }
        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid =Integer.parseInt(map.get("uid").toString());

        TypechoUserlog query = new TypechoUserlog();
        query.setToid(uid);
        query.setType("buy");

        List jsonList = new ArrayList();
        List cacheList = redisHelp.getList(this.dataprefix+"_"+"orderSellList_"+page+"_"+limit+"_"+uid,redisTemplate);
        try{
            if(cacheList.size()>0){
                jsonList = cacheList;
            }else {
                PageList<TypechoUserlog> pageList = service.selectPage(query, page, limit);
                List<TypechoUserlog> list = pageList.getList();
                for (int i = 0; i < list.size(); i++) {
                    Integer cid = list.get(i).getCid();
                    Integer touid = list.get(i).getUid();
                    //这里cid是商品id
                    TypechoShop shop = shopService.selectByKey(cid);
                    Map shopInfo = JSONObject.parseObject(JSONObject.toJSONString(shop), Map.class);
                    Map json = JSONObject.parseObject(JSONObject.toJSONString(list.get(i)), Map.class);
                    json.put("shopInfo",shopInfo);
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
        return response.toString();
    }

    /***
     * 查询商品是否已经购买过
     */
    @RequestMapping(value = "/dataClean")
    @ResponseBody
    public String dataClean(@RequestParam(value = "clean", required = false) Integer  clean,@RequestParam(value = "token", required = false) String  token) {
        try {
            //1是清理用户签到，2是清理用户资产日志，3是清理用户订单数据，4是清理无效卡密
            Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
            if (uStatus == 0) {
                return Result.getResultJson(0, "用户未登录或Token验证失败", null);
            }
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
            JSONObject response = new JSONObject();
            response.put("code" , 1);
            response.put("msg"  , "清理成功");
            return response.toString();
        }catch (Exception e){
            JSONObject response = new JSONObject();
            response.put("code" , 0);
            response.put("msg"  , "操作失败");
            return response.toString();
        }

    }
}
