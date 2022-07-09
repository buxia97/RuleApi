package com.RuleApi.web;

import com.RuleApi.common.ResultAll;
import com.RuleApi.common.UserStatus;
import com.RuleApi.entity.TypechoApiconfig;
import com.RuleApi.service.TypechoApiconfigService;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
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
    private RedisTemplate redisTemplate;

    ResultAll Result = new ResultAll();
    UserStatus UStatus = new UserStatus();

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
        if(file == null){
            return new UploadMsg(0,"文件为空",null);
        }
        TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);
        String oldFileName = file.getOriginalFilename();
        //String eName = oldFileName.substring(oldFileName.lastIndexOf("."));
        String eName = "";
        try{
            eName = oldFileName.substring(oldFileName.lastIndexOf("."));
        }catch (Exception e){
            oldFileName = oldFileName +".png";
            eName = oldFileName.substring(oldFileName.lastIndexOf("."));
        }
        //检查是否是图片
        BufferedImage bi = ImageIO.read(file.getInputStream());
        if(bi == null){
            return Result.getResultJson(0,"请上传图片文件",null);
        }

        String newFileName = UUID.randomUUID()+eName;
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month=cal.get(Calendar.MONTH)+1;
        int day=cal.get(Calendar.DATE);
        // 1 初始化用户身份信息(secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials(apiconfig.getCosAccessKey(), apiconfig.getCosSecretKey());
        // 2 设置bucket的区域, COS地域的简称请参照 https://cloud.tencent.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region(apiconfig.getCosBucket()));
        // 3 生成cos客户端
        COSClient cosclient = new COSClient(cred, clientConfig);
        // bucket的命名规则为{name}-{appid} ，此处填写的存储桶名称必须为此格式
        String bucketName = apiconfig.getCosBucketName();

        // 简单文件上传, 最大支持 5 GB, 适用于小文件上传, 建议 20 M 以下的文件使用该接口
        // 大文件上传请参照 API 文档高级 API 上传
        File localFile = null;
        try {
            localFile = File.createTempFile("temp",null);
            file.transferTo(localFile);
            // 指定要上传到 COS 上的路径
            String key = "/"+apiconfig.getCosPrefix()+"/"+year+"/"+month+"/"+day+"/"+newFileName;
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, localFile);
            PutObjectResult putObjectResult = cosclient.putObject(putObjectRequest);
            //return new UploadMsg(1,"上传成功",this.path + putObjectRequest.getKey());
            Map<String,String> info =new HashMap<String, String>();
            info.put("url",apiconfig.getCosPath() + putObjectRequest.getKey());
            return Result.getResultJson(1,"上传成功",info);

        } catch (IOException e) {
            return Result.getResultJson(0,"上传失败",null);
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
        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }
        TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);

        String filename = file.getOriginalFilename();
        //String filetype = filename.substring(filename.lastIndexOf("."));
        //下面代码是解决app上传剪裁后图片无后缀问题。
        String filetype = "";
        try{
            filetype = filename.substring(filename.lastIndexOf("."));
        }catch (Exception e){
            filename = filename +".png";
            filetype = filename.substring(filename.lastIndexOf("."));
        }
        String newfile = UUID.randomUUID()+filetype;
        //检查是否是图片
        BufferedImage bi = ImageIO.read(file.getInputStream());
        if(bi == null){
            return Result.getResultJson(0,"请上传图片文件",null);
        }

        /*解决文件路径中的空格问题*/
        ApplicationHome h = new ApplicationHome(getClass());
        File jarF = h.getSource();
        /* 配置文件路径 */
        String classespath = jarF.getParentFile().toString()+"/files";

        String decodeClassespath = URLDecoder.decode(classespath,"utf-8");
        //System.out.println(decodeClassespath);

        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month=cal.get(Calendar.MONTH)+1;
        int day=cal.get(Calendar.DATE);

        /**/
        File file1 = new File(decodeClassespath+"/static/upload/"+"/"+year+"/"+month+"/"+day+"/"+newfile);
        if(!file1.exists()){
            file1.mkdirs();
        }
        try {
            file.transferTo(file1);

            Map<String,String> info =new HashMap<String, String>();
            info.put("url",apiconfig.getWebinfoUploadUrl()+"upload"+"/"+year+"/"+month+"/"+day+"/"+newfile);
            return Result.getResultJson(1,"上传成功",info);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Result.getResultJson(0,"上传失败",null);
    }

    /**
     * 上传到oss
     * */
    @RequestMapping(value = "/ossUpload",method = RequestMethod.POST)
    @ResponseBody
    public String uploadOssFile(@RequestParam("file") MultipartFile file, @RequestParam(value = "token", required = false) String  token) throws IOException {
        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }
        TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);
        //获取上传文件MultipartFile
        //返回上传到oss的路径
        OSS ossClient = new OSSClientBuilder().build(apiconfig.getAliyunEndpoint(), apiconfig.getAliyunAccessKeyId(),apiconfig.getAliyunAccessKeySecret());
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
        int month=cal.get(Calendar.MONTH)+1;
        int day=cal.get(Calendar.DATE);
        //获取文件名称
        String filename = file.getOriginalFilename();
        //String eName = filename.substring(filename.lastIndexOf("."));
        //应对图片剪裁后的无后缀图片
        String eName = "";
        try{
            eName = filename.substring(filename.lastIndexOf("."));
        }catch (Exception e){
            filename = filename +".png";
            eName = filename.substring(filename.lastIndexOf("."));
        }
        //1.在文件名称中添加随机唯一的值
        String newFileName = UUID.randomUUID()+eName;

       // String uuid = UUID.randomUUID().toString().replaceAll("-","");
        filename = newFileName;

        String key = apiconfig.getAliyunFilePrefix()+"/"+year+"/"+month+"/"+day+"/"+filename;
        //调用OSS方法实现上传
        ossClient.putObject(apiconfig.getAliyunAucketName(), key, inputStream);
        ossClient.shutdown();
        String url = apiconfig.getAliyunUrlPrefix()+key;
        Map<String,String> info =new HashMap<String, String>();
        info.put("url",url);
        return Result.getResultJson(1,"上传成功",info);
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
        String oldFileName = file.getOriginalFilename();
        //检查是否是图片
        BufferedImage bi = null;
        try {
            bi = ImageIO.read(file.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(bi == null){
            return Result.getResultJson(0,"请上传图片文件",null);
        }

        FTPClient ftpClient = new FTPClient();
        //检查是否是图片
        try {

            //指定存放上传文件的目录
            ApplicationHome h = new ApplicationHome(getClass());
            File jarF = h.getSource();
            /* 配置文件路径 */
            String classespath = jarF.getParentFile().toString()+"/files";

            String decodeClassespath = URLDecoder.decode(classespath,"utf-8");
            String fileDir = decodeClassespath+"/temp";
            File dir = new File(fileDir);

            //判断目录是否存在，不存在则创建目录
            if (!dir.exists()){
                dir.mkdirs();
            }

            //生成新文件名，防止文件名重复而导致文件覆盖
            //1、获取原文件后缀名 .img .jpg ....
            String originalFileName = file.getOriginalFilename();
            //String suffix = originalFileName.substring(originalFileName.lastIndexOf('.'));
            //应对图片剪裁后的无后缀图片
            String suffix = "";
            try{
                suffix = originalFileName.substring(originalFileName.lastIndexOf("."));
            }catch (Exception e){
                originalFileName = originalFileName +".png";
                suffix = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            //2、使用UUID生成新文件名
            String newFileName = UUID.randomUUID() + suffix;

            //生成文件
            File file1 = new File(dir, newFileName);

            //传输内容
            try {
                file.transferTo(file1);
                System.out.println("上传文件成功！");
            } catch (IOException e) {
                System.out.println("上传文件失败！");
                e.printStackTrace();
            }
            TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);
            //在服务器上生成新的目录
            String key = apiconfig.getFtpBasePath()+"/"+file1.getName();

            ftpClient.setConnectTimeout(1000 * 30);//设置连接超时时间
            ftpClient.setControlEncoding("utf-8");//设置ftp字符集
            //连接ftp服务器 参数填服务器的ip
            ftpClient.connect(apiconfig.getFtpHost(),apiconfig.getFtpPort());

            //进行登录 参数分别为账号 密码
            ftpClient.login(apiconfig.getFtpUsername(),apiconfig.getFtpPassword());



            //开启被动模式（按自己如何配置的ftp服务器来决定是否开启）
            ftpClient.enterLocalPassiveMode();
            //只能选择local_root下已存在的目录
            //ftpClient.changeWorkingDirectory(this.ftpBasePath);

            // 文件夹不存在时新建
            String remotePath = apiconfig.getFtpBasePath();
            if (!ftpClient.changeWorkingDirectory(remotePath)) {
                ftpClient.makeDirectory(remotePath);
                ftpClient.changeWorkingDirectory(remotePath);
            }
            //设置文件类型为二进制文件
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            //inputStream = file.getInputStream();
            //上传文件 参数：上传后的文件名，输入流
            ftpClient.storeFile(key, new FileInputStream(file1));

            ftpClient.disconnect();
            Map<String,String> info =new HashMap<String, String>();
            info.put("url",apiconfig.getWebinfoUploadUrl()+key);
            return Result.getResultJson(1,"上传成功",info);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(0,"上传失败",null);
        }

    }
}
