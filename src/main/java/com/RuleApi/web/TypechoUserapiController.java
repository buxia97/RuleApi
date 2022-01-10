package com.RuleApi.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.RuleApi.entity.*;
import com.RuleApi.common.ApiResult;
import com.RuleApi.common.PageList;
import com.RuleApi.common.ResultCode;
import com.RuleApi.service.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 社会化接口
 * TypechoUserapiController
 * @author buxia97
 * @date 2022/01/10
 */
@Controller
@RequestMapping(value = "/typechoUserapi")
public class TypechoUserapiController {

    @Autowired
    TypechoUserapiService service;

}
