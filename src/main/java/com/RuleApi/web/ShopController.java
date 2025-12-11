package com.RuleApi.web;

import com.RuleApi.annotation.LoginRequired;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.RuleApi.entity.*;
import com.RuleApi.common.*;
import com.RuleApi.service.*;
import net.dreamlu.mica.xss.core.XssCleanIgnore;
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
import java.util.*;

/**
 * 控制层
 * TypechoShopController
 * @author buxia97
 * @date 2022/01/27
 */
@Controller
@RequestMapping(value = "/typechoShop")
public class ShopController {

    @Autowired
    ShopService service;

    @Autowired
    private ShoptypeService shoptypeService;

    @Autowired
    private UsersService usersService;

    @Autowired
    private UserlogService userlogService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MailService MailService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private PaylogService paylogService;

    @Autowired
    private AllconfigService allconfigService;

    @Autowired
    private InboxService inboxService;

    @Autowired
    private SpaceService spaceService;

    @Autowired
    private PushService pushService;

    @Value("${web.prefix}")
    private String dataprefix;

    @Value("${mybatis.configuration.variables.prefix}")
    private String prefix;

    @Autowired
    private EmailtemplateService emailtemplateService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    emailResult emailText = new emailResult();

    RedisHelp redisHelp =new RedisHelp();
    ResultAll Result = new ResultAll();
    UserStatus UStatus = new UserStatus();
    EditFile editFile = new EditFile();
    baseFull baseFull = new baseFull();

