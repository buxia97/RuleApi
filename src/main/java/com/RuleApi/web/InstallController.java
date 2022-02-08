package com.RuleApi.web;

import com.RuleApi.common.RedisHelp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
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

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${mybatis.configuration.variables.prefix}")
    private String prefix;

    @Value("${web.prefix}")
    private String dataprefix;

    RedisHelp redisHelp =new RedisHelp();
    /***
     * 文章删除
     */
    @RequestMapping(value = "/newInstall")
    @ResponseBody
    public String newInstall() {

        String isInstall = redisHelp.getRedis(this.dataprefix+"_"+"isInstall",redisTemplate);
        if(isInstall!=null){
            return "虽然重复执行也没关系，但是还是尽量不要频繁点哦，十分钟后再来操作吧！";
        }
        Integer i = 1;
        //判断typecho是否安装，或者数据表前缀是否正确

        String text = "执行信息 ------";
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_users';", Integer.class);
        if (i == 0){
            return "Typecho未安装或者数据表前缀不正确，请尝试安装typecho或者修改properties配置文件。";
        }else{
            text+="Typecho程序确认安装。";
        }
        //修改请求头
        jdbcTemplate.execute("ALTER TABLE "+prefix+"_comments MODIFY agent varchar(500);");
        //查询文章表是否存在views字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_contents' and column_name = 'views';", Integer.class);
        if (i == 0){
            //新增字段
            jdbcTemplate.execute("alter table "+prefix+"_contents ADD views integer(10) DEFAULT 0;");
            text+="数据表typecho_contents，字段views添加完成。";
        }else{
            text+="数据表typecho_contents，字段views已经存在，无需添加。";
        }
        //查询文章表是否存在likes字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_contents' and column_name = 'likes';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_contents ADD likes integer(10) DEFAULT 0;");
            text+="数据表typecho_contents，字段likes添加完成。";
        }else{
            text+="数据表typecho_contents，字段likes已经存在，无需添加。";
        }
        //查询用户表是否存在introduce字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_users' and column_name = 'introduce';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_users ADD introduce varchar(255);");
            text+="数据表typecho_users，字段introduce添加完成。";
        }else{
            text+="数据表typecho_users，字段introduce已经存在，无需添加。";
        }
        //查询用户表是否存在account字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_users' and column_name = 'assets';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table typecho_users ADD assets integer(11) DEFAULT 0;");
            text+="数据表typecho_users，字段assets添加完成。";
        }else{
            text+="数据表typecho_users，字段assets已经存在，无需添加。";
        }
        //查询用户表是否存在address字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_users' and column_name = 'address';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table typecho_users ADD address text;");
            text+="数据表typecho_users，字段address添加完成。";
        }else{
            text+="数据表typecho_users，字段address已经存在，无需添加。";
        }
        //查询用户表是否存在address字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_users' and column_name = 'pay';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table typecho_users ADD pay text;");
            text+="数据表typecho_users，字段pay添加完成。";
        }else{
            text+="数据表typecho_users，字段pay已经存在，无需添加。";
        }
        //判断用户日志表是否存在
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_userlog';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("CREATE TABLE `"+prefix+"_userlog` (" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT," +
                    "  `uid` int(11) NOT NULL DEFAULT '-1' COMMENT '用户id'," +
                    "  `cid` int(11) NOT NULL DEFAULT '0'," +
                    "  `type` varchar(255) DEFAULT NULL COMMENT '类型'," +
                    "  `num` int(11) DEFAULT '0' COMMENT '数值，用于后期扩展'," +
                    "  `created` int(10) NOT NULL DEFAULT '0' COMMENT '时间'," +
                    "  PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='用户日志（收藏，扩展等）';");
            text+="数据表typecho_userlog创建完成。";
        }else{
            text+="数据表typecho_userlog已经存在，无需添加。";
        }
        //查询日志表是否存在toid字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_userlog' and column_name = 'toid';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_userlog ADD toid integer(11) DEFAULT 0;");
            text+="数据表typecho_userlog，字段toid添加完成。";
        }else{
            text+="数据表typecho_userlog，字段toid已经存在，无需添加。";
        }
        //判断用户社会化API表是否存在
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_userapi';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("CREATE TABLE `"+prefix+"_userapi` (" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT," +
                    "  `headImgUrl` varchar(255) DEFAULT NULL COMMENT '头像，可能用不上'," +
                    "  `openId` varchar(255) DEFAULT NULL COMMENT '开放平台ID'," +
                    "  `access_token` varchar(255) DEFAULT NULL COMMENT '唯一值'," +
                    "  `appLoginType` varchar(255) DEFAULT NULL COMMENT '渠道类型'," +
                    "  `uid` int(11) DEFAULT '0' COMMENT '用户ID'," +
                    "  PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='社会化登陆';");
            text+="数据表typecho_userapi创建完成。";
        }else{
            text+="数据表typecho_userapi已经存在，无需添加。";
        }


        //积分商品&积分阅读模块
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_shop';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("CREATE TABLE `"+prefix+"_shop` (" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT," +
                    "  `title` varchar(300) DEFAULT NULL COMMENT '商品标题'," +
                    "  `imgurl` varchar(500) DEFAULT NULL COMMENT '商品图片'," +
                    "  `text` text COMMENT '商品内容'," +
                    "  `price` int(11) DEFAULT '0' COMMENT '商品价格'," +
                    "  `num` int(11) DEFAULT '0' COMMENT '商品数量'," +
                    "  `type` int(11) DEFAULT '1' COMMENT '商品类型（实体，源码，工具，教程）'," +
                    "  `value` text COMMENT '收费显示（除实体外，这个字段购买后显示）'," +
                    "  `cid` int(11) DEFAULT '-1' COMMENT '所属文章'," +
                    "  `uid` int(11) DEFAULT '0' COMMENT '发布人'," +
                    "  PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='商品表';");
            text+="数据表typecho_shop创建完成。";
        }else{
            text+="数据表typecho_shop已经存在，无需添加。";
        }
        //查询商品表是否存在created字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_shop' and column_name = 'created';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_shop ADD created integer(10) DEFAULT 0;");
            text+="数据表typecho_shop，字段created添加完成。";
        }else{
            text+="数据表typecho_shop，字段created已经存在，无需添加。";
        }
        //查询商品表是否存在created字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_shop' and column_name = 'status';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_shop ADD status integer(10) DEFAULT 0;");
            text+="数据表typecho_shop，字段status添加完成。";
        }else{
            text+="数据表typecho_shop，字段status已经存在，无需添加。";
        }
        //判断充值记录表是否存在
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_paylog';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("CREATE TABLE `"+prefix+"_paylog` (" +
                    "  `pid` int(11) NOT NULL AUTO_INCREMENT," +
                    "  `subject` varchar(255) DEFAULT NULL," +
                    "  `total_amount` varchar(255) DEFAULT NULL," +
                    "  `out_trade_no` varchar(255) DEFAULT NULL," +
                    "  `trade_no` varchar(255) DEFAULT NULL," +
                    "  `paytype` varchar(255) DEFAULT '' COMMENT '支付类型'," +
                    "  `uid` int(11) DEFAULT '-1' COMMENT '充值人ID'," +
                    "  `created` int(10) DEFAULT NULL," +
                    "  `status` int(11) DEFAULT '0' COMMENT '支付状态（0未支付，1已支付）'," +
                    "  PRIMARY KEY (`pid`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='支付渠道充值记录';");
            text+="数据表typecho_paylog创建完成。";
        }else{
            text+="数据表typecho_paylog已经存在，无需添加。";
        }

        text+=" ------ 执行结束，安装执行完成";

        redisHelp.setRedis(this.dataprefix+"_"+"isInstall","1",600,redisTemplate);
        return text;
    }
}
