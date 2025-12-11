package com.RuleApi.service.impl;

import com.RuleApi.common.HttpClient;
import com.RuleApi.common.RedisHelp;
import com.RuleApi.common.UserStatus;
import com.RuleApi.common.baseFull;
import com.RuleApi.entity.*;
import com.RuleApi.service.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.tea.TeaException;
import com.aliyun.teautil.Common;
import com.aliyun.teautil.models.RuntimeOptions;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.ims.v20201229.ImsClient;
import com.tencentcloudapi.ims.v20201229.models.ImageModerationRequest;
import com.tencentcloudapi.ims.v20201229.models.ImageModerationResponse;
import com.tencentcloudapi.tms.v20201229.TmsClient;
import com.tencentcloudapi.tms.v20201229.models.TextModerationRequest;
import com.tencentcloudapi.tms.v20201229.models.TextModerationResponse;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static com.RuleApi.web.UsersController.createClient;

@Service
public class SecurityServiceImpl implements SecurityService {
    @Autowired
    private InboxService inboxService;

    @Autowired
    private UsersService usersService;

    @Autowired
    private AllconfigService allconfigService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${web.prefix}")
    private String dataprefix;

    UserStatus UStatus = new UserStatus();

    baseFull baseFull = new baseFull();

    RedisHelp redisHelp =new RedisHelp();

    HttpClient HttpClient = new HttpClient();

    @Override
    public void safetyMessage(String msg,String type){
        //向所有管理员发送警告
        try{

            TypechoUsers user = new TypechoUsers();
            user.setGroupKey("administrator");
            List<TypechoUsers> userList = usersService.selectList(user);
            for (int i = 0; i < userList.size(); i++) {
                TypechoInbox inbox = new TypechoInbox();
                Integer uid = userList.get(i).getUid();
                Long date = System.currentTimeMillis();
                String created = String.valueOf(date).substring(0,10);
                TypechoInbox insert = new TypechoInbox();
                insert.setUid(uid);
                insert.setTouid(uid);
                insert.setType(type);
                insert.setText(msg);
                insert.setCreated(Integer.parseInt(created));
                inboxService.insert(insert);
            }
            System.err.println("有用户存在违规行为，已向所有管理员发送警告");
        }catch (Exception e){
            System.err.println(e);
        }
    }

    //文本违规检测
    @Override
    public Map textViolation(String text){
        try{
            Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
            // 密钥可前往官网控制台 https://console.cloud.tencent.com/cam/capi 进行获取
            Credential cred = new Credential(apiconfig.get("cmsSecretId").toString(),apiconfig.get("cmsSecretKey").toString());
            // 实例化一个http选项，可选的，没有特殊需求可以跳过
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("tms.tencentcloudapi.com");
            // 实例化一个client选项，可选的，没有特殊需求可以跳过
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            // 实例化要请求产品的client对象,clientProfile是可选的
            TmsClient client = new TmsClient(cred, apiconfig.get("cmsRegion").toString(), clientProfile);
            // 实例化一个请求对象,每个接口都会对应一个request对象
            TextModerationRequest req = new TextModerationRequest();
            req.setContent(text);
            // 返回的resp是一个TextModerationResponse的实例，与请求对象对应
            TextModerationResponse resp = client.TextModeration(req);
            // 输出json格式的字符串回包
            String resText = TextModerationResponse.toJsonString(resp);
            System.out.println(resText);
            Map resMap = JSONObject.parseObject(JSON.parseObject(resText).toString());
            return resMap;
        } catch (TencentCloudSDKException e) {
            System.out.println(e.toString());
            return null;

        }

    }

    //图片违规检测
    @Override
    public Map picViolation(String text,Integer type){
        try{
            Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
            // 密钥可前往官网控制台 https://console.cloud.tencent.com/cam/capi 进行获取
            Credential cred = new Credential(apiconfig.get("cmsSecretId").toString(),apiconfig.get("cmsSecretKey").toString());
            // 实例化一个http选项，可选的，没有特殊需求可以跳过
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("ims.tencentcloudapi.com");
            // 实例化一个client选项，可选的，没有特殊需求可以跳过
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            // 实例化要请求产品的client对象,clientProfile是可选的
            ImsClient client = new ImsClient(cred, apiconfig.get("cmsRegion").toString(), clientProfile);
            // 实例化一个请求对象,每个接口都会对应一个request对象
            ImageModerationRequest req = new ImageModerationRequest();
            //type为0，则为base64检测，其他值为远程url检测。
            if(type.equals(0)){
                req.setFileContent(text);
            }else{
                req.setFileUrl(text);
            }
            // 返回的resp是一个ImageModerationResponse的实例，与请求对象对应
            ImageModerationResponse resp = client.ImageModeration(req);
            // 输出json格式的字符串回包
            String resText = ImageModerationResponse.toJsonString(resp);
            System.out.println(resText);
            Map resMap = JSONObject.parseObject(JSON.parseObject(resText).toString());
            return resMap;
        } catch (TencentCloudSDKException e) {
            System.out.println(e.toString());
            return null;
        }
    }

