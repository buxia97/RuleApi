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
 * 控制层
 * TypechoFieldsController
 * @author buxia97
 * @date 2021/11/29
 */
@Controller
@RequestMapping(value = "/typechoFields")
public class TypechoFieldsController {

    @Autowired
    TypechoFieldsService service;





}
