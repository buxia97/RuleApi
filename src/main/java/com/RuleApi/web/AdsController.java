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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 控制层
 * TypechoAdsController
 * @author ads
 * @date 2022/09/06
 */
@Controller
@RequestMapping(value = "/typechoAds")
public class AdsController {

    @Autowired
    AdsService service;

    @Autowired
    private AllconfigService allconfigService;

    @Autowired
    private UsersService usersService;

    @Autowired
    private UserlogService userlogService;

    @Autowired
    private PaylogService paylogService;

    @Autowired
    private InboxService inboxService;

    @Autowired
    private RedisTemplate redisTemplate;

    RedisHelp redisHelp =new RedisHelp();
    ResultAll Result = new ResultAll();
    baseFull baseFull = new baseFull();
    UserStatus UStatus = new UserStatus();
    EditFile editFile = new EditFile();



    @Value("${web.prefix}")
    private String dataprefix;


    /**
     * 参数请求报文:
     *
     * {
     *   "key":1
     * }
     */
    @RequestMapping(value = "/adsInfo")
    @ResponseBody
    @LoginRequired(purview = "-1")
    public String adsInfo (@RequestParam(value = "id", required = false) String  id, @RequestParam(value = "token", required = false) String  token) {
        Map adsInfoJson = new HashMap<String, String>();
        Map cacheInfo = new HashMap<String, String>();
        try{
            Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
            if(uStatus==0){
                cacheInfo = redisHelp.getMapValue(this.dataprefix+"_"+"adsInfo_"+id,redisTemplate);
            }
            if(cacheInfo.size()>0){
                adsInfoJson = cacheInfo;
            }else{
                TypechoAds typechoAds = service.selectByKey(id);
                adsInfoJson = JSONObject.parseObject(JSONObject.toJSONString(typechoAds), Map.class);
                //为了性能和用户体验，广告数据缓存10分钟
                redisHelp.delete(this.dataprefix+"_"+"adsInfo_"+id,redisTemplate);
                redisHelp.setKey(this.dataprefix+"_"+"adsInfo_"+id,adsInfoJson,60,redisTemplate);
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        JSONObject adsInfo = JSON.parseObject(JSON.toJSONString(adsInfoJson),JSONObject.class);
        return adsInfo.toJSONString();
    }


    /***
     * 广告列表
     * @param searchParams Bean对象JSON字符串
     * @param page         页码
     * @param limit        每页显示数量
     */
    @RequestMapping(value = "/adsList")
    @ResponseBody
    @LoginRequired(purview = "-1")
    public String adsList (@RequestParam(value = "searchParams", required = false) String  searchParams,
                            @RequestParam(value = "searchKey"        , required = false, defaultValue = "") String searchKey,
                            @RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                            @RequestParam(value = "limit"       , required = false, defaultValue = "100") Integer limit,
                            @RequestParam(value = "token", required = false) String  token) {
        TypechoAds query = new TypechoAds();
        if(limit>100){
            limit = 50;
        }
        String sqlParams = "null";
        Integer total = 0;
        List jsonList = new ArrayList();
        List cacheList = new ArrayList();
        try{
            if (StringUtils.isNotBlank(searchParams)) {
                JSONObject object = JSON.parseObject(searchParams);
                query = object.toJavaObject(TypechoAds.class);
                Map paramsJson = JSONObject.parseObject(JSONObject.toJSONString(query), Map.class);
                sqlParams = paramsJson.toString();
            }
            Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
            if(uStatus==0){
                cacheList = redisHelp.getList(this.dataprefix + "_" + "adsList_" + page + "_" + limit + "_" + sqlParams+"_"+searchKey, redisTemplate);
            }


            if(cacheList.size()>0){
                jsonList = cacheList;
            }else{

                //无token访问则传公开广告数据，已登录用户除管理员外只能查询自己的广告
                if(uStatus==0){
                    query.setStatus(1);
                }else{
                    Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
                    String uid = map.get("uid").toString();
                    String group = map.get("group").toString();
                    if (!group.equals("administrator")) {
                        query.setUid(Integer.parseInt(uid));
                    }
                }
                total = service.total(query);
                PageList<TypechoAds> pageList = service.selectPage(query, page, limit,searchKey);
                List<TypechoAds> list = pageList.getList();
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
                    TypechoAds ads = list.get(i);
                    Integer userid = ads.getUid();
                    //获取用户信息
                    Map userJson = UserStatus.getUserInfo(userid,allconfigService,usersService);
                    json.put("userJson",userJson);
                    jsonList.add(json);
                }
                redisHelp.delete(this.dataprefix + "_" + "adsList_" + page + "_" + limit + "_" + sqlParams+"_"+searchKey,redisTemplate);
                //为了性能和用户体验，广告数据缓存10分钟
                redisHelp.setList(this.dataprefix + "_" + "adsList_" + page + "_" + limit + "_" + sqlParams+"_"+searchKey,jsonList,20,redisTemplate);
            }
        }catch (Exception e){
            e.printStackTrace();
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
     * 表单插入
     * @param params Bean对象JSON字符串
     */
    @RequestMapping(value = "/addAds")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String addAds(@RequestParam(value = "params", required = false) String  params,@RequestParam(value = "day", required = false, defaultValue = "0") Integer  day, @RequestParam(value = "token", required = false) String  token) {
        TypechoAds insert = null;
        try{
            if(day<=0){
                return Result.getResultJson(0,"购买天数不正确",null);
            }
            Map jsonToMap =null;
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            String uid = map.get("uid").toString();
            String group = map.get("group").toString();
            Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
            Integer price = 0;
            Integer typeNum = 0;
            if (StringUtils.isNotBlank(params)) {
                jsonToMap =  JSONObject.parseObject(JSON.parseObject(params).toString());
                //获取广告价格
                if(jsonToMap.get("type")==null){
                    return Result.getResultJson(0,"请选择广告类型",null);
                }

                String type = jsonToMap.get("type").toString();
                if(!type.equals("0")&&!type.equals("1")&&!type.equals("2")&&!type.equals("3")){
                    return Result.getResultJson(0,"广告类型不正确",null);
                }
                if(type.equals("0")){
                    price =  0;
                    if(apiconfig.get("pushAdsPrice")!=null){
                        price = Integer.parseInt(apiconfig.get("pushAdsPrice").toString());
                    }
                    typeNum = 0;
                    if(apiconfig.get("pushAdsNum")!=null){
                        typeNum = Integer.parseInt(apiconfig.get("pushAdsNum").toString());
                    }
                }
                if(type.equals("1")){
                    price =  0;
                    if(apiconfig.get("bannerAdsPrice")!=null){
                        price = Integer.parseInt(apiconfig.get("bannerAdsPrice").toString());
                    }
                    typeNum = 0;
                    if(apiconfig.get("bannerAdsNum")!=null){
                        typeNum = Integer.parseInt(apiconfig.get("bannerAdsNum").toString());
                    }
                }
                if(type.equals("2")){
                    price =  0;
                    if(apiconfig.get("startAdsPrice")!=null){
                        price = Integer.parseInt(apiconfig.get("startAdsPrice").toString());
                    }
                    typeNum = 0;
                    if(apiconfig.get("startAdsNum")!=null){
                        typeNum = Integer.parseInt(apiconfig.get("startAdsNum").toString());
                    }
                }
                if(type.equals("3")){
                    price =  0;
                    if(apiconfig.get("swiperAdsPrice")!=null){
                        price = Integer.parseInt(apiconfig.get("swiperAdsPrice").toString());
                    }
                    typeNum = 0;
                    if(apiconfig.get("swiperAdsNum")!=null){
                        typeNum = Integer.parseInt(apiconfig.get("swiperAdsNum").toString());
                    }

                }
                //获取当前公开广告数量和总数量
                TypechoAds num = new TypechoAds();
                num.setStatus(1);
                num.setType(Integer.parseInt(type));
                Integer total = service.total(num);
                if(typeNum <= total){
                    return Result.getResultJson(0,"该广告位已售完",null);
                }
                //判断余额是否足够
                price = price * day;
                TypechoUsers usersinfo =usersService.selectByKey(uid);
                Integer oldAssets =usersinfo.getAssets();
                if(price>oldAssets){
                    return Result.getResultJson(0,"积分余额不足",null);
                }
                Long date = System.currentTimeMillis();
                String created = String.valueOf(date).substring(0,10);
                Integer days = 86400;
                Integer closeTime = Integer.parseInt(created) + days*day;
                jsonToMap.put("close",closeTime);
                jsonToMap.put("price",price);
                jsonToMap.put("created",created);
                insert = JSON.parseObject(JSON.toJSONString(jsonToMap), TypechoAds.class);

                //扣除积分
                Integer Assets = oldAssets - price;
                usersinfo.setAssets(Assets);
                usersService.update(usersinfo);

                //生成购买者资产日志
                TypechoPaylog paylog = new TypechoPaylog();
                paylog.setStatus(1);
                paylog.setCreated(Integer.parseInt(created));
                paylog.setUid(Integer.parseInt(uid));
                paylog.setOutTradeNo(created+"buyAds");
                paylog.setTotalAmount("-"+price);
                paylog.setPaytype("buyAds");
                paylog.setSubject("开通广告位");
                paylogService.insert(paylog);
            }else{
                return Result.getResultJson(0,"参数不正确",null);
            }
            if(!group.equals("administrator")&&!group.equals("editor")) {
                insert.setStatus(0);
            }else{
                insert.setStatus(1);
            }
            insert.setUid(Integer.parseInt(uid));
            int rows = service.insert(insert);
            editFile.setLog("用户"+uid+"请求发布了新广告");
            JSONObject response = new JSONObject();
            response.put("code" , rows);
            response.put("msg"  , rows > 0 ? "添加成功，等待管理员审核" : "添加失败");
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }



    }

    /***
     * 修改广告
     * @param params Bean对象JSON字符串
     */
    @RequestMapping(value = "/editAds")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String editAds(@RequestParam(value = "params", required = false) String  params, @RequestParam(value = "token", required = false) String  token) {
        TypechoAds update = null;
        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        String uid = map.get("uid").toString();
        String group = map.get("group").toString();
        Map jsonToMap =null;
        if (StringUtils.isNotBlank(params)) {
            jsonToMap =  JSONObject.parseObject(JSON.parseObject(params).toString());
            if(jsonToMap.get("aid")==null){
                return Result.getResultJson(0,"请传入广告ID",null);
            }else {
                if(!group.equals("administrator")&&!group.equals("editor")) {
                    Integer aid = Integer.parseInt(jsonToMap.get("aid").toString());
                    TypechoAds total = new TypechoAds();
                    total.setAid(aid);
                    total.setUid(Integer.parseInt(uid));
                    Integer num = service.total(total);
                    if (num < 1) {
                        return Result.getResultJson(0, "你无权限修改他人的广告", null);
                    }
                }
            }
            jsonToMap.remove("close");
            jsonToMap.remove("created");
            jsonToMap.remove("price");
            jsonToMap.remove("uid");
            update = JSON.parseObject(JSON.toJSONString(jsonToMap), TypechoAds.class);
        }else{
            return Result.getResultJson(0,"参数不正确",null);
        }
        update.setUid(Integer.parseInt(uid));
        //广告修改也要审核
        if(!group.equals("administrator")&&!group.equals("editor")) {
            update.setStatus(0);
        }
        int rows = service.update(update);
        editFile.setLog("用户"+uid+"请求修改了广告");
        JSONObject response = new JSONObject();
        response.put("code" , rows);
        response.put("msg"  , rows > 0 ? "修改成功，等待管理员审核" : "修改失败");
        return response.toString();
    }

    /***
     * 广告删除
     */
    @RequestMapping(value = "/deleteAds")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String deleteAds(@RequestParam(value = "id", required = false) String  id, @RequestParam(value = "token", required = false) String  token) {
        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }
        Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
        String group = map.get("group").toString();
        if (!group.equals("administrator")) {
            return Result.getResultJson(0, "你没有操作权限", null);
        }
        String uid = map.get("uid").toString();
        Integer rows =  service.delete(id);
        editFile.setLog("管理员"+uid+"请求删除广告"+id);
        JSONObject response = new JSONObject();
        response.put("code" ,rows > 0 ? 1: 0 );
        response.put("data" , rows);
        response.put("msg"  , rows > 0 ? "操作成功" : "操作失败");
        return response.toString();
    }

    /***
     * 广告审核
     */
    @RequestMapping(value = "/auditAds")
    @ResponseBody
    @LoginRequired(purview = "2")
    public String auditAds(@RequestParam(value = "id", required = false) String  id, @RequestParam(value = "token", required = false) String  token) {
        Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
        String uid = map.get("uid").toString();
        TypechoAds ads = service.selectByKey(id);
        if(ads.getStatus().equals(1)){
            return Result.getResultJson(0, "该广告已审核通过", null);
        }
        ads.setStatus(1);
        Integer rows = service.update(ads);
        Long date = System.currentTimeMillis();
        String created = String.valueOf(date).substring(0,10);
        TypechoInbox insert = new TypechoInbox();
        insert.setUid(Integer.parseInt(uid));
        insert.setTouid(ads.getUid());
        insert.setType("system");
        insert.setText("你的广告已审核通过");
        insert.setCreated(Integer.parseInt(created));
        inboxService.insert(insert);
        editFile.setLog("管理员"+uid+"请求审核广告"+id);
        JSONObject response = new JSONObject();
        response.put("code" ,rows > 0 ? 1: 0 );
        response.put("data" , rows);
        response.put("msg"  , rows > 0 ? "操作成功" : "操作失败");
        return response.toString();
    }

    /***
     * 广告续期
     */
    @RequestMapping(value = "/renewalAds")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String renewalAds(@RequestParam(value = "id", required = false) String  id,
                             @RequestParam(value = "token", required = false) String  token,
                             @RequestParam(value = "day", required = false, defaultValue = "0") Integer  day) {
        Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
        String group = map.get("group").toString();
        if (!group.equals("administrator")) {
            return Result.getResultJson(0, "你没有操作权限", null);
        }
        String logUid = map.get("uid").toString();
        if(day<=0){
            return Result.getResultJson(0,"购买天数不正确",null);
        }
        TypechoAds ads = service.selectByKey(id);
        Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
        Integer type = ads.getType();
        Integer price = 0;
        Integer typeNum = 0;
        if(type.equals(0)){
            price =  0;
            if(apiconfig.get("pushAdsPrice")!=null){
                price = Integer.parseInt(apiconfig.get("pushAdsPrice").toString());
            }
            typeNum = 0;
            if(apiconfig.get("pushAdsNum")!=null){
                typeNum = Integer.parseInt(apiconfig.get("pushAdsNum").toString());
            }
        }
        if(type.equals(1)){
            price =  0;
            if(apiconfig.get("bannerAdsPrice")!=null){
                price = Integer.parseInt(apiconfig.get("bannerAdsPrice").toString());
            }
            typeNum = 0;
            if(apiconfig.get("bannerAdsNum")!=null){
                typeNum = Integer.parseInt(apiconfig.get("bannerAdsNum").toString());
            }
        }
        if(type.equals(2)){
            price =  0;
            if(apiconfig.get("startAdsPrice")!=null){
                price = Integer.parseInt(apiconfig.get("startAdsPrice").toString());
            }
            typeNum = 0;
            if(apiconfig.get("startAdsNum")!=null){
                typeNum = Integer.parseInt(apiconfig.get("startAdsNum").toString());
            }
        }
        if(type.equals(3)){
            price =  0;
            if(apiconfig.get("swiperAdsPrice")!=null){
                price = Integer.parseInt(apiconfig.get("swiperAdsPrice").toString());
            }
            typeNum = 0;
            if(apiconfig.get("swiperAdsNum")!=null){
                typeNum = Integer.parseInt(apiconfig.get("swiperAdsNum").toString());
            }

        }
        //获取当前公开广告数量和总数量，如果现在的广告是关闭状态，续期需要校验数量
        if(!ads.getStatus().equals(1)){
            TypechoAds num = new TypechoAds();
            num.setStatus(1);
            num.setType(type);
            Integer total = service.total(num);
            if(typeNum <= total){
                return Result.getResultJson(0,"该广告位已售完，无法续期",null);
            }
        }
        //计算价格
        Integer cost = price * day;
        Integer newPrice = ads.getPrice();
        newPrice = newPrice + cost;
        ads.setPrice(newPrice);
        //计算时间
        Long date = System.currentTimeMillis();
        Integer curTime = Integer.parseInt(String.valueOf(date).substring(0,10));
        Integer closeTime = ads.getClose();
        Integer days = 86400;
        if(closeTime > curTime){
            closeTime = closeTime + days*day;
        }else{
            closeTime = curTime + days*day;
        }
        ads.setStatus(0);
        ads.setClose(closeTime);
        //获取用户ID并插入日志数据库
        Integer uid = ads.getUid();
        TypechoPaylog paylog = new TypechoPaylog();
        paylog.setStatus(1);
        paylog.setCreated(curTime);
        paylog.setUid(uid);
        paylog.setOutTradeNo(curTime+"renewalAds");
        paylog.setTotalAmount(""+cost);
        paylog.setPaytype("renewalAds");
        paylog.setSubject("系统赠送广告位时间"+day+"天");
        paylogService.insert(paylog);
        //修改广告信息
        editFile.setLog("管理员"+uid+"请求续期广告"+id+"总计"+day+"天");
        Integer rows = service.update(ads);
        JSONObject response = new JSONObject();
        response.put("code" ,rows > 0 ? 1: 0 );
        response.put("data" , rows);
        response.put("msg"  , rows > 0 ? "操作成功" : "操作失败");
        return response.toString();
    }
    /***
     * 广告配置信息
     */
    @RequestMapping(value = "/adsConfig")
    @ResponseBody
    @LoginRequired(purview = "-1")
    public String adsConfig () {
        Map adsConfigJSon = new HashMap<String, String>();
        try{
            Map cacheInfo = redisHelp.getMapValue(this.dataprefix+"_adsConfig",redisTemplate);

            if(cacheInfo.size()>0){
                adsConfigJSon = cacheInfo;
            }else{
                Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
                TypechoAds ads = new TypechoAds();
                ads.setStatus(1);
                Integer pushAdsPrice = 0;
                if(apiconfig.get("pushAdsPrice")!=null){
                    pushAdsPrice = Integer.parseInt(apiconfig.get("pushAdsPrice").toString());
                }
                Integer pushAdsNum = 0;
                if(apiconfig.get("pushAdsNum")!=null){
                    pushAdsNum = Integer.parseInt(apiconfig.get("pushAdsNum").toString());
                }
                ads.setType(0);
                Integer pushAdsOldNum = service.total(ads);
                Integer pushAdsCurNum = pushAdsNum - pushAdsOldNum;
                Integer bannerAdsPrice = 0;
                if(apiconfig.get("bannerAdsPrice")!=null){
                    bannerAdsPrice = Integer.parseInt(apiconfig.get("bannerAdsPrice").toString());
                }
                Integer bannerAdsNum = 0;
                if(apiconfig.get("bannerAdsNum")!=null){
                    bannerAdsNum = Integer.parseInt(apiconfig.get("bannerAdsNum").toString());
                }
                ads.setType(1);
                Integer bannerAdsOldNum = service.total(ads);
                Integer bannerAdsCurNum = bannerAdsNum - bannerAdsOldNum;
                Integer startAdsPrice = 0;
                if(apiconfig.get("startAdsPrice")!=null){
                    startAdsPrice = Integer.parseInt(apiconfig.get("startAdsPrice").toString());
                }
                Integer startAdsNum = 0;
                if(apiconfig.get("startAdsNum")!=null){
                    startAdsNum = Integer.parseInt(apiconfig.get("startAdsNum").toString());
                }
                ads.setType(2);
                Integer startAdsOldNum = service.total(ads);
                Integer startAdsCurNum = startAdsNum - startAdsOldNum;

                Integer swiperAdsPrice = 0;
                if(apiconfig.get("swiperAdsPrice")!=null){
                    swiperAdsPrice = Integer.parseInt(apiconfig.get("swiperAdsPrice").toString());
                }
                Integer swiperAdsNum = 0;
                if(apiconfig.get("swiperAdsNum")!=null){
                    swiperAdsNum = Integer.parseInt(apiconfig.get("swiperAdsNum").toString());
                }
                ads.setType(3);
                Integer swiperAdsOldNum = service.total(ads);
                Integer swiperAdsCurNum = swiperAdsNum - swiperAdsOldNum;

                adsConfigJSon.put("pushAdsPrice",pushAdsPrice);
                adsConfigJSon.put("pushAdsNum",pushAdsCurNum);
                adsConfigJSon.put("bannerAdsPrice",bannerAdsPrice);
                adsConfigJSon.put("bannerAdsNum",bannerAdsCurNum);
                adsConfigJSon.put("startAdsPrice",startAdsPrice);
                adsConfigJSon.put("startAdsNum",startAdsCurNum);
                adsConfigJSon.put("swiperAdsPrice",swiperAdsPrice);
                adsConfigJSon.put("swiperAdsNum",swiperAdsCurNum);
                redisHelp.delete(this.dataprefix+"_adsConfig",redisTemplate);
                redisHelp.setKey(this.dataprefix+"_adsConfig",adsConfigJSon,5,redisTemplate);
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        JSONObject response = new JSONObject();
        response.put("code" ,1 );
        response.put("data" , adsConfigJSon);
        response.put("msg"  , "");
        return response.toString();
    }
}
