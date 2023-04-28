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
public class TypechoAdsController {

    @Autowired
    TypechoAdsService service;

    @Autowired
    private TypechoApiconfigService apiconfigService;

    @Autowired
    private TypechoUsersService usersService;

    @Autowired
    private TypechoUserlogService userlogService;

    @Autowired
    private TypechoPaylogService paylogService;

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
                redisHelp.setKey(this.dataprefix+"_"+"adsInfo_"+id,adsInfoJson,600,redisTemplate);
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
    public String formPage (@RequestParam(value = "searchParams", required = false) String  searchParams,
                            @RequestParam(value = "searchKey"        , required = false, defaultValue = "") String searchKey,
                            @RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                            @RequestParam(value = "limit"       , required = false, defaultValue = "100") Integer limit,
                            @RequestParam(value = "token", required = false) String  token) {
        TypechoAds query = new TypechoAds();
        if(limit>100){
            limit = 50;
        }
        Integer total = 0;
        List jsonList = new ArrayList();
        List cacheList = new ArrayList();
        try{
            Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
            if(uStatus==0){
                cacheList = redisHelp.getList(this.dataprefix + "_" + "adsList_" + page + "_" + limit + "_" + searchParams+"_"+searchKey, redisTemplate);
            }


            if(cacheList.size()>0){
                jsonList = cacheList;
            }else{
                if (StringUtils.isNotBlank(searchParams)) {
                    JSONObject object = JSON.parseObject(searchParams);
                    query = object.toJavaObject(TypechoAds.class);
                }
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
                List list = pageList.getList();
                if(list.size() < 1){
                    JSONObject noData = new JSONObject();
                    noData.put("code" , 1);
                    noData.put("msg"  , "");
                    noData.put("data" , new ArrayList());
                    noData.put("count", 0);
                    noData.put("total", total);
                    return noData.toString();
                }
                jsonList = pageList.getList();
                redisHelp.delete(this.dataprefix + "_" + "adsList_" + page + "_" + limit + "_" + searchParams+"_"+searchKey,redisTemplate);
                //为了性能和用户体验，广告数据缓存10分钟
                redisHelp.setList(this.dataprefix + "_" + "adsList_" + page + "_" + limit + "_" + searchParams+"_"+searchKey,jsonList,600,redisTemplate);
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
    public String addAds(@RequestParam(value = "params", required = false) String  params,@RequestParam(value = "day", required = false, defaultValue = "0") Integer  day, @RequestParam(value = "token", required = false) String  token) {
        TypechoAds insert = null;
        try{
            Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
            if(uStatus==0){
                return Result.getResultJson(0,"用户未登录或Token验证失败",null);
            }
            if(day<=0){
                return Result.getResultJson(0,"购买天数不正确",null);
            }
            Map jsonToMap =null;
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            String uid = map.get("uid").toString();
            String group = map.get("group").toString();
            TypechoApiconfig apiconfig = UStatus.getConfig(this.dataprefix,apiconfigService,redisTemplate);
            Integer price = 0;
            Integer typeNum = 0;
            if (StringUtils.isNotBlank(params)) {
                jsonToMap =  JSONObject.parseObject(JSON.parseObject(params).toString());
                //获取广告价格
                if(jsonToMap.get("type")==null){
                    return Result.getResultJson(0,"请选择广告类型",null);
                }

                String type = jsonToMap.get("type").toString();
                if(!type.equals("0")&&!type.equals("1")&&!type.equals("2")){
                    return Result.getResultJson(0,"广告类型不正确",null);
                }
                if(type.equals("0")){
                    price = apiconfig.getPushAdsPrice();
                    typeNum = apiconfig.getPushAdsNum();
                }
                if(type.equals("1")){
                    price = apiconfig.getBannerAdsPrice();
                    typeNum = apiconfig.getBannerAdsNum();
                }
                if(type.equals("2")){
                    price = apiconfig.getStartAdsPrice();
                    typeNum = apiconfig.getStartAdsNum();
                }
                //获取当前广告数量和总数量
                TypechoAds num = new TypechoAds();
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
    public String editAds(@RequestParam(value = "params", required = false) String  params, @RequestParam(value = "token", required = false) String  token) {
        TypechoAds update = null;
        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }
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
    public String auditAds(@RequestParam(value = "id", required = false) String  id, @RequestParam(value = "token", required = false) String  token) {
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
        TypechoAds ads = service.selectByKey(id);
        if(ads.getStatus().equals(1)){
            return Result.getResultJson(0, "该广告已审核通过", null);
        }
        ads.setStatus(1);
        Integer rows = service.update(ads);
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
    public String renewalAds(@RequestParam(value = "id", required = false) String  id, @RequestParam(value = "token", required = false) String  token,@RequestParam(value = "day", required = false, defaultValue = "0") Integer  day) {
        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }
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
        TypechoApiconfig apiconfig = UStatus.getConfig(this.dataprefix,apiconfigService,redisTemplate);
        Integer type = ads.getType();
        Integer price = 0;
        if(type.equals(0)){
            price = apiconfig.getPushAdsPrice();
        }
        if(type.equals(1)){
            price = apiconfig.getBannerAdsPrice();
        }
        if(type.equals(2)){
            price = apiconfig.getStartAdsPrice();
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
    public String adsConfig () {
        Map adsConfigJSon = new HashMap<String, String>();
        try{
            Map cacheInfo = redisHelp.getMapValue(this.dataprefix+"_adsConfig",redisTemplate);

            if(cacheInfo.size()>0){
                adsConfigJSon = cacheInfo;
            }else{
                TypechoApiconfig apiconfig = UStatus.getConfig(this.dataprefix,apiconfigService,redisTemplate);
                TypechoAds ads = new TypechoAds();
                ads.setStatus(1);
                Integer pushAdsPrice = apiconfig.getPushAdsPrice();
                Integer pushAdsNum = apiconfig.getPushAdsNum();
                ads.setType(0);
                Integer pushAdsOldNum = service.total(ads);
                Integer pushAdsCurNum = pushAdsNum - pushAdsOldNum;
                Integer bannerAdsPrice = apiconfig.getBannerAdsPrice();
                Integer bannerAdsNum = apiconfig.getBannerAdsNum();
                ads.setType(1);
                Integer bannerAdsOldNum = service.total(ads);
                Integer bannerAdsCurNum = bannerAdsNum - bannerAdsOldNum;
                Integer startAdsPrice = apiconfig.getStartAdsPrice();
                Integer startAdsNum = apiconfig.getStartAdsNum();
                ads.setType(2);
                Integer startAdsOldNum = service.total(ads);
                Integer startAdsCurNum = startAdsNum - startAdsOldNum;
                adsConfigJSon.put("pushAdsPrice",pushAdsPrice);
                adsConfigJSon.put("pushAdsNum",pushAdsCurNum);
                adsConfigJSon.put("bannerAdsPrice",bannerAdsPrice);
                adsConfigJSon.put("bannerAdsNum",bannerAdsCurNum);
                adsConfigJSon.put("startAdsPrice",startAdsPrice);
                adsConfigJSon.put("startAdsNum",startAdsCurNum);
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
