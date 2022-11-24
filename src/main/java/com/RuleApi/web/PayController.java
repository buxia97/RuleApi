package com.RuleApi.web;

import com.RuleApi.common.*;
import com.RuleApi.entity.*;
import com.RuleApi.service.*;
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
import com.github.wxpay.sdk.WXPay;
import com.github.wxpay.sdk.WXPayConstants;
import com.github.wxpay.sdk.WXPayUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
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
    private TypechoUsersService usersService;


    @Autowired
    private TypechoPaylogService paylogService;

    @Autowired
    private TypechoPaykeyService paykeyService;


    @Autowired
    private TypechoApiconfigService apiconfigService;

    @Value("${web.prefix}")
    private String dataprefix;

    @Value("${mybatis.configuration.variables.prefix}")
    private String prefix;

    RedisHelp redisHelp =new RedisHelp();
    ResultAll Result = new ResultAll();
    HttpClient HttpClient = new HttpClient();
    UserStatus UStatus = new UserStatus();


    /**
     * 支付宝扫码支付
     * @return 支付宝生成的订单信息
     */
    @RequestMapping(value = "/scancodePay")
    @ResponseBody
    public String scancodepay(@RequestParam(value = "num", required = false) String num, @RequestParam(value = "token", required = false) String  token) throws AlipayApiException {

        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }
        Pattern pattern = Pattern.compile("[0-9]*");
        if(!pattern.matcher(num).matches()){
            return Result.getResultJson(0,"充值金额必须为正整数",null);
        }
        if(Integer.parseInt(num) <= 0){
            return Result.getResultJson(0,"充值金额不正确",null);
        }
        TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);

        final String APPID = apiconfig.getAlipayAppId();
        String RSA2_PRIVATE = apiconfig.getAlipayPrivateKey();
        String ALIPAY_PUBLIC_KEY = apiconfig.getAlipayPublicKey();

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
                "    \"subject\":\"扫码支付\"," +
                "    \"timeout_express\":\"90m\"}");//设置业务参数
        request.setNotifyUrl(apiconfig.getAlipayNotifyUrl());
        AlipayTradePrecreateResponse response = alipayClient.execute(request);//通过alipayClient调用API，获得对应的response类
        System.out.print(response.getBody());

        //根据response中的结果继续业务逻辑处理
        if (response.getMsg().equals("Success")) {
            //先生成订单
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer uid  = Integer.parseInt(map.get("uid").toString());
            Long date = System.currentTimeMillis();
            String created = String.valueOf(date).substring(0,10);
            TypechoPaylog paylog = new TypechoPaylog();
            Integer TotalAmount = Integer.parseInt(total_fee) * apiconfig.getScale();
            paylog.setStatus(0);
            paylog.setCreated(Integer.parseInt(created));
            paylog.setUid(uid);
            paylog.setOutTradeNo(order_no);
            paylog.setTotalAmount(TotalAmount.toString());
            paylog.setPaytype("scancodePay");
            paylog.setSubject("扫码支付");
            paylogService.insert(paylog);
            //再返回二维码
            String qrcode = response.getQrCode();
            JSONObject toResponse = new JSONObject();
            toResponse.put("code" ,1);
            toResponse.put("data" , qrcode);
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
        TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);
        String CHARSET = "UTF-8";
        //支付宝公钥
        String ALIPAY_PUBLIC_KEY = apiconfig.getAlipayPublicKey();

        String tradeStatus = request.getParameter("trade_status");
        boolean flag = AlipaySignature.rsaCheckV1(params, ALIPAY_PUBLIC_KEY, CHARSET, "RSA2");

        if (flag) {//验证成功

            if (tradeStatus.equals("TRADE_FINISHED") || tradeStatus.equals("TRADE_SUCCESS")) {
                //支付完成后，写入充值日志
                String trade_no = params.get("trade_no");
                String out_trade_no = params.get("out_trade_no");
                String total_amount = params.get("total_amount");
                Integer scale = apiconfig.getScale();
                Integer integral = Double.valueOf(total_amount).intValue() * scale;

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
                    paylogService.update(paylog);
                    //订单修改后，插入用户表
                    TypechoUsers users = usersService.selectByKey(uid);
                    Integer oldAssets = users.getAssets();
                    Integer assets = oldAssets + integral;
                    users.setAssets(assets);
                    usersService.update(users);
                }else{
                    System.out.println("数据库不存在订单");
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
            System.out.println("用户未的登陆");
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
    public String payLogList (@RequestParam(value = "token", required = false) String  token) {

        String page = "1";
        String limit = "30";
        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }
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
    public String financeList (@RequestParam(value = "searchParams", required = false) String  searchParams,
                               @RequestParam(value = "token", required = false) String  token,
                               @RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                               @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit) {

        Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
        if (uStatus == 0) {
            return Result.getResultJson(0, "用户未登录或Token验证失败", null);
        }
        Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
        String group = map.get("group").toString();
        if (!group.equals("administrator")) {
            return Result.getResultJson(0, "你没有操作权限", null);
        }
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
     * 财务记录(管理员)
     * */
    @RequestMapping(value = "/financeTotal")
    @ResponseBody
    public String financeTotal (@RequestParam(value = "token", required = false) String  token){
        Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
        if (uStatus == 0) {
            return Result.getResultJson(0, "用户未登录或Token验证失败", null);
        }
        Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
        String group = map.get("group").toString();
        if (!group.equals("administrator")) {
            return Result.getResultJson(0, "你没有操作权限", null);
        }
        Map financeData = new HashMap<String, Integer>();
        Integer recharge = jdbcTemplate.queryForObject("SELECT SUM(total_amount) FROM `"+prefix+"_paylog` where `status` = 1 and (`subject` = '扫码支付' or `subject` = '微信APP支付' or `subject` = '卡密充值' or `subject` = '系统充值');", Integer.class);
        Integer trade = jdbcTemplate.queryForObject("SELECT SUM(total_amount) FROM `"+prefix+"_paylog` where `status` = 1 and (`paytype` = 'buyshop' or `paytype` = 'buyvip' or `paytype` = 'toReward' or `paytype` = 'buyAds');", Integer.class);
        Integer withdraw = jdbcTemplate.queryForObject("SELECT SUM(total_amount) FROM `"+prefix+"_paylog` where `status` = 1 and (`paytype` = 'withdraw' or `subject` = '系统扣款');", Integer.class);
        Integer income = jdbcTemplate.queryForObject("SELECT SUM(total_amount) FROM `"+prefix+"_paylog` where `status` = 1 and (`paytype` = 'clock' or `paytype` = 'sellshop' or `paytype` = 'reward');", Integer.class);
        trade = trade * -1;
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
     * 微信支付
     * */
    @RequestMapping(value = "/WxPay")
    @ResponseBody
    public String wxAdd(HttpServletRequest request,@RequestParam(value = "price", required = false) Integer price,@RequestParam(value = "token", required = false) String  token) throws Exception {
        Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
        if (uStatus == 0) {
            return Result.getResultJson(0, "用户未登录或Token验证失败", null);
        }
        TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);
        //商户订单号
        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");//可以方便地修改日期格式
        String timeID = dateFormat.format(now);
        String outTradeNo = timeID+"WxPay";
        Map<String, String> data = WeChatPayUtils.native_payment_order(price.toString(), "微信商品下单", outTradeNo);
        if("200".equals(data.get("code"))){
            //先生成订单
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer uid  = Integer.parseInt(map.get("uid").toString());
            Long date = System.currentTimeMillis();
            String created = String.valueOf(date).substring(0,10);
            TypechoPaylog paylog = new TypechoPaylog();
            Integer TotalAmount = price * apiconfig.getScale();
            paylog.setStatus(0);
            paylog.setCreated(Integer.parseInt(created));
            paylog.setUid(uid);
            paylog.setOutTradeNo(outTradeNo);
            paylog.setTotalAmount(TotalAmount.toString());
            paylog.setPaytype("WxPay");
            paylog.setSubject("扫码支付");
            paylogService.insert(paylog);
            //再返回二维码
            data.put("outTradeNo", outTradeNo);
            data.put("totalAmount", price.toString());

            JSONObject toResponse = new JSONObject();
            toResponse.put("code" ,1);
            toResponse.put("data" , data);
            toResponse.put("msg"  , "获取成功");
            return toResponse.toString();
        } else {
            JSONObject toResponse = new JSONObject();
            toResponse.put("code", 0);
            toResponse.put("data", "");
            toResponse.put("msg", "请求失败");
            return toResponse.toString();
        }

    }
    /**
     * 微信回调
     * */
    @RequestMapping(value = "/wxPayNotify")
    @ResponseBody
    public String wxPayNotify(
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);
        Map<String, Object> map = new ObjectMapper().readValue(request.getInputStream(), Map.class);
        Map<String, Object> dataMap = WeChatPayUtils.paramDecodeForAPIV3(map);
        //判断是否⽀付成功
        if("SUCCESS".equals(dataMap.get("trade_state"))){
            //支付完成后，写入充值日志
            String trade_no = dataMap.get("transaction_id").toString();
            String out_trade_no = dataMap.get("out_trade_no").toString();


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
                paylogService.update(paylog);
                //订单修改后，插入用户表
                String total_amount = logList.get(0).getTotalAmount();
                Integer scale = apiconfig.getScale();
                Integer integral = Double.valueOf(total_amount).intValue() * scale;
                TypechoUsers users = usersService.selectByKey(uid);
                Integer oldAssets = users.getAssets();
                Integer assets = oldAssets + integral;
                users.setAssets(assets);
                usersService.update(users);
            }else{
                System.out.println("数据库不存在订单");
                Map<String, String> returnMap = new HashMap<>();
                returnMap.put("code", "FALL");
                returnMap.put("message", "");
                //将返回微信的对象转换为xml
                String returnXml = WeChatPayUtils.mapToXml(returnMap);
                return returnXml;
            }

            //给微信发送我已接收通知的响应
            //创建给微信响应的对象
            Map<String, String> returnMap = new HashMap<>();
            returnMap.put("code", "SUCCESS");
            returnMap.put("message", "成功");
            //将返回微信的对象转换为xml
            String returnXml = WeChatPayUtils.mapToXml(returnMap);
            return returnXml;
        }
        //支付失败
        //创建给微信响应的对象
        Map<String, String> returnMap = new HashMap<>();
        returnMap.put("code", "FALL");
        returnMap.put("message", "");
        //将返回微信的对象转换为xml
        String returnXml = WeChatPayUtils.mapToXml(returnMap);
        return returnXml;

    }

    /**
     * 卡密充值相关
     * **/

    /**
     * 创建卡密
     * **/
    @RequestMapping(value = "/madetoken")
    @ResponseBody
    public String madetoken(@RequestParam(value = "price", required = false) Integer  price,@RequestParam(value = "num", required = false) Integer  num,@RequestParam(value = "token", required = false) String  token) {
        try{
            Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
            if (uStatus == 0) {
                return Result.getResultJson(0, "用户未登录或Token验证失败", null);
            }
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            String group = map.get("group").toString();
            if (!group.equals("administrator")) {
                return Result.getResultJson(0, "你没有操作权限", null);
            }
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
    public String tokenPayList (@RequestParam(value = "searchParams", required = false) String  searchParams,
                            @RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                            @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit,
                                @RequestParam(value = "searchKey"        , required = false, defaultValue = "") String searchKey,
                            @RequestParam(value = "token", required = false) String  token) {
        Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
        if (uStatus == 0) {
            return Result.getResultJson(0, "用户未登录或Token验证失败", null);
        }
        Integer total = 0;
        Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
        String group = map.get("group").toString();
        if (!group.equals("administrator")) {
            return Result.getResultJson(0, "你没有操作权限", null);
        }
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
        PageList<TypechoPaykey> pageList = paykeyService.selectPage(query, 1, limit,"");
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
    public String tokenPay(@RequestParam(value = "key", required = false) String key,@RequestParam(value = "token", required = false) String  token) {
        try {
            Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
            if(uStatus==0){
                return Result.getResultJson(0,"用户未登录或Token验证失败",null);
            }
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer uid =Integer.parseInt(map.get("uid").toString());

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

            //修改用户账户
            Integer newassets = assets + pirce;
            users.setAssets(newassets);
            usersService.update(users);

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

            JSONObject response = new JSONObject();
            response.put("code" , 1);
            response.put("msg"  , "卡密充值成功");
            return response.toString();
        }catch (Exception e){
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
    public String EPay(@RequestParam(value = "type", required = false) String type,@RequestParam(value = "money", required = false) Integer money,@RequestParam(value = "device", required = false) String device,@RequestParam(value = "token", required = false) String  token,HttpServletRequest request) {
        if(type==null&&money==null&&money==null&&device==null){
            return Result.getResultJson(0,"参数不正确",null);
        }
        try{
            Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
            if(uStatus==0){
                return Result.getResultJson(0,"用户未登录或Token验证失败",null);
            }
            TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);
            String url = apiconfig.getEpayUrl();
            Date now = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");//可以方便地修改日期格式
            String timeID = dateFormat.format(now);
            String outTradeNo=timeID+"Epay_"+type;
            String  clientip = baseFull.getIpAddr(request);
            Map<String,String> sign = new HashMap<>();
            sign.put("pid",apiconfig.getEpayPid().toString());
            sign.put("type",type.toString());
            sign.put("out_trade_no",outTradeNo);
            sign.put("notify_url",apiconfig.getEpayNotifyUrl());
            sign.put("clientip",clientip);
            sign.put("name","在线充值金额");
            sign.put("money",money.toString());
            sign = sortByKey(sign);
            String signStr = "";
            for(Map.Entry<String,String> m :sign.entrySet()){
                signStr += m.getKey() + "=" +m.getValue()+"&";
            }
            signStr = signStr.substring(0,signStr.length()-1);
            signStr += apiconfig.getEpayKey();
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
                Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
                Integer uid  = Integer.parseInt(map.get("uid").toString());
                Long date = System.currentTimeMillis();
                String created = String.valueOf(date).substring(0,10);
                TypechoPaylog paylog = new TypechoPaylog();
                Integer TotalAmount = money * apiconfig.getScale();
                paylog.setStatus(0);
                paylog.setCreated(Integer.parseInt(created));
                paylog.setUid(uid);
                paylog.setOutTradeNo(outTradeNo);
                paylog.setTotalAmount(TotalAmount.toString());
                paylog.setPaytype("ePay_"+type);
                paylog.setSubject("扫码支付");
                paylogService.insert(paylog);
                //再返回数据
                JSONObject toResponse = new JSONObject();
                toResponse.put("code" ,1);
                toResponse.put("payapi" ,apiconfig.getEpayUrl());
                toResponse.put("data" , jsonMap);
                toResponse.put("msg"  , "获取成功");
                return toResponse.toString();
            }else {
                return Result.getResultJson(0,jsonMap.get("msg").toString(),null);
            }
        }catch (Exception e){
            System.out.println(e);
            return Result.getResultJson(0,"接口异常，请检查配置",null);
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
                TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);
                //支付完成后，写入充值日志
                String trade_no = params.get("trade_no");
                String out_trade_no = params.get("out_trade_no");
                String total_amount = params.get("money");
                Integer scale = apiconfig.getScale();
                Integer integral = Double.valueOf(total_amount).intValue() * scale;

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
                    paylogService.update(paylog);
                    //订单修改后，插入用户表
                    TypechoUsers users = usersService.selectByKey(uid);
                    Integer oldAssets = users.getAssets();
                    Integer assets = oldAssets + integral;
                    users.setAssets(assets);
                    usersService.update(users);
                    return "success";
                }else{
                    System.out.println("数据库不存在订单");
                    return "fail";
                }
            }else{
                return "fail";
            }
        }catch (Exception e){
            System.out.println(e);
            return "fail";
        }

    }

}
