package com.RuleApi.web;


import com.RuleApi.common.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/plugin")
public class pluginController {
    ResultAll Result = new ResultAll();
    EditFile editFile = new EditFile();
    Plugin plugin = new Plugin();
    com.RuleApi.common.HttpClient HttpClient = new HttpClient();
    RedisHelp redisHelp =new RedisHelp();

    @Autowired
    private ApplicationContext applicationContext;

    @RequestMapping(value = "/main/{pluginName}/{pluginObject}")
    @ResponseBody
    public String main(@PathVariable String pluginName,
                       @PathVariable String pluginObject) {
        try {
            // 获取TypechoApiconfigService Bean
            String sourceCode = "" +
                    "import com.RuleApi.common.*;" +
                    "import com.RuleApi.common.*;\n" +
                    "import com.RuleApi.entity.*;\n" +
                    "import com.RuleApi.service.*;\n" +
                    "import com.alibaba.fastjson.JSON;\n" +
                    "import com.alibaba.fastjson.JSONObject;" +
                    "import java.lang.reflect.Method;\n" +
                    "import org.springframework.beans.factory.annotation.Autowired;\n" +
                    "import org.springframework.beans.factory.annotation.Value;\n" +
                    "import org.springframework.boot.system.ApplicationHome;\n" +
                    "import org.springframework.data.redis.core.RedisTemplate;\n" +
                    "import org.springframework.stereotype.Controller;\n" +
                    "import org.springframework.web.bind.annotation.RequestMapping;\n" +
                    "import org.springframework.web.bind.annotation.RequestParam;\n" +
                    "import org.springframework.web.bind.annotation.ResponseBody;" +
                    "import java.io.*;\n" +
                    "import java.util.ArrayList;\n" +
                    "import java.util.HashMap;\n" +
                    "import java.util.List;\n" +
                    "import java.util.Map;\n" +
                    "import java.util.Map;\n" +
                    "" +
                    "@Controller\n" +
                    "@RequestMapping(value = \""+pluginName+"\")\n" +
                    "public class "+pluginName+" {\n" +
                    "   ResultAll Result = new ResultAll();\n" +
                    "    EditFile editFile = new EditFile();\n" +
                    "    HttpClient HttpClient = new HttpClient();\n" +
                    "    RedisHelp redisHelp =new RedisHelp();\n" +
                    "    @Autowired\n" +
                    "    private TypechoApiconfigService apiconfigService;\n" +
                    "    public String greet() {\n" +
                    "        System.out.println(\"插件加载"+pluginName+"完成\");\n" +
                    "        return Result.getResultJson(0,\"插件加载"+pluginName+"完成\",null);" +
                    "    }\n" +
                    "   @RequestMapping(value = \"/getApiConfig\")\n" +
                    "    @ResponseBody\n" +
                    "    public String getApiConfig() {\n" +
                    "        TypechoApiconfig typechoApiconfig = apiconfigService.selectByKey(1);\n" +
                    "        Map json = JSONObject.parseObject(JSONObject.toJSONString(typechoApiconfig), Map.class);\n" +
                    "        JSONObject response = new JSONObject();\n" +
                    "        response.put(\"code\", 1);\n" +
                    "        response.put(\"msg\", \"\");\n" +
                    "        response.put(\"data\", json);\n" +
                    "        return response.toString();\n" +
                    "    }" +
                    "}";

            String result = plugin.loadCode(sourceCode, pluginName, pluginObject, 0);

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return "插件对象不存在" + e.getMessage();
        }
    }
}
