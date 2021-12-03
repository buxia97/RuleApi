package com.RuleApi.common;
import com.alibaba.fastjson.JSONObject;

import java.util.Map;

public class ResultAll {
    private JSONObject ResultJson = new JSONObject();

    public String getResultJson(Integer code, String descr, Map data){
        this.ResultJson.put("code" , code);
        this.ResultJson.put("msg"  , descr);
        this.ResultJson.put("data"  , data);
        return ResultJson.toString();
    }
}
