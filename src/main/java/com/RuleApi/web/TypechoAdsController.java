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
    public String adsInfo (@RequestParam(value = "id", required = false) String  id) {
        Map adsInfoJson = new HashMap<String, String>();
        try{
            Map cacheInfo = redisHelp.getMapValue(this.dataprefix+"_"+"adsInfo_"+id,redisTemplate);
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
                            @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit) {
        TypechoAds query = new TypechoAds();
        if(limit>50){
            limit = 50;
        }
        Integer total = 0;
        List jsonList = new ArrayList();
        List cacheList = new ArrayList();
        try{
            cacheList = redisHelp.getList(this.dataprefix + "_" + "adsList_" + page + "_" + limit + "_" + searchParams, redisTemplate);
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
    public String addAds(@RequestParam(value = "params", required = false) String  params, @RequestParam(value = "token", required = false) String  token) {
        TypechoAds insert = null;

        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }
        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        String uid = map.get("uid").toString();
        if (StringUtils.isNotBlank(params)) {
            JSONObject object = JSON.parseObject(params);
            insert = object.toJavaObject(TypechoAds.class);
        }
        insert.setUid(Integer.parseInt(uid));
        int rows = service.insert(insert);

        JSONObject response = new JSONObject();
        response.put("code" , rows);
        response.put("msg"  , rows > 0 ? "添加成功" : "添加失败");
        return response.toString();
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
        if (StringUtils.isNotBlank(params)) {
            JSONObject object = JSON.parseObject(params);
            update = object.toJavaObject(TypechoAds.class);
        }
        update.setUid(Integer.parseInt(uid));
        int rows = service.update(update);

        JSONObject response = new JSONObject();
        response.put("code" , rows);
        response.put("msg"  , rows > 0 ? "修改成功" : "修改失败");
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
        Integer rows =  service.delete(id);
        JSONObject response = new JSONObject();
        response.put("code" ,rows > 0 ? 1: 0 );
        response.put("data" , rows);
        response.put("msg"  , rows > 0 ? "操作成功" : "操作失败");
        return response.toString();
    }
}
