package com.RuleApi.common;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.wxpay.sdk.WXPayUtil;
import com.wechat.pay.contrib.apache.httpclient.WechatPayHttpClientBuilder;
import com.wechat.pay.contrib.apache.httpclient.auth.PrivateKeySigner;
import com.wechat.pay.contrib.apache.httpclient.auth.Verifier;
import com.wechat.pay.contrib.apache.httpclient.auth.WechatPay2Credentials;
import com.wechat.pay.contrib.apache.httpclient.auth.WechatPay2Validator;
import com.wechat.pay.contrib.apache.httpclient.cert.CertificatesManager;
import com.wechat.pay.contrib.apache.httpclient.util.AesUtil;
import com.wechat.pay.contrib.apache.httpclient.util.PemUtil;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 微信支付工具类
 * @date 2022/6/16
 * @author gxw
 */
public class WeChatPayUtils {
    /*公众号/小程序信息*/
    //appId
    private static final String APP_ID = "";
    //secret
    private static final String APP_SECRET = "";

    /*商户信息*/
    //商户号mch_id
    private static final String MCH_ID = "";
    //商户私钥mch_key
    private static final String MCH_KEY = "下载证书的***key.pem文件内容";
    //商户证书序列号
    private static final String MCH_SERIAL_NO = "";
    //API3私钥
    private static final String MCH_API_V3_KEY = "";

    /*支付信息*/
    //native 统一下单API
    public static final String NATIVE_PAY_API = "";
    //native 商户订单号查单API
    public static final String NATIVE_PAY_OUT_TRADE_NO_QUERY_ORDER_API ="";
    //native 微信系统订单号查单API
    public static final String NATIVE_PAY_TRANSACTIONS_ID_QUERY_ORDER_API = "";
    //货币类型
    public static final String CURRENCY_CNY = "CNY";
    //支付类型
    public static final String TRADE_TYPE = "NATIVE";
    //异步回调地址
    public static final String NOTIFY_URL = "";

