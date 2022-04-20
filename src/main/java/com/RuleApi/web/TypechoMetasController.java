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
import org.springframework.stereotype.Component;
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
 * TypechoMetasController
 * @author buxia97
 * @date 2021/11/29
 */
@Component
@Controller
@RequestMapping(value = "/typechoMetas")
public class TypechoMetasController {

    @Autowired
    TypechoMetasService service;

    @Autowired
    private TypechoRelationshipsService relationshipsService;

    @Autowired
    private TypechoContentsService contentsService;

    @Autowired
    private TypechoFieldsService fieldsService;

    @Autowired
    private TypechoUsersService usersService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${webinfo.contentCache}")
    private Integer contentCache;

    @Value("${web.prefix}")
    private String dataprefix;

    @Value("${webinfo.avatar}")
    private String avatar;

    RedisHelp redisHelp =new RedisHelp();
    ResultAll Result = new ResultAll();
    baseFull baseFull = new baseFull();
    UserStatus UStatus = new UserStatus();
    /***
     * 查询分类或标签下的文章
     *
     */
    @RequestMapping(value = "/selectContents")
    @ResponseBody
    public String selectContents (@RequestParam(value = "searchParams", required = false) String  searchParams,
                                  @RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                                  @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit) {

        TypechoRelationships query = new TypechoRelationships();
        if(limit>50){
            limit = 50;
        }
        if (StringUtils.isNotBlank(searchParams)) {
            JSONObject object = JSON.parseObject(searchParams);
            Integer mid = Integer.parseInt(object.get("mid").toString());
            query.setMid(mid);
            TypechoContents contents = new TypechoContents();
            contents.setType("post");
            contents.setStatus("publish");
            query.setContents(contents);
        }
        List jsonList = new ArrayList();
        List cacheList = redisHelp.getList(this.dataprefix+"_"+"selectContents_"+page+"_"+limit+"_"+searchParams,redisTemplate);

        try{
            if(cacheList.size()>0){
                jsonList = cacheList;
            }else{
                //首先查询typechoRelationships获取映射关系
                PageList<TypechoRelationships> pageList = relationshipsService.selectPage(query, page, limit);
                List<TypechoRelationships> list = pageList.getList();

                for (int i = 0; i < list.size(); i++) {
                    Integer cid = list.get(i).getCid();
                    TypechoContents typechoContents = list.get(i).getContents();
                    Map contentsInfo = JSONObject.parseObject(JSONObject.toJSONString(typechoContents), Map.class);
                    //写入作者详细信息
                    Integer uid = typechoContents.getAuthorId();
                    if(uid>0){
                        TypechoUsers author = usersService.selectByKey(uid);
                        Map authorInfo = new HashMap();
                        String name = author.getName();
                        if(author.getScreenName()!=""){
                            name = author.getScreenName();
                        }
                        String avatar = this.avatar + "null";
                        if(author.getMail()!=""){
                            avatar = baseFull.getAvatar(this.avatar, author.getMail());
                        }
                        authorInfo.put("name",name);
                        authorInfo.put("avatar",avatar);
                        authorInfo.put("customize",author.getCustomize());
                        //判断是否为VIP
                        authorInfo.put("isvip", 0);
                        Long date = System.currentTimeMillis();
                        String curTime = String.valueOf(date).substring(0, 10);
                        Integer viptime  = author.getVip();

                        if(viptime>Integer.parseInt(curTime)||viptime.equals(1)){
                            authorInfo.put("isvip", 1);
                        }
                        contentsInfo.put("authorInfo",authorInfo);
                    }

                    //处理文章内容为简介

                    String text = contentsInfo.get("text").toString();
                    List imgList = baseFull.getImageSrc(text);
                    text = baseFull.toStrByChinese(text);
                    contentsInfo.put("text",text.length()>200 ? text.substring(0,200) : text);
                    contentsInfo.put("images",imgList);
                    //加入自定义字段，分类和标签
                    //加入自定义字段信息，这里取消注释即可开启，但是数据库查询会消耗性能
                    List<TypechoFields> fields = fieldsService.selectByKey(cid);
                    contentsInfo.put("fields",fields);

                    List<TypechoRelationships> relationships = relationshipsService.selectByKey(cid);

                    List metas = new ArrayList();
                    List tags = new ArrayList();
                    for (int j = 0; j < relationships.size(); j++) {
                        Map info = JSONObject.parseObject(JSONObject.toJSONString(relationships.get(j)), Map.class);
                        if(info!=null){
                            String mid = info.get("mid").toString();

                            TypechoMetas metasList  = service.selectByKey(mid);
                            Map metasInfo = JSONObject.parseObject(JSONObject.toJSONString(metasList), Map.class);
                            String type = metasInfo.get("type").toString();
                            if(type.equals("category")){
                                metas.add(metasInfo);
                            }
                            if(type.equals("tag")){
                                tags.add(metasInfo);
                            }
                        }

                    }

                    contentsInfo.remove("password");
                    contentsInfo.put("category",metas);
                    contentsInfo.put("tag",tags);

                    jsonList.add(contentsInfo);


                    //存入redis

                }
                redisHelp.delete(this.dataprefix+"_"+"selectContents_"+page+"_"+limit+"_"+searchParams,redisTemplate);
                redisHelp.setList(this.dataprefix+"_"+"selectContents_"+page+"_"+limit+"_"+searchParams,jsonList,this.contentCache,redisTemplate);
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
     * 查询分类和标签
     * @param searchParams Bean对象JSON字符串
     * @param page         页码
     * @param limit        每页显示数量
     */
    @RequestMapping(value = "/metasList")
    @ResponseBody
    public String metasList (@RequestParam(value = "searchParams", required = false) String  searchParams,
                            @RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                            @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit,
                             @RequestParam(value = "searchKey"        , required = false, defaultValue = "") String searchKey,
                             @RequestParam(value = "order"        , required = false, defaultValue = "") String order) {
        TypechoMetas query = new TypechoMetas();
        if(limit>50){
            limit = 50;
        }
        if (StringUtils.isNotBlank(searchParams)) {
            JSONObject object = JSON.parseObject(searchParams);
            query = object.toJavaObject(TypechoMetas.class);
        }

        PageList<TypechoMetas> pageList = service.selectPage(query, page, limit,searchKey,order);
        JSONObject response = new JSONObject();
        response.put("code" , 1);
        response.put("msg"  , "");
        response.put("data" , null != pageList.getList() ? pageList.getList() : new JSONArray());
        response.put("count", pageList.getTotalCount());
        return response.toString();
    }
    /***
     * 查询分类详情
     */
    @RequestMapping(value = "/metaInfo")
    @ResponseBody
    public String metaInfo(@RequestParam(value = "key", required = false) String key) {
        try{
            TypechoMetas metas = service.selectByKey(key);
            Map json = JSONObject.parseObject(JSONObject.toJSONString(metas), Map.class);
            JSONObject response = new JSONObject();

            response.put("code", 1);
            response.put("msg", "");
            response.put("data", json);

            return response.toString();
        }catch (Exception e){
            JSONObject response = new JSONObject();
            response.put("code", 1);
            response.put("msg", "");
            response.put("data", null);

            return response.toString();
        }
    }
    /***
     * 修改分类和标签
     */
    @RequestMapping(value = "/editMeta")
    @ResponseBody
    public String editMeta(@RequestParam(value = "params", required = false) String  params,@RequestParam(value = "token", required = false) String  token) {
        try{
            Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
            if (uStatus == 0) {
                return Result.getResultJson(0, "用户未登录或Token验证失败", null);
            }
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            String group = map.get("group").toString();
            if (!group.equals("administrator")) {
                return Result.getResultJson(0, "你没有操作权限", null);
            }
            TypechoMetas update = new TypechoMetas();
            Map jsonToMap =null;
            if (StringUtils.isNotBlank(params)) {
                jsonToMap =  JSONObject.parseObject(JSON.parseObject(params).toString());
                //为了数据稳定性考虑，禁止修改类型
                jsonToMap.remove("type");
                update = JSON.parseObject(JSON.toJSONString(jsonToMap), TypechoMetas.class);
            }

            int rows = service.update(update);
            JSONObject response = new JSONObject();
            response.put("code" , rows);
            response.put("msg"  , rows > 0 ? "操作成功" : "操作失败");
            return response.toString();
        }catch (Exception e){
            JSONObject response = new JSONObject();
            response.put("code" , 0);
            response.put("msg"  , "操作失败");
            return response.toString();
        }

    }

}
