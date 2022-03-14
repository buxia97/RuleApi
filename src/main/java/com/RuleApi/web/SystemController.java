package com.RuleApi.web;
import com.RuleApi.common.EditFile;
import com.RuleApi.common.ResultAll;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.*;
import java.util.HashMap;
import java.util.Map;



/**
 * 接口系统控制器，负责在线修改配置文件，在线重启RuleAPI接口
 * */
@Controller
@RequestMapping(value = "/system")
public class SystemController {

    ResultAll Result = new ResultAll();
    EditFile editFile = new EditFile();


    /**
     * 读取配置文件，网站基本信息部分
     * */
    private String webinfoTitle;
    private String webinfoUrl;

    @Value("${webinfo.key}")
    private String key;

    private String webinfoKey;

    private String webinfoUsertime;
    private String webinfoUploadUrl;
    private String webinfoAvatar;
    private String pexelsKey;
    private String scale;

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
     * COS
     * */
    private String cosAccessKey;
    private String cosSecretKey;
    private String cosBucket;
    private String cosBucketName;
    private String cosPath;
    private String cosPrefix;
    /**
     * OSS
     * */
    private String aliyunEndpoint;
    private String aliyunAccessKeyId;
    private String aliyunAccessKeySecret;
    private String aliyunAucketName;
    private String aliyunUrlPrefix;
    private String aliyunFilePrefix;

    /**
     * FTP
     * */
    private String ftpHost;
    private String ftpPort;
    private String ftpUsername;
    private String ftpPassword;
    private String ftpBasePath;
    /**
     * alipay
     * */
    private String alipayAppId;
    private String alipayPrivateKey;
    private String alipayPublicKey;
    private String alipayNotifyUrl;


