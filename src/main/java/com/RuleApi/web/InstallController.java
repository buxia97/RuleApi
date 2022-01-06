package com.RuleApi.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 初次安装控制器
 *
 * 用户检测数据表和字段是否存在，不存在则添加实现安装
 * */
@Controller
@RequestMapping(value = "/install")
public class InstallController {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    /***
     * 文章删除
     */
    @RequestMapping(value = "/newInstall")
    @ResponseBody
    public String newInstall() {
        String text = "执行信息 ||";
        //查询文章表是否存在views字段
        Integer i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = 'typecho_contents' and column_name = 'views';", Integer.class);
        if (i == 0){
            //新增字段
            jdbcTemplate.execute("alter table typecho_contents ADD views integer(10) DEFAULT 0;");
            text+="数据表typecho_contents，字段views添加完成。/";
        }
        //查询文章表是否存在likes字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = 'typecho_contents' and column_name = 'likes';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table typecho_contents ADD likes integer(10) DEFAULT 0;");
            text+="数据表typecho_contents，字段likes添加完成。/";
        }
        //查询用户表是否存在introduce字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = 'typecho_users' and column_name = 'introduce';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table typecho_users ADD introduce varchar(255);");
            text+="数据表typecho_users，字段introduce添加完成。/";
        }
        //查询用户表是否存在account字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = 'typecho_users' and column_name = 'account';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table typecho_users ADD account integer(11) DEFAULT 0;");
            text+="数据表typecho_users，字段account添加完成。/";
        }
        //判断用户日志表是否存在
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = 'typecho_userlog';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("CREATE TABLE `typecho_userlog` (" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT," +
                    "  `uid` int(11) NOT NULL DEFAULT '-1' COMMENT '用户id'," +
                    "  `cid` int(11) NOT NULL DEFAULT '0'," +
                    "  `type` varchar(255) DEFAULT NULL COMMENT '类型'," +
                    "  `num` int(11) DEFAULT '0' COMMENT '数值，用于后期扩展'," +
                    "  `created` int(10) NOT NULL DEFAULT '0' COMMENT '时间'," +
                    "  PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='用户日志（收藏，扩展等）';");
            text+="数据表typecho_userlog创建完成。/";
        }
        if (i == 0){
            text+="没有可执行的操作";
        }
        text+="|| 执行结束，安装执行完成";
        return text;
    }
}
