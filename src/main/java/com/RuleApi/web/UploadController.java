package com.RuleApi.web;

import com.RuleApi.common.*;
import com.RuleApi.entity.TypechoApiconfig;
import com.RuleApi.service.TypechoApiconfigService;
import com.RuleApi.service.UploadService;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.google.gson.Gson;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.region.Region;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLDecoder;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传控制器
 *
 * 提供本地和cos上传，之后的接口支持都加在这里
 * */

@Controller
@RequestMapping(value = "/upload")
public class UploadController {

    @Value("${web.prefix}")
    private String dataprefix;



    @Autowired
    private TypechoApiconfigService apiconfigService;

    @Autowired
    private UploadService uploadService;

    @Autowired
    private RedisTemplate redisTemplate;

    EditFile editFile = new EditFile();
    RedisHelp redisHelp =new RedisHelp();
    ResultAll Result = new ResultAll();
    baseFull baseFull = new baseFull();
    UserStatus UStatus = new UserStatus();

    /**
     * 通用上传接口
     * 除这个接口外，其它接口都是为了兼容旧版
     */
    @RequestMapping(value = "/full",method = RequestMethod.POST)
    @ResponseBody
    public Object full(@RequestParam(value = "file") MultipartFile file, @RequestParam(value = "token", required = false) String  token) throws IOException {
        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }

        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid =Integer.parseInt(map.get("uid").toString());
        if(file == null){
            return new UploadMsg(0,"文件为空",null);
        }
        String result = Result.getResultJson(0,"未开启任何上传通道，请检查配置",null);
        TypechoApiconfig apiconfig = UStatus.getConfig(this.dataprefix,apiconfigService,redisTemplate);
        if(apiconfig.getUploadType().equals("cos")){
            result = uploadService.cosUpload(file,this.dataprefix,apiconfig,uid);
        }
        if(apiconfig.getUploadType().equals("local")){
            result = uploadService.localUpload(file,this.dataprefix,apiconfig,uid);
        }
        if(apiconfig.getUploadType().equals("oss")){
            result = uploadService.ossUpload(file,this.dataprefix,apiconfig,uid);
        }
        if(apiconfig.getUploadType().equals("ftp")){
            result = uploadService.ftpUpload(file,this.dataprefix,apiconfig,uid);
        }
        if(apiconfig.getUploadType().equals("qiniu")){
            result = uploadService.qiniuUpload(file,this.dataprefix,apiconfig,uid);
        }
        return result;
    }

    /**
     * 上传cos
     * @return
     */
    @RequestMapping(value = "/cosUpload",method = RequestMethod.POST)
    @ResponseBody
    public Object cosUpload(@RequestParam(value = "file") MultipartFile file, @RequestParam(value = "token", required = false) String  token) throws IOException {
        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }

        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid =Integer.parseInt(map.get("uid").toString());
        if(file == null){
            return new UploadMsg(0,"文件为空",null);
        }
        TypechoApiconfig apiconfig = UStatus.getConfig(this.dataprefix,apiconfigService,redisTemplate);
        if(!apiconfig.getUploadType().equals("cos")){
            return Result.getResultJson(0,"该上传通道已关闭",null);
        }
        String result = uploadService.cosUpload(file,this.dataprefix,apiconfig,uid);
        return result;
    }

    private class UploadMsg {
        public int status;
        public String msg;
        public String path;

        public UploadMsg() {
            super();
        }

        public UploadMsg(int status, String msg, String path) {
            this.status = status;
            this.msg = msg;
            this.path = path;
        }
    }
    /**
     * 上传到本地
     * */
    @RequestMapping(value = "/localUpload",method = RequestMethod.POST)
    @ResponseBody
    public String localUpload(@RequestParam("file") MultipartFile file, @RequestParam(value = "token", required = false) String  token) throws IOException {
        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }
        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid =Integer.parseInt(map.get("uid").toString());
        TypechoApiconfig apiconfig = UStatus.getConfig(this.dataprefix,apiconfigService,redisTemplate);
        if(!apiconfig.getUploadType().equals("local")){
            return Result.getResultJson(0,"该上传通道已关闭",null);
        }
        String result = uploadService.localUpload(file,this.dataprefix,apiconfig,uid);
        return result;

    }

    /**
     * 上传到oss
     * */
    @RequestMapping(value = "/ossUpload",method = RequestMethod.POST)
    @ResponseBody
    public String ossUpload(@RequestParam("file") MultipartFile file, @RequestParam(value = "token", required = false) String  token) throws IOException {
        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }
        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid =Integer.parseInt(map.get("uid").toString());
        TypechoApiconfig apiconfig = UStatus.getConfig(this.dataprefix,apiconfigService,redisTemplate);
        if(!apiconfig.getUploadType().equals("oss")){
            return Result.getResultJson(0,"该上传通道已关闭",null);
        }
        String result = uploadService.ossUpload(file,this.dataprefix,apiconfig,uid);
        return result;

    }
    /**
     * 上传到七牛云
     * */
    @RequestMapping(value = "/qiniuUpload",method = RequestMethod.POST)
    @ResponseBody
    public String qiniuUpload(@RequestParam("file") MultipartFile file, @RequestParam(value = "token", required = false) String  token) throws IOException {
        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }
        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid =Integer.parseInt(map.get("uid").toString());
        TypechoApiconfig apiconfig = UStatus.getConfig(this.dataprefix,apiconfigService,redisTemplate);
        if(!apiconfig.getUploadType().equals("qiniu")){
            return Result.getResultJson(0,"该上传通道已关闭",null);
        }
        String result = uploadService.qiniuUpload(file,this.dataprefix,apiconfig,uid);
        return result;
    }
    /**
     * 上传到远程ftp
     * */
    @RequestMapping(value = "ftpUpload",method = RequestMethod.POST)
    @ResponseBody
    public String ftpUpload(@RequestParam(value = "file") MultipartFile file, @RequestParam(value = "token", required = false) String  token) {
        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }
        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid =Integer.parseInt(map.get("uid").toString());
        String oldFileName = file.getOriginalFilename();
        TypechoApiconfig apiconfig = UStatus.getConfig(this.dataprefix,apiconfigService,redisTemplate);
        if(!apiconfig.getUploadType().equals("ftp")){
            return Result.getResultJson(0,"该上传通道已关闭",null);
        }
        String result = uploadService.ftpUpload(file,this.dataprefix,apiconfig,uid);
        return result;


    }
}
