package com.RuleApi.common;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.wxpay.sdk.WXPayUtil;
import com.wechat.pay.contrib.apache.httpclient.WechatPayHttpClientBuilder;
import com.wechat.pay.contrib.apache.httpclient.util.AesUtil;
import com.wechat.pay.contrib.apache.httpclient.util.PemUtil;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
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
import java.security.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 微信支付工具类（支持 App、JSAPI小程序、Native扫码、H5支付）
 * @author gxw
 */
public class WeChatPayUtils {

    // 支付方式对应的统一下单 URL
    private static final String NATIVE_PAY_API = "https://api.mch.weixin.qq.com/v3/pay/transactions/native";
    private static final String APP_PAY_API = "https://api.mch.weixin.qq.com/v3/pay/transactions/app";
    private static final String JSAPI_PAY_API = "https://api.mch.weixin.qq.com/v3/pay/transactions/jsapi";
    private static final String H5_PAY_API = "https://api.mch.weixin.qq.com/v3/pay/transactions/h5";

    // 货币类型
    private static final String CURRENCY_CNY = "CNY";

    /**
     * 初始化 HttpClient（字符串私钥+公钥方式）
     */
    private static CloseableHttpClient initHttpClient(Map<String, Object> apiconfig) {
        try {
            String mchId = apiconfig.get("wxpayMchId").toString();
            String mchSerialNo = apiconfig.get("mchSerialNo").toString();
            String privateKeyContent = apiconfig.get("wxpayKey").toString();
            String publicKeyContent = apiconfig.get("wxpayPublicKey").toString();

            PrivateKey merchantPrivateKey = PemUtil.loadPrivateKey(
                    new ByteArrayInputStream(privateKeyContent.getBytes(StandardCharsets.UTF_8))
            );

            PublicKey wechatpayPublicKey = PemUtil.loadPublicKey(
                    new ByteArrayInputStream(publicKeyContent.getBytes(StandardCharsets.UTF_8))
            );

            return WechatPayHttpClientBuilder.create()
                    .withMerchant(mchId, mchSerialNo, merchantPrivateKey)
                    .withWechatPay(mchSerialNo, wechatpayPublicKey)
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 统一下单接口（支持 Native / App / JSAPI / H5）
     * @param money 金额（元）
     * @param body 商品描述
     * @param outTradeNo 商户订单号
     * @param tradeType 支付类型：NATIVE / APP / JSAPI / H5
     * @param apiconfig 支付配置
     * @param openidOrWapUrl JSAPI传openid，H5传wap_url
     * @return Map<String,String> 返回支付信息（code_url、prepay_id、h5_url）
     */
    public static Map<String, String> unifiedOrder(String money, String body, String outTradeNo,
                                                   String tradeType, Map<String, Object> apiconfig,
                                                   String openidOrWapUrl) {
        try {
            CloseableHttpClient httpClient = initHttpClient(apiconfig);
            String MCH_ID = apiconfig.get("wxpayMchId").toString();
            String NOTIFY_URL = apiconfig.get("wxpayNotifyUrl").toString();
            String APPID = apiconfig.get("wxpayAppId").toString();

            String url = null;
            JSONObject json = new JSONObject();
            json.put("mchid", MCH_ID);
            json.put("description", body);
            json.put("out_trade_no", outTradeNo);
            JSONObject amountJson = new JSONObject();
            amountJson.put("total", (int)(Float.parseFloat(money) * 100));
            amountJson.put("currency", CURRENCY_CNY);
            json.put("amount", amountJson);
            json.put("notify_url", NOTIFY_URL);
            json.put("appid", APPID);

            switch (tradeType.toUpperCase()) {
                case "NATIVE":
                    url = NATIVE_PAY_API;
                    break;
                case "APP":
                    url = APP_PAY_API;
                    break;
                case "JSAPI":
                    url = JSAPI_PAY_API;
                    if (openidOrWapUrl != null) {
                        JSONObject payer = new JSONObject();
                        payer.put("openid", openidOrWapUrl);
                        json.put("payer", payer);
                    }
                    break;
                case "H5":
                    url = H5_PAY_API;
                    if (openidOrWapUrl != null) {
                        JSONObject sceneInfo = new JSONObject();
                        JSONObject h5Info = new JSONObject();
                        h5Info.put("type", "Wap");
                        h5Info.put("wap_url", openidOrWapUrl);
                        h5Info.put("wap_name", body);
                        sceneInfo.put("h5_info", h5Info);
                        json.put("scene_info", sceneInfo);
                    }
                    break;
                default:
                    throw new RuntimeException("tradeType不支持");
            }

            HttpPost httpPost = new HttpPost(url);
            StringEntity entity = new StringEntity(json.toJSONString(), "utf-8");
            entity.setContentType("application/json");
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");

            CloseableHttpResponse response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            String respBody = EntityUtils.toString(response.getEntity());
            Map<String, String> resultMap = new HashMap<>();

            if (statusCode == 200 || statusCode == 201) {
                JSONObject respJson = JSON.parseObject(respBody);
                switch (tradeType.toUpperCase()) {
                    case "NATIVE":
                        resultMap.put("code", "200");
                        resultMap.put("data", respJson.getString("code_url"));
                        break;
                    case "APP":
                        resultMap.put("code", "200");
                        resultMap.put("prepay_id", respJson.getString("prepay_id"));
                        break;
                    case "JSAPI":
                        resultMap.put("code", "200");
                        resultMap.put("prepay_id", respJson.getString("prepay_id"));
                        break;
                    case "H5":
                        resultMap.put("code", "200");
                        resultMap.put("h5_url", respJson.getJSONObject("h5_url").getString("web_url"));
                        break;
                }
                return resultMap;
            } else {
                resultMap.put("code", String.valueOf(statusCode));
                resultMap.put("msg", respBody);
                return resultMap;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 查询订单（通用）
     */
    public static Map<String, String> queryOrder(String outTradeNo, Map<String, Object> apiconfig) {
        CloseableHttpResponse response = null;
        String MCH_ID = apiconfig.get("wxpayMchId").toString();
        try {
            String url = "https://api.mch.weixin.qq.com/v3/pay/transactions/id/" + outTradeNo;
            URIBuilder uriBuilder = new URIBuilder(url);
            uriBuilder.setParameter("mchid", MCH_ID);
            HttpGet httpGet = new HttpGet(uriBuilder.build());
            httpGet.addHeader("Accept", "application/json");
            response = initHttpClient(apiconfig).execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            Map<String, String> resultMap = new HashMap<>();
            if (statusCode == 200) {
                String resData = EntityUtils.toString(response.getEntity());
                Map<String, Object> data = JSON.parseObject(resData, HashMap.class);
                String tradeState = String.valueOf(data.get("trade_state"));
                if ("SUCCESS".equals(tradeState)) resultMap.put("msg", "支付成功");
                else if ("NOTPAY".equals(tradeState)) resultMap.put("msg", "订单尚未支付");
                else if ("CLOSED".equals(tradeState)) resultMap.put("msg", "此订单已关闭，请重新下单");
                else if ("USERPAYING".equals(tradeState)) resultMap.put("msg", "正在支付中");
                else if ("PAYERROR".equals(tradeState)) resultMap.put("msg", "支付失败，请重新下单");
                resultMap.put("code", "200");
                resultMap.put("tradeState", tradeState);
                return resultMap;
            } else {
                resultMap.put("code", String.valueOf(statusCode));
                resultMap.put("msg", EntityUtils.toString(response.getEntity()));
                return resultMap;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try { if (response != null) response.close(); } catch (IOException ignored) {}
        }
    }

    /**
     * APIV3 回调参数解密
     */
    public static Map<String, Object> paramDecodeForAPIV3(Map<String, Object> map, Map<String,Object> apiconfig){
        String MCH_API_V3_KEY = apiconfig.get("mchApiV3Key").toString();
        AesUtil aesUtil = new AesUtil(MCH_API_V3_KEY.getBytes(StandardCharsets.UTF_8));
        JSONObject paramsObj = new JSONObject(map);
        JSONObject rJ = paramsObj.getJSONObject("resource");
        Map<String, String> paramMap = (Map) rJ;
        try {
            String decryptToString = aesUtil.decryptToString(
                    paramMap.get("associated_data").getBytes(StandardCharsets.UTF_8),
                    paramMap.get("nonce").getBytes(StandardCharsets.UTF_8),
                    paramMap.get("ciphertext"));
            return strToMap(decryptToString);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // -------------------------- 以下保留原有通用方法 --------------------------

    public static String getTimeStart(){ return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()); }
    public static String getTimeExpire(){ Calendar now = Calendar.getInstance(); now.add(Calendar.MINUTE,30); return new SimpleDateFormat("yyyyMMddHHmmss").format(now.getTime()); }
    public static String getRandomStr() { return UUID.randomUUID().toString().replace("-", ""); }
    public static String generateOrderNo() { return new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date()) + makeRandom(15); }
    public static String makeRandom(int n){ Long num =  (long)(Math.random()*9*Math.pow(10,n-1)) + (long)Math.pow(10,n-1); return num.toString(); }

    public static String formatUrlMap(Map<String, String> paraMap, boolean urlEncode, boolean keyToLower){
        String buff = "";
        try {
            List<Map.Entry<String, String>> infoIds = new ArrayList<>(paraMap.entrySet());
            Collections.sort(infoIds, Comparator.comparing(Map.Entry::getKey));
            StringBuilder buf = new StringBuilder();
            for (Map.Entry<String, String> item : infoIds) {
                if (StringUtils.isNotBlank(item.getKey())) {
                    String key = item.getKey();
                    String val = item.getValue();
                    if (urlEncode) val = URLEncoder.encode(val, "utf-8");
                    if (keyToLower) buf.append(key.toLowerCase()).append("=").append(val);
                    else buf.append(key).append("=").append(val);
                    buf.append("&");
                }
            }
            buff = buf.toString();
            if (!buff.isEmpty()) buff = buff.substring(0, buff.length() - 1);
        } catch (Exception e) { return null; }
        return buff;
    }

    public static Map<String,Object> strToMap(String str){ return JSON.parseObject(str, HashedMap.class); }

    public static String mapToXml(Map<String,String> data) throws Exception {
        org.w3c.dom.Document document = WXPayXmlUtil.newDocument();
        org.w3c.dom.Element root = document.createElement("xml");
        document.appendChild(root);
        for (String key: data.keySet()) {
            String value = data.get(key) == null ? "" : data.get(key).trim();
            org.w3c.dom.Element filed = document.createElement(key);
            filed.appendChild(document.createTextNode(value));
            root.appendChild(filed);
        }
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT,"yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.getBuffer().toString();
    }

    public static Map<String,String> xmlStrToMap(String strXML) throws Exception {
        Map<String,String> data = new HashMap<>();
        DocumentBuilder documentBuilder = WXPayXmlUtil.newDocumentBuilder();
        InputStream stream = new ByteArrayInputStream(strXML.getBytes("UTF-8"));
        org.w3c.dom.Document doc = documentBuilder.parse(stream);
        doc.getDocumentElement().normalize();
        NodeList nodeList = doc.getDocumentElement().getChildNodes();
        for(int idx=0; idx<nodeList.getLength(); idx++){
            Node node = nodeList.item(idx);
            if(node.getNodeType()==Node.ELEMENT_NODE){
                org.w3c.dom.Element element = (org.w3c.dom.Element) node;
                data.put(element.getNodeName(), element.getTextContent());
            }
        }
        stream.close();
        return data;
    }
}
