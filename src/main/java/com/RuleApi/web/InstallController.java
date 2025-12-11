package com.RuleApi.web;

import com.RuleApi.annotation.LoginRequired;
import com.RuleApi.common.PHPass;
import com.RuleApi.common.RedisHelp;
import com.RuleApi.common.ResultAll;
import com.RuleApi.entity.TypechoAllconfig;
import com.RuleApi.entity.TypechoApiconfig;
import com.RuleApi.entity.TypechoUsers;
import com.RuleApi.service.AllconfigService;
import com.RuleApi.service.ApiconfigService;
import com.RuleApi.service.UsersService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

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
    private UsersService usersService;

    @Autowired
    private ApiconfigService apiconfigService;

    @Autowired
    private AllconfigService allconfigService;

    @Value("${mybatis.configuration.variables.prefix}")
    private String prefix;

    @Value("${web.prefix}")
    private String dataprefix;

    @Value("${webinfo.key}")
    private String key;

    //当前版本，之后每次更新都要改
    private  Integer version = 205;



    RedisHelp redisHelp =new RedisHelp();
    ResultAll Result = new ResultAll();
    PHPass phpass = new PHPass(8);

    /***
     * 检测环境和应用
     */
    @RequestMapping(value = "/isInstall")
    @ResponseBody
    public String isInstall(){

        try {
            String isInstall = redisHelp.getRedis(this.dataprefix+"_"+"isInstall",redisTemplate);

        }catch (Exception e){
            return Result.getResultJson(100,"Redis连接失败或未安装",null);
        }
        try {
            //先检测初始字段是否安装
            Integer i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_users';", Integer.class);
            if (i.equals(0)){
                return Result.getResultJson(101,"初始表未安装或者数据表前缀不正确。",null);
            }
            //检测RuleApi附加表是否安装
            i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_allconfig';", Integer.class);
            if (i.equals(0)){
                return Result.getResultJson(103,"RuleApi附加表未安装，或Typecho初次接入。",null);
            }
            //检查是否存在旧版本表，如果存在，则提示从1.X升级2.X
            i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_apiconfig';", Integer.class);
            if (!i.equals(0)){
                return Result.getResultJson(104,"当前需从1.X升级到2.X。",null);
            }
            //检查是否为最新版本
            TypechoAllconfig versionConfig = new TypechoAllconfig();
            versionConfig.setField("version");
            List<TypechoAllconfig> data =  allconfigService.selectList(versionConfig);
            if(data.size() < 1){
                return Result.getResultJson(105,"检测到新版本。",null);
            }else {
                Integer curVersion = Integer.parseInt(data.get(0).getValue());
                if(curVersion < this.version){
                    return Result.getResultJson(105,"检测到新版本。",null);
                }
            }

        }catch (Exception e){
            return Result.getResultJson(102,"Mysql数据库连接失败或未安装",null);
        }
        return Result.getResultJson(1,"安装正常",null);
    }
    /***
     * 安装Typecho数据库
     */
    @RequestMapping(value = "/typechoInstall")
    @ResponseBody
    @LoginRequired(purview = "-3")
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
            i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_users';", Integer.class);
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
    @LoginRequired(purview = "-3")
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
            i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_users';", Integer.class);
            if (i == 0){
                return "Typecho未安装或者数据表前缀不正确，请尝试安装typecho或者修改properties配置文件。";
            }else{
                text+="Typecho程序确认安装。";
            }
        }catch (Exception e){
            return "Mysql数据库连接失败或未安装";
        }
        //每次安装和升级都删除配置缓存
        redisHelp.delete(dataprefix+"_"+"config",redisTemplate);
        //修改请求头
        jdbcTemplate.execute("ALTER TABLE "+prefix+"_comments MODIFY agent varchar(520);");
        //查询文章表是否存在views字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_contents' and column_name = 'views';", Integer.class);
        if (i == 0){
            //新增字段
            jdbcTemplate.execute("alter table "+prefix+"_contents ADD views integer(10) DEFAULT 0;");
            text+="内容模块，字段views添加完成。";
        }else{
            text+="内容模块，字段views已经存在，无需添加。";
        }
        //查询文章表是否存在likes字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_contents' and column_name = 'likes';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_contents ADD likes integer(10) DEFAULT 0;");
            text+="内容模块，字段likes添加完成。";
        }else{
            text+="内容模块，字段likes已经存在，无需添加。";
        }
        //查询文章表是否存在isrecommend字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_contents' and column_name = 'isrecommend';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_contents ADD isrecommend integer(2) DEFAULT 0;");
            text+="内容模块，字段isrecommend添加完成。";
        }else{
            text+="内容模块，字段isrecommend已经存在，无需添加。";
        }
        //查询文章表是否存在istop字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_contents' and column_name = 'istop';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_contents ADD istop integer(2) DEFAULT 0;");
            text+="内容模块，字段istop添加完成。";
        }else{
            text+="内容模块，字段istop已经存在，无需添加。";
        }
        //查询文章表是否存在isswiper字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_contents' and column_name = 'isswiper';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_contents ADD isswiper integer(2) DEFAULT 0;");
            text+="内容模块，字段isswiper添加完成。";
        }else{
            text+="内容模块，字段isswiper已经存在，无需添加。";
        }
        //查询文章表是否存在replyTime字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_contents' and column_name = 'replyTime';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_contents ADD replyTime integer(10) DEFAULT 0;");
            text+="内容模块，字段replyTime添加完成。";
        }else{
            text+="内容模块，字段replyTime已经存在，无需添加。";
        }
        //查询文章评论表是否存在likes字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_comments' and column_name = 'likes';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_comments ADD likes integer(11) DEFAULT 0;");
            text+="文章评论表，字段likes添加完成。";
        }else{
            text+="文章评论表，字段likes已经存在，无需添加。";
        }
        //查询用户表是否存在introduce字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_users' and column_name = 'introduce';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_users ADD introduce varchar(255);");
            text+="用户模块，字段introduce添加完成。";
        }else{
            text+="用户模块，字段introduce已经存在，无需添加。";
        }
        //查询用户表是否存在account字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_users' and column_name = 'assets';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_users ADD assets integer(11) DEFAULT 0;");
            text+="用户模块，字段assets添加完成。";
        }else{
            text+="用户模块，字段assets已经存在，无需添加。";
        }
        //查询用户表是否存在address字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_users' and column_name = 'address';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_users ADD address text;");
            text+="用户模块，字段address添加完成。";
        }else{
            text+="用户模块，字段address已经存在，无需添加。";
        }
        //查询用户表是否存在address字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_users' and column_name = 'pay';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_users ADD pay text;");
            text+="用户模块，字段pay添加完成。";
        }else{
            text+="用户模块，字段pay已经存在，无需添加。";
        }
        //查询用户表是否存在customize字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_users' and column_name = 'customize';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_users ADD customize varchar(255) DEFAULT NULL;");
            text+="用户模块，字段customize添加完成。";
        }else{
            text+="用户模块，字段customize已经存在，无需添加。";
        }
        //查询用户表是否存在vip字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_users' and column_name = 'vip';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_users ADD vip integer(10) DEFAULT 0;");
            text+="用户模块，字段vip添加完成。";
        }else{
            text+="用户模块，字段vip已经存在，无需添加。";
        }
        //查询用户表是否存在experience字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_users' and column_name = 'experience';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_users ADD experience integer(11) DEFAULT 0;");
            text+="用户模块，字段experience添加完成。";
        }else{
            text+="用户模块，字段experience已经存在，无需添加。";
        }

        //查询用户表是否存在avatar字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_users' and column_name = 'avatar';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_users ADD avatar text;");
            text+="用户模块，字段avatar添加完成。";
        }else{
            text+="用户模块，字段avatar已经存在，无需添加。";
        }
        //查询用户表是否存在clientId字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_users' and column_name = 'clientId';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_users ADD clientId varchar(255) DEFAULT NULL;");
            text+="用户模块，字段clientId添加完成。";
        }else{
            text+="用户模块，字段clientId已经存在，无需添加。";
        }
        //查询用户表是否存在bantime字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_users' and column_name = 'bantime';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_users ADD bantime integer(10) DEFAULT 0;");
            text+="用户模块，字段bantime添加完成。";
        }else{
            text+="用户模块，字段bantime已经存在，无需添加。";
        }
        //查询用户表是否存在posttime字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_users' and column_name = 'posttime';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_users ADD posttime integer(10) DEFAULT 0;");
            text+="用户模块，字段posttime添加完成。";
        }else{
            text+="用户模块，字段posttime已经存在，无需添加。";
        }
        //查询用户表是否存在ip字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_users' and column_name = 'ip';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_users ADD ip varchar(255) DEFAULT '';");
            text+="用户模块，字段ip添加完成。";
        }else{
            text+="用户模块，字段ip已经存在，无需添加。";
        }
        //查询用户表是否存在local字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_users' and column_name = 'local';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_users ADD local varchar(255) DEFAULT '';");
            text+="用户模块，字段local添加完成。";
        }else{
            text+="用户模块，字段local已经存在，无需添加。";
        }
        //查询用户表是否存在phone字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_users' and column_name = 'phone';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_users ADD phone varchar(30) DEFAULT '';");
            text+="用户模块，字段phone添加完成。";
        }else{
            text+="用户模块，字段phone已经存在，无需添加。";
        }
        //查询用户表是否存在userBg字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_users' and column_name = 'userBg';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_users ADD userBg varchar(400) DEFAULT ''  COMMENT '用户主页背景图链接';");
            text+="用户模块，字段userBg添加完成。";
        }else{
            text+="用户模块，字段userBg已经存在，无需添加。";
        }
        //查询用户表是否存在invitationCode字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_users' and column_name = 'invitationCode';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_users ADD invitationCode varchar(255) DEFAULT ''  COMMENT '用户邀请码';");
            text+="用户模块，字段invitationCode添加完成。";
        }else{
            text+="用户模块，字段invitationCode已经存在，无需添加。";
        }
        //查询用户表是否存在invitationUser字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_users' and column_name = 'invitationUser';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_users ADD invitationUser integer(11) DEFAULT 0  COMMENT '邀请用户';");
            text+="用户模块，字段invitationUser添加完成。";
        }else{
            text+="用户模块，字段invitationUser已经存在，无需添加。";
        }
        //查询用户表是否存在points字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_users' and column_name = 'points';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_users ADD points integer(11) DEFAULT 0  COMMENT '用户积分';");
            text+="用户模块，字段points添加完成。";
        }else{
            text+="用户模块，字段points已经存在，无需添加。";
        }

        //查询用户表是否存在honor字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_users' and column_name = 'honor';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_users ADD honor integer(11) DEFAULT 0  COMMENT '荣誉头衔';");
            text+="用户模块，字段honor添加完成。";
        }else{
            text+="用户模块，字段honor已经存在，无需添加。";
        }
        //查询用户表是否存在gender字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_users' and column_name = 'gender';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_users ADD gender integer(2) DEFAULT 0  COMMENT '用户性别';");
            text+="用户模块，字段gender添加完成。";
        }else{
            text+="用户模块，字段gender已经存在，无需添加。";
        }
        //查询用户表是否存在region字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_users' and column_name = 'region';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_users ADD region varchar(255) DEFAULT ''  COMMENT '用户所在地区';");
            text+="用户模块，字段region添加完成。";
        }else{
            text+="用户模块，字段region已经存在，无需添加。";
        }
        //查询用户表是否存在birthday字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_users' and column_name = 'birthday';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_users ADD birthday integer(10) DEFAULT 0  COMMENT '生日';");
            text+="用户模块，字段birthday添加完成。";
        }else{
            text+="用户模块，字段birthday已经存在，无需添加。";
        }


            //查询分类标签表是否存在imgurl字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_metas' and column_name = 'imgurl';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_metas ADD imgurl varchar(500) DEFAULT NULL;");
            text+="标签分类模块，字段imgurl添加完成。";
        }else{
            text+="标签分类模块，字段imgurl已经存在，无需添加。";
        }
        //查询分类标签表是否存在isrecommend字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_metas' and column_name = 'isrecommend';", Integer.class);
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
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_userlog';", Integer.class);
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
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_userlog' and column_name = 'toid';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_userlog ADD toid integer(11) DEFAULT 0;");
            text+="用户操作模块，字段toid添加完成。";
        }else{
            text+="用户操作模块，字段toid已经存在，无需添加。";
        }
        //判断用户社会化API表是否存在
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_userapi';", Integer.class);
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
        //强制修改两个字段为text
        jdbcTemplate.execute("alter table "+prefix+"_userapi MODIFY openId TEXT NULL;");
        jdbcTemplate.execute("alter table "+prefix+"_userapi MODIFY access_token TEXT NULL;");
        try {
            Thread.sleep(500);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }

        //积分商品&积分阅读模块
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_shop';", Integer.class);
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
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_shop' and column_name = 'vipDiscount';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_shop ADD vipDiscount varchar(255) NOT NULL DEFAULT '0.1' COMMENT 'VIP折扣，权高于系统设置折扣';");
            text+="积分商城模块，字段vipDiscount添加完成。";
        }else{
            text+="积分商城模块，字段vipDiscount已经存在，无需添加。";
        }
        //查询商品表是否存在created字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_shop' and column_name = 'created';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_shop ADD created integer(10) DEFAULT 0;");
            text+="积分商城模块，字段created添加完成。";
        }else{
            text+="积分商城模块，字段created已经存在，无需添加。";
        }
        //查询商品表是否存在status字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_shop' and column_name = 'status';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_shop ADD status integer(10) DEFAULT 0;");
            text+="积分商城模块，字段status添加完成。";
        }else{
            text+="积分商城模块，字段status已经存在，无需添加。";
        }
        //查询商品表是否存在sellNum字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_shop' and column_name = 'sellNum';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_shop ADD sellNum integer(11) DEFAULT 0;");
            text+="积分商城模块，字段sellNum添加完成。";
        }else{
            text+="积分商城模块，字段sellNum已经存在，无需添加。";
        }
        //查询聊天室模块是否存在isMd字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_shop' and column_name = 'isMd';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_shop ADD `isMd` int(2) unsigned DEFAULT '1' COMMENT '是否为Markdown编辑器发布'");
            text+="积分商城模块，字段isMd添加完成。";
        }else{
            text+="积分商城模块，字段isMd已经存在，无需添加。";
        }
        //查询聊天室模块是否存在sort字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_shop' and column_name = 'sort';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_shop ADD `sort` int(11) unsigned DEFAULT '0' COMMENT '商品大类'");
            text+="积分商城模块，字段sort添加完成。";
        }else{
            text+="积分商城模块，字段sort已经存在，无需添加。";
        }
        //查询聊天室模块是否存在subtype字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_shop' and column_name = 'subtype';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_shop ADD `subtype` int(11) unsigned DEFAULT '0' COMMENT '子类型'");
            text+="积分商城模块，字段subtype添加完成。";
        }else{
            text+="积分商城模块，字段subtype已经存在，无需添加。";
        }
        //查询聊天室模块是否存在isView字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_shop' and column_name = 'isView';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_shop ADD `isView` int(2) unsigned DEFAULT '1' COMMENT '是否可见'");
            text+="积分商城模块，字段isView添加完成。";
        }else{
            text+="积分商城模块，字段isView已经存在，无需添加。";
        }
        //查询商品表是否存在integral字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_shop' and column_name = 'integral';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_shop ADD integral integer(11) DEFAULT 0 COMMENT '商品所需积分';");
            text+="积分商城模块，字段integral添加完成。";
        }else{
            text+="积分商城模块，字段integral已经存在，无需添加。";
        }
        //判断充值记录表是否存在
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_paylog';", Integer.class);
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

        //资产日志模块是否存在packageId字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_paylog' and column_name = 'packageId';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_paylog ADD `packageId` int(2) unsigned DEFAULT '0' COMMENT '套餐ID'");
            text+="资产日志模块，字段packageId添加完成。";
        }else{
            text+="资产日志模块，字段packageId已经存在，无需添加。";
        }

        //添加卡密充值模块
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_paykey';", Integer.class);
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
        //添加API配置中心模块(新版)
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_allconfig';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("CREATE TABLE `"+prefix+"_allconfig` (" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT," +
                    "  `name` varchar(255) DEFAULT NULL COMMENT '字段name'," +
                    "  `type` varchar(255) DEFAULT NULL COMMENT '字段类型，number，string，text'," +
                    "  `value` text COMMENT '字段值'," +
                    "  `field` varchar(255) DEFAULT NULL COMMENT '字段缩略名'," +
                    "  `isPublic` int(2) DEFAULT '0' COMMENT '是否公开'," +
                    "  `classId` int(11) DEFAULT '0' COMMENT '所属类目ID'," +
                    "  `modules` int(11) DEFAULT '0' COMMENT '所属模块ID'," +
                    "  `intro` varchar(300) DEFAULT NULL COMMENT '字段简介'," +
                    "  `grade` int(11) DEFAULT '0' COMMENT '排序权重'," +
                    "  PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8;");
            text+="配置模块创建完成。";
            String configSql = "INSERT INTO `"+prefix+"_allconfig` (`id`,`name`,`type`,`value`,`field`,`isPublic`,`classId`,`modules`,`intro`,`grade`) VALUES (1,NULL,'String','','qiniuDomain',0,0,0,NULL,0),(2,NULL,'String','','ftpUsername',0,0,0,NULL,0),(3,NULL,'Integer','1','forumAudit',0,0,0,NULL,0),(4,NULL,'Integer','10','spaceMinExp',0,0,0,NULL,0),(5,NULL,'String','','codeSignName',0,0,0,NULL,0),(6,NULL,'Integer','5','postMax',0,0,0,NULL,0),(7,NULL,'Integer','1','adsVideoType',0,0,0,NULL,0),(8,NULL,'String','','wxpayAppId',0,0,0,NULL,0),(9,NULL,'Integer','3600','interceptTime',0,0,0,NULL,0),(10,NULL,'String','','localPath',0,0,0,NULL,0),(11,NULL,'Integer','0','spaceAudit',0,0,0,NULL,0),(12,NULL,'Integer','1','auditlevel',0,0,0,NULL,0),(13,NULL,'Integer','1','id',0,0,0,NULL,0),(14,NULL,'String','','identifyiIdcardAppcode',0,0,0,NULL,0),(15,NULL,'String','','qiniuSecretKey',0,0,0,NULL,0),(16,NULL,'Integer','2','contentAuditlevel',0,0,0,NULL,0),(17,NULL,'String','','s3accessKeyId',0,0,0,NULL,0),(18,NULL,'Integer','0','identifyPost',0,0,0,NULL,0),(19,NULL,'String','','codeAccessKeySecret',0,0,0,NULL,0),(20,NULL,'String','','codeTemplate',0,0,0,NULL,0),(21,NULL,'Integer','1','isPhone',0,0,0,NULL,0),(22,NULL,'String','','cmsSecretKey',0,0,0,NULL,0),(23,NULL,'String','','wxAppId',0,0,0,NULL,0),(24,NULL,'Integer','600','silenceTime',0,0,0,NULL,0),(25,NULL,'String','','smsbaoApikey',0,0,0,NULL,0),(26,NULL,'String','/idcard','identifyiIdcardPath',0,0,0,NULL,0),(27,NULL,'Integer','1','isEmail',0,0,0,NULL,0),(28,NULL,'Integer','50','violationExp',0,0,0,NULL,0),(29,NULL,'String','','s3bucketName',0,0,0,NULL,0),(30,NULL,'String','','smsbaoUsername',0,0,0,NULL,0),(31,NULL,'Integer','0','verifyLevel',0,0,0,NULL,0),(32,NULL,'String','https://www.ruletree.club/','webinfoUrl',0,0,0,NULL,0),(33,NULL,'String','','aliyunAucketName',0,0,0,NULL,0),(34,NULL,'String','','alipayAppId',0,0,0,NULL,0),(35,NULL,'Integer','100','bannerAdsPrice',0,0,0,NULL,0),(36,NULL,'Integer','0','isInvite',0,0,0,NULL,0),(37,NULL,'Integer','100','pushAdsPrice',0,0,0,NULL,0),(38,NULL,'Integer','5','adsGiftAward',0,0,0,NULL,0),(39,NULL,'Integer','0','isLogin',0,0,0,NULL,0),(40,NULL,'String','http://127.0.0.1:8081/','webinfoUploadUrl',0,0,0,NULL,0),(41,NULL,'Integer','100','startAdsPrice',0,0,0,NULL,0),(42,NULL,'Integer','0','disableCode',0,0,0,NULL,0),(43,NULL,'String','cos','uploadType',0,0,0,NULL,0),(44,NULL,'String','','s3region',0,0,0,NULL,0),(45,NULL,'String','','aliyunUrlPrefix',0,0,0,NULL,0),(46,NULL,'String','','alipayNotifyUrl',0,0,0,NULL,0),(47,NULL,'Integer','300','vipDay',0,0,0,NULL,0),(48,NULL,'Integer','10','rebateNum',0,0,0,NULL,0),(49,NULL,'Integer','5','clockExp',0,0,0,NULL,0),(50,NULL,'Integer','0','uploadLevel',0,0,0,NULL,0),(51,NULL,'String','','aliyunAccessKeyId',0,0,0,NULL,0),(52,NULL,'String','','s3secretAccessKey',0,0,0,NULL,0),(53,NULL,'String','https://qy4ys.market.alicloudapi.com','identifyiCompanyHost',0,0,0,NULL,0),(54,NULL,'Integer','5','uploadPicMax',0,0,0,NULL,0),(55,NULL,'Integer','50','uploadMediaMax',0,0,0,NULL,0),(56,NULL,'String','api','cosPrefix',0,0,0,NULL,0),(57,NULL,'String','','ftpBasePath',0,0,0,NULL,0),(58,NULL,'String','','codeEndpoint',0,0,0,NULL,0),(59,NULL,'Integer','0','banRobots',0,0,0,NULL,0),(60,NULL,'String','ap-guangzhou','cosBucket',0,0,0,NULL,0),(61,NULL,'Integer','1','startAdsNum',0,0,0,NULL,0),(62,NULL,'String','','wxpayNotifyUrl',0,0,0,NULL,0),(63,NULL,'String','','adsSecuritykey',0,0,0,NULL,0),(64,NULL,'Integer','5','bannerAdsNum',0,0,0,NULL,0),(65,NULL,'String','','codeAccessKeyId',0,0,0,NULL,0),(66,NULL,'Integer','1','allowDelete',0,0,0,NULL,0),(67,NULL,'String','','cosPath',0,0,0,NULL,0),(68,NULL,'String','','ftpPassword',0,0,0,NULL,0),(69,NULL,'String','https://idcert.market.alicloudapi.com','identifyiIdcardHost',0,0,0,NULL,0),(70,NULL,'Integer','0','clockPoints',0,0,0,NULL,0),(71,NULL,'String','','cosBucketName',0,0,0,NULL,0),(72,NULL,'Integer','5','clock',0,0,0,NULL,0),(73,NULL,'String','','appletsAppid',0,0,0,NULL,0),(74,NULL,'String','','cosAccessKey',0,0,0,NULL,0),(75,NULL,'String','','aliyunAccessKeySecret',0,0,0,NULL,0),(76,NULL,'String','','wxpayMchId',0,0,0,NULL,0),(77,NULL,'String','able','fields',0,0,0,NULL,0),(78,NULL,'Integer','0','rebateLevel',0,0,0,NULL,0),(79,NULL,'Integer','1','isPush',0,0,0,NULL,0),(80,NULL,'String','规则之树','webinfoTitle',0,0,0,NULL,0),(81,NULL,'String','','pexelsKey',0,0,0,NULL,0),(82,NULL,'Integer','1','reviewExp',0,0,0,NULL,0),(83,NULL,'Integer','10','pushAdsNum',0,0,0,NULL,0),(84,NULL,'Integer','100','scale',0,0,0,NULL,0),(85,NULL,'String','','identifyiCompanyAppcode',0,0,0,NULL,0),(86,NULL,'String','','qiniuAccessKey',0,0,0,NULL,0),(87,NULL,'String','https://cdn.helingqi.com/wavatar/','webinfoAvatar',0,0,0,NULL,0),(88,NULL,'String','','ftpHost',0,0,0,NULL,0),(89,NULL,'String','','qiniuBucketName',0,0,0,NULL,0),(90,NULL,'String','','s3endpoint',0,0,0,NULL,0),(91,NULL,'Integer','0','identifyiLv',0,0,0,NULL,0),(92,NULL,'Integer','200','vipPrice',0,0,0,NULL,0),(93,NULL,'Integer','0','smsType',0,0,0,NULL,0),(94,NULL,'Integer','5','chatMinExp',0,0,0,NULL,0),(95,NULL,'Integer','1','forumReplyAudit',0,0,0,NULL,0),(96,NULL,'String','0.1','vipDiscount',0,0,0,NULL,0),(97,NULL,'String','','banIP',0,0,0,NULL,0),(98,NULL,'String','','cmsSecretId',0,0,0,NULL,0),(99,NULL,'Integer','20','uploadFilesMax',0,0,0,NULL,0),(100,NULL,'Integer','5','rebateProportion',0,0,0,NULL,0),(101,NULL,'String','','aliyunFilePrefix',0,0,0,NULL,0),(102,NULL,'String','','wxAppSecret',0,0,0,NULL,0),(103,NULL,'String','','smsbaoTemplate',0,0,0,NULL,0),(104,NULL,'Integer','20','deleteExp',0,0,0,NULL,0),(105,NULL,'String','ap-shanghai','cmsRegion',0,0,0,NULL,0),(106,NULL,'String','/qysys/dmp/api/jinrun.company.company.elements4','identifyiCompanyPath',0,0,0,NULL,0),(107,NULL,'Integer','10','postExp',0,0,0,NULL,0),(108,NULL,'Integer','10','adsGiftNum',0,0,0,NULL,0),(109,NULL,'String','','aliyunEndpoint',0,0,0,NULL,0),(110,NULL,'Integer','0','cmsSwitch',0,0,0,NULL,0),(111,NULL,'Integer','21','ftpPort',0,0,0,NULL,0),(112,NULL,'String','','cosSecretKey',0,0,0,NULL,0);";
            jdbcTemplate.update(configSql);
            text+="配置模块数据载入完成。";
        }else{
            text+="配置模块及数据已经存在，无需添加。";
        }
        //载入或更新数据库里的版本号
        TypechoAllconfig versionConfig = new TypechoAllconfig();
        versionConfig.setField("version");
        List<TypechoAllconfig> data =  allconfigService.selectList(versionConfig);
        if(data.size() < 1){
            versionConfig.setType("Integer");
            versionConfig.setValue(this.version.toString());
            allconfigService.insert(versionConfig);
            text+="版本号已经载入。";
        }else {
            versionConfig.setType("Integer");
            versionConfig.setValue(this.version.toString());
            allconfigService.update(versionConfig);
            text+="版本号已经更新。";
        }

        try {
            Thread.sleep(500);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        //添加邀请码模块
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_invitation';", Integer.class);
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
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_ads';", Integer.class);
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
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_inbox';", Integer.class);
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
        //查询消息通知模块是否存在cid字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_inbox' and column_name = 'cid';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_inbox ADD `cid` int(11) DEFAULT '0' COMMENT '次级消息内容ID'");
            text+="消息通知模块，字段cid添加完成。";
        }else{
            text+="消息通知模块，字段cid已经存在，无需添加。";
        }

        //关注模块
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_fan';", Integer.class);
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
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_violation';", Integer.class);
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
        //查询聊违规记录模块是否存在value字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_violation' and column_name = 'value';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_violation ADD `value` int(11) NOT NULL DEFAULT '0' COMMENT '预留字段，用于指定范围禁言'");
            text+="违规记录模块，字段value添加完成。";
        }else{
            text+="违规记录模块，字段value已经存在，无需添加。";
        }
        try {
            Thread.sleep(500);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        //聊天室模块
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_chat';", Integer.class);
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
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_chat' and column_name = 'name';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_chat ADD `name` varchar(400) DEFAULT NULL COMMENT '聊天室名称（群聊）'");
            text+="聊天室模块，字段name添加完成。";
        }else{
            text+="聊天室模块，字段name已经存在，无需添加。";
        }
        //查询聊天室模块是否存在pic字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_chat' and column_name = 'pic';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_chat ADD `pic` varchar(400) DEFAULT NULL COMMENT '图片地址（群聊）'");
            text+="聊天室模块，字段pic添加完成。";
        }else{
            text+="聊天室模块，字段pic已经存在，无需添加。";
        }
        //查询聊天室模块是否存在ban字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_chat' and column_name = 'ban';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_chat ADD `ban` int(11) unsigned DEFAULT '0' COMMENT '屏蔽和全体禁言，存操作人id'");
            text+="聊天室模块，字段ban添加完成。";
        }else{
            text+="聊天室模块，字段ban已经存在，无需添加。";
        }
        //查询聊天室模块是否存在myUnRead字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_chat' and column_name = 'myUnRead';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_chat ADD `myUnRead` int(11) unsigned DEFAULT '0' COMMENT '我未读（只对私聊生效）'");
            text+="聊天室模块，字段myUnRead添加完成。";
        }else{
            text+="聊天室模块，字段myUnRead已经存在，无需添加。";
        }
        //查询聊天室模块是否存在otherUnRead字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_chat' and column_name = 'otherUnRead';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_chat ADD `otherUnRead` int(11) unsigned DEFAULT '0' COMMENT '对方未读（只对私聊生效）'");
            text+="聊天室模块，字段otherUnRead添加完成。";
        }else{
            text+="聊天室模块，字段otherUnRead已经存在，无需添加。";
        }
        //聊天记录模块
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_chat_msg';", Integer.class);
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
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_space';", Integer.class);
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
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_space' and column_name = 'status';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_space ADD `status` int(2) unsigned DEFAULT '1' COMMENT '动态状态，0审核，1发布，2锁定'");
            text+="动态模块，字段status添加完成。";
        }else{
            text+="动态模块，字段status已经存在，无需添加。";
        }
        //查询动态模块是否存在onlyMe字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_space' and column_name = 'onlyMe';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_space ADD `onlyMe` int(2) unsigned DEFAULT '0' COMMENT '仅自己可见'");
            text+="动态模块，字段onlyMe添加完成。";
        }else{
            text+="动态模块，字段onlyMe已经存在，无需添加。";
        }
        //安装应用表
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_app';", Integer.class);
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
                    "  PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COMMENT='应用表（web应用和APP应用）';");
            text += "应用表创建完成。";
        }else{
            text+="应用表已存在，无需安装。";
        }
        //查询应用表是否存在adpid字段
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_app' and column_name = 'adpid';", Integer.class);
        if (i == 0){
            jdbcTemplate.execute("alter table "+prefix+"_app ADD `adpid` varchar(255) CHARACTER SET utf8 DEFAULT NULL COMMENT '广告联盟ID'");
            text+="应用表，字段adpid添加完成。";
        }else{
            text+="应用表，字段adpid已经存在，无需添加。";
        }
        //安装商品分类表
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_shoptype';", Integer.class);
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
        //安装邮件模板表
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_emailtemplate';", Integer.class);
        if (i == 0) {
            jdbcTemplate.execute("CREATE TABLE `" + prefix + "_emailtemplate` (" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT," +
                    "  `verifyTemplate` text COMMENT '验证码模板'," +
                    "  `reviewTemplate` text COMMENT '审核通知模板'," +
                    "  `safetyTemplate` text COMMENT '安全通知模板'," +
                    "  `replyTemplate` text COMMENT '评论&回复通知模板'," +
                    "  `orderTemplate` text COMMENT '订单通知模板'," +
                    "  PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COMMENT='邮件模板';");
            //
            jdbcTemplate.execute("INSERT INTO `" + prefix + "_emailtemplate` " +
                    "(`verifyTemplate`, `reviewTemplate`, `safetyTemplate`, `replyTemplate`, `orderTemplate`) VALUES " +
                    "('尊敬的用户{{userName}}，您的验证码为{{code}}。验证码将在十分钟后失效，请尽快进行验证，并不要透露给他人'," +
                    "'尊敬的用户{{userName}}，您的内容【{{title}}】，{{reviewText}}。'," +
                    "'尊敬的用户{{userName}}，安全通知：{{safetyText}}。'," +
                    "'尊敬的用户{{userName}}，您的内容【{{title}}】有了新的回复：{{replyText}}。'," +
                    "'尊敬的用户{{userName}}，您的商品【{{title}}】有了新的订单。')");
            text += "安装邮件模板表创建完成。";
        }else{
            text+="安装邮件模板表已存在，无需安装。";
        }


        //安装文件表
        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_files';", Integer.class);
        if (i == 0) {
            jdbcTemplate.execute("CREATE TABLE `" + prefix + "_files` (" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT," +
                    "  `name` varchar(255) DEFAULT NULL COMMENT '文件名称'," +
                    "  `md5` text COMMENT 'md5值'," +
                    "  `links` text COMMENT '文件链接'," +
                    "  `source` varchar(255) DEFAULT 'local' COMMENT '文件源'," +
                    "  `created` int(10) DEFAULT '0' COMMENT '创建时间'," +
                    "  `uid` int(11) DEFAULT '0' COMMENT '上传用户'," +
                    "  `type` varchar(255) DEFAULT '' COMMENT '文件类型'," +
                    "  `suffix` varchar(255) DEFAULT NULL COMMENT '文件后缀'," +
                    "  `size` int(11) DEFAULT '0' COMMENT '文件大小'," +
                    "  PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='文件表';");
            text += "文件表创建完成。";
        }else{
            text+="文件表已存在，无需安装。";
        }

        i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_pay_package';", Integer.class);
        if (i == 0) {
            jdbcTemplate.execute("CREATE TABLE `" + prefix + "_pay_package` (" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT," +
                    "  `name` varchar(255) DEFAULT '' COMMENT '套餐名称'," +
                    "  `intro` text COMMENT '套餐描述'," +
                    "  `price` int(11) DEFAULT '0' COMMENT '套餐价格'," +
                    "  `gold` int(11) DEFAULT '0' COMMENT '金币到账数量'," +
                    "  `integral` int(11) DEFAULT '0' COMMENT '积分套餐数量'," +
                    "  `appleProductId` varchar(255) DEFAULT '' COMMENT 'Apple专用，ProductId'," +
                    "  PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COMMENT='支付套餐表';");
            text += "充值套餐表创建完成。";
        }else{
            text+="充值套餐表已存在，无需安装。";
        }
        text+=" ------ 配置增加检测 ------";
        TypechoAllconfig config = new TypechoAllconfig();
        config.setType("String");
        config.setField("s3UrlPrefix");
        if(allconfigService.selectList(config).size()<1){
            allconfigService.insert(config);
        }else{
            text+="配置中心模块，字段s3UrlPrefix已经存在，无需添加。";
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
    @LoginRequired(purview = "-3")
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
            jdbcTemplate.execute("ALTER TABLE `" + prefix + "_contents` MODIFY COLUMN `title` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
            jdbcTemplate.execute("ALTER TABLE `" + prefix + "_contents` MODIFY COLUMN `text` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
            jdbcTemplate.execute("ALTER TABLE `" + prefix + "_shop` MODIFY COLUMN `title` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
            jdbcTemplate.execute("ALTER TABLE `" + prefix + "_shop` MODIFY COLUMN `text` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
            jdbcTemplate.execute("ALTER TABLE `" + prefix + "_shop` MODIFY COLUMN `value` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
            jdbcTemplate.execute("ALTER TABLE `" + prefix + "_inbox` MODIFY COLUMN `text` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
            jdbcTemplate.execute("ALTER TABLE `" + prefix + "_comments` MODIFY COLUMN `text` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
            jdbcTemplate.execute("ALTER TABLE `" + prefix + "_forum` MODIFY COLUMN `title` VARCHAR(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
            return Result.getResultJson(1,"操作成功",null);
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(1,"操作失败",null);
        }

    }

    /***
     * 1.X版本升级，配置中心重构
     */
    @RequestMapping(value = "/configToconfig")
    @ResponseBody
    public String configToconfig() {
        try{
            try {
                String isInstall = redisHelp.getRedis(this.dataprefix+"_"+"configToconfig",redisTemplate);
                if(isInstall!=null){
                    return "请不要频繁操作！";
                }
            }catch (Exception e){
                return "Redis连接失败或未安装";
            }
            Integer i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema = DATABASE() and table_name = '"+prefix+"_apiconfig';", Integer.class);
            if (i.equals(0)){
                return Result.getResultJson(0,"当前无需升级",null);
            }
            TypechoAllconfig allconfig = new TypechoAllconfig();
//            //先添加默认的字段和值
//            String sql = "INSERT INTO `" + prefix + "_allconfig` (`id`,`name`,`type`,`value`,`field`,`isPublic`,`classId`,`modules`,`intro`,`grade`) VALUES (1,NULL,'String','','qiniuDomain',0,0,0,NULL,0),(2,NULL,'String','','ftpUsername',0,0,0,NULL,0),(3,NULL,'Integer','1','forumAudit',0,0,0,NULL,0),(4,NULL,'Integer','10','spaceMinExp',0,0,0,NULL,0),(5,NULL,'String','','codeSignName',0,0,0,NULL,0),(6,NULL,'Integer','5','postMax',0,0,0,NULL,0),(7,NULL,'Integer','1','adsVideoType',0,0,0,NULL,0),(8,NULL,'String','','wxpayAppId',0,0,0,NULL,0),(9,NULL,'Integer','3600','interceptTime',0,0,0,NULL,0),(10,NULL,'String','','localPath',0,0,0,NULL,0),(11,NULL,'Integer','0','spaceAudit',0,0,0,NULL,0),(12,NULL,'Integer','1','auditlevel',0,0,0,NULL,0),(13,NULL,'Integer','1','id',0,0,0,NULL,0),(14,NULL,'String','','identifyiIdcardAppcode',0,0,0,NULL,0),(15,NULL,'String','','qiniuSecretKey',0,0,0,NULL,0),(16,NULL,'Integer','2','contentAuditlevel',0,0,0,NULL,0),(17,NULL,'String','','s3accessKeyId',0,0,0,NULL,0),(18,NULL,'Integer','0','identifyPost',0,0,0,NULL,0),(19,NULL,'String','','codeAccessKeySecret',0,0,0,NULL,0),(20,NULL,'String','','codeTemplate',0,0,0,NULL,0),(21,NULL,'Integer','1','isPhone',0,0,0,NULL,0),(22,NULL,'String','','cmsSecretKey',0,0,0,NULL,0),(23,NULL,'String','','wxAppId',0,0,0,NULL,0),(24,NULL,'Integer','600','silenceTime',0,0,0,NULL,0),(25,NULL,'String','','smsbaoApikey',0,0,0,NULL,0),(26,NULL,'String','/idcard','identifyiIdcardPath',0,0,0,NULL,0),(27,NULL,'Integer','1','isEmail',0,0,0,NULL,0),(28,NULL,'Integer','50','violationExp',0,0,0,NULL,0),(29,NULL,'String','','s3bucketName',0,0,0,NULL,0),(30,NULL,'String','','smsbaoUsername',0,0,0,NULL,0),(31,NULL,'Integer','0','verifyLevel',0,0,0,NULL,0),(32,NULL,'String','https://www.ruletree.club/','webinfoUrl',0,0,0,NULL,0),(33,NULL,'String','','aliyunAucketName',0,0,0,NULL,0),(34,NULL,'String','','alipayAppId',0,0,0,NULL,0),(35,NULL,'Integer','100','bannerAdsPrice',0,0,0,NULL,0),(36,NULL,'Integer','0','isInvite',0,0,0,NULL,0),(37,NULL,'Integer','100','pushAdsPrice',0,0,0,NULL,0),(38,NULL,'Integer','5','adsGiftAward',0,0,0,NULL,0),(39,NULL,'Integer','0','isLogin',0,0,0,NULL,0),(40,NULL,'String','http://127.0.0.1:8081/','webinfoUploadUrl',0,0,0,NULL,0),(41,NULL,'Integer','100','startAdsPrice',0,0,0,NULL,0),(42,NULL,'Integer','0','disableCode',0,0,0,NULL,0),(43,NULL,'String','cos','uploadType',0,0,0,NULL,0),(44,NULL,'String','','s3region',0,0,0,NULL,0),(45,NULL,'String','','aliyunUrlPrefix',0,0,0,NULL,0),(46,NULL,'String','','alipayNotifyUrl',0,0,0,NULL,0),(47,NULL,'Integer','300','vipDay',0,0,0,NULL,0),(48,NULL,'Integer','10','rebateNum',0,0,0,NULL,0),(49,NULL,'Integer','5','clockExp',0,0,0,NULL,0),(50,NULL,'Integer','0','uploadLevel',0,0,0,NULL,0),(51,NULL,'String','','aliyunAccessKeyId',0,0,0,NULL,0),(52,NULL,'String','','s3secretAccessKey',0,0,0,NULL,0),(53,NULL,'String','https://qy4ys.market.alicloudapi.com','identifyiCompanyHost',0,0,0,NULL,0),(54,NULL,'Integer','5','uploadPicMax',0,0,0,NULL,0),(55,NULL,'Integer','50','uploadMediaMax',0,0,0,NULL,0),(56,NULL,'String','api','cosPrefix',0,0,0,NULL,0),(57,NULL,'String','','ftpBasePath',0,0,0,NULL,0),(58,NULL,'String','','codeEndpoint',0,0,0,NULL,0),(59,NULL,'Integer','0','banRobots',0,0,0,NULL,0),(60,NULL,'String','ap-guangzhou','cosBucket',0,0,0,NULL,0),(61,NULL,'Integer','1','startAdsNum',0,0,0,NULL,0),(62,NULL,'String','','wxpayNotifyUrl',0,0,0,NULL,0),(63,NULL,'String','','adsSecuritykey',0,0,0,NULL,0),(64,NULL,'Integer','5','bannerAdsNum',0,0,0,NULL,0),(65,NULL,'String','','codeAccessKeyId',0,0,0,NULL,0),(66,NULL,'Integer','1','allowDelete',0,0,0,NULL,0),(67,NULL,'String','','cosPath',0,0,0,NULL,0),(68,NULL,'String','','ftpPassword',0,0,0,NULL,0),(69,NULL,'String','https://idcert.market.alicloudapi.com','identifyiIdcardHost',0,0,0,NULL,0),(70,NULL,'Integer','0','clockPoints',0,0,0,NULL,0),(71,NULL,'String','','cosBucketName',0,0,0,NULL,0),(72,NULL,'Integer','5','clock',0,0,0,NULL,0),(73,NULL,'String','','appletsAppid',0,0,0,NULL,0),(74,NULL,'String','','cosAccessKey',0,0,0,NULL,0),(75,NULL,'String','','aliyunAccessKeySecret',0,0,0,NULL,0),(76,NULL,'String','','wxpayMchId',0,0,0,NULL,0),(77,NULL,'String','able','fields',0,0,0,NULL,0),(78,NULL,'Integer','0','rebateLevel',0,0,0,NULL,0),(79,NULL,'Integer','1','isPush',0,0,0,NULL,0),(80,NULL,'String','规则之树','webinfoTitle',0,0,0,NULL,0),(81,NULL,'String','','pexelsKey',0,0,0,NULL,0),(82,NULL,'Integer','1','reviewExp',0,0,0,NULL,0),(83,NULL,'Integer','10','pushAdsNum',0,0,0,NULL,0),(84,NULL,'Integer','100','scale',0,0,0,NULL,0),(85,NULL,'String','','identifyiCompanyAppcode',0,0,0,NULL,0),(86,NULL,'String','','qiniuAccessKey',0,0,0,NULL,0),(87,NULL,'String','https://cdn.helingqi.com/wavatar/','webinfoAvatar',0,0,0,NULL,0),(88,NULL,'String','','ftpHost',0,0,0,NULL,0),(89,NULL,'String','','qiniuBucketName',0,0,0,NULL,0),(90,NULL,'String','','s3endpoint',0,0,0,NULL,0),(91,NULL,'Integer','0','identifyiLv',0,0,0,NULL,0),(92,NULL,'Integer','200','vipPrice',0,0,0,NULL,0),(93,NULL,'Integer','0','smsType',0,0,0,NULL,0),(94,NULL,'Integer','5','chatMinExp',0,0,0,NULL,0),(95,NULL,'Integer','1','forumReplyAudit',0,0,0,NULL,0),(96,NULL,'String','0.1','vipDiscount',0,0,0,NULL,0),(97,NULL,'String','','banIP',0,0,0,NULL,0),(98,NULL,'String','','cmsSecretId',0,0,0,NULL,0),(99,NULL,'Integer','20','uploadFilesMax',0,0,0,NULL,0),(100,NULL,'Integer','5','rebateProportion',0,0,0,NULL,0),(101,NULL,'String','','aliyunFilePrefix',0,0,0,NULL,0),(102,NULL,'String','','wxAppSecret',0,0,0,NULL,0),(103,NULL,'String','','smsbaoTemplate',0,0,0,NULL,0),(104,NULL,'Integer','20','deleteExp',0,0,0,NULL,0),(105,NULL,'String','ap-shanghai','cmsRegion',0,0,0,NULL,0),(106,NULL,'String','/qysys/dmp/api/jinrun.company.company.elements4','identifyiCompanyPath',0,0,0,NULL,0),(107,NULL,'Integer','10','postExp',0,0,0,NULL,0),(108,NULL,'Integer','10','adsGiftNum',0,0,0,NULL,0),(109,NULL,'String','','aliyunEndpoint',0,0,0,NULL,0),(110,NULL,'Integer','0','cmsSwitch',0,0,0,NULL,0),(111,NULL,'Integer','21','ftpPort',0,0,0,NULL,0),(112,NULL,'String','','cosSecretKey',0,0,0,NULL,0);";
//            /*先创建全新的配置表*/
//            jdbcTemplate.queryForObject(sql, Integer.class);
            /*再将旧配置更新到配置表*/
            TypechoApiconfig apiconfig = apiconfigService.selectByKey(1);
            Map<String, Object> configJson = JSONObject.parseObject(JSONObject.toJSONString(apiconfig), Map.class);

            for (Map.Entry<String, Object> entry : configJson.entrySet()) {

                Object value = entry.getValue();
                String type = getValueType(value);
                allconfig.setField(entry.getKey());
                //对于存在的，把配置更新下
                if(allconfigService.selectList(allconfig).size()>0){
                    allconfig.setValue(entry.getValue().toString());
                    allconfig.setType(type);
                    allconfigService.update(allconfig);
                }else{
                    //对于不存在的，添加这个配置，其实概率不大
                    allconfig.setValue(entry.getValue().toString());
                    allconfig.setType(type);
                    allconfigService.insert(allconfig);
                }
            }
            //更新或添加完成后，删除数据表
            String sql = "DROP TABLE IF EXISTS `" + prefix + "_apiconfig`";
            jdbcTemplate.execute(sql);
            redisHelp.setRedis(this.dataprefix+"_"+"configToconfig","1",600,redisTemplate);
            return Result.getResultJson(1,"配置升级成功，欢迎使用2.0版本",null);
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0,"配置更新失败，请检查数据库配置，并确认旧版本是否为1.X最新版！",null);
        }

    }
    /***
     * 清理redis所有数据
     */
    @RequestMapping(value = "/cleanRedis")
    @ResponseBody
    @LoginRequired(purview = "-3")
    public String cleanRedis(@RequestParam(value = "webkey", required = false,defaultValue = "") String  webkey){
        try{
            if(!webkey.equals(this.key)){
                return Result.getResultJson(0,"请输入正确的访问KEY。如果忘记，可在服务器/opt/application.properties中查看",null);
            }
            redisHelp.deleteKeysWithPattern("*"+this.dataprefix+"*",redisTemplate,this.dataprefix);
            return Result.getResultJson(1,"操作成功！",null);
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(0,"操作失败，请检查redis配置信息！",null);
        }

    }



    public static String getValueType(Object value) {
        if (value instanceof String) {
            return "String";
        } else if (value instanceof Integer) {
            return "Integer";
        } else if (value instanceof Long) {
            return "Long";
        } else if (value instanceof Double) {
            return "Double";
        } else if (value instanceof Float) {
            return "Float";
        } else if (value instanceof Boolean) {
            return "Boolean";
        }else if (value instanceof Map) {
            return "Map";
        } else if (value == null) {
            return "null";
        } else {
            return value.getClass().getSimpleName(); // 默认返回类名
        }
    }
}
