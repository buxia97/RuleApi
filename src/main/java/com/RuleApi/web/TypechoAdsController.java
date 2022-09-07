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
            System.out.println(e);
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
                cacheList = redisHelp.getList(this.dataprefix + "_" + "adsList_" + page + "_" + limit + "_" + searchParams, redisTemplate);
            }

            if(cacheList.size()>0){
                jsonList = cacheList;
            }else{
                if (StringUtils.isNotBlank(searchParams)) {
                    JSONObject object = JSON.parseObject(searchParams);
                    query = object.toJavaObject(TypechoAds.class);
                }
                total = service.total(query);
                PageList<TypechoAds> pageList = service.selectPage(query, page, limit);
                jsonList = pageList.getList();
                redisHelp.delete(this.dataprefix + "_" + "adsList_" + page + "_" + limit + "_" + searchParams,redisTemplate);
                //为了性能和用户体验，广告数据缓存10分钟
                redisHelp.setList(this.dataprefix + "_" + "adsList_" + page + "_" + limit + "_" + searchParams,jsonList,600,redisTemplate);
            }
        }catch (Exception e){
            System.out.println(e);
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
                return Result.getResultJson(0,"购买时间不正确",null);
            }
            Map jsonToMap =null;
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            String uid = map.get("uid").toString();
            TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);
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
                paylog.setOutTradeNo(created+"buyads");
                paylog.setTotalAmount("-"+price);
                paylog.setPaytype("buyads");
                paylog.setSubject("开通广告位");
                paylogService.insert(paylog);
            }else{
                return Result.getResultJson(0,"参数不正确",null);
            }
            insert.setStatus(0);
            insert.setUid(Integer.parseInt(uid));
            int rows = service.insert(insert);
            JSONObject response = new JSONObject();
            response.put("code" , rows);
            response.put("msg"  , rows > 0 ? "添加成功，等待管理员审核" : "添加失败");
            return response.toString();
        }catch (Exception e){
            System.out.println(e);
            return Result.getResultJson(0,"添加失败",null);
        }



    }

    /***
     * 表单修改
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
        update.setStatus(0);
        int rows = service.update(update);

        JSONObject response = new JSONObject();
        response.put("code" , rows);
        response.put("msg"  , rows > 0 ? "修改成功，等待管理员审核" : "修改失败");
        return response.toString();
    }

    /***
     * 表单删除
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
        Integer rows =  service.delete(id);
        JSONObject response = new JSONObject();
        response.put("code" ,rows > 0 ? 1: 0 );
        response.put("data" , rows);
        response.put("msg"  , rows > 0 ? "操作成功" : "操作失败");
        return response.toString();
    }
}
