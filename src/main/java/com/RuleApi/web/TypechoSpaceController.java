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
import java.util.*;

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
    private TypechoContentsService contentsService;

    @Autowired
    private TypechoUserlogService userlogService;

    @Autowired
    private TypechoShopService shopService;

    @Autowired
    private TypechoCommentsService commentsService;

    @Autowired
    private TypechoFanService fanService;

    @Value("${web.prefix}")
    private String dataprefix;

    @Autowired
    private RedisTemplate redisTemplate;


    @Autowired
    private TypechoInboxService inboxService;

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
                            @RequestParam(value = "token", required = false) String  token,
                            HttpServletRequest request) {
        try{
            if(!type.equals(0)&&!type.equals(1)&&!type.equals(2)&&!type.equals(3)&&!type.equals(4)&&!type.equals(5)){
                return Result.getResultJson(0,"参数不正确",null);
            }
            //类型不为0时，需要传toid
            if(!type.equals(0)&&!type.equals(4)){
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
            TypechoApiconfig apiconfig = UStatus.getConfig(this.dataprefix,apiconfigService,redisTemplate);
            //登录情况下，刷数据攻击拦截
            String isRepeated = redisHelp.getRedis(this.dataprefix+"_"+uid+"_isAddSpace",redisTemplate);
            if(isRepeated==null){
                redisHelp.setRedis(this.dataprefix+"_"+uid+"_isAddSpace","1",4,redisTemplate);
            }else{
                Integer frequency = Integer.parseInt(isRepeated) + 1;
                if(frequency==4){
                    securityService.safetyMessage("用户ID："+uid+"，在聊天发送消息接口疑似存在攻击行为，请及时确认处理。","system");
                    redisHelp.setRedis(this.dataprefix+"_"+uid+"_silence","1",apiconfig.getSilenceTime(),redisTemplate);
                    return Result.getResultJson(0,"你的操作过于频繁，已被禁言十分钟！",null);
                }else{
                    redisHelp.setRedis(this.dataprefix+"_"+uid+"_isAddSpace",frequency.toString(),5,redisTemplate);
                }
                return Result.getResultJson(0,"你的操作太频繁了",null);
            }

            //攻击拦截结束
            //普通用户最大发文限制
            Map userMap =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            String group = userMap.get("group").toString();
            if(!group.equals("administrator")&&!group.equals("editor")){
                String spaceNum = redisHelp.getRedis(this.dataprefix+"_"+uid+"_spaceNum",redisTemplate);
                if(spaceNum==null){
                    redisHelp.setRedis(this.dataprefix+"_"+uid+"_spaceNum","1",86400,redisTemplate);
                }else{
                    Integer space_Num = Integer.parseInt(spaceNum) + 1;
                    if(space_Num > apiconfig.getPostMax()){
                        return Result.getResultJson(0,"你已超过最大发布数量限制，请您24小时后再操作",null);
                    }else{
                        redisHelp.setRedis(this.dataprefix+"_"+uid+"_spaceNum",space_Num.toString(),apiconfig.getInterceptTime(),redisTemplate);
                    }
                }
            }
            //限制结束
            String  ip = baseFull.getIpAddr(request);


            //判断用户经验值
            Integer spaceMinExp = apiconfig.getSpaceMinExp();
            TypechoUsers curUser = usersService.selectByKey(uid);
            Integer Exp = curUser.getExperience();
            if(Exp < spaceMinExp){
                return Result.getResultJson(0,"发布动态最低要求经验值为"+spaceMinExp+",你当前经验值"+Exp,null);
            }

            //违禁词拦截
            String forbidden = apiconfig.getForbidden();
            Integer intercept = 0;
            Integer isForbidden = baseFull.getForbidden(forbidden,text);
            if(isForbidden.equals(1)){
                intercept = 1;
            }
            if(intercept.equals(1)){
                //以十分钟为检测周期，违禁一次刷新一次，等于4次则禁言
                String isIntercept = redisHelp.getRedis(this.dataprefix+"_"+uid+"_isIntercept",redisTemplate);
                if(isIntercept==null){
                    redisHelp.setRedis(this.dataprefix+"_"+uid+"_isIntercept","1",600,redisTemplate);
                }else{
                    Integer frequency = Integer.parseInt(isIntercept) + 1;
                    if(frequency==4){
                        securityService.safetyMessage("用户ID："+uid+"，在动态发布接口多次触发违禁，请及时确认处理。","system");
                        redisHelp.setRedis(this.dataprefix+"_"+uid+"_silence","1",3600,redisTemplate);
                        return Result.getResultJson(0,"你已多次发送违禁词，被禁言一小时！",null);
                    }else{
                        redisHelp.setRedis(this.dataprefix+"_"+uid+"_isIntercept",frequency.toString(),600,redisTemplate);
                    }

                }
                return Result.getResultJson(0,"内容存在违禁词",null);
            }
            //违禁词拦截结束
            Long date = System.currentTimeMillis();
            String created = String.valueOf(date).substring(0,10);
            if(type.equals(3)){
                TypechoSpace toSpace = service.selectByKey(toid);
                if(toSpace==null){
                    return Result.getResultJson(0,"动态不存在",null);
                }
                if(toSpace.getStatus().equals(0)){
                    return Result.getResultJson(0,"动态还未通过审核",null);
                }
                if(toSpace.getStatus().equals(2)){
                    return Result.getResultJson(0,"动态已锁定，无法评论及转发",null);
                }
            }

            TypechoSpace space = new TypechoSpace();
            text = text.replace("||rn||","\r\n");
            space.setText(text);
            space.setUid(uid);
            space.setType(type);
            space.setPic(pic);
            space.setToid(toid);
            space.setCreated(Integer.parseInt(created));
            space.setModified(Integer.parseInt(created));
            if(apiconfig.getSpaceAudit().equals(1)){
                space.setStatus(0);
            }else{
                space.setStatus(1);
            }
            //修改用户最新发布时间
            TypechoUsers user = new TypechoUsers();
            user.setUid(uid);
            user.setPosttime(Integer.parseInt(created));
            usersService.update(user);
            int rows = service.insert(space);
            editFile.setLog("用户"+uid+"发布了新动态。");
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
            if(!type.equals(0)&&!type.equals(1)&&!type.equals(2)&&!type.equals(3)&&!type.equals(4)&&!type.equals(5)){
                return Result.getResultJson(0,"参数不正确",null);
            }
            //类型不为0时，需要传toid
            if(!type.equals(0)&&!type.equals(4)){
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
            TypechoApiconfig apiconfig = UStatus.getConfig(this.dataprefix,apiconfigService,redisTemplate);
            //登录情况下，刷数据攻击拦截
            String isRepeated = redisHelp.getRedis(this.dataprefix+"_"+uid+"_isAddSpace",redisTemplate);
            if(isRepeated==null){
                redisHelp.setRedis(this.dataprefix+"_"+uid+"_isAddSpace","1",4,redisTemplate);
            }else{
                Integer frequency = Integer.parseInt(isRepeated) + 1;
                if(frequency==4){
                    securityService.safetyMessage("用户ID："+uid+"，在动态编辑接口疑似存在攻击行为，请及时确认处理。","system");
                    redisHelp.setRedis(this.dataprefix+"_"+uid+"_silence","1",600,redisTemplate);
                    return Result.getResultJson(0,"你的操作过于频繁，已被禁言十分钟！",null);
                }else{
                    redisHelp.setRedis(this.dataprefix+"_"+uid+"_isAddSpace",frequency.toString(),5,redisTemplate);
                }
                return Result.getResultJson(0,"你的操作太频繁了",null);
            }

            //攻击拦截结束
            //违禁词拦截

            String forbidden = apiconfig.getForbidden();
            Integer intercept = 0;

            Integer isForbidden = baseFull.getForbidden(forbidden,text);
            if(isForbidden.equals(1)){
                intercept = 1;
            }
            if(intercept.equals(1)){
                //以十分钟为检测周期，违禁一次刷新一次，等于4次则禁言
                String isIntercept = redisHelp.getRedis(this.dataprefix+"_"+uid+"_isIntercept",redisTemplate);
                if(isIntercept==null){
                    redisHelp.setRedis(this.dataprefix+"_"+uid+"_isIntercept","1",600,redisTemplate);
                }else{
                    Integer frequency = Integer.parseInt(isIntercept) + 1;
                    if(frequency==4){
                        securityService.safetyMessage("用户ID："+uid+"，在动态编辑接口接口多次触发违禁，请及时确认处理。","system");
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
            text = text.replace("||rn||","\r\n");
            space.setId(id);
            space.setText(text);
            space.setUid(uid);
            space.setPic(pic);
            space.setToid(toid);
            space.setModified(Integer.parseInt(created));
            int rows = service.update(space);
            editFile.setLog("用户"+uid+"修改了动态"+id);
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
    public String spaceInfo (@RequestParam(value = "id", required = false) Integer  id,
                             @RequestParam(value = "token", required = false) String  token) {
        try{
            Map spaceInfoJson = new HashMap();
            Map cacheInfo = redisHelp.getMapValue(this.dataprefix+"_"+"spaceInfo_"+id,redisTemplate);
            Map map = new HashMap();
            Integer uid = 0;
            //如果开启全局登录，则必须登录才能得到数据
            Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
            TypechoApiconfig apiconfig = UStatus.getConfig(this.dataprefix,apiconfigService,redisTemplate);
            if(apiconfig.getIsLogin().equals(1)){
                if(uStatus==0){
                    return Result.getResultJson(0,"用户未登录或Token验证失败",null);
                }
            }
            if (uStatus != 0) {
                map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
                uid =Integer.parseInt(map.get("uid").toString());
            }
            if(cacheInfo.size()>0){
                spaceInfoJson = cacheInfo;
            }else{
                TypechoSpace space;
                space = service.selectByKey(id);
                spaceInfoJson = JSONObject.parseObject(JSONObject.toJSONString(space), Map.class);
                //获取创建人信息
                Integer userid = space.getUid();
                Map userJson = UserStatus.getUserInfo(userid,apiconfigService,usersService);
                //获取用户等级
                TypechoComments comments = new TypechoComments();
                comments.setAuthorId(userid);
                Integer lv = commentsService.total(comments);
                userJson.put("lv", baseFull.getLv(lv));
                spaceInfoJson.put("userJson",userJson);
                if (uStatus != 0) {
                    TypechoFan fan = new TypechoFan();
                    fan.setUid(uid);
                    fan.setTouid(space.getUid());
                    Integer isFollow = fanService.total(fan);
                    spaceInfoJson.put("isFollow",isFollow);
                    TypechoUserlog userlog = new TypechoUserlog();
                    userlog.setCid(space.getId());
                    userlog.setUid(uid);
                    userlog.setType("spaceLike");
                    Integer isLikes = userlogService.total(userlog);
                    if(isLikes > 0){
                        spaceInfoJson.put("isLikes",1);
                    }else{
                        spaceInfoJson.put("isLikes",0);
                    }
                }else{
                    spaceInfoJson.put("isFollow",0);
                    spaceInfoJson.put("isLikes",0);
                }

                //获取转发，评论
                TypechoSpace dataSpace = new TypechoSpace();
                dataSpace.setType(2);
                dataSpace.setToid(space.getId());
                Integer forward = service.total(dataSpace);
                dataSpace.setType(3);
                Integer reply = service.total(dataSpace);
                spaceInfoJson.put("forward",forward);
                spaceInfoJson.put("reply",reply);

                //对于转发和发布文章
                if(space.getType().equals(1)){
                    Integer cid = space.getToid();
                    Map contentJson = new HashMap();
                    TypechoContents contents = contentsService.selectByKey(cid);
                    if(contents!=null){
                        String text = contents.getText();
                        List imgList = baseFull.getImageSrc(text);
                        text = baseFull.toStrByChinese(text);
                        contentJson.put("cid",contents.getCid());

                        contentJson.put("title",contents.getTitle());
                        contentJson.put("images",imgList);
                        contentJson.put("text",text.length()>300 ? text.substring(0,300) : text);
                        contentJson.put("status",contents.getStatus());
                    }else{
                        contentJson.put("cid",0);
                        contentJson.put("title","该文章已被删除或屏蔽");
                        contentJson.put("text","");
                    }
                    spaceInfoJson.put("contentJson",contentJson);
                }
                //对于转发动态
                if(space.getType().equals(2)){
                    Integer sid = space.getToid();
                    Map forwardJson = new HashMap();
                    TypechoSpace forwardSpace = service.selectByKey(sid);
                    if(forwardSpace!=null){
                        forwardJson = JSONObject.parseObject(JSONObject.toJSONString(forwardSpace), Map.class);
                        Integer spaceUid = forwardSpace.getUid();
                        TypechoUsers spaceUser = usersService.selectByKey(spaceUid);
                        String name = spaceUser.getName();
                        if(spaceUser.getScreenName()!=null){
                            name = spaceUser.getScreenName();
                        }
                        forwardJson.put("username",name);
                    }else{
                        forwardJson.put("id",0);
                        forwardJson.put("username","");
                        forwardJson.put("text","该动态已被删除或屏蔽");
                    }

                    spaceInfoJson.put("forwardJson",forwardJson);
                }
                //对于评论，获取上级动态
                if(space.getType().equals(3)){
                    Integer sid = space.getToid();
                    Map parentJson = new HashMap();
                    TypechoSpace parentSpace = service.selectByKey(sid);
                    if(parentSpace!=null){
                        parentJson = JSONObject.parseObject(JSONObject.toJSONString(parentSpace), Map.class);
                        Integer spaceUid = parentSpace.getUid();
                        TypechoUsers spaceUser = usersService.selectByKey(spaceUid);
                        String name = spaceUser.getName();
                        if(spaceUser.getScreenName()!=null){
                            name = spaceUser.getScreenName();
                        }
                        parentJson.put("username",name);
                    }else{
                        parentJson.put("id",0);
                        parentJson.put("username","");
                        parentJson.put("text","该动态已被删除或屏蔽");
                    }

                    spaceInfoJson.put("parentJson",parentJson);
                }
                //对于商品
                if(space.getType().equals(5)){
                    Integer sid = space.getToid();
                    TypechoShop shop = shopService.selectByKey(sid);
                    Map shopJson = new HashMap();
                    if(shop!=null){
                        shopJson = JSONObject.parseObject(JSONObject.toJSONString(shop), Map.class);
                        Integer shopUid = shop.getUid();
                        TypechoUsers shopUser = usersService.selectByKey(shopUid);
                        String name = shopUser.getName();
                        if(shopUser.getScreenName()!=null){
                            name = shopUser.getScreenName();
                        }
                        shopJson.put("username",name);

                    }else{
                        shopJson.put("id",0);
                        shopJson.put("username","");
                        shopJson.put("title","该商品已被删除或屏蔽");
                    }
                    spaceInfoJson.put("shopJson",shopJson);
                }

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
    public String spaceList (
            @RequestParam(value = "searchParams", required = false) String  searchParams,
            @RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit,
            @RequestParam(value = "searchKey"        , required = false, defaultValue = "") String searchKey,
            @RequestParam(value = "order", required = false, defaultValue = "created") String  order,
            @RequestParam(value = "token", required = false) String  token) {
        if(limit>50){
            limit = 50;
        }
        TypechoSpace query = new TypechoSpace();
        if (StringUtils.isNotBlank(searchParams)) {
            JSONObject object = JSON.parseObject(searchParams);
            query = object.toJavaObject(TypechoSpace.class);


        }
        Map map = new HashMap();
        Integer uid = 0;
        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        TypechoApiconfig apiconfig = UStatus.getConfig(this.dataprefix,apiconfigService,redisTemplate);
        if(apiconfig.getIsLogin().equals(1)){
            if(uStatus==0){
                return Result.getResultJson(0,"用户未登录或Token验证失败",null);
            }
        }
        if (uStatus != 0) {
            map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            uid =Integer.parseInt(map.get("uid").toString());
        }
        List cacheList =  redisHelp.getList(this.dataprefix+"_"+"spaceList_"+page+"_"+limit+"_"+searchKey+"_"+uid+"_"+searchParams,redisTemplate);
        List jsonList = new ArrayList();

        Integer total = service.total(query);
        try{
            if(cacheList.size()>0){
                jsonList = cacheList;
            }else{
                Integer isReply = 0;
                if(query.getType()!=null){
                    if(query.getType().equals(3)){
                        isReply = 1;
                    }
                }
                PageList<TypechoSpace> pageList = service.selectPage(query, page, limit,order,searchKey,isReply);
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
                    //获取用户信息
                    Map userJson = UserStatus.getUserInfo(userid,apiconfigService,usersService);
                    //获取用户等级
                    TypechoComments comments = new TypechoComments();
                    comments.setAuthorId(userid);
                    Integer lv = commentsService.total(comments);
                    userJson.put("lv", baseFull.getLv(lv));
                    json.put("userJson",userJson);
                    if (uStatus != 0) {
                        TypechoFan fan = new TypechoFan();
                        fan.setUid(uid);
                        fan.setTouid(space.getUid());
                        Integer isFollow = fanService.total(fan);
                        json.put("isFollow",isFollow);

                        TypechoUserlog userlog = new TypechoUserlog();
                        userlog.setCid(space.getId());
                        userlog.setType("spaceLike");
                        userlog.setUid(uid);
                        Integer isLikes = userlogService.total(userlog);
                        if(isLikes > 0){
                            json.put("isLikes",1);
                        }else{
                            json.put("isLikes",0);
                        }

                    }else{
                        json.put("isFollow",0);
                        json.put("isLikes",0);
                    }
                    //获取转发，评论
                    TypechoSpace dataSpace = new TypechoSpace();
                    dataSpace.setType(2);
                    dataSpace.setToid(space.getId());
                    Integer forward = service.total(dataSpace);
                    dataSpace.setType(3);
                    Integer reply = service.total(dataSpace);
                    json.put("forward",forward);
                    json.put("reply",reply);

                    //对于转发和发布文章
                    if(space.getType().equals(1)){
                        Integer cid = space.getToid();
                        Map contentJson = new HashMap();
                        TypechoContents contents = contentsService.selectByKey(cid);
                        if(contents!=null){
                            String text = contents.getText();
                            List imgList = baseFull.getImageSrc(text);
                            text = baseFull.toStrByChinese(text);
                            contentJson.put("cid",contents.getCid());
                            contentJson.put("title",contents.getTitle());
                            contentJson.put("images",imgList);
                            contentJson.put("status",contents.getStatus());
                            contentJson.put("text",text.length()>300 ? text.substring(0,300) : text);
                        }else{
                            contentJson.put("cid",0);
                            contentJson.put("title","该文章已被删除或屏蔽");
                            contentJson.put("text","");
                        }
                        json.put("contentJson",contentJson);
                    }
                    //对于转发动态
                    if(space.getType().equals(2)){
                        Integer sid = space.getToid();
                        Map forwardJson = new HashMap();
                        TypechoSpace forwardSpace = service.selectByKey(sid);
                        if(forwardSpace!=null){
                            forwardJson = JSONObject.parseObject(JSONObject.toJSONString(forwardSpace), Map.class);
                            Integer spaceUid = forwardSpace.getUid();
                            TypechoUsers spaceUser = usersService.selectByKey(spaceUid);
                            String name = spaceUser.getName();
                            if(spaceUser.getScreenName()!=null){
                                name = spaceUser.getScreenName();
                            }
                            forwardJson.put("username",name);
                        }else{
                            forwardJson.put("id",0);
                            forwardJson.put("username","");
                            forwardJson.put("text","该动态已被删除或屏蔽");
                        }

                        json.put("forwardJson",forwardJson);
                    }
                    //对于评论，获取上级动态
                    if(space.getType().equals(3)){
                        Integer sid = space.getToid();
                        Map parentJson = new HashMap();
                        TypechoSpace parentSpace = service.selectByKey(sid);
                        if(parentSpace!=null){
                            parentJson = JSONObject.parseObject(JSONObject.toJSONString(parentSpace), Map.class);
                            Integer spaceUid = parentSpace.getUid();
                            TypechoUsers spaceUser = usersService.selectByKey(spaceUid);
                            String name = spaceUser.getName();
                            if(spaceUser.getScreenName()!=null){
                                name = spaceUser.getScreenName();
                            }
                            parentJson.put("username",name);
                        }else{
                            parentJson.put("id",0);
                            parentJson.put("username","");
                            parentJson.put("text","该动态已被删除或屏蔽");
                        }

                        json.put("parentJson",parentJson);
                    }
                    //对于商品
                    if(space.getType().equals(5)){
                        Integer sid = space.getToid();
                        TypechoShop shop = shopService.selectByKey(sid);
                        Map shopJson = new HashMap();
                        if(shop!=null){
                            shopJson = JSONObject.parseObject(JSONObject.toJSONString(shop), Map.class);
                            Integer shopUid = shop.getUid();
                            TypechoUsers shopUser = usersService.selectByKey(shopUid);
                            String name = shopUser.getName();
                            if(shopUser.getScreenName()!=null){
                                name = shopUser.getScreenName();
                            }
                            shopJson.put("username",name);
                            //获取用户等级
                            TypechoComments shopUserComments = new TypechoComments();
                            comments.setAuthorId(shopUser.getUid());
                            Integer userlv = commentsService.total(shopUserComments);
                            shopJson.put("lv", baseFull.getLv(userlv));

                        }else{
                            shopJson.put("id",0);
                            shopJson.put("username","");
                            shopJson.put("title","该商品已被删除或屏蔽");
                        }
                        json.put("shopJson",shopJson);
                    }
                    jsonList.add(json);

                }
                redisHelp.delete(this.dataprefix+"_"+"spaceList_"+page+"_"+limit+"_"+searchKey+"_"+uid+"_"+searchParams,redisTemplate);
                redisHelp.setList(this.dataprefix+"_"+"spaceList_"+page+"_"+limit+"_"+searchKey+"_"+uid+"_"+searchParams,jsonList,5,redisTemplate);
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
     * 动态审核
     */
    @RequestMapping(value = "/spaceReview")
    @ResponseBody
    public String spaceReview(@RequestParam(value = "id", required = false) Integer  id,
                              @RequestParam(value = "type", required = false, defaultValue = "1") Integer  type,
                              @RequestParam(value = "token", required = false) String  token) {
        try{
            if(!type.equals(1)&&!type.equals(0)){
                return Result.getResultJson(0,"参数错误",null);
            }
            Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
            if(uStatus==0){
                return Result.getResultJson(0,"用户未登录或Token验证失败",null);
            }
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer uid  = Integer.parseInt(map.get("uid").toString());
            String group = map.get("group").toString();
            if(!group.equals("administrator")&&!group.equals("editor")){
                return Result.getResultJson(0,"你没有操作权限",null);
            }
            TypechoSpace space = service.selectByKey(id);
            if(space==null){
                return Result.getResultJson(0,"动态不存在",null);
            }
            if(space.getStatus().equals(type)){
                return Result.getResultJson(0,"动态已被进行相同操作",null);
            }
            int rows = 0;
            if(type.equals(1)){
                TypechoSpace newPost = new TypechoSpace();
                newPost.setId(id);
                newPost.setStatus(type);
                rows = service.update(newPost);
            }else{
                rows = service.delete(id);
            }
            Long date = System.currentTimeMillis();
            String created = String.valueOf(date).substring(0,10);
            TypechoInbox insert = new TypechoInbox();
            insert.setUid(uid);
            insert.setTouid(space.getUid());
            insert.setType("system");
            if(type.equals(1)){
                insert.setText("你的动态已审核通过");
            }
            if(type.equals(0)){
                insert.setText("你的动态未审核通过，已被删除");
            }
            insert.setCreated(Integer.parseInt(created));
            inboxService.insert(insert);

            JSONObject response = new JSONObject();
            response.put("code" ,rows > 0 ? 1: 0 );
            response.put("data" , rows);
            response.put("msg"  , rows > 0 ? "操作成功，请等待缓存刷新" : "操作失败");
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }
    }
    /***
     * 动态锁定&解锁
     */
    @RequestMapping(value = "/spaceLock")
    @ResponseBody
    public String postLock(@RequestParam(value = "id", required = false) Integer  id,
                           @RequestParam(value = "type", required = false, defaultValue = "1") Integer  type,
                           @RequestParam(value = "token", required = false) String  token) {
        try{
            if(!type.equals(1)&&!type.equals(2)){
                return Result.getResultJson(0,"参数错误",null);
            }
            Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
            if(uStatus==0){
                return Result.getResultJson(0,"用户未登录或Token验证失败",null);
            }
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer uid  = Integer.parseInt(map.get("uid").toString());
            String group = map.get("group").toString();
            if(!group.equals("administrator")&&!group.equals("editor")){
                return Result.getResultJson(0,"你没有操作权限",null);
            }
            TypechoSpace space = service.selectByKey(id);
            if(space==null){
                return Result.getResultJson(0,"动态不存在",null);
            }
            if(space.getStatus().equals(0)){
                return Result.getResultJson(0,"动态未过审，暂无法操作",null);
            }
            if(space.getStatus().equals(type)){
                return Result.getResultJson(0,"动态已被进行相同操作",null);
            }

            TypechoSpace newSpace = new TypechoSpace();
            newSpace.setId(id);
            newSpace.setStatus(type);
            int rows = service.update(newSpace);

            Long date = System.currentTimeMillis();
            String created = String.valueOf(date).substring(0,10);
            TypechoInbox insert = new TypechoInbox();
            insert.setUid(uid);
            insert.setTouid(space.getUid());
            insert.setType("system");
            if(type.equals(1)){
                insert.setText("你的动态【ID:"+space.getId()+"】已被解锁");
            }
            if(type.equals(2)){
                insert.setText("你的动态【ID:"+space.getId()+"】已被锁定");
            }
            insert.setCreated(Integer.parseInt(created));
            inboxService.insert(insert);

            JSONObject response = new JSONObject();
            response.put("code" ,rows > 0 ? 1: 0 );
            response.put("data" , rows);
            response.put("msg"  , rows > 0 ? "操作成功" : "操作失败");
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }
    }

    /***
     * 我关注的人的动态
     */
    @RequestMapping(value = "/myFollowSpace")
    @ResponseBody
    public String followList(@RequestParam(value = "token", required = false) String  token,
                             @RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                             @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit) {
        if(limit>50){
            limit = 50;
        }
        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }
        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid =Integer.parseInt(map.get("uid").toString());
        TypechoFan query = new TypechoFan();
        List jsonList = new ArrayList();
        List cacheList = redisHelp.getList(this.dataprefix+"_"+"myFollowSpace_"+page+"_"+limit+"_"+uid,redisTemplate);
        query.setUid(uid);
        Integer total = fanService.total(query);
        try{
            if(cacheList.size()>0){
                jsonList = cacheList;
            }else{
                PageList<TypechoFan> pageList = fanService.selectUserPage(query, page, limit);
                List<TypechoFan> list = pageList.getList();
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
                    TypechoFan fan = list.get(i);
                    Integer userid = fan.getTouid();
                    //获取用户信息
                    Map userJson = UserStatus.getUserInfo(userid,apiconfigService,usersService);
                    //获取用户等级
                    TypechoComments comments = new TypechoComments();
                    comments.setAuthorId(userid);
                    Integer lv = commentsService.total(comments);
                    userJson.put("lv", baseFull.getLv(lv));
                    json.put("userJson",userJson);
                    //获取用户动态数据
                    TypechoSpace space = new TypechoSpace();
                    space.setUid(userid);
                    List<TypechoSpace> spaceList = service.selectList(space);
                    if(spaceList.size()>0){
                        space = spaceList.get(0);
                        Map spaceJson = JSONObject.parseObject(JSONObject.toJSONString(space), Map.class);
                        json.put("spaceJson",spaceJson);
                    }
                    json.put("spaceNum",spaceList.size());
                    jsonList.add(json);
                }
                redisHelp.delete(this.dataprefix+"_"+"myFollowSpace_"+page+"_"+limit+"_"+uid,redisTemplate);
                redisHelp.setList(this.dataprefix+"_"+"myFollowSpace_"+page+"_"+limit+"_"+uid,jsonList,3,redisTemplate);
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
     * 动态删除
     */
    @RequestMapping(value = "/spaceDelete")
    @ResponseBody
    public String spaceDelete(@RequestParam(value = "id", required = false) String  id, @RequestParam(value = "token", required = false) String  token) {
        try {
            Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
            if(uStatus==0){
                return Result.getResultJson(0,"用户未登录或Token验证失败",null);
            }

            //String group = (String) redisHelp.getValue("userInfo"+token,"group",redisTemplate);
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer uid  = Integer.parseInt(map.get("uid").toString());
            // 查询发布者是不是自己，如果是管理员则跳过
            TypechoSpace space = service.selectByKey(id);
            String group = map.get("group").toString();
            if(!group.equals("administrator")&&!group.equals("editor")){
                if(!space.getUid().equals(uid)){
                    return Result.getResultJson(0,"你没有操作权限",null);
                }
            }else{
                Integer aid = space.getUid();
                //如果管理员不是评论发布者，则发送消息给用户（但不推送通知）
                if(!aid.equals(uid)){
                    Long date = System.currentTimeMillis();
                    String created = String.valueOf(date).substring(0,10);
                    TypechoInbox insert = new TypechoInbox();
                    insert.setUid(uid);
                    insert.setTouid(aid);
                    insert.setType("system");
                    insert.setText("你的动态【"+space.getText()+"】已被删除");
                    insert.setCreated(Integer.parseInt(created));
                    inboxService.insert(insert);
                }
            }

            int rows = service.delete(id);
            editFile.setLog("用户"+uid+"删除了动态"+id);
            JSONObject response = new JSONObject();
            response.put("code" ,rows > 0 ? 1: 0 );
            response.put("data" , rows);
            response.put("msg"  , rows > 0 ? "操作成功" : "操作失败");
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }
    }
    /***
     * 动态点赞
     */
    @RequestMapping(value = "/spaceLikes")
    @ResponseBody
    public String spaceLikes(@RequestParam(value = "id", required = false) Integer  id, @RequestParam(value = "token", required = false) String  token) {
        try{
            Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
            if(uStatus==0){
                return Result.getResultJson(0,"用户未登录或Token验证失败",null);
            }

            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer uid  = Integer.parseInt(map.get("uid").toString());
            Long date = System.currentTimeMillis();
            String userTime = String.valueOf(date).substring(0,10);

            //生成操作日志
            TypechoUserlog userlog = new TypechoUserlog();
            userlog.setUid(uid);
            userlog.setCid(id);
            Integer isLikes = userlogService.total(userlog);
            if(isLikes>0){
                return Result.getResultJson(0,"你已经点赞过了",null);
            }
            TypechoSpace space = service.selectByKey(id);
            if(space==null){
                return Result.getResultJson(0,"该动态不存在",null);
            }
            userlog.setType("spaceLike");
            userlog.setCreated(Integer.parseInt(userTime));
            userlogService.insert(userlog);
            Integer likes = space.getLikes();
            likes = likes + 1;
            TypechoSpace newSpace = new TypechoSpace();
            newSpace.setLikes(likes);
            newSpace.setId(id);
            int rows = service.update(newSpace);
            JSONObject response = new JSONObject();
            response.put("code" ,rows > 0 ? 1: 0 );
            response.put("data" , rows);
            response.put("msg"  , rows > 0 ? "点赞成功" : "点赞失败");
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }

    }

}
