package com.RuleApi.service;

import java.util.Map;

public interface SecurityService {
    void safetyMessage(String msg,String type);

    Map textViolation(String text);

    Map picViolation(String text,Integer type);


    Integer sendSMSCode(String phone,String area);

}
