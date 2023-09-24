package com.RuleApi.web;

import com.RuleApi.common.PHPass;
import com.RuleApi.common.RedisHelp;
import com.RuleApi.common.ResultAll;
import com.RuleApi.entity.TypechoUsers;
import com.RuleApi.service.TypechoUsersService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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

    @Autowired
    private TypechoUsersService usersService;

    @Value("${mybatis.configuration.variables.prefix}")
    private String prefix;

    @Value("${web.prefix}")
    private String dataprefix;

    @Value("${webinfo.key}")
    private String key;



    RedisHelp redisHelp =new RedisHelp();
    ResultAll Result = new ResultAll();
    PHPass phpass = new PHPass(8);
    /***
     * 检测环境和应用
     */
    @RequestMapping(value = "/isInstall")
    @ResponseBody
    public String isInstall(){
        Integer code = 1;
        String msg = "安装正常";
        try {
            String isInstall = redisHelp.getRedis(this.dataprefix+"_"+"isInstall",redisTemplate);

        }catch (Exception e){
            code = 100;
            msg =  "Redis连接失败或未安装";
        }
        try {
            Integer i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_users';", Integer.class);
            if (i.equals(0)){
                code = 101;
                msg =  "Typecho未安装或者数据表前缀不正确。";
            }
        }catch (Exception e){
            code = 102;
            msg =  "Mysql数据库连接失败或未安装";
        }
        JSONObject response = new JSONObject();
        response.put("code" , code);
        response.put("msg"  ,msg);
        return response.toString();
    }
    /***
     * 安装Typecho数据库
     */
    @RequestMapping(value = "/typechoInstall")
    @ResponseBody
    public String typechoInstall(@RequestParam(value = "webkey", required = false,defaultValue = "") String  webkey,@RequestParam(value = "name", required = false) String  name,@RequestParam(value = "password", required = false) String  password) {
        if(!webkey.equals(this.key)){
            return Result.getResultJson(0,"请输入正确的访问KEY。如果忘记，可在服务器/opt/application.properties中查看",null);
        }
        if(name==null||password==null){
            return Result.getResultJson(0,"请求参数错误！",null);
        }
        String isRepeated = redisHelp.getRedis("isTypechoInstall",redisTemplate);
        if(isRepeated==null){
            redisHelp.setRedis("isTypechoInstall","1",15,redisTemplate);
        }else{
            return Result.getResultJson(0,"你的操作太频繁了",null);
        }
        String text = "执行信息 ------";
        Integer i = 1;
        //判断typecho是否安装，或者数据表前缀是否正确
        try {
            i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_users';", Integer.class);
            if (i > 0){
                return Result.getResultJson(0,"Typecho数据库已载入，无需重试",null);
            }
        }catch (Exception e){
            return Result.getResultJson(0,"Mysql数据库连接失败或未安装",null);
        }
        try {
            //安装用户表
            jdbcTemplate.execute("CREATE TABLE `"+prefix+"_users` (" +
                    "  `uid` int(10) unsigned NOT NULL AUTO_INCREMENT," +
                    "  `name` varchar(32) DEFAULT NULL," +
                    "  `password` varchar(64) DEFAULT NULL," +
                    "  `mail` varchar(200) DEFAULT NULL," +
                    "  `url` varchar(200) DEFAULT NULL," +
                    "  `screenName` varchar(32) DEFAULT NULL," +
                    "  `created` int(10) unsigned DEFAULT '0'," +
                    "  `activated` int(10) unsigned DEFAULT '0'," +
                    "  `logged` int(10) unsigned DEFAULT '0'," +
                    "  `group` varchar(16) DEFAULT 'visitor'," +
                    "  `authCode` varchar(64) DEFAULT NULL," +
                    "  PRIMARY KEY (`uid`)," +
                    "  UNIQUE KEY `name` (`name`)," +
                    "  UNIQUE KEY `mail` (`mail`)" +
                    ") ENGINE=MyISAM AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;");
            text+="用户表创建完成。";
            String passwd = phpass.HashPassword(password);
            Long date = System.currentTimeMillis();
            String userTime = String.valueOf(date).substring(0, 10);
            TypechoUsers user = new TypechoUsers();
            user.setName(name);
            user.setPassword(passwd);
            user.setCreated(Integer.parseInt(userTime));
            user.setGroupKey("administrator");
            usersService.insert(user);
            text+="管理员"+name+"添加完成。";
            //安装内容表
            jdbcTemplate.execute("CREATE TABLE `"+prefix+"_contents` (" +
                    "  `cid` int(10) unsigned NOT NULL AUTO_INCREMENT," +
                    "  `title` varchar(200) DEFAULT NULL," +
                    "  `slug` varchar(200) DEFAULT NULL," +
                    "  `created` int(10) unsigned DEFAULT '0'," +
                    "  `modified` int(10) unsigned DEFAULT '0'," +
                    "  `text` longtext," +
                    "  `order` int(10) unsigned DEFAULT '0'," +
                    "  `authorId` int(10) unsigned DEFAULT '0'," +
                    "  `template` varchar(32) DEFAULT NULL," +
                    "  `type` varchar(16) DEFAULT 'post'," +
                    "  `status` varchar(16) DEFAULT 'publish'," +
                    "  `password` varchar(32) DEFAULT NULL," +
                    "  `commentsNum` int(10) unsigned DEFAULT '0'," +
                    "  `allowComment` char(1) DEFAULT '0'," +
                    "  `allowPing` char(1) DEFAULT '0'," +
                    "  `allowFeed` char(1) DEFAULT '0'," +
                    "  `parent` int(10) unsigned DEFAULT '0'," +
                    "  PRIMARY KEY (`cid`)," +
                    "  UNIQUE KEY `slug` (`slug`)," +
                    "  KEY `created` (`created`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8;");
            text+="内容表创建完成。";
            //安装评论表
            jdbcTemplate.execute("CREATE TABLE `"+prefix+"_comments` (" +
                    "  `coid` int(10) unsigned NOT NULL AUTO_INCREMENT," +
                    "  `cid` int(10) unsigned DEFAULT '0'," +
                    "  `created` int(10) unsigned DEFAULT '0'," +
                    "  `author` varchar(200) DEFAULT NULL," +
                    "  `authorId` int(10) unsigned DEFAULT '0'," +
                    "  `ownerId` int(10) unsigned DEFAULT '0'," +
                    "  `mail` varchar(200) DEFAULT NULL," +
                    "  `url` varchar(200) DEFAULT NULL," +
                    "  `ip` varchar(64) DEFAULT NULL," +
                    "  `agent` varchar(200) DEFAULT NULL," +
                    "  `text` text," +
                    "  `type` varchar(16) DEFAULT 'comment'," +
                    "  `status` varchar(16) DEFAULT 'approved'," +
                    "  `parent` int(10) unsigned DEFAULT '0'," +
                    "  PRIMARY KEY (`coid`)," +
                    "  KEY `cid` (`cid`)," +
                    "  KEY `created` (`created`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8;");
            text+="评论表创建完成。";
            //自定义字段表
            jdbcTemplate.execute("CREATE TABLE `"+prefix+"_fields` (" +
                    "  `cid` int(10) unsigned NOT NULL," +
                    "  `name` varchar(200) NOT NULL," +
                    "  `type` varchar(8) DEFAULT 'str'," +
                    "  `str_value` text," +
                    "  `int_value` int(10) DEFAULT '0'," +
                    "  `float_value` float DEFAULT '0'," +
                    "  PRIMARY KEY (`cid`,`name`)," +
                    "  KEY `int_value` (`int_value`)," +
                    "  KEY `float_value` (`float_value`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8;");
            text+="自定义字段表创建完成。";
            //分类标签表
            jdbcTemplate.execute("CREATE TABLE `"+prefix+"_metas` (" +
                    "  `mid` int(10) unsigned NOT NULL AUTO_INCREMENT," +
                    "  `name` varchar(200) DEFAULT NULL," +
                    "  `slug` varchar(200) DEFAULT NULL," +
                    "  `type` varchar(32) NOT NULL," +
                    "  `description` varchar(200) DEFAULT NULL," +
                    "  `count` int(10) unsigned DEFAULT '0'," +
                    "  `order` int(10) unsigned DEFAULT '0'," +
                    "  `parent` int(10) unsigned DEFAULT '0'," +
                    "  PRIMARY KEY (`mid`)," +
                    "  KEY `slug` (`slug`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8;");
            text+="分类标签表创建完成。";
            //数据关联表
            jdbcTemplate.execute("CREATE TABLE `"+prefix+"_relationships` (" +
                    "  `cid` int(10) unsigned NOT NULL," +
                    "  `mid` int(10) unsigned NOT NULL," +
                    "  PRIMARY KEY (`cid`,`mid`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8;");
            text+="数据关联表创建完成。";
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0,"数据库语句执行失败，请检查数据库版本及服务器性能后重试。",null);
        }
        text+=" ------ 执行结束，独立安装数据表导入完成，请继续安装RuleApi扩展数据表";
        JSONObject response = new JSONObject();
        response.put("code" , 1);
        response.put("msg"  ,text);
        return response.toString();
    }
    /***
     * 新安装
     */
    @RequestMapping(value = "/newInstall")
    @ResponseBody
    public String newInstall(@RequestParam(value = "webkey", required = false,defaultValue = "") String  webkey) {
        if(!webkey.equals(this.key)){
            return "请输入正确的访问KEY。如果忘记，可在服务器/opt/application.properties中查看";
        }
        try {
            String isInstall = redisHelp.getRedis(this.dataprefix+"_"+"isInstall",redisTemplate);
            if(isInstall!=null){
                return "虽然重复执行也没关系，但是还是尽量不要频繁点哦，1分钟后再来操作吧！";
            }
        }catch (Exception e){
            return "Redis连接失败或未安装";
        }
        Integer i = 1;
        String text = "执行信息 ------";
        //判断typecho是否安装，或者数据表前缀是否正确
        try {
            i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_users';", Integer.class);
            if (i == 0){
                return "Typecho未安装或者数据表前缀不正确，请尝试安装typecho或者修改properties配置文件。";
            }else{
                text+="Typecho程序确认安装。";
            }
        }catch (Exception e){
            return "Mysql数据库连接失败或未安装";
        }
        //修改请求头
        jdbcTemplate.execute("ALTER TABLE "+prefix+"_comments MODIFY agent varchar(520);");
        //查询文章表是否存在views字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_contents' and column_name = 'views';", Integer.class);
        if (i == 0){
            //新增字段
            jdbcTemplate.execute("alter table "+prefix+"_contents ADD views integer(10) DEFAULT 0;");
            text+="内容模块，字段views添加完成。";
        }else{
            text+="内容模块，字段views已经存在，无需添加。";
        }
        //查询文章表是否存在likes字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_contents' and column_name = 'likes';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_contents ADD likes integer(10) DEFAULT 0;");
            text+="内容模块，字段likes添加完成。";
        }else{
            text+="内容模块，字段likes已经存在，无需添加。";
        }
        //查询文章表是否存在isrecommend字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_contents' and column_name = 'isrecommend';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_contents ADD isrecommend integer(2) DEFAULT 0;");
            text+="内容模块，字段isrecommend添加完成。";
        }else{
            text+="内容模块，字段isrecommend已经存在，无需添加。";
        }
        //查询文章表是否存在istop字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_contents' and column_name = 'istop';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_contents ADD istop integer(2) DEFAULT 0;");
            text+="内容模块，字段istop添加完成。";
        }else{
            text+="内容模块，字段istop已经存在，无需添加。";
        }
        //查询文章表是否存在isswiper字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_contents' and column_name = 'isswiper';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_contents ADD isswiper integer(2) DEFAULT 0;");
            text+="内容模块，字段isswiper添加完成。";
        }else{
            text+="内容模块，字段isswiper已经存在，无需添加。";
        }
        //查询文章评论表是否存在likes字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_comments' and column_name = 'likes';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_comments ADD likes integer(11) DEFAULT 0;");
            text+="文章评论表，字段likes添加完成。";
        }else{
            text+="文章评论表，字段likes已经存在，无需添加。";
        }
        //查询用户表是否存在introduce字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_users' and column_name = 'introduce';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_users ADD introduce varchar(255);");
            text+="用户模块，字段introduce添加完成。";
        }else{
            text+="用户模块，字段introduce已经存在，无需添加。";
        }
        //查询用户表是否存在account字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_users' and column_name = 'assets';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_users ADD assets integer(11) DEFAULT 0;");
            text+="用户模块，字段assets添加完成。";
        }else{
            text+="用户模块，字段assets已经存在，无需添加。";
        }
        //查询用户表是否存在address字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_users' and column_name = 'address';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_users ADD address text;");
            text+="用户模块，字段address添加完成。";
        }else{
            text+="用户模块，字段address已经存在，无需添加。";
        }
        //查询用户表是否存在address字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_users' and column_name = 'pay';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_users ADD pay text;");
            text+="用户模块，字段pay添加完成。";
        }else{
            text+="用户模块，字段pay已经存在，无需添加。";
        }
        //查询用户表是否存在customize字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_users' and column_name = 'customize';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_users ADD customize varchar(255) DEFAULT NULL;");
            text+="用户模块，字段customize添加完成。";
        }else{
            text+="用户模块，字段customize已经存在，无需添加。";
        }
        //查询用户表是否存在vip字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_users' and column_name = 'vip';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_users ADD vip integer(10) DEFAULT 0;");
            text+="用户模块，字段vip添加完成。";
        }else{
            text+="用户模块，字段vip已经存在，无需添加。";
        }
        //查询用户表是否存在experience字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_users' and column_name = 'experience';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_users ADD experience integer(11) DEFAULT 0;");
            text+="用户模块，字段experience添加完成。";
        }else{
            text+="用户模块，字段experience已经存在，无需添加。";
        }

        //查询用户表是否存在avatar字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_users' and column_name = 'avatar';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_users ADD avatar text;");
            text+="用户模块，字段avatar添加完成。";
        }else{
            text+="用户模块，字段avatar已经存在，无需添加。";
        }
        //查询用户表是否存在clientId字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_users' and column_name = 'clientId';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_users ADD clientId varchar(255) DEFAULT NULL;");
            text+="用户模块，字段clientId添加完成。";
        }else{
            text+="用户模块，字段clientId已经存在，无需添加。";
        }
        //查询用户表是否存在bantime字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_users' and column_name = 'bantime';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_users ADD bantime integer(10) DEFAULT 0;");
            text+="用户模块，字段bantime添加完成。";
        }else{
            text+="用户模块，字段bantime已经存在，无需添加。";
        }
        //查询用户表是否存在posttime字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_users' and column_name = 'posttime';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_users ADD posttime integer(10) DEFAULT 0;");
            text+="用户模块，字段posttime添加完成。";
        }else{
            text+="用户模块，字段posttime已经存在，无需添加。";
        }

        //查询分类标签表是否存在imgurl字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_metas' and column_name = 'imgurl';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_metas ADD imgurl varchar(500) DEFAULT NULL;");
            text+="标签分类模块，字段imgurl添加完成。";
        }else{
            text+="标签分类模块，字段imgurl已经存在，无需添加。";
        }
        //查询分类标签表是否存在isrecommend字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_metas' and column_name = 'isrecommend';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_metas ADD isrecommend integer(2) DEFAULT 0;");
            text+="标签分类模块，字段isrecommend添加完成。";
        }else{
            text+="标签分类模块，字段isrecommend已经存在，无需添加。";
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
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
            text+="用户操作模块创建完成。";
        }else{
            text+="用户操作模块已经存在，无需添加。";
        }
        //查询日志表是否存在toid字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_userlog' and column_name = 'toid';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_userlog ADD toid integer(11) DEFAULT 0;");
            text+="用户操作模块，字段toid添加完成。";
        }else{
            text+="用户操作模块，字段toid已经存在，无需添加。";
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
            text+="社会化登录模块创建完成。";
        }else{
            text+="社会化登录模块已经存在，无需添加。";
        }
        try {
            Thread.sleep(500);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
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
            text+="积分商城模块创建完成。";
        }else{
            text+="积分商城模块已经存在，无需添加。";
        }

        //查询商品表是否存在vipDiscount字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_shop' and column_name = 'vipDiscount';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_shop ADD vipDiscount varchar(255) NOT NULL DEFAULT '0.1' COMMENT 'VIP折扣，权高于系统设置折扣';");
            text+="积分商城模块，字段vipDiscount添加完成。";
        }else{
            text+="积分商城模块，字段vipDiscount已经存在，无需添加。";
        }
        //查询商品表是否存在created字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_shop' and column_name = 'created';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_shop ADD created integer(10) DEFAULT 0;");
            text+="积分商城模块，字段created添加完成。";
        }else{
            text+="积分商城模块，字段created已经存在，无需添加。";
        }
        //查询商品表是否存在status字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_shop' and column_name = 'status';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_shop ADD status integer(10) DEFAULT 0;");
            text+="积分商城模块，字段status添加完成。";
        }else{
            text+="积分商城模块，字段status已经存在，无需添加。";
        }
        //查询商品表是否存在sellNum字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_shop' and column_name = 'sellNum';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_shop ADD sellNum integer(11) DEFAULT 0;");
            text+="积分商城模块，字段sellNum添加完成。";
        }else{
            text+="积分商城模块，字段sellNum已经存在，无需添加。";
        }
        //查询聊天室模块是否存在isMd字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_shop' and column_name = 'isMd';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_shop ADD `isMd` int(2) unsigned DEFAULT '1' COMMENT '是否为Markdown编辑器发布'");
            text+="积分商城模块，字段isMd添加完成。";
        }else{
            text+="积分商城模块，字段isMd已经存在，无需添加。";
        }
        //查询聊天室模块是否存在sort字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_shop' and column_name = 'sort';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_shop ADD `sort` int(11) unsigned DEFAULT '0' COMMENT '商品大类'");
            text+="积分商城模块，字段sort添加完成。";
        }else{
            text+="积分商城模块，字段sort已经存在，无需添加。";
        }
        //查询聊天室模块是否存在subtype字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_shop' and column_name = 'subtype';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_shop ADD `subtype` int(11) unsigned DEFAULT '0' COMMENT '子类型'");
            text+="积分商城模块，字段subtype添加完成。";
        }else{
            text+="积分商城模块，字段subtype已经存在，无需添加。";
        }
        //查询聊天室模块是否存在isView字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_shop' and column_name = 'isView';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_shop ADD `isView` int(2) unsigned DEFAULT '1' COMMENT '是否可见'");
            text+="积分商城模块，字段isView添加完成。";
        }else{
            text+="积分商城模块，字段isView已经存在，无需添加。";
        }
        //安装商品分类表
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_shoptype';", Integer.class);
        if (i == 0) {
            jdbcTemplate.execute("CREATE TABLE `" + prefix + "_shoptype` (" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT," +
                    "  `parent` int(11) DEFAULT '0' COMMENT '上级分类'," +
                    "  `name` varchar(255) DEFAULT NULL COMMENT '分类名称'," +
                    "  `pic` varchar(400) DEFAULT NULL COMMENT '分类缩略图'," +
                    "  `intro` varchar(400) DEFAULT NULL COMMENT '分类简介'," +
                    "  `orderKey` int(11) DEFAULT '0' COMMENT '分类排序'," +
                    "  PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM AUTO_INCREMENT=7 DEFAULT CHARSET=utf8 COMMENT='商品分类表';");
            text += "商品分类表创建完成。";
        }else{
            text+="商品分类表已存在，无需安装。";
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
            text+="资产日志模块创建完成。";
        }else{
            text+="资产日志模块已经存在，无需添加。";
        }

        //添加卡密充值模块
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_paykey';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("CREATE TABLE `"+prefix+"_paykey` (" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT," +
                    "  `value` varchar(255) DEFAULT '' COMMENT '密钥'," +
                    "  `price` int(11) DEFAULT '0' COMMENT '等值积分'," +
                    "  `status` int(2) DEFAULT '0' COMMENT '0未使用，1已使用'," +
                    "  `created` int(10) DEFAULT '0' COMMENT '创建时间'," +
                    "  `uid` int(11) DEFAULT '-1' COMMENT '使用用户'," +
                    "  PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='卡密充值相关';");
            text+="卡密充值模块创建完成。";
        }else{
            text+="卡密充值模块已经存在，无需添加。";
        }
        try {
            Thread.sleep(500);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        //添加API配置中心模块
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("CREATE TABLE `"+prefix+"_apiconfig` (" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT," +
                    "  `webinfoTitle` varchar(500) NOT NULL DEFAULT '' COMMENT '网站名称'," +
                    "  `webinfoUrl` varchar(500) NOT NULL DEFAULT '' COMMENT '网站URL'," +
                    "  `webinfoUploadUrl` varchar(255) NOT NULL DEFAULT 'http://127.0.0.1:8081/' COMMENT '本地图片访问路径'," +
                    "  `webinfoAvatar` varchar(500) NOT NULL DEFAULT 'https://cdn.helingqi.com/wavatar/' COMMENT '头像源'," +
                    "  `pexelsKey` varchar(255) NOT NULL DEFAULT '' COMMENT '图库key'," +
                    "  `scale` int(11) NOT NULL DEFAULT '100' COMMENT '一元能买多少积分'," +
                    "  `clock` int(11) NOT NULL DEFAULT '5' COMMENT '签到最多多少积分'," +
                    "  `vipPrice` int(11) NOT NULL DEFAULT '200' COMMENT 'VIP一天价格'," +
                    "  `vipDay` int(11) NOT NULL DEFAULT '300' COMMENT '多少天VIP等于永久'," +
                    "  `vipDiscount` varchar(11) NOT NULL DEFAULT '0.1' COMMENT 'VIP折扣'," +
                    "  `isEmail` int(2) NOT NULL DEFAULT '1' COMMENT '邮箱开关（0完全关闭邮箱，1只开启邮箱注册，2邮箱注册和操作通知）'," +
                    "  `isInvite` int(11) NOT NULL DEFAULT '0' COMMENT '注册是否验证邀请码（默认关闭）'," +
                    "  `cosAccessKey` varchar(300) NOT NULL DEFAULT ''," +
                    "  `cosSecretKey` varchar(300) NOT NULL DEFAULT ''," +
                    "  `cosBucket` varchar(255) NOT NULL DEFAULT ''," +
                    "  `cosBucketName` varchar(255) NOT NULL DEFAULT ''," +
                    "  `cosPath` varchar(255) DEFAULT ''," +
                    "  `cosPrefix` varchar(255) NOT NULL DEFAULT ''," +
                    "  `aliyunEndpoint` varchar(500) NOT NULL DEFAULT ''," +
                    "  `aliyunAccessKeyId` varchar(255) NOT NULL DEFAULT ''," +
                    "  `aliyunAccessKeySecret` varchar(255) NOT NULL DEFAULT ''," +
                    "  `aliyunAucketName` varchar(255) NOT NULL DEFAULT ''," +
                    "  `aliyunUrlPrefix` varchar(255) NOT NULL DEFAULT ''," +
                    "  `aliyunFilePrefix` varchar(255) NOT NULL DEFAULT ''," +
                    "  `ftpHost` varchar(255) NOT NULL DEFAULT ''," +
                    "  `ftpPort` int(11) NOT NULL DEFAULT '21'," +
                    "  `ftpUsername` varchar(255) NOT NULL DEFAULT ''," +
                    "  `ftpPassword` varchar(255) NOT NULL DEFAULT ''," +
                    "  `ftpBasePath` varchar(255) NOT NULL DEFAULT ''," +
                    "  `alipayAppId` varchar(255) NOT NULL DEFAULT ''," +
                    "  `alipayPrivateKey` text," +
                    "  `alipayPublicKey` text," +
                    "  `alipayNotifyUrl` varchar(500) NOT NULL DEFAULT ''," +
                    "  `appletsAppid` varchar(255) NOT NULL DEFAULT ''," +
                    "  `appletsSecret` text," +
                    "  `wxpayAppId` varchar(255) NOT NULL DEFAULT ''," +
                    "  `wxpayMchId` varchar(255) NOT NULL DEFAULT ''," +
                    "  `wxpayKey` text," +
                    "  `wxpayNotifyUrl` varchar(500) DEFAULT ''," +
                    "  PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='api配置信息表';");
            text+="API配置中心模块创建完成。";
            //修改请求头
            jdbcTemplate.execute("INSERT INTO `"+prefix+"_apiconfig` (webinfoTitle) VALUES ('网站名称');");
        }else{
            text+="API配置中心模块已经存在，无需添加。";
        }
        try {
            Thread.sleep(500);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        //查询配置中心表是否存在auditlevel字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'auditlevel';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD auditlevel integer(2) DEFAULT 1;");
            text+="配置中心模块，字段auditlevel添加完成。";
        }else{
            text+="配置中心模块，字段auditlevel已经存在，无需添加。";
        }
        //查询配置中心表是否存在forbidden字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'forbidden';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD forbidden text;");
            text+="配置中心模块，字段forbidden添加完成。";
        }else{
            text+="配置中心模块，字段forbidden已经存在，无需添加。";
        }
        //查询配置中心表是否存在qqAppletsAppid字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'qqAppletsAppid';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD qqAppletsAppid varchar(500) DEFAULT NULL;");
            text+="配置中心模块，字段qqAppletsAppid添加完成。";
        }else{
            text+="配置中心模块，字段qqAppletsAppid已经存在，无需添加。";
        }
        //查询配置中心表是否存在qqAppletsSecret字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'qqAppletsSecret';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD qqAppletsSecret varchar(500) DEFAULT NULL;");
            text+="配置中心模块，字段qqAppletsSecret添加完成。";
        }else{
            text+="配置中心模块，字段qqAppletsSecret已经存在，无需添加。";
        }
        //查询配置中心表是否存在wxAppId字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'wxAppId';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD wxAppId varchar(500) DEFAULT NULL;");
            text+="配置中心模块，字段wxAppId添加完成。";
        }else{
            text+="配置中心模块，字段wxAppId已经存在，无需添加。";
        }
        //查询配置中心表是否存在wxAppSecret字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'wxAppSecret';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD wxAppSecret varchar(500) DEFAULT NULL;");
            text+="配置中心模块，字段wxAppSecret添加完成。";
        }else{
            text+="配置中心模块，字段wxAppSecret已经存在，无需添加。";
        }
        //查询配置中心表是否存在fields字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'fields';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD fields varchar(500) DEFAULT 'able';");
            text+="配置中心模块，字段fields添加完成。";
        }else{
            text+="配置中心模块，字段fields已经存在，无需添加。";
        }
        //查询配置中心表是否存在pushAdsPrice字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'pushAdsPrice';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `pushAdsPrice` int(11) NOT NULL DEFAULT '100' COMMENT '推流广告价格(积分/天)'");
            text+="配置中心模块，字段pushAdsPrice添加完成。";
        }else{
            text+="配置中心模块，字段pushAdsPrice已经存在，无需添加。";
        }
        try {
            Thread.sleep(500);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        //查询配置中心表是否存在pushAdsNum字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'pushAdsNum';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `pushAdsNum` int(11) NOT NULL DEFAULT '10' COMMENT '推流广告数量'");
            text+="配置中心模块，字段pushAdsNum添加完成。";
        }else{
            text+="配置中心模块，字段pushAdsNum已经存在，无需添加。";
        }
        //查询配置中心表是否存在bannerAdsPrice字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'bannerAdsPrice';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `bannerAdsPrice` int(11) NOT NULL DEFAULT '100' COMMENT '横幅广告价格(积分/天)'");
            text+="配置中心模块，字段bannerAdsPrice添加完成。";
        }else{
            text+="配置中心模块，字段bannerAdsPrice已经存在，无需添加。";
        }
        //查询配置中心表是否存在bannerAdsNum字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'bannerAdsNum';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `bannerAdsNum` int(11) NOT NULL DEFAULT '5' COMMENT '横幅广告数量'");
            text+="配置中心模块，字段bannerAdsNum添加完成。";
        }else{
            text+="配置中心模块，字段bannerAdsNum已经存在，无需添加。";
        }
        //查询配置中心表是否存在startAdsPrice字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'startAdsPrice';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `startAdsPrice` int(11) NOT NULL DEFAULT '100' COMMENT '启动图广告价格(积分/天)'");
            text+="配置中心模块，字段startAdsPrice添加完成。";
        }else{
            text+="配置中心模块，字段startAdsPrice已经存在，无需添加。";
        }
        //查询配置中心表是否存在startAdsNum字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'startAdsNum';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `startAdsNum` int(11) NOT NULL DEFAULT '1' COMMENT '启动图广告数量'");
            text+="配置中心模块，字段startAdsNum添加完成。";
        }else{
            text+="配置中心模块，字段startAdsNum已经存在，无需添加。";
        }
        //查询配置中心表是否存在epayUrl字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'epayUrl';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `epayUrl` varchar(500) DEFAULT '' COMMENT '易支付接口地址'");
            text+="配置中心模块，字段epayUrl添加完成。";
        }else{
            text+="配置中心模块，字段epayUrl已经存在，无需添加。";
        }
        //查询配置中心表是否存在epayPid字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'epayPid';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `epayPid` int(11) COMMENT '易支付商户ID'");
            text+="配置中心模块，字段epayPid添加完成。";
        }else{
            text+="配置中心模块，字段epayPid已经存在，无需添加。";
        }
        //查询配置中心表是否存在epayKey字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'epayKey';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `epayKey` varchar(300) DEFAULT '' COMMENT '易支付商户密钥'");
            text+="配置中心模块，字段epayKey添加完成。";
        }else{
            text+="配置中心模块，字段epayKey已经存在，无需添加。";
        }
        //查询配置中心表是否存在epayNotifyUrl字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'epayNotifyUrl';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `epayNotifyUrl` varchar(500) DEFAULT '' COMMENT '易支付回调地址'");
            text+="配置中心模块，字段epayNotifyUrl添加完成。";
        }else{
            text+="配置中心模块，字段epayNotifyUrl已经存在，无需添加。";
        }
        //查询配置中心表是否存在mchSerialNo字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'mchSerialNo';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `mchSerialNo` text COMMENT '微信支付商户证书序列号'");
            text+="配置中心模块，字段mchSerialNo添加完成。";
        }else{
            text+="配置中心模块，字段mchSerialNo已经存在，无需添加。";
        }
        //查询配置中心表是否存在mchApiV3Key字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'mchApiV3Key';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `mchApiV3Key` text COMMENT '微信支付API3私钥'");
            text+="配置中心模块，字段mchApiV3Key添加完成。";
        }else{
            text+="配置中心模块，字段mchApiV3Key已经存在，无需添加。";
        }
        //查询配置中心表是否存在cloudUid字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'cloudUid';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `cloudUid` varchar(255) DEFAULT '' COMMENT '云控UID'");
            text+="配置中心模块，字段cloudUid添加完成。";
        }else{
            text+="配置中心模块，字段cloudUid已经存在，无需添加。";
        }
        //查询配置中心表是否存在cloudUrl字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'cloudUrl';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `cloudUrl` varchar(255) DEFAULT '' COMMENT '云控URL'");
            text+="配置中心模块，字段cloudUrl添加完成。";
        }else{
            text+="配置中心模块，字段cloudUrl已经存在，无需添加。";
        }
        //查询配置中心表是否存在pushAppId字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'pushAppId';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `pushAppId` varchar(255) DEFAULT '' COMMENT 'pushAppId'");
            text+="配置中心模块，字段pushAppId添加完成。";
        }else{
            text+="配置中心模块，字段pushAppId已经存在，无需添加。";
        }
        //查询配置中心表是否存在pushAppKey字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'pushAppKey';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `pushAppKey` varchar(255) DEFAULT '' COMMENT 'pushAppKey'");
            text+="配置中心模块，字段pushAppKey添加完成。";
        }else{
            text+="配置中心模块，字段pushAppKey已经存在，无需添加。";
        }
        //查询配置中心表是否存在pushMasterSecret字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'pushMasterSecret';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `pushMasterSecret` varchar(255) DEFAULT '' COMMENT 'pushMasterSecret'");
            text+="配置中心模块，字段pushMasterSecret添加完成。";
        }else{
            text+="配置中心模块，字段pushMasterSecret已经存在，无需添加。";
        }
        //查询配置中心表是否存在isPush字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'isPush';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `isPush` int(2) DEFAULT '0' COMMENT '是否开启消息通知'");
            text+="配置中心模块，字段isPush添加完成。";
        }else{
            text+="配置中心模块，字段isPush已经存在，无需添加。";
        }
        //查询配置中心表是否存在disableCode字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'disableCode';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `disableCode` int(2) DEFAULT '0' COMMENT '是否禁用代码'");
            text+="配置中心模块，字段disableCode添加完成。";
        }else{
            text+="配置中心模块，字段disableCode已经存在，无需添加。";
        }
        //查询配置中心表是否存在allowDelete字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'allowDelete';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `allowDelete` int(2) DEFAULT '0' COMMENT '是否允许用户删除文章或评论'");
            text+="配置中心模块，字段allowDelete添加完成。";
        }else{
            text+="配置中心模块，字段allowDelete已经存在，无需添加。";
        }
        //查询配置中心表是否存在allowDelete字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'contentAuditlevel';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `contentAuditlevel` int(2) DEFAULT '2' COMMENT '内容审核模式'");
            text+="配置中心模块，字段contentAuditlevel添加完成。";
        }else{
            text+="配置中心模块，字段contentAuditlevel已经存在，无需添加。";
        }
        //查询配置中心表是否存在uploadLevel字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'uploadLevel';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `uploadLevel` int(2) DEFAULT '0' COMMENT '上传限制等级（0只允许图片，1关闭上传接口，2只允许图片视频，3允许所有类型文件）'");
            text+="配置中心模块，字段uploadLevel添加完成。";
        }else{
            text+="配置中心模块，字段uploadLevel已经存在，无需添加。";
        }
        //查询配置中心表是否存在clockExp字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'clockExp';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `clockExp` int(11) DEFAULT '5' COMMENT '签到经验'");
            text+="配置中心模块，字段clockExp添加完成。";
        }else{
            text+="配置中心模块，字段clockExp已经存在，无需添加。";
        }
        //查询配置中心表是否存在reviewExp字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'reviewExp';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `reviewExp` int(11) DEFAULT '1' COMMENT '每日前三次评论经验'");
            text+="配置中心模块，字段reviewExp添加完成。";
        }else{
            text+="配置中心模块，字段reviewExp已经存在，无需添加。";
        }
        //查询配置中心表是否存在postExp字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'postExp';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `postExp` int(11) DEFAULT '10' COMMENT '每日前三次发布内容经验（文章，动态，帖子）'");
            text+="配置中心模块，字段postExp添加完成。";
        }else{
            text+="配置中心模块，字段postExp已经存在，无需添加。";
        }
        //查询配置中心表是否存在violationExp字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'violationExp';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `violationExp` int(11) DEFAULT '50' COMMENT '违规扣除经验'");
            text+="配置中心模块，字段violationExp添加完成。";
        }else{
            text+="配置中心模块，字段violationExp已经存在，无需添加。";
        }
        //查询配置中心表是否存在deleteExp字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'deleteExp';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `deleteExp` int(11) DEFAULT '20' COMMENT '删除扣除经验（文章，评论，动态，帖子）'");
            text+="配置中心模块，字段deleteExp添加完成。";
        }else{
            text+="配置中心模块，字段deleteExp已经存在，无需添加。";
        }
        //查询配置中心表是否存在spaceMinExp字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'spaceMinExp';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `spaceMinExp` int(11) DEFAULT '20' COMMENT '发布动态要求最低经验值'");
            text+="配置中心模块，字段spaceMinExp添加完成。";
        }else{
            text+="配置中心模块，字段spaceMinExp已经存在，无需添加。";
        }
        //查询配置中心表是否存在chatMinExp字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'chatMinExp';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `chatMinExp` int(11) DEFAULT '20' COMMENT '聊天要求最低经验值'");
            text+="配置中心模块，字段chatMinExp添加完成。";
        }else{
            text+="配置中心模块，字段chatMinExp已经存在，无需添加。";
        }
        //查询配置中心表是否存在qiniuDomain字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'qiniuDomain';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `qiniuDomain` varchar(400) DEFAULT '' COMMENT '七牛云访问域名'");
            text+="配置中心模块，字段qiniuDomain添加完成。";
        }else{
            text+="配置中心模块，字段qiniuDomain已经存在，无需添加。";
        }
        //查询配置中心表是否存在qiniuAccessKey字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'qiniuAccessKey';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `qiniuAccessKey` varchar(400) DEFAULT '' COMMENT '七牛云公钥'");
            text+="配置中心模块，字段qiniuAccessKey添加完成。";
        }else{
            text+="配置中心模块，字段qiniuAccessKey已经存在，无需添加。";
        }
        //查询配置中心表是否存在qiniuSecretKey字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'qiniuSecretKey';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `qiniuSecretKey` varchar(400) DEFAULT '' COMMENT '七牛云私钥'");
            text+="配置中心模块，字段qiniuSecretKey添加完成。";
        }else{
            text+="配置中心模块，字段qiniuSecretKey已经存在，无需添加。";
        }
        //查询配置中心表是否存在qiniuBucketName字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'qiniuBucketName';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `qiniuBucketName` varchar(255) DEFAULT '' COMMENT '七牛云存储桶名称'");
            text+="配置中心模块，字段qiniuBucketName添加完成。";
        }else{
            text+="配置中心模块，字段qiniuBucketName已经存在，无需添加。";
        }
        //查询配置中心表是否存在silenceTime字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'silenceTime';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `silenceTime` int(11) DEFAULT '600' COMMENT '疑似攻击自动封禁时间(s)'");
            text+="配置中心模块，字段silenceTime添加完成。";
        }else{
            text+="配置中心模块，字段silenceTime已经存在，无需添加。";
        }
        //查询配置中心表是否存在interceptTime字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'interceptTime';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `interceptTime` int(11) DEFAULT '3600' COMMENT '多次触发违规自动封禁时间(s)'");
            text+="配置中心模块，字段interceptTime添加完成。";
        }else{
            text+="配置中心模块，字段interceptTime已经存在，无需添加。";
        }
        //查询配置中心表是否存在isLogin字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'isLogin';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `isLogin` int(2) DEFAULT '0' COMMENT '开启全局登录'");
            text+="配置中心模块，字段isLogin添加完成。";
        }else{
            text+="配置中心模块，字段isLogin已经存在，无需添加。";
        }

        //查询配置中心表是否存在postMax字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'postMax';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `postMax` int(11) DEFAULT '5' COMMENT '每日最大发布'");
            text+="配置中心模块，字段postMax添加完成。";
        }else{
            text+="配置中心模块，字段postMax已经存在，无需添加。";
        }
        //查询配置中心表是否存在forumAudit字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'forumAudit';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `forumAudit` int(11) DEFAULT '1' COMMENT '帖子及帖子评论是否需要审核'");
            text+="配置中心模块，字段forumAudit添加完成。";
        }else{
            text+="配置中心模块，字段forumAudit已经存在，无需添加。";
        }
        //查询配置中心表是否存在spaceAudit字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'spaceAudit';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `spaceAudit` int(11) DEFAULT '0' COMMENT '动态是否需要审核'");
            text+="配置中心模块，字段spaceAudit添加完成。";
        }else{
            text+="配置中心模块，字段spaceAudit已经存在，无需添加。";
        }
        //查询配置中心表是否存在uploadType字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'uploadType';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `uploadType` varchar(100) DEFAULT 'local' COMMENT '上传类型'");
            text+="配置中心模块，字段uploadType添加完成。";
        }else{
            text+="配置中心模块，字段uploadType已经存在，无需添加。";
        }
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'banRobots';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `banRobots` int(2) DEFAULT '0' COMMENT '是否开启机器人严格限制模式'");
            text+="配置中心模块，字段banRobots添加完成。";
        }else{
            text+="配置中心模块，字段banRobots已经存在，无需添加。";
        }
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'adsGiftNum';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `adsGiftNum` int(11) DEFAULT '10' COMMENT '每日广告奖励次数'");
            text+="配置中心模块，字段adsGiftNum添加完成。";
        }else{
            text+="配置中心模块，字段adsGiftNum已经存在，无需添加。";
        }
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_apiconfig' and column_name = 'adsGiftAward';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_apiconfig ADD `adsGiftAward` int(11) DEFAULT '5' COMMENT '每日广告奖励额'");
            text+="配置中心模块，字段adsGiftAward添加完成。";
        }else{
            text+="配置中心模块，字段adsGiftAward已经存在，无需添加。";
        }
        try {
            Thread.sleep(500);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        //添加邀请码模块
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_invitation';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("CREATE TABLE `"+prefix+"_invitation` (" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT," +
                    "  `code` varchar(255) DEFAULT NULL COMMENT '邀请码'," +
                    "  `created` int(10) DEFAULT '0' COMMENT '创建时间'," +
                    "  `uid` int(11) DEFAULT '0' COMMENT '创建者'," +
                    "  `status` int(2) DEFAULT '0' COMMENT '0未使用，1已使用'," +
                    "  PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='邀请码';");
            text+="邀请码模块创建完成。";
        }else{
            text+="邀请码模块已经存在，无需添加。";
        }
        //添加付费广告模块
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_ads';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("CREATE TABLE `"+prefix+"_ads` (" +
                    "  `aid` int(11) NOT NULL AUTO_INCREMENT," +
                    "  `name` varchar(255) DEFAULT '' COMMENT '广告名称'," +
                    "  `type` int(11) DEFAULT '0' COMMENT '广告类型（0推流，1横幅，2启动图，3轮播图）'," +
                    "  `img` varchar(500) DEFAULT NULL COMMENT '广告缩略图'," +
                    "  `close` int(10) DEFAULT '0' COMMENT '0代表永久，其它代表结束时间'," +
                    "  `created` int(10) unsigned DEFAULT '0' COMMENT '创建时间'," +
                    "  `price` int(11) unsigned DEFAULT '0' COMMENT '购买价格'," +
                    "  `intro` varchar(500) DEFAULT '' COMMENT '广告简介'," +
                    "  `urltype` int(11) DEFAULT '0' COMMENT '0为APP内部打开，1为跳出APP'," +
                    "  `url` text COMMENT '跳转Url'," +
                    "  `uid` int(11) DEFAULT '-1' COMMENT '发布人'," +
                    "  `status` int(2) DEFAULT '0' COMMENT '0审核中，1已公开，2已到期'," +
                    "  PRIMARY KEY (`aid`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='广告表';");
            text+="付费广告模块创建完成。";
        }else{
            text+="付费广告模块已经存在，无需添加。";
        }
        //添加消息通知模块
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_inbox';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("CREATE TABLE `"+prefix+"_inbox` (" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT," +
                    "  `type` varchar(255) DEFAULT NULL COMMENT '消息类型：system(系统消息)，comment(评论消息)，finance(财务消息)'," +
                    "  `uid` int(11) DEFAULT '0' COMMENT '消息发送人，0是平台'," +
                    "  `text` text COMMENT '消息内容（只有简略信息）'," +
                    "  `touid` int(11) NOT NULL DEFAULT '0' COMMENT '消息接收人uid'," +
                    "  `isread` int(2) DEFAULT '0' COMMENT '是否已读，0已读，1未读'," +
                    "  `value` int(11) DEFAULT '0' COMMENT '消息指向内容的id，根据类型跳转'," +
                    "  `created` int(10) unsigned DEFAULT '0' COMMENT '创建时间'," +
                    "  PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='消息表';");
            text+="消息通知模块创建完成。";
        }else{
            text+="消息通知模块已经存在，无需添加。";
        }

        //关注模块
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_fan';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("CREATE TABLE `"+prefix+"_fan` (" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT," +
                    "  `created` int(10) unsigned DEFAULT '0' COMMENT '关注时间'," +
                    "  `uid` int(11) unsigned NOT NULL DEFAULT '0' COMMENT '关注人'," +
                    "  `touid` int(11) unsigned NOT NULL DEFAULT '0' COMMENT '被关注人'," +
                    "  PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='关注表（全局内容）';");
            text+="关注模块创建完成。";
        }else{
            text+="关注模块已经存在，无需添加。";
        }
        //违规记录模块
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_violation';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("CREATE TABLE `"+prefix+"_violation` (" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT," +
                    "  `uid` int(11) NOT NULL DEFAULT '0' COMMENT '违规者uid'," +
                    "  `type` varchar(255) DEFAULT NULL COMMENT '处理类型（manager管理员操作，system系统自动）'," +
                    "  `text` text COMMENT '具体原因'," +
                    "  `created` int(10) unsigned DEFAULT '0' COMMENT '违规时间'," +
                    "  `handler` int(11) unsigned DEFAULT '0' COMMENT '处理人，0为系统自动，其它为真实用户'," +
                    "  PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='违规记录表';");
            text+="违规记录模块创建完成。";
        }else{
            text+="违规记录模块已经存在，无需添加。";
        }
        try {
            Thread.sleep(500);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        //聊天室模块
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_chat';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("CREATE TABLE `"+prefix+"_chat` (" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT," +
                    "  `chatid` varchar(255) DEFAULT NULL COMMENT '聊天室id（加密值）'," +
                    "  `uid` int(11) DEFAULT '0' COMMENT '创建者'," +
                    "  `toid` int(11) DEFAULT '0' COMMENT '也是创建者（和上一个字段共同判断私聊）'," +
                    "  `created` int(10) unsigned DEFAULT '0' COMMENT '创建时间'," +
                    "  `lastTime` int(10) unsigned DEFAULT '0' COMMENT '最后聊天时间'," +
                    "  `type` int(2) unsigned DEFAULT '0' COMMENT '0是私聊，1是群聊'," +
                    "  PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='聊天室表';");
            text+="聊天室模块创建完成。";
        }else{
            text+="聊天室模块已经存在，无需添加。";
        }
        //查询聊天室模块是否存在name字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_chat' and column_name = 'name';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_chat ADD `name` varchar(400) DEFAULT NULL COMMENT '聊天室名称（群聊）'");
            text+="聊天室模块，字段name添加完成。";
        }else{
            text+="聊天室模块，字段name已经存在，无需添加。";
        }
        //查询聊天室模块是否存在pic字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_chat' and column_name = 'pic';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_chat ADD `pic` varchar(400) DEFAULT NULL COMMENT '图片地址（群聊）'");
            text+="聊天室模块，字段pic添加完成。";
        }else{
            text+="聊天室模块，字段pic已经存在，无需添加。";
        }
        //查询聊天室模块是否存在ban字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_chat' and column_name = 'ban';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_chat ADD `ban` int(11) unsigned DEFAULT '0' COMMENT '屏蔽和全体禁言，存操作人id'");
            text+="聊天室模块，字段pic添加完成。";
        }else{
            text+="聊天室模块，字段pic已经存在，无需添加。";
        }
        //聊天记录模块
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_chat_msg';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("CREATE TABLE `"+prefix+"_chat_msg` (" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT," +
                    "  `uid` int(11) DEFAULT '0' COMMENT '发送人'," +
                    "  `cid` int(11) DEFAULT '0' COMMENT '聊天室'," +
                    "  `text` text CHARACTER SET utf8mb4 COMMENT '消息内容'," +
                    "  `created` int(10) unsigned DEFAULT '0' COMMENT '发送时间'," +
                    "  `type` int(2) unsigned DEFAULT '0' COMMENT '0文字消息，1图片消息，3视频消息，4系统提示'," +
                    "  `url` varchar(400) DEFAULT NULL COMMENT '为链接时的url'," +
                    "  PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='聊天消息';");
            text+="聊天记录模块创建完成。";
        }else{
            text+="聊天记录模块已经存在，无需添加。";
        }
        //动态模块
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_space';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("CREATE TABLE `"+prefix+"_space` (" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT," +
                    "  `uid` int(11) DEFAULT '0' COMMENT '发布者'," +
                    "  `created` int(10) unsigned DEFAULT '0' COMMENT '发布时间'," +
                    "  `modified` int(10) unsigned DEFAULT '0' COMMENT '修改时间'," +
                    "  `text` text CHARACTER SET utf8mb4 COMMENT '内容'," +
                    "  `pic` text COMMENT '图片或视频，自己拆分'," +
                    "  `type` int(2) DEFAULT NULL COMMENT '0普通动态，1转发和发布文章，2转发动态，3动态评论，4视频，5商品'," +
                    "  `likes` int(10) DEFAULT '0' COMMENT '喜欢动态的数量'," +
                    "  `toid` int(10) DEFAULT '0' COMMENT '文章id，动态id等'," +
                    "  PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='个人动态表';");
            text+="动态模块创建完成。";
        }else{
            text+="动态模块已经存在，无需添加。";
        }
        //查询动态模块是否存在status字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_space' and column_name = 'status';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_space ADD `status` int(2) unsigned DEFAULT '1' COMMENT '动态状态，0审核，1发布，2锁定'");
            text+="动态模块，字段status添加完成。";
        }else{
            text+="动态模块，字段status已经存在，无需添加。";
        }
        //安装应用表
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_app';", Integer.class);
        if (i == 0) {
            jdbcTemplate.execute("CREATE TABLE `" + prefix + "_app` (" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT," +
                    "  `key` varchar(255) DEFAULT NULL COMMENT '链接密钥'," +
                    "  `name` varchar(255) CHARACTER SET utf8 DEFAULT NULL COMMENT '应用名称'," +
                    "  `type` varchar(255) CHARACTER SET utf8 DEFAULT 'app' COMMENT '应用类型（web或App）'," +
                    "  `logo` varchar(500) CHARACTER SET utf8 DEFAULT NULL COMMENT 'logo图标地址'," +
                    "  `keywords` varchar(255) CHARACTER SET utf8 DEFAULT NULL COMMENT 'web专属，SEO关键词'," +
                    "  `description` varchar(255) DEFAULT NULL COMMENT '应用简介'," +
                    "  `announcement` varchar(400) DEFAULT NULL COMMENT '弹窗公告（支持html）'," +
                    "  `mail` varchar(400) CHARACTER SET utf8 DEFAULT NULL COMMENT '邮箱地址（用于通知和显示）'," +
                    "  `website` varchar(400) CHARACTER SET utf8 DEFAULT NULL COMMENT '网址（非Api地址）'," +
                    "  `currencyName` varchar(255) CHARACTER SET utf8 DEFAULT NULL COMMENT '货币名称'," +
                    "  `version` varchar(255) CHARACTER SET utf8 DEFAULT 'v1.0.0 beta' COMMENT 'app专属，版本号'," +
                    "  `versionCode` int(11) DEFAULT '10' COMMENT 'app专属，版本码'," +
                    "  `versionIntro` varchar(400) DEFAULT NULL COMMENT '版本简介'," +
                    "  `androidUrl` varchar(400) CHARACTER SET utf8 DEFAULT NULL COMMENT '安卓下载地址'," +
                    "  `iosUrl` varchar(255) CHARACTER SET utf8 DEFAULT NULL COMMENT 'ios下载地址'," +
                    "  `field1` varchar(400) CHARACTER SET utf8 DEFAULT NULL COMMENT '预留字段1'," +
                    "  `field2` varchar(400) CHARACTER SET utf8 DEFAULT NULL COMMENT '预留字段2'," +
                    "  PRIMARY KEY (`id`)\n" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COMMENT='应用表（web应用和APP应用）';");
            text += "应用模块创建完成。";
        }else{
            text+="应用模块已存在，无需安装。";
        }
        //查询应用表是否存在adpid字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '"+prefix+"_app' and column_name = 'adpid';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_app ADD `adpid` varchar(255) CHARACTER SET utf8 DEFAULT NULL COMMENT '广告联盟ID'");
            text+="应用表，字段adpid添加完成。";
        }else{
            text+="应用表，字段adpid已经存在，无需添加。";
        }
        text+=" ------ 执行结束，安装执行完成";

        redisHelp.setRedis(this.dataprefix+"_"+"isInstall","1",60,redisTemplate);
        return text;
    }
    /***
     * 让内容字段变为utf8mb4，以支持emoji标签
     */
    @RequestMapping(value = "/toUtf8mb4")
    @ResponseBody
    public String toUtf8mb4(@RequestParam(value = "webkey", required = false,defaultValue = "") String  webkey) {
        if(!webkey.equals(this.key)){
            return Result.getResultJson(0,"请输入正确的访问KEY。如果忘记，可在服务器/opt/application.properties中查看",null);
        }
        try{
            String isRepeated = redisHelp.getRedis("isTypechoInstall",redisTemplate);
            if(isRepeated==null){
                redisHelp.setRedis("isTypechoInstall","1",15,redisTemplate);
            }else{
                return Result.getResultJson(0,"你的操作太频繁了",null);
            }
            jdbcTemplate.execute("alter table `"+prefix+"_contents`  MODIFY COLUMN `text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
            jdbcTemplate.execute("alter table `"+prefix+"_shop`  MODIFY COLUMN `text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
            jdbcTemplate.execute("alter table `"+prefix+"_shop`  MODIFY COLUMN `value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
            jdbcTemplate.execute("alter table `"+prefix+"_inbox`  MODIFY COLUMN `text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
            jdbcTemplate.execute("alter table `"+prefix+"_comments`  MODIFY COLUMN `text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
            return Result.getResultJson(1,"操作成功",null);
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(1,"操作失败",null);
        }

    }
}
