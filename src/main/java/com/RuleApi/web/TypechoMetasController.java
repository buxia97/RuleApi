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
    private RedisTemplate redisTemplate;

    @Value("${webinfo.contentCache}")
    private Integer contentCache;

    @Value("${web.prefix}")
    private String dataprefix;


    RedisHelp redisHelp =new RedisHelp();
    ResultAll Result = new ResultAll();
    baseFull baseFull = new baseFull();

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
        if (StringUtils.isNotBlank(searchParams)) {
            JSONObject object = JSON.parseObject(searchParams);
            Integer mid = Integer.parseInt(object.get("mid").toString());
            query.setMid(mid);
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
                    //只有开放状态文章允许加入
                    String status = contentsInfo.get("status").toString();
                    String ctype = contentsInfo.get("type").toString();
                    //应该判断类型和发布状态，而不是直接判断状态
                    if(status.equals("publish")&&ctype.equals("post")){
                        //处理文章内容为简介

                        String text = contentsInfo.get("text").toString();
                        List imgList = baseFull.getImageSrc(text);
                        text=text.replaceAll("(\\\r\\\n|\\\r|\\\n|\\\n\\\r)", "");
                        text=text.replaceAll("\\s*", "");
                        text=text.replaceAll("</?[^>]+>", "");
                        //去掉文章开头的图片插入
                        text=text.replaceAll("((https?|http):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)","");
                        text=text.replaceAll("((!\\[)[\\s\\S]+?(\\]\\[)[\\s\\S]+?(\\]))", "");
                        text=text.replaceAll("((!\\[)[\\s\\S]+?(\\]))", "");
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
                    }

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
                             @RequestParam(value = "order"        , required = false, defaultValue = "") String order) {
        TypechoMetas query = new TypechoMetas();
        if (StringUtils.isNotBlank(searchParams)) {
            JSONObject object = JSON.parseObject(searchParams);
            query = object.toJavaObject(TypechoMetas.class);
        }

        PageList<TypechoMetas> pageList = service.selectPage(query, page, limit,order);
        JSONObject response = new JSONObject();
        response.put("code" , 1);
        response.put("msg"  , "");
        response.put("data" , null != pageList.getList() ? pageList.getList() : new JSONArray());
        response.put("count", pageList.getTotalCount());
        return response.toString();
    }

}