    /**
     * NATIVE获取CloseableHttpClient
     */
    private static CloseableHttpClient initHttpClient(){
        PrivateKey merchantPrivateKey = null;
        try {
            merchantPrivateKey = PemUtil
                    .loadPrivateKey(new ByteArrayInputStream(MCH_KEY.getBytes("utf-8")));
//            //*加载证书管理器实例*//*
//            // 加载平台证书（mchId：商户号,mchSerialNo：商户证书序列号,apiV3Key：V3密钥）
//            AutoUpdateCertificatesVerifier verifier = new AutoUpdateCertificatesVerifier(
//                    new WechatPay2Credentials(MCH_ID, new PrivateKeySigner(MCH_SERIAL_NO, merchantPrivateKey)),MCH_API_V3_KEY.getBytes("utf-8"));
//            //获取单例实例
            CertificatesManager certificatesManager = CertificatesManager.getInstance();
//            //向证书管理器增加商户信息，并开启自动更新
            certificatesManager.putMerchant(
                    MCH_ID,
                    new WechatPay2Credentials(
                            MCH_ID, new PrivateKeySigner(MCH_SERIAL_NO, merchantPrivateKey)),
                    MCH_API_V3_KEY.getBytes("utf-8")
            );
            //从证书管理器获得验签器
            Verifier verifier = certificatesManager.getVerifier(MCH_ID);
            CloseableHttpClient httpClient = WechatPayHttpClientBuilder.create()
                    .withMerchant(MCH_ID, MCH_SERIAL_NO, merchantPrivateKey)
                    .withValidator(new WechatPay2Validator(verifier)).build();
            return httpClient;
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * NATIVE 统一下单
     * @param money
     * @param body
     * @return
     */
    public static Map<String, String> native_payment_order(String money, String body, String outTradeNo) {
        try {
            CloseableHttpClient httpClient = initHttpClient();

            HttpPost httpPost = new HttpPost(NATIVE_PAY_API);
            // 请求body参数
            String reqdata = "{"
                    //+ "\"time_expire\":\"2018-06-08T10:34:56+08:00\","
                    + "\"amount\": {"
                    + "\"total\":" + Integer.parseInt(String.valueOf(Float.parseFloat(money) * 100).split("\\.")[0]) + ","
                    + "\"currency\":\"" + CURRENCY_CNY + "\""
                    + "},"
                    + "\"mchid\":\"" + MCH_ID + "\","
                    + "\"description\":\"" + body + "\","
                    + "\"notify_url\":\"" + NOTIFY_URL + "\","
                    + "\"out_trade_no\":\"" + outTradeNo + "\","
                    + "\"goods_tag\":\"课程购买\","
                    + "\"appid\":\"" + APP_ID + "\""
                    + "}";
            StringEntity entity = new StringEntity(reqdata, "utf-8");
            entity.setContentType("application/json");
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");

            //完成签名并执行请求
            CloseableHttpResponse response = null;
            Map<String, String> resultMap = new HashMap<>();
            try {
                response = httpClient.execute(httpPost);

                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) { //处理成功
                    String codeUrl = EntityUtils.toString(response.getEntity());
                    codeUrl = codeUrl.substring(codeUrl.indexOf("w"), codeUrl.indexOf("}") - 1);
                    //String path = QRCodeGenerator.generateQRCodeImage(codeUrl);
                    String path = codeUrl;
                    resultMap.put("code", "200");
                    resultMap.put("data", path);
                    System.out.println("生成成功，路径为：" + path);
                    System.out.println("success,return body = " + codeUrl);
                    return resultMap;
                } else if (statusCode == 204) { //处理成功，无返回Body
                    System.out.println("success");
                    resultMap.put("code", "204");
                    resultMap.put("msg", "处理成功，但无返回Body");
                    return resultMap;
                } else {
                    System.out.println("failed,resp code = " + statusCode + ",return body = " + EntityUtils.toString(response.getEntity()));
                    throw new IOException("request failed");
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                response.close();
            }
        }  catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * NATIVE 查询订单
     * @param outTradeNo 商户订单号
     * @return
     */
    public static Map<String, String> native_query_order(String outTradeNo) {
        CloseableHttpResponse response = null;
        try {
            String url = NATIVE_PAY_OUT_TRADE_NO_QUERY_ORDER_API + outTradeNo;
            //请求URL
            URIBuilder uriBuilder = new URIBuilder(url);

            uriBuilder.setParameter("mchid", MCH_ID);

            //完成签名并执行请求
            HttpGet httpGet = new HttpGet(uriBuilder.build());
            httpGet.addHeader("Accept", "application/json");
            response = initHttpClient().execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            Map<String, String> resultMap = new HashMap<>();
            if (statusCode == 200) {
                String resData = EntityUtils.toString(response.getEntity());
                Map<String, Object> data = JSON.parseObject(resData, HashMap.class);
                String tradeState = String.valueOf(data.get("trade_state"));
                if("SUCCESS".equals(tradeState)){
                    resultMap.put("msg", "支付成功");
                }else if("NOTPAY".equals(tradeState)){
                    resultMap.put("msg", "订单尚未支付");
                }else if("CLOSED".equals(tradeState)){
                    resultMap.put("msg", "此订单已关闭，请重新下单");
                }else if("USERPAYING".equals(tradeState)){
                    resultMap.put("msg", "正在支付中，请尽快支付完成哦");
                }else if("PAYERROR".equals(tradeState)){
                    resultMap.put("msg", "支付失败，请重新下单");
                }
                resultMap.put("code", "200");
                resultMap.put("tradeState", tradeState);
//                resultMap.put("openId", String.valueOf(JSON.parseObject(String.valueOf(data.get("payer")), HashMap.class).get("openid")));
//                resultMap.put("transactionId", String.valueOf(data.get("transaction_id")));
                return resultMap;
            } else if (statusCode == 204) {
                System.out.println("success");resultMap.put("code", "204");
                resultMap.put("msg", "处理成功，但无返回Body");
                return resultMap;
            } else {
                System.out.println("failed,resp code = " + statusCode+ ",return body = " + EntityUtils.toString(response.getEntity()));
                throw new IOException("request failed");
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * APIV3密钥版，NATIVE支付回调参数解密
     * @param map
     * @return
     */
    public static Map<String, Object> paramDecodeForAPIV3(Map<String, Object> map){
        //使用微信SDK提供的AesUtil工具类和APIV3密钥进行签名验证
        AesUtil aesUtil = new AesUtil(MCH_API_V3_KEY.getBytes(StandardCharsets.UTF_8));
        JSONObject paramsObj = new JSONObject(map);
        JSONObject rJ = paramsObj.getJSONObject("resource");
        Map<String, String> paramMap = (Map) rJ;
        try {
            //如果APIV3密钥和微信返回的resource中的信息不一致，是拿不到微信返回的支付信息
            String decryptToString = aesUtil.decryptToString(
                    paramMap.get("associated_data").getBytes(StandardCharsets.UTF_8),
                    paramMap.get("nonce").getBytes(StandardCharsets.UTF_8),
                    paramMap.get("ciphertext"));

            //验证成功后将获取的支付信息转为Map
            Map<String, Object> resultMap = WeChatPayUtils.strToMap(decryptToString);
            return resultMap;
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 交易起始时间
     */
    public static String getTimeStart(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        return sdf.format(new Date());
    }

    /**
     * 交易结束时间（订单失效时间）
     * @return
     */
    public static String getTimeExpire(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        Calendar now = Calendar.getInstance();
        now.add(Calendar.MINUTE, 30);
        return sdf.format(now.getTimeInMillis());
    }

    /**
     * 获取32位随机字符串
     * @return
     */
    public static String getRandomStr() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成订单号
     * @return
     */
    public static String generateOrderNo() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        return sdf.format(new Date()) + makeRandom(15);
    }

    /**
     * 生成随机数 纯数字
     *
     * @return
     */
    public static String makeRandom(int n){
        if(n<1){
            throw new IllegalArgumentException("随机数位数必须大于0");
        }
        Long num =  (long)(Math.random()*9*Math.pow(10,n-1)) + (long)Math.pow(10,n-1);
        return num.toString();
    }

    /**
     * 将Map转换为XML格式的字符串
     *
     * @param data Map类型数据
     * @return XML格式的字符串
     * @throws Exception
     */
    public static String mapToXml(Map<String, String> data) throws Exception {
        org.w3c.dom.Document document = WXPayXmlUtil.newDocument();
        org.w3c.dom.Element root = document.createElement("xml");
        document.appendChild(root);
        for (String key: data.keySet()) {
            String value = data.get(key);
            if (value == null) {
                value = "";
            }
            value = value.trim();
            org.w3c.dom.Element filed = document.createElement(key);
            filed.appendChild(document.createTextNode(value));
            root.appendChild(filed);
        }
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        DOMSource source = new DOMSource(document);
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        transformer.transform(source, result);
        String output = writer.getBuffer().toString(); //.replaceAll("\n|\r", "");
        try {
            writer.close();
        }
        catch (Exception ex) {
        }
        return output;
    }

    /**
     * Xml字符串转换为Map
     *
     * @param
     * @return
     */

    public static Map<String, String> xmlStrToMap(String strXML) throws Exception {
        try {
            Map<String, String> data = new HashMap<String, String>();
            DocumentBuilder documentBuilder = WXPayXmlUtil.newDocumentBuilder();
            InputStream stream = new ByteArrayInputStream(strXML.getBytes("UTF-8"));
            org.w3c.dom.Document doc = documentBuilder.parse(stream);
            doc.getDocumentElement().normalize();
            NodeList nodeList = doc.getDocumentElement().getChildNodes();
            for (int idx = 0; idx < nodeList.getLength(); ++idx) {
                Node node = nodeList.item(idx);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    org.w3c.dom.Element element = (org.w3c.dom.Element) node;
                    data.put(element.getNodeName(), element.getTextContent());
                }
            }
            try {
                stream.close();
            } catch (Exception ex) {
                // do nothing
            }
            return data;
        } catch (Exception ex) {
            //WXPayUtil.getLogger().warn("Invalid XML, can not convert to map. Error message: {}. XML content: {}", ex.getMessage(), strXML);
            throw ex;
        }

    }



    /**
     * 方法用途: 对所有传入参数按照字段名的 ASCII 码从小到大排序（字典序），并且生成url参数串<br>
     * 实现步骤: <br>
     *
     * @param paraMap    要排序的Map对象
     * @param urlEncode  是否需要URLENCODE
     * @param keyToLower 是否需要将Key转换为全小写
     *                   true:key转化成小写，false:不转化
     * @return
     */
    public static String formatUrlMap(Map<String, String> paraMap, boolean urlEncode, boolean keyToLower) {
        String buff = "";
        Map<String, String> tmpMap = paraMap;
        try {
            List<Map.Entry<String, String>> infoIds = new ArrayList<Map.Entry<String, String>>(tmpMap.entrySet());
            // 对所有传入参数按照字段名的 ASCII 码从小到大排序（字典序）
            Collections.sort(infoIds, new Comparator<Map.Entry<String, String>>() {
                @Override
                public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2) {
                    return (o1.getKey()).toString().compareTo(o2.getKey());
                }
            });
            // 构造URL 键值对的格式
            StringBuilder buf = new StringBuilder();
            for (Map.Entry<String, String> item : infoIds) {
                if (StringUtils.isNotBlank(item.getKey())) {
                    String key = item.getKey();
                    String val = item.getValue();
                    if (urlEncode) {
                        val = URLEncoder.encode(val, "utf-8");
                    }
                    if (keyToLower) {
                        buf.append(key.toLowerCase() + "=" + val);
                    } else {
                        buf.append(key + "=" + val);
                    }
                    buf.append("&");
                }

            }
            buff = buf.toString();
            if (buff.isEmpty() == false) {
                buff = buff.substring(0, buff.length() - 1);
            }
        } catch (Exception e) {
            return null;
        }
        return buff;
    }

    /**
     * 字符串转换为Map集合
     * @param str
     * @return
     */
    public static Map<String, Object> strToMap(String str){
        Map<String, Object> map = JSON.parseObject(str, HashedMap.class);
        return map;
    }


}