package com.RuleApi.web;

import com.RuleApi.annotation.LoginRequired;
import com.RuleApi.common.*;
import com.RuleApi.entity.TypechoFiles;

import com.RuleApi.service.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;

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
    private AllconfigService allconfigService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private UsersService usersService;

    @Autowired
    private UploadService uploadService;

    @Autowired
    private FilesService filesService;

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
    @LoginRequired(purview = "0")
    public Object full(@RequestParam(value = "file") MultipartFile file, @RequestParam(value = "token", required = false) String  token) throws IOException {

        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
        Integer uid =Integer.parseInt(map.get("uid").toString());
        //图片压缩字段初始化
        Integer isvip =Integer.parseInt(map.get("isvip").toString());
        Integer imageCompression = 0;
        if(apiconfig.get("imageCompression")!=null){
            imageCompression  = Integer.parseInt(apiconfig.get("imageCompression").toString());
        }
        Integer imageCompressionLv = 1080;
        if(apiconfig.get("imageCompression")!=null){
            imageCompressionLv  = Integer.parseInt(apiconfig.get("imageCompressionLv").toString());
        }
        Integer isCompression = 0;
        if(imageCompression.equals(1)){
            if(isvip.equals(0)){
                isCompression = 1;
            }
        }
        if(imageCompression.equals(2)){
            isCompression = 1;

        }
        //图片压缩字段初始化结束
        if(file == null){
            return new UploadMsg(0,"文件为空",null);
        }



        TypechoFiles files = new TypechoFiles();
        String md5 = getFileMd5(file);
        files.setMd5(md5);
        List<TypechoFiles> list = filesService.selectList(files);
        if(list.size()>0){
            Map<String,String> info =new HashMap<String, String>();
            info.put("url",list.get(0).getLinks());
            return Result.getResultJson(1,"上传成功",info);
        }


        String result = Result.getResultJson(0,"未开启任何上传通道，请检查配置",null);

        //验证上传大小

        Integer flieUploadType = 0;  //0为普通文件，1为图片，2为媒体
        String oldFileName = file.getOriginalFilename();
        String eName = "";
        try{
            eName = oldFileName.substring(oldFileName.lastIndexOf("."));
        }catch (Exception e){
            oldFileName = oldFileName +".png";
            eName = oldFileName.substring(oldFileName.lastIndexOf("."));
        }
        BufferedImage bi = null;
        try {
            bi = ImageIO.read(file.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(bi != null||eName.equals(".WEBP")||eName.equals(".webp")){
            flieUploadType = 1;
        }

        Integer isMedia = baseFull.isMedia(eName);
        if(isMedia.equals(1)){
            flieUploadType = 2;
        }
        Integer uploadPicMax = Integer.parseInt(apiconfig.get("uploadPicMax").toString());
        Integer uploadMediaMax = Integer.parseInt(apiconfig.get("uploadMediaMax").toString());
        Integer uploadFilesMax = Integer.parseInt(apiconfig.get("uploadFilesMax").toString());
        if(flieUploadType.equals(0)){
            long filesMax = uploadFilesMax * 1024 * 1024;
            if (file.getSize() > filesMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(0,"文件大小不能超过"+filesMax+"M",null);
            }
        }
        if(flieUploadType.equals(1)){
            long picMax = uploadPicMax * 1024 * 1024;
            if (file.getSize() > picMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(0,"图片大小不能超过"+picMax+"M",null);
            }
        }

        if(flieUploadType.equals(2)){
            long mediaMax = uploadMediaMax * 1024 * 1024;
            if (file.getSize() > mediaMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(0,"媒体大小不能超过"+mediaMax+"M",null);
            }
        }
        //如果为图片，则开始内容检测
        if(flieUploadType.equals(1)){
            Integer cmsSwitch = Integer.parseInt(apiconfig.get("cmsSwitch").toString());
            if(cmsSwitch>1){
                try {
                    // 读取文件内容为字节数组
                    byte[] fileContent = file.getBytes();

                    // 将字节数组进行Base64编码
                    String fileBase64 =  Base64.getEncoder().encodeToString(fileContent);
                    Map violationData = securityService.picViolation(fileBase64,0);
                    if(violationData.get("Suggestion")!=null){
                        String Suggestion = violationData.get("Suggestion").toString();
                        if(Suggestion.equals("Block")){
                            return Result.getResultJson(0,"图片内容违规，请检查后重新提交！",null);
                        }
                    }

                } catch (IOException e) {
                    // 处理异常
                    e.printStackTrace();
                }
            }
            if(isCompression.equals(1)){
                File compressedFile = compressImage(file, imageCompressionLv);
                String contentType = file.getContentType();
                file = convertFileToMultipartFile(compressedFile, contentType);
            }
        }
        //验证上传大小结束
        String uploadType = apiconfig.get("uploadType").toString();
        if(uploadType.equals("cos")){
            result = uploadService.cosUpload(file,this.dataprefix,apiconfig,uid);
        }
        if(uploadType.equals("local")){
            result = uploadService.localUpload(file,this.dataprefix,apiconfig,uid);
        }
        if(uploadType.equals("oss")){
            result = uploadService.ossUpload(file,this.dataprefix,apiconfig,uid);
        }
        if(uploadType.equals("ftp")){
            result = uploadService.ftpUpload(file,this.dataprefix,apiconfig,uid);
        }
        if(uploadType.equals("qiniu")){
            result = uploadService.qiniuUpload(file,this.dataprefix,apiconfig,uid);
        }
        if(uploadType.equals("s3")){
            result = uploadService.s3Upload(file,this.dataprefix,apiconfig,uid);
        }

        //如果文件上传成功，则添加进文件表
        JSONObject resultObject = JSON.parseObject(result);
        if(resultObject.get("code").toString().equals("1")){
            String resultData = resultObject.get("data").toString();
            JSONObject resultInfo = JSON.parseObject(resultData);
            String url =  resultInfo.get("url").toString();
            String suffix = oldFileName.substring(oldFileName.lastIndexOf(".") + 1);

            files.setUid(uid);
            files.setSize(file.getSize());
            files.setName(oldFileName);
            files.setSuffix(suffix);
            Long date = System.currentTimeMillis();
            String curTime = String.valueOf(date).substring(0, 10);
            files.setCreated(Integer.parseInt(curTime));
            files.setSource(uploadType);
            files.setLinks(url);
            files.setType("other");
            if(flieUploadType.equals(1)){
                files.setType("picture");
            }
            if(flieUploadType.equals(2)){
                files.setType("media");
            }
            if(isDocOrArchive(file)){
                files.setType("document");
            }
            filesService.insert(files);
        }

        return result;
    }

    /**
     * 上传cos
     * @return
     */
    @RequestMapping(value = "/cosUpload",method = RequestMethod.POST)
    @ResponseBody
    @LoginRequired(purview = "0")
    public Object cosUpload(@RequestParam(value = "file") MultipartFile file, @RequestParam(value = "token", required = false) String  token) throws IOException {
        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid =Integer.parseInt(map.get("uid").toString());
        if(file == null){
            return new UploadMsg(0,"文件为空",null);
        }

        TypechoFiles files = new TypechoFiles();
        String md5 = getFileMd5(file);
        files.setMd5(md5);
        List<TypechoFiles> list = filesService.selectList(files);
        if(list.size()>0){
            Map<String,String> info =new HashMap<String, String>();
            info.put("url",list.get(0).getLinks());
            return Result.getResultJson(1,"上传成功",info);
        }

        Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
        //图片压缩字段初始化
        Integer isvip =Integer.parseInt(map.get("isvip").toString());
        Integer imageCompression = 0;
        if(apiconfig.get("imageCompression")!=null){
            imageCompression  = Integer.parseInt(apiconfig.get("imageCompression").toString());
        }
        Integer imageCompressionLv = 1080;
        if(apiconfig.get("imageCompression")!=null){
            imageCompressionLv  = Integer.parseInt(apiconfig.get("imageCompressionLv").toString());
        }
        Integer isCompression = 0;
        if(imageCompression.equals(1)){
            if(isvip.equals(0)){
                isCompression = 1;
            }
        }
        if(imageCompression.equals(2)){
            isCompression = 1;

        }
        //图片压缩字段初始化结束
        //验证上传大小

        Integer flieUploadType = 0;  //0为普通文件，1为图片，2为媒体
        String oldFileName = file.getOriginalFilename();
        String eName = "";
        try{
            eName = oldFileName.substring(oldFileName.lastIndexOf("."));
        }catch (Exception e){
            oldFileName = oldFileName +".png";
            eName = oldFileName.substring(oldFileName.lastIndexOf("."));
        }
        BufferedImage bi = null;
        try {
            bi = ImageIO.read(file.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(bi != null||eName.equals(".WEBP")||eName.equals(".webp")){
            flieUploadType = 1;
        }

        Integer isMedia = baseFull.isMedia(eName);
        if(isMedia.equals(1)){
            flieUploadType = 2;
        }
        Integer uploadPicMax = Integer.parseInt(apiconfig.get("uploadPicMax").toString());
        Integer uploadMediaMax = Integer.parseInt(apiconfig.get("uploadMediaMax").toString());
        Integer uploadFilesMax = Integer.parseInt(apiconfig.get("uploadFilesMax").toString());
        if(flieUploadType.equals(0)){
            long filesMax = uploadFilesMax * 1024 * 1024;
            if (file.getSize() > filesMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(0,"文件大小不能超过"+filesMax+"M",null);
            }
        }
        if(flieUploadType.equals(1)){
            long picMax = uploadPicMax * 1024 * 1024;
            if (file.getSize() > picMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(0,"图片大小不能超过"+picMax+"M",null);
            }
        }

        if(flieUploadType.equals(2)){
            long mediaMax = uploadMediaMax * 1024 * 1024;
            if (file.getSize() > mediaMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(0,"媒体大小不能超过"+mediaMax+"M",null);
            }
        }

        //验证上传大小结束
        String uploadType = apiconfig.get("uploadType").toString();
        if(!uploadType.equals("cos")){
            return Result.getResultJson(0,"该上传通道已关闭",null);
        }

        //如果为图片，则开始内容检测
        if(flieUploadType.equals(1)){
            Integer cmsSwitch = Integer.parseInt(apiconfig.get("cmsSwitch").toString());
            if(cmsSwitch>1){
                try {
                    // 读取文件内容为字节数组
                    byte[] fileContent = file.getBytes();

                    // 将字节数组进行Base64编码
                    String fileBase64 =  Base64.getEncoder().encodeToString(fileContent);
                    Map violationData = securityService.picViolation(fileBase64,0);
                    if(violationData.get("Suggestion")!=null){
                        String Suggestion = violationData.get("Suggestion").toString();
                        if(Suggestion.equals("Block")){
                            return Result.getResultJson(0,"图片内容违规，请检查后重新提交！",null);
                        }
                    }
                } catch (IOException e) {
                    // 处理异常
                    e.printStackTrace();
                }
            }
            if(isCompression.equals(1)){
                File compressedFile = compressImage(file, imageCompressionLv);
                String contentType = file.getContentType();
                file = convertFileToMultipartFile(compressedFile, contentType);
            }
        }

        String result = uploadService.cosUpload(file,this.dataprefix,apiconfig,uid);
        //如果文件上传成功，则添加进文件表
        JSONObject resultObject = JSON.parseObject(result);
        if(resultObject.get("code").toString().equals("1")){
            String resultData = resultObject.get("data").toString();
            JSONObject resultInfo = JSON.parseObject(resultData);
            String url =  resultInfo.get("url").toString();
            String suffix = oldFileName.substring(oldFileName.lastIndexOf(".") + 1);
            files.setUid(uid);
            files.setSize(file.getSize());
            files.setName(oldFileName);
            files.setSuffix(suffix);
            Long date = System.currentTimeMillis();
            String curTime = String.valueOf(date).substring(0, 10);
            files.setCreated(Integer.parseInt(curTime));
            files.setSource(uploadType);
            files.setLinks(url);
            files.setType("other");
            if(flieUploadType.equals(1)){
                files.setType("picture");
            }
            if(flieUploadType.equals(2)){
                files.setType("media");
            }
            if(isDocOrArchive(file)){
                files.setType("document");
            }
            filesService.insert(files);
        }
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
    @LoginRequired(purview = "0")
    public String localUpload(@RequestParam("file") MultipartFile file, @RequestParam(value = "token", required = false) String  token) throws IOException {
        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid =Integer.parseInt(map.get("uid").toString());

        TypechoFiles files = new TypechoFiles();
        String md5 = getFileMd5(file);
        files.setMd5(md5);
        List<TypechoFiles> list = filesService.selectList(files);
        if(list.size()>0){
            Map<String,String> info =new HashMap<String, String>();
            info.put("url",list.get(0).getLinks());
            return Result.getResultJson(1,"上传成功",info);
        }
        Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
        //图片压缩字段初始化
        Integer isvip =Integer.parseInt(map.get("isvip").toString());
        Integer imageCompression = 0;
        if(apiconfig.get("imageCompression")!=null){
            imageCompression  = Integer.parseInt(apiconfig.get("imageCompression").toString());
        }
        Integer imageCompressionLv = 1080;
        if(apiconfig.get("imageCompression")!=null){
            imageCompressionLv  = Integer.parseInt(apiconfig.get("imageCompressionLv").toString());
        }
        Integer isCompression = 0;
        if(imageCompression.equals(1)){
            if(isvip.equals(0)){
                isCompression = 1;
            }
        }
        if(imageCompression.equals(2)){
            isCompression = 1;

        }
        //图片压缩字段初始化结束
        //验证上传大小

        Integer flieUploadType = 0;  //0为普通文件，1为图片，2为媒体
        String oldFileName = file.getOriginalFilename();
        String eName = "";
        try{
            eName = oldFileName.substring(oldFileName.lastIndexOf("."));
        }catch (Exception e){
            oldFileName = oldFileName +".png";
            eName = oldFileName.substring(oldFileName.lastIndexOf("."));
        }
        BufferedImage bi = null;
        try {
            bi = ImageIO.read(file.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(bi != null||eName.equals(".WEBP")||eName.equals(".webp")){
            flieUploadType = 1;
        }

        Integer isMedia = baseFull.isMedia(eName);
        if(isMedia.equals(1)){
            flieUploadType = 2;
        }
        Integer uploadPicMax = Integer.parseInt(apiconfig.get("uploadPicMax").toString());
        Integer uploadMediaMax = Integer.parseInt(apiconfig.get("uploadMediaMax").toString());
        Integer uploadFilesMax = Integer.parseInt(apiconfig.get("uploadFilesMax").toString());
        if(flieUploadType.equals(0)){
            long filesMax = uploadFilesMax * 1024 * 1024;
            if (file.getSize() > filesMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(0,"文件大小不能超过"+filesMax+"M",null);
            }
        }
        if(flieUploadType.equals(1)){
            long picMax = uploadPicMax * 1024 * 1024;
            if (file.getSize() > picMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(0,"图片大小不能超过"+picMax+"M",null);
            }
        }

        if(flieUploadType.equals(2)){
            long mediaMax = uploadMediaMax * 1024 * 1024;
            if (file.getSize() > mediaMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(0,"媒体大小不能超过"+mediaMax+"M",null);
            }
        }

        //验证上传大小结束
        String uploadType = apiconfig.get("uploadType").toString();
        if(!uploadType.equals("local")){
            return Result.getResultJson(0,"该上传通道已关闭",null);
        }
        //如果为图片，则开始内容检测
        if(flieUploadType.equals(1)){
            Integer cmsSwitch = Integer.parseInt(apiconfig.get("cmsSwitch").toString());
            if(cmsSwitch>1){
                try {
                    // 读取文件内容为字节数组
                    byte[] fileContent = file.getBytes();

                    // 将字节数组进行Base64编码
                    String fileBase64 =  Base64.getEncoder().encodeToString(fileContent);
                    Map violationData = securityService.picViolation(fileBase64,0);
                    if(violationData.get("Suggestion")!=null){
                        String Suggestion = violationData.get("Suggestion").toString();
                        if(Suggestion.equals("Block")){
                            return Result.getResultJson(0,"图片内容违规，请检查后重新提交！",null);
                        }
                    }
                } catch (IOException e) {
                    // 处理异常
                    e.printStackTrace();
                }
            }
            if(isCompression.equals(1)){
                File compressedFile = compressImage(file, imageCompressionLv);
                String contentType = file.getContentType();
                file = convertFileToMultipartFile(compressedFile, contentType);
            }
        }

        String result = uploadService.localUpload(file,this.dataprefix,apiconfig,uid);
        //如果文件上传成功，则添加进文件表
        JSONObject resultObject = JSON.parseObject(result);
        if(resultObject.get("code").toString().equals("1")){
            String resultData = resultObject.get("data").toString();
            JSONObject resultInfo = JSON.parseObject(resultData);
            String url =  resultInfo.get("url").toString();
            String suffix = oldFileName.substring(oldFileName.lastIndexOf(".") + 1);
            files.setUid(uid);
            files.setSize(file.getSize());
            files.setName(oldFileName);
            files.setSuffix(suffix);
            Long date = System.currentTimeMillis();
            String curTime = String.valueOf(date).substring(0, 10);
            files.setCreated(Integer.parseInt(curTime));
            files.setSource(uploadType);
            files.setLinks(url);
            files.setType("other");
            if(flieUploadType.equals(1)){
                files.setType("picture");
            }
            if(flieUploadType.equals(2)){
                files.setType("media");
            }
            if(isDocOrArchive(file)){
                files.setType("document");
            }
            filesService.insert(files);
        }
        return result;

    }

    /**
     * 上传到oss
     * */
    @RequestMapping(value = "/ossUpload",method = RequestMethod.POST)
    @ResponseBody
    @LoginRequired(purview = "0")
    public String ossUpload(@RequestParam("file") MultipartFile file, @RequestParam(value = "token", required = false) String  token) throws IOException {
        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid =Integer.parseInt(map.get("uid").toString());
        TypechoFiles files = new TypechoFiles();
        String md5 = getFileMd5(file);
        files.setMd5(md5);
        List<TypechoFiles> list = filesService.selectList(files);
        if(list.size()>0){
            Map<String,String> info =new HashMap<String, String>();
            info.put("url",list.get(0).getLinks());
            return Result.getResultJson(1,"上传成功",info);
        }
        Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
        //图片压缩字段初始化
        Integer isvip =Integer.parseInt(map.get("isvip").toString());
        Integer imageCompression = 0;
        if(apiconfig.get("imageCompression")!=null){
            imageCompression  = Integer.parseInt(apiconfig.get("imageCompression").toString());
        }
        Integer imageCompressionLv = 1080;
        if(apiconfig.get("imageCompression")!=null){
            imageCompressionLv  = Integer.parseInt(apiconfig.get("imageCompressionLv").toString());
        }
        Integer isCompression = 0;
        if(imageCompression.equals(1)){
            if(isvip.equals(0)){
                isCompression = 1;
            }
        }
        if(imageCompression.equals(2)){
            isCompression = 1;

        }
        //图片压缩字段初始化结束
        //验证上传大小

        Integer flieUploadType = 0;  //0为普通文件，1为图片，2为媒体
        String oldFileName = file.getOriginalFilename();
        String eName = "";
        try{
            eName = oldFileName.substring(oldFileName.lastIndexOf("."));
        }catch (Exception e){
            oldFileName = oldFileName +".png";
            eName = oldFileName.substring(oldFileName.lastIndexOf("."));
        }
        BufferedImage bi = null;
        try {
            bi = ImageIO.read(file.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(bi != null||eName.equals(".WEBP")||eName.equals(".webp")){
            flieUploadType = 1;
        }

        Integer isMedia = baseFull.isMedia(eName);
        if(isMedia.equals(1)){
            flieUploadType = 2;
        }
        Integer uploadPicMax = Integer.parseInt(apiconfig.get("uploadPicMax").toString());
        Integer uploadMediaMax = Integer.parseInt(apiconfig.get("uploadMediaMax").toString());
        Integer uploadFilesMax = Integer.parseInt(apiconfig.get("uploadFilesMax").toString());
        if(flieUploadType.equals(0)){
            long filesMax = uploadFilesMax * 1024 * 1024;
            if (file.getSize() > filesMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(0,"文件大小不能超过"+filesMax+"M",null);
            }
        }
        if(flieUploadType.equals(1)){
            long picMax = uploadPicMax * 1024 * 1024;
            if (file.getSize() > picMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(0,"图片大小不能超过"+picMax+"M",null);
            }
        }

        if(flieUploadType.equals(2)){
            long mediaMax = uploadMediaMax * 1024 * 1024;
            if (file.getSize() > mediaMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(0,"媒体大小不能超过"+mediaMax+"M",null);
            }
        }

        //验证上传大小结束
        String uploadType = apiconfig.get("uploadType").toString();
        if(!uploadType.equals("oss")){
            return Result.getResultJson(0,"该上传通道已关闭",null);
        }

        //如果为图片，则开始内容检测
        if(flieUploadType.equals(1)){
            Integer cmsSwitch = Integer.parseInt(apiconfig.get("cmsSwitch").toString());
            if(cmsSwitch>1){
                try {
                    // 读取文件内容为字节数组
                    byte[] fileContent = file.getBytes();

                    // 将字节数组进行Base64编码
                    String fileBase64 =  Base64.getEncoder().encodeToString(fileContent);
                    Map violationData = securityService.picViolation(fileBase64,0);
                    if(violationData.get("Suggestion")!=null){
                        String Suggestion = violationData.get("Suggestion").toString();
                        if(Suggestion.equals("Block")){
                            return Result.getResultJson(0,"图片内容违规，请检查后重新提交！",null);
                        }
                    }
                } catch (IOException e) {
                    // 处理异常
                    e.printStackTrace();
                }
            }
            if(isCompression.equals(1)){
                File compressedFile = compressImage(file, imageCompressionLv);
                String contentType = file.getContentType();
                file = convertFileToMultipartFile(compressedFile, contentType);
            }
        }

        String result = uploadService.ossUpload(file,this.dataprefix,apiconfig,uid);
        //如果文件上传成功，则添加进文件表
        JSONObject resultObject = JSON.parseObject(result);
        if(resultObject.get("code").toString().equals("1")){
            String resultData = resultObject.get("data").toString();
            JSONObject resultInfo = JSON.parseObject(resultData);
            String url =  resultInfo.get("url").toString();
            String suffix = oldFileName.substring(oldFileName.lastIndexOf(".") + 1);
            files.setUid(uid);
            files.setSize(file.getSize());
            files.setName(oldFileName);
            files.setSuffix(suffix);
            Long date = System.currentTimeMillis();
            String curTime = String.valueOf(date).substring(0, 10);
            files.setCreated(Integer.parseInt(curTime));
            files.setSource(uploadType);
            files.setLinks(url);
            files.setType("other");
            if(flieUploadType.equals(1)){
                files.setType("picture");
            }
            if(flieUploadType.equals(2)){
                files.setType("media");
            }
            if(isDocOrArchive(file)){
                files.setType("document");
            }
            filesService.insert(files);
        }
        return result;

    }
    /**
     * 上传到七牛云
     * */
    @RequestMapping(value = "/qiniuUpload",method = RequestMethod.POST)
    @ResponseBody
    @LoginRequired(purview = "0")
    public String qiniuUpload(@RequestParam("file") MultipartFile file, @RequestParam(value = "token", required = false) String  token) throws IOException {
        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid =Integer.parseInt(map.get("uid").toString());
        TypechoFiles files = new TypechoFiles();
        String md5 = getFileMd5(file);
        files.setMd5(md5);
        List<TypechoFiles> list = filesService.selectList(files);
        if(list.size()>0){
            Map<String,String> info =new HashMap<String, String>();
            info.put("url",list.get(0).getLinks());
            return Result.getResultJson(1,"上传成功",info);
        }
        Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
        //图片压缩字段初始化
        Integer isvip =Integer.parseInt(map.get("isvip").toString());
        Integer imageCompression = 0;
        if(apiconfig.get("imageCompression")!=null){
            imageCompression  = Integer.parseInt(apiconfig.get("imageCompression").toString());
        }
        Integer imageCompressionLv = 1080;
        if(apiconfig.get("imageCompression")!=null){
            imageCompressionLv  = Integer.parseInt(apiconfig.get("imageCompressionLv").toString());
        }
        Integer isCompression = 0;
        if(imageCompression.equals(1)){
            if(isvip.equals(0)){
                isCompression = 1;
            }
        }
        if(imageCompression.equals(2)){
            isCompression = 1;

        }
        //图片压缩字段初始化结束
        //验证上传大小

        Integer flieUploadType = 0;  //0为普通文件，1为图片，2为媒体
        String oldFileName = file.getOriginalFilename();
        String eName = "";
        try{
            eName = oldFileName.substring(oldFileName.lastIndexOf("."));
        }catch (Exception e){
            oldFileName = oldFileName +".png";
            eName = oldFileName.substring(oldFileName.lastIndexOf("."));
        }
        BufferedImage bi = null;
        try {
            bi = ImageIO.read(file.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(bi != null||eName.equals(".WEBP")||eName.equals(".webp")){
            flieUploadType = 1;
        }

        Integer isMedia = baseFull.isMedia(eName);
        if(isMedia.equals(1)){
            flieUploadType = 2;
        }
        Integer uploadPicMax = Integer.parseInt(apiconfig.get("uploadPicMax").toString());
        Integer uploadMediaMax = Integer.parseInt(apiconfig.get("uploadMediaMax").toString());
        Integer uploadFilesMax = Integer.parseInt(apiconfig.get("uploadFilesMax").toString());
        if(flieUploadType.equals(0)){
            long filesMax = uploadFilesMax * 1024 * 1024;
            if (file.getSize() > filesMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(0,"文件大小不能超过"+filesMax+"M",null);
            }
        }
        if(flieUploadType.equals(1)){
            long picMax = uploadPicMax * 1024 * 1024;
            if (file.getSize() > picMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(0,"图片大小不能超过"+picMax+"M",null);
            }
        }

        if(flieUploadType.equals(2)){
            long mediaMax = uploadMediaMax * 1024 * 1024;
            if (file.getSize() > mediaMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(0,"媒体大小不能超过"+mediaMax+"M",null);
            }
        }

        //验证上传大小结束
        String uploadType = apiconfig.get("uploadType").toString();
        if(!uploadType.equals("qiniu")){
            return Result.getResultJson(0,"该上传通道已关闭",null);
        }

        //如果为图片，则开始内容检测
        if(flieUploadType.equals(1)){
            Integer cmsSwitch = Integer.parseInt(apiconfig.get("cmsSwitch").toString());
            if(cmsSwitch>1){
                try {
                    // 读取文件内容为字节数组
                    byte[] fileContent = file.getBytes();

                    // 将字节数组进行Base64编码
                    String fileBase64 =  Base64.getEncoder().encodeToString(fileContent);
                    Map violationData = securityService.picViolation(fileBase64,0);
                    if(violationData.get("Suggestion")!=null){
                        String Suggestion = violationData.get("Suggestion").toString();
                        if(Suggestion.equals("Block")){
                            return Result.getResultJson(0,"图片内容违规，请检查后重新提交！",null);
                        }
                    }
                } catch (IOException e) {
                    // 处理异常
                    e.printStackTrace();
                }
            }
            if(isCompression.equals(1)){
                File compressedFile = compressImage(file, imageCompressionLv);
                String contentType = file.getContentType();
                file = convertFileToMultipartFile(compressedFile, contentType);
            }
        }

        String result = uploadService.qiniuUpload(file,this.dataprefix,apiconfig,uid);
        //如果文件上传成功，则添加进文件表
        JSONObject resultObject = JSON.parseObject(result);
        if(resultObject.get("code").toString().equals("1")){
            String resultData = resultObject.get("data").toString();
            JSONObject resultInfo = JSON.parseObject(resultData);
            String url =  resultInfo.get("url").toString();
            String suffix = oldFileName.substring(oldFileName.lastIndexOf(".") + 1);
            files.setUid(uid);
            files.setSize(file.getSize());
            files.setName(oldFileName);
            files.setSuffix(suffix);
            Long date = System.currentTimeMillis();
            String curTime = String.valueOf(date).substring(0, 10);
            files.setCreated(Integer.parseInt(curTime));
            files.setSource(uploadType);
            files.setLinks(url);
            files.setType("other");
            if(flieUploadType.equals(1)){
                files.setType("picture");
            }
            if(flieUploadType.equals(2)){
                files.setType("media");
            }
            if(isDocOrArchive(file)){
                files.setType("document");
            }
            filesService.insert(files);
        }
        return result;
    }
    /**
     * 上传到远程ftp
     * */
    @RequestMapping(value = "ftpUpload",method = RequestMethod.POST)
    @ResponseBody
    @LoginRequired(purview = "0")
    public String ftpUpload(@RequestParam(value = "file") MultipartFile file, @RequestParam(value = "token", required = false) String  token) throws IOException {
        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid =Integer.parseInt(map.get("uid").toString());
        String oldFileName = file.getOriginalFilename();
        TypechoFiles files = new TypechoFiles();
        String md5 = getFileMd5(file);
        files.setMd5(md5);
        List<TypechoFiles> list = filesService.selectList(files);
        if(list.size()>0){
            Map<String,String> info =new HashMap<String, String>();
            info.put("url",list.get(0).getLinks());
            return Result.getResultJson(1,"上传成功",info);
        }
        Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
        //图片压缩字段初始化
        Integer isvip =Integer.parseInt(map.get("isvip").toString());
        Integer imageCompression = 0;
        if(apiconfig.get("imageCompression")!=null){
            imageCompression  = Integer.parseInt(apiconfig.get("imageCompression").toString());
        }
        Integer imageCompressionLv = 1080;
        if(apiconfig.get("imageCompression")!=null){
            imageCompressionLv  = Integer.parseInt(apiconfig.get("imageCompressionLv").toString());
        }
        Integer isCompression = 0;
        if(imageCompression.equals(1)){
            if(isvip.equals(0)){
                isCompression = 1;
            }
        }
        if(imageCompression.equals(2)){
            isCompression = 1;

        }
        //图片压缩字段初始化结束
        //验证上传大小

        Integer flieUploadType = 0;  //0为普通文件，1为图片，2为媒体
        String eName = "";
        try{
            eName = oldFileName.substring(oldFileName.lastIndexOf("."));
        }catch (Exception e){
            oldFileName = oldFileName +".png";
            eName = oldFileName.substring(oldFileName.lastIndexOf("."));
        }
        BufferedImage bi = null;
        try {
            bi = ImageIO.read(file.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(bi != null||eName.equals(".WEBP")||eName.equals(".webp")){
            flieUploadType = 1;
        }

        Integer isMedia = baseFull.isMedia(eName);
        if(isMedia.equals(1)){
            flieUploadType = 2;
        }
        Integer uploadPicMax = Integer.parseInt(apiconfig.get("uploadPicMax").toString());
        Integer uploadMediaMax = Integer.parseInt(apiconfig.get("uploadMediaMax").toString());
        Integer uploadFilesMax = Integer.parseInt(apiconfig.get("uploadFilesMax").toString());
        if(flieUploadType.equals(0)){
            long filesMax = uploadFilesMax * 1024 * 1024;
            if (file.getSize() > filesMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(0,"文件大小不能超过"+filesMax+"M",null);
            }
        }
        if(flieUploadType.equals(1)){
            long picMax = uploadPicMax * 1024 * 1024;
            if (file.getSize() > picMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(0,"图片大小不能超过"+picMax+"M",null);
            }
        }

        if(flieUploadType.equals(2)){
            long mediaMax = uploadMediaMax * 1024 * 1024;
            if (file.getSize() > mediaMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(0,"媒体大小不能超过"+mediaMax+"M",null);
            }
        }

        //验证上传大小结束
        String uploadType = apiconfig.get("uploadType").toString();
        if(!uploadType.equals("ftp")){
            return Result.getResultJson(0,"该上传通道已关闭",null);
        }

        //如果为图片，则开始内容检测
        if(flieUploadType.equals(1)){
            Integer cmsSwitch = Integer.parseInt(apiconfig.get("cmsSwitch").toString());
            if(cmsSwitch>1){
                try {
                    // 读取文件内容为字节数组
                    byte[] fileContent = file.getBytes();

                    // 将字节数组进行Base64编码
                    String fileBase64 =  Base64.getEncoder().encodeToString(fileContent);
                    Map violationData = securityService.picViolation(fileBase64,0);
                    if(violationData.get("Suggestion")!=null){
                        String Suggestion = violationData.get("Suggestion").toString();
                        if(Suggestion.equals("Block")){
                            return Result.getResultJson(0,"图片内容违规，请检查后重新提交！",null);
                        }
                    }
                } catch (IOException e) {
                    // 处理异常
                    e.printStackTrace();
                }
            }
            if(isCompression.equals(1)){
                File compressedFile = compressImage(file, imageCompressionLv);
                String contentType = file.getContentType();
                file = convertFileToMultipartFile(compressedFile, contentType);
            }
        }
        String result = uploadService.ftpUpload(file,this.dataprefix,apiconfig,uid);
        //如果文件上传成功，则添加进文件表
        JSONObject resultObject = JSON.parseObject(result);
        if(resultObject.get("code").toString().equals("1")){
            String resultData = resultObject.get("data").toString();
            JSONObject resultInfo = JSON.parseObject(resultData);
            String url =  resultInfo.get("url").toString();
            String suffix = oldFileName.substring(oldFileName.lastIndexOf(".") + 1);
            files.setUid(uid);
            files.setSize(file.getSize());
            files.setName(oldFileName);
            files.setSuffix(suffix);
            Long date = System.currentTimeMillis();
            String curTime = String.valueOf(date).substring(0, 10);
            files.setCreated(Integer.parseInt(curTime));
            files.setSource(uploadType);
            files.setLinks(url);
            files.setType("other");
            if(flieUploadType.equals(1)){
                files.setType("picture");
            }
            if(flieUploadType.equals(2)){
                files.setType("media");
            }
            if(isDocOrArchive(file)){
                files.setType("document");
            }
            filesService.insert(files);
        }
        return result;


    }
    /**
     * 上传到oss
     * */
    @RequestMapping(value = "/s3Upload",method = RequestMethod.POST)
    @ResponseBody
    @LoginRequired(purview = "0")
    public String s3Upload(@RequestParam("file") MultipartFile file, @RequestParam(value = "token", required = false) String  token) throws IOException {
        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid =Integer.parseInt(map.get("uid").toString());
        TypechoFiles files = new TypechoFiles();
        String md5 = getFileMd5(file);
        files.setMd5(md5);
        List<TypechoFiles> list = filesService.selectList(files);
        if(list.size()>0){
            Map<String,String> info =new HashMap<String, String>();
            info.put("url",list.get(0).getLinks());
            return Result.getResultJson(1,"上传成功",info);
        }
        Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
        //图片压缩字段初始化
        Integer isvip =Integer.parseInt(map.get("isvip").toString());
        Integer imageCompression = 0;
        if(apiconfig.get("imageCompression")!=null){
            imageCompression  = Integer.parseInt(apiconfig.get("imageCompression").toString());
        }
        Integer imageCompressionLv = 1080;
        if(apiconfig.get("imageCompression")!=null){
            imageCompressionLv  = Integer.parseInt(apiconfig.get("imageCompressionLv").toString());
        }
        Integer isCompression = 0;
        if(imageCompression.equals(1)){
            if(isvip.equals(0)){
                isCompression = 1;
            }
        }
        if(imageCompression.equals(2)){
            isCompression = 1;

        }
        //图片压缩字段初始化结束
        //验证上传大小

        Integer flieUploadType = 0;  //0为普通文件，1为图片，2为媒体
        String oldFileName = file.getOriginalFilename();
        String eName = "";
        try{
            eName = oldFileName.substring(oldFileName.lastIndexOf("."));
        }catch (Exception e){
            oldFileName = oldFileName +".png";
            eName = oldFileName.substring(oldFileName.lastIndexOf("."));
        }
        BufferedImage bi = null;
        try {
            bi = ImageIO.read(file.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(bi != null||eName.equals(".WEBP")||eName.equals(".webp")){
            flieUploadType = 1;
        }

        Integer isMedia = baseFull.isMedia(eName);
        if(isMedia.equals(1)){
            flieUploadType = 2;
        }
        Integer uploadPicMax = Integer.parseInt(apiconfig.get("uploadPicMax").toString());
        Integer uploadMediaMax = Integer.parseInt(apiconfig.get("uploadMediaMax").toString());
        Integer uploadFilesMax = Integer.parseInt(apiconfig.get("uploadFilesMax").toString());
        if(flieUploadType.equals(0)){
            long filesMax = uploadFilesMax * 1024 * 1024;
            if (file.getSize() > filesMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(0,"文件大小不能超过"+filesMax+"M",null);
            }
        }
        if(flieUploadType.equals(1)){
            long picMax = uploadPicMax * 1024 * 1024;
            if (file.getSize() > picMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(0,"图片大小不能超过"+picMax+"M",null);
            }
        }

        if(flieUploadType.equals(2)){
            long mediaMax = uploadMediaMax * 1024 * 1024;
            if (file.getSize() > mediaMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(0,"媒体大小不能超过"+mediaMax+"M",null);
            }
        }

        //验证上传大小结束
        String uploadType = apiconfig.get("uploadType").toString();
        if(!uploadType.equals("s3")){
            return Result.getResultJson(0,"该上传通道已关闭",null);
        }

        //如果为图片，则开始内容检测
        if(flieUploadType.equals(1)){
            Integer cmsSwitch = Integer.parseInt(apiconfig.get("cmsSwitch").toString());
            if(cmsSwitch>1){
                try {
                    // 读取文件内容为字节数组
                    byte[] fileContent = file.getBytes();

                    // 将字节数组进行Base64编码
                    String fileBase64 =  Base64.getEncoder().encodeToString(fileContent);
                    Map violationData = securityService.picViolation(fileBase64,0);
                    if(violationData.get("Suggestion")!=null){
                        String Suggestion = violationData.get("Suggestion").toString();
                        if(Suggestion.equals("Block")){
                            return Result.getResultJson(0,"图片内容违规，请检查后重新提交！",null);
                        }
                    }
                } catch (IOException e) {
                    // 处理异常
                    e.printStackTrace();
                }
            }
            if(isCompression.equals(1)){
                File compressedFile = compressImage(file, imageCompressionLv);
                String contentType = file.getContentType();
                file = convertFileToMultipartFile(compressedFile, contentType);
            }
        }

        String result = uploadService.s3Upload(file,this.dataprefix,apiconfig,uid);
        //如果文件上传成功，则添加进文件表
        JSONObject resultObject = JSON.parseObject(result);
        if(resultObject.get("code").toString().equals("1")){
            String resultData = resultObject.get("data").toString();
            JSONObject resultInfo = JSON.parseObject(resultData);
            String url =  resultInfo.get("url").toString();
            String suffix = oldFileName.substring(oldFileName.lastIndexOf(".") + 1);
            files.setUid(uid);
            files.setSize(file.getSize());
            files.setName(oldFileName);
            files.setSuffix(suffix);
            Long date = System.currentTimeMillis();
            String curTime = String.valueOf(date).substring(0, 10);
            files.setCreated(Integer.parseInt(curTime));
            files.setSource(uploadType);
            files.setLinks(url);
            files.setType("other");
            if(flieUploadType.equals(1)){
                files.setType("picture");
            }
            if(flieUploadType.equals(2)){
                files.setType("media");
            }
            if(isDocOrArchive(file)){
                files.setType("document");
            }
            filesService.insert(files);
        }
        return result;

    }

    /***
     * 文件列表
     */
    @RequestMapping(value = "/fileList")
    @ResponseBody
    @LoginRequired(purview = "-1")
    public String fileList (@RequestParam(value = "searchParams", required = false) String  searchParams,
                           @RequestParam(value = "searchKey"        , required = false, defaultValue = "") String searchKey,
                           @RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                           @RequestParam(value = "order"        , required = false, defaultValue = "created") String order,
                           @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit) {
        List jsonList = new ArrayList();
        TypechoFiles query = new TypechoFiles();
        String sqlParams = "null";
        Integer total = 0;
        if (StringUtils.isNotBlank(searchParams)) {
            JSONObject object = JSON.parseObject(searchParams);
            query = object.toJavaObject(TypechoFiles.class);
            Map paramsJson = JSONObject.parseObject(JSONObject.toJSONString(query), Map.class);
            sqlParams = paramsJson.toString();
        }
        total = filesService.total(query,searchKey);
        List cacheList = redisHelp.getList(this.dataprefix+"_"+"fileList_"+page+"_"+limit+"_"+sqlParams+"_"+order+"_"+searchKey,redisTemplate);
        try {
            if (cacheList.size() > 0) {
                jsonList = cacheList;
            } else {
                PageList<TypechoFiles> pageList = filesService.selectPage(query, page, limit, searchKey,order);
                List<TypechoFiles> list = pageList.getList();
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
                    TypechoFiles files =  list.get(i);
                    Integer userid = files.getUid();
                    //获取用户信息
                    Map userJson = UserStatus.getUserInfo(userid,allconfigService,usersService);
                    json.put("userJson",userJson);
                    jsonList.add(json);
                }
                redisHelp.delete(this.dataprefix+"_"+"fileList_"+page+"_"+limit+"_"+sqlParams+"_"+order+"_"+searchKey,redisTemplate);
                redisHelp.setList(this.dataprefix+"_"+"fileList_"+page+"_"+limit+"_"+sqlParams+"_"+order+"_"+searchKey,jsonList,300,redisTemplate);
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
    //文件转base64
    public static String convertToBase64(MultipartFile file) {
        try {
            // 读取文件内容为字节数组
            byte[] fileContent = file.getBytes();

            // 将字节数组进行Base64编码
            return Base64.getEncoder().encodeToString(fileContent);
        } catch (IOException e) {
            // 处理异常
            e.printStackTrace();
            return null;
        }
    }

    public static boolean isDocOrArchive(MultipartFile file) {
        if (file == null || file.getOriginalFilename() == null) {
            return false;
        }

        String originalFilename = file.getOriginalFilename();
        int dotIndex = originalFilename.lastIndexOf(".");
        if (dotIndex == -1) {
            return false;
        }

        String ext = originalFilename.substring(dotIndex + 1).toLowerCase();

        switch (ext) {
            // 文档
            case "doc":
            case "docx":
            case "xls":
            case "xlsx":
            case "ppt":
            case "pptx":
            case "pdf":
            case "txt":
                // 压缩包
            case "zip":
            case "rar":
            case "7z":
            case "tar":
            case "gz":
            case "bz2":
                return true;
            default:
                return false;
        }
    }

    public static String getFileMd5(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
            byte[] digest = md.digest();

            // 转为十六进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 图片压缩方法，保持图片宽高比，最大宽度和最大高度都不超过 maxSize
     * @param file 传入的 MultipartFile
     * @param maxSize 最大宽高值
     * @return 压缩后的文件
     * @throws IOException
     */
    public static File compressImage(MultipartFile file, int maxSize) throws IOException {
        // 1. 创建缓存目录：JAR运行目录下的 cache
        String currentDir = new File(".").getCanonicalPath();
        File cacheDir = new File(currentDir, "cache");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }

        // 2. 将 MultipartFile 转换为临时 File
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null) {
            originalFileName = "tempFile_" + System.currentTimeMillis() + ".tmp";
        }
        File tempFile = new File(cacheDir, originalFileName);

        try (InputStream in = file.getInputStream();
             FileOutputStream out = new FileOutputStream(tempFile)) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        }

        // 3. 压缩后的文件存储位置
        File compressedFile = new File(cacheDir, "compressed_" + originalFileName);

        // 4. 使用 Thumbnails 进行压缩
        Thumbnails.of(tempFile)
                .size(maxSize, maxSize)
                .toFile(compressedFile);

        // 5. 返回压缩后的文件
        return compressedFile;
    }

    /**
     * 将 MultipartFile 转换为 File
     * @param file
     * @return
     * @throws IOException
     */
    private static File convertMultipartFileToFile(MultipartFile file) throws IOException {
        File convFile = new File(file.getOriginalFilename());
        file.transferTo(convFile);
        return convFile;
    }

    public static MultipartFile convertFileToMultipartFile(File file, String contentType) throws IOException {
        DiskFileItemFactory factory = new DiskFileItemFactory();
        FileItem fileItem = factory.createItem(file.getName(), contentType, false, file.getName());

        try (InputStream inputStream = new FileInputStream(file);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            fileItem.getOutputStream().write(outputStream.toByteArray());
        }

        return new CommonsMultipartFile(fileItem);
    }
    //分片上传开始
    @RequestMapping(value = "/chunk",method = RequestMethod.POST)
    @LoginRequired(purview = "0")
    @ResponseBody
    public Object uploadChunk(
            @RequestParam String uploadId,
            @RequestParam Integer index,
            @RequestParam Integer total,
            @RequestParam(required = false) MultipartFile file, // H5 上传 MultipartFile
            @RequestParam(required = false) String chunk,      // App / 小程序 base64
            @RequestParam(required = false) String token
    ) throws IOException {

        byte[] data;

        if (file != null) {
            data = file.getBytes(); // MultipartFile
        } else if (chunk != null) {
            data = Base64.getDecoder().decode(chunk); // base64
        } else {
            return Result.getResultJson(0, "分片为空", null);
        }

        String base = getChunkBasePath() + uploadId + "/";
        File dir = new File(base);
        if (!dir.exists()) dir.mkdirs();

        File outFile = new File(base + index);
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            fos.write(data);
        }

        return Result.getResultJson(1, "上传成功", null);
    }



    @RequestMapping(value = "/merge",method = RequestMethod.POST)
    @LoginRequired(purview = "0")
    @ResponseBody
    public Object mergeChunks(
            @RequestParam String uploadId,
            @RequestParam String filename,
            @RequestParam Integer total,
            @RequestParam(required = false) String token
    ) throws Exception {

        String basePath = getChunkBasePath();
        String chunkDir = basePath + uploadId + "/";

        File dir = new File(chunkDir);
        if (!dir.exists()) {
            return Result.getResultJson(0, "分片不存在", null);
        }

        File merged = new File(basePath + uploadId + "_merged_" + filename);
        if (merged.exists()) merged.delete();

        try (FileOutputStream fos = new FileOutputStream(merged, true)) {
            for (int i = 0; i < total; i++) {
                File chunk = new File(chunkDir + i);
                if (!chunk.exists()) {
                    return Result.getResultJson(0, "缺少分片：" + i, null);
                }
                Files.copy(chunk.toPath(), fos);
            }
        }

        // 删除分片目录
        for (int i = 0; i < total; i++) {
            new File(chunkDir + i).delete();
        }
        dir.delete();

        // ⭐ 使用 MockMultipartFile 再走 full 上传逻辑
        MultipartFile multipartFile = new MockMultipartFile(
                filename,
                filename,
                "application/octet-stream",
                new FileInputStream(merged)
        );

        return full(multipartFile, token);
    }

    private String getChunkBasePath() {
        String osName = System.getProperty("os.name").toLowerCase();

        // 默认路径（Linux）
        String defaultLinuxPath = "/opt/upload_chunks/";

        // Windows 使用用户目录，确保一定有权限
        String defaultWindowsPath = System.getProperty("user.home") + "/upload_chunks/";

        // 允许 application.properties 覆盖
        String configPath = System.getProperty("chunk.base.path"); // 可选
        if (configPath != null && !configPath.trim().isEmpty()) {
            return configPath.endsWith("/") ? configPath : (configPath + "/");
        }

        // OS 判断
        if (osName.contains("win")) {
            return defaultWindowsPath.endsWith("/") ? defaultWindowsPath : (defaultWindowsPath + "/");
        } else {
            return defaultLinuxPath.endsWith("/") ? defaultLinuxPath : (defaultLinuxPath + "/");
        }
    }

}
