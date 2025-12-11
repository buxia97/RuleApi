package com.RuleApi.common;

import com.RuleApi.entity.TypechoEmailtemplate;

import javax.servlet.http.HttpServletRequest;

public class emailResult {

    public static String getVerifyEmail(TypechoEmailtemplate template,String userName,String code) {
        String VerifyTemplate = template.getVerifyTemplate();
        if(userName!=null){
            VerifyTemplate = VerifyTemplate.replace("{{userName}}", userName);
        }else{
            VerifyTemplate = VerifyTemplate.replace("{{userName}}", "平台用户");
        }
        VerifyTemplate = VerifyTemplate.replace("{{code}}", code);
        return VerifyTemplate;
    }
    public static String getReviewEmail(TypechoEmailtemplate template,String userName,String title,String reviewText) {
        String VerifyTemplate = template.getReviewTemplate();
        VerifyTemplate = VerifyTemplate.replace("{{title}}", title);
        VerifyTemplate = VerifyTemplate.replace("{{userName}}", userName);
        VerifyTemplate = VerifyTemplate.replace("{{reviewText}}", reviewText);
        return VerifyTemplate;
    }
    public static String getSafetyEmail(TypechoEmailtemplate template,String safetyText) {
        String VerifyTemplate = template.getSafetyTemplate();
        VerifyTemplate = VerifyTemplate.replace("{{safetyText}}", safetyText);
        return VerifyTemplate;
    }
    public static String getReplyEmail(TypechoEmailtemplate template,String userName,String title,String replyText) {
        String VerifyTemplate = template.getReplyTemplate();
        VerifyTemplate = VerifyTemplate.replace("{{title}}", title);
        VerifyTemplate = VerifyTemplate.replace("{{userName}}", userName);
        VerifyTemplate = VerifyTemplate.replace("{{replyText}}", replyText);
        return VerifyTemplate;
    }
    public static String getOrderEmail(TypechoEmailtemplate template,String userName,String title) {
        String VerifyTemplate = template.getOrderTemplate();
        VerifyTemplate = VerifyTemplate.replace("{{title}}", title);
        VerifyTemplate = VerifyTemplate.replace("{{userName}}", userName);
        return VerifyTemplate;
    }

}
