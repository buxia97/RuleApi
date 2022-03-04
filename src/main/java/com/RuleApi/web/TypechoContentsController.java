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
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 * 控制层
 * TypechoContentsController
 * @author buxia97
 * @date 2021/11/29
 */
@Component
@Controller
@RequestMapping(value = "/typechoContents")
public class TypechoContentsController {

    @Autowired
    TypechoContentsService service;

    @Autowired
    private TypechoFieldsService fieldsService;

    @Autowired
    private TypechoRelationshipsService relationshipsService;

    @Autowired
    private TypechoMetasService metasService;

    @Autowired
    private TypechoUsersService usersService;

    @Autowired
    private TypechoCommentsService commentsService;

    @Autowired
    private TypechoShopService shopService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MailService MailService;

    @Value("${webinfo.contentCache}")
    private Integer contentCache;

    @Value("${webinfo.contentInfoCache}")
    private Integer contentInfoCache;

    @Value("${webinfo.pexelsKey}")
    private String pexelsKey;

    @Value("${web.prefix}")
    private String dataprefix;

    @Value("${webinfo.title}")
    private String webTitle;

    @Value("${webinfo.url}")
    private String webUrl;

    RedisHelp redisHelp =new RedisHelp();
    ResultAll Result = new ResultAll();
    baseFull baseFull = new baseFull();
    UserStatus UStatus = new UserStatus();
    HttpClient HttpClient = new HttpClient();

