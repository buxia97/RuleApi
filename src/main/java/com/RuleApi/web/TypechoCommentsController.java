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
 * TypechoCommentsController
 * @author buxia97
 * @date 2021/11/29
 */
@Component
@Controller
@RequestMapping(value = "/typechoComments")
public class TypechoCommentsController {

    @Autowired
    TypechoCommentsService service;

    @Autowired
    private TypechoContentsService contentsService;

    @Autowired
    private TypechoUsersService usersService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${webinfo.CommentCache}")
    private Integer CommentCache;

    RedisHelp redisHelp =new RedisHelp();
    ResultAll Result = new ResultAll();
    UserStatus UStatus = new UserStatus();

    /***
     * 查询评论
     * @param searchParams Bean对象JSON字符串
     * @param page         页码
     * @param limit        每页显示数量
     */
    @RequestMapping(value = "/commentsList")
    @ResponseBody
    public String commentsList (@RequestParam(value = "searchParams", required = false) String  searchParams,
                            @RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                            @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit) {
        TypechoComments query = new TypechoComments();

        if (StringUtils.isNotBlank(searchParams)) {
            JSONObject object = JSON.parseObject(searchParams);
            //只查询开放状态评论
            object.put("status","approved");
            query = object.toJavaObject(TypechoComments.class);
        }
        List jsonList = new ArrayList();
        List cacheList = redisHelp.getList("searchParams_"+page+"_"+limit+"_"+searchParams,redisTemplate);
        try{
            if(cacheList.size()>0){
                jsonList = cacheList;
            }else{
                PageList<TypechoComments> pageList = service.selectPage(query, page, limit);
                List list = pageList.getList();
                for (int i = 0; i < list.size(); i++) {
                    Map json = JSONObject.parseObject(JSONObject.toJSONString(list.get(i)), Map.class);
                    String cid = json.get("cid").toString();
                    TypechoContents contentsInfo = contentsService.selectByKey(cid);
                    json.put("contenTitle",contentsInfo.getTitle());
                    jsonList.add(json);
                    redisHelp.delete("contensList_"+page+"_"+limit+"_"+searchParams,redisTemplate);
                    redisHelp.setList("contensList_"+page+"_"+limit+"_"+searchParams,jsonList,this.CommentCache,redisTemplate);
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
     * 添加评论
     * @param params Bean对象JSON字符串
     */
    @RequestMapping(value = "/commentsAdd")
    @ResponseBody
    public String commentsAdd(@RequestParam(value = "params", required = false) String  params, @RequestParam(value = "token", required = false) String  token,HttpServletRequest request) {
        Integer uStatus = UStatus.getStatus(token,redisTemplate);
        Map jsonToMap =null;

        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }
        TypechoComments insert = null;
        String  agent =  request.getHeader("User-Agent");
        String  ip = request.getHeader("X-FORWARDED-FOR ");
        if(ip==null){
            ip = request.getRemoteAddr();
        }
        if (StringUtils.isNotBlank(params)) {
            jsonToMap =  JSONObject.parseObject(JSON.parseObject(params).toString());
            //获取发布者信息
            Map map =redisHelp.getMapValue("userInfo"+token,redisTemplate);
            Long date = System.currentTimeMillis();
            String created = String.valueOf(date).substring(0,10);
            //获取文章作者信息和填写其它不可定义的值
            jsonToMap.put("authorId",map.get("uid").toString());
            jsonToMap.put("author",map.get("name").toString());
            jsonToMap.put("url",map.get("url").toString());
            jsonToMap.put("mail",map.get("mail").toString());
            jsonToMap.put("created",created);
            jsonToMap.put("type","comment");
            jsonToMap.put("agent",agent);
            jsonToMap.put("ip",ip);
            //下面这个属性控制评论状态，默认是直接显示
            jsonToMap.put("status","approved");

            insert = JSON.parseObject(JSON.toJSONString(jsonToMap), TypechoComments.class);


        }

        int rows = service.insert(insert);

        //更新文章评论数量
        String cid = jsonToMap.get("cid").toString();
        TypechoContents contents = contentsService.selectByKey(cid);
        Integer cnum = contents.getCommentsNum()+1;
        contents.setCommentsNum(cnum);
        contentsService.update(contents);

        JSONObject response = new JSONObject();
        response.put("code" ,rows > 0 ? 1: 0 );
        response.put("data" , rows);
        response.put("msg"  , rows > 0 ? "发布成功" : "发布失败");
        return response.toString();
    }


}