    //发送短信验证码
    @Override
    public Integer sendSMSCode(String phone,String area){
        Integer status = 0;
        try {
            Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
            Integer smsType = Integer.parseInt(apiconfig.get("smsType").toString());
            //阿里云
            if(smsType.equals(0)){
                // 替换成自己的
                String accesskeyId = apiconfig.get("codeAccessKeyId").toString();
                String accesskeySecret = apiconfig.get("codeAccessKeySecret").toString();
                //如果不是中国号码，则加上国际区号
                if(!area.equals("+86")){
                    phone = area+phone;
                    phone = phone.replace("+","");
                }
                Client client = createClient(accesskeyId, accesskeySecret,apiconfig.get("codeEndpoint").toString());
                //生成六位随机验证码
                Random random = new Random();
                String randCode = "";
                for (int i = 0; i < 6; i++) {
                    randCode += random.nextInt(10);
                }
                //存入redis并发送邮件

                JSONObject jsonObject = new JSONObject(1);
                jsonObject.put("code", randCode);
                SendSmsRequest sendSmsRequest = new SendSmsRequest()
                        .setSignName(apiconfig.get("codeSignName").toString())
                        .setTemplateCode(apiconfig.get("codeTemplate").toString())
                        .setPhoneNumbers(phone)
                        .setTemplateParam(jsonObject.toJSONString());
                RuntimeOptions runtime = new RuntimeOptions();
                String returnMessage = null;
                try {
                    // 复制代码运行请自行打印 API 的返回值
                    SendSmsResponse sendSmsResponse = client.sendSmsWithOptions(sendSmsRequest, runtime);
                    // 获取返回体code
                    String code = sendSmsResponse.getBody().getCode();
                    // 获取返回体状态
                    Integer statusCode = sendSmsResponse.getStatusCode();
                    System.out.println(JSONObject.toJSONString(sendSmsResponse));
                    redisHelp.delete(this.dataprefix + "_" + "sendSMS" + area+phone, redisTemplate);
                    redisHelp.setRedis(this.dataprefix + "_" + "sendSMS" + area+phone, randCode, 1800, redisTemplate);
                    status = 1;
                } catch (TeaException error) {
                    // 如有需要，请打印 error
                    System.err.println(Common.assertAsString(error.message));
                    status = 0;
                } catch (Exception errorMsg) {
                    TeaException error = new TeaException(errorMsg.getMessage(), errorMsg);
                    // 如有需要，请打印 error
                    System.err.println(Common.assertAsString(error.message));
                    status = 0;
                }
            }
            //短信宝
            if(smsType.equals(1)){
                if(!area.equals("+86")){
                    phone = area+phone;
                }
                //生成六位随机验证码
                Random random = new Random();
                String randCode = "";
                for (int i = 0; i < 4; i++) {
                    randCode += random.nextInt(10);
                }
                String username = apiconfig.get("smsbaoUsername").toString();    //在短信宝注册的用户名
                String APIKey = apiconfig.get("smsbaoApikey").toString();
                String content = "【" + apiconfig.get("smsbaoTemplate").toString() + "】您的验证码是"+ randCode + "。如非本人操作，请忽略本短信" ;
                String url = "http://api.smsbao.com/sms?u=" + username + "&p=" + APIKey + "&m=" + phone + "&c=" + content;
                if(!area.equals("+86")){
                    url = " http://api.smsbao.com/wsms?u=" + username + "&p=" + APIKey + "&m=" + phone + "&c=" + content;
                }
                String res = HttpClient.doGet(url);
                if (res == null) {
                    status = 0;
                }
                // res 返回的是"0\r\n"，帮我折分后取0前面的数字即可
                int number = Integer.parseInt(res.substring(0, 1));
                if (number == 0) {
                    redisHelp.delete(this.dataprefix + "_" + "sendSMS" + area+phone, redisTemplate);
                    redisHelp.setRedis(this.dataprefix + "_" + "sendSMS" + area+phone, randCode, 1800, redisTemplate);
                    status = 1;
                } else {
                    status = 0;
                }
            }
            //飞鸽云通信
            if (smsType.equals(2)) {
                long timestamp = System.currentTimeMillis() / 1000;
                String nonce = baseFull.createRandomStr(16);

                String API_URL = apiconfig.get("feigeApiUrl").toString();
                String API_KEY = apiconfig.get("feigeApiKey").toString();
                String SECRET = apiconfig.get("feigeSecret").toString();
                String sign_id = apiconfig.get("feigeSignId").toString();
                Integer templateId = Integer.parseInt(apiconfig.get("feigeTemplateId").toString());
                //String signName = apiconfig.get("feigeSignName").toString(); // 可选，若模板绑定签名可不传
                // ===== 生成验证码 =====
                Random random = new Random();
                String randCode = "";
                for (int i = 0; i < 6; i++) {
                    randCode += random.nextInt(10);
                }

                //飞鸽云通信有新版和旧版区别
                int code = 0;
                String msg = "";
                Integer feigeType = 1;
                if(apiconfig.get("feigeType")!=null){
                    feigeType = Integer.parseInt(apiconfig.get("feigeType").toString());
                }
                if(feigeType.equals(1)){
                    // ===== 生成签名 =====
                    String raw = API_KEY + SECRET + timestamp + nonce;
                    String sign = sha256(raw);
                    API_URL = API_URL+"/sendsms/template/send";
                    // ===== 处理手机号 =====
                    if (!area.equals("+86")) {
                        phone = area + phone;
                        phone = phone.replace("+", "");
                    }

                    // ===== 组装 messages 数组 =====
                    JSONObject paramObj = new JSONObject();
                    paramObj.put("code", randCode); // 假设模板变量名是 code

                    JSONObject msgObj = new JSONObject();
                    msgObj.put("phone_number", phone);
                    msgObj.put("params", paramObj);

                    List<JSONObject> msgList = new ArrayList<>();
                    msgList.add(msgObj);

                    JSONObject body = new JSONObject();
                    body.put("template_id", templateId);
                    body.put("messages", msgList);

                    // ===== 建立连接 =====
                    URL url = new URL(API_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setDoInput(true);
                    conn.setUseCaches(false);
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                    // 设置公共请求头
                    conn.setRequestProperty("X-Api-Key", API_KEY);
                    conn.setRequestProperty("X-Timestamp", String.valueOf(timestamp));
                    conn.setRequestProperty("X-Nonce", nonce);
                    conn.setRequestProperty("X-Sign", sign);

                    // ===== 发送请求体 =====
                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(body.toJSONString().getBytes(StandardCharsets.UTF_8));
                        os.flush();
                    }

                    // ===== 读取响应 =====
                    int httpCode = conn.getResponseCode();
                    InputStream inputStream = (httpCode == 200) ? conn.getInputStream() : conn.getErrorStream();
                    String response = readStream(inputStream);
                    conn.disconnect();

                    // ===== 解析返回 =====
                    JSONObject resJson = JSONObject.parseObject(response);
                    code = resJson.getInteger("code");
                    msg = resJson.getString("msg");
                    if (code == 0) {
                        // 请求成功
                        redisHelp.delete(this.dataprefix + "_" + "sendSMS" + area + phone, redisTemplate);
                        redisHelp.setRedis(this.dataprefix + "_" + "sendSMS" + area + phone, randCode, 1800, redisTemplate);
                        status = 1;
                    } else {
                        System.err.println("飞鸽短信发送失败：" + msg);
                        status = 0;
                    }
                }
                if (feigeType.equals(0)) {
                    // 拼接请求地址（不要带参数）
                    if (!area.equals("+86")) {
                        API_URL = API_URL + "/inter/send";
                        phone = area + phone;
                        phone = phone.replace("+", "");
                    } else {
                        API_URL = API_URL + "/sms/template";
                    }

                    // 生成 POST 参数
                    StringBuilder paramBuilder = new StringBuilder();
                    paramBuilder.append("apikey=").append(API_KEY);
                    paramBuilder.append("&secret=").append(SECRET);
                    paramBuilder.append("&content=").append(randCode);
                    paramBuilder.append("&mobile=").append(phone);
                    paramBuilder.append("&template_id=").append(templateId);
                    paramBuilder.append("&send_time=").append(timestamp);

                    if (area.equals("+86")) {
                        // 国内短信要带 sign_id
                        paramBuilder.append("&sign_id=").append(sign_id);
                    }

                    String params = paramBuilder.toString();

                    // 发送 POST 请求
                    String res = HttpClient.doPost(API_URL, params);

                    if (res == null || res.trim().isEmpty()) {
                        System.out.println("短信接口未返回任何值，可能是接口错误，或服务器无法访问。");
                        return 0;
                    }

                    System.out.println("飞鸽短信返回: " + res);
                    JSONObject json = JSONObject.parseObject(res);
                    code = json.getIntValue("code");
                    msg = json.getString("msg");

                    if (code == 0) {
                        // 请求成功
                        redisHelp.delete(this.dataprefix + "_" + "sendSMS" + area + phone, redisTemplate);
                        redisHelp.setRedis(this.dataprefix + "_" + "sendSMS" + area + phone, randCode, 1800, redisTemplate);
                        status = 1;
                    } else {
                        System.err.println("飞鸽短信发送失败：" + msg);
                        status = 0;
                    }
                }

            }

        }catch (Exception e){
            e.printStackTrace();
            return 0;
        }
        return  status;
    }
    /**
     * 计算SHA256签名（小写）
     */
    private String sha256(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            String hexStr = Integer.toHexString(0xff & b);
            if (hexStr.length() == 1) hex.append('0');
            hex.append(hexStr);
        }
        return hex.toString().toLowerCase();
    }
    // 在类里添加或替换为这个方法（返回 String）
    private static String readStream(InputStream inputStream) throws IOException {
        if (inputStream == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            return result.toString();
        }
    }
}
