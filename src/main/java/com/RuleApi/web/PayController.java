package com.RuleApi.web;

import com.RuleApi.annotation.LoginRequired;
import com.RuleApi.common.*;
import com.RuleApi.entity.*;
import com.RuleApi.service.*;
import com.RuleApi.dto.AppleVerifyRequest;
import com.RuleApi.dto.AppleVerifyResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

@Controller
@RequestMapping(value = "/pay")
public class PayController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UsersService usersService;

    @Autowired
    private PayPackageService payPackageService;

    @Autowired
    private PaylogService paylogService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private PaykeyService paykeyService;

    @Autowired
    private AllconfigService allconfigService;


    @Value("${web.prefix}")
    private String dataprefix;

    @Value("${mybatis.configuration.variables.prefix}")
    private String prefix;

    RedisHelp redisHelp =new RedisHelp();
    ResultAll Result = new ResultAll();
    HttpClient HttpClient = new HttpClient();
    UserStatus UStatus = new UserStatus();
    RestTemplate restTemplate = new RestTemplate();

    /**
     * Apple客户端支付成功后调用此接口，服务端验证苹果收据
     */
    @PostMapping("/verifyReceipt")
    public ResponseEntity<Map<String, Object>> verifyReceipt(@RequestBody AppleVerifyRequest req) {
        Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);

        String prodUrl = "";
        if(apiconfig.get("appleProdUrl")!=null){
            prodUrl = apiconfig.get("appleProdUrl").toString();
        }
        String sandboxUrl = "";
        if(apiconfig.get("appleSandboxUrl")!=null){
            sandboxUrl = apiconfig.get("appleSandboxUrl").toString();
        }
        String sharedSecret = null;
        if(apiconfig.get("appleSharedSecret")!=null){
            sharedSecret = apiconfig.get("appleSharedSecret").toString();
        }
        String bundleId = "";
        if(apiconfig.get("appleBundleId")!=null){
            bundleId = apiconfig.get("appleBundleId").toString();
        }
        //初始化配置信息

        Map<String, Object> result = new HashMap<>();
        String uid = req.getUserId();
        Integer payType = 0;
        if(req.getPayType()!=null){
            payType = req.getPayType();
        }
        String receipt = req.getReceipt();
        if (receipt == null || receipt.isEmpty()) {
            result.put("success", false);
            result.put("msg", "receipt empty");
            return ResponseEntity.ok(result);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("receipt-data", receipt);
        if (sharedSecret != null && !sharedSecret.isEmpty()) {
            payload.put("password", sharedSecret);
        }

        AppleVerifyResponse resp = postToApple(prodUrl, payload);
        if (resp == null) {
            result.put("success", false);
            result.put("msg", "apple verify failed");
            return ResponseEntity.ok(result);
        }

        if (resp.getStatus() == 21007) {
            resp = postToApple(sandboxUrl, payload);
        }

        if (resp.getStatus() == 0) {
            Map<String, Object> receiptMap = resp.getReceipt();
            if (receiptMap != null && receiptMap.containsKey("bundle_id")) {
                String respBundleId = receiptMap.get("bundle_id").toString();
                if (!bundleId.equals(respBundleId)) {
                    result.put("success", false);
                    result.put("msg", "bundle_id mismatch");
                    return ResponseEntity.ok(result);
                }
            }
            if (receiptMap != null && receiptMap.containsKey("in_app")) {
                List<Map<String, Object>> inAppList = (List<Map<String, Object>>) receiptMap.get("in_app");

                if (inAppList != null && !inAppList.isEmpty()) {
                    Long date = System.currentTimeMillis();
                    String created = String.valueOf(date).substring(0,10);
                    Map<String, Object> lastPurchase = inAppList.get(inAppList.size() - 1);

                    String transactionId = (String) lastPurchase.get("transaction_id");
                    String productId = (String) lastPurchase.get("product_id");
                    String quantity = (String) lastPurchase.get("quantity");

                    //先生成订单，根据支付类型不同，支付套餐还是VIP支付
                    //根据 productId 获取对应金额，或获取VIP套餐

                    Integer amount = 0;
                    Integer integral = 0;
                    TypechoPayPackage payPackage = new TypechoPayPackage();
                    payPackage.setAppleProductId(productId);
                    List<TypechoPayPackage> payPackageList = payPackageService.selectList(payPackage);
                    if(payPackageList.size() > 0){
                        payPackage = payPackageList.get(0);
                        amount = payPackage.getGold();
                        integral = payPackage.getIntegral();
                    }else{
                        result.put("success", false);
                        result.put("msg", "未找到支付套餐");
                        return ResponseEntity.ok(result);
                    }
                    //存入充值记录
                    //支付完成后，写入充值日志
                    String trade_no =transactionId;
                    String out_trade_no = transactionId;

                    TypechoPaylog paylog = new TypechoPaylog();
                    //根据订单和发起人，是否有数据库对应，来是否充值成功
                    paylog.setOutTradeNo(out_trade_no);
                    paylog.setStatus(1);
                    List<TypechoPaylog> logList= paylogService.selectList(paylog);
                    if(logList.size() > 0){
                        result.put("success", false);
                        result.put("msg", "订单已存在！");
                        return ResponseEntity.ok(result);
                    }else{

                        Integer TotalAmount = amount;
                        paylog.setStatus(1);
                        paylog.setCreated(Integer.parseInt(created));
                        paylog.setUid(Integer.parseInt(uid));
                        paylog.setOutTradeNo(out_trade_no);
                        paylog.setTotalAmount(TotalAmount.toString());
                        paylog.setPaytype("applePay");
                        paylog.setSubject("苹果支付");
                        paylogService.insert(paylog);
                        //订单修改后，插入用户表
                        TypechoUsers users = usersService.selectByKey(uid);
                        Integer oldAssets = users.getAssets();
                        Integer oldPoints = users.getPoints();
                        Integer assets = oldAssets + amount;
                        Integer points = oldPoints + integral;
                        users.setAssets(assets);
                        users.setPoints(points);
                        usersService.update(users);
                    }


                }
            }

            // 发货 / 业务逻辑
            result.put("success", true);
            return ResponseEntity.ok(result);
        } else {
            result.put("success", false);
            result.put("status", resp.getStatus());
            result.put("msg", "apple verify failed with status");
            return ResponseEntity.ok(result);
        }
    }

    /**
     * 向 Apple 服务器发送验证请求
     */
    private AppleVerifyResponse postToApple(String url, Map<String, Object> payload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<AppleVerifyResponse> response =
                    restTemplate.postForEntity(url, entity, AppleVerifyResponse.class);
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 支付宝扫码支付
     * @return 支付宝生成的订单信息
     */
    @RequestMapping(value = "/scancodePay")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String scancodepay(@RequestParam(value = "num", required = false) String num,
                              @RequestParam(value = "token", required = false) String  token,
                              @RequestParam(value = "packageId", required = false ,defaultValue = "0") Integer  packageId) throws AlipayApiException {

        //当packageId为0时，以num参数结算，当packageId不为0时，以套餐价格结算。
        TypechoPayPackage payPackage = new TypechoPayPackage();
        if(packageId.equals(0)){
            Pattern pattern = Pattern.compile("[0-9]*");
            if(!pattern.matcher(num).matches()){
                return Result.getResultJson(0,"充值金额必须为正整数",null);
            }
            if(Integer.parseInt(num) <= 0){
                return Result.getResultJson(0,"充值金额不正确",null);
            }
        }else{
            payPackage = payPackageService.selectByKey(packageId);
            if(payPackage == null){
                return Result.getResultJson(0,"套餐不存在",null);
            }
            num = payPackage.getPrice().toString();
        }
        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid =Integer.parseInt(map.get("uid").toString());
        //登录情况下，恶意充值攻击拦截
        Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
        Integer minPayNum = 5;
        if(apiconfig.get("minPayNum")!=null){
            minPayNum = Integer.parseInt(apiconfig.get("minPayNum").toString());
        }
        if(Integer.parseInt(num) < minPayNum){
            return Result.getResultJson(0,"最小充值金额为："+minPayNum,null);
        }
        Integer switchAlipay = 1;
        if(apiconfig.get("switchAlipay")!=null){
            switchAlipay = Integer.parseInt(apiconfig.get("switchAlipay").toString());
        }
        if(switchAlipay.equals(0)){
            return Result.getResultJson(0,"支付渠道已关闭",null);
        }
        if(apiconfig.get("banRobots").toString().equals("1")) {
            String isSilence = redisHelp.getRedis(this.dataprefix + "_" + uid + "_silence", redisTemplate);
            if (isSilence != null) {
                return Result.getResultJson(0, "你的操作太频繁了，请稍后再试", null);
            }
            String isRepeated = redisHelp.getRedis(this.dataprefix + "_" + uid + "_isRepeated", redisTemplate);
            if (isRepeated == null) {
                redisHelp.setRedis(this.dataprefix + "_" + uid + "_isRepeated", "1", 2, redisTemplate);
            } else {
                Integer frequency = Integer.parseInt(isRepeated) + 1;
                if (frequency == 3) {
                    securityService.safetyMessage("用户ID：" + uid + "，在微信充值接口疑似存在攻击行为，请及时确认处理。", "system");
                    redisHelp.setRedis(this.dataprefix + "_" + uid + "_silence", "1", 900, redisTemplate);
                    return Result.getResultJson(0, "你的请求存在恶意行为，15分钟内禁止操作！", null);
                } else {
                    redisHelp.setRedis(this.dataprefix + "_" + uid + "_isRepeated", frequency.toString(), 3, redisTemplate);
                }
                return Result.getResultJson(0, "你的操作太频繁了", null);
            }
        }
        //攻击拦截结束

        final String APPID = apiconfig.get("alipayAppId").toString();
        String RSA2_PRIVATE = apiconfig.get("alipayPrivateKey").toString();
        String ALIPAY_PUBLIC_KEY = apiconfig.get("alipayPublicKey").toString();

        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");//可以方便地修改日期格式
        String timeID = dateFormat.format(now);
        String order_no=timeID+"scancodealipay";
        String body = "";


        String total_fee=num;  //真实金钱

        AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do", APPID, RSA2_PRIVATE, "json",
                "UTF-8", ALIPAY_PUBLIC_KEY, "RSA2"); //获得初始化的AlipayClient
        AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();//创建API对应的request类
        request.setBizContent("{" +
                "    \"out_trade_no\":\"" + order_no + "\"," +
                "    \"total_amount\":\"" + total_fee + "\"," +
                "    \"body\":\"" + body + "\"," +
                "    \"subject\":\"商品购买\"," +
                "    \"timeout_express\":\"90m\"}");//设置业务参数
        request.setNotifyUrl(apiconfig.get("alipayNotifyUrl").toString());
        AlipayTradePrecreateResponse response = alipayClient.execute(request);//通过alipayClient调用API，获得对应的response类
        System.out.print(response.getBody());

        //根据response中的结果继续业务逻辑处理
        if (response.getMsg().equals("Success")) {
            //先生成订单
            Long date = System.currentTimeMillis();
            String created = String.valueOf(date).substring(0,10);
            TypechoPaylog paylog = new TypechoPaylog();
            Integer TotalAmount = Integer.parseInt(total_fee) * Integer.parseInt(apiconfig.get("scale").toString());
            paylog.setStatus(0);
            paylog.setCreated(Integer.parseInt(created));
            paylog.setUid(uid);
            paylog.setOutTradeNo(order_no);
            paylog.setPaytype("scancodePay");
            if(packageId.equals(0)){
                paylog.setSubject("支付宝支付");
                paylog.setTotalAmount(TotalAmount.toString());
            }else{
                TotalAmount = payPackage.getGold();
                paylog.setTotalAmount(TotalAmount.toString());
                paylog.setSubject("支付宝支付-套餐充值");
                paylog.setPackageId(packageId);
            }
            paylogService.insert(paylog);
            //再返回二维码
            String qrcode = response.getQrCode();
            JSONObject toResponse = new JSONObject();
            toResponse.put("code" ,1);
            toResponse.put("data" , qrcode);
            toResponse.put("pid" , paylog.getPid());
            toResponse.put("msg"  , "获取成功");
            return toResponse.toString();
        } else{
            JSONObject toResponse = new JSONObject();
            toResponse.put("code" ,0);
            toResponse.put("data" , "");
            toResponse.put("msg"  , "请求失败");
            return toResponse.toString();
        }

    }

    @RequestMapping(value = "/notify", method = RequestMethod.POST)
    @ResponseBody
    public String notify(HttpServletRequest request,
                         HttpServletResponse response) throws AlipayApiException {
        Map<String, String> params = new HashMap<String, String>();
        Map requestParams = request.getParameterMap();
        for (Iterator iter = requestParams.keySet().iterator(); iter.hasNext(); ) {
            String name = (String) iter.next();
            String[] values = (String[]) requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
            }
            params.put(name, valueStr);
        }
        System.err.println(params);
        Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
        String CHARSET = "UTF-8";
        //支付宝公钥
        String ALIPAY_PUBLIC_KEY = apiconfig.get("alipayPublicKey").toString();

        String tradeStatus = request.getParameter("trade_status");
        boolean flag = AlipaySignature.rsaCheckV1(params, ALIPAY_PUBLIC_KEY, CHARSET, "RSA2");

        if (flag) {//验证成功

            if (tradeStatus.equals("TRADE_FINISHED") || tradeStatus.equals("TRADE_SUCCESS")) {
                //支付完成后，写入充值日志
                String trade_no = params.get("trade_no");
                String out_trade_no = params.get("out_trade_no");
                String total_amount = params.get("total_amount");
                Integer scale = Integer.parseInt(apiconfig.get("scale").toString());
                Integer gold = Double.valueOf(total_amount).intValue() * scale;
                Integer integral = 0;
                Long date = System.currentTimeMillis();
                String created = String.valueOf(date).substring(0,10);
                TypechoPaylog paylog = new TypechoPaylog();
                //根据订单和发起人，是否有数据库对应，来是否充值成功
                paylog.setOutTradeNo(out_trade_no);
                paylog.setStatus(0);
                List<TypechoPaylog> logList= paylogService.selectList(paylog);
                if(logList.size() > 0){
                    Integer pid = logList.get(0).getPid();
                    Integer uid = logList.get(0).getUid();
                    paylog.setStatus(1);
                    paylog.setTradeNo(trade_no);
                    paylog.setPid(pid);
                    paylog.setCreated(Integer.parseInt(created));
                    paylog.setPackageId(logList.get(0).getPackageId());
                    //如果是套餐充值，且套餐存在，则赋值套餐
                    if(!paylog.getPackageId().equals(0)){
                        TypechoPayPackage payPackage = payPackageService.selectByKey(paylog.getPackageId());
                        if(payPackage != null){
                            gold = payPackage.getGold();
                            integral = payPackage.getIntegral();
                        }
                    }

                    paylogService.update(paylog);
                    //订单修改后，插入用户表
                    TypechoUsers users = usersService.selectByKey(uid);
                    Integer oldAssets = users.getAssets();
                    Integer assets = oldAssets + gold;
                    Integer oldPoints = users.getPoints();
                    Integer points = oldPoints + integral;
                    users.setAssets(assets);
                    users.setPoints(points);
                    usersService.update(users);
                }else{
                    System.err.println("数据库不存在订单");
                    return "fail";
                }
            }
            return "success";
        } else {//验证失败
            return "fail";
        }
    }
    /**
     * 二维码生成
     * */
    @RequestMapping(value = "/qrCode")
    @ResponseBody
    public void getQRCode(String codeContent,@RequestParam(value = "token", required = false) String  token, HttpServletResponse response) {
        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        if(uStatus==0){
            System.err.println("用户未的登陆");
        }
        System.out.println("codeContent=" + codeContent);
        try {
            /*
             * 调用工具类生成二维码并输出到输出流中
             */
            QRCodeUtil.createCodeToOutputStream(codeContent, response.getOutputStream());
            System.out.println("成功生成二维码!");
        } catch (IOException e) {
            System.out.println("发生错误");
        }
    }
    /**
     * 充值记录
     * */
    @RequestMapping(value = "/payLogList")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String payLogList (@RequestParam(value = "token", required = false) String  token) {

        String page = "1";
        String limit = "30";
        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid =Integer.parseInt(map.get("uid").toString());
        Integer total = 0;
        TypechoPaylog query = new TypechoPaylog();
        query.setUid(uid);
        total = paylogService.total(query);
        List<TypechoPaylog> list = new ArrayList();
        List cacheList = redisHelp.getList(this.dataprefix+"_"+"payLogList_"+page+"_"+limit+"_"+uid,redisTemplate);
        try{
            if(cacheList.size()>0){
                list = cacheList;
            }else {
                PageList<TypechoPaylog> pageList = paylogService.selectPage(query, Integer.parseInt(page), Integer.parseInt(limit));
                list = pageList.getList();
                redisHelp.delete(this.dataprefix+"_"+"payLogList_"+page+"_"+limit+"_"+uid, redisTemplate);
                redisHelp.setList(this.dataprefix+"_"+"payLogList_"+page+"_"+limit+"_"+uid, list, 5, redisTemplate);
            }
        }catch (Exception e){
            if(cacheList.size()>0){
                list = cacheList;
            }
        }
        JSONObject response = new JSONObject();
        response.put("code" , 1);
        response.put("msg"  , "");
        response.put("data" , null != list ? list : new JSONArray());
        response.put("count", list.size());
        response.put("total", total);
        return response.toString();
    }
    /**
     * 财务记录(管理员)
     * */
    @RequestMapping(value = "/financeList")
    @ResponseBody
    @LoginRequired(purview = "2")
    public String financeList (@RequestParam(value = "searchParams", required = false) String  searchParams,
                               @RequestParam(value = "token", required = false) String  token,
                               @RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                               @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit) {

        Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
        Integer total = 0;
        TypechoPaylog query = new TypechoPaylog();
        if (StringUtils.isNotBlank(searchParams)) {
            JSONObject object = JSON.parseObject(searchParams);
            query = object.toJavaObject(TypechoPaylog.class);
            total = paylogService.total(query);
        }
        PageList<TypechoPaylog> pageList = paylogService.selectPage(query, page, limit);
        List<TypechoPaylog> list = pageList.getList();
        JSONObject response = new JSONObject();
        response.put("code" , 1);
        response.put("msg"  , "");
        response.put("data" , null != list ? list : new JSONArray());
        response.put("count", list.size());
        response.put("total", total);
        return response.toString();
    }
    /**
     * 财务统计(管理员)
     * */
    @RequestMapping(value = "/financeTotal")
    @ResponseBody
    @LoginRequired(purview = "2")
    public String financeTotal (@RequestParam(value = "token", required = false) String  token){
        Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
        Map financeData = new HashMap<String, Integer>();
        Integer recharge = jdbcTemplate.queryForObject("SELECT SUM(total_amount) FROM `"+prefix+"_paylog` where `status` = 1 and (`subject` = '扫码支付' or `subject` = '微信APP支付' or `subject` = '卡密充值' or `subject` = '系统充值');", Integer.class);
        Integer trade = jdbcTemplate.queryForObject("SELECT SUM(total_amount) FROM `"+prefix+"_paylog` where `status` = 1 and (`paytype` = 'buyshop' or `paytype` = 'buyvip' or `paytype` = 'toReward' or `paytype` = 'buyAds');", Integer.class);
        Integer withdraw = jdbcTemplate.queryForObject("SELECT SUM(total_amount) FROM `"+prefix+"_paylog` where `status` = 1 and (`paytype` = 'withdraw' or `subject` = '系统扣款');", Integer.class);
        Integer income = jdbcTemplate.queryForObject("SELECT SUM(total_amount) FROM `"+prefix+"_paylog` where `status` = 1 and (`paytype` = 'clock' or `paytype` = 'sellshop' or `paytype` = 'reward' or `paytype` = 'adsGift');", Integer.class);
        if(trade!=null){
            trade = trade * -1;
        }
        if(withdraw!=null){
            withdraw = withdraw * -1;
        }
        financeData.put("recharge",recharge);
        financeData.put("trade",trade);
        financeData.put("withdraw",withdraw);
        financeData.put("income",income);
        JSONObject response = new JSONObject();
        response.put("code" ,1 );
        response.put("data" , financeData);
        response.put("msg"  , "");
        return response.toString();
    }

    /**
     * 微信支付（支持 App / JSAPI / Native / H5）
     */
    @RequestMapping(value = "/WxPay")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String wxAdd(HttpServletRequest request,
                        @RequestParam(value = "price", required = false) Integer price,
                        @RequestParam(value = "token", required = false) String token,
                        @RequestParam(value = "packageId", required = false ,defaultValue = "0") Integer packageId,
                        @RequestParam(value = "tradeType", required = false ,defaultValue = "NATIVE") String tradeType,
                        @RequestParam(value = "openidOrWapUrl", required = false) String openidOrWapUrl) throws Exception {
        try {
            // 处理套餐或自定义金额
            TypechoPayPackage payPackage = new TypechoPayPackage();
            if(packageId.equals(0)){
                if(price == null || price <= 0){
                    return Result.getResultJson(0,"充值金额不正确",null);
                }
            } else {
                payPackage = payPackageService.selectByKey(packageId);
                if(payPackage == null){
                    return Result.getResultJson(0,"套餐不存在",null);
                }
                price = payPackage.getPrice();
            }

            Map map = redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer uid = Integer.parseInt(map.get("uid").toString());

            // 登录情况下，恶意充值攻击拦截
            Map<String,Object> apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
            Integer minPayNum = 5;
            if(apiconfig.get("minPayNum")!=null){
                minPayNum = Integer.parseInt(apiconfig.get("minPayNum").toString());
            }
            if(price < minPayNum){
                return Result.getResultJson(0,"最小充值金额为："+minPayNum,null);
            }
            Integer switchWxpay = 1;
            if(apiconfig.get("switchWxpay")!=null){
                switchWxpay = Integer.parseInt(apiconfig.get("switchWxpay").toString());
            }
            if(switchWxpay.equals(0)){
                return Result.getResultJson(0,"支付渠道已关闭",null);
            }
            if("1".equals(apiconfig.get("banRobots").toString())) {
                String isSilence = redisHelp.getRedis(this.dataprefix+"_"+uid+"_silence",redisTemplate);
                if(isSilence!=null){
                    return Result.getResultJson(0,"你的操作太频繁了，请稍后再试",null);
                }
                String isRepeated = redisHelp.getRedis(this.dataprefix+"_"+uid+"_isRepeated",redisTemplate);
                if(isRepeated==null){
                    redisHelp.setRedis(this.dataprefix+"_"+uid+"_isRepeated","1",2,redisTemplate);
                } else {
                    Integer frequency = Integer.parseInt(isRepeated) + 1;
                    if(frequency >= 3){
                        securityService.safetyMessage("用户ID："+uid+"，在微信充值接口疑似存在攻击行为，请及时确认处理。","system");
                        redisHelp.setRedis(this.dataprefix+"_"+uid+"_silence","1",900,redisTemplate);
                        return Result.getResultJson(0,"你的请求存在恶意行为，15分钟内禁止操作！",null);
                    } else {
                        redisHelp.setRedis(this.dataprefix+"_"+uid+"_isRepeated",frequency.toString(),3,redisTemplate);
                        return Result.getResultJson(0,"你的操作太频繁了",null);
                    }
                }
            }

            // 商户订单号
            String outTradeNo = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + "WxPay";

            // 调用统一下单
            Map<String, String> data = WeChatPayUtils.unifiedOrder(
                    price.toString(),
                    packageId.equals(0) ? "微信商品下单" : "微信支付-套餐充值",
                    outTradeNo,
                    tradeType,
                    apiconfig,
                    openidOrWapUrl
            );
            if(data != null && "200".equals(data.get("code"))){
                // 生成订单记录
                Long date = System.currentTimeMillis();
                String created = String.valueOf(date).substring(0,10);
                TypechoPaylog paylog = new TypechoPaylog();

                Integer scale = Integer.parseInt(apiconfig.get("scale").toString());
                Integer TotalAmount = price * scale;

                paylog.setStatus(0);
                paylog.setCreated(Integer.parseInt(created));
                paylog.setUid(uid);
                paylog.setOutTradeNo(outTradeNo);
                paylog.setPaytype("WxPay");

                if(packageId.equals(0)){
                    paylog.setSubject("微信支付");
                    paylog.setTotalAmount(TotalAmount.toString());
                } else {
                    TotalAmount = payPackage.getGold();
                    paylog.setTotalAmount(TotalAmount.toString());
                    paylog.setSubject("微信支付-套餐充值");
                    paylog.setPackageId(packageId);
                }
                paylogService.insert(paylog);

                // 返回支付信息
                data.put("outTradeNo", outTradeNo);
                data.put("totalAmount", price.toString());

                JSONObject toResponse = new JSONObject();
                toResponse.put("code" ,1);
                toResponse.put("pid" , paylog.getPid());
                toResponse.put("data" , data);
                toResponse.put("msg"  , "获取成功");
                return toResponse.toString();
            } else {
                JSONObject toResponse = new JSONObject();
                toResponse.put("code", 0);
                toResponse.put("data", "");
                toResponse.put("msg", "请求失败：" + (data != null ? data.get("msg") : "未知错误"));
                return toResponse.toString();
            }

        } catch (Exception e){
            e.printStackTrace();
            JSONObject toResponse = new JSONObject();
            toResponse.put("code", 0);
            toResponse.put("data", "");
            toResponse.put("msg", "请求异常");
            return toResponse.toString();
        }
    }
    /**
     * 微信回调（支持 App、JSAPI、Native 扫码、H5）
     */
    @RequestMapping(value = "/wxPayNotify")
    @ResponseBody
    public String wxPayNotify(
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        Map apiconfig = UStatus.getConfig(this.dataprefix, allconfigService, redisTemplate);

        // 解析微信回调请求
        Map<String, Object> map = new ObjectMapper().readValue(request.getInputStream(), Map.class);
        Map<String, Object> dataMap = WeChatPayUtils.paramDecodeForAPIV3(map, apiconfig);

        // 判断支付是否成功
        if ("SUCCESS".equals(dataMap.get("trade_state"))) {
            String trade_no = String.valueOf(dataMap.get("transaction_id"));
            String out_trade_no = String.valueOf(dataMap.get("out_trade_no"));
            String trade_type = String.valueOf(dataMap.getOrDefault("trade_type", "NATIVE")); // 默认为 NATIVE

            Long date = System.currentTimeMillis();
            String created = String.valueOf(date).substring(0, 10);

            TypechoPaylog paylog = new TypechoPaylog();
            paylog.setOutTradeNo(out_trade_no);
            paylog.setStatus(0);

            // 查询订单
            List<TypechoPaylog> logList = paylogService.selectList(paylog);
            if (logList.size() > 0) {
                TypechoPaylog oldLog = logList.get(0);
                Integer pid = oldLog.getPid();
                Integer uid = oldLog.getUid();

                paylog.setStatus(1);
                paylog.setTradeNo(trade_no);
                paylog.setPid(pid);
                paylog.setCreated(Integer.parseInt(created));
                paylog.setPackageId(oldLog.getPackageId());

                // 更新订单及用户资产
                String total_amount = oldLog.getTotalAmount();
                Integer gold = Double.valueOf(total_amount).intValue();
                Integer integral = 0;

                if (!paylog.getPackageId().equals(0)) {
                    TypechoPayPackage payPackage = payPackageService.selectByKey(paylog.getPackageId());
                    if (payPackage != null) {
                        gold = payPackage.getGold();
                        integral = payPackage.getIntegral();
                    }
                }

                paylogService.update(paylog);

                TypechoUsers users = usersService.selectByKey(uid);
                users.setAssets(users.getAssets() + gold);
                users.setPoints(users.getPoints() + integral);
                usersService.update(users);

            } else {
                System.err.println("数据库不存在订单");
                Map<String, String> returnMap = new HashMap<>();
                returnMap.put("code", "FALL");
                returnMap.put("message", "");
                String returnXml = WeChatPayUtils.mapToXml(returnMap);
                return returnXml;
            }

            // 返回微信成功响应（保持原格式）
            Map<String, String> returnMap = new HashMap<>();
            returnMap.put("code", "SUCCESS");
            returnMap.put("message", "成功");
            String returnXml = WeChatPayUtils.mapToXml(returnMap);
            return returnXml;

        } else {
            System.err.println("微信支付失败");
            Map<String, String> returnMap = new HashMap<>();
            returnMap.put("code", "FALL");
            returnMap.put("message", "");
            String returnXml = WeChatPayUtils.mapToXml(returnMap);
            return returnXml;
        }
    }


    /**
     * 卡密充值相关
     * **/

    /**
     * 创建卡密
     * **/
    @RequestMapping(value = "/madetoken")
    @ResponseBody
    @LoginRequired(purview = "2")
    public String madetoken(@RequestParam(value = "price", required = false) Integer  price,@RequestParam(value = "num", required = false) Integer  num,@RequestParam(value = "token", required = false) String  token) {
        try{
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            if(num>100){
                num = 100;
            }
            if(price<1){
                return Result.getResultJson(0, "充值码价格不能小于1", null);
            }
            Long date = System.currentTimeMillis();
            String curTime = String.valueOf(date).substring(0, 10);
            //循环生成卡密
            for (int i = 0; i < num; i++) {
                TypechoPaykey paykey = new TypechoPaykey();
                String value = UUID.randomUUID()+"";
                paykey.setValue(value);
                paykey.setStatus(0);
                paykey.setPrice(price);
                paykey.setCreated(Integer.parseInt(curTime));
                paykey.setUid(-1);
                paykeyService.insert(paykey);
            }
            JSONObject response = new JSONObject();
            response.put("code" , 1);
            response.put("msg"  , "生成卡密成功");
            return response.toString();
        }catch (Exception e){
            JSONObject response = new JSONObject();
            response.put("code" , 1);
            response.put("msg"  , "生成卡密失败");
            return response.toString();
        }
    }
    /***
     * 卡密列表
     *
     */
    @RequestMapping(value = "/tokenPayList")
    @ResponseBody
    @LoginRequired(purview = "2")
    public String tokenPayList (@RequestParam(value = "searchParams", required = false) String  searchParams,
                            @RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                            @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit,
                                @RequestParam(value = "searchKey"        , required = false, defaultValue = "") String searchKey,
                            @RequestParam(value = "token", required = false) String  token) {
        Integer total = 0;
        Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
        TypechoPaykey query = new TypechoPaykey();
        if (StringUtils.isNotBlank(searchParams)) {
            JSONObject object = JSON.parseObject(searchParams);
            query = object.toJavaObject(TypechoPaykey.class);
            total = paykeyService.total(query);
        }

        PageList<TypechoPaykey> pageList = paykeyService.selectPage(query, page, limit,searchKey);
        JSONObject response = new JSONObject();
        response.put("code" , 1);
        response.put("msg"  , "");
        response.put("data" , null != pageList.getList() ? pageList.getList() : new JSONArray());
        response.put("count", pageList.getTotalCount());
        response.put("total", total);
        return response.toString();
    }

    @RequestMapping(value = "/tokenPayExcel")
    @ResponseBody
    @LoginRequired(purview = "2")
    public void tokenPayExcel(@RequestParam(value = "limit" , required = false, defaultValue = "15") Integer limit,@RequestParam(value = "token", required = false) String  token,HttpServletResponse response) throws IOException {
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet("充值码列表");

        Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
        if (uStatus == 0) {
            response.setContentType("application/octet-stream");
            response.setHeader("Content-disposition", "attachment;filename=nodata.xls");
            response.flushBuffer();
            workbook.write(response.getOutputStream());
        }
        Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
        String group = map.get("group").toString();
        if (!group.equals("administrator")) {
            response.setContentType("application/octet-stream");
            response.setHeader("Content-disposition", "attachment;filename=nodata.xls");
            response.flushBuffer();
            workbook.write(response.getOutputStream());
        }
        TypechoPaykey query = new TypechoPaykey();
        query.setStatus(0);
        PageList<TypechoPaykey> pageList = paykeyService.selectPage(query, 1, limit,null);
        List<TypechoPaykey> list = pageList.getList();
        String fileName = "tokenPayExcel"  + ".xls";//设置要导出的文件的名字
        //新增数据行，并且设置单元格数据

        int rowNum = 1;

        String[] headers = { "ID", "充值码", "等同积分"};
        //headers表示excel表中第一行的表头

        HSSFRow row = sheet.createRow(0);
        //在excel表中添加表头

        for(int i=0;i<headers.length;i++){
            HSSFCell cell = row.createCell(i);
            HSSFRichTextString text = new HSSFRichTextString(headers[i]);
            cell.setCellValue(text);
        }
        for (TypechoPaykey paykey : list) {
            HSSFRow row1 = sheet.createRow(rowNum);
            row1.createCell(0).setCellValue(paykey.getId());
            row1.createCell(1).setCellValue(paykey.getValue());
            row1.createCell(2).setCellValue(paykey.getPrice());
            rowNum++;
        }

        response.setContentType("application/octet-stream");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName);
        response.flushBuffer();
        workbook.write(response.getOutputStream());
    }
    /**
     * 卡密充值
     * **/
    @RequestMapping(value = "/tokenPay")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String tokenPay(@RequestParam(value = "key", required = false) String key,@RequestParam(value = "token", required = false) String  token) {
        try {
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer uid =Integer.parseInt(map.get("uid").toString());
            //登录情况下，恶意充值攻击拦截
            Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
            Integer switchTokenpay = 1;
            if(apiconfig.get("switchTokenpay")!=null){
                switchTokenpay = Integer.parseInt(apiconfig.get("switchTokenpay").toString());
            }
            if(switchTokenpay.equals(0)){
                return Result.getResultJson(0,"支付渠道已关闭",null);
            }
            if(apiconfig.get("banRobots").toString().equals("1")) {
                String isSilence = redisHelp.getRedis(this.dataprefix+"_"+uid+"_silence",redisTemplate);
                if(isSilence!=null){
                    return Result.getResultJson(0,"你的操作太频繁了，请稍后再试",null);
                }

                String isRepeated = redisHelp.getRedis(this.dataprefix+"_"+uid+"_isRepeated",redisTemplate);
                if(isRepeated==null){
                    redisHelp.setRedis(this.dataprefix+"_"+uid+"_isRepeated","1",2,redisTemplate);
                }else{
                    Integer frequency = Integer.parseInt(isRepeated) + 1;
                    if(frequency==3){
                        securityService.safetyMessage("用户ID："+uid+"，在卡密充值接口疑似存在攻击行为，请及时确认处理。","system");
                        redisHelp.setRedis(this.dataprefix+"_"+uid+"_silence","1",900,redisTemplate);
                        return Result.getResultJson(0,"你的请求存在恶意行为，15分钟内禁止操作！",null);
                    }else{
                        redisHelp.setRedis(this.dataprefix+"_"+uid+"_isRepeated",frequency.toString(),3,redisTemplate);
                    }
                    return Result.getResultJson(0,"你的操作太频繁了",null);
                }
            }

            //攻击拦截结束

            TypechoPaykey paykey = paykeyService.selectByKey(key);
            if(paykey==null){
                return Result.getResultJson(0,"卡密已失效",null);
            }
            Integer pirce = paykey.getPrice();

            TypechoUsers users = usersService.selectByKey(uid);
            Integer assets = users.getAssets();

            if(!paykey.getStatus().equals(0)){
                return Result.getResultJson(0,"卡密已失效",null);
            }

            //修改卡密状态
            paykey.setStatus(1);
            paykey.setUid(uid);
            paykeyService.update(paykey);

            //生成资产日志
            Long date = System.currentTimeMillis();
            String curTime = String.valueOf(date).substring(0,10);
            TypechoPaylog paylog = new TypechoPaylog();
            paylog.setStatus(1);
            paylog.setCreated(Integer.parseInt(curTime));
            paylog.setUid(uid);
            paylog.setOutTradeNo(curTime+"tokenPay");
            paylog.setTotalAmount(pirce.toString());
            paylog.setPaytype("tokenPay");
            paylog.setSubject("卡密充值");
            paylogService.insert(paylog);

            //修改用户账户
            Integer newassets = assets + pirce;
            TypechoUsers newUser = new TypechoUsers();
            newUser.setUid(uid);
            newUser.setAssets(newassets);
            usersService.update(newUser);



            JSONObject response = new JSONObject();
            response.put("code" , 1);
            response.put("msg"  , "卡密充值成功");
            return response.toString();
        }catch (Exception e){
            e.printStackTrace();
            JSONObject response = new JSONObject();
            response.put("code" , 0);
            response.put("msg"  , "卡密充值失败");
            return response.toString();
        }

    }

    /**
     * 彩虹易支付相关
     * **/
    @RequestMapping(value = "/EPay")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String EPay(@RequestParam(value = "type", required = false) String type,
                       @RequestParam(value = "money", required = false) Integer money,
                       @RequestParam(value = "device", required = false) String device,
                       @RequestParam(value = "token", required = false) String  token,
                       @RequestParam(value = "packageId", required = false ,defaultValue = "0") Integer  packageId,
                       HttpServletRequest request) {
        if(type==null&&money==null&&money==null&&device==null){
            return Result.getResultJson(0,"参数不正确",null);
        }
        try{
            //当packageId为0时，以num参数结算，当packageId不为0时，以套餐价格结算。
            TypechoPayPackage payPackage = new TypechoPayPackage();
            if(packageId.equals(0)){
                Pattern pattern = Pattern.compile("[0-9]*");
                if(!pattern.matcher(money.toString()).matches()){
                    return Result.getResultJson(0,"充值金额必须为正整数",null);
                }
                if(money <= 0){
                    return Result.getResultJson(0,"充值金额不正确",null);
                }
            }else{
                payPackage = payPackageService.selectByKey(packageId);
                if(payPackage == null){
                    return Result.getResultJson(0,"套餐不存在",null);
                }
                money = payPackage.getPrice();
            }
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer uid =Integer.parseInt(map.get("uid").toString());
            //登录情况下，恶意充值攻击拦截
            Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
            Integer minPayNum = 5;
            if(apiconfig.get("minPayNum")!=null){
                minPayNum = Integer.parseInt(apiconfig.get("minPayNum").toString());
            }
            if(money < minPayNum){
                return Result.getResultJson(0,"最小充值金额为："+minPayNum,null);
            }
            Integer switchEpay = 1;
            if(apiconfig.get("switchEpay")!=null){
                switchEpay = Integer.parseInt(apiconfig.get("switchEpay").toString());
            }
            if(switchEpay.equals(0)){
                return Result.getResultJson(0,"支付渠道已关闭",null);
            }
            if(apiconfig.get("banRobots").toString().equals("1")) {
                String isSilence = redisHelp.getRedis(this.dataprefix+"_"+uid+"_silence",redisTemplate);
                if(isSilence!=null){
                    return Result.getResultJson(0,"你的操作太频繁了，请稍后再试",null);
                }
                String isRepeated = redisHelp.getRedis(this.dataprefix+"_"+uid+"_isRepeated",redisTemplate);
                if(isRepeated==null){
                    redisHelp.setRedis(this.dataprefix+"_"+uid+"_isRepeated","1",2,redisTemplate);
                }else{
                    Integer frequency = Integer.parseInt(isRepeated) + 1;
                    if(frequency==3){
                        securityService.safetyMessage("用户ID："+uid+"，在微信充值接口疑似存在攻击行为，请及时确认处理。","system");
                        redisHelp.setRedis(this.dataprefix+"_"+uid+"_silence","1",900,redisTemplate);
                        return Result.getResultJson(0,"你的请求存在恶意行为，15分钟内禁止操作！",null);
                    }else{
                        redisHelp.setRedis(this.dataprefix+"_"+uid+"_isRepeated",frequency.toString(),3,redisTemplate);
                    }
                    return Result.getResultJson(0,"你的操作太频繁了",null);
                }
            }

            //攻击拦截结束
            String url = apiconfig.get("epayUrl").toString();
            Date now = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");//可以方便地修改日期格式
            String timeID = dateFormat.format(now);
            String outTradeNo=timeID+"Epay_"+type;
            String  clientip = baseFull.getIpAddr(request);
            Map<String,String> sign = new HashMap<>();
            sign.put("pid",apiconfig.get("epayPid").toString());
            sign.put("type",type.toString());
            sign.put("out_trade_no",outTradeNo);
            sign.put("notify_url",apiconfig.get("epayNotifyUrl").toString());
            sign.put("return_url",apiconfig.get("epayNotifyUrl").toString());
            sign.put("clientip",clientip);
            sign.put("name","在线充值金额");
            sign.put("money",money.toString());
            sign = sortByKey(sign);
            String signStr = "";
            for(Map.Entry<String,String> m :sign.entrySet()){
                signStr += m.getKey() + "=" +m.getValue()+"&";
            }
            signStr = signStr.substring(0,signStr.length()-1);
            signStr += apiconfig.get("epayKey").toString();
            signStr = DigestUtils.md5DigestAsHex(signStr.getBytes());
            sign.put("sign_type","MD5");
            sign.put("sign",signStr);

            String param = "";
            for(Map.Entry<String,String> m :sign.entrySet()){
                param += m.getKey() + "=" +m.getValue()+"&";
            }
            param = param.substring(0,param.length()-1);
            String data = HttpClient.doPost(url+"mapi.php",param);
            if(data==null){
                return Result.getResultJson(0,"易支付接口请求失败，请检查配置",null);
            }
            HashMap  jsonMap = JSON.parseObject(data, HashMap.class);
            if(jsonMap.get("code").toString().equals("1")){
                //先生成订单
                Long date = System.currentTimeMillis();
                String created = String.valueOf(date).substring(0,10);
                TypechoPaylog paylog = new TypechoPaylog();
                Integer TotalAmount = money * Integer.parseInt(apiconfig.get("scale").toString());
                paylog.setStatus(0);
                paylog.setCreated(Integer.parseInt(created));
                paylog.setUid(uid);
                paylog.setOutTradeNo(outTradeNo);
                paylog.setTotalAmount(TotalAmount.toString());
                paylog.setPaytype("ePay_"+type);
                if(packageId.equals(0)){
                    paylog.setSubject("易支付");
                    paylog.setTotalAmount(TotalAmount.toString());
                }else{
                    TotalAmount = payPackage.getGold();
                    paylog.setTotalAmount(TotalAmount.toString());
                    paylog.setSubject("易支付-套餐充值");
                    paylog.setPackageId(packageId);
                }
                paylogService.insert(paylog);
                //再返回数据
                JSONObject toResponse = new JSONObject();
                toResponse.put("code" ,1);
                toResponse.put("pid" , paylog.getPid());
                toResponse.put("payapi" ,apiconfig.get("epayUrl").toString());
                toResponse.put("data" , jsonMap);
                toResponse.put("msg"  , "获取成功");
                return toResponse.toString();
            }else {
                return Result.getResultJson(0,jsonMap.get("msg").toString(),null);
            }
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }


    }
    public static <K extends Comparable<? super K>, V > Map<K, V> sortByKey(Map<K, V> map) {
        Map<K, V> result = new LinkedHashMap<>();
        map.entrySet().stream()
                .sorted(Map.Entry.<K, V>comparingByKey()).forEachOrdered(e -> result.put(e.getKey(), e.getValue()));
        return result;
    }

    @RequestMapping(value = "/EPayNotify")
    @ResponseBody
    public String EPayNotify(HttpServletRequest request,
                         HttpServletResponse response) throws AlipayApiException {
        Map<String, String> params = new HashMap<String, String>();
        Map requestParams = request.getParameterMap();
        for (Iterator iter = requestParams.keySet().iterator(); iter.hasNext(); ) {
            String name = (String) iter.next();
            String[] values = (String[]) requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
            }
            params.put(name, valueStr);
        }

        System.err.println(params);
        try{
            if(params.get("trade_status").equals("TRADE_SUCCESS")){
                Map apiconfig = UStatus.getConfig(this.dataprefix,allconfigService,redisTemplate);
                //支付完成后，写入充值日志
                String trade_no = params.get("trade_no");
                String out_trade_no = params.get("out_trade_no");
                String total_amount = params.get("money");


                Long date = System.currentTimeMillis();
                String created = String.valueOf(date).substring(0,10);
                TypechoPaylog paylog = new TypechoPaylog();
                //根据订单和发起人，是否有数据库对应，来是否充值成功
                paylog.setOutTradeNo(out_trade_no);
                paylog.setStatus(0);
                List<TypechoPaylog> logList= paylogService.selectList(paylog);
                if(logList.size() > 0){
                    Integer pid = logList.get(0).getPid();
                    Integer uid = logList.get(0).getUid();
                    paylog.setStatus(1);
                    paylog.setTradeNo(trade_no);
                    paylog.setPid(pid);
                    paylog.setCreated(Integer.parseInt(created));
                    paylog.setPackageId(logList.get(0).getPackageId());
                    //订单修改后，插入用户表
                    Integer scale = Integer.parseInt(apiconfig.get("scale").toString());
                    Integer gold = Double.valueOf(total_amount).intValue() * scale;
                    Integer integral = 0;
                    //如果是套餐充值，且套餐存在，则赋值套餐
                    if(!paylog.getPackageId().equals(0)){
                        TypechoPayPackage payPackage = payPackageService.selectByKey(paylog.getPackageId());
                        if(payPackage != null){
                            gold = payPackage.getGold();
                            integral = payPackage.getIntegral();
                        }
                    }
                    paylogService.update(paylog);
                    //订单修改后，插入用户表
                    TypechoUsers users = usersService.selectByKey(uid);
                    Integer oldAssets = users.getAssets();
                    Integer assets = oldAssets + gold;
                    Integer oldPoints = users.getPoints();
                    Integer points = oldPoints + integral;
                    users.setAssets(assets);
                    users.setPoints(points);
                    usersService.update(users);
                    return "success";
                }else{
                    System.err.println("数据库不存在订单");
                    return "fail";
                }
            }else{
                return "fail";
            }
        }catch (Exception e){
            e.printStackTrace();
            return "fail";
        }

    }

    /***
     * 支付状态查询，用于支付完成后立即查询支付状态
     *
     */
    @RequestMapping(value = "/payStatus")
    @ResponseBody
    @LoginRequired(purview = "0")
    public String payStatus (@RequestParam(value = "pid", required = false ,defaultValue = "0") Integer  pid,
                             @RequestParam(value = "token", required = false) String  token) {
        try {
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer uid  = Integer.parseInt(map.get("uid").toString());
            TypechoPaylog paylog = paylogService.selectByKey(pid);
            if(paylog==null){
                return Result.getResultJson(1,"暂无支付记录",null);
            }
            if(paylog.getUid().equals(uid)){
                if(paylog.getStatus().equals(1)){
                    return Result.getResultJson(1,"充值成功",null);
                }else{
                    return Result.getResultJson(0,"充值失败，请等待或再次尝试",null);
                }
            }else{
                return Result.getResultJson(1,"你没有操作权限",null);
            }
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0,"接口请求异常，请联系管理员",null);
        }
    }

}
