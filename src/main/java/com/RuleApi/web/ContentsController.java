package com.RuleApi.web;
import com.RuleApi.annotation.LoginRequired;
import com.RuleApi.common.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.RuleApi.entity.*;
import com.RuleApi.service.*;
import net.dreamlu.mica.xss.core.XssCleanIgnore;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class ContentsController {

    @Autowired
    ContentsService service;

    @Autowired
    private ShopService shopService;

    @Autowired
    private FieldsService fieldsService;

    @Autowired
    private RelationshipsService relationshipsService;

    @Autowired
    private FilesService filesService;

    @Autowired
    private UserlogService userlogService;

    @Autowired
    private MetasService metasService;

    @Autowired
    private UsersService usersService;

    @Autowired
    private PaylogService paylogService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private CommentsService commentsService;

    @Autowired
    private AllconfigService allconfigService;

    @Autowired
    private PushService pushService;

    @Autowired
    private UploadService uploadService;

    @Autowired
    private InboxService inboxService;

    @Autowired
    private SpaceService spaceService;

    @Autowired
    private AdsService adsService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MailService MailService;

    @Value("${webinfo.contentCache}")
    private Integer contentCache;

    @Value("${webinfo.contentInfoCache}")
    private Integer contentInfoCache;

    @Value("${web.prefix}")
    private String dataprefix;

    @Value("${mybatis.configuration.variables.prefix}")
    private String prefix;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EmailtemplateService emailtemplateService;

    emailResult emailText = new emailResult();

    RedisHelp redisHelp =new RedisHelp();
    ResultAll Result = new ResultAll();
    baseFull baseFull = new baseFull();
    UserStatus UStatus = new UserStatus();
    HttpClient HttpClient = new HttpClient();
    EditFile editFile = new EditFile();

    /**
     * 查询文章详情
     *
     */
    @RequestMapping(value = "/contentsInfo")
    @ResponseBody
    @LoginRequired(purview = "-2")
    public String contentsInfo (@RequestParam(value = "key", required = false) String  key,
                                @RequestParam(value = "isMd" , required = false, defaultValue = "0") Integer isMd,
                                @RequestParam(value = "token", required = false) String  token,HttpServletRequest request) {
        TypechoContents typechoContents = null;

        //如果开启全局登录，则必须登录才能得到数据
        String group = "";
        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
        Integer uid = 0;
        //验证结束
        if(uStatus.equals(1)) {
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            group = map.get("group").toString();
            uid =Integer.parseInt(map.get("uid").toString());
        }
        Map contensjson = new HashMap<String, String>();
        Map cacheInfo = redisHelp.getMapValue(this.dataprefix+"_"+"contentsInfo_"+key+"_"+isMd,redisTemplate);

        try{
            Integer isLogin;
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
                //获取自己被拉黑的情况
                if(typechoContents.getAuthorId()!=null){
                    TypechoUserlog userlog = new TypechoUserlog();
                    userlog.setType("banUser");
                    userlog.setUid(typechoContents.getAuthorId());
                    userlog.setNum(uid);
                    Integer ban = userlogService.total(userlog);
                    if(ban>0){
                        return Result.getResultJson(0,"由于作者设置，您无法查看内容！",null);
                    }
                }
                if(typechoContents==null){
                    return Result.getResultJson(0,"该文章不存在",null);
                }
                if(!group.equals("administrator")&&!group.equals("editor")){
                    if(!typechoContents.getStatus().equals("publish")){
                        return Result.getResultJson(0,"文章暂未公开访问",null);
                    }
                }
                String text = typechoContents.getText();
                String forbidden = "";
                if(apiconfig.get("forbidden")!=null){
                    forbidden = apiconfig.get("forbidden").toString();
                }
               Integer textForbidden = baseFull.getForbidden(forbidden,text);
                if(textForbidden.equals(1)){
                    text = "内容违规，无法展示";
                }
                String oldText = typechoContents.getText();

                boolean markdownStatus = oldText.contains("<!--markdown-->");
                //要做处理将typecho的图片插入格式变成markdown
                List imgList = new ArrayList<>();
                if(markdownStatus){
                    //先把typecho的图片引用模式转为标准markdown
                    List oldImgList = baseFull.getImageSrc(text);
                    List codeList = baseFull.getImageCode(text);
                    for(int c = 0; c < codeList.size(); c++){
                        String codeimg = codeList.get(c).toString();
                        String urlimg = oldImgList.get(c).toString();
                        text=text.replace(codeimg,"![image"+c+"]("+urlimg+")");
                    }
                    imgList = baseFull.getImageSrcFromMarkdown(text);
                    text=text.replace("<!--markdown-->","");
                    List codeImageMk = baseFull.getImageMk(text);
                    for(int d = 0; d < codeImageMk.size(); d++){
                        String mk = codeImageMk.get(d).toString();
                        text=text.replace(mk,"");
                    }
                }else{
                    imgList = baseFull.extractImageSrcFromHtml(text);
                }

                if(isMd==1){
                    //如果isMd等于1，则输出解析后的md代码
                    if(markdownStatus){
                        Parser parser = Parser.builder().build();
                        Node document = parser.parse(text);
                        HtmlRenderer renderer = HtmlRenderer.builder().build();
                        text = renderer.render(document);
                    }


                }
                //获取文章id，从而获取自定义字段，和分类标签
                String cid = typechoContents.getCid().toString();
                TypechoFields f = new TypechoFields();
                f.setCid(Integer.parseInt(cid));
                List<TypechoFields> fields = fieldsService.selectList(f);
                TypechoRelationships rs = new TypechoRelationships();
                rs.setCid(Integer.parseInt(cid));
                List<TypechoRelationships> relationships = relationshipsService.selectList(rs);

                List metas = new ArrayList();
                List tags = new ArrayList();
                for (int i = 0; i < relationships.size(); i++) {
                    Map json = JSONObject.parseObject(JSONObject.toJSONString(relationships.get(i)), Map.class);
                    if(json!=null){
                        String mid = json.get("mid").toString();
                        TypechoMetas metasList  = metasService.selectByKey(mid);
                        if(metasList!=null){
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

                }
                contensjson = JSONObject.parseObject(JSONObject.toJSONString(typechoContents), Map.class);
                if(markdownStatus){
                    contensjson.put("markdown",1);
                }else{
                    contensjson.put("markdown",0);
                }
                //转为map，再加入字段
                contensjson.remove("password");
                contensjson.put("images",imgList);
                contensjson.put("fields",fields);
                contensjson.put("category",metas);
                contensjson.put("tag",tags);
                contensjson.put("text",text);
                if(apiconfig.get("isLogin").toString().equals("1")){
                    if(uStatus==0){
                        contensjson.put("text","该内容登录可见");
                    }
                }

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
                redisHelp.delete(this.dataprefix+"_"+"contentsInfo_"+key+"_"+isMd,redisTemplate);
                redisHelp.setKey(this.dataprefix+"_"+"contentsInfo_"+key+"_"+isMd,contensjson,this.contentInfoCache,redisTemplate);
            }

        }catch (Exception e){
            e.printStackTrace();
            if(cacheInfo.size()>0){
                contensjson = cacheInfo;
            }
        }

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
    @RequestMapping(value = "/contentsList")
    @ResponseBody
    @LoginRequired(purview = "-1")
    public String contentsList (@RequestParam(value = "searchParams", required = false) String  searchParams,
                                @RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                                @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit,
                                @RequestParam(value = "searchKey"        , required = false, defaultValue = "") String searchKey,
                                @RequestParam(value = "order"        , required = false, defaultValue = "") String order,
                                @RequestParam(value = "random"        , required = false, defaultValue = "0") Integer random,
                                @RequestParam(value = "token"        , required = false, defaultValue = "") String token){
        TypechoContents query = new TypechoContents();
        if(limit>50){
            limit = 50;
        }
        String sqlParams = "null";
        List cacheList = new ArrayList();
        String group = "";
        Integer total = 0;
        //如果开启全局登录，则必须登录才能得到数据
        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
        if(apiconfig.get("isLogin").toString().equals("1")){
            if(uStatus==0){
                return Result.getResultJson(0,"用户未登录或Token验证失败",null);
            }
        }
        Integer uid = 0;
        if(uStatus > 0){
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            uid = Integer.parseInt(map.get("uid").toString());
        }

        //验证结束

        if (StringUtils.isNotBlank(searchParams)) {
            JSONObject object = JSON.parseObject(searchParams);
            //如果不是登陆状态，那么只显示开放状态文章。如果是，则查询自己发布的文章

            if(token==""||uStatus==0){

                object.put("status","publish");
            }else{
                if(object.get("status")==null){
                    object.put("status","publish");
                }
                //后面再优化

            }

            query = object.toJavaObject(TypechoContents.class);
            //获取自己被拉黑的情况
            if(uStatus > 0){
                if(query.getAuthorId()!=null){
                    TypechoUserlog userlog = new TypechoUserlog();
                    userlog.setType("banUser");
                    userlog.setUid(query.getAuthorId());
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
        total = service.total(query,searchKey);
        List jsonList = new ArrayList();
        //管理员和编辑以登录状态请求时，不调用缓存
        if(!group.equals("administrator")&&!group.equals("editor")) {
            cacheList = redisHelp.getList(this.dataprefix+"_"+"contentsList_"+page+"_"+limit+"_"+sqlParams+"_"+order+"_"+searchKey+"_"+random+"_"+uid, redisTemplate);
        }
        //监听异常，如果有异常则调用redis缓存中的list，如果无异常也调用redis，但是会更新数据
        try{
            if(cacheList.size()>0){
                jsonList = cacheList;
            }else{

                PageList<TypechoContents> pageList = service.selectPage(query, page, limit, searchKey,order,random);
                List<TypechoContents> list = pageList.getList();
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
                    TypechoContents content = list.get(i);

                    Map json = baseFull.getContentListInfo(content,fieldsService,relationshipsService,metasService,usersService,apiconfig);
                    if(json!=null){
                        jsonList.add(json);
                    }

                }
                redisHelp.delete(this.dataprefix+"_"+"contentsList_"+page+"_"+limit+"_"+sqlParams+"_"+order+"_"+searchKey+"_"+random+"_"+uid,redisTemplate);
                redisHelp.setList(this.dataprefix+"_"+"contentsList_"+page+"_"+limit+"_"+sqlParams+"_"+order+"_"+searchKey+"_"+random+"_"+uid,jsonList,this.contentCache,redisTemplate);
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
     * 发布文章
     * @param params Bean对象JSON字符串
     */
    @RequestMapping(value = "/contentsAdd")
    @XssCleanIgnore
    @ResponseBody
    @LoginRequired(purview = "0")
    public String contentsAdd(@RequestParam(value = "params", required = false) String  params,
                              @RequestParam(value = "token", required = false) String  token,
                              @RequestParam(value = "text", required = false) String  text,
                              @RequestParam(value = "isMd", required = false, defaultValue = "1") Integer  isMd,
                              @RequestParam(value = "isSpace", required = false, defaultValue = "0") Integer  isSpace,
                              @RequestParam(value = "isDraft", required = false, defaultValue = "0") Integer  isDraft,
                              @RequestParam(value = "isPaid", required = false, defaultValue = "0") Integer  isPaid,
                              @RequestParam(value = "shopPice", required = false) Integer  shopPice,
                              @RequestParam(value = "shopText", required = false) String  shopText,
                              @RequestParam(value = "shopDiscount", required = false, defaultValue = "1.0") String  shopDiscount,
                              @RequestParam(value = "verifyCode", required = false) String verifyCode,
                              HttpServletRequest request) {
        try {
            TypechoContents insert = null;
            String  ip = baseFull.getIpAddr(request);
            Map jsonToMap = new HashMap();
            String category = "";
            String tag = "";
            Integer sid = -1;
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer logUid =Integer.parseInt(map.get("uid").toString());
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
                //登录情况下，刷数据攻击拦截
                String isSilence = redisHelp.getRedis(this.dataprefix+"_"+logUid+"_silence",redisTemplate);
                if(isSilence!=null){
                    return Result.getResultJson(0,"你已被禁言，请耐心等待",null);
                }
                String isRepeated = redisHelp.getRedis(this.dataprefix+"_"+logUid+"_isRepeated",redisTemplate);
                if(isRepeated==null){
                    redisHelp.setRedis(this.dataprefix+"_"+logUid+"_isRepeated","1",3,redisTemplate);
                }else{
                    Integer frequency = Integer.parseInt(isRepeated) + 1;
                    if(frequency==3){
                        securityService.safetyMessage("用户ID："+logUid+"，在文章发布接口疑似存在攻击行为，请及时确认处理。","system");
                        redisHelp.setRedis(this.dataprefix+"_"+logUid+"_silence","1",Integer.parseInt(apiconfig.get("silenceTime").toString()),redisTemplate);
                        return Result.getResultJson(0,"你的请求存在恶意行为，10分钟内禁止操作！",null);
                    }else{
                        redisHelp.setRedis(this.dataprefix+"_"+logUid+"_isRepeated",frequency.toString(),3,redisTemplate);
                    }
                    return Result.getResultJson(0,"你的操作太频繁了",null);
                }
            }

            //攻击拦截结束

            //实名认证拦截
            if(apiconfig.get("identifyPost").toString().equals("1")) {
                if(UStatus.isIdentify(logUid,this.prefix,jdbcTemplate).equals(0)){
                    return Result.getResultJson(0,"请先完成身份认证",null);
                }
            }


            Integer isWaiting = 0;
            if (StringUtils.isNotBlank(params)) {
                jsonToMap =  JSONObject.parseObject(JSON.parseObject(params).toString());

                //支持两种模式提交文章内容
                if(text==null){
                    text = jsonToMap.get("text").toString();
                }

                //将内容里的base64图片变为链接
                List<String> oldImgBase64List = baseFull.getImageBase64(text);
                if(oldImgBase64List.size()>0){
                    for (int i = 0; i < oldImgBase64List.size(); i++) {
                        try{
                            String imageBase64 = oldImgBase64List.get(i);
                            String imgUrl =  uploadService.base64Upload(imageBase64, this.dataprefix,apiconfig,logUid,filesService);
                            // 使用Pattern.quote对Base64字符串进行转义，以便安全用作正则表达式
                            String safeImageBase64 = Pattern.quote(imageBase64);
                            // 使用Matcher.quoteReplacement对URL进行转义，以便安全用作替换字符串
                            String safeImgUrl = Matcher.quoteReplacement(imgUrl);

                            // 安全地替换Base64数据为图片链接
                            text = text.replaceAll(safeImageBase64, safeImgUrl);

                        }catch (Exception e){
                            e.printStackTrace();
                            System.out.println("图片转换失败");
                        }

                    }

                }

                //获取发布者信息
                String uid = map.get("uid").toString();
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
                if(text.length()<1){
                    return Result.getResultJson(0,"文章内容不能为空",null);
                }else{
                    if(text.length()>60000){
                        return Result.getResultJson(0,"超出最大文章内容长度",null);
                    }

                    //是否开启代码拦截
                    if(apiconfig.get("disableCode").toString().equals("1")) {
                        if(baseFull.haveCode(text).equals(1)){
                            return Result.getResultJson(0,"你的内容包含敏感代码，请修改后重试！",null);
                        }
                    }
                    //满足typecho的要求，加入markdown申明
                    if(isMd.equals(1)){
                        boolean status = text.contains("<!--markdown-->");
                        if(!status){
                            text = "<!--markdown-->"+text;
                        }
                    }

                }
                if(isMd.equals(1)){
                    text = text.replace("||rn||","\n");
                }
                //写入创建时间和作者
                jsonToMap.put("created",userTime);
                jsonToMap.put("modified",userTime);
                jsonToMap.put("replyTime",userTime);
                jsonToMap.put("authorId",uid);


                Map userMap =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
                String group = userMap.get("group").toString();
                //普通用户最大发文限制
                if(!group.equals("administrator")&&!group.equals("editor")){
                    String postNum = redisHelp.getRedis(this.dataprefix+"_"+logUid+"_postNum",redisTemplate);
                    if(postNum==null){
                        redisHelp.setRedis(this.dataprefix+"_"+logUid+"_postNum","1",86400,redisTemplate);
                    }else{
                        Integer post_Num = Integer.parseInt(postNum) + 1;
                        Integer postMax = Integer.parseInt(apiconfig.get("postMax").toString());
                        if(post_Num > postMax){
                            return Result.getResultJson(0,"你已超过最大发布数量限制，请您24小时后再操作",null);
                        }else{
                            redisHelp.setRedis(this.dataprefix+"_"+logUid+"_postNum",post_Num.toString(),86400,redisTemplate);
                        }


                    }
                }
                //限制结束
                //标题强制验证违禁
                String forbidden = "";
                if(apiconfig.get("forbidden")!=null){
                    forbidden = apiconfig.get("forbidden").toString();
                }
                String title = jsonToMap.get("title").toString();
                Integer titleForbidden = baseFull.getForbidden(forbidden,title);
                if(titleForbidden.equals(1)){
                    return Result.getResultJson(0,"标题存在违禁词",null);
                }
                //腾讯云内容违规检测
                if(apiconfig.get("cmsSwitch").toString().equals("1")|apiconfig.get("cmsSwitch").toString().equals("3")){
                    try{
                        String setTitle = baseFull.encrypt(title);
                        Map violationData = securityService.textViolation(setTitle);
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
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                }
                //根据后台的开关判断是否需要审核
                if(isDraft.equals(0)){
                    Integer contentAuditlevel = Integer.parseInt(apiconfig.get("contentAuditlevel").toString());

                    if(contentAuditlevel.equals(0)){
                        jsonToMap.put("status","publish");
                    }
                    if(contentAuditlevel.equals(1)){

                        if(!group.equals("administrator")&&!group.equals("editor")){
                            Integer isForbidden = baseFull.getForbidden(forbidden,text);
                            if(isForbidden.equals(0)){
                                jsonToMap.put("status","publish");
                            }else{
                                jsonToMap.put("status","waiting");
                            }
                        }else{
                            jsonToMap.put("status","publish");
                        }

                    }
                    if(contentAuditlevel.equals(2)){
                        if(!group.equals("administrator")&&!group.equals("editor")){
                            jsonToMap.put("status","waiting");
                        }else{
                            jsonToMap.put("status","publish");
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
                                jsonToMap.put("status","waiting");
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }

                    }

                }else{
                    jsonToMap.put("status","publish");
                    jsonToMap.put("type","post_draft");
                }


                jsonToMap.put("text",text);
                //部分字段不允许定义

                jsonToMap.put("commentsNum",0);
                jsonToMap.put("allowPing",1);
                jsonToMap.put("allowFeed",1);
                jsonToMap.put("allowComment",1);
                jsonToMap.put("orderKey",0);
                jsonToMap.put("parent",0);
                jsonToMap.remove("password");
                jsonToMap.remove("sid");
                jsonToMap.remove("isrecommend");
                insert = JSON.parseObject(JSON.toJSONString(jsonToMap), TypechoContents.class);

            }


            int rows = service.insert(insert);

            Integer cid = insert.getCid();
            //文章添加完成后，再处理分类和标签还有挂载商品，还有slug
            TypechoContents slugUpdate = new TypechoContents();
            slugUpdate.setSlug(cid.toString());
            slugUpdate.setCid(cid);
            service.update(slugUpdate);

            if(rows > 0) {
                if (category.length()>0) {
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
                if (tag.length()>0) {
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
                if(isPaid.equals(0)){
                    if(sid>-1){
                        Integer uid  = Integer.parseInt(map.get("uid").toString());
                        //判断商品是不是自己的
                        TypechoShop shop = new TypechoShop();
                        shop.setUid(uid);
                        shop.setId(sid);
                        Integer num  = shopService.total(shop,null);
                        if(num >= 1){
                            shop.setCid(cid);
                            shopService.update(shop);
                        }
                    }
                }



            }
            Long date = System.currentTimeMillis();
            String created = String.valueOf(date).substring(0,10);

            if(isDraft.equals(0)){
                if(isSpace.equals(1)){
                    //判断用户经验值
                    Integer spaceMinExp = 0;
                    if(apiconfig.get("spaceMinExp")!=null){
                        spaceMinExp = Integer.parseInt(apiconfig.get("spaceMinExp").toString());
                    }
                    TypechoUsers curUser = usersService.selectByKey(logUid);
                    Integer Exp = curUser.getExperience();
                    if(Exp < spaceMinExp){
                        return Result.getResultJson(0,"发布动态最低要求经验值为"+spaceMinExp+",你当前经验值"+Exp,null);
                    }
                    TypechoSpace space = new TypechoSpace();
                    space.setType(1);
                    space.setText("发布了新文章");
                    space.setCreated(Integer.parseInt(created));
                    space.setModified(Integer.parseInt(created));
                    space.setUid(logUid);
                    space.setToid(cid);
                    Integer spaceAudit = Integer.parseInt(apiconfig.get("spaceAudit").toString());
                    if(spaceAudit.equals(1)){
                        space.setStatus(0);
                    }else{
                        space.setStatus(1);
                    }
                    spaceService.insert(space);
                }
            }
            String resText = "发布成功";
            if(isWaiting>0){
                resText = "文章将在审核后发布！";

            }else{
                TypechoUsers updateUser = new TypechoUsers();
                updateUser.setUid(logUid);
                updateUser.setIp(ip);
                updateUser.setLocal(baseFull.getLocal(ip));
                updateUser.setPosttime(Integer.parseInt(created));
                //如果无需审核，则立即增加经验
                Integer postExp = Integer.parseInt(apiconfig.get("postExp").toString());
                if(postExp>0){
                    //生成操作记录

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                    String curtime = sdf.format(new Date(date));
                    TypechoUserlog userlog = new TypechoUserlog();
                    userlog.setUid(logUid);
                    //cid用于存放真实时间
                    userlog.setCid(Integer.parseInt(curtime));
                    userlog.setType("postExp");
                    Integer size = userlogService.total(userlog);
                    //只有前三次发布文章获得经验
                    if(size < 3){
                        userlog.setNum(postExp);
                        userlog.setCreated(Integer.parseInt(created));
                        userlogService.insert(userlog);
                        //修改用户资产
                        TypechoUsers oldUser = usersService.selectByKey(logUid);
                        Integer experience = oldUser.getExperience();
                        experience = experience + postExp;
                        updateUser.setExperience(experience);


                    }
                }
                usersService.update(updateUser);

            }
            //添加付费阅读
            if (isPaid.equals(1)) {
                TypechoShop shop = new TypechoShop();
                shop.setValue(shopText);
                shop.setUid(logUid);
                shop.setVipDiscount(shopDiscount);
                shop.setIsView(0);
                shop.setCreated(Integer.parseInt(created));
                shop.setNum(-1);
                shop.setStatus(1);
                shop.setPrice(shopPice);
                shop.setType(4);
                shop.setCid(cid);
                shop.setUid(logUid);
                shop.setIsMd(isMd);
                shopService.insert(shop);
            }
            editFile.setLog("用户"+logUid+"请求发布了新文章");
            JSONObject response = new JSONObject();
            response.put("code" ,rows > 0 ? 1: 0 );
            response.put("data" , rows);
            response.put("msg"  , rows > 0 ? resText : "发布失败");
            redisHelp.deleteKeysWithPattern("*"+this.dataprefix+"_contentsList_1*",redisTemplate,this.dataprefix);
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }
    }

    /***
     * 文章修改
     * @param params Bean对象JSON字符串
     */
    @RequestMapping(value = "/contentsUpdate")
    @XssCleanIgnore
    @ResponseBody
    @LoginRequired(purview = "0")
    public String contentsUpdate(@RequestParam(value = "params", required = false) String  params,
                                 @RequestParam(value = "token", required = false) String  token,
                                 @RequestParam(value = "text", required = false) String  text,
                                 @RequestParam(value = "isDraft", required = false, defaultValue = "0") Integer  isDraft,
                                 @RequestParam(value = "isMd", required = false, defaultValue = "1") Integer  isMd,
                                 @RequestParam(value = "isPaid", required = false, defaultValue = "0") Integer  isPaid,
                                 @RequestParam(value = "shopPice", required = false) Integer  shopPice,
                                 @RequestParam(value = "shopText", required = false) String  shopText,
                                 @RequestParam(value = "shopDiscount", required = false, defaultValue = "1.0") String  shopDiscount) {

        try {
            TypechoContents update = null;
            Map jsonToMap =null;
            TypechoContents info = new TypechoContents();
            String category = "";
            String tag = "";
            Integer sid = -1;
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer isWaiting = 0;
            Integer logUid =Integer.parseInt(map.get("uid").toString());
            if (StringUtils.isNotBlank(params)) {
                Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
                jsonToMap =  JSONObject.parseObject(JSON.parseObject(params).toString());
                //支持两种模式提交评论内容
                if(text==null){
                    text = jsonToMap.get("text").toString();
                }
                if(text.length()<1){
                    return Result.getResultJson(0,"文章内容不能为空",null);
                }
                //生成typecho数据库格式的修改时间戳
                Long date = System.currentTimeMillis();
                String userTime = String.valueOf(date).substring(0,10);
                jsonToMap.put("modified",userTime);
                //获取商品id
                if(jsonToMap.get("sid")!=null){
                    sid = Integer.parseInt(jsonToMap.get("sid").toString());
                }
                info = service.selectByKey(jsonToMap.get("cid").toString());
                if(info==null){
                    return Result.getResultJson(0,"文章不存在",null);
                }
                //验证用户是否为作品的作者，以及权限
                Integer uid =Integer.parseInt(map.get("uid").toString());
                String group = map.get("group").toString();
                if(!group.equals("administrator")&&!group.equals("editor")){

                    Integer authorId = info.getAuthorId();
                    if(!uid.equals(authorId)){
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
                }else{
                    //满足typecho的要求，加入markdown申明
                    //是否开启代码拦截
                    if(apiconfig.get("disableCode").toString().equals("1")) {
                        if(baseFull.haveCode(text).equals(1)){
                            return Result.getResultJson(0,"你的内容包含敏感代码，请修改后重试！",null);
                        }
                    }


                }
                if(isMd.equals(1)){
                    boolean status = text.contains("<!--markdown-->");
                    if(!status){
                        text = "<!--markdown-->"+text;

                    }
                }
                if(isMd.equals(1)){
                    text = text.replace("||rn||","\n");
                }
                jsonToMap.put("text",text);
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

                jsonToMap.remove("isrecommend");
                jsonToMap.remove("istop");
                jsonToMap.remove("isswiper");
                jsonToMap.remove("replyTime");
//                //状态重新变成待审核
//                if(!group.equals("administrator")){
//                    jsonToMap.put("status","waiting");
//                }
                //标题强制验证违禁
                String forbidden = "";
                if(apiconfig.get("forbidden")!=null){
                    forbidden = apiconfig.get("forbidden").toString();
                }
                String title = jsonToMap.get("title").toString();
                Integer titleForbidden = baseFull.getForbidden(forbidden,title);
                if(titleForbidden.equals(1)){
                    return Result.getResultJson(0,"标题存在违禁词",null);
                }
                //腾讯云内容违规检测
                if(apiconfig.get("cmsSwitch").toString().equals("1")|apiconfig.get("cmsSwitch").toString().equals("3")){
                    try{
                        String setTitle = baseFull.encrypt(title);
                        Map violationData = securityService.textViolation(setTitle);
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
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                }
                //根据后台的开关判断
                if(isDraft.equals(0)){
                    Integer contentAuditlevel = Integer.parseInt(apiconfig.get("contentAuditlevel").toString());
                    if(contentAuditlevel.equals(0)){
                        jsonToMap.put("status","publish");
                    }
                    if(contentAuditlevel.equals(1)){

                        if(!group.equals("administrator")&&!group.equals("editor")){
                            Integer isForbidden = baseFull.getForbidden(forbidden,text);
                            if(isForbidden.equals(0)){
                                jsonToMap.put("status","publish");
                            }else{
                                jsonToMap.put("status","waiting");
                            }
                        }else{
                            jsonToMap.put("status","publish");
                        }

                    }
                    if(contentAuditlevel.equals(2)){
                        //除管理员外，文章默认待审核
                        if(!group.equals("administrator")&&!group.equals("editor")){
                            jsonToMap.put("status","waiting");
                        }else{
                            jsonToMap.put("status","publish");
                        }
                    }
                    jsonToMap.put("type","post");

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
                                jsonToMap.put("status","waiting");
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }

                    }
                }else{
                    jsonToMap.put("status","publish");
                    jsonToMap.put("type","post_draft");
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
                if (isPaid.equals(0)) {
                    if(sid>-1){
                        Integer uid  = Integer.parseInt(map.get("uid").toString());
                        //判断商品是不是自己的
                        TypechoShop shop = new TypechoShop();
                        shop.setUid(uid);
                        shop.setId(sid);
                        Integer num  = shopService.total(shop,null);
                        if(num >= 1){
                            //如果是，去数据库将其它商品的cid改为0
                            TypechoShop rmshop = new TypechoShop();
                            rmshop.setCid(cid);
                            List<TypechoShop> list = shopService.selectList(rmshop);
                            for (int i = 0; i < list.size(); i++) {
                                list.get(i).setCid(-1);
                                shopService.update(list.get(i));
                            }
                            //清除完之前的时候，修改新的
                            shop.setCid(cid);
                            shopService.update(shop);
                        }

                    }
                }

            }
            //编辑付费阅读
            if (isPaid.equals(1)) {
                TypechoShop shop = new TypechoShop();
                if(!sid.equals(0)){
                    shop.setId(sid);
                }


                shop.setValue(shopText);
                shop.setVipDiscount(shopDiscount);
                shop.setIsView(0);
                shop.setNum(-1);
                shop.setStatus(1);
                shop.setPrice(shopPice);
                shop.setType(4);
                shop.setIsMd(isMd);
                shop.setUid(info.getAuthorId());
                if(sid.equals(0)) {
                    shop.setCid(cid);
                    shopService.insert(shop);
                }else{
                    shopService.update(shop);
                }

            }

            editFile.setLog("用户"+logUid+"请求修改了文章"+cid);
            String resText = "修改成功";
            if(isWaiting>0){
                resText = "文章将在审核后发布！";
            }
            //清除缓存
            if(rows > 0){
                redisHelp.deleteKeysWithPattern("*"+this.dataprefix+"_"+"contentsInfo_"+cid+"*",redisTemplate,this.dataprefix);
                redisHelp.deleteKeysWithPattern("*"+this.dataprefix+"_contentsList_1*",redisTemplate,this.dataprefix);
            }

            JSONObject response = new JSONObject();
            response.put("code" ,rows > 0 ? 1: 0 );
            response.put("data" , rows);
            response.put("msg"  , rows > 0 ? resText : "修改失败");
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }
    }

    /***
     * 文章删除
     */
    @RequestMapping(value = "/contentsDelete")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String formDelete(@RequestParam(value = "key", required = false) String  key, @RequestParam(value = "token", required = false) String  token) {
        try {
            //String group = (String) redisHelp.getValue("userInfo"+token,"group",redisTemplate);
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer uid  = Integer.parseInt(map.get("uid").toString());
            String group = map.get("group").toString();
            TypechoContents contents = service.selectByKey(key);
            Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
            if(!group.equals("administrator")&&!group.equals("editor")){
                Integer allowDelete = Integer.parseInt(apiconfig.get("allowDelete").toString());
                if(allowDelete.equals(0)){
                    return Result.getResultJson(0,"系统禁止删除文章",null);
                }

                Integer aid = contents.getAuthorId();
                if(!aid.equals(uid)){
                    return Result.getResultJson(0,"你无权进行此操作",null);
                }
//                jsonToMap.put("status","0");
            }
            //发送消息
            Long date = System.currentTimeMillis();
            String created = String.valueOf(date).substring(0,10);
            TypechoInbox insert = new TypechoInbox();
            insert.setUid(uid);
            insert.setTouid(contents.getAuthorId());
            insert.setType("system");
            insert.setText("你的文章【"+contents.getTitle()+"】已被删除");
            insert.setCreated(Integer.parseInt(created));
            inboxService.insert(insert);

            Integer logUid =Integer.parseInt(map.get("uid").toString());
            int rows = service.delete(key);
            //删除与分类的映射
            int st = relationshipsService.delete(key);

            //更新用户经验
            Integer deleteExp = Integer.parseInt(apiconfig.get("deleteExp").toString());
            if(deleteExp > 0){
                TypechoUsers oldUser = usersService.selectByKey(contents.getAuthorId());
                if(oldUser!=null){
                    Integer experience = oldUser.getExperience();
                    experience = experience - deleteExp;
                    TypechoUsers updateUser = new TypechoUsers();
                    updateUser.setUid(contents.getAuthorId());
                    updateUser.setExperience(experience);
                    usersService.update(updateUser);
                }


            }
            editFile.setLog("管理员"+logUid+"请求删除文章"+key);
            if(rows > 0){
                redisHelp.deleteKeysWithPattern("*"+this.dataprefix+"_contentsList_1*",redisTemplate,this.dataprefix);
            }
            JSONObject response = new JSONObject();
            response.put("code" ,rows > 0 ? 1: 0 );
            response.put("data" , rows);
            response.put("msg"  , rows > 0 ? "操作成功" : "操作失败");
            //删除列表redis
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
    @LoginRequired(purview = "0")
    public String contentsAudit(@RequestParam(value = "key", required = false) String  key,
                                @RequestParam(value = "token", required = false) String  token,
                                @RequestParam(value = "type", required = false, defaultValue = "0") Integer  type,
                                @RequestParam(value = "reason", required = false) String  reason) {
        try {
            if(type==null){
                type = 0;
            }
            Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
            //String group = (String) redisHelp.getValue("userInfo"+token,"group",redisTemplate);
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            String group = map.get("group").toString();
            Integer logUid  = Integer.parseInt(map.get("uid").toString());
            if(!group.equals("administrator")&&!group.equals("editor")){
                return Result.getResultJson(0,"你没有操作权限",null);
            }
            TypechoContents info = service.selectByKey(key);
            if(info.getStatus().equals("publish")){
                return Result.getResultJson(0,"该文章已审核通过",null);
            }
            Integer cUid = info.getAuthorId();
            info.setCid(Integer.parseInt(key));
            //0为审核通过，1为不通过，并发送消息
            if(type.equals(0)){
                info.setStatus("publish");
            }else{
                if(reason==""||reason==null){
                    return Result.getResultJson(0,"请输入拒绝理由",null);
                }
                info.setStatus("reject");
            }
            Integer rows = service.update(info);
            //给作者发送邮件
            TypechoUsers ainfo = usersService.selectByKey(info.getAuthorId());
            if(ainfo==null){
                return Result.getResultJson(0,"文章作者已注销",null);
            }
            TypechoEmailtemplate emailtemplate = emailtemplateService.selectByKey(1);
            String title = info.getTitle();
            Integer uid = ainfo.getUid();
            //根据过审状态发送不同的内容
            if(type.equals(0)) {
                if(apiconfig.get("isEmail").toString().equals("2")){
                    if (ainfo.getMail() != null) {
                        String email = ainfo.getMail();
                        try {
                            String userName = ainfo.getName();
                            if(ainfo.getScreenName()!=null){
                                userName = ainfo.getScreenName();
                            }
                            if(emailtemplate!=null) {
                                MailService.send(userName + ",您的文章已审核通过",
                                        emailText.getReviewEmail(emailtemplate, userName, title, "已审核通过"),
                                        new String[]{email}, new String[]{},apiconfig);
                            }
                        } catch (Exception e) {
                            System.err.println("邮箱发信配置错误：" + e);
                        }



                    }
                }

                //发送消息
                Long date = System.currentTimeMillis();
                String created = String.valueOf(date).substring(0, 10);
                TypechoInbox insert = new TypechoInbox();
                insert.setUid(logUid);
                insert.setTouid(info.getAuthorId());
                insert.setType("system");
                insert.setText("你的文章【" + info.getTitle() + "】已审核通过");
                insert.setCreated(Integer.parseInt(created));
                inboxService.insert(insert);
            }else{
                if(apiconfig.get("isEmail").toString().equals("2")){
                    if (ainfo.getMail() != null) {
                        String email = ainfo.getMail();
                        try {
                            String userName = ainfo.getName();
                            if(ainfo.getScreenName()!=null){
                                userName = ainfo.getScreenName();
                            }
                            if(emailtemplate!=null) {
                                MailService.send(userName + ",您的文章未审核通过",
                                        emailText.getReviewEmail(emailtemplate, userName, title, "未审核通过！理由如下：" + reason),
                                        new String[]{email}, new String[]{},apiconfig);
                            }
                        } catch (Exception e) {
                            System.err.println("邮箱发信配置错误：" + e);
                        }
                    }
                }
                //发送消息
                Long date = System.currentTimeMillis();
                String created = String.valueOf(date).substring(0, 10);
                TypechoInbox insert = new TypechoInbox();
                insert.setUid(logUid);
                insert.setTouid(info.getAuthorId());
                insert.setType("system");
                insert.setText("你的文章【" + info.getTitle() + "】未审核通过。理由如下："+reason);
                insert.setCreated(Integer.parseInt(created));
                inboxService.insert(insert);
            }
            try{
                if(type.equals(0)) {
                    //审核后增加经验
                    Integer postExp = Integer.parseInt(apiconfig.get("postExp").toString());
                    if(postExp > 0){
                        //生成操作记录
                        Long date = System.currentTimeMillis();
                        String created = String.valueOf(date).substring(0,10);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                        String curtime = sdf.format(new Date(date));

                        TypechoUserlog userlog = new TypechoUserlog();
                        userlog.setUid(cUid);
                        //cid用于存放真实时间
                        userlog.setCid(Integer.parseInt(curtime));
                        userlog.setType("postExp");
                        Integer size = userlogService.total(userlog);
                        //只有前三次发布文章获得经验
                        if(size < 3){
                            userlog.setNum(postExp);
                            userlog.setCreated(Integer.parseInt(created));
                            userlogService.insert(userlog);
                            //修改用户资产
                            TypechoUsers oldUser = usersService.selectByKey(cUid);
                            Integer experience = oldUser.getExperience();
                            experience = experience + postExp;
                            TypechoUsers updateUser = new TypechoUsers();
                            updateUser.setUid(cUid);
                            updateUser.setExperience(experience);
                            usersService.update(updateUser);
                        }
                    }

                }

            }catch (Exception e){
                System.out.println("经验增加出错！");
                e.printStackTrace();
            }

            if(rows > 0){
                redisHelp.deleteKeysWithPattern("*"+this.dataprefix+"_contentsList_1*",redisTemplate,this.dataprefix);
            }
            editFile.setLog("管理员"+logUid+"请求审核文章"+key);
            JSONObject response = new JSONObject();
            response.put("code" ,rows > 0 ? 1: 0 );
            response.put("data" , rows);
            response.put("msg"  , rows > 0 ? "操作成功，缓存缘故，数据可能存在延迟" : "操作失败");
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }
    }
    /***
     * 文章自定义字段设置
     */
    @RequestMapping(value = "/setFields")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String setFields(@RequestParam(value = "cid", required = false) Integer  cid,@RequestParam(value = "name", required = false) String  name,@RequestParam(value = "strvalue", required = false) String  strvalue,  @RequestParam(value = "token", required = false) String  token) {
        try {
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer logUid =Integer.parseInt(map.get("uid").toString());
            Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
            String fieldstext = apiconfig.get("fields").toString();

            Integer status = 1;
            if(fieldstext!=null){
                if(fieldstext.indexOf(",") != -1){
                    String[] strarray=fieldstext.split(",");
                    for (int i = 0; i < strarray.length; i++){
                        String str = strarray[i];
                        if(name.indexOf(str) != -1){
                            status = 0;
                        }
                        break;
                    }
                }else{
                    if(name.indexOf(fieldstext) != -1){
                        status = 0;
                    }
                }
            }else{
                status = 0;
            }
            if(status==0){
                return Result.getResultJson(0,"操作失败，字段未被定义",null);
            }
            TypechoFields fields = new TypechoFields();
            fields.setCid(cid);
            fields.setType("str");
            fields.setName(name);
            //判断是否存在
            Integer isFields = fieldsService.total(fields);
            fields.setStrValue(strvalue);
            Integer rows;
            if(isFields>0){
                fieldsService.delete(cid,name);
                rows = fieldsService.insert(fields);
            }else{
                rows = fieldsService.insert(fields);
            }
            editFile.setLog("用户"+logUid+"请求设置自定义字段");
            JSONObject response = new JSONObject();
            response.put("code" ,rows > 0 ? 1: 0 );
            response.put("data" , rows);
            response.put("msg"  , "操作成功");
            return response.toString();
        }catch (Exception e){
            System.out.print(e);
            return Result.getResultJson(0,"操作失败",null);
        }
    }
    /***
     * 文章推荐&加精
     */
    @RequestMapping(value = "/toRecommend")
    @ResponseBody
    @LoginRequired(purview = "1")
    public String addRecommend(@RequestParam(value = "key", required = false) String  key,@RequestParam(value = "recommend", required = false) Integer  recommend, @RequestParam(value = "token", required = false) String  token) {
        try {
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            String group = map.get("group").toString();
            Integer logUid =Integer.parseInt(map.get("uid").toString());
            TypechoContents info = service.selectByKey(key);
            Long date = System.currentTimeMillis();
            String time = String.valueOf(date).substring(0,10);
            Integer modified = Integer.parseInt(time);
            info.setModified(modified);
            info.setCid(Integer.parseInt(key));
            info.setIsrecommend(recommend);
            Integer rows = service.update(info);
            if(rows > 0){
                redisHelp.deleteKeysWithPattern("*"+this.dataprefix+"_contentsList_1*",redisTemplate,this.dataprefix);
            }
            editFile.setLog("管理员"+logUid+"请求推荐文章"+key);
            JSONObject response = new JSONObject();
            response.put("code" ,rows > 0 ? 1: 0 );
            response.put("data" , rows);
            response.put("msg"  , rows > 0 ? "操作成功" : "操作失败");
            return response.toString();
        }catch (Exception e){
            return Result.getResultJson(0,"操作失败",null);
        }
    }

    /***
     * 文章置顶
     */
    @RequestMapping(value = "/addTop")
    @ResponseBody
    @LoginRequired(purview = "1")
    public String addTop(@RequestParam(value = "key", required = false) String  key,@RequestParam(value = "istop", required = false) Integer  istop, @RequestParam(value = "token", required = false) String  token) {
        try {

            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer logUid =Integer.parseInt(map.get("uid").toString());
            TypechoContents info = service.selectByKey(key);
            //生成typecho数据库格式的修改时间戳
            Long date = System.currentTimeMillis();
            String time = String.valueOf(date).substring(0,10);
            Integer modified = Integer.parseInt(time);
            info.setModified(modified);
            info.setCid(Integer.parseInt(key));
            info.setIstop(istop);
            Integer rows = service.update(info);
            if(rows > 0){
                redisHelp.deleteKeysWithPattern("*"+this.dataprefix+"_contentsList_1*",redisTemplate,this.dataprefix);
            }
            editFile.setLog("管理员"+logUid+"请求置顶文章"+key);
            JSONObject response = new JSONObject();
            response.put("code" ,rows > 0 ? 1: 0 );
            response.put("data" , rows);
            response.put("msg"  , rows > 0 ? "操作成功" : "操作失败");
            return response.toString();
        }catch (Exception e){
            return Result.getResultJson(0,"操作失败",null);
        }
    }
    /***
     * 文章轮播
     */
    @RequestMapping(value = "/addSwiper")
    @ResponseBody
    @LoginRequired(purview = "1")
    public String addSwiper(@RequestParam(value = "key", required = false) String  key,@RequestParam(value = "isswiper", required = false) Integer  isswiper, @RequestParam(value = "token", required = false) String  token) {
        try {

            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer logUid =Integer.parseInt(map.get("uid").toString());
            TypechoContents info = service.selectByKey(key);
            //生成typecho数据库格式的修改时间戳
            Long date = System.currentTimeMillis();
            String time = String.valueOf(date).substring(0,10);
            Integer modified = Integer.parseInt(time);
            info.setModified(modified);
            info.setCid(Integer.parseInt(key));
            info.setIsswiper(isswiper);
            Integer rows = service.update(info);
            if(rows > 0){
                redisHelp.deleteKeysWithPattern("*"+this.dataprefix+"_contentsList_1*",redisTemplate,this.dataprefix);
            }
            editFile.setLog("管理员"+logUid+"请求轮播文章"+key);
            JSONObject response = new JSONObject();
            response.put("code" ,rows > 0 ? 1: 0 );
            response.put("data" , rows);
            response.put("msg"  , rows > 0 ? "操作成功" : "操作失败");
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
    @LoginRequired(purview = "0")
    public String isCommnet(@RequestParam(value = "key", required = false) String  key, @RequestParam(value = "token", required = false) String  token) {
        try {
            if(key.length()<1){
                return Result.getResultJson(0,"参数错误",null);
            }
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer uid  = Integer.parseInt(map.get("uid").toString());
            //首先判断是否为作者自己
            TypechoContents contents = new TypechoContents();
            contents.setCid(Integer.parseInt(key));
            contents.setAuthorId(uid);
            Integer isAuthor = service.total(contents,null);
            if(isAuthor>0){
                return Result.getResultJson(1,"",null);
            }
            TypechoComments comments = new TypechoComments();
            comments.setCid(Integer.parseInt(key));
            comments.setAuthorId(uid);
            Integer isCommnet = commentsService.total(comments,null);
            if(isCommnet>0){
                return Result.getResultJson(1,"",null);
            }else{
                return Result.getResultJson(0,"",null);
            }

        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0,"",null);
        }
    }
    /**
     * pexels图库
     * */
    @RequestMapping(value = "/ImagePexels")
    @ResponseBody
    @LoginRequired(purview = "-1")
    public String ImagePexels(@RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                              @RequestParam(value = "searchKey"        , required = false) String searchKey,HttpServletRequest request) {
        String  ip = baseFull.getIpAddr(request);
        String  agent =  request.getHeader("User-Agent");
        String isRepeated = redisHelp.getRedis(agent+"_isRepeated_"+ip,redisTemplate);
        if(isRepeated==null){
            redisHelp.setRedis(agent+"_isRepeated_"+ip,"1",3,redisTemplate);
        }else{
            return Result.getResultJson(0,"你的操作太频繁了",null);
        }

        String cacheImage = redisHelp.getRedis(this.dataprefix+"_"+"ImagePexels_"+searchKey+"_"+page,redisTemplate);
        String imgList = "";
        Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
        if(searchKey!=null){
            try {
                searchKey = URLDecoder.decode(searchKey, "utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        String pexelsKey = apiconfig.get("pexelsKey").toString();
        if(cacheImage==null){
            if(searchKey==null){

                imgList = HttpClient.doGetImg("https://api.pexels.com/v1/curated?per_page=15&page="+page,pexelsKey);
            }else{
                try {
                    searchKey = URLEncoder.encode(searchKey,"UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                imgList = HttpClient.doGetImg("https://api.pexels.com/v1/search?query="+searchKey+"&page="+page,pexelsKey);
            }

            if(imgList==null){
                return Result.getResultJson(0,"图片接口异常",null);
            }
            HashMap  jsonMap = JSON.parseObject(imgList, HashMap.class);
            if(jsonMap.get("code")!=null||jsonMap.get("error")!=null){
                return Result.getResultJson(0,"图片获取失败，请重试",null);
            }
            if(imgList!=null){
                redisHelp.delete(this.dataprefix+"_"+"ImagePexels",redisTemplate);
                redisHelp.setRedis(this.dataprefix+"_"+"ImagePexels_"+searchKey+"_"+page,imgList,21600,redisTemplate);
            }

        }else{
            imgList = cacheImage;
        }
        return imgList;
    }

    /***
     * 文章打赏者列表
     */
    @RequestMapping(value = "/rewardList")
    @ResponseBody
    @LoginRequired(purview = "-1")
    public String rewardList(@RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                                 @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit,
                                 @RequestParam(value = "id", required = false) Integer  id) {
        if(limit>50){
            limit = 50;
        }
        Integer total = 0;

        TypechoUserlog query = new TypechoUserlog();
        query.setCid(id);
        query.setType("reward");
        total = userlogService.total(query);

        List jsonList = new ArrayList();
        List cacheList = redisHelp.getList(this.dataprefix+"_"+"rewardList_"+page+"_"+limit,redisTemplate);
        try{
            if(cacheList.size()>0){
                jsonList = cacheList;
            }else {
                PageList<TypechoUserlog> pageList = userlogService.selectPage(query, page, limit);
                List<TypechoUserlog> list = pageList.getList();
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
                    Integer userid = list.get(i).getUid();
                    Map json = JSONObject.parseObject(JSONObject.toJSONString(list.get(i)), Map.class);
                    //获取用户信息
                    Map userJson = UserStatus.getUserInfo(userid,allconfigService,usersService);
                    //获取用户等级
//                    TypechoComments comments = new TypechoComments();
//                    comments.setAuthorId(userid);
//                    Integer lv = commentsService.total(comments,null);
//                    userJson.put("lv", baseFull.getLv(lv));
                    json.put("userJson",userJson);
                    jsonList.add(json);
                }
                redisHelp.delete(this.dataprefix+"_"+"rewardList_"+page+"_"+limit, redisTemplate);
                redisHelp.setList(this.dataprefix+"_"+"rewardList_"+page+"_"+limit, jsonList, 5, redisTemplate);
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
        response.put("total", total);
        return response.toString();

    }
    /**
     * 十年之约
     * https://www.foreverblog.cn/
     * */
    @RequestMapping(value = "/foreverblog")
    @ResponseBody
    @LoginRequired(purview = "-1")
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
     * 注册系统配置信息
     */
    @RequestMapping(value = "/contentConfig")
    @ResponseBody
    @LoginRequired(purview = "-1")
    public String contentConfig() {
        Map contentConfig = new HashMap<String, String>();
        try{
            Map cacheInfo = redisHelp.getMapValue(this.dataprefix+"_contentConfig",redisTemplate);

            if(cacheInfo.size()>0){
                contentConfig = cacheInfo;
            }else{
                Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
                Integer allowDelete = Integer.parseInt(apiconfig.get("allowDelete").toString());
                contentConfig.put("allowDelete",allowDelete);
                redisHelp.delete(this.dataprefix+"_contentConfig",redisTemplate);
                redisHelp.setKey(this.dataprefix+"_contentConfig",contentConfig,5,redisTemplate);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        JSONObject response = new JSONObject();
        response.put("code" , 1);
        response.put("data" , contentConfig);
        response.put("msg"  , "");
        return response.toString();
    }
    /***
     * 全站统计
     */
    @RequestMapping(value = "/allData")
    @ResponseBody
    @LoginRequired(purview = "1")
    public String allData( @RequestParam(value = "token", required = false) String  token) {
        //String group = (String) redisHelp.getValue("userInfo"+token,"group",redisTemplate);
        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        JSONObject data = new JSONObject();

        TypechoContents contents = new TypechoContents();
        contents.setType("post");
        contents.setStatus("publish");
        Integer allContents = service.total(contents,null);

        TypechoComments comments = new TypechoComments();
        Integer allComments = commentsService.total(comments,null);

        TypechoUsers users = new TypechoUsers();
        Integer allUsers = usersService.total(users,null);


        TypechoShop shop = new TypechoShop();
        Integer allShop = shopService.total(shop,null);

        TypechoSpace space = new TypechoSpace();
        Integer allSpace = spaceService.total(space,null);

        TypechoAds ads = new TypechoAds();
        Integer allAds = adsService.total(ads);


        TypechoInbox inbox = new TypechoInbox();
        inbox.setType("selfDelete");
        Integer selfDelete = inboxService.total(inbox);

        inbox.setType("report");
        inbox.setValue(0);
        Integer report = inboxService.total(inbox);



        contents.setType("post");
        contents.setStatus("waiting");
        Integer upcomingContents = service.total(contents,null);

        comments.setStatus("waiting");
        Integer upcomingComments = commentsService.total(comments,null);

        shop.setStatus(0);
        Integer upcomingShop = shopService.total(shop,null);

        space.setStatus(0);
        Integer upcomingSpace = spaceService.total(space,null);

        ads.setStatus(0);
        Integer upcomingAds = adsService.total(ads);

        TypechoUserlog userlog = new TypechoUserlog();
        userlog.setType("withdraw");
        userlog.setCid(-1);
        Integer upcomingWithdraw = userlogService.total(userlog);

        Integer upcomingIdentifyConsumer = jdbcTemplate.queryForObject("select count(*) from `" + prefix + "_consumer` where identifyStatus = '0';", Integer.class);
        Integer upcomingIdentifyCompany = jdbcTemplate.queryForObject("select count(*) from `" + prefix + "_company` where identifyStatus = '0';", Integer.class);
        data.put("allContents",allContents);
        data.put("allComments",allComments);
        data.put("allUsers",allUsers);
        data.put("allShop",allShop);
        data.put("allSpace",allSpace);
        data.put("allAds",allAds);

        data.put("upcomingContents",upcomingContents);
        data.put("selfDelete",selfDelete);
        data.put("report",report);
        data.put("upcomingComments",upcomingComments);
        data.put("upcomingShop",upcomingShop);
        data.put("upcomingSpace",upcomingSpace);
        data.put("upcomingAds",upcomingAds);
        data.put("upcomingWithdraw",upcomingWithdraw);
        data.put("upcomingIdentifyConsumer",upcomingIdentifyConsumer);
        data.put("upcomingIdentifyCompany",upcomingIdentifyCompany);

        JSONObject response = new JSONObject();
        response.put("code" , 1);
        response.put("msg"  , "");
        response.put("data" , data);

        return response.toString();
    }

    /***
     * 我关注的人的文章
     */
    @RequestMapping(value = "/followContents")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String followSpace(@RequestParam(value = "token", required = false) String  token,
                              @RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                              @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit){
        page = page - 1;

        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid  = Integer.parseInt(map.get("uid").toString());
        List jsonList = new ArrayList();
        List cacheList = redisHelp.getList(this.dataprefix+"_"+"followContents_"+uid+"_"+page+"_"+limit,redisTemplate);
        try {
            if (cacheList.size() > 0) {
                jsonList = cacheList;
            } else {
                Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
                String sql = "SELECT content.* FROM "+prefix+"_contents AS content JOIN "+prefix+"_fan AS fan ON content.authorId = fan.touid WHERE fan.uid = ? AND content.status = 'publish' ORDER BY content.created DESC LIMIT ?, ?";
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
                    //加入自定义字段信息，这里取消注释即可开启，但是数据库查询会消耗性能
                    String cid = json.get("cid").toString();
                    TypechoFields f = new TypechoFields();
                    f.setCid(Integer.parseInt(cid));
                    List<TypechoFields> fields = fieldsService.selectList(f);
                    json.put("fields",fields);

                    TypechoRelationships rs = new TypechoRelationships();
                    rs.setCid(Integer.parseInt(cid));
                    List<TypechoRelationships> relationships = relationshipsService.selectList(rs);

                    List metas = new ArrayList();
                    List tags = new ArrayList();
                    if(relationships.size()>0){
                        for (int j = 0; j < relationships.size(); j++) {
                            Map info = JSONObject.parseObject(JSONObject.toJSONString(relationships.get(j)), Map.class);
                            if(info!=null){
                                String mid = info.get("mid").toString();

                                TypechoMetas metasList  = metasService.selectByKey(mid);
                                if(metasList!=null){
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

                        }
                    }

                    //写入作者详细信息
                    Integer authorId = Integer.parseInt(json.get("authorId").toString());
                    if(uid>0){
                        TypechoUsers author = usersService.selectByKey(authorId);
                        Map authorInfo = new HashMap();
                        if(author!=null){
                            String name = author.getName();
                            if(author.getScreenName()!=null&&author.getScreenName()!=""){
                                name = author.getScreenName();
                            }
                            String avatar = apiconfig.get("webinfoAvatar").toString() + "null";
                            if(author.getAvatar()!=null&&author.getAvatar()!=""){
                                avatar = author.getAvatar();
                            }else{
                                if(author.getMail()!=null&&author.getMail()!=""){
                                    String mail = author.getMail();

                                    if(mail.indexOf("@qq.com") != -1){
                                        String qq = mail.replace("@qq.com","");
                                        avatar = "https://q1.qlogo.cn/g?b=qq&nk="+qq+"&s=640";
                                    }else{
                                        avatar = baseFull.getAvatar(apiconfig.get("webinfoAvatar").toString(), author.getMail());
                                    }
                                    //avatar = baseFull.getAvatar(apiconfig.get("webinfoAvatar").toString(), author.getMail());
                                }
                            }

                            authorInfo.put("name",name);
                            authorInfo.put("avatar",avatar);
                            authorInfo.put("customize",author.getCustomize());
                            authorInfo.put("experience",author.getExperience());
                            //判断是否为VIP
                            authorInfo.put("isvip", 0);
                            Long date = System.currentTimeMillis();
                            String curTime = String.valueOf(date).substring(0, 10);
                            Integer viptime  = author.getVip();

                            if(viptime>Integer.parseInt(curTime)||viptime.equals(1)){
                                authorInfo.put("isvip", 1);
                            }
                            if(viptime.equals(1)){
                                //永久VIP
                                authorInfo.put("isvip", 2);
                            }
                        }else{
                            authorInfo.put("name","用户已注销");
                            authorInfo.put("avatar",apiconfig.get("webinfoAvatar").toString() + "null");
                        }


                        json.put("authorInfo",authorInfo);
                    }

                    String text = json.get("text").toString();
                    boolean status = text.contains("<!--markdown-->");
                    if(status){
                        json.put("markdown",1);
                    }else{
                        json.put("markdown",0);
                    }
                    List imgList = new ArrayList<>();
                    if(status){
                        //先把typecho的图片引用模式转为标准markdown
                        List oldImgList = baseFull.getImageSrc(text);
                        List codeList = baseFull.getImageCode(text);
                        for(int c = 0; c < codeList.size(); c++){
                            String codeimg = codeList.get(c).toString();
                            String urlimg = oldImgList.get(c).toString();
                            text=text.replace(codeimg,"![image"+c+"]("+urlimg+")");
                        }
                        imgList = baseFull.getImageSrcFromMarkdown(text);
                    }else{
                        imgList = baseFull.extractImageSrcFromHtml(text);
                    }

                    text = baseFull.toStrByChinese(text);

                    json.put("images",imgList);
                    json.put("text",text.length()>400 ? text.substring(0,400) : text);
                    json.put("category",metas);
                    json.put("tag",tags);
                    //获取文章挂载的商品
                    TypechoShop shop = new TypechoShop();
                    shop.setCid(Integer.parseInt(cid));
                    shop.setStatus(1);
                    List<TypechoShop> shopList = shopService.selectList(shop);
                    //去除付费内容显示
                    for (int s = 0; s < shopList.size(); s++) {
                        shopList.get(s).setValue(null);
                    }
                    json.put("shop",shopList);
                    json.remove("password");

                    jsonList.add(json);

                }
                redisHelp.delete(this.dataprefix+"_"+"followContents_"+uid+"_"+page+"_"+limit,redisTemplate);
                redisHelp.setList(this.dataprefix+"_"+"followContents_"+uid+"_"+page+"_"+limit,jsonList,5,redisTemplate);
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

    @RequestMapping(value = "/getDocx")
    @ResponseBody
    @LoginRequired(purview = "2")
    public void getDocx(HttpServletResponse response,
                        HttpServletRequest request,
                        @RequestParam(value = "token", required = false) String  token,
                        @RequestParam(value = "cid", required = false) String  cid) throws IOException {

        String htmlContent = "";
        Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
        if (uStatus == 0) {
            InputStream docxInputStream = null;
            try {
                docxInputStream = baseFull.convertHtmlToDocx("");
            } catch (InvalidFormatException e) {
                throw new RuntimeException(e);
            }
            response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            response.setHeader("Content-Disposition", "attachment; filename=\"converted.docx\"");
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = docxInputStream.read(buffer)) != -1) {
                response.getOutputStream().write(buffer, 0, bytesRead);
            }
            docxInputStream.close();
        }
        Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
        Integer logUid =Integer.parseInt(map.get("uid").toString());
        String group = map.get("group").toString();
        if (!group.equals("administrator")) {
            InputStream docxInputStream = null;
            try {
                docxInputStream = baseFull.convertHtmlToDocx("");
            } catch (InvalidFormatException e) {
                throw new RuntimeException(e);
            }
            response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            response.setHeader("Content-Disposition", "attachment; filename=\"converted.docx\"");
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = docxInputStream.read(buffer)) != -1) {
                response.getOutputStream().write(buffer, 0, bytesRead);
            }
            docxInputStream.close();
        }

        TypechoContents contents = service.selectByKey(cid);
        if(contents==null){
            InputStream docxInputStream = null;
            try {
                docxInputStream = baseFull.convertHtmlToDocx("");
            } catch (InvalidFormatException e) {
                throw new RuntimeException(e);
            }
            response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            response.setHeader("Content-Disposition", "attachment; filename=\"converted.docx\"");
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = docxInputStream.read(buffer)) != -1) {
                response.getOutputStream().write(buffer, 0, bytesRead);
            }
            docxInputStream.close();
        }
        htmlContent = contents.getText();
        boolean markdownStatus = htmlContent.contains("<!--markdown-->");
        if(markdownStatus){
            Parser parser = Parser.builder().build();
            Node document = parser.parse(htmlContent);
            HtmlRenderer renderer = HtmlRenderer.builder().build();
            htmlContent = renderer.render(document);
        }

        String fileName = contents.getTitle() + ".docx"; // 你的原始文件名
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString()); // 使用UTF-8进行编码

        String contentDispositionValue;

// 对于大多数现代浏览器，使用RFC 5987编码机制
        if (request.getHeader("User-Agent").toLowerCase().contains("firefox") ||
                request.getHeader("User-Agent").toLowerCase().contains("chrome") ||
                request.getHeader("User-Agent").toLowerCase().contains("safari")) {
            contentDispositionValue = "attachment; filename*=UTF-8''" + encodedFileName;
        } else {
            // 对于其他浏览器，如IE，使用较为传统的方式，可能需要根据实际情况调整
            contentDispositionValue = "attachment; filename=\"" + encodedFileName + "\"";
        }
        InputStream docxInputStream = null;
        try {
            docxInputStream = baseFull.convertHtmlToDocx(htmlContent);
        } catch (InvalidFormatException e) {
            throw new RuntimeException(e);
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader("Content-Disposition", contentDispositionValue);
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = docxInputStream.read(buffer)) != -1) {
            response.getOutputStream().write(buffer, 0, bytesRead);
        }
        docxInputStream.close();
    }

    /**
     * 用户自己检测文章是否违规
     * */
    @RequestMapping(value = "/userTextBlockStatus")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String userTextBlockStatus(@RequestParam(value = "text", required = false) String  text,
                                      @RequestParam(value = "token", required = false) String  token) {
        try {
            Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
            if(uStatus==0){
                return Result.getResultJson(0,"用户未登录或Token验证失败",null);
            }
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer uid =Integer.parseInt(map.get("uid").toString());
            Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
            //先扣钱
            //获取当前时间
            Long date = System.currentTimeMillis();
            String curTime = String.valueOf(date).substring(0,10);
            //先写死，后面再改
            Integer price = 5;
            TypechoUsers user = usersService.selectByKey(uid);
            Integer assets = user.getAssets();
            if(price>assets){
                return Result.getResultJson(0,"当前资产不足，请充值",null);
            }
            Integer newassets = assets - price;
            //更新用户资产与登录状态
            TypechoUsers updateUser = new TypechoUsers();
            updateUser.setUid(user.getUid());
            updateUser.setAssets(newassets);
            usersService.update(updateUser);
            TypechoPaylog paylog = new TypechoPaylog();
            paylog.setStatus(1);
            paylog.setCreated(Integer.parseInt(curTime));
            paylog.setUid(uid);
            paylog.setOutTradeNo(curTime+"buyshop");
            paylog.setTotalAmount("-"+price);
            paylog.setPaytype("buyshop");
            paylog.setSubject("购买平台内容安全检测");
            paylogService.insert(paylog);


            String textBlockTips = "您做得很好！当前内容无违规！";
            // 腾讯云内容违规检测
            if(apiconfig.get("cmsSwitch").toString().equals("1")|apiconfig.get("cmsSwitch").toString().equals("3")){
                try {
                    String plainText = baseFull.htmlToText(text);
                    // 拆分并检测内容
                    List<Map> allViolationResults = checkTextInChunks(plainText, 10000);

                    boolean isBlocked = false;
                    boolean needReview = false;
                    Set<String> allKeywords = new HashSet<>();

                    for (Map result : allViolationResults) {
                        String suggestion = result.get("Suggestion").toString();
                        if ("Block".equals(suggestion)) {
                            isBlocked = true;
                        } else if ("Review".equals(suggestion)) {
                            needReview = true;
                        }
                        Object keywordObj = result.get("Keywords");
                        if (keywordObj instanceof List) {
                            List<?> keywords = (List<?>) keywordObj;
                            for (Object word : keywords) {
                                if (word != null) {
                                    allKeywords.add(word.toString());
                                }
                            }
                        }
                    }

                    if (isBlocked && !allKeywords.isEmpty()) {
                        textBlockTips = "当前内容涉及违规，违规内容：" + String.join("，", allKeywords);
                    } else if (needReview && !allKeywords.isEmpty()) {
                        textBlockTips = "文章内容可能违规，可能违规内容：" + String.join("，", allKeywords);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    textBlockTips = "当前内容检测异常，请联系管理员检查接口。";
                }
            }
            return Result.getResultJson(1,textBlockTips,null);
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }
    }

    //适应文本内容检测，如果大于一万字则拆分
    private List<Map> checkTextInChunks(String text, int maxLength) {
        List<Map> results = new ArrayList<>();
        int length = text.length();
        for (int i = 0; i < length; i += maxLength) {
            int end = Math.min(length, i + maxLength);
            String chunk = text.substring(i, end);
            String encryptedText = baseFull.encrypt(chunk);
            Map result = securityService.textViolation(encryptedText);
            results.add(result);
        }
        return results;
    }
}
