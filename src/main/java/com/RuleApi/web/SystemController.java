package com.RuleApi.web;
import com.RuleApi.common.*;
import com.RuleApi.entity.TypechoAds;
import com.RuleApi.entity.TypechoApiconfig;
import com.RuleApi.service.PushService;
import com.RuleApi.service.TypechoAdsService;
import com.RuleApi.service.TypechoApiconfigService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.RuleApi.common.*;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



/**
 * 接口系统控制器，负责在线修改配置文件，在线重启RuleAPI接口
 * */
@Controller
@RequestMapping(value = "/system")
public class SystemController {

    ResultAll Result = new ResultAll();
    EditFile editFile = new EditFile();
    HttpClient HttpClient = new HttpClient();
    RedisHelp redisHelp =new RedisHelp();


    @Autowired
    private TypechoApiconfigService apiconfigService;

    @Autowired
    private TypechoAdsService adsService;

    @Autowired
    private PushService pushService;

    @Autowired
    private RedisTemplate redisTemplate;


    @Value("${webinfo.key}")
    private String key;

    @Value("${web.prefix}")
    private String dataprefix;
    /**
     * 密钥配置
     * */
    private String webinfoKey;


    /**
     * 缓存配置
     * */
    private String usertime;
    private String contentCache;
    private String contentInfoCache;
    private String CommentCache;
    private String userCache;
    /**
     * 邮箱配置
     * */
    private String mailHost;
    private String mailUsername;
    private String mailPassword;
    /**
     * Mysql配置
     * */
    private String dataUrl;
    private String dataUsername;
    private String dataPassword;
    private String dataPrefix;
    /**
     * Redis配置
     * */
    private String redisHost;
    private String redisPassword;
    private String redisPort;
    private String redisPrefix;

