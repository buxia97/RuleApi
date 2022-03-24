package com.RuleApi.web;


import com.RuleApi.service.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;


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
