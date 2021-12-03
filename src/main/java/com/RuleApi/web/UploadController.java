package com.RuleApi.web;

import com.RuleApi.common.ResultAll;
import com.RuleApi.common.UserStatus;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
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

    //获取腾讯云cos相关配置
    @Value("${spring.cos.accessKey}")
    private String accessKey;
    @Value("${spring.cos.secretKey}")
    private String secretKey;
    @Value("${spring.cos.bucket}")
    private String bucket;
    @Value("${spring.cos.bucketName}")
    private String bucketName;
    @Value("${spring.cos.path}")
    private String path;
    @Value("${spring.cos.prefix}")
    private String prefix;

    //获取阿里云oss相关配置
    @Value("${spring.aliyun.endpoint}")
    private String endpoint;
    @Value("${spring.aliyun.accessKeyId}")
    private String accessKeyId;
    @Value("${spring.aliyun.accessKeySecret}")
    private String accessKeySecret;
    @Value("${spring.aliyun.bucketName}")
    private String ossBucketName;
    @Value("${spring.aliyun.urlPrefix}")
    private String urlPrefix;

    //获取本地上传相关配置
    @Value("${webinfo.uploadUrl}")
    private String localUrl;

    @Autowired
    private RedisTemplate redisTemplate;

    ResultAll Result = new ResultAll();
    UserStatus UStatus = new UserStatus();

    /**
     * 上传道腾讯云服务器（https://cloud.tencent.com/document/product/436/10199）
     * @return
     */
    @RequestMapping(value = "/cosUpload",method = RequestMethod.POST)
    @ResponseBody
    public Object cosUpload(@RequestParam(value = "file") MultipartFile file, @RequestParam(value = "token", required = false) String  token) throws IOException {
        Integer uStatus = UStatus.getStatus(token,redisTemplate);
        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }
        if(file == null){
            return new UploadMsg(0,"文件为空",null);
        }
        String oldFileName = file.getOriginalFilename();
        String eName = oldFileName.substring(oldFileName.lastIndexOf("."));
        //检查是否是图片
        BufferedImage bi = ImageIO.read(file.getInputStream());
        if(bi == null){
            return Result.getResultJson(0,"请上传图片文件",null);
        }

        String newFileName = UUID.randomUUID()+eName;
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month=cal.get(Calendar.MONTH);
        int day=cal.get(Calendar.DATE);
        // 1 初始化用户身份信息(secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials(accessKey, secretKey);
        // 2 设置bucket的区域, COS地域的简称请参照 https://cloud.tencent.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region(bucket));
        // 3 生成cos客户端
        COSClient cosclient = new COSClient(cred, clientConfig);
        // bucket的命名规则为{name}-{appid} ，此处填写的存储桶名称必须为此格式
        String bucketName = this.bucketName;

        // 简单文件上传, 最大支持 5 GB, 适用于小文件上传, 建议 20 M 以下的文件使用该接口
        // 大文件上传请参照 API 文档高级 API 上传
        File localFile = null;
        try {
            localFile = File.createTempFile("temp",null);
            file.transferTo(localFile);
            // 指定要上传到 COS 上的路径
            String key = "/"+this.prefix+"/"+year+"/"+month+"/"+day+"/"+newFileName;
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, localFile);
            PutObjectResult putObjectResult = cosclient.putObject(putObjectRequest);
            //return new UploadMsg(1,"上传成功",this.path + putObjectRequest.getKey());
            Map<String,String> info =new HashMap<String, String>();
            info.put("url",this.path + putObjectRequest.getKey());
            return Result.getResultJson(1,"上传成功",info);

        } catch (IOException e) {
            return Result.getResultJson(1,"上传失败",null);
        }finally {
            // 关闭客户端(关闭后台线程)
            cosclient.shutdown();
        }
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
        Integer uStatus = UStatus.getStatus(token,redisTemplate);
        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }


        String filename = file.getOriginalFilename();
        String filetype = filename.substring(filename.lastIndexOf("."));
        String newfile = UUID.randomUUID()+filetype;
        //检查是否是图片
        BufferedImage bi = ImageIO.read(file.getInputStream());
        if(bi == null){
            return Result.getResultJson(0,"请上传图片文件",null);
        }

        String classespath = ClassUtils.getDefaultClassLoader().getResource("").getPath();
        /*解决文件路径中的空格问题*/
        String decodeClassespath = URLDecoder.decode(classespath,"utf-8");
        //System.out.println(decodeClassespath);

        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month=cal.get(Calendar.MONTH);
        int day=cal.get(Calendar.DATE);

        /**/
        File file1 = new File(decodeClassespath+"/static/upload/"+"/"+year+"/"+month+"/"+day+"/"+newfile);
        if(!file1.exists()){
            file1.mkdirs();
        }
        try {
            file.transferTo(file1);
            Map<String,String> info =new HashMap<String, String>();
            info.put("url",this.localUrl+"upload"+"/"+year+"/"+month+"/"+day+"/"+newfile);
            return Result.getResultJson(1,"上传成功",info);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Result.getResultJson(0,"上传失败",null);
    }

    /**
     * 上传到本地
     * */
    @RequestMapping(value = "/ossUpload",method = RequestMethod.POST)
    @ResponseBody
    public String uploadOssFile(@RequestParam("file") MultipartFile file, @RequestParam(value = "token", required = false) String  token) throws IOException {
        Integer uStatus = UStatus.getStatus(token,redisTemplate);
        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }
        //获取上传文件MultipartFile
        //返回上传到oss的路径
        OSS ossClient = new OSSClientBuilder().build(this.endpoint, this.accessKeyId, this.accessKeySecret);
        InputStream inputStream = null;
        //检查是否是图片
        BufferedImage bi = ImageIO.read(file.getInputStream());
        if(bi == null){
            return Result.getResultJson(0,"请上传图片文件",null);
        }
        try {
            //获取文件流
            inputStream = file.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //生成时间，用于创建目录
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month=cal.get(Calendar.MONTH);
        int day=cal.get(Calendar.DATE);
        //获取文件名称
        String filename = file.getOriginalFilename();
        String eName = filename.substring(filename.lastIndexOf("."));
        //1.在文件名称中添加随机唯一的值
        String newFileName = UUID.randomUUID()+eName;

       // String uuid = UUID.randomUUID().toString().replaceAll("-","");
        filename = newFileName;

        //2.把文件按日期分类
//        String datePath = new DateTime().toString("yyyy/MM/dd");
//        filename = datePath +"/"+filename;
        String key = "/"+this.prefix+"/"+year+"/"+month+"/"+day+"/"+filename;
        //调用OSS方法实现上传
        ossClient.putObject(this.ossBucketName, key, inputStream);
        ossClient.shutdown();
        String url = this.urlPrefix+key;
        Map<String,String> info =new HashMap<String, String>();
        info.put("url",url);
        return Result.getResultJson(1,"上传成功",info);
    }


}
