package com.RuleApi.common;

import com.github.wxpay.sdk.WXPayConfig;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.util.ClassUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class WXConfigUtil implements WXPayConfig {

    private byte[] certData;

    public WXConfigUtil() throws Exception {
        ApplicationHome h = new ApplicationHome(getClass());
        File jarF = h.getSource();
        /* 读入配置文件 */
        String certPath = jarF.getParentFile().toString()+"/weixin/apiclient_cert.p12";//从微信商户平台下载的安全证书存放的路径
        File file = new File(certPath);
        InputStream certStream = new FileInputStream(file);
        this.certData = new byte[(int) file.length()];
        certStream.read(this.certData);
        certStream.close();
    }

    @Override
    public String getAppID() {
        return "";
    }

    @Override
    public String getMchID() {
        return "";
    }

    @Override
    public String getKey() {
        return "";
    }


    @Override
    public InputStream getCertStream() {

        ByteArrayInputStream certBis = new ByteArrayInputStream(this.certData);
        return certBis;
    }

    @Override
    public int getHttpConnectTimeoutMs() {

        return 8000;
    }

    @Override
    public int getHttpReadTimeoutMs() {

        return 10000;
    }
}
