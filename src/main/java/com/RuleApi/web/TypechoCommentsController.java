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

    @Value("${webinfo.avatar}")
    private String avatar;

    @Value("${web.prefix}")
    private String dataprefix;

    RedisHelp redisHelp =new RedisHelp();
    ResultAll Result = new ResultAll();
    UserStatus UStatus = new UserStatus();
    baseFull baseFull = new baseFull();

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
                                @RequestParam(value = "searchKey"        , required = false, defaultValue = "") String searchKey,
                            @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit,
                                @RequestParam(value = "token"       , required = false, defaultValue = "") String token) {
        TypechoComments query = new TypechoComments();
        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        Integer uid = 0;
        if (StringUtils.isNotBlank(searchParams)) {
            JSONObject object = JSON.parseObject(searchParams);
            //如果不是管理员，则只查询开放状态评论
            if(uStatus!=0&&token!=""){
                Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
                String group = map.get("group").toString();
                if(!group.equals("administrator")){
                    object.put("status","approved");
                    //如果是登陆状态，那么查询回复我的评论
                    String aid = redisHelp.getValue(this.dataprefix+"_"+"userInfo"+token,"uid",redisTemplate).toString();
                    uid = Integer.parseInt(aid);
                    object.put("ownerId",uid);
                }
            }
            query = object.toJavaObject(TypechoComments.class);
        }
        List jsonList = new ArrayList();
        List cacheList = redisHelp.getList(this.dataprefix+"_"+"searchParams_"+page+"_"+limit+"_"+searchKey+"_"+searchParams,redisTemplate);
        if(uStatus!=0){
            cacheList = redisHelp.getList(this.dataprefix+"_"+"searchParams_"+page+"_"+limit+"_"+searchKey+"_"+searchParams+"_"+uid,redisTemplate);
        }

        try{
            if(cacheList.size()>0){
                jsonList = cacheList;
            }else{
                PageList<TypechoComments> pageList = service.selectPage(query, page, limit,searchKey);
                List list = pageList.getList();
                for (int i = 0; i < list.size(); i++) {
                    Map json = JSONObject.parseObject(JSONObject.toJSONString(list.get(i)), Map.class);
                    String cid = json.get("cid").toString();
                    TypechoContents contentsInfo = contentsService.selectByKey(cid);
                    //如果存在上级评论
                    Map<String, String> parentComments = new HashMap<String, String>();
                    if(Integer.parseInt(json.get("parent").toString())>0){
                        String coid = json.get("parent").toString();
                        TypechoComments parent = service.selectByKey(coid);
                        if(parent.getStatus().equals("approved")){
                            parentComments.put("author",parent.getAuthor());
                            parentComments.put("text",parent.getText());
                            parentComments.put("created",JSONObject.toJSONString(parent.getCreated()));

                        }
                    }
                    if(json.get("mail")!=null){
                        json.put("avatar",this.avatar+ DigestUtils.md5DigestAsHex(json.get("mail").toString().getBytes()));
                    }else{
                        json.put("avatar",this.avatar+"null");
                    }
                    json.put("parentComments",parentComments);
                    json.put("contenTitle",contentsInfo.getTitle());
                    jsonList.add(json);
                    if(uStatus!=0){
                        redisHelp.delete(this.dataprefix+"_"+"contensList_"+page+"_"+limit+"_"+searchKey+"_"+searchParams+"_"+uid,redisTemplate);
                        redisHelp.setList(this.dataprefix+"_"+"contensList_"+page+"_"+limit+"_"+searchKey+"_"+searchParams+"_"+uid,jsonList,this.CommentCache,redisTemplate);
                    }else{
                        redisHelp.delete(this.dataprefix+"_"+"contensList_"+page+"_"+limit+"_"+searchKey+"_"+searchParams,redisTemplate);
                        redisHelp.setList(this.dataprefix+"_"+"contensList_"+page+"_"+limit+"_"+searchKey+"_"+searchParams,jsonList,this.CommentCache,redisTemplate);
                    }

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
        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        Map jsonToMap =null;

        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }
        TypechoComments insert = null;
        String  agent =  request.getHeader("User-Agent");
        //部分机型在uniapp打包下长度大于200
        if(agent.length()>200){
            String[] arr = agent.split("uni-app");
            agent = arr[0];
        }
        String  ip = baseFull.getIpAddr(request);
        if (StringUtils.isNotBlank(params)) {
            jsonToMap =  JSONObject.parseObject(JSON.parseObject(params).toString());
            //获取发布者信息
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Long date = System.currentTimeMillis();
            String created = String.valueOf(date).substring(0,10);
            //获取评论发布者信息和填写其它不可定义的值
            jsonToMap.put("authorId",map.get("uid").toString());
            if(map.get("screenName")==null){
                jsonToMap.put("author",map.get("name").toString());
            }else{
                jsonToMap.put("author",map.get("screenName").toString());
            }
            if(jsonToMap.get("text")==null){
                return Result.getResultJson(0,"评论不能为空",null);
            }else{
                if(jsonToMap.get("text").toString().length()>1500){
                    return Result.getResultJson(0,"超出最大评论长度",null);
                }
            }
            if(map.get("url")!=null){
                jsonToMap.put("url",map.get("url").toString());
            }
            if(map.get("mail")!=null){
                jsonToMap.put("mail",map.get("mail").toString());
            }else{
                return Result.getResultJson(0,"请先绑定邮箱！",null);
            }
            //根据cid获取文章作者信息
            String cid = jsonToMap.get("cid").toString();
            TypechoContents contents = contentsService.selectByKey(cid);
            jsonToMap.put("ownerId", contents.getAuthorId());
            jsonToMap.put("created",created);
            jsonToMap.put("type","comment");
            jsonToMap.put("agent",agent);
            jsonToMap.put("ip",ip);
            //下面这个属性控制评论状态，判断是否已经有评论过审，有则直接通过审核，没有则默认审核状态

            TypechoComments ucomment = new TypechoComments();
            ucomment.setAuthorId(Integer.parseInt(map.get("uid").toString()));
            ucomment.setStatus("approved");
            List<TypechoComments> ucommentList = service.selectList(ucomment);
            if(ucommentList.size()>0){
                jsonToMap.put("status","approved");
            }else{
                jsonToMap.put("status","waiting");
            }

            insert = JSON.parseObject(JSON.toJSONString(jsonToMap), TypechoComments.class);
            //更新文章评论数量
            TypechoComments suminfo = new TypechoComments();
            suminfo.setCid(Integer.parseInt(cid));
            Integer cnum = service.total(suminfo);
            contents.setCommentsNum(cnum);
            contentsService.update(contents);
        }

        int rows = service.insert(insert);



        JSONObject response = new JSONObject();
        response.put("code" ,rows > 0 ? 1: 0 );
        response.put("data" , rows);
        response.put("msg"  , rows > 0 ? "发布成功" : "发布失败");
        return response.toString();
    }
    /***
     * 评论审核
     */
    @RequestMapping(value = "/commentsAudit")
    @ResponseBody
    public String Audit(@RequestParam(value = "key", required = false) String  key, @RequestParam(value = "token", required = false) String  token) {
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
        TypechoComments comments = service.selectByKey(key);
        comments.setStatus("approved");
        Integer rows = service.update(comments);
        JSONObject response = new JSONObject();
        response.put("code" ,rows > 0 ? 1: 0 );
        response.put("data" , rows);
        response.put("msg"  , rows > 0 ? "操作成功" : "操作失败");
        return response.toString();
    }
    /***
     * 评论删除
     */
    @RequestMapping(value = "/commentsDelete")
    @ResponseBody
    public String commentsDelete(@RequestParam(value = "key", required = false) String  key, @RequestParam(value = "token", required = false) String  token) {
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
        JSONObject response = new JSONObject();
        response.put("code" ,rows > 0 ? 1: 0 );
        response.put("data" , rows);
        response.put("msg"  , rows > 0 ? "操作成功" : "操作失败");
        return response.toString();
    }

}
