package com.RuleApi.web;

import com.RuleApi.annotation.LoginRequired;
import com.RuleApi.common.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.RuleApi.entity.*;
import com.RuleApi.service.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 控制层
 * TypechoSpaceController
 * @author buxia97
 * @date 2023/02/05
 */
@Controller
@RequestMapping(value = "/typechoSpace")
public class SpaceController {

    @Autowired
    SpaceService service;

    @Autowired
    private SecurityService securityService;
    @Autowired
    private AllconfigService allconfigService;

    @Autowired
    private UsersService usersService;

    @Autowired
    private ContentsService contentsService;

    @Autowired
    private UserlogService userlogService;

    @Autowired
    private ShopService shopService;

    @Autowired
    private CommentsService commentsService;

    @Autowired
    private FanService fanService;

    @Value("${web.prefix}")
    private String dataprefix;

    @Value("${mybatis.configuration.variables.prefix}")
    private String prefix;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;


    @Autowired
    private InboxService inboxService;

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
    @LoginRequired(purview = "0")
    public String addSpace (@RequestParam(value = "text", required = false, defaultValue = "") String  text,
                            @RequestParam(value = "type", required = false, defaultValue = "0") Integer  type,
                            @RequestParam(value = "toid", required = false, defaultValue = "0") Integer  toid,
                            @RequestParam(value = "pic", required = false) String  pic,
                            @RequestParam(value = "onlyMe", required = false, defaultValue = "0") Integer  onlyMe,
                            @RequestParam(value = "token", required = false) String  token,
                            @RequestParam(value = "verifyCode", required = false) String verifyCode,
                            HttpServletRequest request) {
        try{
            String  ip = baseFull.getIpAddr(request);
            if(!type.equals(0)&&!type.equals(1)&&!type.equals(2)&&!type.equals(3)&&!type.equals(4)&&!type.equals(5)&&!type.equals(6)){
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
            if(text.length()>1500){
                return Result.getResultJson(0,"最大动态内容为1500字符",null);
            }
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer uid =Integer.parseInt(map.get("uid").toString());

            String isSilence = redisHelp.getRedis(this.dataprefix+"_"+uid+"_silence",redisTemplate);
            if(isSilence!=null){
                return Result.getResultJson(0,"你的操作太频繁了，请稍后再试",null);
            }
            Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
            if(Integer.parseInt(apiconfig.get("verifyLevel").toString())>1) {
                if (StringUtils.isEmpty(verifyCode)) {
                    return Result.getResultJson(0,"图片验证码不能为空",null);
                }
                String kaptchaCode = redisHelp.getRedis(this.dataprefix+"_"+ip+"_verifyCode",redisTemplate);
                if (StringUtils.isEmpty(kaptchaCode) || !verifyCode.equals(kaptchaCode)) {
                    return Result.getResultJson(0,"图片验证码错误",null);
                }
            }
            //登录情况下，刷数据攻击拦截
            if(apiconfig.get("banRobots").toString().equals("1")) {
                String isRepeated = redisHelp.getRedis(this.dataprefix+"_"+uid+"_isAddSpace",redisTemplate);
                if(isRepeated==null){
                    redisHelp.setRedis(this.dataprefix+"_"+uid+"_isAddSpace","1",4,redisTemplate);
                }else{
                    Integer frequency = Integer.parseInt(isRepeated) + 1;
                    if(frequency==4){
                        securityService.safetyMessage("用户ID："+uid+"，在聊天发送消息接口疑似存在攻击行为，请及时确认处理。","system");
                        redisHelp.setRedis(this.dataprefix+"_"+uid+"_silence","1",Integer.parseInt(apiconfig.get("silenceTime").toString()),redisTemplate);
                        return Result.getResultJson(0,"你的操作过于频繁，已被禁言十分钟！",null);
                    }else{
                        redisHelp.setRedis(this.dataprefix+"_"+uid+"_isAddSpace",frequency.toString(),5,redisTemplate);
                    }
                    return Result.getResultJson(0,"你的操作太频繁了",null);
                }
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
                    Integer postMax = 0;
                    if(apiconfig.get("postMax")!=null){
                        postMax = Integer.parseInt(apiconfig.get("postMax").toString());
                    }
                    if(space_Num > postMax){
                        return Result.getResultJson(0,"你已超过最大发布数量限制，请您24小时后再操作",null);
                    }else{
                        redisHelp.setRedis(this.dataprefix+"_"+uid+"_spaceNum",space_Num.toString(),86400,redisTemplate);
                    }
                }
            }
            //限制结束
            //判断用户经验值
            Integer spaceMinExp = 0;
            if(apiconfig.get("spaceMinExp")!=null){
                spaceMinExp = Integer.parseInt(apiconfig.get("spaceMinExp").toString());
            }
            TypechoUsers curUser = usersService.selectByKey(uid);
            Integer Exp = curUser.getExperience();
            if(Exp < spaceMinExp){
                return Result.getResultJson(0,"发布动态最低要求经验值为"+spaceMinExp+",你当前经验值"+Exp,null);
            }
            //判断是否开启内容发布限制
            Integer postRestrict = 0;
            if(apiconfig.get("postRestrict")!=null){
                postRestrict = Integer.parseInt(apiconfig.get("postRestrict").toString());
            }
            if(postRestrict>0){
                TypechoUsers users = usersService.selectByKey(uid);
                if(postRestrict.equals(1)) {
                    if (StringUtils.isEmpty(users.getMail())) {
                        return Result.getResultJson(0, "发布内容前，请先绑定邮箱", null);
                    }
                }

                if(postRestrict.equals(2)) {
                    if (StringUtils.isEmpty(users.getPhone())) {
                        return Result.getResultJson(0, "发布内容前，请先绑定手机号", null);
                    }
                }

                if(postRestrict.equals(3)) {
                    if (StringUtils.isEmpty(users.getPhone()) || StringUtils.isEmpty(users.getMail())) {
                        return Result.getResultJson(0, "发布内容前，请先绑定手机号及邮箱", null);
                    }
                }
            }
            //违禁词拦截
            String forbidden = "";
            if(apiconfig.get("forbidden")!=null){
                forbidden = apiconfig.get("forbidden").toString();
            }
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
                        redisHelp.setRedis(this.dataprefix+"_"+uid+"_silence","1",Integer.parseInt(apiconfig.get("interceptTime").toString()),redisTemplate);
                        return Result.getResultJson(0,"你已多次发送违禁词，被禁言一小时！",null);
                    }else{
                        redisHelp.setRedis(this.dataprefix+"_"+uid+"_isIntercept",frequency.toString(),600,redisTemplate);
                    }

                }
                return Result.getResultJson(0,"内容存在违禁词",null);
            }
            //违禁词拦截结束
            //实名认证拦截
            if(apiconfig.get("identifyPost").toString().equals("1")) {
                if(UStatus.isIdentify(uid,prefix,jdbcTemplate).equals(0)){
                    return Result.getResultJson(0,"请先完成身份认证",null);
                }
            }


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
            space.setOnlyMe(onlyMe);
            space.setCreated(Integer.parseInt(created));
            space.setModified(Integer.parseInt(created));
            Integer spaceAudit = Integer.parseInt(apiconfig.get("spaceAudit").toString());
            if(spaceAudit.equals(1)){
                space.setStatus(0);
            }else{
                space.setStatus(1);
            }
            //腾讯云内容违规检测
            if(apiconfig.get("cmsSwitch").toString().equals("1")|apiconfig.get("cmsSwitch").toString().equals("3")){
                try{
                    String setText = baseFull.htmlToText(text);
                    setText = baseFull.encrypt(setText);
                    Map violationData = securityService.textViolation(setText);
                    String Suggestion = violationData.get("Suggestion").toString();
                    if(Suggestion.equals("Block")){
                        Set<String> allKeywords = new HashSet<>();
                        Object keywordObj = violationData.get("Keywords");
                        if (keywordObj instanceof List) {
                            List<?> keywords = (List<?>) keywordObj;
                            for (Object word : keywords) {
                                if (word != null) {
                                    allKeywords.add(word.toString());
                                }
                            }
                        }
                        String textBlockTips = "内容涉及违规，违规内容：" + String.join("，", allKeywords);
                        return Result.getResultJson(0,textBlockTips,null);
                    }
                    if(Suggestion.equals("Review")){
                        space.setStatus(0);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            //修改用户最新发布时间和IP
            TypechoUsers updateUser = new TypechoUsers();
            updateUser.setUid(uid);
            updateUser.setPosttime(Integer.parseInt(created));
            updateUser.setIp(ip);
            updateUser.setLocal(baseFull.getLocal(ip));
            int rows = service.insert(space);
            if(spaceAudit.equals(0)){
                //如果无需审核，则立即增加经验
                Integer postExp = Integer.parseInt(apiconfig.get("postExp").toString());

                if(postExp>0){
                    //生成操作记录

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                    String curtime = sdf.format(new Date(date));
                    TypechoUserlog userlog = new TypechoUserlog();
                    userlog.setUid(uid);
                    //cid用于存放真实时间
                    userlog.setCid(Integer.parseInt(curtime));
                    userlog.setType("postExp");
                    Integer size = userlogService.total(userlog);
                    //只有前三次发布内容获得经验
                    if(size < 3){
                        userlog.setNum(postExp);
                        userlog.setCreated(Integer.parseInt(created));
                        userlogService.insert(userlog);
                        //修改用户资产
                        TypechoUsers oldUser = usersService.selectByKey(uid);
                        Integer experience = oldUser.getExperience();
                        experience = experience + postExp;
                        updateUser.setExperience(experience);
                    }
                }
            }
            usersService.update(updateUser);
            editFile.setLog("用户"+uid+"发布了新动态。");
            String addTips = "";
            if(spaceAudit.equals(1)){
                addTips = "请等待管理员审核";
            }
            JSONObject response = new JSONObject();
            response.put("code" , rows);
            response.put("msg"  , rows > 0 ? "发布成功"+addTips : "发布失败");
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
    @LoginRequired(purview = "0")
    public String editSpace (
            @RequestParam(value = "id", required = false) Integer  id,
            @RequestParam(value = "text", required = false, defaultValue = "") String  text,
            @RequestParam(value = "type", required = false, defaultValue = "0") Integer  type,
            @RequestParam(value = "toid", required = false, defaultValue = "0") Integer  toid,
            @RequestParam(value = "pic", required = false) String  pic,
            @RequestParam(value = "onlyMe", required = false, defaultValue = "0") Integer  onlyMe,
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
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer uid =Integer.parseInt(map.get("uid").toString());
            String group = map.get("group").toString();
            String isSilence = redisHelp.getRedis(this.dataprefix+"_"+uid+"_silence",redisTemplate);
            if(isSilence!=null){
                return Result.getResultJson(0,"你的操作太频繁了，请稍后再试",null);
            }
            Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
            //登录情况下，刷数据攻击拦截
            if(apiconfig.get("banRobots").toString().equals("1")) {
                String isRepeated = redisHelp.getRedis(this.dataprefix+"_"+uid+"_isAddSpace",redisTemplate);
                if(isRepeated==null){
                    redisHelp.setRedis(this.dataprefix+"_"+uid+"_isAddSpace","1",4,redisTemplate);
                }else{
                    Integer frequency = Integer.parseInt(isRepeated) + 1;
                    if(frequency==4){
                        securityService.safetyMessage("用户ID："+uid+"，在动态编辑接口接口疑似存在攻击行为，请及时确认处理。","system");
                        redisHelp.setRedis(this.dataprefix+"_"+uid+"_silence","1",Integer.parseInt(apiconfig.get("silenceTime").toString()),redisTemplate);
                        return Result.getResultJson(0,"你的操作过于频繁，已被禁言十分钟！",null);
                    }else{
                        redisHelp.setRedis(this.dataprefix+"_"+uid+"_isAddSpace",frequency.toString(),5,redisTemplate);
                    }
                    return Result.getResultJson(0,"你的操作太频繁了",null);
                }
            }


            //攻击拦截结束
            //违禁词拦截

            String forbidden = "";
            if(apiconfig.get("forbidden")!=null){
                forbidden = apiconfig.get("forbidden").toString();
            }
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
                        redisHelp.setRedis(this.dataprefix+"_"+uid+"_silence","1",Integer.parseInt(apiconfig.get("interceptTime").toString()),redisTemplate);
                        return Result.getResultJson(0,"你已多次发送违禁词，被禁言一小时！",null);
                    }else{
                        redisHelp.setRedis(this.dataprefix+"_"+uid+"_isIntercept",frequency.toString(),600,redisTemplate);
                    }

                }
                return Result.getResultJson(0,"动态存在违禁词",null);
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
            space.setOnlyMe(onlyMe);
            space.setModified(Integer.parseInt(created));
            //腾讯云内容违规检测
            if(apiconfig.get("cmsSwitch").toString().equals("1")|apiconfig.get("cmsSwitch").toString().equals("3")){
                try{
                    String setText = baseFull.htmlToText(text);
                    setText = baseFull.encrypt(setText);
                    Map violationData = securityService.textViolation(setText);
                    String Suggestion = violationData.get("Suggestion").toString();
                    if(Suggestion.equals("Block")){
                        Set<String> allKeywords = new HashSet<>();
                        Object keywordObj = violationData.get("Keywords");
                        if (keywordObj instanceof List) {
                            List<?> keywords = (List<?>) keywordObj;
                            for (Object word : keywords) {
                                if (word != null) {
                                    allKeywords.add(word.toString());
                                }
                            }
                        }
                        String textBlockTips = "评论内容涉及违规，违规内容：" + String.join("，", allKeywords);
                        return Result.getResultJson(0,textBlockTips,null);
                    }
                    if(Suggestion.equals("Review")){
                        space.setStatus(0);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
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
    /***
     * 动态审核
     */
    @RequestMapping(value = "/spaceReview")
    @ResponseBody
    @LoginRequired(purview = "1")
    public String spaceReview(@RequestParam(value = "id", required = false) Integer  id,
                              @RequestParam(value = "type", required = false, defaultValue = "1") Integer  type,
                              @RequestParam(value = "token", required = false) String  token) {
        try{
            if(!type.equals(1)&&!type.equals(0)){
                return Result.getResultJson(0,"参数错误",null);
            }
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer uid  = Integer.parseInt(map.get("uid").toString());
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
            if(rows > 0){
                redisHelp.deleteKeysWithPattern("*"+this.dataprefix+"_spaceList_1*",redisTemplate,this.dataprefix);
            }
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
    @LoginRequired(purview = "1")
    public String postLock(@RequestParam(value = "id", required = false) Integer  id,
                           @RequestParam(value = "type", required = false, defaultValue = "1") Integer  type,
                           @RequestParam(value = "token", required = false) String  token) {
        try{
            if(!type.equals(1)&&!type.equals(2)){
                return Result.getResultJson(0,"参数错误",null);
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
            response.put("msg"  , rows > 0 ? "操作成功，请等待缓存刷新" : "操作失败");
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
    @LoginRequired(purview = "-1")
    public String spaceInfo (@RequestParam(value = "id", required = false) Integer  id,
                             @RequestParam(value = "token", required = false) String  token) {
        try{
            Map spaceInfoJson = new HashMap();
            Map cacheInfo = redisHelp.getMapValue(this.dataprefix+"_"+"spaceInfo_"+id,redisTemplate);
            Map map = new HashMap();
            Integer uid = 0;
            String group = "";
            //如果开启全局登录，则必须登录才能得到数据
            Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
            Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
            if(apiconfig.get("isLogin").toString().equals("1")){
                if(uStatus==0){
                    return Result.getResultJson(0,"用户未登录或Token验证失败",null);
                }
            }
            //验证结束
            if (uStatus != 0) {
                map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
                uid =Integer.parseInt(map.get("uid").toString());
                group = map.get("group").toString();
            }
            if(cacheInfo.size()>0){
                spaceInfoJson = cacheInfo;
            }else{
                TypechoSpace space = service.selectByKey(id);
                //获取自己被拉黑的情况
                if(space.getUid()!=null){
                    TypechoUserlog userlog = new TypechoUserlog();
                    userlog.setType("banUser");
                    userlog.setUid(space.getUid());
                    userlog.setNum(uid);
                    Integer ban = userlogService.total(userlog);
                    if(ban>0){
                        return Result.getResultJson(0,"由于作者设置，您无法查看内容！",null);
                    }
                }
                Integer onlyMe = space.getOnlyMe();
                if(onlyMe.equals(1)&&!space.getUid().equals(uid)&&group.equals("administrator")&&group.equals("editor")){
                    return Result.getResultJson(0,"该动态被设置为仅自己可见",null);
                }
                String spaceText = space.getText();
                String forbidden = "";
                if(apiconfig.get("forbidden")!=null){
                    forbidden = apiconfig.get("forbidden").toString();
                }
                Integer textForbidden = baseFull.getForbidden(forbidden,spaceText);
                if(textForbidden.equals(1)){
                    spaceText = "内容违规，无法展示";
                    space.setText(spaceText);
                }
                spaceInfoJson = JSONObject.parseObject(JSONObject.toJSONString(space), Map.class);
                //获取创建人信息
                Integer userid = space.getUid();
                Map userJson = UserStatus.getUserInfo(userid,allconfigService,usersService);
                //获取用户等级
//                TypechoComments comments = new TypechoComments();
//                comments.setAuthorId(userid);
//                Integer lv = commentsService.total(comments,null);
//                userJson.put("lv", baseFull.getLv(lv));
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
                Integer forward = service.total(dataSpace,null);
                dataSpace.setType(3);
                Integer reply = service.total(dataSpace,null);
                spaceInfoJson.put("forward",forward);
                spaceInfoJson.put("reply",reply);

                //对于转发和发布文章
                if(space.getType().equals(1)){
                    Integer cid = space.getToid();
                    Map contentJson = new HashMap();
                    TypechoContents contents = contentsService.selectByKey(cid);
                    if(contents!=null){
                        String text = contents.getText();
                        boolean markdownStatus = text.contains("<!--markdown-->");
                        List imgList = new ArrayList<>();
                        if(markdownStatus){
                            imgList = baseFull.getImageSrcFromMarkdown(text);
                        }else{
                            imgList = baseFull.extractImageSrcFromHtml(text);
                        }
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
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }
    }

    /***
     * 动态列表
     */
    @RequestMapping(value = "/spaceList")
    @ResponseBody
    @LoginRequired(purview = "-1")
    public String spaceList (
            @RequestParam(value = "searchParams", required = false) String  searchParams,
            @RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit,
            @RequestParam(value = "searchKey"        , required = false, defaultValue = "") String searchKey,
            @RequestParam(value = "order", required = false, defaultValue = "created") String  order,
            @RequestParam(value = "isManage", required = false, defaultValue = "0") Integer  isManage,
            @RequestParam(value = "token", required = false) String  token) {
        if(limit>50){
            limit = 50;
        }
        Map map = new HashMap();
        Integer uid = 0;
        String group = "";
        //如果开启全局登录，则必须登录才能得到数据
        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
        if(apiconfig.get("isLogin").toString().equals("1")){
            if(uStatus==0){
                return Result.getResultJson(0,"用户未登录或Token验证失败",null);
            }
        }
        //验证结束
        if (uStatus != 0) {
            map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            uid =Integer.parseInt(map.get("uid").toString());
            group = map.get("group").toString();
        }
        String sqlParams = "null";
        TypechoSpace query = new TypechoSpace();
        if (StringUtils.isNotBlank(searchParams)) {
            JSONObject object = JSON.parseObject(searchParams);
            query = object.toJavaObject(TypechoSpace.class);
            //获取自己被拉黑的情况
            if(uStatus > 0){
                if(query.getUid()!=null){
                    TypechoUserlog userlog = new TypechoUserlog();
                    userlog.setType("banUser");
                    userlog.setUid(query.getUid());
                    userlog.setNum(uid);
                    Integer ban = userlogService.total(userlog);
                    if(ban>0){
                        return Result.getResultJson(0,"由于作者设置，您无法查阅内容！",null);
                    }
                }
            }
            Map paramsJson = JSONObject.parseObject(JSONObject.toJSONString(query), Map.class);
            sqlParams = paramsJson.toString();

        }

        List cacheList =  redisHelp.getList(this.dataprefix+"_"+"spaceList_"+page+"_"+limit+"_"+searchKey+"_"+uid+"_"+order+"_"+sqlParams+"_"+isManage,redisTemplate);
        List jsonList = new ArrayList();

        Integer total = service.total(query,null);
        try{
            if(cacheList.size()>0){
                jsonList = cacheList;
            }else{
                Integer isReply = 0;
                if(isManage.equals(1)){
                    isReply = -1;
                }else{
                    if(query.getType()!=null){
                        if(query.getType().equals(3)){
                            isReply = 1;
                        }
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
                    Integer onlyMe = space.getOnlyMe();
                    //如果帖子没有开启仅自己可见，或帖子属于本人发布，则显示在列表
                    if(onlyMe.equals(0)||space.getUid().equals(uid)||group.equals("administrator")||group.equals("editor")){
                        Integer userid = space.getUid();
                        //获取用户信息
                        Map userJson = UserStatus.getUserInfo(userid,allconfigService,usersService);
                        //获取用户等级
//                        TypechoComments comments = new TypechoComments();
//                        comments.setAuthorId(userid);
//                        Integer lv = commentsService.total(comments,searchKey);
//                        userJson.put("lv", baseFull.getLv(lv));
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
                        Integer forward = service.total(dataSpace,null);
                        dataSpace.setType(3);
                        Integer reply = service.total(dataSpace,null);
                        json.put("forward",forward);
                        json.put("reply",reply);

                        //对于转发和发布文章
                        if(space.getType().equals(1)){
                            Integer cid = space.getToid();
                            Map contentJson = new HashMap();
                            TypechoContents contents = contentsService.selectByKey(cid);
                            if(contents!=null){
                                String text = contents.getText();
                                boolean markdownStatus = text.contains("<!--markdown-->");
                                List imgList = new ArrayList<>();
                                if(markdownStatus){
                                    imgList = baseFull.getImageSrcFromMarkdown(text);
                                }else{
                                    imgList = baseFull.extractImageSrcFromHtml(text);
                                }
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
//                                TypechoComments shopUserComments = new TypechoComments();
//                                comments.setAuthorId(shopUser.getUid());
//                                Integer userlv = commentsService.total(shopUserComments,null);
//                                shopJson.put("lv", baseFull.getLv(userlv));

                            }else{
                                shopJson.put("id",0);
                                shopJson.put("username","");
                                shopJson.put("title","该商品已被删除或屏蔽");
                            }
                            json.put("shopJson",shopJson);
                        }
                        jsonList.add(json);
                    }


                }
                redisHelp.delete(this.dataprefix+"_"+"spaceList_"+page+"_"+limit+"_"+searchKey+"_"+uid+"_"+order+"_"+sqlParams+"_"+isManage,redisTemplate);
                redisHelp.setList(this.dataprefix+"_"+"spaceList_"+page+"_"+limit+"_"+searchKey+"_"+uid+"_"+order+"_"+sqlParams+"_"+isManage,jsonList,5,redisTemplate);
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
    @LoginRequired(purview = "1")
    public String spaceDelete(@RequestParam(value = "id", required = false) String  id, @RequestParam(value = "token", required = false) String  token) {
        try {

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
    @LoginRequired(purview = "0")
    public String spaceLikes(@RequestParam(value = "id", required = false) Integer  id, @RequestParam(value = "token", required = false) String  token) {
        try{

            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer uid  = Integer.parseInt(map.get("uid").toString());
            Long date = System.currentTimeMillis();
            String userTime = String.valueOf(date).substring(0,10);

            //生成操作日志
            TypechoUserlog userlog = new TypechoUserlog();
            userlog.setUid(uid);
            userlog.setCid(id);
            userlog.setType("spaceLike");
            Integer isLikes = userlogService.total(userlog);
            if(isLikes>0){
                return Result.getResultJson(0,"你已经点赞过了",null);
            }
            TypechoSpace space = service.selectByKey(id);
            if(space==null){
                return Result.getResultJson(0,"该动态不存在",null);
            }

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
    /***
     * 我关注的人的动态
     */
    @RequestMapping(value = "/followSpace")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String followSpace(@RequestParam(value = "token", required = false) String  token,
                              @RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                              @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit){
        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        page = page - 1;

        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid  = Integer.parseInt(map.get("uid").toString());
        List jsonList = new ArrayList();
        List cacheList = redisHelp.getList(this.dataprefix+"_"+"followSpace_"+uid+"_"+page+"_"+limit,redisTemplate);
        try {
            if (cacheList.size() > 0) {
                jsonList = cacheList;
            } else {
                //获取今日签到列表
                System.out.println("SELECT space.* FROM "+prefix+"_space AS space JOIN "+prefix+"_fan AS fan ON space.uid = fan.touid WHERE fan.uid = "+uid+" AND space.status = 1 ORDER BY space.created  DESC limit "+page+", "+limit+";");

//                List list = jdbcTemplate.queryForObject("SELECT space.* FROM "+prefix+"_space AS space JOIN "+prefix+"_fan AS fan ON space.uid = fan.touid WHERE fan.uid = "+uid+" AND space.status = 1 ORDER BY space.created  DESC limit "+page+", "+limit+";", List.class);
//                System.out.println("SELECT space.* FROM "+prefix+"_space AS space JOIN "+prefix+"_fan AS fan ON space.uid = fan.touid WHERE fan.uid = "+uid+" AND space.status = 1 ORDER BY space.created  DESC limit "+page+", "+limit+";");
                String sql = "SELECT space.* FROM "+prefix+"_space AS space JOIN "+prefix+"_fan AS fan ON space.uid = fan.touid WHERE fan.uid = ? AND space.status = 1 ORDER BY space.created DESC LIMIT ?, ?";
                List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, uid, page, limit);
                if(list.size() < 1){
                    JSONObject noData = new JSONObject();
                    noData.put("code" , 1);
                    noData.put("msg"  , "");
                    noData.put("data" , new ArrayList());
                    noData.put("count", 0);
                    return noData.toString();
                }
                for (int i = 0; i < list.size(); i++) {
                    Map json = JSONObject.parseObject(JSONObject.toJSONString(list.get(i)), Map.class);
                    TypechoSpace space = JSON.parseObject(JSON.toJSONString(json), TypechoSpace.class);
                    Integer userid = space.getUid();
                    //获取用户信息
                    Map userJson = UserStatus.getUserInfo(userid,allconfigService,usersService);
                    //获取用户等级
//                    TypechoComments comments = new TypechoComments();
//                    comments.setAuthorId(userid);
//                    Integer lv = commentsService.total(comments,null);
//                    userJson.put("lv", baseFull.getLv(lv));
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
                    Integer forward = service.total(dataSpace,null);
                    dataSpace.setType(3);
                    Integer reply = service.total(dataSpace,null);
                    json.put("forward",forward);
                    json.put("reply",reply);

                    //对于转发和发布文章
                    if(space.getType().equals(1)){
                        Integer cid = space.getToid();
                        Map contentJson = new HashMap();
                        TypechoContents contents = contentsService.selectByKey(cid);
                        if(contents!=null){
                            String text = contents.getText();
                            boolean markdownStatus = text.contains("<!--markdown-->");
                            List imgList = new ArrayList<>();
                            if(markdownStatus){
                                imgList = baseFull.getImageSrcFromMarkdown(text);
                            }else{
                                imgList = baseFull.extractImageSrcFromHtml(text);
                            }
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
//                            TypechoComments shopUserComments = new TypechoComments();
//                            comments.setAuthorId(shopUser.getUid());
//                            Integer userlv = commentsService.total(shopUserComments,null);
//                            shopJson.put("lv", baseFull.getLv(userlv));

                        }else{
                            shopJson.put("id",0);
                            shopJson.put("username","");
                            shopJson.put("title","该商品已被删除或屏蔽");
                        }
                        json.put("shopJson",shopJson);
                    }
                    jsonList.add(json);

                }
                redisHelp.delete(this.dataprefix+"_"+"followSpace_"+uid+"_"+page+"_"+limit,redisTemplate);
                redisHelp.setList(this.dataprefix+"_"+"followSpace_"+uid+"_"+page+"_"+limit,jsonList,5,redisTemplate);
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
        return response.toString();

    }

}
