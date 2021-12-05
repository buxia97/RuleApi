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
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

import org.springframework.stereotype.Component;


/**
 * 控制层
 * TypechoUsersController
 * @author buxia97
 * @date 2021/11/29
 */
@Component
@Controller
@RequestMapping(value = "/typechoUsers")
public class TypechoUsersController {

    @Autowired
    TypechoUsersService service;

    @Autowired
    MailService MailService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${webinfo.url}")
    private String url;

    @Value("${webinfo.usertime}")
    private Integer usertime;

    @Value("${webinfo.userCache}")
    private Integer userCache;


    RedisHelp redisHelp =new RedisHelp();
    ResultAll Result = new ResultAll();
    HttpClient HttpClient = new HttpClient();
    UserStatus UStatus = new UserStatus();

    /***
     * 用户查询
     * @param searchParams Bean对象JSON字符串
     * @param page         页码
     * @param limit        每页显示数量
     */
    @RequestMapping(value = "/userList")
    @ResponseBody
    public String userList (@RequestParam(value = "searchParams", required = false) String  searchParams,
                            @RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                            @RequestParam(value = "searchKey"        , required = false, defaultValue = "") String searchKey,
                            @RequestParam(value = "order"        , required = false, defaultValue = "") String order,
                            @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit) {
        TypechoUsers query = new TypechoUsers();
        if (StringUtils.isNotBlank(searchParams)) {
            JSONObject object = JSON.parseObject(searchParams);
            object.remove("password");
            query = object.toJavaObject(TypechoUsers.class);
        }
        List jsonList = new ArrayList();

        List cacheList = redisHelp.getList("userList_"+page+"_"+limit+"_"+searchParams+"_"+order,redisTemplate);
        try{
            if(cacheList.size()>0){
                jsonList = cacheList;
            }else{
                PageList<TypechoUsers> pageList = service.selectPage(query, page, limit, searchKey,order);
                List list = pageList.getList();
                for (int i = 0; i < list.size(); i++) {
                    Map json = JSONObject.parseObject(JSONObject.toJSONString(list.get(i)), Map.class);
                    json.remove("password");
                    jsonList.add(json);

                }
                redisHelp.delete("userList_"+page+"_"+limit+"_"+searchParams+"_"+order+"_"+searchKey,redisTemplate);
                redisHelp.setList("userList_"+page+"_"+limit+"_"+searchParams+"_"+order+"_"+searchKey,jsonList,this.userCache,redisTemplate);
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
     * 表单查询
     */
    @RequestMapping(value = "/userInfo")
    @ResponseBody
    public TypechoUsers userInfo(@RequestParam(value = "key", required = false) String  key) {
        TypechoUsers info =  service.selectByKey(key);
        Map json = JSONObject.parseObject(JSONObject.toJSONString(info), Map.class);
        json.remove("password");
        TypechoUsers userInfo = JSON.parseObject(JSON.toJSONString(json), TypechoUsers.class);
        return userInfo;
    }
    /***
     * 登陆
     * @param params Bean对象JSON字符串
     */
    @RequestMapping(value = "/userLogin")
    @ResponseBody
    public String userLogin(@RequestParam(value = "params", required = false) String  params) {
        Map jsonToMap =null;
        String oldpw = null;
        if (StringUtils.isNotBlank(params)) {
            jsonToMap =  JSONObject.parseObject(JSON.parseObject(params).toString());
            if(jsonToMap.get("name")==null||jsonToMap.get("password")==null){
                return Result.getResultJson(0,"请输入正确的参数",null);
            }
            oldpw = jsonToMap.get("password").toString();
        }else{
            return Result.getResultJson(0,"请输入正确的参数",null);
        }
        jsonToMap.remove("password");
        TypechoUsers Users = JSON.parseObject(JSON.toJSONString(jsonToMap), TypechoUsers.class);
        List<TypechoUsers> rows = service.selectList(Users);
        if(rows.size() > 0){
            //查询出用户信息后，通过接口验证用户密码
            String newpw = rows.get(0).getPassword();
            String url = this.url+"/apiResult.php?oldpw="+oldpw+"&newpw="+newpw;
            String passwd = HttpClient.doGet(url);
            if(passwd==null){
                return Result.getResultJson(0,"用户接口异常",null);
            }
            passwd = passwd.replaceAll("(\\\r\\\n|\\\r|\\\n|\\\n\\\r)", "");
            if(!passwd.equals(newpw)){
                return Result.getResultJson(0,"用户密码错误",null);
            }

            Long date = System.currentTimeMillis();
            String Token = date + jsonToMap.get("name").toString();
            jsonToMap.put("uid",rows.get(0).getUid());
            //生成唯一性token用于验证
            jsonToMap.put("token",jsonToMap.get("name").toString()+DigestUtils.md5DigestAsHex(Token.getBytes()));
            jsonToMap.put("time",date);
            jsonToMap.put("group",rows.get(0).getGroupKey());
            jsonToMap.put("mail",rows.get(0).getMail());
            jsonToMap.put("url",rows.get(0).getUrl());

            //更新用户登录时间和第一次登陆时间（满足typecho要求）
            String userTime = String.valueOf(date).substring(0,10);
            Map updateLogin = new HashMap<String, String>();
            updateLogin.put("uid",rows.get(0).getUid());
            updateLogin.put("logged",userTime);
            if(rows.get(0).getLogged()==0){
                updateLogin.put("activated",userTime);
            }
            TypechoUsers updateuser = JSON.parseObject(JSON.toJSONString(updateLogin), TypechoUsers.class);
            service.update(updateuser);


            //删除之前的token后，存入redis(防止积累导致内存溢出，超时时间默认是24小时)
            String oldToken = redisHelp.getRedis("userkey"+jsonToMap.get("name").toString(),redisTemplate);
            if(oldToken!=null){
                redisHelp.delete("userInfo"+oldToken,redisTemplate);
            }
            //redisHelp.deleteByPrex("userInfo"+jsonToMap.get("name").toString()+":*",redisTemplate);
            redisHelp.setRedis("userkey"+jsonToMap.get("name").toString(),jsonToMap.get("token").toString(),this.usertime,redisTemplate);
            redisHelp.setKey("userInfo"+jsonToMap.get("name").toString()+DigestUtils.md5DigestAsHex(Token.getBytes()),jsonToMap,this.usertime,redisTemplate);

        }
        return Result.getResultJson(rows.size() > 0 ? 1 : 0,rows.size() > 0 ? "登录成功" : "用户名或密码错误",jsonToMap);
    }
    /***
     * 注册用户
     * @param params Bean对象JSON字符串
     */
    @RequestMapping(value = "/userRegister")
    @ResponseBody
    public String userRegister(@RequestParam(value = "params", required = false) String  params) {
        TypechoUsers insert = null;
        Map jsonToMap =null;


        if (StringUtils.isNotBlank(params)) {
            jsonToMap =  JSONObject.parseObject(JSON.parseObject(params).toString());
            //在之前需要做判断，验证用户名或者邮箱在数据库中是否存在
            //验证是否存在相同用户名
            Map keyMail = new HashMap<String, String>();
            keyMail.put("mail",jsonToMap.get("mail").toString());
            TypechoUsers toKey = JSON.parseObject(JSON.toJSONString(keyMail), TypechoUsers.class);
            List isMail = service.selectList(toKey);
            if(isMail.size() > 0){
                return Result.getResultJson(0,"该邮箱已注册",null);
            }
            Map keyName = new HashMap<String, String>();
            keyName.put("name",jsonToMap.get("name").toString());
            TypechoUsers toKey1 = JSON.parseObject(JSON.toJSONString(keyName), TypechoUsers.class);
            List isName = service.selectList(toKey1);
            if(isName.size() > 0){
                return Result.getResultJson(0,"该用户名已注册",null);
            }
            String p = jsonToMap.get("password").toString();
            String url = this.url+"/apiResult.php?pw="+p;
            String passwd = HttpClient.doGet(url);
            if(passwd==null){
                return Result.getResultJson(0,"用户接口异常",null);
            }
            Long date = System.currentTimeMillis();
            String userTime = String.valueOf(date).substring(0,10);
            jsonToMap.put("created",userTime);
            jsonToMap.put("password", passwd.replaceAll("(\\\r\\\n|\\\r|\\\n|\\\n\\\r)", ""));
        }
        insert = JSON.parseObject(JSON.toJSONString(jsonToMap), TypechoUsers.class);
        int rows = service.insert(insert);

        JSONObject response = new JSONObject();
        response.put("code" ,rows > 0 ? 1: 0 );
        response.put("data" , rows);
        response.put("msg"  , rows > 0 ? "添加成功" : "添加失败");
        return response.toString();
    }

    @RequestMapping(value = "/SendCode")
    @ResponseBody
    public String SendCode(@RequestParam(value = "params", required = false) String  params) throws MessagingException {
        Map jsonToMap =null;

        if (StringUtils.isNotBlank(params)) {
            jsonToMap =  JSONObject.parseObject(JSON.parseObject(params).toString());
            Map keyName = new HashMap<String, String>();
            keyName.put("name",jsonToMap.get("name").toString());
            TypechoUsers toKey1 = JSON.parseObject(JSON.toJSONString(keyName), TypechoUsers.class);
            List<TypechoUsers> isName = service.selectList(toKey1);


            if(isName.size() > 0){
                //生成六位随机验证码
                Random random = new Random();
                String code="";
                for (int i=0;i<6;i++) {
                    code += random.nextInt(10);
                }
                //存入redis并发送邮件
                String name = isName.get(0).getName();
                String email = isName.get(0).getMail();
                redisHelp.setRedis("sendCode"+name,code,1800,redisTemplate);
                MailService.send("你本次的验证码为"+code, "<!DOCTYPE html><html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" /><title></title><meta charset=\"utf-8\" /><style>*{padding:0px;margin:0px;box-sizing:border-box;}html{box-sizing:border-box;}body{font-size:15px;background:#fff}.main{margin:20px auto;max-width:500px;border:solid 1px #20a4ab;border-radius:8px;overflow:hidden;}.main h1{display:block;width:100%;background:#20a4ab;font-size:18px;color:#fff;text-align:center;padding:15px;}.text{padding:30px;}.text p{margin:10px 0px;line-height:25px;}.text p span{color:#20a4ab;font-weight:bold;font-size:22px;margin-left:5px;}</style></head><body><div class=\"main\"><h1>用户验证码</h1><div class=\"text\"><p>用户 "+name+"，你本次的验证码为<span>"+code+"</span>。</p><p>出于安全原因，该验证码将于30分钟后失效。请勿将验证码透露给他人。</p></div></div></body></html>",
                        new String[] {email}, new String[] {});
                return Result.getResultJson(1,"邮件发送成功",null);
            }else{
                return Result.getResultJson(0,"该用户不存在",null);
            }

        }else{
            return Result.getResultJson(0,"参数错误",null);
        }


    }
    /***
     * 找回密码
     * @param params Bean对象JSON字符串
     */
    @RequestMapping(value = "/userFoget")
    @ResponseBody
    public String userFoget(@RequestParam(value = "params", required = false) String  params) {
        TypechoUsers update = null;
        Map jsonToMap =null;
        if (StringUtils.isNotBlank(params)) {

            jsonToMap =  JSONObject.parseObject(JSON.parseObject(params).toString());
            String code = jsonToMap.get("code").toString();
            String name = jsonToMap.get("name").toString();
            //从redis获取验证码
            String sendCode = null;
            if(redisHelp.getRedis("sendCode"+name,redisTemplate)!=null){
                sendCode =redisHelp.getRedis("sendCode"+name,redisTemplate);
            }else{
                return Result.getResultJson(0,"验证码已超时或未发送",null);
            }
            if(!sendCode.equals(code)){
                return Result.getResultJson(0,"验证码不正确",null);
            }
            redisHelp.delete("sendCode"+name,redisTemplate);
            String p = jsonToMap.get("password").toString();
            String url = this.url+"/apiResult.php?pw="+p;
            String passwd = HttpClient.doGet(url);
            if(passwd==null){
                return Result.getResultJson(0,"用户接口异常",null);
            }
            jsonToMap.put("password", passwd.replaceAll("(\\\r\\\n|\\\r|\\\n|\\\n\\\r)", ""));
            jsonToMap.remove("code");

            Map keyName = new HashMap<String, String>();
            keyName.put("name",jsonToMap.get("name").toString());
            TypechoUsers toKey1 = JSON.parseObject(JSON.toJSONString(keyName), TypechoUsers.class);
            List<TypechoUsers> isName = service.selectList(toKey1);
            if(isName.size() == 0){
                return Result.getResultJson(0,"用户不存在",null);
            }

            Map updateMap = new HashMap<String, String>();
            updateMap.put("uid",isName.get(0).getUid().toString());
            updateMap.put("name",jsonToMap.get("name").toString());
            updateMap.put("password", jsonToMap.get("password").toString());

            update = JSON.parseObject(JSON.toJSONString(updateMap), TypechoUsers.class);
        }

        int rows = service.update(update);

        JSONObject response = new JSONObject();
        response.put("code" ,rows > 0 ? 1: 0 );
        response.put("data" , rows);
        response.put("msg"  , rows > 0 ? "操作成功" : "操作失败");
        return response.toString();
    }

    /***
     * 用户修改
     * @param params Bean对象JSON字符串
     */
    @RequestMapping(value = "/userEdit")
    @ResponseBody
    public String userEdit(@RequestParam(value = "params", required = false) String  params, @RequestParam(value = "token", required = false) String  token) {
        TypechoUsers update = null;
        Map jsonToMap =null;
        Integer uStatus = UStatus.getStatus(token,redisTemplate);
        String updateName = "";
        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }
        if (StringUtils.isNotBlank(params)) {

            jsonToMap =  JSONObject.parseObject(JSON.parseObject(params).toString());
            String p = jsonToMap.get("password").toString();
            String url = this.url+"/apiResult.php?pw="+p;
            String passwd = HttpClient.doGet(url);
            if(passwd==null){
                return Result.getResultJson(0,"用户接口异常",null);
            }
            jsonToMap.put("password", passwd.replaceAll("(\\\r\\\n|\\\r|\\\n|\\\n\\\r)", ""));
            jsonToMap.remove("code");

            Map keyName = new HashMap<String, String>();
            keyName.put("name",jsonToMap.get("name").toString());
            TypechoUsers toKey1 = JSON.parseObject(JSON.toJSONString(keyName), TypechoUsers.class);
            List<TypechoUsers> isName = service.selectList(toKey1);
            if(isName.size() == 0){
                return Result.getResultJson(0,"用户不存在",null);
            }
            updateName = jsonToMap.get("name").toString();
            //部分字段不允许修改
            jsonToMap.remove("name");
            jsonToMap.remove("group");
            update = JSON.parseObject(JSON.toJSONString(jsonToMap), TypechoUsers.class);
        }

        int rows = service.update(update);

        if(rows>0&&jsonToMap.get("password").toString()!="") {
            //执行成功后，如果密码发生了改变，需要重新登陆
            redisHelp.delete("userInfo"+token,redisTemplate);
        }

        JSONObject response = new JSONObject();
        response.put("code" ,rows > 0 ? 1: 0 );
        response.put("data" , rows);
        response.put("msg"  , rows > 0 ? "操作成功" : "操作失败");
        return response.toString();
    }
    /***
     * 用户删除
     */
    @RequestMapping(value = "/userDelete")
    @ResponseBody
    public String userDelete(@RequestParam(value = "key", required = false) String  key, @RequestParam(value = "token", required = false) String  token) {
        Integer uStatus = UStatus.getStatus(token,redisTemplate);
        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }
        //String group = (String) redisHelp.getValue("userInfo"+token,"group",redisTemplate);
        Map map =redisHelp.getMapValue("userInfo"+token,redisTemplate);
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
