package com.RuleApi.service;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Created by xxs on 2021/7/30 9:56
 *
 * @Description
 * @Version 2.9
 */
public interface WxPayService {

    String payBack(String resXml);
}