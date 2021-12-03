package com.RuleApi.service;

import javax.mail.MessagingException;

import org.springframework.mail.javamail.MimeMessageHelper;

public interface MailService {

    void handleAttachments(MimeMessageHelper mimeMessageHelper, String subject, String[] attachmentFilePaths);


    /**
     * 发送附件
     * @param subject 邮件主题
     * @param content 邮件内容
     * @param toEmails 接收的邮箱
     * @param ccPeoples 抄送人
     * @param bccPeoples 密送人，就是发送人和抄送人那里都不显示该收件人，但能收到邮件。
     * @param attachmentFilePaths 附件
     * @throws MessagingException
     */
    void send(String subject, String content, String[] toEmails, String[] ccPeoples,
              String[] bccPeoples, String[] attachmentFilePaths) throws MessagingException;


    /**
     * 发送附件
     * @param subject String 邮件主题
     * @param content String 邮件内容
     * @param toEmails String[] 接收的邮箱
     */
    void send(String subject, String content, String[] toEmails) throws MessagingException;


    /**
     * 发送附件
     * @param subject String 邮件主题
     * @param content String 邮件内容
     * @param toEmails String[] 接收的邮箱
     * @param attachmentFilePaths String[] 附件
     * <li>new String[] {"F:\\0desk\\邮件文档哦.pdf", "F:\\0desk\\maven-jar-plugin.txt"}</li>
     * @throws MessagingException
     */
    void send(String subject, String content, String[] toEmails, String[] attachmentFilePaths)
            throws MessagingException;

}