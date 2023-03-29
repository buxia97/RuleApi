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
import java.text.SimpleDateFormat;
import java.util.*;

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
    private TypechoUserlogService userlogService;

    @Autowired
    private TypechoUsersService usersService;

    @Autowired
    private TypechoMetasService metasService;

    @Autowired
    private TypechoRelationshipsService relationshipsService;

    @Autowired
    private TypechoApiconfigService apiconfigService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MailService MailService;

    @Autowired
    private PushService pushService;

    @Autowired
    private TypechoInboxService inboxService;

    @Value("${webinfo.CommentCache}")
    private Integer CommentCache;

    @Value("${web.prefix}")
    private String dataprefix;

    RedisHelp redisHelp =new RedisHelp();
    ResultAll Result = new ResultAll();
    UserStatus UStatus = new UserStatus();
    baseFull baseFull = new baseFull();
    EditFile editFile = new EditFile();

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
                                @RequestParam(value = "order"        , required = false, defaultValue = "created") String order,
                            @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit,
                                @RequestParam(value = "token"       , required = false, defaultValue = "") String token) {
        TypechoComments query = new TypechoComments();
        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        if(limit>50){
            limit = 50;
        }
        Integer uid = 0;
        Integer total = 0;
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
            total = service.total(query);
        }
        List jsonList = new ArrayList();
        List cacheList = redisHelp.getList(this.dataprefix+"_"+"searchParams_"+page+"_"+limit+"_"+searchKey+"_"+searchParams+"_"+uid+"_"+order,redisTemplate);
        if(uStatus!=0){
            cacheList = redisHelp.getList(this.dataprefix+"_"+"searchParams_"+page+"_"+limit+"_"+searchKey+"_"+searchParams+"_"+order,redisTemplate);
        }

        try{
            if(cacheList.size()>0){
                jsonList = cacheList;
            }else{
                TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);
                PageList<TypechoComments> pageList = service.selectPage(query, page, limit,searchKey,order);
                List<TypechoComments> list = pageList.getList();
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

                    TypechoComments comments = list.get(i);
                    Integer cid = comments.getCid();


                    //如果存在上级评论
                    Map<String, String> parentComments = new HashMap<String, String>();
                    if(Integer.parseInt(json.get("parent").toString())>0){
                        String coid = json.get("parent").toString();
                        TypechoComments parent = service.selectByKey(coid);
                        if(parent!=null){
                            if(parent.getStatus().equals("approved")){
                                parentComments.put("author",parent.getAuthor());
                                parentComments.put("text",parent.getText());
                                parentComments.put("created",JSONObject.toJSONString(parent.getCreated()));

                            }else{
                                parentComments.put("text","该评论已被删除");
                            }
                        }else{
                            parentComments.put("text","该评论已被删除");
                        }

                    }


                    //获取用户等级和自定义头衔
                    Integer userid = comments.getAuthorId();
                    String avatar = apiconfig.getWebinfoAvatar() + "null";
                    if(userid<1){
                        String mail = json.get("mail").toString();

                        if(mail.indexOf("@qq.com") != -1){
                            String qq = mail.replace("@qq.com","");
                            avatar = "https://q1.qlogo.cn/g?b=qq&nk="+qq+"&s=640";
                        }else{
                            avatar = baseFull.getAvatar(apiconfig.getWebinfoAvatar(), mail);
                        }
                        json.put("lv",0);
                        json.put("customize","");
                        json.put("avatar",avatar);
                    }else{
                        TypechoComments usercomments = new TypechoComments();
                        usercomments.setAuthorId(userid);
                        Integer lv = service.total(usercomments);
                        TypechoUsers userinfo = usersService.selectByKey(userid);
                        if(userinfo!=null){
                            String name = userinfo.getName();
                            if(userinfo.getScreenName()!=null&&userinfo.getScreenName()!=""){
                                name = userinfo.getScreenName();
                            }

                            if(userinfo.getAvatar()!=null&&userinfo.getAvatar()!=""){
                                avatar = userinfo.getAvatar();
                            }else{
                                if(userinfo.getMail()!=null&&userinfo.getMail()!=""){
                                    String mail = userinfo.getMail();

                                    if(mail.indexOf("@qq.com") != -1){
                                        String qq = mail.replace("@qq.com","");
                                        avatar = "https://q1.qlogo.cn/g?b=qq&nk="+qq+"&s=640";
                                    }else{
                                        avatar = baseFull.getAvatar(apiconfig.getWebinfoAvatar(), userinfo.getMail());
                                    }
                                    //avatar = baseFull.getAvatar(apiconfig.getWebinfoAvatar(), author.getMail());
                                }
                            }
                            json.put("avatar",avatar);
                            json.put("author",name);
                            json.put("mail",userinfo.getMail());
                            json.put("lv",baseFull.getLv(lv));
                            json.put("customize",userinfo.getCustomize());
                            json.put("experience",userinfo.getExperience());
                            //判断是否为VIP
                            json.put("isvip", 0);
                            json.put("vip", userinfo.getVip());

                            Long date = System.currentTimeMillis();
                            String curTime = String.valueOf(date).substring(0, 10);
                            Integer viptime  = userinfo.getVip();
                            if(viptime>Integer.parseInt(curTime)){
                                json.put("isvip", 1);
                            }
                            if(viptime.equals(1)){
                                //永久VIP
                                json.put("isvip", 2);
                            }
                        }
                    }
                    json.put("parentComments",parentComments);

                    TypechoContents contentsInfo = contentsService.selectByKey(cid);
                    if(contentsInfo!=null){
                        json.put("contenTitle",contentsInfo.getTitle());
                        //加入文章数据
                        Map contentsJson = new HashMap();
                        contentsJson.put("cid",contentsInfo.getCid());
                        contentsJson.put("slug",contentsInfo.getSlug());
                        contentsJson.put("title",contentsInfo.getTitle());
                        contentsJson.put("type",contentsInfo.getType());
                        List<TypechoRelationships> relationships = relationshipsService.selectByKey(cid);
                        if(relationships.size()>0){
                            TypechoRelationships rinfo = relationships.get(0);
                            Integer mid = rinfo.getMid();
                            TypechoMetas cmetas = metasService.selectByKey(mid);
                            if(cmetas!=null){
                                List category = new ArrayList();
                                Map metasInfo = JSONObject.parseObject(JSONObject.toJSONString(cmetas), Map.class);
                                category.add(metasInfo);
                                contentsJson.put("category",category);
                            }
                        }


                        json.put("contentsInfo",contentsJson);
                    }else{
                        json.put("contenTitle","文章已删除");
                    }


                    jsonList.add(json);


                }
                if(uStatus!=0){
                    redisHelp.delete(this.dataprefix+"_"+"contensList_"+page+"_"+limit+"_"+searchKey+"_"+searchParams+"_"+uid+"_"+order,redisTemplate);
                    redisHelp.setList(this.dataprefix+"_"+"contensList_"+page+"_"+limit+"_"+searchKey+"_"+searchParams+"_"+uid+"_"+order,jsonList,this.CommentCache,redisTemplate);
                }else{
                    redisHelp.delete(this.dataprefix+"_"+"contensList_"+page+"_"+limit+"_"+searchKey+"_"+searchParams+"_"+order,redisTemplate);
                    redisHelp.setList(this.dataprefix+"_"+"contensList_"+page+"_"+limit+"_"+searchKey+"_"+searchParams+"_"+order,jsonList,this.CommentCache,redisTemplate);
                }
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
        response.put("total", total);
        return response.toString();
    }


    /***
     * 添加评论
     * @param params Bean对象JSON字符串
     */
    @RequestMapping(value = "/commentsAdd")
    @ResponseBody
    public String commentsAdd(@RequestParam(value = "params", required = false) String  params,
                              @RequestParam(value = "token", required = false) String  token,
                              @RequestParam(value = "text", required = false) String  text,
                              HttpServletRequest request) {
        try {
            Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
            Map jsonToMap =null;

            if(uStatus==0){
                return Result.getResultJson(0,"用户未登录或Token验证失败",null);
            }
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer logUid =Integer.parseInt(map.get("uid").toString());
            //登录情况下，刷数据攻击拦截
            String isSilence = redisHelp.getRedis(this.dataprefix+"_"+logUid+"_silence",redisTemplate);
            if(isSilence!=null){
                return Result.getResultJson(0,"你的操作太频繁了，请稍后再试",null);
            }
            String isRepeated = redisHelp.getRedis(this.dataprefix+"_"+logUid+"_isRepeated",redisTemplate);
            if(isRepeated==null){
                redisHelp.setRedis(this.dataprefix+"_"+logUid+"_isRepeated","1",2,redisTemplate);
            }else{
                Integer frequency = Integer.parseInt(isRepeated) + 1;
                if(frequency==3){
                    securityService.safetyMessage("用户ID："+logUid+"，在评论发布接口疑似存在攻击行为，请及时确认处理。","system");
                    redisHelp.setRedis(this.dataprefix+"_"+logUid+"_silence","1",900,redisTemplate);
                    return Result.getResultJson(0,"你的请求存在恶意行为，15分钟内禁止操作！",null);
                }else{
                    redisHelp.setRedis(this.dataprefix+"_"+logUid+"_isRepeated",frequency.toString(),3,redisTemplate);
                }
                return Result.getResultJson(0,"你的操作太频繁了",null);
            }
            //攻击拦截结束
            Integer cuid =Integer.parseInt(map.get("uid").toString());
            Long date = System.currentTimeMillis();
            String created = String.valueOf(date).substring(0,10);
            String cstatus = "approved";
            TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);
            String title = apiconfig.getWebinfoTitle();

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
                Integer isEmail = apiconfig.getIsEmail();
                Integer isPush = apiconfig.getIsPush();

                //获取发布者信息

                //获取评论发布者信息和填写其它不可定义的值

                //支持两种模式提交评论内容
                if(text==null){
                    text = jsonToMap.get("text").toString();
                }
                jsonToMap.put("authorId",map.get("uid").toString());

                TypechoUsers user = usersService.selectByKey(map.get("uid").toString());
                String postName = "";
                if(user.getScreenName()==null){
                    jsonToMap.put("author",user.getName());
                    postName = user.getName();
                }else{
                    jsonToMap.put("author",user.getScreenName());
                    postName = user.getScreenName();
                }
                if(text.length()<4){
                    return Result.getResultJson(0,"评论长度过短",null);
                }else{
                    if(text.length()>1500){
                        return Result.getResultJson(0,"超出最大评论长度",null);
                    }
                }
                if(map.get("url")!=null){
                    jsonToMap.put("url",user.getUrl());
                }
                if(isEmail > 0){
                    if(map.get("mail")!=null){
                        jsonToMap.put("mail",map.get("mail").toString());
                    }else{
                        return Result.getResultJson(0,"请先绑定邮箱！",null);
                    }
                }
                //是否开启代码拦截
                if(apiconfig.getDisableCode().equals(1)){

                    if(baseFull.haveCode(text).equals(1)){
                        return Result.getResultJson(0,"你的内容包含敏感代码，请修改后重试！",null);
                    }
                }
                //根据cid获取文章作者信息
                String cid = jsonToMap.get("cid").toString();
                TypechoContents contents = contentsService.selectByKey(cid);
                if(contents==null){
                    //文章不存在，代表评论已经失效，直接删除
                    return Result.getResultJson(0,"文章已被删除！",null);
                }
                jsonToMap.put("text",text);
                jsonToMap.put("ownerId", contents.getAuthorId());
                jsonToMap.put("created",created);
                jsonToMap.put("type","comment");
                jsonToMap.put("agent",agent);
                jsonToMap.put("ip",ip);
                //下面这个属性控制评论状态，判断是否已经有评论过审，有则直接通过审核，没有则默认审核状态
                Integer auditlevel = apiconfig.getAuditlevel();

                if(auditlevel.equals(0)){
                    //为0不审核
                    cstatus = "approved";
                } else if(auditlevel.equals(1)){
                    //为1第一次评论审核
                    TypechoComments ucomment = new TypechoComments();
                    ucomment.setAuthorId(Integer.parseInt(map.get("uid").toString()));
                    ucomment.setStatus("approved");
                    List<TypechoComments> ucommentList = service.selectList(ucomment);
                    if(ucommentList.size()>0) {
                        cstatus = "approved";
                    }else{
                        cstatus = "waiting";
                    }
                } else if(auditlevel.equals(2)){
                    //为2违禁词匹配审核
                    String forbidden = apiconfig.getForbidden();
                    if(forbidden!=null){
                        if(forbidden.indexOf(",") != -1){
                            String[] strarray=forbidden.split(",");
                            for (int i = 0; i < strarray.length; i++){
                                String str = strarray[i];
                                if(text.indexOf(str) != -1){
                                    cstatus = "waiting";
                                }

                            }
                        }else{
                            if(text.indexOf(forbidden) != -1){
                                cstatus = "waiting";
                            }
                        }
                    }else{
                        cstatus = "approved";
                    }

                } else if(auditlevel.equals(3)){
                    //为2违禁词匹配拦截
                    String forbidden = apiconfig.getForbidden();
                    if(forbidden!=null){
                        if(forbidden.indexOf(",") != -1){
                            String[] strarray=forbidden.split(",");
                            for (int i = 0; i < strarray.length; i++){
                                String str = strarray[i];
                                if(text.indexOf(str) != -1){
                                    return Result.getResultJson(0,"存在违规内容，评论发布失败",null);
                                }
                            }
                        }else{
                            if(text.indexOf(forbidden) != -1){
                                return Result.getResultJson(0,"存在违规内容，评论发布失败",null);
                            }
                        }
                    }else{
                        cstatus = "approved";
                    }
                }else{
                    cstatus = "waiting";
                }

                if(cstatus.equals("approved")){
                    //如果评论是发布状态，就给文章作者发送消息
                    //给回复者发送信息
                    Integer parent = 0;
                    if(jsonToMap.get("parent")!=null){
                        parent = Integer.parseInt(jsonToMap.get("parent").toString());
                    }
                    if(parent > 0){
                        TypechoComments pComments = service.selectByKey(parent);
                        if(apiconfig.getIsEmail().equals(2)) {
                            if (pComments.getMail() != null) {
                                String pemail = pComments.getMail();
                                try {
                                    MailService.send("您的评论有了新的回复！", "<!DOCTYPE html><html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" /><title></title>" +
                                                    "<meta charset=\"utf-8\" /><style>*{padding:0px;margin:0px;box-sizing:border-box;}html{box-sizing:border-box;}body{font-size:15px;background:#fff}.main{margin:20px auto;max-width:500px;border:solid 1px #2299dd;overflow:hidden;}.main h1{display:block;width:100%;background:#2299dd;font-size:18px;color:#fff;text-align:center;padding:15px;}.text{padding:30px;}.text p{margin:10px 0px;line-height:25px;}.text p span{color:#2299dd;font-weight:bold;font-size:22px;margin-left:5px;}</style></head>" +
                                                    "<body><div class=\"main\"><h1>文章评论</h1>" +
                                                    "<div class=\"text\"><p>您的评论有了新的回复：</p><p>”" + postName + "：" + jsonToMap.get("text") + "“</p>" +
                                                    "<p>可前往<a href=\"" + apiconfig.getWebinfoUrl() + "\">" + title + "</a>查看详情</p>" +
                                                    "</div></div></body></html>",
                                            new String[]{pemail}, new String[]{});
                                } catch (Exception e) {
                                    System.err.println("邮箱发信配置错误");
                                    e.printStackTrace();
                                }

                            }
                        }
                        //发送消息通知
                        TypechoInbox inbox = new TypechoInbox();
                        inbox.setUid(logUid);
                        inbox.setTouid(pComments.getAuthorId());
                        inbox.setType("comment");
                        inbox.setText(jsonToMap.get("text").toString());
                        inbox.setValue(Integer.parseInt(cid));
                        inbox.setCreated(Integer.parseInt(created));
                        inboxService.insert(inbox);
                        if(isPush.equals(1)) {
                            TypechoUsers parentUser = usersService.selectByKey(pComments.getAuthorId());
                            if(parentUser.getClientId()!=null){
                                try {
                                    pushService.sendPushMsg(parentUser.getClientId(),title,"你有新的回复消息！","payload","comment:"+Integer.parseInt(cid));
                                }catch (Exception e){
                                    System.err.println("通知发送失败");
                                    e.printStackTrace();
                                }

                            }
                        }

                    }else{
                        TypechoUsers author = usersService.selectByKey(contents.getAuthorId());
                        Integer uid = author.getUid();
                        if(!cuid.equals(contents.getAuthorId())){
                            if(apiconfig.getIsEmail().equals(2)) {
                                if (author.getMail() != null) {
                                    String email = author.getMail();
                                    try {
                                        MailService.send("您的文章有新的评论", "<!DOCTYPE html><html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" /><title></title>" +
                                                        "<meta charset=\"utf-8\" /><style>*{padding:0px;margin:0px;box-sizing:border-box;}html{box-sizing:border-box;}body{font-size:15px;background:#fff}.main{margin:20px auto;max-width:500px;border:solid 1px #2299dd;overflow:hidden;}.main h1{display:block;width:100%;background:#2299dd;font-size:18px;color:#fff;text-align:center;padding:15px;}.text{padding:30px;}.text p{margin:10px 0px;line-height:25px;}.text p span{color:#2299dd;font-weight:bold;font-size:22px;margin-left:5px;}</style></head>" +
                                                        "<body><div class=\"main\"><h1>文章评论</h1>" +
                                                        "<div class=\"text\"><p>用户 " + uid + "，你的文章有新的评论：</p><p>”" + postName + "：" + jsonToMap.get("text") + "“</p>" +
                                                        "<p>可前往<a href=\"" + apiconfig.getWebinfoUrl() + "\">" + title + "</a>查看详情</p>" +
                                                        "</div></div></body></html>",
                                                new String[]{email}, new String[]{});
                                    } catch (Exception e) {
                                        System.err.println("邮箱发信配置错误");
                                        e.printStackTrace();
                                    }

                                }
                            }
                            //发送消息通知
                            TypechoInbox inbox = new TypechoInbox();
                            inbox.setUid(logUid);
                            inbox.setTouid(uid);
                            inbox.setType("comment");
                            inbox.setValue(Integer.parseInt(cid));
                            inbox.setText(text);
                            inbox.setCreated(Integer.parseInt(created));
                            inboxService.insert(inbox);
                            if(isPush.equals(1)) {
                                if(author.getClientId()!=null){
                                    try {
                                        pushService.sendPushMsg(author.getClientId(),title,"你的文章有新评论！","payload","comment:"+Integer.parseInt(cid));
                                    }catch (Exception e){
                                        System.err.println("通知发送失败");
                                        e.printStackTrace();
                                    }

                                }
                            }
                        }

                    }

                }

                insert = JSON.parseObject(JSON.toJSONString(jsonToMap), TypechoComments.class);
                //更新文章评论数量
                TypechoContents updateContents = new TypechoContents();
                TypechoComments suminfo = new TypechoComments();
                suminfo.setCid(Integer.parseInt(cid));
                Integer cnum = service.total(suminfo);
                updateContents.setCid(Integer.parseInt(cid));
                updateContents.setCommentsNum(cnum);
                contentsService.update(updateContents);
            }
            insert.setStatus(cstatus);
            int rows = service.insert(insert);
            String addtext ="";
            if(cstatus.equals("waiting")){
                addtext = "，将在审核通过后显示！";
            }else{
                //如果无需审核，则立即增加经验
                Integer reviewExp = apiconfig.getReviewExp();

                if(reviewExp > 0){
                    //生成操作记录
                    String cur = created + "000";
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                    String curtime = sdf.format(new Date(Long.parseLong(cur)));

                    TypechoUserlog userlog = new TypechoUserlog();
                    userlog.setUid(logUid);
                    //cid用于存放真实时间
                    userlog.setCid(Integer.parseInt(curtime));
                    userlog.setType("reviewExp");
                    Integer size = userlogService.total(userlog);
                    //只有前三次评论获得姜堰
                    if(size < 3){
                        userlog.setNum(reviewExp);
                        userlog.setCreated(Integer.parseInt(created));
                        userlogService.insert(userlog);
                        //修改用户资产
                        TypechoUsers oldUser = usersService.selectByKey(logUid);
                        Integer experience = oldUser.getExperience();
                        experience = experience + reviewExp;
                        TypechoUsers updateUser = new TypechoUsers();
                        updateUser.setUid(logUid);
                        updateUser.setExperience(experience);
                        usersService.update(updateUser);
                        addtext = "，获得"+reviewExp+"经验值";
                    }
                }

            }
            editFile.setLog("用户"+logUid+"提交发布评论，IP："+ip);
            JSONObject response = new JSONObject();
            response.put("code" ,rows > 0 ? 1: 0 );
            response.put("data" , rows);
            response.put("msg"  , rows > 0 ? "发布成功"+addtext : "发布失败");
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }

    }
    /***
     * 编辑
     */
    @RequestMapping(value = "/commentsEdit")
    @ResponseBody
    public String commentsEdit(@RequestParam(value = "params", required = false) String  params,
                               @RequestParam(value = "token", required = false) String  token,
                               @RequestParam(value = "text", required = false) String  text) {
        try {
            Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
            if(uStatus==0){
                return Result.getResultJson(0,"用户未登录或Token验证失败",null);
            }

            //只有管理员允许修改
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            String group = map.get("group").toString();
            if(!group.equals("administrator")&&!group.equals("editor")){
                return Result.getResultJson(0,"你没有操作权限",null);
            }
            Integer logUid =Integer.parseInt(map.get("uid").toString());
            Map jsonToMap =new HashMap();
            //String group = (String) redisHelp.getValue("userInfo"+token,"group",redisTemplate);
            if (StringUtils.isNotBlank(params)) {
                TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);
                jsonToMap =  JSONObject.parseObject(JSON.parseObject(params).toString());
                if(jsonToMap.get("coid")==null){
                    return Result.getResultJson(0,"请传入评论id",null);
                }
                //支持两种模式提交评论内容
                if(text==null){
                    text = jsonToMap.get("text").toString();
                }
                if(text.length()<1){
                    return Result.getResultJson(0,"评论不能为空",null);
                }else{
                    if(text.length()>1500){
                        return Result.getResultJson(0,"超出最大评论长度",null);
                    }
                }
                //是否开启代码拦截
                if(apiconfig.getDisableCode().equals(1)){
                    if(baseFull.haveCode(text).equals(1)){
                        return Result.getResultJson(0,"你的内容包含敏感代码，请修改后重试！",null);
                    }
                }
                jsonToMap.put("text",text);
                jsonToMap.remove("parent");
                jsonToMap.remove("ownerId");
                jsonToMap.remove("created");
                jsonToMap.remove("type");
                jsonToMap.remove("cid");
                jsonToMap.remove("agent");
                jsonToMap.remove("ip");
            }
            TypechoComments comments = JSON.parseObject(JSON.toJSONString(jsonToMap), TypechoComments.class);
            Integer rows = service.update(comments);
            editFile.setLog("用户"+logUid+"修改了评论"+jsonToMap.get("coid"));
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
     * 评论审核
     */
    @RequestMapping(value = "/commentsAudit")
    @ResponseBody
    public String Audit(@RequestParam(value = "key", required = false) String  key,
                        @RequestParam(value = "token", required = false) String  token,
                        @RequestParam(value = "type", required = false) Integer  type) {
        try {
            if(type==null){
                type = 0;
            }
            Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
            if(uStatus==0){
                return Result.getResultJson(0,"用户未登录或Token验证失败",null);
            }
            TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);
            String title = apiconfig.getWebinfoTitle();
            //String group = (String) redisHelp.getValue("userInfo"+token,"group",redisTemplate);
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            String group = map.get("group").toString();
            Integer logUid =Integer.parseInt(map.get("uid").toString());
            if(!group.equals("administrator")&&!group.equals("editor")){
                return Result.getResultJson(0,"你没有操作权限",null);
            }
            TypechoComments comments = service.selectByKey(key);
            if(comments.getStatus().equals("approved")){
                return Result.getResultJson(0,"该评论已被通过",null);
            }
            Integer rows = 0;
            if(type.equals(0)){

                comments.setStatus("approved");
                rows = service.update(comments);
                Integer isPush = apiconfig.getIsPush();

                //给评论者发送邮件
                Integer uid = comments.getAuthorId();
                if(comments.getMail()!=null){
                    String email = comments.getMail();
                    try{
                        MailService.send("用户："+uid+",您的评论已审核通过", "<!DOCTYPE html><html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" /><title></title><meta charset=\"utf-8\" /><style>*{padding:0px;margin:0px;box-sizing:border-box;}html{box-sizing:border-box;}body{font-size:15px;background:#fff}.main{margin:20px auto;max-width:500px;border:solid 1px #2299dd;overflow:hidden;}.main h1{display:block;width:100%;background:#2299dd;font-size:18px;color:#fff;text-align:center;padding:15px;}.text{padding:30px;}.text p{margin:10px 0px;line-height:25px;}.text p span{color:#2299dd;font-weight:bold;font-size:22px;margin-left:5px;}</style></head><body><div class=\"main\"><h1>商品订单</h1><div class=\"text\"><p>用户 "+uid+"，你的评论已经审核通过！</p><p>可前往<a href=\""+apiconfig.getWebinfoUrl()+"\">"+apiconfig.getWebinfoTitle()+"</a>查看详情</p></div></div></body></html>",
                                new String[] {email}, new String[] {});
                    }catch (Exception e){
                        System.err.println("邮箱发信配置错误："+e);
                    }
                }
                //给相关人员发送评论
                TypechoUsers author = usersService.selectByKey(comments.getOwnerId());
                String postName = comments.getAuthor();
                String text = comments.getText();
                Integer authorUid = author.getUid();
                Integer parent = comments.getParent();

                //给回复者发送信息
                if(parent>0){
                    TypechoComments pComments = service.selectByKey(parent);
                    if(apiconfig.getIsEmail().equals(2)) {
                        if (pComments.getMail() != null) {
                            String pemail = pComments.getMail();
                            try {
                                MailService.send("您的评论有了新的回复！", "<!DOCTYPE html><html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" /><title></title>" +
                                                "<meta charset=\"utf-8\" /><style>*{padding:0px;margin:0px;box-sizing:border-box;}html{box-sizing:border-box;}body{font-size:15px;background:#fff}.main{margin:20px auto;max-width:500px;border:solid 1px #2299dd;overflow:hidden;}.main h1{display:block;width:100%;background:#2299dd;font-size:18px;color:#fff;text-align:center;padding:15px;}.text{padding:30px;}.text p{margin:10px 0px;line-height:25px;}.text p span{color:#2299dd;font-weight:bold;font-size:22px;margin-left:5px;}</style></head>" +
                                                "<body><div class=\"main\"><h1>文章评论</h1>" +
                                                "<div class=\"text\"><p>您的评论有了新的回复：</p><p>”" + postName + "：" + text + "“</p>" +
                                                "<p>可前往<a href=\"" + apiconfig.getWebinfoUrl() + "\">" + title + "</a>查看详情</p>" +
                                                "</div></div></body></html>",
                                        new String[]{pemail}, new String[]{});
                            } catch (Exception e) {
                                System.err.println("邮箱发信配置错误");
                                e.printStackTrace();
                            }

                        }
                    }
                    //发送消息通知
                    Long date = System.currentTimeMillis();
                    String created = String.valueOf(date).substring(0,10);
                    TypechoInbox inbox = new TypechoInbox();
                    inbox.setUid(comments.getAuthorId());
                    inbox.setTouid(pComments.getAuthorId());
                    inbox.setType("comment");
                    inbox.setText(text);
                    inbox.setValue(pComments.getCid());
                    inbox.setCreated(Integer.parseInt(created));
                    inboxService.insert(inbox);
                    if(isPush.equals(1)) {
                        TypechoUsers user = usersService.selectByKey(pComments.getAuthorId());
                        if(user.getClientId()!=null){
                            try {
                                pushService.sendPushMsg(user.getClientId(),title,"你有新的评论回复！","payload","comment:"+pComments.getCid());
                            }catch (Exception e){
                                System.err.println("通知发送失败");
                                e.printStackTrace();
                            }

                        }
                    }


                }else{
                    //不是作者本人才通知
                    if(!comments.getAuthorId().equals(author.getUid())){
                        if(apiconfig.getIsEmail().equals(2)) {
                            if (author.getMail() != null) {
                                String aemail = author.getMail();
                                try {
                                    MailService.send("您的文章有新的评论", "<!DOCTYPE html><html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" /><title></title>" +
                                                    "<meta charset=\"utf-8\" /><style>*{padding:0px;margin:0px;box-sizing:border-box;}html{box-sizing:border-box;}body{font-size:15px;background:#fff}.main{margin:20px auto;max-width:500px;border:solid 1px #2299dd;overflow:hidden;}.main h1{display:block;width:100%;background:#2299dd;font-size:18px;color:#fff;text-align:center;padding:15px;}.text{padding:30px;}.text p{margin:10px 0px;line-height:25px;}.text p span{color:#2299dd;font-weight:bold;font-size:22px;margin-left:5px;}</style></head>" +
                                                    "<body><div class=\"main\"><h1>文章评论</h1>" +
                                                    "<div class=\"text\"><p>用户 " + authorUid + "，你的文章有新的评论：</p><p>”" + postName + "：" + text + "“</p>" +
                                                    "<p>可前往<a href=\"" + apiconfig.getWebinfoUrl() + "\">" + title + "</a>查看详情</p>" +
                                                    "</div></div></body></html>",
                                            new String[]{aemail}, new String[]{});
                                } catch (Exception e) {
                                    System.err.println("邮箱发信配置错误");
                                    e.printStackTrace();
                                }

                            }
                        }
                        //发送消息通知
                        Long date = System.currentTimeMillis();
                        String created = String.valueOf(date).substring(0,10);
                        TypechoInbox inbox = new TypechoInbox();
                        inbox.setUid(comments.getAuthorId());
                        inbox.setTouid(authorUid);
                        inbox.setType("comment");
                        inbox.setValue(comments.getCid());
                        inbox.setText(text);
                        inbox.setCreated(Integer.parseInt(created));
                        inboxService.insert(inbox);
                        if(isPush.equals(1)) {
                            TypechoUsers user = usersService.selectByKey(uid);
                            if(user.getClientId()!=null){
                                try {
                                    pushService.sendPushMsg(user.getClientId(),title,"你的文章有新评论！","payload","comment:"+comments.getCid());
                                }catch (Exception e){
                                    System.err.println("通知发送失败");
                                    e.printStackTrace();
                                }

                            }
                        }
                    }

                }
                //如果无需审核，则立即增加经验
                Integer reviewExp = apiconfig.getReviewExp();
                if(reviewExp > 0){
                    //生成操作记录
                    Long date = System.currentTimeMillis();
                    String created = String.valueOf(date).substring(0,10);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                    String curtime = sdf.format(new Date(date));

                    TypechoUserlog userlog = new TypechoUserlog();
                    userlog.setUid(logUid);
                    //cid用于存放真实时间
                    userlog.setCid(Integer.parseInt(curtime));
                    userlog.setType("reviewExp");
                    Integer size = userlogService.total(userlog);
                    //只有前三次评论获得姜堰
                    if(size < 3){
                        userlog.setNum(reviewExp);
                        userlog.setCreated(Integer.parseInt(created));
                        userlogService.insert(userlog);
                        //修改用户经验
                        TypechoUsers oldUser = usersService.selectByKey(logUid);
                        Integer experience = oldUser.getExperience();
                        experience = experience + reviewExp;
                        TypechoUsers updateUser = new TypechoUsers();
                        updateUser.setUid(logUid);
                        updateUser.setExperience(experience);
                        usersService.update(updateUser);
                    }
                }

            }else{
                rows = service.delete(key);
                //删除后发送消息通知
                Long date = System.currentTimeMillis();
                String created = String.valueOf(date).substring(0,10);
                TypechoInbox inbox = new TypechoInbox();
                inbox.setUid(logUid);
                inbox.setTouid(comments.getAuthorId());
                inbox.setType("system");
                inbox.setText("你的评论【"+comments.getText()+"】未审核通过，已被删除！");
                inbox.setCreated(Integer.parseInt(created));
                inboxService.insert(inbox);
            }
            editFile.setLog("管理员"+logUid+"审核了评论"+key);
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
     * 评论删除
     */
    @RequestMapping(value = "/commentsDelete")
    @ResponseBody
    public String commentsDelete(@RequestParam(value = "key", required = false) String  key, @RequestParam(value = "token", required = false) String  token) {
        try {
            Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
            if(uStatus==0){
                return Result.getResultJson(0,"用户未登录或Token验证失败",null);
            }

            //String group = (String) redisHelp.getValue("userInfo"+token,"group",redisTemplate);
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer uid  = Integer.parseInt(map.get("uid").toString());
            // 查询发布者是不是自己，如果是管理员则跳过
            TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);
            TypechoComments comments = service.selectByKey(key);
            String group = map.get("group").toString();
            if(!group.equals("administrator")&&!group.equals("editor")){
                if(apiconfig.getAllowDelete().equals(0)){
                    return Result.getResultJson(0,"系统禁止删除评论",null);
                }
                Integer aid = comments.getAuthorId();
                if(!aid.equals(uid)){
                    return Result.getResultJson(0,"你无权进行此操作",null);
                }
//                jsonToMap.put("status","0");
            }else{
                Integer aid = comments.getAuthorId();
                //如果管理员不是评论发布者，则发送消息给用户（但不推送通知）
                if(!aid.equals(uid)){
                    Long date = System.currentTimeMillis();
                    String created = String.valueOf(date).substring(0,10);
                    TypechoInbox insert = new TypechoInbox();
                    insert.setUid(uid);
                    insert.setTouid(aid);
                    insert.setType("system");
                    insert.setText("你的评论【"+comments.getText()+"】已被删除");
                    insert.setCreated(Integer.parseInt(created));
                    inboxService.insert(insert);
                }
            }
            //更新文章评论数量

            Integer cid = comments.getCid();
            TypechoContents contents = new TypechoContents();
            TypechoComments sum = new TypechoComments();
            sum.setCid(cid);
            Integer total = service.total(sum);
            contents.setCid(cid);
            contents.setCommentsNum(total);
            contentsService.update(contents);

            //删除
            //更新用户经验
            Integer deleteExp = apiconfig.getDeleteExp();
            if(deleteExp > 0){
                TypechoUsers oldUser = usersService.selectByKey(comments.getAuthorId());
                if(oldUser!=null){
                    Integer experience = oldUser.getExperience();
                    experience = experience - deleteExp;
                    TypechoUsers updateUser = new TypechoUsers();
                    updateUser.setUid(comments.getAuthorId());
                    updateUser.setExperience(experience);
                    usersService.update(updateUser);
                }
            }


            int rows = service.delete(key);
            editFile.setLog("用户"+uid+"删除了评论"+key);
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

}