    /**
     * 验证Key
     * */
    @RequestMapping(value = "/isKey")
    @ResponseBody
    public String isKey(@RequestParam(value = "webkey", required = false) String  webkey) {
        try{
            if(!webkey.equals(this.key)){
                return Result.getResultJson(0,"请输入正确的访问key",null);
            }else{
                return Result.getResultJson(1,"验证成功！",null);
            }
        }catch (Exception e){
            return Result.getResultJson(0,"请求参数不正确",null);
        }

    }
    /**
     * 配置文件读取
     *
     * */
    @RequestMapping(value = "/getConfig")
    @ResponseBody
    public String getConfig(@RequestParam(value = "webkey", required = false) String  webkey) {
        if(webkey.length()<1){
            return Result.getResultJson(0,"请输入正确的访问key",null);
        }
        if(!webkey.equals(this.key)){
            return Result.getResultJson(0,"请输入正确的访问key",null);
        }
        ApplicationHome h = new ApplicationHome(getClass());
        File jarF = h.getSource();
        /* 读入配置文件 */
        String pathname = jarF.getParentFile().toString()+"/application.properties";
        String encoding = "UTF-8";
        File file = new File(pathname);
        Long filelength = file.length();
        byte[] filecontent = new byte[filelength.intValue()];
        try {
            FileInputStream in = new FileInputStream(file);
            in.read(filecontent);
            String text = new String(filecontent, encoding);
            text = editFile.unicodeToString(text);
            JSONObject response = new JSONObject();
            response.put("code" , 1);
            response.put("data" , text);
            response.put("msg"  , "");
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(0,"配置文件读取出错",null);
        }
    }
    /***
     * 网站基本信息配置
     */
    @RequestMapping(value = "/setupWebKey")
    @ResponseBody
    public String setupWebKey(@RequestParam(value = "webkey", required = false) String  webkey,@RequestParam(value = "params", required = false) String  params) {
        if(webkey.length()<1){
            return Result.getResultJson(0,"请输入正确的访问key",null);
        }
        if(!webkey.equals(this.key)){
            return Result.getResultJson(0,"请输入正确的访问key",null);
        }
        Map jsonToMap = new HashMap();
        try {
        //读取参数，开始写入
            if (StringUtils.isNotBlank(params)) {
                jsonToMap = JSONObject.parseObject(JSON.parseObject(params).toString());
                //新的配置

            }
            String new_webinfoKey = "";
            String webinfoKey = "webinfo.key=";
            //老的配置
            String old_webinfoKey =  webinfoKey+this.webinfoKey;
            //新的配置

            if(jsonToMap.get("webinfoKey")!=null){
                new_webinfoKey = webinfoKey+jsonToMap.get("webinfoKey").toString();
            }else{
                new_webinfoKey = webinfoKey;
            }
            editFile.replacTextContent(old_webinfoKey,new_webinfoKey);

            return Result.getResultJson(1,"修改成功，手动重启后生效",null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(1,"修改失败，请确认参数是否正确",null);
        }
    }
    /***
     * 缓存配置
     */
    @RequestMapping(value = "/setupCache")
    @ResponseBody
    public String setupCache(@RequestParam(value = "webkey", required = false) String  webkey,@RequestParam(value = "params", required = false) String  params) {
        if(webkey.length()<1){
            return Result.getResultJson(0,"请输入正确的访问key",null);
        }
        if(!webkey.equals(this.key)){
            return Result.getResultJson(0,"请输入正确的访问key",null);
        }
        Map jsonToMap = new HashMap();
        try {
            //读取参数，开始写入
            if (StringUtils.isNotBlank(params)) {
                jsonToMap = JSONObject.parseObject(JSON.parseObject(params).toString());
                //新的配置

            }
            String new_usertime = "";
            String new_contentCache = "";
            String new_contentInfoCache = "";
            String new_CommentCache = "";
            String new_userCache = "";

            String usertime = "webinfo.usertime=";
            String contentCache = "webinfo.contentCache=";
            String contentInfoCache = "webinfo.contentInfoCache=";
            String CommentCache = "webinfo.CommentCache=";
            String userCache = "webinfo.userCache=";
            //老的配置
            String old_usertime =  usertime+this.usertime;
            String old_contentCache =  contentCache+this.contentCache;
            String old_contentInfoCache =  contentInfoCache+this.contentInfoCache;
            String old_CommentCache =  CommentCache+this.CommentCache;
            String old_userCache =  userCache+this.userCache;
            //新的配置

            if(jsonToMap.get("usertime")!=null){
                new_usertime = usertime+jsonToMap.get("usertime").toString();
            }else{
                new_usertime = usertime;
            }
            editFile.replacTextContent(old_usertime,new_usertime);
            if(jsonToMap.get("contentCache")!=null){
                new_contentCache = contentCache+jsonToMap.get("contentCache").toString();
            }else{
                new_contentCache = contentCache;
            }
            editFile.replacTextContent(old_contentCache,new_contentCache);
            if(jsonToMap.get("contentInfoCache")!=null){
                new_contentInfoCache = contentInfoCache+jsonToMap.get("contentInfoCache").toString();
            }else{
                new_contentInfoCache = contentInfoCache;
            }
            editFile.replacTextContent(old_contentInfoCache,new_contentInfoCache);
            if(jsonToMap.get("CommentCache")!=null){
                new_CommentCache = CommentCache+jsonToMap.get("CommentCache").toString();
            }else{
                new_CommentCache = CommentCache;
            }
            editFile.replacTextContent(old_CommentCache,new_CommentCache);

            if(jsonToMap.get("userCache")!=null){
                new_userCache = userCache+jsonToMap.get("userCache").toString();
            }else{
                new_userCache = userCache;
            }
            editFile.replacTextContent(old_userCache,new_userCache);
            return Result.getResultJson(1,"修改成功，手动重启后生效",null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(1,"修改失败，请确认参数是否正确",null);
        }
    }
    /***
     * 数据库配置
     */
    @RequestMapping(value = "/setupMysql")
    @ResponseBody
    public String setupMysql(@RequestParam(value = "webkey", required = false) String  webkey,@RequestParam(value = "params", required = false) String  params) {
        if(webkey.length()<1){
            return Result.getResultJson(0,"请输入正确的访问key",null);
        }
        if(!webkey.equals(this.key)){
            return Result.getResultJson(0,"请输入正确的访问key",null);
        }
        Map jsonToMap = new HashMap();
        try {
            //读取参数，开始写入
            if (StringUtils.isNotBlank(params)) {
                jsonToMap = JSONObject.parseObject(JSON.parseObject(params).toString());
                //新的配置

            }
            String new_dataUrl = "";
            String new_dataUsername = "";
            String new_dataPassword = "";
            String new_dataPrefix = "";

            String dataUrl = "spring.datasource.url=";
            String dataUsername = "spring.datasource.username=";
            String dataPassword = "spring.datasource.password=";
            String dataPrefix = "mybatis.configuration.variables.prefix=";

            //老的配置
            String old_dataUrl = dataUrl+this.dataUrl;
            String old_dataUsername = dataUsername+this.dataUsername;
            String old_dataPassword =  dataPassword+this.dataPassword;
            String old_dataPrefix =  dataPrefix+this.dataPrefix;
            //新的配置
            if(jsonToMap.get("dataUrl")!=null){
                new_dataUrl = dataUrl+jsonToMap.get("dataUrl").toString();
            }else {
                new_dataUrl = dataUrl;
            }
            editFile.replacTextContent(old_dataUrl,new_dataUrl);
            if(jsonToMap.get("dataUsername")!=null){
                new_dataUsername = dataUsername+jsonToMap.get("dataUsername").toString();
            }else {
                new_dataUsername = dataUsername;
            }
            editFile.replacTextContent(old_dataUsername,new_dataUsername);
            if(jsonToMap.get("dataPassword")!=null){
                new_dataPassword = dataPassword+jsonToMap.get("dataPassword").toString();
            }else {
                new_dataPassword = dataPassword;
            }
            editFile.replacTextContent(old_dataPassword,new_dataPassword);
            if(jsonToMap.get("dataPrefix")!=null){
                new_dataPrefix = dataPrefix+jsonToMap.get("dataPrefix").toString();
            }else {
                new_dataPrefix= dataPrefix;
            }
            editFile.replacTextContent(old_dataPrefix,new_dataPrefix);
            return Result.getResultJson(1,"修改成功，手动重启后生效",null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(1,"修改失败，请确认参数是否正确",null);
        }
    }
    /***
     * Redis配置，包括数据前缀
     */
    @RequestMapping(value = "/setupRedis")
    @ResponseBody
    public String setupRedis(@RequestParam(value = "webkey", required = false) String  webkey,@RequestParam(value = "params", required = false) String  params) {
        if(webkey.length()<1){
            return Result.getResultJson(0,"请输入正确的访问key",null);
        }
        if(!webkey.equals(this.key)){
            return Result.getResultJson(0,"请输入正确的访问key",null);
        }
        Map jsonToMap = new HashMap();
        try {
            //读取参数，开始写入
            if (StringUtils.isNotBlank(params)) {
                jsonToMap = JSONObject.parseObject(JSON.parseObject(params).toString());
                //新的配置

            }
            String new_redisHost = "";
            String new_redisPassword = "";
            String new_redisPort = "";
            String new_redisPrefix = "";

            String redisHost = "spring.redis.host=";
            String redisPassword = "spring.redis.password=";
            String redisPort = "spring.redis.port=";
            String redisPrefix = "web.prefix=";

            //老的配置
            String old_redisHost = redisHost+this.redisHost;
            String old_redisPassword = redisPassword+this.redisPassword;
            String old_redisPort =  redisPort+this.redisPort;
            String old_redisPrefix =   redisPrefix+this.redisPrefix;

            //新的配置
            if(jsonToMap.get("redisHost")!=null){
                new_redisHost = redisHost+jsonToMap.get("redisHost").toString();
            }else {
                new_redisHost = redisHost;
            }
            editFile.replacTextContent(old_redisHost,new_redisHost);
            if(jsonToMap.get("redisPassword")!=null){
                new_redisPassword = redisPassword+jsonToMap.get("redisPassword").toString();
            }else {
                new_redisPassword = redisPassword;
            }
            editFile.replacTextContent(old_redisPassword,new_redisPassword);
            if(jsonToMap.get("redisPort")!=null){
                new_redisPort = redisPort+jsonToMap.get("redisPort").toString();
            }else {
                new_redisPort = redisPort;
            }
            editFile.replacTextContent(old_redisPort,new_redisPort);

            if(jsonToMap.get("redisPrefix")!=null){
                new_redisPrefix = redisPrefix+jsonToMap.get("redisPrefix").toString();
            }else {
                new_redisPrefix = redisPrefix;
            }
            editFile.replacTextContent(old_redisPrefix,new_redisPrefix);

            return Result.getResultJson(1,"修改成功，手动重启后生效",null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(1,"修改失败，请确认参数是否正确",null);
        }
    }



    /***
     * 邮件发送配置
     */
    @RequestMapping(value = "/setupEmail")
    @ResponseBody
    public String setupEmail(@RequestParam(value = "webkey", required = false) String  webkey,@RequestParam(value = "params", required = false) String  params) {
        if(webkey.length()<1){
            return Result.getResultJson(0,"请输入正确的访问key",null);
        }
        if(!webkey.equals(this.key)){
            return Result.getResultJson(0,"请输入正确的访问key",null);
        }
        Map jsonToMap = new HashMap();
        try {
            //读取参数，开始写入
            if (StringUtils.isNotBlank(params)) {
                jsonToMap = JSONObject.parseObject(JSON.parseObject(params).toString());
                //新的配置

            }
            String new_mailHost = "";
            String new_mailUsername = "";
            String new_mailPassword = "";

            String mailHost = "spring.mail.host=";
            String mailUsername = "spring.mail.username=";
            String mailPassword = "spring.mail.password=";

            //老的配置
            String old_mailHost = mailHost+this.mailHost;
            String old_mailUsername = mailUsername+this.mailUsername;
            String old_mailPassword =  mailPassword+this.mailPassword;
            //新的配置
            if(jsonToMap.get("mailHost")!=null){
                new_mailHost = mailHost+jsonToMap.get("mailHost").toString();
            }else {
                new_mailHost = mailHost;
            }
            editFile.replacTextContent(old_mailHost,new_mailHost);
            if(jsonToMap.get("mailUsername")!=null){
                new_mailUsername = mailUsername+jsonToMap.get("mailUsername").toString();
            }else {
                new_mailUsername = mailUsername;
            }
            editFile.replacTextContent(old_mailUsername,new_mailUsername);
            if(jsonToMap.get("mailPassword")!=null){
                new_mailPassword = mailPassword+jsonToMap.get("mailPassword").toString();
            }else {
                new_mailPassword = mailPassword;
            }
            editFile.replacTextContent(old_mailPassword,new_mailPassword);

            return Result.getResultJson(1,"修改成功，手动重启后生效",null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(1,"修改失败，请确认参数是否正确",null);
        }
    }

    /***
     * 编辑配置文件
     */
    @RequestMapping(value = "/setupConfig")
    @ResponseBody
    public String setupConfig(@RequestParam(value = "webkey", required = false) String  webkey,@RequestParam(value = "params", required = false) String  params) {
        try{
            ApplicationHome h = new ApplicationHome(getClass());
            File jarF = h.getSource();
            /* 读入配置文件 */
            String pathname = jarF.getParentFile().toString()+"/application.properties";
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(pathname)));

            writer.write(params);
            writer.close();
            return Result.getResultJson(1,"修改成功，手动重启后生效",null);
        }catch(Exception e){
            e.printStackTrace();
            return Result.getResultJson(1,"修改失败，请确认参数是否正确",null);
        }
    }
    /***
     * 获取所有配置信息
     */
    @RequestMapping(value = "/allConfig")
    @ResponseBody
    public String allConfig(@RequestParam(value = "webkey", required = false) String  webkey) {
        if(webkey.length()<1){
            return Result.getResultJson(0,"请输入正确的访问key",null);
        }
        if(!webkey.equals(this.key)){
            return Result.getResultJson(0,"请输入正确的访问key",null);
        }
        //网站基本信息
        String webinfoKey = "webinfo.key=";

        //邮箱配置信息
        String mailHost = "spring.mail.host=";
        String mailUsername = "spring.mail.username=";
        String mailPassword = "spring.mail.password=";
        //Mysql配置信息
        String dataUrl = "spring.datasource.url=";
        String dataUsername = "spring.datasource.username=";
        String dataPassword = "spring.datasource.password=";
        String dataPrefix =  "mybatis.configuration.variables.prefix=";
        //Redis配置
        String redisHost = "spring.redis.host=";
        String redisPassword = "spring.redis.password=";
        String redisPort = "spring.redis.port=";
        String redisPrefix = "web.prefix=";
        //缓存配置
        String usertime = "webinfo.usertime=";
        String contentCache = "webinfo.contentCache=";
        String contentInfoCache = "webinfo.contentInfoCache=";
        String CommentCache = "webinfo.CommentCache=";
        String userCache = "webinfo.userCache=";



        ApplicationHome h = new ApplicationHome(getClass());
        File jarF = h.getSource();


        /* 配置文件路径 */
        String path = jarF.getParentFile().toString()+"/application.properties";
        try {

            File filename = new File(path);
            InputStreamReader reader = new InputStreamReader(
                    new FileInputStream(filename)); // 建立一个输入流对象reader
            BufferedReader br = new BufferedReader(reader); // 建立一个对象，它把文件内容转成计算机能读懂的语言
            String line = "";
            line = br.readLine();
            while (line != null) {
                line = br.readLine(); // 一次读入一行数据
                if(line != null){
                    if (line.contains(webinfoKey)){
                        this.webinfoKey = line.replace(webinfoKey,"");
                    }

                    //邮箱信息
                    if (line.contains(mailHost)){
                        this.mailHost = line.replace(mailHost,"");
                    }
                    if (line.contains(mailUsername)){
                        this.mailUsername = line.replace(mailUsername,"");
                    }
                    if (line.contains(mailPassword)){
                        this.mailPassword = line.replace(mailPassword,"");
                    }
                    //Mysql信息
                    if (line.contains(dataUrl)){
                        this.dataUrl = line.replace(dataUrl,"");
                    }
                    if (line.contains(dataUsername)){
                        this.dataUsername = line.replace(dataUsername,"");
                    }
                    if (line.contains(dataPassword)){
                        this.dataPassword = line.replace(dataPassword,"");
                    }
                    if (line.contains(dataPrefix)){
                        this.dataPrefix = line.replace(dataPrefix,"");
                    }

                    //Redis信息
                    if (line.contains(redisHost)){
                        this.redisHost = line.replace(redisHost,"");
                    }
                    if (line.contains(redisPassword)){
                        this.redisPassword = line.replace(redisPassword,"");
                    }
                    if (line.contains(redisPort)){
                        this.redisPort = line.replace(redisPort,"");
                    }
                    if (line.contains(redisPrefix)){
                        this.redisPrefix = line.replace(redisPrefix,"");
                    }
                    //缓存信息
                    if (line.contains(usertime)){
                        this.usertime = line.replace(usertime,"");
                    }
                    if (line.contains(contentCache)){
                        this.contentCache = line.replace(contentCache,"");
                    }
                    if (line.contains(contentInfoCache)){
                        this.contentInfoCache = line.replace(contentInfoCache,"");
                    }
                    if (line.contains(CommentCache)){
                        this.CommentCache = line.replace(CommentCache,"");
                    }
                    if (line.contains(userCache)){
                        this.userCache = line.replace(userCache,"");
                    }



                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        JSONObject data = new JSONObject();
        data.put("webinfoKey",this.webinfoKey);

        //邮箱信息
        data.put("mailHost",this.mailHost);
        data.put("mailUsername",this.mailUsername);
        data.put("mailPassword",this.mailPassword);
        //mysql信息
        data.put("dataUrl",this.dataUrl);
        data.put("dataUsername",this.dataUsername);
        data.put("dataPassword",this.dataPassword);
        data.put("dataPrefix",this.dataPrefix);

        //Redis信息
        data.put("redisHost",this.redisHost);
        data.put("redisPassword",this.redisPassword);
        data.put("redisPort",this.redisPort);
        data.put("redisPrefix",this.redisPrefix);
        //缓存配置信息
        data.put("usertime",this.usertime);
        data.put("contentCache",this.contentCache);
        data.put("contentInfoCache",this.contentInfoCache);
        data.put("CommentCache",this.CommentCache);
        data.put("userCache",this.userCache);


        JSONObject response = new JSONObject();
        response.put("code" , 1);
        response.put("data" , data);
        response.put("msg"  , "");
        return response.toString();
    }
    /***
     * 获取数据库中的配置
     */
    @RequestMapping(value = "/getApiConfig")
    @ResponseBody
    public String getApiConfig(@RequestParam(value = "webkey", required = false) String  webkey) {
        if(webkey.length()<1){
            return Result.getResultJson(0,"请输入正确的访问key",null);
        }
        if(!webkey.equals(this.key)){
            return Result.getResultJson(0,"请输入正确的访问key",null);
        }
        TypechoApiconfig typechoApiconfig = apiconfigService.selectByKey(1);
        Map json = JSONObject.parseObject(JSONObject.toJSONString(typechoApiconfig), Map.class);
        JSONObject response = new JSONObject();
        response.put("code", 1);
        response.put("msg", "");
        response.put("data", json);
        return response.toString();
    }
    /***
     * 配置修改
     */
    @RequestMapping(value = "/apiConfigUpdate")
    @ResponseBody
    public String apiConfigUpdate(@RequestParam(value = "params", required = false) String  params,@RequestParam(value = "webkey", required = false) String  webkey) {
        TypechoApiconfig update = null;
        if(webkey.length()<1){
            return Result.getResultJson(0,"请输入正确的访问key",null);
        }
        if(!webkey.equals(this.key)){
            return Result.getResultJson(0,"请输入正确的访问key",null);
        }
        if (StringUtils.isNotBlank(params)) {
            JSONObject object = JSON.parseObject(params);
            update = object.toJavaObject(TypechoApiconfig.class);
        }
        update.setId(1);
        int rows = apiconfigService.update(update);

        JSONObject response = new JSONObject();
        response.put("code" , rows);
        response.put("msg"  , rows > 0 ? "修改成功，当前配置已生效！" : "修改失败");
        return response.toString();
    }
    /***
     * 获取新版本
     */
    @RequestMapping(value = "/apiNewVersion")
    @ResponseBody
    public String apiNewVersion() {
        String apiNewVersion = redisHelp.getRedis(this.dataprefix+"_"+"apiNewVersion",redisTemplate);
        HashMap data = new HashMap();
        if(apiNewVersion==null) {
            String requestUrl = "https://www.ruletree.club/ruleApiInfo.php";
            String res = HttpClient.doGet(requestUrl);
            if (res == null) {
                return Result.getResultJson(0, "获取服务端信息失败", null);
            }
            data = JSON.parseObject(res, HashMap.class);
            redisHelp.delete(this.dataprefix+"_"+"apiNewVersion",redisTemplate);
            redisHelp.setRedis(this.dataprefix+"_"+"apiNewVersion",res,600,redisTemplate);
        }else{
            data = JSON.parseObject(apiNewVersion, HashMap.class);
        }
        JSONObject response = new JSONObject();
        response.put("code" , 1);
        response.put("msg"  , "");
        response.put("data", data);
        return response.toString();
    }
    /***
     * 外部任务(广告)
     */
    @RequestMapping(value = "/taskAds")
    @ResponseBody
    public String taskAds(@RequestParam(value = "webkey", required = false) String  webkey) {
        if(webkey.length()<1){
            return Result.getResultJson(0,"请输入正确的访问key",null);
        }
        if(!webkey.equals(this.key)){
            return Result.getResultJson(0,"请输入正确的访问key",null);
        }
        TypechoAds ads = new TypechoAds();
        ads.setStatus(1);
        try {
            List<TypechoAds> adsList = adsService.selectList(ads);
            for (int i = 0; i < adsList.size(); i++) {
                TypechoAds info = adsList.get(i);
                Integer close = info.getClose();
                Long date = System.currentTimeMillis();
                String created = String.valueOf(date).substring(0,10);
                Integer curTime = Integer.parseInt(created);
                //如果当前时间大于到期时间，广告变为到期状态
                if(curTime>close){
                    info.setStatus(2);
                    adsService.update(info);
                }

            }
            return Result.getResultJson(1, "执行成功", null);
        }catch (Exception e){
            System.err.println(e);
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }

    }
    /***
     * CR云控信息
     */
    @RequestMapping(value = "/getCRCloud")
    @ResponseBody
    public String getCRCloud() {
        TypechoApiconfig typechoApiconfig = apiconfigService.selectByKey(1);
        JSONObject json = new JSONObject();
        json.put("cloudUid", typechoApiconfig.getCloudUid());
        json.put("cloudUrl", typechoApiconfig.getCloudUrl());
        JSONObject response = new JSONObject();
        response.put("code", 1);
        response.put("msg", "");
        response.put("data", json);
        return response.toString();
    }
    /***
     * 发送状态栏通知
     */
    @RequestMapping(value = "/sendPushMsg")
    @ResponseBody
    public String sendPushMsg(@RequestParam(value = "webkey", required = false) String  webkey,
                              @RequestParam(value = "cid", required = false) String  cid,
                              @RequestParam(value = "title", required = false) String  title,
                              @RequestParam(value = "content", required = false) String  content) {
        if(webkey.length()<1){
            return Result.getResultJson(0,"请输入正确的访问key",null);
        }
        if(!webkey.equals(this.key)){
            return Result.getResultJson(0,"请输入正确的访问key",null);
        }
        pushService.sendPushMsg(cid,title,content,"payload","打开评论区");
        return Result.getResultJson(1, "发送成功", null);
    }

}
