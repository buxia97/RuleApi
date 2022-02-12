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
    private String webinfoKey;

    private String webinfoUsertime;
    private String webinfoUploadUrl;
    private String webinfoAvatar;
    /**
     * 配置文件读取
     *
     * */
    @RequestMapping(value = "/getConfig")
    @ResponseBody
    public String getConfig(@RequestParam(value = "webkey", required = false) String  webkey) {
        if(!webkey.equals(this.webinfoKey)){
            return Result.getResultJson(0,"请输入正确的访问key",null);
        }
        ApplicationHome h = new ApplicationHome(getClass());
        File jarF = h.getSource();
        System.out.println(jarF.getParentFile().toString());
        /* 读入配置文件 */
        String pathname = jarF.getParentFile().toString()+"/application.properties";
        String encoding = "UTF-8";
        File file = new File(pathname);
        Long filelength = file.length();
        byte[] filecontent = new byte[filelength.intValue()];
        try {
            FileInputStream in = new FileInputStream(file);
            in.read(filecontent);
            return Result.getResultJson(1,new String(filecontent, encoding),null);
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
        if(!webkey.equals(this.webinfoKey)){
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

            String webinfoTitle = "webinfo.title=";
            String webinfoUrl = "webinfo.url=";
            String webinfoKey = "webinfo.key=";
            String webinfoUsertime = "webinfo.usertime=";
            String webinfoUploadUrl = "webinfo.uploadUrl=";
            String webinfoAvatar = "webinfo.avatar=";



            //老的配置
            String old_webinfoTitle = webinfoTitle+this.webinfoTitle;
            String old_webinfoUrl = webinfoUrl+this.webinfoUrl;
            String old_webinfoKey =  webinfoKey+this.webinfoKey;
            String old_webinfoUsertime =  webinfoUsertime+this.webinfoUsertime;
            String old_webinfoUploadUrl =  webinfoUploadUrl+this.webinfoUploadUrl;
            String old_webinfoAvatar =  webinfoAvatar+this.webinfoAvatar;
            //新的配置
            if(jsonToMap.get("webinfoTitle")!=null){
                new_webinfoTitle = webinfoTitle+jsonToMap.get("webinfoTitle").toString();
                editFile.replacTextContent(old_webinfoTitle,new_webinfoTitle);
            }
            if(jsonToMap.get("webinfoUrl")!=null){
                new_webinfoUrl = webinfoUrl+jsonToMap.get("webinfoUrl").toString();
                editFile.replacTextContent(new_webinfoUrl,old_webinfoUrl);
            }
            if(jsonToMap.get("webinfoKey")!=null){
                new_webinfoKey = webinfoKey+jsonToMap.get("webinfoKey").toString();
                editFile.replacTextContent(old_webinfoKey,new_webinfoKey);
            }
            if(jsonToMap.get("webinfoUsertime")!=null){
                new_webinfoUsertime = webinfoUsertime+jsonToMap.get("webinfoUsertime").toString();
                editFile.replacTextContent(old_webinfoUsertime,new_webinfoUsertime);
            }
            if(jsonToMap.get("webinfoUploadUrl")!=null){
                new_webinfoUploadUrl = webinfoUploadUrl+jsonToMap.get("webinfoUploadUrl").toString();
                editFile.replacTextContent(old_webinfoUploadUrl,new_webinfoUploadUrl);
            }
            if(jsonToMap.get("webinfoAvatar")!=null){
                new_webinfoAvatar = webinfoAvatar+jsonToMap.get("webinfoAvatar").toString();
                editFile.replacTextContent(old_webinfoAvatar,new_webinfoAvatar);
            }
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
        return "0";
    }
    /***
     * Redis配置，包括数据浅醉
     */
    @RequestMapping(value = "/setupRedis")
    @ResponseBody
    public String setupRedis(@RequestParam(value = "webkey", required = false) String  webkey,@RequestParam(value = "params", required = false) String  params) {
        return "0";
    }
    /***
     * Cos配置
     */
    @RequestMapping(value = "/setupCos")
    @ResponseBody
    public String setupCos(@RequestParam(value = "webkey", required = false) String  webkey,@RequestParam(value = "params", required = false) String  params) {
        return "0";
    }
    /***
     * Oss配置
     */
    @RequestMapping(value = "/setupOss")
    @ResponseBody
    public String setupOss(@RequestParam(value = "webkey", required = false) String  webkey,@RequestParam(value = "params", required = false) String  params) {
        return "0";
    }
    /***
     * 图库配置
     */
    @RequestMapping(value = "/setupPexelsKey")
    @ResponseBody
    public String setupPexelsKey(@RequestParam(value = "webkey", required = false) String  webkey,@RequestParam(value = "params", required = false) String  params) {
        return "0";
    }
    /***
     * 邮件发送配置
     */
    @RequestMapping(value = "/setupEmail")
    @ResponseBody
    public String setupEmail(@RequestParam(value = "webkey", required = false) String  webkey,@RequestParam(value = "params", required = false) String  params) {
        return "0";
    }
    /***
     * 支付宝当面付配置
     */
    @RequestMapping(value = "/setupAlipay")
    @ResponseBody
    public String setupAlipay(@RequestParam(value = "webkey", required = false) String  webkey,@RequestParam(value = "params", required = false) String  params) {
        return "0";
    }
    /***
     * 编辑配置文件
     */
    @RequestMapping(value = "/application.properties")
    @ResponseBody
    public String setupConfig(@RequestParam(value = "webkey", required = false) String  webkey,@RequestParam(value = "params", required = false) String  params) {
        return "0";
    }
    /***
     * 获取所有配置信息
     */
    @RequestMapping(value = "/allConfig")
    @ResponseBody
    public String allConfig(@RequestParam(value = "webkey", required = false) String  webkey) {
        if(!webkey.equals(this.webinfoKey)){
            return Result.getResultJson(0,"请输入正确的访问key",null);
        }
        //网站基本信息
        String webinfoTitle = "webinfo.title=";
        String webinfoUrl = "webinfo.url=";
        String webinfoKey = "webinfo.key=";
        String webinfoUsertime = "webinfo.usertime=";
        String webinfoUploadUrl = "webinfo.uploadUrl=";
        String webinfoAvatar = "webinfo.avatar=";
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
                        this.webinfoTitle = line.replace(webinfoTitle,"");
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

        JSONObject response = new JSONObject();
        response.put("code" , 1);
        response.put("data" , data);
        response.put("msg"  , "");
        return response.toString();
    }
}