    /***
     * 商品列表
     */
    @RequestMapping(value = "/shopList")
    @ResponseBody
    @LoginRequired(purview = "-1")
    public String shopList (@RequestParam(value = "searchParams", required = false) String  searchParams,
                            @RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                            @RequestParam(value = "searchKey"        , required = false, defaultValue = "") String searchKey,
                            @RequestParam(value = "order", required = false, defaultValue = "created") String  order,
                            @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit,
                            @RequestParam(value = "token", required = false) String  token) {
        TypechoShop query = new TypechoShop();
        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        Map map = new HashMap();
        Integer uid = 0;
        String group = "";
        if (uStatus != 0) {
            map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            uid =Integer.parseInt(map.get("uid").toString());
            group = map.get("group").toString();
        }
        String sqlParams = "null";
        if(limit>50){
            limit = 50;
        }
        Integer total = 0;
        List jsonList = new ArrayList();
        if (StringUtils.isNotBlank(searchParams)) {
            JSONObject object = JSON.parseObject(searchParams);
            query = object.toJavaObject(TypechoShop.class);
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
        total = service.total(query,null);
        List cacheList = redisHelp.getList(this.dataprefix+"_"+"shopList_"+page+"_"+limit+"_"+searchKey+"_"+sqlParams+"_"+order+"_"+uid,redisTemplate);


        try {
            if (cacheList.size() > 0) {
                jsonList = cacheList;
            } else {
                PageList<TypechoShop> pageList = service.selectPage(query, page, limit,searchKey,order);
                List<TypechoShop> list = pageList.getList();
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
                    TypechoShop shop = list.get(i);
                    Integer userid = shop.getUid();
                    //获取用户信息
                    Map userJson = UserStatus.getUserInfo(userid,allconfigService,usersService);
                    json.put("userJson",userJson);
                    if(!group.equals("administrator")&&!group.equals("editor")){
                        if(!shop.getUid().equals(uid)){
                            json.remove("value");
                        }
                    }

                    jsonList.add(json);
                }
                redisHelp.delete(this.dataprefix+"_"+"shopList_"+page+"_"+limit+"_"+searchKey+"_"+sqlParams+"_"+order+"_"+uid,redisTemplate);
                redisHelp.setList(this.dataprefix+"_"+"shopList_"+page+"_"+limit+"_"+searchKey+"_"+sqlParams+"_"+order+"_"+uid,jsonList,30,redisTemplate);
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

    /**
     * 查询商品详情
     */
    @RequestMapping(value = "/shopInfo")
    @ResponseBody
    @LoginRequired(purview = "-1")
    public String shopInfo(@RequestParam(value = "key", required = false) String  key,
                           @RequestParam(value = "token", required = false) String  token) {
        Map shopInfoJson = new HashMap<String, String>();
        try{
            Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
            Map cacheInfo = new HashMap();
            //验证结束
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            String group = map.get("group").toString();
            Integer uid =Integer.parseInt(map.get("uid").toString());
            if(uStatus==0){
                cacheInfo = redisHelp.getMapValue(this.dataprefix+"_"+"shopInfo"+key,redisTemplate);
            }
            if(cacheInfo.size()>0){
                shopInfoJson = cacheInfo;
            }else{
                TypechoShop info =  service.selectByKey(key);
                //获取自己被拉黑的情况
                if(info.getUid()!=null){
                    TypechoUserlog userlog = new TypechoUserlog();
                    userlog.setType("banUser");
                    userlog.setUid(info.getUid());
                    userlog.setNum(uid);
                    Integer ban = userlogService.total(userlog);
                    if(ban>0){
                        return Result.getResultJson(0,"由于作者设置，您无法查看内容！",null);
                    }
                }
                if(info.getStatus().equals(0)){
                    info = new TypechoShop();
                }
                Map shopinfo = JSONObject.parseObject(JSONObject.toJSONString(info), Map.class);
                if(uStatus==0){
                    shopinfo.remove("value");
                    redisHelp.delete(this.dataprefix+"_"+"spaceInfo_"+key,redisTemplate);
                    redisHelp.setKey(this.dataprefix+"_"+"spaceInfo_"+key,shopinfo,10,redisTemplate);
                }else{
                    //如果登陆，判断是否购买过
                    TypechoUserlog log = new TypechoUserlog();
                    log.setType("buy");
                    log.setUid(uid);
                    log.setCid(Integer.parseInt(key));
                    Integer isBuy = userlogService.total(log);
                    //判断自己是不是发布者
                    Integer aid = info.getUid();
                    if(!group.equals("administrator")&&!group.equals("editor")){
                        if(!uid.equals(aid)&&isBuy < 1){
                            shopinfo.remove("value");
                        }
                    }


                }
                shopInfoJson = shopinfo;
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        JSONObject JsonMap = JSON.parseObject(JSON.toJSONString(shopInfoJson),JSONObject.class);
        return JsonMap.toJSONString();



    }

    /***
     * 添加商品
     */
    @XssCleanIgnore
    @RequestMapping(value = "/addShop")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String addShop(@RequestParam(value = "params", required = false) String  params,
                          @RequestParam(value = "token", required = false) String  token,
                          @RequestParam(value = "text", required = false) String  text,
                          @RequestParam(value = "isSpace", required = false, defaultValue = "0") Integer isSpace,
                          @RequestParam(value = "isMd", required = false, defaultValue = "1") Integer  isMd,
                          @RequestParam(value = "verifyCode", required = false) String verifyCode,
                          HttpServletRequest request) {
        try{
            String  ip = baseFull.getIpAddr(request);
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer uid  = Integer.parseInt(map.get("uid").toString());
            //登录情况下，刷数据攻击拦截
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
            if(apiconfig.get("banRobots").toString().equals("1")) {
                String isSilence = redisHelp.getRedis(this.dataprefix+"_"+uid+"_silence",redisTemplate);
                if(isSilence!=null){
                    return Result.getResultJson(0,"你已被禁言，请耐心等待",null);
                }
                String isRepeated = redisHelp.getRedis(this.dataprefix+"_"+uid+"_isRepeated",redisTemplate);
                if(isRepeated==null){
                    redisHelp.setRedis(this.dataprefix+"_"+uid+"_isRepeated","1",3,redisTemplate);
                }else{
                    Integer frequency = Integer.parseInt(isRepeated) + 1;
                    if(frequency==3){
                        securityService.safetyMessage("用户ID："+uid+"，在商品发布接口疑似存在攻击行为，请及时确认处理。","system");
                        redisHelp.setRedis(this.dataprefix+"_"+uid+"_silence","1",Integer.parseInt(apiconfig.get("silenceTime").toString()),redisTemplate);
                        return Result.getResultJson(0,"你的请求存在恶意行为，10分钟内禁止操作！",null);
                    }else{
                        redisHelp.setRedis(this.dataprefix+"_"+uid+"_isRepeated",frequency.toString(),3,redisTemplate);
                    }
                    return Result.getResultJson(0,"你的操作太频繁了",null);
                }
            }

            //攻击拦截结束
            //实名认证拦截
            if(apiconfig.get("identifyPost").toString().equals("1")) {
                if(UStatus.isIdentify(uid,prefix,jdbcTemplate).equals(0)){
                    return Result.getResultJson(0,"请先完成身份认证",null);
                }
            }
            Map jsonToMap =null;
            TypechoShop insert = null;

            if (StringUtils.isNotBlank(params)) {

                jsonToMap =  JSONObject.parseObject(JSON.parseObject(params).toString());
                //支持两种模式提交商品内容
                if(text==null){
                    text = jsonToMap.get("text").toString();
                }
                Integer price = 0;
                if(jsonToMap.get("price")!=null){
                    price = Integer.parseInt(jsonToMap.get("price").toString());
                    if(price < 0){
                        return Result.getResultJson(0,"请输入正确的参数",null);
                    }
                }
                jsonToMap.put("status","0");

                if(text.length()<1){
                    return Result.getResultJson(0,"内容不能为空",null);
                }else{
                    if(text.length()>10000){
                        return Result.getResultJson(0,"超出最大内容长度",null);
                    }
                }
                //是否开启代码拦截
                if(apiconfig.get("disableCode").toString().equals("1")) {
                    if(baseFull.haveCode(text).equals(1)){
                        return Result.getResultJson(0,"你的内容包含敏感代码，请修改后重试！",null);
                    }
                }
                if(isMd.equals(1)){
                    text = text.replace("||rn||","\n");
                }
                jsonToMap.put("text",text);
                jsonToMap.put("isMd",isMd);

                //如果用户不设置VIP折扣，则调用系统设置
                Double vipDiscount = Double.valueOf(apiconfig.get("vipDiscount").toString());
                if(jsonToMap.get("vipDiscount")==null){
                    jsonToMap.put("vipDiscount",vipDiscount);
                }

//            if(group.equals("administrator")||group.equals("editor")){
//                jsonToMap.put("status","1");
//            }
                //根据后台的开关判断
                String title = jsonToMap.get("title").toString();
                if(apiconfig.get("cmsSwitch").toString().equals("1")|apiconfig.get("cmsSwitch").toString().equals("3")){
                    try{
                        String setTitle = baseFull.encrypt(title);
                        Map violationData = securityService.textViolation(setTitle);
                        String Suggestion = violationData.get("Suggestion").toString();
                        if(Suggestion.equals("Block")){
                            return Result.getResultJson(0,"内容涉及违规，请检查后重新提交！",null);
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                }
                Integer contentAuditlevel = Integer.parseInt(apiconfig.get("contentAuditlevel").toString());;

                if(contentAuditlevel.equals(0)){
                    jsonToMap.put("status","1");
                }
                if(contentAuditlevel.equals(1)){
                    String forbidden = "";
                    if(apiconfig.get("forbidden")!=null){
                        forbidden = apiconfig.get("forbidden").toString();
                    }
                    if(forbidden!=null){
                        if(forbidden.indexOf(",") != -1){
                            String[] strarray=forbidden.split(",");
                            for (int i = 0; i < strarray.length; i++){
                                String str = strarray[i];
                                if(text.indexOf(str) != -1){
                                    jsonToMap.put("status","0");
                                }

                            }
                        }else{
                            if(text.indexOf(forbidden) != -1){
                                jsonToMap.put("status","0");
                            }
                        }
                    }else{
                        jsonToMap.put("status","1");
                    }

                }
                if(contentAuditlevel.equals(2)){
                    //除管理员外，商品默认待审核
                    String group = map.get("group").toString();
                    if(!group.equals("administrator")&&!group.equals("editor")){
                        jsonToMap.put("status","0");
                    }else{
                        jsonToMap.put("status","1");
                    }
                }
                //腾讯云内容违规检测
                if(apiconfig.get("cmsSwitch").toString().equals("1")|apiconfig.get("cmsSwitch").toString().equals("3")){
                    try{
                        String setText = baseFull.htmlToText(text);
                        setText = baseFull.encrypt(setText);
                        Map violationData = securityService.textViolation(setText);
                        String Suggestion = violationData.get("Suggestion").toString();
                        if(Suggestion.equals("Block")){
                            return Result.getResultJson(0,"内容涉及违规，请检查后重新提交！",null);
                        }
                        if(Suggestion.equals("Review")){
                            jsonToMap.put("status","0");
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                }

                //判断是否开启邮箱验证
                Integer isEmail = Integer.parseInt(apiconfig.get("isEmail").toString());
                if(isEmail>0) {
                    //判断用户是否绑定了邮箱
                    TypechoUsers users = usersService.selectByKey(uid);
                    if (users.getMail() == null) {
                        return Result.getResultJson(0, "发布商品前，请先绑定邮箱", null);
                    }
                }
                insert = JSON.parseObject(JSON.toJSONString(jsonToMap), TypechoShop.class);
                Long date = System.currentTimeMillis();
                String created = String.valueOf(date).substring(0,10);
                insert.setCreated(Integer.parseInt(created));
                insert.setUid(uid);
            }

            int rows = service.insert(insert);
            //同步到动态
            if(isSpace.equals(1)){
                Long date = System.currentTimeMillis();
                String created = String.valueOf(date).substring(0,10);
                TypechoSpace space = new TypechoSpace();
                space.setType(5);
                space.setText("发布了新商品");
                space.setCreated(Integer.parseInt(created));
                space.setModified(Integer.parseInt(created));
                space.setUid(uid);
                space.setToid(insert.getId());
                Integer spaceAudit = Integer.parseInt(apiconfig.get("spaceAudit").toString());
                if(spaceAudit.equals(1)){
                    space.setStatus(0);
                }else{
                    space.setStatus(1);
                }
                spaceService.insert(space);
            }
            if(rows > 0){
                redisHelp.deleteKeysWithPattern("*"+this.dataprefix+"_shopList_1*",redisTemplate,this.dataprefix);
            }
            editFile.setLog("用户"+uid+"请求添加商品");
            JSONObject response = new JSONObject();
            response.put("code" , rows);
            response.put("msg"  , rows > 0 ? "添加成功" : "添加失败");
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }

    }

    /***
     * 修改商品
     */
    @XssCleanIgnore
    @RequestMapping(value = "/editShop")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String editShop(@RequestParam(value = "params", required = false) String  params,
                           @RequestParam(value = "token", required = false) String  token,
                           @RequestParam(value = "text", required = false) String  text,
                           @RequestParam(value = "isMd", required = false, defaultValue = "1") Integer  isMd) {
        TypechoShop update = null;
        Map jsonToMap =null;
        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid  = Integer.parseInt(map.get("uid").toString());
        if (StringUtils.isNotBlank(params)) {
            Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
            jsonToMap =  JSONObject.parseObject(JSON.parseObject(params).toString());
            //支持两种模式提交评论内容
            if(text==null){
                text = jsonToMap.get("text").toString();
            }
            Integer price = 0;
            if(jsonToMap.get("price")!=null){
                price = Integer.parseInt(jsonToMap.get("price").toString());
                if(price < 0){
                    return Result.getResultJson(0,"请输入正确的参数",null);
                }
            }

            // 查询发布者是不是自己，如果是管理员则跳过
            String group = map.get("group").toString();
            if(!group.equals("administrator")&&!group.equals("editor")){
                Integer sid = Integer.parseInt(jsonToMap.get("id").toString());
                TypechoShop info = service.selectByKey(sid);
                Integer aid = info.getUid();
                if(!aid.equals(uid)){
                    return Result.getResultJson(0,"你无权进行此操作",null);
                }
//                jsonToMap.put("status","0");
            }
            if(text.length()<1){
                return Result.getResultJson(0,"内容不能为空",null);
            }else{
                if(text.length()>10000){
                    return Result.getResultJson(0,"超出最大内容长度",null);
                }
            }
            String title = "未命名商品";
            if(jsonToMap.get("title")!=null){
                title = jsonToMap.get("title").toString();
            }
            if(apiconfig.get("cmsSwitch").toString().equals("1")|apiconfig.get("cmsSwitch").toString().equals("3")){
                try{
                    String setTitle = baseFull.encrypt(title);
                    Map violationData = securityService.textViolation(setTitle);
                    String Suggestion = violationData.get("Suggestion").toString();
                    if(Suggestion.equals("Block")){
                        return Result.getResultJson(0,"内容涉及违规，请检查后重新提交！",null);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }

            }
            //根据后台的开关判断
            Integer contentAuditlevel = Integer.parseInt(apiconfig.get("contentAuditlevel").toString());
            if(contentAuditlevel.equals(0)){
                jsonToMap.put("status","1");
            }
            if(contentAuditlevel.equals(1)){
                String forbidden = "";
                if(apiconfig.get("forbidden")!=null){
                    forbidden = apiconfig.get("forbidden").toString();
                }
                if(forbidden!=null){
                    if(forbidden.indexOf(",") != -1){
                        String[] strarray=forbidden.split(",");
                        for (int i = 0; i < strarray.length; i++){
                            String str = strarray[i];
                            if(text.indexOf(str) != -1){
                                jsonToMap.put("status","0");
                            }

                        }
                    }else{
                        if(text.indexOf(forbidden) != -1){
                            jsonToMap.put("status","0");
                        }
                    }
                }else{
                    jsonToMap.put("status","1");
                }

            }
            if(contentAuditlevel.equals(2)){
                //除管理员外，商品默认待审核
                if(!group.equals("administrator")&&!group.equals("editor")){
                    jsonToMap.put("status","0");
                }else{
                    jsonToMap.put("status","1");
                }
            }
            //腾讯云内容违规检测
            if(apiconfig.get("cmsSwitch").toString().equals("1")|apiconfig.get("cmsSwitch").toString().equals("3")){
                try{
                    String setText = baseFull.htmlToText(text);
                    setText = baseFull.encrypt(setText);
                    Map violationData = securityService.textViolation(setText);
                    String Suggestion = violationData.get("Suggestion").toString();
                    if(Suggestion.equals("Block")){
                        return Result.getResultJson(0,"内容涉及违规，请检查后重新提交！",null);
                    }
                    if(Suggestion.equals("Review")){
                        jsonToMap.put("status","0");
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }

            }
            //是否开启代码拦截
            if(apiconfig.get("disableCode").toString().equals("1")) {
                if(baseFull.haveCode(text).equals(1)){
                    return Result.getResultJson(0,"你的内容包含敏感代码，请修改后重试！",null);
                }
            }
            if(isMd.equals(1)){
                text = text.replace("||rn||","\n");
            }
            jsonToMap.put("text",text);
            jsonToMap.remove("created");
            jsonToMap.put("isMd",isMd);
            update = JSON.parseObject(JSON.toJSONString(jsonToMap), TypechoShop.class);
        }

        int rows = service.update(update);
        editFile.setLog("用户"+uid+"请求修改商品");
        JSONObject response = new JSONObject();
        response.put("code" , rows);
        response.put("msg"  , rows > 0 ? "修改成功" : "修改失败");
        return response.toString();
    }

    /***
     * 删除商品
     */
    @RequestMapping(value = "/deleteShop")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String deleteShop(@RequestParam(value = "key", required = false) String  key,@RequestParam(value = "token", required = false) String  token) {

        // 查询发布者是不是自己，如果是管理员则跳过
        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid  = Integer.parseInt(map.get("uid").toString());
        String group = map.get("group").toString();
        Integer sid = Integer.parseInt(key);
        TypechoShop info = service.selectByKey(sid);
        if(!group.equals("administrator")&&!group.equals("editor")){

            Integer aid = info.getUid();
            if(!aid.equals(uid)){
                return Result.getResultJson(0,"你无权进行此操作",null);
            }
        }else{
            //发送消息
            Long date = System.currentTimeMillis();
            String created = String.valueOf(date).substring(0,10);
            TypechoInbox insert = new TypechoInbox();
            insert.setUid(uid);
            insert.setTouid(info.getUid());
            insert.setType("system");
            insert.setText("你的商品【"+info.getTitle()+"】已被删除");
            insert.setCreated(Integer.parseInt(created));
            inboxService.insert(insert);
        }

        int rows =  service.delete(key);
        if(rows > 0){
            redisHelp.deleteKeysWithPattern("*"+this.dataprefix+"_shopList_1*",redisTemplate,this.dataprefix);
        }
        editFile.setLog("用户"+uid+"请求删除商品"+key);
        JSONObject response = new JSONObject();
        response.put("code" , rows);
        response.put("msg"  , rows > 0 ? "操作成功" : "操作失败");
        return response.toString();
    }
    /***
     * 审核商品
     */
    @RequestMapping(value = "/auditShop")
    @ResponseBody
    @LoginRequired(purview = "1")
    public String auditShop(@RequestParam(value = "key", required = false) String  key,
                            @RequestParam(value = "token", required = false) String  token,
                            @RequestParam(value = "type", required = false) Integer  type,
                            @RequestParam(value = "reason", required = false) String  reason) {
        if(type==null){
            type = 0;
        }
        try{
            // 查询发布者是不是自己，如果是管理员则跳过
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer uid  = Integer.parseInt(map.get("uid").toString());
            String group = map.get("group").toString();
            Integer sid = Integer.parseInt(key);
            TypechoShop info = service.selectByKey(sid);
            TypechoShop shop = new TypechoShop();
            shop.setId(Integer.parseInt(key));
            if(type.equals(0)){
                shop.setStatus(1);
            }else{
                if(reason==""||reason==null){
                    return Result.getResultJson(0,"请输入拒绝理由",null);
                }
                shop.setStatus(2);
            }
            Integer rows = service.update(shop);
            //根据过审状态发送不同的内容
            if(type.equals(0)) {
                //发送消息
                Long date = System.currentTimeMillis();
                String created = String.valueOf(date).substring(0,10);
                TypechoInbox insert = new TypechoInbox();
                insert.setUid(uid);
                insert.setTouid(info.getUid());
                insert.setType("system");
                insert.setText("你的商品【"+info.getTitle()+"】已审核通过");
                insert.setCreated(Integer.parseInt(created));
                inboxService.insert(insert);
            }else{
                //发送消息
                Long date = System.currentTimeMillis();
                String created = String.valueOf(date).substring(0,10);
                TypechoInbox insert = new TypechoInbox();
                insert.setUid(uid);
                insert.setTouid(info.getUid());
                insert.setType("system");
                insert.setText("你的商品【"+info.getTitle()+"】未审核通过。理由如下："+reason);
                insert.setCreated(Integer.parseInt(created));
                inboxService.insert(insert);
            }


            if(rows > 0){
                redisHelp.deleteKeysWithPattern("*"+this.dataprefix+"_shopList_1*",redisTemplate,this.dataprefix);
            }
            editFile.setLog("管理员"+uid+"请求审核商品"+key);
            JSONObject response = new JSONObject();
            response.put("code" , rows);
            response.put("msg"  , rows > 0 ? "操作成功" : "操作失败");
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }

    }
    /***
     * 购买商品
     */
    @RequestMapping(value = "/buyShop")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String buyShop(@RequestParam(value = "sid", required = false) String  sid,
                          @RequestParam(value = "isIntegral", required = false,defaultValue = "0") Integer  isIntegral,
                          @RequestParam(value = "token", required = false) String  token) {
        try {

            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer uid  = Integer.parseInt(map.get("uid").toString());
            TypechoShop shopinfo = service.selectByKey(sid);
            Integer aid = shopinfo.getUid();

            if(uid.equals(aid)){
                return Result.getResultJson(0,"你不可以买自己的商品",null);
            }
            Double vipDiscount = Double.valueOf(shopinfo.getVipDiscount());

            TypechoUsers usersinfo =usersService.selectByKey(uid.toString());
            Integer price = shopinfo.getPrice();
            //判断是否为VIP，是VIP则乘以折扣
            Long date = System.currentTimeMillis();
            String curTime = String.valueOf(date).substring(0, 10);
            Integer viptime  = usersinfo.getVip();
            if(viptime>Integer.parseInt(curTime)||viptime.equals(1)){
                double newPrice = price;
                newPrice = newPrice * vipDiscount;
                price =(int)newPrice;
            }
            Integer oldPoints = usersinfo.getPoints();
            Integer integral = shopinfo.getIntegral();
            if(isIntegral.equals(1)){

                if(integral > 0){

                    if(integral>oldPoints){
                        return Result.getResultJson(0,"用户积分不足",null);
                    }
                    price = price - integral;
                }
            }
            Integer oldAssets =usersinfo.getAssets();
            if(price>oldAssets){
                return Result.getResultJson(0,"当前资产不足，请充值",null);
            }



            Integer status = shopinfo.getStatus();
            if(!status.equals(1)){
                return Result.getResultJson(0,"该商品已下架",null);
            }
            Integer num = shopinfo.getNum();
            if(!num.equals(-1)){
                if(num<1){
                    return Result.getResultJson(0,"该商品已售完",null);
                }
            }

            if(price<0){
                return Result.getResultJson(0,"该商品价格参数异常，无法交易",null);
            }
            Integer Assets = oldAssets - price;
            if(isIntegral.equals(1)){
                //更新用户积分数量
                Integer points = oldPoints - integral;
                usersinfo.setPoints(points);
            }

            usersinfo.setAssets(Assets);
            //生成用户日志，这里的cid用于商品id
            TypechoUserlog log = new TypechoUserlog();
            log.setType("buy");
            log.setUid(uid);
            log.setCid(Integer.parseInt(sid));

            //判断商品类型，如果是实体商品需要设置收货地址
            Integer type = shopinfo.getType();
            String address = usersinfo.getAddress();
            if(type.equals(1)){
                if(address==null||address==""){
                    return Result.getResultJson(0,"购买实体商品前，需要先设置收货地址",null);
                }
            }else {
                //判断是否购买，非实体商品不能多次购买
                Integer isBuy = userlogService.total(log);
                if(isBuy > 0){
                    return Result.getResultJson(0,"你已经购买过了",null);
                }
            }



            log.setNum(Assets);
            log.setToid(aid);
            log.setCreated(Integer.parseInt(curTime));
            userlogService.insert(log);


            //生成购买者资产日志
            TypechoPaylog paylog = new TypechoPaylog();
            paylog.setStatus(1);
            paylog.setCreated(Integer.parseInt(curTime));
            paylog.setUid(uid);
            paylog.setOutTradeNo(curTime+"buyshop");
            paylog.setTotalAmount("-"+price);
            paylog.setPaytype("buyshop");
            paylog.setSubject("购买商品");
            paylogService.insert(paylog);

            //修改用户账户
            usersService.update(usersinfo);
            //修改商品剩余数量
            if(!num.equals(-1)){
                Integer shopnum = shopinfo.getNum();
                shopnum = shopnum - 1;
                shopinfo.setNum(shopnum);
            }


            //更新商品卖出数量
            Integer sellNum = shopinfo.getSellNum();
            sellNum = sellNum + 1;
            shopinfo.setSellNum(sellNum);
            service.update(shopinfo);


            //修改店家资产
            TypechoUsers minfo = usersService.selectByKey(aid);
            if(minfo!=null){
                Integer mAssets = minfo.getAssets();
                mAssets = mAssets + price;
                if(isIntegral.equals(1)){
                    //更新用户积分数量
                    Integer mPoints =minfo.getPoints();
                    mPoints = mPoints + integral;
                    minfo.setPoints(mPoints);
                }
                minfo.setAssets(mAssets);
                usersService.update(minfo);
                //生成店家资产日志

                TypechoPaylog paylogB = new TypechoPaylog();
                paylogB.setStatus(1);
                paylogB.setCreated(Integer.parseInt(curTime));
                paylogB.setUid(aid);
                paylogB.setOutTradeNo(curTime+"sellshop");
                paylogB.setTotalAmount(price.toString());
                paylogB.setPaytype("sellshop");
                if(isIntegral.equals(1)&&integral > 0){
                    paylogB.setSubject("出售商品收益，积分抵扣"+integral);
                }else{
                    paylogB.setSubject("出售商品收益");
                }

                paylogService.insert(paylogB);
            }else{
                System.out.println("商品ID"+sid+"店家不存在");
            }


            Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
            //给店家发送邮件
            Integer isEmail = Integer.parseInt(apiconfig.get("isEmail").toString());
            if(isEmail.equals(2)){
                String email = minfo.getMail();
                String name = minfo.getName();
                String title = shopinfo.getTitle();
                if(email!=null){
                    try{
                        TypechoEmailtemplate emailtemplate = emailtemplateService.selectByKey(1);
                        if(emailtemplate!=null) {

                            MailService.send("用户" + name + "您有新的商品订单",
                                    emailText.getOrderEmail(emailtemplate,name,title),
                                    new String[]{email}, new String[]{},apiconfig);
                        }
                    }catch (Exception e){
                        System.err.println("邮箱发信配置错误："+e);
                    }
                }
            }

            //发送消息通知
            String created = String.valueOf(date).substring(0,10);
            TypechoInbox inbox = new TypechoInbox();
            inbox.setUid(uid);
            inbox.setTouid(shopinfo.getUid());
            inbox.setType("finance");
            if(shopinfo.getTitle()==null){
                inbox.setText("你的商品有新的订单。");
            }else{
                inbox.setText("你的商品【"+shopinfo.getTitle()+"】有新的订单。");
            }

            inbox.setValue(shopinfo.getId());
            inbox.setCreated(Integer.parseInt(created));
            inboxService.insert(inbox);
            Integer isPush = Integer.parseInt(apiconfig.get("isPush").toString());
            if(isPush.equals(1)){
                String webTitle = apiconfig.get("webinfoTitle").toString();
                if(minfo.getClientId()!=null){
                    try {
                        pushService.sendPushMsg(minfo.getClientId(),webTitle,"你有新的商品订单！","payload","finance",apiconfig);
                    }catch (Exception e){
                        System.err.println("通知发送失败："+e);
                    }
                }
            }
            //如果邀请返利打开，则对邀请者返利
            Integer rebateLevel =Integer.parseInt(apiconfig.get("rebateLevel").toString());;

            if(rebateLevel > 1){
                if(usersinfo.getInvitationUser()!=null&&!usersinfo.getInvitationUser().equals(0)){
                    Integer inviteUserID = usersinfo.getInvitationUser();
                    Integer rebateProportion = Integer.parseInt(apiconfig.get("rebateProportion").toString());;
                    TypechoUsers users = usersService.selectByKey(inviteUserID);
                    if(users!=null){
                        Integer inviteUserAssets = users.getAssets();
                        //商品购买后分成奖励
                        double rebate = price * rebateProportion / 100;
                        //如果转为整数大于0，则有返利
                        if((int)rebate > 0){
                            inviteUserAssets = inviteUserAssets + (int)rebate;
                            TypechoUsers updateUser = new TypechoUsers();
                            updateUser.setUid(inviteUserID);
                            updateUser.setAssets(inviteUserAssets);
                            usersService.update(updateUser);

                            //生成财务日志
                            TypechoPaylog payInvitelog = new TypechoPaylog();
                            payInvitelog.setStatus(1);
                            payInvitelog.setCreated(Integer.parseInt(created));
                            payInvitelog.setUid(inviteUserID);
                            payInvitelog.setOutTradeNo(created+"rebate");
                            payInvitelog.setTotalAmount(""+(int)rebate);
                            payInvitelog.setPaytype("rebate");
                            payInvitelog.setSubject("被邀请者消费后返利");
                            paylogService.insert(payInvitelog);
                        }

                    }
                }
            }

            JSONObject response = new JSONObject();
            response.put("code" , 1);
            response.put("msg"  , "操作成功");
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            JSONObject response = new JSONObject();
            response.put("code" , 0);
            response.put("msg"  , "操作失败");
            return response.toString();
        }


    }
    /***
     * 购买VIP
     */
    @RequestMapping(value = "/buyVIP")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String buyVIP(@RequestParam(value = "day", required = false) Integer  day,@RequestParam(value = "token", required = false) String  token) {
        try {
            if(day < 1){
                return Result.getResultJson(0,"参数错误！",null);
            }
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer uid  = Integer.parseInt(map.get("uid").toString());
            Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);

            Long date = System.currentTimeMillis();
            String curTime = String.valueOf(date).substring(0, 10);
            Integer days = 86400;
            TypechoUsers users = usersService.selectByKey(uid);
            Integer assets = users.getAssets();
            //判断用户是否为VIP，决定是续期还是从当前时间开始计算
            Integer vip = users.getVip();
            //默认是从当前时间开始相加
            Integer vipTime = Integer.parseInt(curTime) + days*day;
            if(vip.equals(1)){
                return Result.getResultJson(0,"您已经是永久VIP，无需购买",null);
            }
            //如果已经是vip，走续期逻辑。
            if(vip>Integer.parseInt(curTime)){
                vipTime = vip+ days*day;
            }
            Integer vipPrice = Integer.parseInt(apiconfig.get("vipPrice").toString());
            Integer AllPrice = day * vipPrice;
            if(AllPrice>assets){
                return Result.getResultJson(0,"当前资产不足，请充值",null);
            }

            Integer vipDay = Integer.parseInt(apiconfig.get("vipDay").toString());
            if(day >= vipDay){
                //如果时间戳为1就是永久会员
                vipTime = 1;
            }
            if(AllPrice < 0 ){
                return Result.getResultJson(0,"参数错误！",null);
            }
            Integer newassets = assets - AllPrice;
            //更新用户资产与登录状态
            users.setAssets(newassets);
            users.setVip(vipTime);

            int rows =  usersService.update(users);
            String created = String.valueOf(date).substring(0,10);
            TypechoPaylog paylog = new TypechoPaylog();
            paylog.setStatus(1);
            paylog.setCreated(Integer.parseInt(created));
            paylog.setUid(uid);
            paylog.setOutTradeNo(created+"buyvip");
            paylog.setTotalAmount("-"+AllPrice);
            paylog.setPaytype("buyvip");
            paylog.setSubject("购买VIP");
            paylogService.insert(paylog);

            JSONObject response = new JSONObject();
            response.put("code" , rows);
            response.put("msg"  , rows > 0 ? "开通VIP成功" : "操作失败");
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            JSONObject response = new JSONObject();
            response.put("code" , 0);
            response.put("msg"  , "接口请求异常，请联系管理员");
            return response.toString();
        }


    }
    /***
     * VIP信息
     */
    @RequestMapping(value = "/vipInfo")
    @ResponseBody
    @LoginRequired(purview = "-1")
    public String vipInfo() {
        JSONObject data = new JSONObject();
        Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
        data.put("vipDiscount",apiconfig.get("vipDiscount").toString());
        data.put("vipPrice",Integer.parseInt(apiconfig.get("vipPrice").toString()));
        data.put("scale",Integer.parseInt(apiconfig.get("scale").toString()));
        data.put("vipDay",Integer.parseInt(apiconfig.get("vipDay").toString()));
        JSONObject response = new JSONObject();
        response.put("code" , 1);
        response.put("data" , data);
        response.put("msg"  , "");
        return response.toString();
    }
    /**
     * 文章挂载商品
     * */
    @RequestMapping(value = "/mountShop")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String mountShop(@RequestParam(value = "cid", required = false) String  cid,@RequestParam(value = "sid", required = false) String  sid,@RequestParam(value = "token", required = false) String  token) {

        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid  = Integer.parseInt(map.get("uid").toString());
        //判断商品是不是自己的
        TypechoShop shop = new TypechoShop();
        shop.setUid(uid);
        shop.setId(Integer.parseInt(sid));
        Integer num  = service.total(shop,null);
        if(num < 1){
            return Result.getResultJson(0,"你无权限修改他人的商品",null);
        }
        shop.setCid(Integer.parseInt(cid));
        int rows =  service.update(shop);
        JSONObject response = new JSONObject();
        response.put("code" , rows);
        response.put("msg"  , rows > 0 ? "操作成功" : "操作失败");
        return response.toString();
    }
    /***
     * 查询商品是否已经购买过
     */
    @RequestMapping(value = "/isBuyShop")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String isBuyShop(@RequestParam(value = "sid", required = false) String  sid,@RequestParam(value = "token", required = false) String  token) {

        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid  = Integer.parseInt(map.get("uid").toString());

        TypechoUserlog log = new TypechoUserlog();
        log.setType("buy");
        log.setUid(uid);
        log.setCid(Integer.parseInt(sid));
        int rows =  userlogService.total(log);
        JSONObject response = new JSONObject();
        response.put("code" , rows > 0 ? 1 : 0);
        response.put("msg"  , rows > 0 ? "已购买" : "未购买");
        return response.toString();
    }
    /***
     * 添加商品分类
     */
    @RequestMapping(value = "/addShopType")
    @ResponseBody
    @LoginRequired(purview = "1")
    public String addShopType(@RequestParam(value = "params", required = false) String  params,@RequestParam(value = "token", required = false) String  token) {
        try{
            // 查询发布者是不是自己，如果是管理员则跳过
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer uid  = Integer.parseInt(map.get("uid").toString());
            TypechoShoptype insert = null;
            if (StringUtils.isNotBlank(params)) {
                JSONObject object = JSON.parseObject(params);
                insert = object.toJavaObject(TypechoShoptype.class);
            }else{
                return Result.getResultJson(0, "参数不正确", null);
            }

            int rows = shoptypeService.insert(insert);
            editFile.setLog("管理员"+uid+"请求添加商品分类");
            JSONObject response = new JSONObject();
            response.put("code" , rows);
            response.put("msg"  , rows > 0 ? "添加成功" : "添加失败");
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            JSONObject response = new JSONObject();
            response.put("code" , 0);
            response.put("msg"  , "接口请求异常，请联系管理员");
            return response.toString();
        }

    }
    /***
     * 修改商品分类
     */
    @RequestMapping(value = "/editShopType")
    @ResponseBody
    @LoginRequired(purview = "2")
    public String editShopType(@RequestParam(value = "params", required = false) String  params,@RequestParam(value = "token", required = false) String  token) {
        try{
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            String logUid = map.get("uid").toString();
            TypechoShoptype update = new TypechoShoptype();
            Map jsonToMap = null;
            if (StringUtils.isNotBlank(params)) {
                jsonToMap =  JSONObject.parseObject(JSON.parseObject(params).toString());
                if(jsonToMap.get("id")==null){
                    return Result.getResultJson(0,"请传入商品分类id",null);
                }
                TypechoShoptype shoptype = shoptypeService.selectByKey(jsonToMap.get("id").toString());
                if(shoptype==null){
                    return Result.getResultJson(0,"商品分类不存在",null);
                }
                update = JSON.parseObject(JSON.toJSONString(jsonToMap), TypechoShoptype.class);
            }else{
                return Result.getResultJson(0, "参数不正确", null);
            }

            int rows = shoptypeService.update(update);
            editFile.setLog("管理员"+logUid+"请求修改商品分类"+update.getId());
            JSONObject response = new JSONObject();
            response.put("code" , rows);
            response.put("msg"  , rows > 0 ? "操作成功" : "操作失败");
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0, "接口请求异常，请联系管理员", null);
        }

    }
    /***
     * 删除分类
     */
    @RequestMapping(value = "/deleteShopType")
    @ResponseBody
    @LoginRequired(purview = "2")
    public String deleteShopType(@RequestParam(value = "id", required = false) String  id,
                             @RequestParam(value = "token", required = false) String  token) {
        try {
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            String logUid = map.get("uid").toString();
            TypechoShoptype typeInfo = shoptypeService.selectByKey(id);
            if(typeInfo==null){
                return Result.getResultJson(0, "商品分类不存在", null);
            }
            Integer typeId = typeInfo.getId();
            //有下级分类的大类不能删除
            TypechoShoptype shoptype = new TypechoShoptype();
            shoptype.setParent(typeId);
            Integer total = shoptypeService.total(shoptype);
            if(total > 0){
                return Result.getResultJson(0, "该分类存在下级分类，无法删除", null);
            }

            int rows = shoptypeService.delete(id);
            editFile.setLog("管理员"+logUid+"请求删除商品分类"+id);
            JSONObject response = new JSONObject();
            response.put("code" , rows);
            response.put("msg"  , rows > 0 ? "操作成功" : "操作失败");
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(0, "接口请求异常，请联系管理员", null);
        }
    }
    /***
     * 查询商品分类
     */
    @RequestMapping(value = "/shopTypeList")
    @ResponseBody
    @LoginRequired(purview = "-1")
    public String shopTypeList (@RequestParam(value = "searchParams", required = false) String  searchParams,
                             @RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                             @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit,
                             @RequestParam(value = "searchKey"        , required = false, defaultValue = "") String searchKey,
                             @RequestParam(value = "order"        , required = false, defaultValue = "") String order) {
        TypechoShoptype query = new TypechoShoptype();
        String sqlParams = "null";
        if(limit>100){
            limit = 100;
        }
        Integer total = 0;
        List jsonList = new ArrayList();

        if (StringUtils.isNotBlank(searchParams)) {
            JSONObject object = JSON.parseObject(searchParams);
            query = object.toJavaObject(TypechoShoptype.class);
            Map paramsJson = JSONObject.parseObject(JSONObject.toJSONString(query), Map.class);
            sqlParams = paramsJson.toString();
        }
        List cacheList = redisHelp.getList(this.dataprefix+"_"+"shopTypeList_"+page+"_"+limit+"_"+searchKey+"_"+sqlParams,redisTemplate);

        total = shoptypeService.total(query);
        try {
            if (cacheList.size() > 0) {
                jsonList = cacheList;
            } else {
                PageList<TypechoShoptype> pageList = shoptypeService.selectPage(query, page, limit, searchKey, order);
                jsonList = pageList.getList();
                if(jsonList.size() < 1){
                    JSONObject noData = new JSONObject();
                    noData.put("code" , 1);
                    noData.put("msg"  , "");
                    noData.put("data" , new ArrayList());
                    noData.put("count", 0);
                    return noData.toString();
                }
                redisHelp.delete(this.dataprefix+"_"+"shopTypeList_"+page+"_"+limit+"_"+searchKey+"_"+sqlParams,redisTemplate);
                redisHelp.setList(this.dataprefix+"_"+"shopTypeList_"+page+"_"+limit+"_"+searchKey+"_"+sqlParams,jsonList,10,redisTemplate);
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
     * 商品分类信息
     */
    @RequestMapping(value = "/shopTypeInfo")
    @ResponseBody
    @LoginRequired(purview = "-1")
    public String shopTypeInfo(@RequestParam(value = "id", required = false) Integer  id,
                              @RequestParam(value = "token", required = false) String  token) {
        try {
            Integer uid = 0;
            Integer cacheTime = 20;
            Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
            if (!uStatus.equals(0)) {
                Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
                uid =Integer.parseInt(map.get("uid").toString());
                cacheTime = 3;
            }
            Map shopTypeInfoJson = new HashMap<String, String>();
            Map cacheInfo = redisHelp.getMapValue(this.dataprefix+"_"+"shopTypeInfoJson_"+id,redisTemplate);
            if(cacheInfo.size()>0){
                shopTypeInfoJson = cacheInfo;
            }else{
                TypechoShoptype shoptype = shoptypeService.selectByKey(id);
                if(shoptype==null){
                    return Result.getResultJson(0,"板块不存在",null);
                }

                shopTypeInfoJson = JSONObject.parseObject(JSONObject.toJSONString(shoptype), Map.class);

                redisHelp.delete(this.dataprefix+"_"+"shopTypeInfoJson_"+id+'_'+uid,redisTemplate);
                redisHelp.setKey(this.dataprefix+"_"+"shopTypeInfoJson_"+id+'_'+uid,shopTypeInfoJson,cacheTime,redisTemplate);
            }
            JSONObject response = new JSONObject();
            response.put("code", 1);
            response.put("msg", "");
            response.put("data", shopTypeInfoJson);
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
}