    /**
     * 查询文章详情
     *
     */
    @RequestMapping(value = "/contentsInfo")
    @ResponseBody
    public String contentsInfo (@RequestParam(value = "key", required = false) String  key,@RequestParam(value = "isMd" , required = false, defaultValue = "0") Integer isMd,@RequestParam(value = "token", required = false) String  token,HttpServletRequest request) {
        TypechoContents typechoContents = null;
        Map contensjson = new HashMap<String, String>();
        Map cacheInfo = redisHelp.getMapValue(this.dataprefix+"_"+"contentsInfo_"+key+"_"+isMd,redisTemplate);

        try{
            Integer isLogin;
            Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
            if(uStatus==0){
                isLogin=0;
            }else{
                isLogin=1;
            }
            //如果是登录用户，且传入了token，就不缓存
            if(cacheInfo.size()>0&isLogin==0){
                contensjson = cacheInfo;
            }else{
                typechoContents = service.selectByKey(key);
                if(typechoContents==null){
                    return Result.getResultJson(0,"该文章不存在",null);
                }
                String text = typechoContents.getText();
                //要做处理将typecho的图片插入格式变成markdown
                List imgList = baseFull.getImageSrc(text);
                List codeList = baseFull.getImageCode(text);



                for(int c = 0; c < codeList.size(); c++){
                    String codeimg = codeList.get(c).toString();
                    String urlimg = imgList.get(c).toString();
                    text=text.replace(codeimg,"![image"+c+"]("+urlimg+")");
                }
                text=text.replace("<!--markdown-->","");
                List codeImageMk = baseFull.getImageMk(text);
                for(int d = 0; d < codeImageMk.size(); d++){
                    String mk = codeImageMk.get(d).toString();
                    text=text.replace(mk,"");
                }
                if(isMd==1){
                    //如果isMd等于1，则输出解析后的md代码
                    Parser parser = Parser.builder().build();
                    Node document = parser.parse(text);
                    HtmlRenderer renderer = HtmlRenderer.builder().build();
                    text = renderer.render(document);

                }
                //获取文章id，从而获取自定义字段，和分类标签
                String cid = typechoContents.getCid().toString();
                List<TypechoFields> fields = fieldsService.selectByKey(cid);
                List<TypechoRelationships> relationships = relationshipsService.selectByKey(cid);

                List metas = new ArrayList();
                List tags = new ArrayList();
                for (int i = 0; i < relationships.size(); i++) {
                    Map json = JSONObject.parseObject(JSONObject.toJSONString(relationships.get(i)), Map.class);
                    if(json!=null){
                        String mid = json.get("mid").toString();
                        TypechoMetas metasList  = metasService.selectByKey(mid);
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
                contensjson = JSONObject.parseObject(JSONObject.toJSONString(typechoContents), Map.class);

                //转为map，再加入字段
                contensjson.remove("password");
                contensjson.put("images",imgList);
                contensjson.put("fields",fields);
                contensjson.put("category",metas);
                contensjson.put("tag",tags);
                contensjson.put("text",text);

                //文章阅读量增加
                String  agent =  request.getHeader("User-Agent");
                String  ip = baseFull.getIpAddr(request);
                String isRead = redisHelp.getRedis(this.dataprefix+"_"+"isRead"+"_"+ip+"_"+agent+"_"+key,redisTemplate);
                if(isRead==null){
                   //添加阅读量
                    Integer views = Integer.parseInt(contensjson.get("views").toString());
                    views = views + 1;
                    TypechoContents toContents = new TypechoContents();
                    toContents.setCid(Integer.parseInt(key));
                    toContents.setViews(views);
                    service.update(toContents);

                }
                redisHelp.setRedis(this.dataprefix+"_"+"isRead"+"_"+ip+"_"+agent+"_"+key,"yes",900,redisTemplate);
            }

        }catch (Exception e){
            if(cacheInfo.size()>0){
                contensjson = cacheInfo;
            }
        }
        redisHelp.delete(this.dataprefix+"_"+"contentsInfo_"+key+"_"+isMd,redisTemplate);
        redisHelp.setKey(this.dataprefix+"_"+"contentsInfo_"+key+"_"+isMd,contensjson,this.contentInfoCache,redisTemplate);
        JSONObject concentInfo = JSON.parseObject(JSON.toJSONString(contensjson),JSONObject.class);
        return concentInfo.toJSONString();
        //return new ApiResult<>(ResultCode.success.getCode(), typechoContents, ResultCode.success.getDescr(), request.getRequestURI());
    }


    /***
     * 表单查询请求
     * @param searchParams Bean对象JSON字符串
     * @param page         页码
     * @param limit        每页显示数量
     */
    @RequestMapping(value = "/contensList")
    @ResponseBody
    public String contensList (@RequestParam(value = "searchParams", required = false) String  searchParams,
                            @RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                            @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit,
                            @RequestParam(value = "searchKey"        , required = false, defaultValue = "") String searchKey,
                            @RequestParam(value = "order"        , required = false, defaultValue = "") String order,
                               @RequestParam(value = "random"        , required = false, defaultValue = "0") Integer random,
                               @RequestParam(value = "token"        , required = false, defaultValue = "") String token){
        TypechoContents query = new TypechoContents();
        String aid = "null";
        if (StringUtils.isNotBlank(searchParams)) {
            JSONObject object = JSON.parseObject(searchParams);
            //如果不是登陆状态，那么只显示开放状态文章。如果是，则查询自己发布的文章
            Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
            if(token==""||uStatus==0){

                object.put("status","publish");
            }else{
                aid = redisHelp.getValue(this.dataprefix+"_"+"userInfo"+token,"uid",redisTemplate).toString();
                Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
                String group = map.get("group").toString();
                if(!group.equals("administrator")){
                    object.put("authorId",aid);
                }

            }

            query = object.toJavaObject(TypechoContents.class);

        }
        List jsonList = new ArrayList();

        List cacheList = redisHelp.getList(this.dataprefix+"_"+"contensList_"+page+"_"+limit+"_"+searchParams+"_"+order+"_"+searchKey+"_"+random+"_"+aid,redisTemplate);
        //监听异常，如果有异常则调用redis缓存中的list，如果无异常也调用redis，但是会更新数据
        try{
            if(cacheList.size()>0){
                jsonList = cacheList;
            }else{
                PageList<TypechoContents> pageList = service.selectPage(query, page, limit, searchKey,order,random);
                List list = pageList.getList();
                for (int i = 0; i < list.size(); i++) {
                    Map json = JSONObject.parseObject(JSONObject.toJSONString(list.get(i)), Map.class);
                    //加入自定义字段信息，这里取消注释即可开启，但是数据库查询会消耗性能
                    String cid = json.get("cid").toString();
                    List<TypechoFields> fields = fieldsService.selectByKey(cid);
                    json.put("fields",fields);

                    List<TypechoRelationships> relationships = relationshipsService.selectByKey(cid);

                    List metas = new ArrayList();
                    List tags = new ArrayList();
                    for (int j = 0; j < relationships.size(); j++) {
                        Map info = JSONObject.parseObject(JSONObject.toJSONString(relationships.get(j)), Map.class);
                        if(info!=null){
                            String mid = info.get("mid").toString();

                            TypechoMetas metasList  = metasService.selectByKey(mid);
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


                    String text = json.get("text").toString();
                    List imgList = baseFull.getImageSrc(text);
                    text=text.replaceAll("(\\\r\\\n|\\\r|\\\n|\\\n\\\r)", "");
                    text=text.replaceAll("\\s*", "");
                    text=text.replaceAll("</?[^>]+>", "");
                    //去掉文章开头的图片插入
                    text=text.replaceAll("((!\\[)[\\s\\S]+?(\\]\\[)[\\s\\S]+?(\\]))+?","");
                    json.put("images",imgList);
                    json.put("text",text.length()>200 ? text.substring(0,200) : text);
                    json.put("category",metas);
                    json.put("tag",tags);
                    json.remove("password");



                    jsonList.add(json);
                    redisHelp.delete(this.dataprefix+"_"+"contensList_"+page+"_"+limit+"_"+searchParams+"_"+order+"_"+searchKey+"_"+random+"_"+aid,redisTemplate);
                    redisHelp.setList(this.dataprefix+"_"+"contensList_"+page+"_"+limit+"_"+searchParams+"_"+order+"_"+searchKey+"_"+random+"_"+aid,jsonList,this.contentCache,redisTemplate);
                }
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
     * 发布文章
     * @param params Bean对象JSON字符串
     */
    @RequestMapping(value = "/contensAdd")
    @ResponseBody
    public String contensAdd(@RequestParam(value = "params", required = false) String  params, @RequestParam(value = "token", required = false) String  token) {
        try {
            TypechoContents insert = null;
            Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
            Map jsonToMap =null;
            String category = "";
            String tag = "";
            Integer sid = -1;
            if(uStatus==0){
                return Result.getResultJson(0,"用户未登录或Token验证失败",null);
            }
            if (StringUtils.isNotBlank(params)) {
                jsonToMap =  JSONObject.parseObject(JSON.parseObject(params).toString());
                //获取发布者信息
                Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
                String uid = map.get("uid").toString();
                //判断用户是否绑定了邮箱
                TypechoUsers users = usersService.selectByKey(uid);
                if(users.getMail()==null){
                    return Result.getResultJson(0,"发布文章前，请先绑定邮箱",null);
                }
                //生成typecho数据库格式的创建时间戳
                Long date = System.currentTimeMillis();
                String userTime = String.valueOf(date).substring(0,10);
                //获取商品id
                if(jsonToMap.get("sid")!=null){
                    sid = Integer.parseInt(jsonToMap.get("sid").toString());
                }


                //获取参数中的分类和标签
                if(jsonToMap.get("category")==null){
                    jsonToMap.put("category","0");
                }
                category = jsonToMap.get("category").toString();
                if(jsonToMap.get("tag")!=null){
                    tag = jsonToMap.get("tag").toString();
                }
                if(jsonToMap.get("text")==null){
                    jsonToMap.put("text","暂无内容");
                }

                //写入创建时间和作者
                jsonToMap.put("created",userTime);
                jsonToMap.put("authorId",uid);
                //除管理员外，文章默认待审核
                Map userMap =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
                String group = userMap.get("group").toString();
                if(!group.equals("administrator")){
                    jsonToMap.put("status","waiting");
                }else{
                    jsonToMap.put("status","publish");
                }

                //部分字段不允许定义
                jsonToMap.put("type","post");
                jsonToMap.put("commentsNum",0);
                jsonToMap.put("allowPing",1);
                jsonToMap.put("allowFeed",1);
                jsonToMap.put("allowComment",1);
                jsonToMap.put("orderKey",0);
                jsonToMap.put("parent",0);
                jsonToMap.remove("password");
                jsonToMap.remove("sid");
                insert = JSON.parseObject(JSON.toJSONString(jsonToMap), TypechoContents.class);

            }

            int rows = service.insert(insert);

            Integer cid = insert.getCid();
            //文章添加完成后，再处理分类和标签还有挂载商品
            if(rows > 0) {
                if (category != "") {
                    Integer result = category.indexOf(",");
                    if (result != -1) {
                        String[] categoryList = category.split(",");
                        List list = Arrays.asList(baseFull.threeClear(categoryList));
                        for (int v = 0; v < list.size(); v++) {
                            TypechoRelationships toCategory = new TypechoRelationships();
                            String id = list.get(v).toString();
                            if(!id.equals("")){
                                Integer mid = Integer.parseInt(id);
                                toCategory.setCid(cid);
                                toCategory.setMid(mid);
                                List<TypechoRelationships> cList = relationshipsService.selectList(toCategory);
                                if (cList.size() == 0) {
                                    relationshipsService.insert(toCategory);
                                }
                            }
                        }
                    }
                }
                if (tag != "") {
                    Integer result = tag.indexOf(",");
                    if (result != -1) {
                        String[] tagList = tag.split(",");
                        List list = Arrays.asList(baseFull.threeClear(tagList));
                        for (int v = 0; v < list.size(); v++) {
                            TypechoRelationships toTag = new TypechoRelationships();
                            String id = list.get(v).toString();
                            if(!id.equals("")){
                                Integer mid = Integer.parseInt(id);
                                toTag.setCid(cid);
                                toTag.setMid(mid);
                                List<TypechoRelationships> mList = relationshipsService.selectList(toTag);
                                if (mList.size() == 0) {
                                    relationshipsService.insert(toTag);
                                }
                            }
                        }
                    }
                }


                //处理完分类标签后，处理挂载的商品
                if(sid>-1){
                    Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
                    Integer uid  = Integer.parseInt(map.get("uid").toString());
                    //判断商品是不是自己的
                    TypechoShop shop = new TypechoShop();
                    shop.setUid(uid);
                    shop.setId(sid);
                    Integer num  = shopService.total(shop);
                    if(num >= 1){
                        shop.setCid(cid);
                        shopService.update(shop);
                    }
                }


            }


            JSONObject response = new JSONObject();
            response.put("code" ,rows > 0 ? 1: 0 );
            response.put("data" , rows);
            response.put("msg"  , rows > 0 ? "添加成功" : "添加失败");
            return response.toString();
        }catch (Exception e){
            return Result.getResultJson(0,"添加失败",null);
        }
    }

    /***
     * 文章修改
     * @param params Bean对象JSON字符串
     */
    @RequestMapping(value = "/contensUpdate")
    @ResponseBody
    public String contensUpdate(@RequestParam(value = "params", required = false) String  params, @RequestParam(value = "token", required = false) String  token) {

        try {
            TypechoContents update = null;
            Map jsonToMap =null;
            String category = "";
            String tag = "";
            Integer sid = -1;
            Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
            if(uStatus==0){
                return Result.getResultJson(0,"用户未登录或Token验证失败",null);
            }
            if (StringUtils.isNotBlank(params)) {
                jsonToMap =  JSONObject.parseObject(JSON.parseObject(params).toString());
                //生成typecho数据库格式的修改时间戳
                Long date = System.currentTimeMillis();
                String userTime = String.valueOf(date).substring(0,10);
                jsonToMap.put("modified",userTime);
                //获取商品id
                if(jsonToMap.get("sid")!=null){
                    sid = Integer.parseInt(jsonToMap.get("sid").toString());
                }

                //验证用户是否为作品的作者，以及权限
                Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
                Integer uid =Integer.parseInt(map.get("uid").toString());
                String group = map.get("group").toString();
                if(!group.equals("administrator")){
                    TypechoContents info = service.selectByKey(jsonToMap.get("cid").toString());
                    Integer authorId = info.getAuthorId();
                    if(uid!=authorId){
                        return Result.getResultJson(0,"你无权操作此文章",null);
                    }
                }



                //获取参数中的分类和标签（暂时不允许定义）
                if(jsonToMap.get("category")==null){
                    jsonToMap.put("category","0,");
                }
                category = jsonToMap.get("category").toString();
                if(jsonToMap.get("tag")!=null){
                    tag = jsonToMap.get("tag").toString();
                }
                if(jsonToMap.get("text")==null){
                    jsonToMap.put("text","暂无内容");
                }


                //部分字段不允许定义
                jsonToMap.remove("authorId");
                jsonToMap.remove("commentsNum");
                jsonToMap.remove("allowPing");
                jsonToMap.remove("allowFeed");
                jsonToMap.remove("allowComment");
                jsonToMap.remove("password");
                jsonToMap.remove("orderKey");
                jsonToMap.remove("parent");
                jsonToMap.remove("created");
                jsonToMap.remove("slug");
                jsonToMap.remove("views");
                jsonToMap.remove("likes");
                jsonToMap.remove("sid");
                jsonToMap.remove("type");
                //状态重新变成待审核
                if(!group.equals("administrator")){
                    jsonToMap.put("status","waiting");
                }

                update = JSON.parseObject(JSON.toJSONString(jsonToMap), TypechoContents.class);
            }

            int rows = service.update(update);
            //处理标签和分类
            Integer cid = Integer.parseInt(jsonToMap.get("cid").toString());
            //删除原本的分类标签映射，反正都会更新，那就一起更新
            relationshipsService.delete(cid);

            //文章添加完成后，再处理分类和标签，只有文章能设置标签和分类
            if(rows > 0){
                if (category != "") {
                    Integer result = category.indexOf(",");
                    if (result != -1) {
                        String[] categoryList = category.split(",");
                        List list = Arrays.asList(baseFull.threeClear(categoryList));
                        for (int v = 0; v < list.size(); v++) {
                            TypechoRelationships toCategory = new TypechoRelationships();
                            String id = list.get(v).toString();
                            if(!id.equals("")){
                                //如果不存在就添加
                                Integer mid = Integer.parseInt(id);
                                toCategory.setCid(cid);
                                toCategory.setMid(mid);
                                relationshipsService.insert(toCategory);
                            }
                        }
                    }
                }
                if (tag != "") {
                    Integer result = tag.indexOf(",");
                    if (result != -1) {
                        String[] tagList = tag.split(",");
                        List list = Arrays.asList(baseFull.threeClear(tagList));
                        for (int v = 0; v < list.size(); v++) {
                            TypechoRelationships toTag = new TypechoRelationships();
                            String id = list.get(v).toString();
                            if(!id.equals("")){
                                Integer mid = Integer.parseInt(id);
                                toTag.setCid(cid);
                                toTag.setMid(mid);
                                relationshipsService.insert(toTag);
                            }
                        }
                    }
                }

                //处理完分类标签后，处理挂载的商品
                if(sid>-1){
                    Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
                    Integer uid  = Integer.parseInt(map.get("uid").toString());
                    //判断商品是不是自己的
                    TypechoShop shop = new TypechoShop();
                    shop.setUid(uid);
                    shop.setId(sid);
                    Integer num  = shopService.total(shop);
                    if(num >= 1){
                        shop.setCid(cid);
                        shopService.update(shop);
                    }
                }
            }


            JSONObject response = new JSONObject();
            response.put("code" ,rows > 0 ? 1: 0 );
            response.put("data" , rows);
            response.put("msg"  , rows > 0 ? "修改成功" : "修改失败");
            return response.toString();
        }catch (Exception e){
            return Result.getResultJson(0,"修改失败",null);
        }
    }

    /***
     * 文章删除
     */
    @RequestMapping(value = "/contentsDelete")
    @ResponseBody
    public String formDelete(@RequestParam(value = "key", required = false) String  key, @RequestParam(value = "token", required = false) String  token) {
        try {
            Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
            if(uStatus==0){
                return Result.getResultJson(0,"用户未登录或Token验证失败",null);
            }
            //String group = (String) redisHelp.getValue("userInfo"+token,"group",redisTemplate);
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            String group = map.get("group").toString();
            if(!group.equals("administrator")){
                return Result.getResultJson(0,"你没有操作权限",null);
            }
            int rows = service.delete(key);
            //删除与分类的映射
            int st = relationshipsService.delete(key);
            JSONObject response = new JSONObject();
            response.put("code" ,rows > 0 ? 1: 0 );
            response.put("data" , rows);
            response.put("msg"  , rows > 0 ? "操作成功，缓存缘故，数据可能存在延迟" : "操作失败");
            return response.toString();
        }catch (Exception e){
            return Result.getResultJson(0,"操作失败",null);
        }
    }
    /***
     * 文章审核
     */
    @RequestMapping(value = "/contentsAudit")
    @ResponseBody
    public String contensAudit(@RequestParam(value = "key", required = false) String  key, @RequestParam(value = "token", required = false) String  token) {
        try {
            Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
            if(uStatus==0){
                return Result.getResultJson(0,"用户未登录或Token验证失败",null);
            }
            //String group = (String) redisHelp.getValue("userInfo"+token,"group",redisTemplate);
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            String group = map.get("group").toString();
            if(!group.equals("administrator")){
                return Result.getResultJson(0,"你没有操作权限",null);
            }
            TypechoContents info = service.selectByKey(key);
            info.setCid(Integer.parseInt(key));
            info.setStatus("publish");
            Integer rows = service.update(info);
            //给作者发送邮件

            TypechoUsers ainfo = usersService.selectByKey(info.getAuthorId());
            String title =info.getTitle();
            Integer uid = ainfo.getUid();
            if(ainfo.getMail()!=null){
                String email = ainfo.getMail();
                MailService.send("用户："+uid+",您的文章已审核通过", "<!DOCTYPE html><html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" /><title></title><meta charset=\"utf-8\" /><style>*{padding:0px;margin:0px;box-sizing:border-box;}html{box-sizing:border-box;}body{font-size:15px;background:#fff}.main{margin:20px auto;max-width:500px;border:solid 1px #2299dd;overflow:hidden;}.main h1{display:block;width:100%;background:#2299dd;font-size:18px;color:#fff;text-align:center;padding:15px;}.text{padding:30px;}.text p{margin:10px 0px;line-height:25px;}.text p span{color:#2299dd;font-weight:bold;font-size:22px;margin-left:5px;}</style></head>" +
                                "<body><div class=\"main\"><h1>文章审核</h1><div class=\"text\"><p>用户 "+uid+"，你的文章<"+title+">已经审核通过！</p>" +
                                "<p>可前往<a href=\""+this.webUrl+"\">"+this.webTitle+"</a>查看详情</p></div></div></body></html>",
                        new String[] {email}, new String[] {});
            }



            JSONObject response = new JSONObject();
            response.put("code" ,rows > 0 ? 1: 0 );
            response.put("data" , rows);
            response.put("msg"  , rows > 0 ? "操作成功，缓存缘故，数据可能存在延迟" : "操作失败");
            return response.toString();
        }catch (Exception e){
            return Result.getResultJson(0,"操作失败",null);
        }
    }
    /**
     * 文章是否评论过（用于回复可见）
     * */
    @RequestMapping(value = "/isCommnet")
    @ResponseBody
    public String isCommnet(@RequestParam(value = "key", required = false) String  key, @RequestParam(value = "token", required = false) String  token) {
        try {
            Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
            if(uStatus==0){
                return Result.getResultJson(0,"用户未登录或Token验证失败",null);
            }
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer uid  = Integer.parseInt(map.get("uid").toString());
            //首先判断是否为作者自己
            TypechoContents contents = new TypechoContents();
            contents.setCid(Integer.parseInt(key));
            contents.setAuthorId(uid);
            Integer isAuthor = service.total(contents);
            if(isAuthor>0){
                return Result.getResultJson(1,"",null);
            }
            TypechoComments comments = new TypechoComments();
            comments.setCid(Integer.parseInt(key));
            comments.setAuthorId(uid);
            Integer isCommnet = commentsService.total(comments);
            if(isCommnet>0){
                return Result.getResultJson(1,"",null);
            }else{
                return Result.getResultJson(0,"",null);
            }

        }catch (Exception e){
            return Result.getResultJson(0,"",null);
        }
    }
    /**
     * pexels图库
     * */
    @RequestMapping(value = "/ImagePexels")
    @ResponseBody
    public String ImagePexels() {
        String cacheImage = redisHelp.getRedis(this.dataprefix+"_"+"ImagePexels",redisTemplate);
        String imgList = "";
        if(cacheImage==null){
            imgList = HttpClient.doGetImg("https://api.pexels.com/v1/curated?per_page=40",this.pexelsKey);
            if(imgList==null){
                return Result.getResultJson(0,"图片接口异常",null);
            }
            redisHelp.delete(this.dataprefix+"_"+"ImagePexels",redisTemplate);
            redisHelp.setRedis(this.dataprefix+"_"+"ImagePexels",imgList,43200,redisTemplate);
        }else{
            imgList = cacheImage;
        }
        return imgList;
    }
    /**
     * 十年之约
     * https://www.foreverblog.cn/
     * */
    @RequestMapping(value = "/foreverblog")
    @ResponseBody
    public String foreverblog(@RequestParam(value = "page", required = false) String  page) {
        String cacheForeverblog = redisHelp.getRedis(this.dataprefix+"_"+"foreverblog_"+page,redisTemplate);
        String res = "";
        if(cacheForeverblog==null){
            String url = "https://www.foreverblog.cn/api/v1/blog/feeds?page="+page;
            res = HttpClient.doGet(url);
            if(res==null){
                return Result.getResultJson(0,"接口异常",null);
            }
            redisHelp.delete(this.dataprefix+"_"+"foreverblog_"+page,redisTemplate);
            redisHelp.setRedis(this.dataprefix+"_"+"foreverblog_"+page,res,120,redisTemplate);
        }else{
            res = cacheForeverblog;
        }
        return res;

    }
    /***
     * 全站统计
     */
    @RequestMapping(value = "/allData")
    @ResponseBody
    public String allData( @RequestParam(value = "token", required = false) String  token) {
        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }
        //String group = (String) redisHelp.getValue("userInfo"+token,"group",redisTemplate);
        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        String group = map.get("group").toString();
        if(!group.equals("administrator")){
            return Result.getResultJson(0,"你没有操作权限",null);
        }
        JSONObject data = new JSONObject();
        TypechoContents contents = new TypechoContents();
        Integer allContents = service.total(contents);
        TypechoComments comments = new TypechoComments();
        Integer allComments = commentsService.total(comments);
        TypechoUsers users = new TypechoUsers();
        Integer allUsers = usersService.total(users);
        JSONObject response = new JSONObject();
        TypechoShop shop = new TypechoShop();
        Integer allShop = shopService.total(shop);

        data.put("allContents",allContents);
        data.put("allComments",allComments);
        data.put("allUsers",allUsers);
        data.put("allShop",allShop);

        response.put("code" , 1);
        response.put("msg"  , "");
        response.put("data" , data);

        return response.toString();
    }
}
