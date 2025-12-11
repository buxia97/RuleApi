package com.RuleApi.common;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;

import java.io.InputStream;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;

public class AppleLoginUtil {

    private static final String APPLE_PUBLIC_KEYS_URL = "https://appleid.apple.com/auth/keys";

    /**
     * 验证Apple返回的identityToken是否合法，并提取用户sub
     * @param identityToken JWT字符串
     * @return Apple用户唯一ID (sub)，验证失败返回null
     */
    public static String verifyAndGetSub(String identityToken) {
        try {
            // 1. 解析JWT
            SignedJWT signedJWT = SignedJWT.parse(identityToken);
            JWSHeader header = signedJWT.getHeader();

            // 2. 下载苹果公钥
            InputStream is = new URL(APPLE_PUBLIC_KEYS_URL).openStream();
            JWKSet jwkSet = JWKSet.load(is);

            // 3. 根据kid匹配对应公钥
            JWK jwk = jwkSet.getKeyByKeyId(header.getKeyID());
            if (jwk == null) {
                System.out.println("未找到对应kid的Apple公钥");
                return null;
            }

            // 4. 构造验证器
            RSAKey rsaKey = (RSAKey) jwk;
            RSAPublicKey publicKey = rsaKey.toRSAPublicKey();
            JWSVerifier verifier = new RSASSAVerifier(publicKey);

            // 5. 验证签名
            if (!signedJWT.verify(verifier)) {
                System.out.println("Apple身份令牌签名验证失败");
                return null;
            }

            // 6. 提取payload中的sub
            String sub = signedJWT.getJWTClaimsSet().getStringClaim("sub");
            return sub;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
