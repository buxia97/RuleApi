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
    private TypechoApiconfigService apiconfigService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${webinfo.contentCache}")
    private Integer contentCache;

    @Value("${web.prefix}")
    private String dataprefix;


    RedisHelp redisHelp =new RedisHelp();
    ResultAll Result = new ResultAll();
    baseFull baseFull = new baseFull();
    UserStatus UStatus = new UserStatus();
    EditFile editFile = new EditFile();
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
        String sqlParams = "null";
        if (StringUtils.isNotBlank(searchParams)) {
            JSONObject object = JSON.parseObject(searchParams);
            Integer mid = 0;
            if(object.get("mid")!=null){
                mid = Integer.parseInt(object.get("mid").toString());
            }

            query.setMid(mid);
            TypechoContents contents = new TypechoContents();
            contents.setType("post");
            contents.setStatus("publish");
            query.setContents(contents);
            Map paramsJson = JSONObject.parseObject(JSONObject.toJSONString(query), Map.class);
            sqlParams = paramsJson.toString();
        }
        List jsonList = new ArrayList();
        List cacheList = redisHelp.getList(this.dataprefix+"_"+"selectContents_"+page+"_"+limit+"_"+sqlParams,redisTemplate);

        try{
            if(cacheList.size()>0){
                jsonList = cacheList;
            }else{
                TypechoApiconfig apiconfig = UStatus.getConfig(this.dataprefix,apiconfigService,redisTemplate);
                //首先查询typechoRelationships获取映射关系
                PageList<TypechoRelationships> pageList = relationshipsService.selectPage(query, page, limit);
                List<TypechoRelationships> list = pageList.getList();
                if(list.size() < 1){
                    JSONObject noData = new JSONObject();
                    noData.put("code" , 1);
                    noData.put("msg"  , "");
                    noData.put("data" , new ArrayList());
                    noData.put("count", 0);
                    return noData.toString();
                }
                for (int i = 0; i < list.size(); i++) {
                    Integer cid = list.get(i).getCid();
                    TypechoContents typechoContents = list.get(i).getContents();
                    Map contentsInfo = JSONObject.parseObject(JSONObject.toJSONString(typechoContents), Map.class);
                    //写入作者详细信息
                    Integer uid = typechoContents.getAuthorId();
                    if(uid>0){
                        TypechoUsers author = usersService.selectByKey(uid);
                        Map authorInfo = new HashMap();
                        if(author!=null){
                            String name = author.getName();
                            if(author.getScreenName()!=""&&author.getScreenName()!=null){
                                name = author.getScreenName();
                            }
                            String avatar = apiconfig.getWebinfoAvatar() + "null";
                            if(author.getAvatar()!=""&&author.getAvatar()!=null){
                                avatar = author.getAvatar();
                            }else{
                                if(author.getMail()!=""&&author.getMail()!=null){
                                    String mail = author.getMail();

                                    if(mail.indexOf("@qq.com") != -1){
                                        String qq = mail.replace("@qq.com","");
                                        avatar = "https://q1.qlogo.cn/g?b=qq&nk="+qq+"&s=640";
                                    }else{
                                        avatar = baseFull.getAvatar(apiconfig.getWebinfoAvatar(), author.getMail());
                                    }
                                    //avatar = baseFull.getAvatar(apiconfig.getWebinfoAvatar(), author.getMail());
                                }
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
                            if(viptime.equals(1)){
                                //永久VIP
                                authorInfo.put("isvip", 2);
                            }
                        }else{
                            authorInfo.put("name","用户已注销");
                            authorInfo.put("avatar",apiconfig.getWebinfoAvatar() + "null");
                        }
                        contentsInfo.put("authorInfo",authorInfo);
                    }

                    //处理文章内容为简介

                    String text = contentsInfo.get("text").toString();
                    boolean status = text.contains("<!--markdown-->");
                    if(status){
                        contentsInfo.put("markdown",1);
                    }else{
                        contentsInfo.put("markdown",0);
                    }
                    List imgList = baseFull.getImageSrc(text);
                    text = baseFull.toStrByChinese(text);
                    contentsInfo.put("text",text.length()>400 ? text.substring(0,400) : text);

                    contentsInfo.put("images",imgList);
                    //加入自定义字段，分类和标签
                    //加入自定义字段信息，这里取消注释即可开启，但是数据库查询会消耗性能
                    TypechoFields f = new TypechoFields();
                    f.setCid(cid);
                    List<TypechoFields> fields = fieldsService.selectList(f);
                    contentsInfo.put("fields",fields);

                    TypechoRelationships rs = new TypechoRelationships();
                    rs.setCid(cid);
                    List<TypechoRelationships> relationships = relationshipsService.selectList(rs);

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
                redisHelp.delete(this.dataprefix+"_"+"selectContents_"+page+"_"+limit+"_"+sqlParams,redisTemplate);
                redisHelp.setList(this.dataprefix+"_"+"selectContents_"+page+"_"+limit+"_"+sqlParams,jsonList,this.contentCache,redisTemplate);
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
        String sqlParams = "null";
        if(limit>50){
            limit = 50;
        }
        Integer total = 0;
        List jsonList = new ArrayList();

        if (StringUtils.isNotBlank(searchParams)) {
            JSONObject object = JSON.parseObject(searchParams);
            query = object.toJavaObject(TypechoMetas.class);
            Map paramsJson = JSONObject.parseObject(JSONObject.toJSONString(query), Map.class);
            sqlParams = paramsJson.toString();
        }
        List cacheList = redisHelp.getList(this.dataprefix+"_"+"metasList_"+page+"_"+limit+"_"+searchKey+"_"+sqlParams,redisTemplate);

        total = service.total(query);
        try {
            if (cacheList.size() > 0) {
                jsonList = cacheList;
            } else {
                PageList<TypechoMetas> pageList = service.selectPage(query, page, limit, searchKey, order);
                jsonList = pageList.getList();
                if(jsonList.size() < 1){
                    JSONObject noData = new JSONObject();
                    noData.put("code" , 1);
                    noData.put("msg"  , "");
                    noData.put("data" , new ArrayList());
                    noData.put("count", 0);
                    return noData.toString();
                }
                redisHelp.delete(this.dataprefix+"_"+"metasList_"+page+"_"+limit+"_"+searchKey+"_"+sqlParams,redisTemplate);
                redisHelp.setList(this.dataprefix+"_"+"metasList_"+page+"_"+limit+"_"+searchKey+"_"+sqlParams,jsonList,10,redisTemplate);
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
     * 查询分类详情
     */
    @RequestMapping(value = "/metaInfo")
    @ResponseBody
    public String metaInfo(@RequestParam(value = "key", required = false) String key,@RequestParam(value = "slug", required = false) String slug) {
        try{
            Map metaInfoJson = new HashMap<String, String>();
            Map cacheInfo = redisHelp.getMapValue(this.dataprefix+"_"+"metaInfo_"+key+"_"+slug,redisTemplate);

            if(cacheInfo.size()>0){
                metaInfoJson = cacheInfo;
            }else{
                TypechoMetas metas;
                //优先处理slug
                if(slug!=null){
                    metas = service.selectBySlug(slug);
                }else{
                    metas = service.selectByKey(key);
                }
                metaInfoJson = JSONObject.parseObject(JSONObject.toJSONString(metas), Map.class);
                redisHelp.delete(this.dataprefix+"_"+"metaInfo_"+key+"_"+slug,redisTemplate);
                redisHelp.setKey(this.dataprefix+"_"+"metaInfo_"+key+"_"+slug,metaInfoJson,20,redisTemplate);
            }

            JSONObject response = new JSONObject();

            response.put("code", 1);
            response.put("msg", "");
            response.put("data", metaInfoJson);

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
            String logUid = map.get("uid").toString();
            TypechoMetas update = new TypechoMetas();
            Map jsonToMap =null;
            if (StringUtils.isNotBlank(params)) {
                jsonToMap =  JSONObject.parseObject(JSON.parseObject(params).toString());
                //为了数据稳定性考虑，禁止修改类型
                jsonToMap.remove("type");
                update = JSON.parseObject(JSON.toJSONString(jsonToMap), TypechoMetas.class);
            }

            int rows = service.update(update);
            editFile.setLog("管理员"+logUid+"请求修改分类"+jsonToMap.get("mid").toString());
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
    /***
     * 修改分类和标签
     */
    @RequestMapping(value = "/addMeta")
    @ResponseBody
    public String addMeta(@RequestParam(value = "params", required = false) String  params,@RequestParam(value = "token", required = false) String  token) {
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
            String logUid = map.get("uid").toString();
            TypechoMetas insert = new TypechoMetas();
            Map jsonToMap =null;
            if (StringUtils.isNotBlank(params)) {
                jsonToMap =  JSONObject.parseObject(JSON.parseObject(params).toString());
                String type = jsonToMap.get("type").toString();
                if(!type.equals("category")&&!type.equals("tag")){
                    return Result.getResultJson(0, "类型参数不正确", null);
                }
                //为了数据稳定性考虑，禁止修改类型
                insert = JSON.parseObject(JSON.toJSONString(jsonToMap), TypechoMetas.class);
            }
            //判断是否存在相同的分类或标签名称
            TypechoMetas oldMeta = new TypechoMetas();
            oldMeta.setName(insert.getName());
            oldMeta.setType(insert.getType());
            Integer isHave = service.total(oldMeta);
            if(isHave>0){
                return Result.getResultJson(0, "已存在同名数据", null);
            }

            int rows = service.insert(insert);
            editFile.setLog("管理员"+logUid+"请求添加分类");
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
