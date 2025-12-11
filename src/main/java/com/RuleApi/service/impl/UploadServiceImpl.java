package com.RuleApi.service.impl;

import com.RuleApi.common.*;
import com.RuleApi.entity.TypechoApiconfig;
import com.RuleApi.entity.TypechoFiles;
import com.RuleApi.service.FilesService;
import com.RuleApi.service.UploadService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.google.gson.Gson;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.region.Region;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
@Service
public class UploadServiceImpl  implements UploadService {

    ResultAll Result = new ResultAll();
    baseFull baseFull = new baseFull();
    EditFile editFile = new EditFile();


    public String base64Upload(String base64Img, String dataprefix, Map apiconfig, Integer uid, FilesService filesService) {
        try {
            // 假设从dataprefix中解析文件扩展名
            String eName = ".png"; // 示例扩展名, 根据实际情况调整

            String base64Image = base64Img.split(",")[1];
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            InputStream inputStream = new ByteArrayInputStream(imageBytes);

            String newFileName = UUID.randomUUID() + eName;
            Calendar cal = Calendar.getInstance();
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH) + 1;
            int day = cal.get(Calendar.DATE);
            
            if(apiconfig.get("uploadType").toString().equals("cos")){
                COSCredentials cred = new BasicCOSCredentials(apiconfig.get("cosAccessKey").toString(), apiconfig.get("cosSecretKey").toString());
                ClientConfig clientConfig = new ClientConfig(new Region(apiconfig.get("cosBucket").toString()));
                COSClient cosclient = new COSClient(cred, clientConfig);

                String bucketName = apiconfig.get("cosBucketName").toString();
                String key = "/" + apiconfig.get("cosPrefix").toString() + "/" + year + "/" + month + "/" + day + "/" + newFileName;

                try {
                    PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, inputStream, null);
                    PutObjectResult putObjectResult = cosclient.putObject(putObjectRequest);
                    // 假设editFile.setLog是有效的日志记录方法
                    editFile.setLog("用户" + uid + "通过cosUpload成功上传了图片");
                    //如果文件上传成功，则添加进文件表
                    TypechoFiles files = new TypechoFiles();
                    String md5 = getMD5(imageBytes);
                    files.setUid(uid);
                    long size = imageBytes.length;  // imageBytes.length 本身就是 long 可赋值给 Long
                    Long sizeLong = Long.valueOf(size);
                    files.setSize(sizeLong);
                    files.setName(newFileName);
                    files.setSuffix("png");
                    Long date = System.currentTimeMillis();
                    String curTime = String.valueOf(date).substring(0, 10);
                    files.setCreated(Integer.parseInt(curTime));
                    files.setSource(apiconfig.get("uploadType").toString());
                    files.setLinks(apiconfig.get("cosPath").toString() + putObjectRequest.getKey());
                    files.setMd5(md5);
                    files.setType("picture");
                    filesService.insert(files);
                    return apiconfig.get("cosPath").toString() + putObjectRequest.getKey();
                } finally {
                    cosclient.shutdown();
                    if(inputStream != null) {
                        inputStream.close();
                    }
                }
            }
            if(apiconfig.get("uploadType").toString().equals("local")){
                String decodeClassespath = null;
                //如果用户自定义了本地存储地址，则使用自定义地址，否则调用jar所在路径拼接地址


                if(apiconfig.get("localPath").toString()!=null&&!apiconfig.get("localPath").toString().isEmpty()){
                    decodeClassespath = apiconfig.get("localPath").toString();
                }else{
                    /*解决文件路径中的空格问题*/
                    ApplicationHome h = new ApplicationHome(getClass());
                    File jarF = h.getSource();
                    /* 配置文件路径 */
                    String classespath = jarF.getParentFile().toString()+"/files/static";
                    try {
                        decodeClassespath = URLDecoder.decode(classespath,"utf-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                        decodeClassespath = "/opt/files/static";
                    }
                }
                File file1 = new File(decodeClassespath+"/upload/"+"/"+year+"/"+month+"/"+day+"/"+newFileName);
                if(!file1.exists()){
                    file1.mkdirs();
                }
                try {
                    Files.copy(inputStream, file1.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    Map<String,String> info =new HashMap<String, String>();
                   // info.put("url",apiconfig.get("webinfoUploadUrl").toString()+"upload"+"/"+year+"/"+month+"/"+day+"/"+newFileName);
                    editFile.setLog("用户"+uid+"通过localUpload成功上传了图片");
                    //如果文件上传成功，则添加进文件表
                    TypechoFiles files = new TypechoFiles();
                    String md5 = getMD5(imageBytes);
                    files.setUid(uid);
                    long size = imageBytes.length;  // imageBytes.length 本身就是 long 可赋值给 Long
                    Long sizeLong = Long.valueOf(size);
                    files.setSize(sizeLong);
                    files.setName(newFileName);
                    files.setSuffix("png");
                    Long date = System.currentTimeMillis();
                    String curTime = String.valueOf(date).substring(0, 10);
                    files.setCreated(Integer.parseInt(curTime));
                    files.setSource(apiconfig.get("uploadType").toString());
                    files.setLinks(apiconfig.get("webinfoUploadUrl").toString()+"upload"+"/"+year+"/"+month+"/"+day+"/"+newFileName);
                    files.setMd5(md5);
                    files.setType("picture");
                    filesService.insert(files);
                    return apiconfig.get("webinfoUploadUrl").toString()+"upload"+"/"+year+"/"+month+"/"+day+"/"+newFileName;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                editFile.setLog("用户"+uid+"通过localUpload上传图片失败");
                return "";
            }
            if(apiconfig.get("uploadType").toString().equals("oss")){

                OSS ossClient = new OSSClientBuilder().build(apiconfig.get("aliyunEndpoint").toString(), apiconfig.get("aliyunAccessKeyId").toString(),apiconfig.get("aliyunAccessKeySecret").toString());
                String key = apiconfig.get("aliyunFilePrefix").toString()+"/"+year+"/"+month+"/"+day+"/"+newFileName;
                //调用OSS方法实现上传
                ossClient.putObject(apiconfig.get("aliyunAucketName").toString(), key, inputStream);
                ossClient.shutdown();
                String url = apiconfig.get("aliyunUrlPrefix").toString()+key;
                Map<String,String> info =new HashMap<String, String>();
                //info.put("url",url);

                editFile.setLog("用户"+uid+"通过ossUpload成功上传了图片");
                //如果文件上传成功，则添加进文件表
                TypechoFiles files = new TypechoFiles();
                String md5 = getMD5(imageBytes);
                files.setUid(uid);
                long size = imageBytes.length;  // imageBytes.length 本身就是 long 可赋值给 Long
                Long sizeLong = Long.valueOf(size);
                files.setSize(sizeLong);
                files.setName(newFileName);
                files.setSuffix("png");
                Long date = System.currentTimeMillis();
                String curTime = String.valueOf(date).substring(0, 10);
                files.setCreated(Integer.parseInt(curTime));
                files.setSource(apiconfig.get("uploadType").toString());
                files.setLinks(url);
                files.setMd5(md5);
                files.setType("picture");
                filesService.insert(files);
                return url;
            }
            if(apiconfig.get("uploadType").toString().equals("qiniu")){
                String key = "/app/"+year+"/"+month+"/"+day+"/"+newFileName;

                // 构造一个带指定Zone对象的配置类, 注意这里的Zone.zone0需要根据主机选择

                Configuration cfg = new Configuration();
                // 其他参数参考类注释
                UploadManager uploadManager = new UploadManager(cfg);
                // 生成上传凭证，然后准备上传
                try {
                    Auth auth = Auth.create(apiconfig.get("qiniuAccessKey").toString(), apiconfig.get("qiniuSecretKey").toString());
                    String upToken = auth.uploadToken(apiconfig.get("qiniuBucketName").toString());
                    try {
                        Response response = uploadManager.put(inputStream, key, upToken, null, null);
                        // 解析上传成功的结果
                        DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);

                        String returnPath = apiconfig.get("qiniuDomain").toString() + putRet.key;

                        editFile.setLog("用户"+uid+"通过qiniuUpload成功上传了图片");
                        //如果文件上传成功，则添加进文件表
                        TypechoFiles files = new TypechoFiles();
                        String md5 = getMD5(imageBytes);
                        files.setUid(uid);
                        long size = imageBytes.length;  // imageBytes.length 本身就是 long 可赋值给 Long
                        Long sizeLong = Long.valueOf(size);
                        files.setSize(sizeLong);
                        files.setName(newFileName);
                        files.setSuffix("png");
                        Long date = System.currentTimeMillis();
                        String curTime = String.valueOf(date).substring(0, 10);
                        files.setCreated(Integer.parseInt(curTime));
                        files.setSource(apiconfig.get("uploadType").toString());
                        files.setLinks(returnPath);
                        files.setMd5(md5);
                        files.setType("picture");
                        filesService.insert(files);
                        return apiconfig.get("qiniuDomain").toString() + putRet.key;
                    } catch (QiniuException ex) {
                        Response r = ex.response;
                        System.err.println(r.toString());
                        try {
                            System.err.println(r.bodyString());
                        } catch (QiniuException ex2) {
                            //ignore
                        }
                        return Result.getResultJson(1,"上传失败",null);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return Result.getResultJson(1,"上传失败",null);
                }
            }
            if(apiconfig.get("uploadType").toString().equals("ftp")){
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
                //生成文件
                File file1 = new File(dir, newFileName);
                //传输内容
                try {
                    Files.copy(inputStream, file1.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("上传文件成功！");
                } catch (IOException e) {
                    System.out.println("上传文件失败！");
                    e.printStackTrace();
                }
                //在服务器上生成新的目录
                FTPClient ftpClient = new FTPClient();

                String key = apiconfig.get("ftpBasePath").toString()+"/"+file1.getName();

                ftpClient.setConnectTimeout(1000 * 30);//设置连接超时时间
                ftpClient.setControlEncoding("utf-8");//设置ftp字符集
                //连接ftp服务器 参数填服务器的ip



                String ftpHost = apiconfig.get("ftpHost").toString();
                Integer ftpPort = Integer.parseInt(apiconfig.get("ftpPort").toString());
                ftpClient.connect(ftpHost,ftpPort);

                //进行登录 参数分别为账号 密码
                ftpClient.login(apiconfig.get("ftpUsername").toString(),apiconfig.get("ftpPassword").toString());
                //开启被动模式（按自己如何配置的ftp服务器来决定是否开启）
                ftpClient.enterLocalPassiveMode();
                //只能选择local_root下已存在的目录
                //ftpClient.changeWorkingDirectory(this.ftpBasePath);

                // 文件夹不存在时新建
                String remotePath = apiconfig.get("ftpBasePath").toString();
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
                editFile.setLog("用户"+uid+"通过ftpUpload成功上传了图片");
                //如果文件上传成功，则添加进文件表
                TypechoFiles files = new TypechoFiles();
                String md5 = getMD5(imageBytes);
                files.setUid(uid);
                long size = imageBytes.length;  // imageBytes.length 本身就是 long 可赋值给 Long
                Long sizeLong = Long.valueOf(size);
                files.setSize(sizeLong);
                files.setName(newFileName);
                files.setSuffix("png");
                Long date = System.currentTimeMillis();
                String curTime = String.valueOf(date).substring(0, 10);
                files.setCreated(Integer.parseInt(curTime));
                files.setSource(apiconfig.get("uploadType").toString());
                files.setLinks(apiconfig.get("webinfoUploadUrl").toString()+key);
                files.setMd5(md5);
                files.setType("picture");
                filesService.insert(files);
                return apiconfig.get("webinfoUploadUrl").toString()+key;
            }
            if(apiconfig.get("uploadType").toString().equals("s3")){

                AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(apiconfig.get("s3accessKeyId").toString(), apiconfig.get("s3secretAccessKey").toString());
                //为了解决包冲突，只能强行引入
                software.amazon.awssdk.regions.Region region = software.amazon.awssdk.regions.Region.of(apiconfig.get("s3region").toString());
                S3Client s3Client = S3Client.builder()
                        .region(region)
                        .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                        .endpointOverride(URI.create(apiconfig.get("s3endpoint").toString()))
                        .serviceConfiguration(S3Configuration.builder()
                                .pathStyleAccessEnabled(true)
                                .build())
                        .build();

                String bucketName = apiconfig.get("s3bucketName").toString();
                String key = dataprefix + "/" + year + "/" + month + "/" + day + "/" + newFileName;

                File localFile = null;
                try {
                    //为了解决包冲突，只能强行引入
                    software.amazon.awssdk.services.s3.model.PutObjectRequest putObjectRequest = software.amazon.awssdk.services.s3.model.PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build();

                    s3Client.putObject(putObjectRequest, software.amazon.awssdk.core.sync.RequestBody.fromInputStream(inputStream, imageBytes.length));
                    //自定义域名
                    String urlPrefix = apiconfig.get("s3UrlPrefix").toString();
                    String fileUrl = urlPrefix.endsWith("/")
                            ? urlPrefix + key
                            : urlPrefix + "/" + key;


                    editFile.setLog("用户" + uid + "通过s3Upload成功上传了图片");
                    //如果文件上传成功，则添加进文件表
                    TypechoFiles files = new TypechoFiles();
                    String md5 = getMD5(imageBytes);
                    files.setUid(uid);
                    long size = imageBytes.length;  // imageBytes.length 本身就是 long 可赋值给 Long
                    Long sizeLong = Long.valueOf(size);
                    files.setSize(sizeLong);
                    files.setName(newFileName);
                    files.setSuffix("png");
                    Long date = System.currentTimeMillis();
                    String curTime = String.valueOf(date).substring(0, 10);
                    files.setCreated(Integer.parseInt(curTime));
                    files.setSource(apiconfig.get("uploadType").toString());
                    files.setLinks(fileUrl);
                    files.setMd5(md5);
                    files.setType("picture");
                    filesService.insert(files);

                    return fileUrl;

                } finally {
                    if (s3Client != null) {
                        s3Client.close();
                    }
                    if (localFile != null && localFile.exists()) {
                        localFile.delete();
                    }
                }
            }
            editFile.setLog("用户" + uid + "上传图片失败");
            return "";

        } catch (Exception e) {
            e.printStackTrace();
            editFile.setLog("用户" + uid + "上传图片失败");
            return "";
        }
    }

    public String cosUpload(MultipartFile file, String  dataprefix, Map apiconfig,Integer uid){

        String oldFileName = file.getOriginalFilename();
        //String eName = oldFileName.substring(oldFileName.lastIndexOf("."));
        String eName = "";
        try{
            eName = oldFileName.substring(oldFileName.lastIndexOf("."));
        }catch (Exception e){
            oldFileName = oldFileName +".png";
            eName = oldFileName.substring(oldFileName.lastIndexOf("."));
        }

        //根据权限等级检查是否为图片
        Integer uploadLevel = Integer.parseInt(apiconfig.get("uploadLevel").toString());;
        
        if(uploadLevel.equals(1)){
            return Result.getResultJson(0,"管理员已关闭上传功能",null);
        }
        //检查是否是图片
        BufferedImage bi = null;
        try {
            bi = ImageIO.read(file.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(uploadLevel.equals(0)){

            if(bi == null&&!eName.equals(".WEBP")&&!eName.equals(".webp")){
                return Result.getResultJson(0,"当前只允许上传图片文件",null);
            }
        }
        if(uploadLevel.equals(2)){
            Integer isVideo = baseFull.isVideo(eName);
            if(bi == null&&!eName.equals(".WEBP")&&!eName.equals(".webp")&&!isVideo.equals(1)){
                return Result.getResultJson(0,"请上传图片或者视频文件",null);
            }
        }
        String newFileName = UUID.randomUUID()+eName;
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month=cal.get(Calendar.MONTH)+1;
        int day=cal.get(Calendar.DATE);
        COSCredentials cred = new BasicCOSCredentials(apiconfig.get("cosAccessKey").toString(), apiconfig.get("cosSecretKey").toString());
        ClientConfig clientConfig = new ClientConfig(new Region(apiconfig.get("cosBucket").toString()));
        COSClient cosclient = new COSClient(cred, clientConfig);

        String bucketName = apiconfig.get("cosBucketName").toString();

        // 简单文件上传, 最大支持 5 GB, 适用于小文件上传, 建议 20 M 以下的文件使用该接口
        // 大文件上传请参照 API 文档高级 API 上传
        File localFile = null;
        try {
            localFile = File.createTempFile("temp", null);
            try (InputStream in = file.getInputStream();
                 FileOutputStream out = new FileOutputStream(localFile)) {

                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            }
            // 指定要上传到 COS 上的路径
            String key = "/" + apiconfig.get("cosPrefix").toString() + "/" + year + "/" + month + "/" + day + "/" + newFileName;
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, localFile);
            PutObjectResult putObjectResult = cosclient.putObject(putObjectRequest);
            //return new UploadMsg(1,"上传成功",this.path + putObjectRequest.getKey());
            Map<String,String> info =new HashMap<String, String>();
            info.put("url",apiconfig.get("cosPath").toString() + putObjectRequest.getKey());
            editFile.setLog("用户"+uid+"通过cosUpload成功上传了图片");
            return Result.getResultJson(1,"上传成功",info);

        } catch (IOException e) {
            e.printStackTrace();
            editFile.setLog("用户"+uid+"通过cosUpload上传图片失败");
            return Result.getResultJson(0,"上传失败",null);
        }finally {
            // 关闭客户端(关闭后台线程)
            cosclient.shutdown();
        }
    }
    public String localUpload(MultipartFile file, String  dataprefix,Map apiconfig,Integer uid){
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
        //根据权限等级检查是否为图片
        Integer uploadLevel = Integer.parseInt(apiconfig.get("uploadLevel").toString());;
        if(uploadLevel.equals(1)){
            return Result.getResultJson(0,"管理员已关闭上传功能",null);
        }
        if(uploadLevel.equals(0)){
            //检查是否是图片
            BufferedImage bi = null;
            try {
                bi = ImageIO.read(file.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(bi == null&&!filetype.equals(".WEBP")&&!filetype.equals(".webp")){
                return Result.getResultJson(0,"当前只允许上传图片文件",null);
            }
        }
        if(uploadLevel.equals(2)){
            //检查是否是图片或视频
            BufferedImage bi = null;
            try {
                bi = ImageIO.read(file.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            Integer isVideo = baseFull.isVideo(filetype);
            if(bi == null&&!filetype.equals(".WEBP")&&!filetype.equals(".webp")&&!isVideo.equals(1)){
                return Result.getResultJson(0,"请上传图片或者视频文件",null);
            }
        }



        String decodeClassespath = null;
        //如果用户自定义了本地存储地址，则使用自定义地址，否则调用jar所在路径拼接地址
        if(apiconfig.get("localPath")!=null){
            decodeClassespath = apiconfig.get("localPath").toString();
        }else{
            /*解决文件路径中的空格问题*/
            ApplicationHome h = new ApplicationHome(getClass());
            File jarF = h.getSource();
            /* 配置文件路径 */
            String classespath = jarF.getParentFile().toString()+"/files/static";
            try {
                decodeClassespath = URLDecoder.decode(classespath,"utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                decodeClassespath = "/opt/files/static";
            }
        }
        //System.out.println(decodeClassespath);

        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month=cal.get(Calendar.MONTH)+1;
        int day=cal.get(Calendar.DATE);

        /**/
        File file1 = new File(decodeClassespath + "/upload/" + year + "/" + month + "/" + day + "/" + newfile);
        File parentDir = file1.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
        try {
            try (InputStream in = file.getInputStream();
                 FileOutputStream out = new FileOutputStream(file1)) {

                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            }
            Map<String,String> info =new HashMap<String, String>();
            info.put("url",apiconfig.get("webinfoUploadUrl").toString()+"upload"+"/"+year+"/"+month+"/"+day+"/"+newfile);
            editFile.setLog("用户"+uid+"通过localUpload成功上传了图片");
            return Result.getResultJson(1,"上传成功",info);
        } catch (IOException e) {
            e.printStackTrace();
        }
        editFile.setLog("用户"+uid+"通过localUpload上传图片失败");
        return Result.getResultJson(0,"上传失败",null);
    }
    public String ossUpload(MultipartFile file, String  dataprefix,Map apiconfig,Integer uid){
        //获取上传文件MultipartFile
        //返回上传到oss的路径
        OSS ossClient = new OSSClientBuilder().build(apiconfig.get("aliyunEndpoint").toString(), apiconfig.get("aliyunAccessKeyId").toString(),apiconfig.get("aliyunAccessKeySecret").toString());
        InputStream inputStream = null;
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
        //根据权限等级检查是否为图片
        Integer uploadLevel = Integer.parseInt(apiconfig.get("uploadLevel").toString());;
        if(uploadLevel.equals(1)){
            return Result.getResultJson(0,"管理员已关闭上传功能",null);
        }
        if(uploadLevel.equals(0)){
            //检查是否是图片或视频
            BufferedImage bi = null;
            try {
                bi = ImageIO.read(file.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(bi == null&&!eName.equals(".WEBP")&&!eName.equals(".webp")){
                return Result.getResultJson(0,"当前只允许上传图片文件",null);
            }
        }
        if(uploadLevel.equals(2)){
            //检查是否是图片
            BufferedImage bi = null;
            try {
                bi = ImageIO.read(file.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            Integer isVideo = baseFull.isVideo(eName);
            if(bi == null&&!eName.equals(".WEBP")&&!eName.equals(".webp")&&!isVideo.equals(1)){
                return Result.getResultJson(0,"请上传图片或者视频文件",null);
            }
        }
        //1.在文件名称中添加随机唯一的值
        String newFileName = UUID.randomUUID()+eName;

        // String uuid = UUID.randomUUID().toString().replaceAll("-","");
        filename = newFileName;

        String key = apiconfig.get("aliyunFilePrefix").toString()+"/"+year+"/"+month+"/"+day+"/"+newFileName;
        //调用OSS方法实现上传
        ossClient.putObject(apiconfig.get("aliyunAucketName").toString(), key, inputStream);
        ossClient.shutdown();
        String url = apiconfig.get("aliyunUrlPrefix").toString()+key;
        Map<String,String> info =new HashMap<String, String>();
        info.put("url",url);
        editFile.setLog("用户"+uid+"通过ossUpload成功上传了图片");
        return Result.getResultJson(1,"上传成功",info);
    }
    public String qiniuUpload(MultipartFile file, String  dataprefix,Map apiconfig,Integer uid){
        //获取上传文件MultipartFile
        InputStream inputStream = null;

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
        //根据权限等级检查是否为图片
        Integer uploadLevel = Integer.parseInt(apiconfig.get("uploadLevel").toString());;
        if(uploadLevel.equals(1)){
            return Result.getResultJson(0,"管理员已关闭上传功能",null);
        }
        if(uploadLevel.equals(0)){
            //检查是否是图片或视频
            BufferedImage bi = null;
            try {
                bi = ImageIO.read(file.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(bi == null&&!eName.equals(".WEBP")&&!eName.equals(".webp")){
                return Result.getResultJson(0,"当前只允许上传图片文件",null);
            }
        }
        if(uploadLevel.equals(2)){
            //检查是否是图片
            BufferedImage bi = null;
            try {
                bi = ImageIO.read(file.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            Integer isVideo = baseFull.isVideo(eName);
            if(bi == null&&!eName.equals(".WEBP")&&!eName.equals(".webp")&&!isVideo.equals(1)){
                return Result.getResultJson(0,"请上传图片或者视频文件",null);
            }
        }
        //1.在文件名称中添加随机唯一的值
        String newFileName = UUID.randomUUID()+eName;

        // String uuid = UUID.randomUUID().toString().replaceAll("-","");
        filename = newFileName;

        String key = "/app/"+year+"/"+month+"/"+day+"/"+filename;

        // 构造一个带指定Zone对象的配置类, 注意这里的Zone.zone0需要根据主机选择

        Configuration cfg = new Configuration();
        // 其他参数参考类注释
        UploadManager uploadManager = new UploadManager(cfg);
        // 生成上传凭证，然后准备上传

        try {
            Auth auth = Auth.create(apiconfig.get("qiniuAccessKey").toString(), apiconfig.get("qiniuSecretKey").toString());
            String upToken = auth.uploadToken(apiconfig.get("qiniuBucketName").toString());
            try {
                Response response = uploadManager.put(inputStream, key, upToken, null, null);
                // 解析上传成功的结果
                DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);

                String returnPath = apiconfig.get("qiniuDomain").toString() + putRet.key;
                // 这个returnPath是获得到的外链地址,通过这个地址可以直接打开图片
                Map<String,String> info =new HashMap<String, String>();
                info.put("url",returnPath);
                editFile.setLog("用户"+uid+"通过qiniuUpload成功上传了图片");
                return Result.getResultJson(1,"上传成功",info);
            } catch (QiniuException ex) {
                Response r = ex.response;
                System.err.println(r.toString());
                try {
                    System.err.println(r.bodyString());
                } catch (QiniuException ex2) {
                    //ignore
                }
                return Result.getResultJson(1,"上传失败",null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(1,"上传失败",null);
        }
    }
    public String ftpUpload(MultipartFile file, String  dataprefix,Map apiconfig,Integer uid){
        FTPClient ftpClient = new FTPClient();
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
            //根据权限等级检查是否为图片
            Integer uploadLevel = Integer.parseInt(apiconfig.get("uploadLevel").toString());;
            if(uploadLevel.equals(0)){
                //检查是否是图片
                BufferedImage bi = ImageIO.read(file.getInputStream());
                if(bi == null&&!suffix.equals(".WEBP")&&!suffix.equals(".webp")){
                    return Result.getResultJson(0,"当前只允许上传图片文件",null);
                }
            }
            if(uploadLevel.equals(2)){
                //检查是否是图片或视频
                BufferedImage bi = ImageIO.read(file.getInputStream());
                Integer isVideo = baseFull.isVideo(suffix);
                if(bi == null&&!suffix.equals(".WEBP")&&!suffix.equals(".webp")&&!isVideo.equals(1)){
                    return Result.getResultJson(0,"请上传图片或者视频文件",null);
                }
            }
            //2、使用UUID生成新文件名
            String newFileName = UUID.randomUUID() + suffix;
            //生成文件
            File file1 = new File(dir, newFileName);
            //传输内容
            try {
                try (InputStream in = file.getInputStream();
                     FileOutputStream out = new FileOutputStream(file1)) {

                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                }
                System.out.println("上传文件成功！");
            } catch (IOException e) {
                System.out.println("上传文件失败！");
                e.printStackTrace();
            }

            //在服务器上生成新的目录
            String key = apiconfig.get("ftpBasePath").toString()+"/"+file1.getName();

            ftpClient.setConnectTimeout(1000 * 30);//设置连接超时时间
            ftpClient.setControlEncoding("utf-8");//设置ftp字符集
            //连接ftp服务器 参数填服务器的ip
            String ftpHost = apiconfig.get("ftpHost").toString();
            Integer ftpPort = Integer.parseInt(apiconfig.get("ftpPort").toString());
            ftpClient.connect(ftpHost,ftpPort);

            //进行登录 参数分别为账号 密码
            ftpClient.login(apiconfig.get("ftpUsername").toString(),apiconfig.get("ftpPassword").toString());
            //开启被动模式（按自己如何配置的ftp服务器来决定是否开启）
            ftpClient.enterLocalPassiveMode();
            //只能选择local_root下已存在的目录
            //ftpClient.changeWorkingDirectory(this.ftpBasePath);

            // 文件夹不存在时新建
            String remotePath = apiconfig.get("ftpBasePath").toString();
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
            info.put("url",apiconfig.get("webinfoUploadUrl").toString()+key);
            editFile.setLog("用户"+uid+"通过ftpUpload成功上传了图片");
            return Result.getResultJson(1,"上传成功",info);

        } catch (Exception e) {
            e.printStackTrace();
            editFile.setLog("用户"+uid+"通过ftpUpload上传图片失败");
            return Result.getResultJson(0,"上传失败",null);
        }
    }

    public String s3Upload(MultipartFile file, String dataprefix, Map apiconfig, Integer uid) {
        String oldFileName = file.getOriginalFilename();
        String eName;
        try {
            eName = oldFileName.substring(oldFileName.lastIndexOf("."));
        } catch (Exception e) {
            oldFileName = oldFileName + ".png";
            eName = oldFileName.substring(oldFileName.lastIndexOf("."));
        }

        if (!checkUploadLevel(file, eName, Integer.parseInt(apiconfig.get("uploadLevel").toString()))) {
            return Result.getResultJson(0, "上传文件不符合要求", null);
        }

        String newFileName = UUID.randomUUID() + eName;
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DATE);

        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(apiconfig.get("s3accessKeyId").toString(), apiconfig.get("s3secretAccessKey").toString());
        //为了解决包冲突，只能强行引入
        software.amazon.awssdk.regions.Region region = software.amazon.awssdk.regions.Region.of(apiconfig.get("s3region").toString());
        S3Client s3Client = S3Client.builder()
                .region(region)
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .endpointOverride(URI.create(apiconfig.get("s3endpoint").toString()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();

        String bucketName = apiconfig.get("s3bucketName").toString();
        String key = dataprefix + "/" + year + "/" + month + "/" + day + "/" + newFileName;

        File localFile = null;
        try {
            localFile = File.createTempFile("temp", null);
            try (InputStream in = file.getInputStream();
                 FileOutputStream out = new FileOutputStream(localFile)) {

                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            }
            //为了解决包冲突，只能强行引入
            software.amazon.awssdk.services.s3.model.PutObjectRequest putObjectRequest = software.amazon.awssdk.services.s3.model.PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.putObject(putObjectRequest, Paths.get(localFile.getAbsolutePath()));

            Map<String, String> info = new HashMap<>();
            String urlPrefix = apiconfig.get("s3UrlPrefix").toString();
            String fileUrl = urlPrefix.endsWith("/")
                    ? urlPrefix + key
                    : urlPrefix + "/" + key;

            info.put("url", fileUrl);
            editFile.setLog("用户" + uid + "通过s3Upload成功上传了图片");
            return Result.getResultJson(1, "上传成功", info);

        } catch (IOException e) {
            editFile.setLog("用户" + uid + "通过s3Upload上传图片失败");
            return Result.getResultJson(0, "上传失败", null);
        } finally {
            if (s3Client != null) {
                s3Client.close();
            }
            if (localFile != null && localFile.exists()) {
                localFile.delete();
            }
        }
    }

    private boolean checkUploadLevel(MultipartFile file, String eName, Integer uploadLevel) {
        if (uploadLevel.equals(1)) {
            return false;
        }
        if (uploadLevel.equals(0) && !isImage(file, eName)) {
            return false;
        }
        if (uploadLevel.equals(2) && !isImage(file, eName) && !isVideo(eName)) {
            return false;
        }
        return true;
    }

    private boolean isImage(MultipartFile file, String eName) {
        try {
            BufferedImage bi = ImageIO.read(file.getInputStream());
            return bi != null || eName.equalsIgnoreCase(".webp");
        } catch (IOException e) {
            return false;
        }
    }

    private boolean isVideo(String eName) {
        // 简单判断视频文件类型，可以根据需要扩展
        return eName.equalsIgnoreCase(".mp4") || eName.equalsIgnoreCase(".avi") || eName.equalsIgnoreCase(".mov");
    }

    public static String getMD5(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data);
            // 转成十六进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5算法不存在", e);
        }
    }

}