    /**
     * wxpay
     * */
    private String wxpayAppId;
    private String wxpayMchId;
    private String wxpayKey;
    private String wxpayNotifyUrl;
    /**
     * 验证Key
     * */
    @RequestMapping(value = "/isKey")
    @ResponseBody
    public String isKey(@RequestParam(value = "webkey", required = false) String  webkey) {
        if(!webkey.equals(this.key)){
            return Result.getResultJson(0,"请输入正确的访问key",null);
        }else{
            return Result.getResultJson(1,"验证成功！",null);
        }
    }
    /**
     * 配置文件读取
     *
     * */
    @RequestMapping(value = "/getConfig")
    @ResponseBody
    public String getConfig(@RequestParam(value = "webkey", required = false) String  webkey) {
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
            return Result.getResultJson(0,"请输入正确的访问key",null);
        }
    }
    /***
     * 网站基本信息配置
     */
    @RequestMapping(value = "/setupWebInfo")
    @ResponseBody
    public String setupWebInfo(@RequestParam(value = "webkey", required = false) String  webkey,@RequestParam(value = "params", required = false) String  params) {
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
            String new_webinfoTitle = "";
            String new_webinfoUrl = "";
            String new_webinfoKey = "";
            String new_webinfoUsertime = "";
            String new_webinfoUploadUrl = "";
            String new_webinfoAvatar = "";
            String new_pexelsKey = "";
            String new_scale = "";

            String webinfoTitle = "webinfo.title=";
            String webinfoUrl = "webinfo.url=";
            String webinfoKey = "webinfo.key=";
            String webinfoUsertime = "webinfo.usertime=";
            String webinfoUploadUrl = "webinfo.uploadUrl=";
            String webinfoAvatar = "webinfo.avatar=";
            String pexelsKey = "webinfo.pexelsKey=";
            String scale = "webinfo.scale=";


            //老的配置
            String old_webinfoTitle = webinfoTitle+this.webinfoTitle;
            String old_webinfoUrl = webinfoUrl+this.webinfoUrl;
            String old_webinfoKey =  webinfoKey+this.webinfoKey;
            String old_webinfoUsertime =  webinfoUsertime+this.webinfoUsertime;
            String old_webinfoUploadUrl =  webinfoUploadUrl+this.webinfoUploadUrl;
            String old_webinfoAvatar =  webinfoAvatar+this.webinfoAvatar;
            String old_pexelsKey =  pexelsKey+this.pexelsKey;
            String old_scale =  scale+this.scale;

            //新的配置
            if(jsonToMap.get("webinfoTitle")!=null){
                new_webinfoTitle = webinfoTitle+jsonToMap.get("webinfoTitle").toString();
            }else{
                new_webinfoTitle = webinfoTitle;
            }
            editFile.replacTextContent(old_webinfoTitle,new_webinfoTitle);
            if(jsonToMap.get("webinfoUrl")!=null){
                new_webinfoUrl = webinfoUrl+jsonToMap.get("webinfoUrl").toString();
            }else{
                new_webinfoUrl = webinfoUrl;
            }
            editFile.replacTextContent(new_webinfoUrl,old_webinfoUrl);
            if(jsonToMap.get("webinfoKey")!=null){
                new_webinfoKey = webinfoKey+jsonToMap.get("webinfoKey").toString();
            }else{
                new_webinfoKey = webinfoKey;
            }
            editFile.replacTextContent(old_webinfoKey,new_webinfoKey);
            if(jsonToMap.get("webinfoUsertime")!=null){
                new_webinfoUsertime = webinfoUsertime+jsonToMap.get("webinfoUsertime").toString();
            }else{
                new_webinfoUsertime = webinfoUsertime;
            }
            editFile.replacTextContent(old_webinfoUsertime,new_webinfoUsertime);
            if(jsonToMap.get("webinfoUploadUrl")!=null){
                new_webinfoUploadUrl = webinfoUploadUrl+jsonToMap.get("webinfoUploadUrl").toString();
            }else{
                new_webinfoUploadUrl = webinfoUploadUrl;
            }
            editFile.replacTextContent(old_webinfoUploadUrl,new_webinfoUploadUrl);
            if(jsonToMap.get("webinfoAvatar")!=null){
                new_webinfoAvatar = webinfoAvatar+jsonToMap.get("webinfoAvatar").toString();
            }else{
                new_webinfoAvatar = webinfoAvatar;
            }
            editFile.replacTextContent(old_webinfoAvatar,new_webinfoAvatar);
            if(jsonToMap.get("pexelsKey")!=null){
                new_pexelsKey = pexelsKey+jsonToMap.get("pexelsKey").toString();
            }else{
                new_pexelsKey = pexelsKey;
            }
            editFile.replacTextContent(old_pexelsKey,new_pexelsKey);

            if(jsonToMap.get("scale")!=null){
                new_scale = scale+jsonToMap.get("scale").toString();
            }else{
                new_scale = scale;
            }
            editFile.replacTextContent(old_scale,new_scale);
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
     * Redis配置，包括数据浅醉
     */
    @RequestMapping(value = "/setupRedis")
    @ResponseBody
    public String setupRedis(@RequestParam(value = "webkey", required = false) String  webkey,@RequestParam(value = "params", required = false) String  params) {
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
     * Cos配置
     */
    @RequestMapping(value = "/setupCos")
    @ResponseBody
    public String setupCos(@RequestParam(value = "webkey", required = false) String  webkey,@RequestParam(value = "params", required = false) String  params) {
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
            String new_cosAccessKey = "";
            String new_cosSecretKey = "";
            String new_cosBucket = "";
            String new_cosBucketName = "";
            String new_cosPath = "";
            String new_cosPrefix = "";

            String cosAccessKey = "spring.cos.accessKey=";
            String cosSecretKey = "spring.cos.secretKey=";
            String cosBucket = "spring.cos.bucket=";
            String cosBucketName = "spring.cos.bucketName=";
            String cosPath = "spring.cos.path=";
            String cosPrefix = "spring.cos.prefix=";

            //老的配置
            String old_cosAccessKey = cosAccessKey+this.cosAccessKey;
            String old_cosSecretKey = cosSecretKey+this.cosSecretKey;
            String old_cosBucket =  cosBucket+this.cosBucket;
            String old_cosBucketName =   cosBucketName+this.cosBucketName;
            String old_cosPath =   cosPath+this.cosPath;
            String old_cosPrefix =   cosPrefix+this.cosPrefix;

            //新的配置
            if(jsonToMap.get("cosAccessKey")!=null){
                new_cosAccessKey = cosAccessKey+jsonToMap.get("cosAccessKey").toString();
            }else {
                new_cosAccessKey = cosAccessKey;
            }
            editFile.replacTextContent(old_cosAccessKey,new_cosAccessKey);
            if(jsonToMap.get("cosSecretKey")!=null){
                new_cosSecretKey = cosSecretKey+jsonToMap.get("cosSecretKey").toString();
            }else {
                new_cosSecretKey = cosSecretKey;
            }
            editFile.replacTextContent(old_cosSecretKey,new_cosSecretKey);
            if(jsonToMap.get("cosBucket")!=null){
                new_cosBucket = cosBucket+jsonToMap.get("cosBucket").toString();
            }else {
                new_cosBucket = cosBucket;
            }
            editFile.replacTextContent(old_cosBucket,new_cosBucket);

            if(jsonToMap.get("cosBucketName")!=null){
                new_cosBucketName = cosBucketName+jsonToMap.get("cosBucketName").toString();
            }else {
                new_cosBucketName = cosBucketName;
            }
            editFile.replacTextContent(old_cosBucketName,new_cosBucketName);

            if(jsonToMap.get("cosPath")!=null){
                new_cosPath = cosPath+jsonToMap.get("cosPath").toString();
            }else {
                new_cosPath = cosPath;
            }
            editFile.replacTextContent(old_cosPath,new_cosPath);

            if(jsonToMap.get("cosPrefix")!=null){
                new_cosPrefix = cosPrefix+jsonToMap.get("cosPrefix").toString();
            }else {
                new_cosPrefix = cosPrefix;
            }
            editFile.replacTextContent(old_cosPrefix,new_cosPrefix);

            return Result.getResultJson(1,"修改成功，手动重启后生效",null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(1,"修改失败，请确认参数是否正确",null);
        }
    }
    /***
     * Oss配置
     */
    @RequestMapping(value = "/setupOss")
    @ResponseBody
    public String setupOss(@RequestParam(value = "webkey", required = false) String  webkey,@RequestParam(value = "params", required = false) String  params) {
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
            String new_aliyunEndpoint = "";
            String new_aliyunAccessKeyId = "";
            String new_aliyunAccessKeySecret = "";
            String new_aliyunAucketName = "";
            String new_aliyunUrlPrefix = "";
            String new_aliyunFilePrefix = "";

            String aliyunEndpoint = "spring.aliyun.endpoint=";
            String aliyunAccessKeyId = "spring.aliyun.accessKeyId=";
            String aliyunAccessKeySecret = "spring.aliyun.accessKeySecret=";
            String aliyunAucketName = "spring.aliyun.bucketName=";
            String aliyunUrlPrefix = "spring.aliyun.urlPrefix=";
            String aliyunFilePrefix = "oss.filePrefix=";

            //老的配置
            String old_aliyunEndpoint = aliyunEndpoint+this.aliyunEndpoint;
            String old_aliyunAccessKeyId = aliyunAccessKeyId+this.aliyunAccessKeyId;
            String old_aliyunAccessKeySecret = aliyunAccessKeySecret+this.aliyunAccessKeySecret;
            String old_aliyunAucketName = aliyunAucketName+this.aliyunAucketName;
            String old_aliyunUrlPrefix = aliyunUrlPrefix+this.aliyunUrlPrefix;
            String old_aliyunFilePrefix = aliyunFilePrefix+this.aliyunFilePrefix;

            //新的配置
            if(jsonToMap.get("aliyunEndpoint")!=null){
                new_aliyunEndpoint = aliyunEndpoint+jsonToMap.get("aliyunEndpoint").toString();
            }else {
                new_aliyunEndpoint = aliyunEndpoint;
            }
            editFile.replacTextContent(old_aliyunEndpoint,new_aliyunEndpoint);
            if(jsonToMap.get("aliyunAccessKeyId")!=null){
                new_aliyunAccessKeyId = aliyunAccessKeyId+jsonToMap.get("aliyunAccessKeyId").toString();
            }else {
                new_aliyunAccessKeyId = aliyunAccessKeyId;
            }
            editFile.replacTextContent(old_aliyunAccessKeyId,new_aliyunAccessKeyId);
            if(jsonToMap.get("aliyunAccessKeySecret")!=null){
                new_aliyunAccessKeySecret = aliyunAccessKeySecret+jsonToMap.get("aliyunAccessKeySecret").toString();
            }else {
                new_aliyunAccessKeySecret = aliyunAccessKeySecret;
            }
            editFile.replacTextContent(old_aliyunAccessKeySecret,new_aliyunAccessKeySecret);

            if(jsonToMap.get("aliyunAucketName")!=null){
                new_aliyunAucketName = aliyunAucketName+jsonToMap.get("aliyunAucketName").toString();
            }else {
                new_aliyunAucketName = aliyunAucketName;
            }
            editFile.replacTextContent(old_aliyunAucketName,new_aliyunAucketName);

            if(jsonToMap.get("aliyunUrlPrefix")!=null){
                new_aliyunUrlPrefix = aliyunUrlPrefix+jsonToMap.get("aliyunUrlPrefix").toString();
            }else {
                new_aliyunUrlPrefix = aliyunUrlPrefix;
            }
            editFile.replacTextContent(old_aliyunUrlPrefix,new_aliyunUrlPrefix);

            if(jsonToMap.get("aliyunFilePrefix")!=null){
                new_aliyunFilePrefix = aliyunFilePrefix+jsonToMap.get("aliyunFilePrefix").toString();
            }else {
                new_aliyunFilePrefix = aliyunFilePrefix;
            }
            editFile.replacTextContent(old_aliyunFilePrefix,new_aliyunFilePrefix);

            return Result.getResultJson(1,"修改成功，手动重启后生效",null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(1,"修改失败，请确认参数是否正确",null);
        }
    }
    /***
     * Oss配置
     */
    @RequestMapping(value = "/setupFtp")
    @ResponseBody
    public String setupFtp(@RequestParam(value = "webkey", required = false) String  webkey,@RequestParam(value = "params", required = false) String  params) {
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
            String new_ftpHost = "";
            String new_ftpPort = "";
            String new_ftpUsername = "";
            String new_ftpPassword = "";
            String new_ftpBasePath = "";

            String ftpHost="spring.ftp.host=";
            String ftpPort="spring.ftp.port=";
            String ftpUsername="spring.ftp.username=";
            String ftpPassword="spring.ftp.password=";
            String ftpBasePath="spring.ftp.basePath=";

            //老的配置
            String old_ftpHost = ftpHost+this.ftpHost;
            String old_ftpPort = ftpPort+this.ftpPort;
            String old_ftpUsername = ftpUsername+this.ftpUsername;
            String old_ftpPassword = ftpPassword+this.ftpPassword;
            String old_ftpBasePath = ftpBasePath+this.ftpBasePath;
            //新的配置
            if(jsonToMap.get("ftpHost")!=null){
                new_ftpHost = ftpHost+jsonToMap.get("ftpHost").toString();
            }else {
                new_ftpHost = ftpHost;
            }
            editFile.replacTextContent(old_ftpHost,new_ftpHost);
            if(jsonToMap.get("ftpPort")!=null){
                new_ftpPort = ftpPort+jsonToMap.get("ftpPort").toString();
            }else {
                new_ftpPort = ftpPort;
            }
            editFile.replacTextContent(old_ftpPort,new_ftpPort);
            if(jsonToMap.get("ftpUsername")!=null){
                new_ftpUsername = ftpUsername+jsonToMap.get("ftpUsername").toString();
            }else {
                new_ftpUsername = ftpUsername;
            }
            editFile.replacTextContent(old_ftpUsername,new_ftpUsername);

            if(jsonToMap.get("ftpPassword")!=null){
                new_ftpPassword = ftpPassword+jsonToMap.get("ftpPassword").toString();
            }else {
                new_ftpPassword = ftpPassword;
            }
            editFile.replacTextContent(old_ftpPassword,new_ftpPassword);

            if(jsonToMap.get("ftpBasePath")!=null){
                new_ftpBasePath = ftpBasePath+jsonToMap.get("ftpBasePath").toString();
            }else {
                new_ftpBasePath = ftpBasePath;
            }
            editFile.replacTextContent(old_ftpBasePath,new_ftpBasePath);


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
     * 支付宝当面付配置
     */
    @RequestMapping(value = "/setupAlipay")
    @ResponseBody
    public String setupAlipay(@RequestParam(value = "webkey", required = false) String  webkey,@RequestParam(value = "params", required = false) String  params) {
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
            String new_alipayAppId = "";
            String new_alipayPrivateKey = "";
            String new_alipayPublicKey = "";
            String new_alipayNotifyUrl = "";

            String alipayAppId = "fastboot.pay.alipay.app-id=";
            String alipayPrivateKey = "fastboot.pay.alipay.private-key=";
            String alipayPublicKey = "fastboot.pay.alipay.alipay-public-key=";
            String alipayNotifyUrl = "fastboot.pay.alipay.notify-url=";

            //老的配置
            String old_alipayAppId = alipayAppId+this.alipayAppId;
            String old_alipayPrivateKey = alipayPrivateKey+this.alipayPrivateKey;
            String old_alipayPublicKey = alipayPublicKey+this.alipayPublicKey;
            String old_alipayNotifyUrl = alipayNotifyUrl+this.alipayNotifyUrl;

            //新的配置
            if(jsonToMap.get("alipayAppId")!=null){
                new_alipayAppId = alipayAppId+jsonToMap.get("alipayAppId").toString();
            }else {
                new_alipayAppId = alipayAppId;
            }
            editFile.replacTextContent(old_alipayAppId,new_alipayAppId);
            if(jsonToMap.get("alipayPrivateKey")!=null){
                new_alipayPrivateKey = alipayPrivateKey+jsonToMap.get("alipayPrivateKey").toString();
            }else {
                new_alipayPrivateKey = alipayPrivateKey;
            }
            editFile.replacTextContent(old_alipayPrivateKey,new_alipayPrivateKey);
            if(jsonToMap.get("alipayPublicKey")!=null){
                new_alipayPublicKey = alipayPublicKey+jsonToMap.get("alipayPublicKey").toString();
            }else {
                new_alipayPublicKey = alipayPublicKey;
            }
            editFile.replacTextContent(old_alipayPublicKey,new_alipayPublicKey);

            if(jsonToMap.get("alipayNotifyUrl")!=null){
                new_alipayNotifyUrl = alipayNotifyUrl+jsonToMap.get("alipayNotifyUrl").toString();
            }else {
                new_alipayNotifyUrl = alipayNotifyUrl;
            }
            editFile.replacTextContent(old_alipayNotifyUrl,new_alipayNotifyUrl);

            return Result.getResultJson(1,"修改成功，手动重启后生效",null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(1,"修改失败，请确认参数是否正确",null);
        }
    }
    /***
     * 支付宝当面付配置
     */
    @RequestMapping(value = "/setupWxpay")
    @ResponseBody
    public String setupWxpay(@RequestParam(value = "webkey", required = false) String  webkey,@RequestParam(value = "params", required = false) String  params) {
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
            String new_wxpayAppId = "";
            String new_wxpayMchId = "";
            String new_wxpayKey = "";
            String new_wxpayNotifyUrl = "";

            String wxpayAppId = "gzh.appid=";
            String wxpayMchId = "wxPay.mchId=";
            String wxpayKey = "wxPay.key=";
            String wxpayNotifyUrl = "wxPay.notifyUrl=";

            //老的配置
            String old_wxpayAppId = wxpayAppId+this.wxpayAppId;
            String old_wxpayMchId = wxpayMchId+this.wxpayMchId;
            String old_wxpayKey = wxpayKey+this.wxpayKey;
            String old_wxpayNotifyUrl = wxpayNotifyUrl+this.wxpayNotifyUrl;

            //新的配置
            if(jsonToMap.get("wxpayAppId")!=null){
                new_wxpayAppId = wxpayAppId+jsonToMap.get("wxpayAppId").toString();
            }else {
                new_wxpayAppId = wxpayAppId;
            }
            editFile.replacTextContent(old_wxpayAppId,new_wxpayAppId);
            if(jsonToMap.get("wxpayMchId")!=null){
                new_wxpayMchId = wxpayMchId+jsonToMap.get("wxpayMchId").toString();
            }else {
                new_wxpayMchId = wxpayMchId;
            }
            editFile.replacTextContent(old_wxpayMchId,new_wxpayMchId);
            if(jsonToMap.get("wxpayKey")!=null){
                new_wxpayKey = wxpayKey+jsonToMap.get("wxpayKey").toString();
            }else {
                new_wxpayKey = wxpayKey;
            }
            editFile.replacTextContent(old_wxpayKey,new_wxpayKey);

            if(jsonToMap.get("wxpayNotifyUrl")!=null){
                new_wxpayNotifyUrl = wxpayNotifyUrl+jsonToMap.get("wxpayNotifyUrl").toString();
            }else {
                new_wxpayNotifyUrl = wxpayNotifyUrl;
            }
            editFile.replacTextContent(old_wxpayNotifyUrl,new_wxpayNotifyUrl);

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
        if(!webkey.equals(this.key)){
            return Result.getResultJson(0,"请输入正确的访问key",null);
        }
        //网站基本信息
        String webinfoTitle = "webinfo.title=";
        String webinfoUrl = "webinfo.url=";
        String webinfoKey = "webinfo.key=";
        String webinfoUsertime = "webinfo.usertime=";
        String webinfoUploadUrl = "webinfo.uploadUrl=";
        String webinfoAvatar = "webinfo.avatar=";
        String pexelsKey = "webinfo.pexelsKey=";
        String scale = "webinfo.scale=";
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
        //COS
        String cosAccessKey = "spring.cos.accessKey=";
        String cosSecretKey = "spring.cos.secretKey=";
        String cosBucket = "spring.cos.bucket=";
        String cosBucketName = "spring.cos.bucketName=";
        String cosPath = "spring.cos.path=";
        String cosPrefix = "spring.cos.prefix=";
        //OSS
        String aliyunEndpoint = "spring.aliyun.endpoint=";
        String aliyunAccessKeyId = "spring.aliyun.accessKeyId=";
        String aliyunAccessKeySecret = "spring.aliyun.accessKeySecret=";
        String aliyunAucketName = "spring.aliyun.bucketName=";
        String aliyunUrlPrefix = "spring.aliyun.urlPrefix=";
        String aliyunFilePrefix = "oss.filePrefix=";
        //ftp
        String ftpHost="spring.ftp.host=";
        String ftpPort="spring.ftp.port=";
        String ftpUsername="spring.ftp.username=";
        String ftpPassword="spring.ftp.password=";
        String ftpBasePath="spring.ftp.basePath=";

        //alipay
        String alipayAppId = "fastboot.pay.alipay.app-id=";
        String alipayPrivateKey = "fastboot.pay.alipay.private-key=";
        String alipayPublicKey = "fastboot.pay.alipay.alipay-public-key=";
        String alipayNotifyUrl = "fastboot.pay.alipay.notify-url=";
        //weixinPay
        String wxpayAppId = "gzh.appid=";
        String wxpayMchId = "wxPay.mchId=";
        String wxpayKey = "wxPay.key=";
        String wxpayNotifyUrl = "wxPay.notifyUrl=";



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
                    if (line.contains(webinfoTitle)){
                        String old_webinfoTitle = line.replace(webinfoTitle,"");
                        byte[] byteName=old_webinfoTitle.getBytes("UTF-8");
                        String str=new String(byteName,"ISO-8859-1");
                        byte[] byteName2=str.getBytes("ISO-8859-1");
                        String newStr=new String(byteName2,"UTF-8");
                        this.webinfoTitle = newStr;
                    }
                    if (line.contains(webinfoUrl)){
                        this.webinfoUrl = line.replace(webinfoUrl,"");
                    }
                    if (line.contains(webinfoKey)){
                        this.webinfoKey = line.replace(webinfoKey,"");
                    }
                    if (line.contains(webinfoUsertime)){
                        this.webinfoUsertime = line.replace(webinfoUsertime,"");
                    }
                    if (line.contains(webinfoUploadUrl)){
                        this.webinfoUploadUrl = line.replace(webinfoUploadUrl,"");
                    }
                    if (line.contains(webinfoAvatar)){
                        this.webinfoAvatar = line.replace(webinfoAvatar,"");
                    }
                    if (line.contains(pexelsKey)){
                        this.pexelsKey = line.replace(pexelsKey,"");
                    }
                    if (line.contains(scale)){
                        this.scale = line.replace(scale,"");
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
                    //COS
                    if (line.contains(cosAccessKey)){
                        this.cosAccessKey = line.replace(cosAccessKey,"");
                    }
                    if (line.contains(cosSecretKey)){
                        this.cosSecretKey = line.replace(cosSecretKey,"");
                    }
                    if (line.contains(cosBucket)){
                        this.cosBucket = line.replace(cosBucket,"");
                    }
                    if (line.contains(cosBucketName)){
                        this.cosBucketName = line.replace(cosBucketName,"");
                    }
                    if (line.contains(cosPath)){
                        this.cosPath = line.replace(cosPath,"");
                    }
                    if (line.contains(cosPrefix)){
                        this.cosPrefix = line.replace(cosPrefix,"");
                    }
                    //OSS
                    if (line.contains(aliyunEndpoint)){
                        this.aliyunEndpoint = line.replace(aliyunEndpoint,"");
                    }
                    if (line.contains(aliyunAccessKeyId)){
                        this.aliyunAccessKeyId = line.replace(aliyunAccessKeyId,"");
                    }
                    if (line.contains(aliyunAccessKeySecret)){
                        this.aliyunAccessKeySecret = line.replace(aliyunAccessKeySecret,"");
                    }
                    if (line.contains(aliyunAucketName)){
                        this.aliyunAucketName = line.replace(aliyunAucketName,"");
                    }
                    if (line.contains(aliyunUrlPrefix)){
                        this.aliyunUrlPrefix = line.replace(aliyunUrlPrefix,"");
                    }
                    if (line.contains(aliyunFilePrefix)){
                        this.aliyunFilePrefix = line.replace(aliyunFilePrefix,"");
                    }
                    //ftp

                    if (line.contains(ftpHost)){
                        this.ftpHost = line.replace(ftpHost,"");
                    }
                    if (line.contains(ftpPort)){
                        this.ftpPort = line.replace(ftpPort,"");
                    }
                    if (line.contains(ftpUsername)){
                        this.ftpUsername = line.replace(ftpUsername,"");
                    }
                    if (line.contains(ftpPassword)){
                        this.ftpPassword = line.replace(ftpPassword,"");
                    }
                    if (line.contains(ftpBasePath)){
                        this.ftpBasePath = line.replace(ftpBasePath,"");
                    }
                    //alipay
                    if (line.contains(alipayAppId)){
                        this.alipayAppId = line.replace(alipayAppId,"");
                    }
                    if (line.contains(alipayPrivateKey)){
                        this.alipayPrivateKey = line.replace(alipayPrivateKey,"");
                    }
                    if (line.contains(alipayPublicKey)){
                        this.alipayPublicKey = line.replace(alipayPublicKey,"");
                    }
                    if (line.contains(alipayNotifyUrl)){
                        this.alipayNotifyUrl = line.replace(alipayNotifyUrl,"");
                    }

                    //wxpay
                    if (line.contains(wxpayAppId)){
                        this.wxpayAppId = line.replace(wxpayAppId,"");
                    }
                    if (line.contains(wxpayMchId)){
                        this.wxpayMchId = line.replace(wxpayMchId,"");
                    }
                    if (line.contains(wxpayKey)){
                        this.wxpayKey = line.replace(wxpayKey,"");
                    }
                    if (line.contains(wxpayNotifyUrl)){
                        this.wxpayNotifyUrl = line.replace(wxpayNotifyUrl,"");
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        JSONObject data = new JSONObject();
        data.put("webinfoTitle",this.webinfoTitle);
        data.put("webinfoUrl",this.webinfoUrl);
        data.put("webinfoKey",this.webinfoKey);
        data.put("webinfoUsertime",this.webinfoUsertime);
        data.put("webinfoUploadUrl",this.webinfoUploadUrl);
        data.put("webinfoAvatar",this.webinfoAvatar);
        data.put("pexelsKey",this.pexelsKey);
        data.put("scale",this.scale);

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
        //COS
        data.put("cosAccessKey",this.cosAccessKey);
        data.put("cosSecretKey",this.cosSecretKey);
        data.put("cosBucket",this.cosBucket);
        data.put("cosBucketName",this.cosBucketName);
        data.put("cosPath",this.cosPath);
        data.put("cosPrefix",this.cosPrefix);
        //OSS
        data.put("aliyunEndpoint",this.aliyunEndpoint);
        data.put("aliyunAccessKeyId",this.aliyunAccessKeyId);
        data.put("aliyunAccessKeySecret",this.aliyunAccessKeySecret);
        data.put("aliyunAucketName",this.aliyunAucketName);
        data.put("aliyunUrlPrefix",this.aliyunUrlPrefix);
        data.put("aliyunFilePrefix",this.aliyunFilePrefix);

        //ftp
        data.put("ftpHost",this.ftpHost);
        data.put("ftpPort",this.ftpPort);
        data.put("ftpUsername",this.ftpUsername);
        data.put("ftpPassword",this.ftpPassword);
        data.put("ftpBasePath",this.ftpBasePath);
        //支付宝当面付
        data.put("alipayAppId",this.alipayAppId);
        data.put("alipayPrivateKey",this.alipayPrivateKey);
        data.put("alipayPublicKey",this.alipayPublicKey);
        data.put("alipayNotifyUrl",this.alipayNotifyUrl);
        //微信支付
        data.put("wxpayAppId",this.wxpayAppId);
        data.put("wxpayMchId",this.wxpayMchId);
        data.put("wxpayKey",this.wxpayKey);
        data.put("wxpayNotifyUrl",this.wxpayNotifyUrl);


        JSONObject response = new JSONObject();
        response.put("code" , 1);
        response.put("data" , data);
        response.put("msg"  , "");
        return response.toString();
    }
}
