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
 * TypechoSpaceController
 * @author buxia97
 * @date 2023/02/05
 */
@Controller
@RequestMapping(value = "/typechoSpace")
public class TypechoSpaceController {

    @Autowired
    TypechoSpaceService service;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private TypechoApiconfigService apiconfigService;

    @Autowired
    private TypechoUsersService usersService;

    @Autowired
    private TypechoFanService fanService;

    @Value("${web.prefix}")
    private String dataprefix;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private PushService pushService;

    RedisHelp redisHelp =new RedisHelp();
    ResultAll Result = new ResultAll();
    UserStatus UStatus = new UserStatus();
    baseFull baseFull = new baseFull();
    EditFile editFile = new EditFile();

    /**
     * 添加动态
     */
    @RequestMapping(value = "/addSpace")
    @ResponseBody
    public String addSpace (@RequestParam(value = "text", required = false, defaultValue = "") String  text,
                             @RequestParam(value = "type", required = false, defaultValue = "0") Integer  type,
                             @RequestParam(value = "toid", required = false, defaultValue = "0") Integer  toid,
                            @RequestParam(value = "pic", required = false) String  pic,
                             @RequestParam(value = "token", required = false) String  token) {
        try{
            if(!type.equals(0)&&!type.equals(1)&&!type.equals(2)&&!type.equals(3)){
                return Result.getResultJson(0,"参数不正确",null);
            }
            //类型不为0时，需要传toid
            if(type > 0){
                if(toid.equals(0)){
                    return Result.getResultJson(0,"参数不正确",null);
                }
            }
            if(text==null){
                return Result.getResultJson(0,"参数不正确",null);
            }
            if(text.length()<4){
                return Result.getResultJson(0,"动态内容长度不能小于4",null);
            }
            Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
            if(uStatus==0){
                return Result.getResultJson(0,"用户未登录或Token验证失败",null);
            }
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer uid =Integer.parseInt(map.get("uid").toString());

            String isSilence = redisHelp.getRedis(this.dataprefix+"_"+uid+"_silence",redisTemplate);
            if(isSilence!=null){
                return Result.getResultJson(0,"你的操作太频繁了，请稍后再试",null);
            }

            //登录情况下，刷数据攻击拦截
            String isRepeated = redisHelp.getRedis(this.dataprefix+"_"+uid+"_isAddSpace",redisTemplate);
            if(isRepeated==null){
                redisHelp.setRedis(this.dataprefix+"_"+uid+"_isAddSpace","1",4,redisTemplate);
            }else{
                Integer frequency = Integer.parseInt(isRepeated) + 1;
                if(frequency==4){
                    securityService.safetyMessage("用户ID："+uid+"，在聊天发送消息接口疑似存在攻击行为，请及时确认处理。","system");
                    redisHelp.setRedis(this.dataprefix+"_"+uid+"_silence","1",600,redisTemplate);
                    return Result.getResultJson(0,"你的发言过于频繁，已被禁言十分钟！",null);
                }else{
                    redisHelp.setRedis(this.dataprefix+"_"+uid+"_isAddSpace",frequency.toString(),5,redisTemplate);
                }
                return Result.getResultJson(0,"你的操作太频繁了",null);
            }

            //攻击拦截结束
            //违禁词拦截
            TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);
            String forbidden = apiconfig.getForbidden();
            Integer intercept = 0;
            if(forbidden!=null){
                if(forbidden.indexOf(",") != -1){
                    String[] strarray=forbidden.split(",");
                    for (int i = 0; i < strarray.length; i++){
                        String str = strarray[i];
                        if(text.indexOf(str) != -1){
                            intercept = 1;
                        }
                    }
                }else{
                    if(text.indexOf(forbidden) != -1){
                        intercept = 1;
                    }
                }
            }
            if(intercept.equals(1)){
                //以十分钟为检测周期，违禁一次刷新一次，等于4次则禁言
                String isIntercept = redisHelp.getRedis(this.dataprefix+"_"+uid+"_isIntercept",redisTemplate);
                if(isIntercept==null){
                    redisHelp.setRedis(this.dataprefix+"_"+uid+"_isIntercept","1",600,redisTemplate);
                }else{
                    Integer frequency = Integer.parseInt(isIntercept) + 1;
                    if(frequency==4){
                        securityService.safetyMessage("用户ID："+uid+"，在聊天发送消息接口多次触发违禁，请及时确认处理。","system");
                        redisHelp.setRedis(this.dataprefix+"_"+uid+"_silence","1",3600,redisTemplate);
                        return Result.getResultJson(0,"你已多次发送违禁词，被禁言一小时！",null);
                    }else{
                        redisHelp.setRedis(this.dataprefix+"_"+uid+"_isIntercept",frequency.toString(),600,redisTemplate);
                    }

                }
                return Result.getResultJson(0,"消息存在违禁词",null);
            }
            //违禁词拦截结束
            Long date = System.currentTimeMillis();
            String created = String.valueOf(date).substring(0,10);


            TypechoSpace space = new TypechoSpace();
            space.setText(text);
            space.setUid(uid);
            space.setPic(pic);
            space.setToid(toid);
            space.setCreated(Integer.parseInt(created));
            space.setModified(Integer.parseInt(created));
            int rows = service.insert(space);
            JSONObject response = new JSONObject();
            response.put("code" , rows);
            response.put("msg"  , rows > 0 ? "发布成功" : "发布失败");
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }

    }

    /**
     * 修改动态
     */
    @RequestMapping(value = "/editSpace")
    @ResponseBody
    public String editSpace (
                            @RequestParam(value = "id", required = false) Integer  id,
                            @RequestParam(value = "text", required = false, defaultValue = "") String  text,
                            @RequestParam(value = "type", required = false, defaultValue = "0") Integer  type,
                            @RequestParam(value = "toid", required = false, defaultValue = "0") Integer  toid,
                            @RequestParam(value = "pic", required = false) String  pic,
                            @RequestParam(value = "token", required = false) String  token) {
        try{
            if(!type.equals(0)&&!type.equals(1)&&!type.equals(2)&&!type.equals(3)){
                return Result.getResultJson(0,"参数不正确",null);
            }
            //类型不为0时，需要传toid
            if(type > 0){
                if(toid.equals(0)){
                    return Result.getResultJson(0,"参数不正确",null);
                }
            }
            if(text==null){
                return Result.getResultJson(0,"参数不正确",null);
            }
            if(text.length()<4){
                return Result.getResultJson(0,"动态内容长度不能小于4",null);
            }
            Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
            if(uStatus==0){
                return Result.getResultJson(0,"用户未登录或Token验证失败",null);
            }
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer uid =Integer.parseInt(map.get("uid").toString());
            String group = map.get("group").toString();
            String isSilence = redisHelp.getRedis(this.dataprefix+"_"+uid+"_silence",redisTemplate);
            if(isSilence!=null){
                return Result.getResultJson(0,"你的操作太频繁了，请稍后再试",null);
            }

            //登录情况下，刷数据攻击拦截
            String isRepeated = redisHelp.getRedis(this.dataprefix+"_"+uid+"_isAddSpace",redisTemplate);
            if(isRepeated==null){
                redisHelp.setRedis(this.dataprefix+"_"+uid+"_isAddSpace","1",4,redisTemplate);
            }else{
                Integer frequency = Integer.parseInt(isRepeated) + 1;
                if(frequency==4){
                    securityService.safetyMessage("用户ID："+uid+"，在聊天发送消息接口疑似存在攻击行为，请及时确认处理。","system");
                    redisHelp.setRedis(this.dataprefix+"_"+uid+"_silence","1",600,redisTemplate);
                    return Result.getResultJson(0,"你的发言过于频繁，已被禁言十分钟！",null);
                }else{
                    redisHelp.setRedis(this.dataprefix+"_"+uid+"_isAddSpace",frequency.toString(),5,redisTemplate);
                }
                return Result.getResultJson(0,"你的操作太频繁了",null);
            }

            //攻击拦截结束
            //违禁词拦截
            TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);
            String forbidden = apiconfig.getForbidden();
            Integer intercept = 0;
            if(forbidden!=null){
                if(forbidden.indexOf(",") != -1){
                    String[] strarray=forbidden.split(",");
                    for (int i = 0; i < strarray.length; i++){
                        String str = strarray[i];
                        if(text.indexOf(str) != -1){
                            intercept = 1;
                        }
                    }
                }else{
                    if(text.indexOf(forbidden) != -1){
                        intercept = 1;
                    }
                }
            }
            if(intercept.equals(1)){
                //以十分钟为检测周期，违禁一次刷新一次，等于4次则禁言
                String isIntercept = redisHelp.getRedis(this.dataprefix+"_"+uid+"_isIntercept",redisTemplate);
                if(isIntercept==null){
                    redisHelp.setRedis(this.dataprefix+"_"+uid+"_isIntercept","1",600,redisTemplate);
                }else{
                    Integer frequency = Integer.parseInt(isIntercept) + 1;
                    if(frequency==4){
                        securityService.safetyMessage("用户ID："+uid+"，在聊天发送消息接口多次触发违禁，请及时确认处理。","system");
                        redisHelp.setRedis(this.dataprefix+"_"+uid+"_silence","1",3600,redisTemplate);
                        return Result.getResultJson(0,"你已多次发送违禁词，被禁言一小时！",null);
                    }else{
                        redisHelp.setRedis(this.dataprefix+"_"+uid+"_isIntercept",frequency.toString(),600,redisTemplate);
                    }

                }
                return Result.getResultJson(0,"消息存在违禁词",null);
            }
            //违禁词拦截结束
            Long date = System.currentTimeMillis();
            String created = String.valueOf(date).substring(0,10);
            TypechoSpace oldSpace = service.selectByKey(id);
            if(oldSpace==null){
                return Result.getResultJson(0,"动态不存在",null);
            }
            if(!group.equals("administrator")&&!group.equals("editor")){
                if(!oldSpace.getUid().equals(uid)){
                    return Result.getResultJson(0,"你没有操作权限",null);
                }
            }
            TypechoSpace space = new TypechoSpace();
            space.setText(text);
            space.setUid(uid);
            space.setPic(pic);
            space.setToid(toid);
            space.setCreated(Integer.parseInt(created));
            space.setModified(Integer.parseInt(created));
            int rows = service.insert(space);
            JSONObject response = new JSONObject();
            response.put("code" , rows);
            response.put("msg"  , rows > 0 ? "保存成功" : "保存失败");
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }

    }

    /**
     * 获取动态详情
     *
     */
    @RequestMapping(value = "/spaceInfo")
    @ResponseBody
    public String spaceInfo (@RequestParam(value = "id", required = false) Integer  id) {
        try{
            Map spaceInfoJson = new HashMap<String, String>();
            Map cacheInfo = redisHelp.getMapValue(this.dataprefix+"_"+"spaceInfo_"+id,redisTemplate);

            if(cacheInfo.size()>0){
                spaceInfoJson = cacheInfo;
            }else{
                TypechoSpace space;
                space = service.selectByKey(id);
                //获取创建人信息
                Integer userid = space.getUid();
                Map userJson = UserStatus.getUserInfo(userid,apiconfigService,usersService);
                spaceInfoJson.put("userJson",userJson);
                spaceInfoJson = JSONObject.parseObject(JSONObject.toJSONString(space), Map.class);
                redisHelp.delete(this.dataprefix+"_"+"spaceInfo_"+id,redisTemplate);
                redisHelp.setKey(this.dataprefix+"_"+"spaceInfo_"+id,spaceInfoJson,5,redisTemplate);
            }

            JSONObject response = new JSONObject();

            response.put("code", 1);
            response.put("msg", "");
            response.put("data", spaceInfoJson);

            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            JSONObject response = new JSONObject();
            response.put("code", 1);
            response.put("msg", "");
            response.put("data", null);

            return response.toString();
        }
    }

    /***
     * 动态列表
     */
    @RequestMapping(value = "/spaceList")
    @ResponseBody
    public String spaceList (@RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                            @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit,
                            @RequestParam(value = "searchKey"        , required = false, defaultValue = "") String searchKey,
                            @RequestParam(value = "order", required = false, defaultValue = "created") String  order,
                            @RequestParam(value = "token", required = false) String  token) {
        if(limit>50){
            limit = 50;
        }
        TypechoSpace query = new TypechoSpace();
        Map map = new HashMap();
        Integer uid = 0;
        Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
        if (uStatus != 0) {
            map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            uid =Integer.parseInt(map.get("uid").toString());
        }
        List cacheList =  redisHelp.getList(this.dataprefix+"_"+"spaceList_"+page+"_"+limit+"_"+searchKey+"_"+uid,redisTemplate);
        List jsonList = new ArrayList();

        Integer total = service.total(query);
        try{
            if(cacheList.size()>0){
                jsonList = cacheList;
            }else{


                TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);

                PageList<TypechoSpace> pageList = service.selectPage(query, page, limit,order,searchKey);
                List<TypechoSpace> list = pageList.getList();
                if(list.size() < 1){
                    JSONObject noData = new JSONObject();
                    noData.put("code" , 1);
                    noData.put("msg"  , "");
                    noData.put("data" , new ArrayList());
                    noData.put("count", 0);
                    noData.put("total", total);
                    return noData.toString();
                }
                for (int i = 0; i < list.size(); i++) {
                    Map json = JSONObject.parseObject(JSONObject.toJSONString(list.get(i)), Map.class);
                    TypechoSpace space = list.get(i);
                    Integer userid = space.getUid();
                    TypechoUsers user = usersService.selectByKey(userid);
                    //获取用户信息
                    Map userJson = UserStatus.getUserInfo(userid,apiconfigService,usersService);
                    if(uStatus!=0){

                    }
                    json.put("userJson",userJson);
                    jsonList.add(json);
                }
                redisHelp.delete(this.dataprefix+"_"+"spaceList_"+page+"_"+limit,redisTemplate);
                redisHelp.setList(this.dataprefix+"_"+"spaceList_"+page+"_"+limit,jsonList,20,redisTemplate);
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

}
