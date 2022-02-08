package com.RuleApi.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.RuleApi.entity.*;
import com.RuleApi.common.*;
import com.RuleApi.service.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 控制层
 * TypechoShopController
 * @author buxia97
 * @date 2022/01/27
 */
@Controller
@RequestMapping(value = "/typechoShop")
public class TypechoShopController {

    @Autowired
    TypechoShopService service;

    @Autowired
    private TypechoUsersService usersService;

    @Autowired
    private TypechoUserlogService userlogService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${web.prefix}")
    private String dataprefix;

    RedisHelp redisHelp =new RedisHelp();
    ResultAll Result = new ResultAll();
    UserStatus UStatus = new UserStatus();

    /***
     * 商品列表
     */
    @RequestMapping(value = "/shopList")
    @ResponseBody
    public String shopList (@RequestParam(value = "searchParams", required = false) String  searchParams,
                            @RequestParam(value = "page"        , required = false, defaultValue = "1") Integer page,
                            @RequestParam(value = "limit"       , required = false, defaultValue = "15") Integer limit) {
        TypechoShop query = new TypechoShop();
        if (StringUtils.isNotBlank(searchParams)) {
            JSONObject object = JSON.parseObject(searchParams);
            query = object.toJavaObject(TypechoShop.class);
        }

        PageList<TypechoShop> pageList = service.selectPage(query, page, limit);
        List jsonList = new ArrayList();
        List list = pageList.getList();
        for (int i = 0; i < list.size(); i++) {
            Map json = JSONObject.parseObject(JSONObject.toJSONString(list.get(i)), Map.class);
            json.remove("value");
            jsonList.add(json);
        }
        JSONObject response = new JSONObject();
        response.put("code" , 1);
        response.put("msg"  , "");
        response.put("data" , null != jsonList ? jsonList : new JSONArray());
        response.put("count", jsonList.size());
        return response.toString();
    }

    /**
     * 查询商品详情
     */
    @RequestMapping(value = "/shopInfo")
    @ResponseBody
    public String shopInfo(@RequestParam(value = "key", required = false) String  key,@RequestParam(value = "token", required = false) String  token) {
        TypechoShop info =  service.selectByKey(key);
        Map shopinfo = JSONObject.parseObject(JSONObject.toJSONString(info), Map.class);
        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);

        if(uStatus==0){

            shopinfo.remove("value");
        }else{
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer uid  = Integer.parseInt(map.get("uid").toString());
            //如果登陆，判断是否购买过
            TypechoUserlog log = new TypechoUserlog();
            log.setType("buy");
            log.setUid(uid);
            log.setCid(Integer.parseInt(key));
            Integer isBuy = userlogService.total(log);
            //判断自己是不是发布者
            Integer aid = info.getUid();
            if(!uid.equals(aid)&&isBuy < 1){
                shopinfo.remove("value");
            }
        }
        JSONObject JsonMap = JSON.parseObject(JSON.toJSONString(shopinfo),JSONObject.class);
        return JsonMap.toJSONString();

    }

    /***
     * 添加商品
     */
    @RequestMapping(value = "/addShop")
    @ResponseBody
    public String addShop(@RequestParam(value = "params", required = false) String  params,@RequestParam(value = "token", required = false) String  token) {
        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }
        Map jsonToMap =null;
        TypechoShop insert = null;
        if (StringUtils.isNotBlank(params)) {
            jsonToMap =  JSONObject.parseObject(JSON.parseObject(params).toString());
            Integer price = 0;
            if(jsonToMap.get("price")!=null){
                price = Integer.parseInt(jsonToMap.get("price").toString());
                if(price < 0){
                    return Result.getResultJson(0,"请输入正确的参数",null);
                }
            }
            jsonToMap.remove("status");
            jsonToMap.remove("created");
            insert = JSON.parseObject(JSON.toJSONString(jsonToMap), TypechoShop.class);
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer uid  = Integer.parseInt(map.get("uid").toString());
            insert.setUid(uid);
        }

        int rows = service.insert(insert);

        JSONObject response = new JSONObject();
        response.put("code" , rows);
        response.put("msg"  , rows > 0 ? "添加成功" : "添加失败");
        return response.toString();
    }

    /***
     * 修改商品
     */
    @RequestMapping(value = "/editShop")
    @ResponseBody
    public String editShop(@RequestParam(value = "params", required = false) String  params,@RequestParam(value = "token", required = false) String  token) {
        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }
        TypechoShop update = null;
        Map jsonToMap =null;
        if (StringUtils.isNotBlank(params)) {
            jsonToMap =  JSONObject.parseObject(JSON.parseObject(params).toString());
            Integer price = 0;
            if(jsonToMap.get("price")!=null){
                price = Integer.parseInt(jsonToMap.get("price").toString());
                if(price < 0){
                    return Result.getResultJson(0,"请输入正确的参数",null);
                }
            }
            // 查询发布者是不是自己，如果是管理员则跳过
            Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
            Integer uid  = Integer.parseInt(map.get("uid").toString());
            String group = map.get("group").toString();
            if(!group.equals("administrator")){
                Integer sid = Integer.parseInt(jsonToMap.get("id").toString());
                TypechoShop info = service.selectByKey(sid);
                Integer aid = info.getUid();
                if(!aid.equals(uid)){
                    return Result.getResultJson(0,"你无权进行此操作",null);
                }
            }
            jsonToMap.put("status","0");
            jsonToMap.remove("created");
            update = JSON.parseObject(JSON.toJSONString(jsonToMap), TypechoShop.class);
        }

        int rows = service.update(update);

        JSONObject response = new JSONObject();
        response.put("code" , rows);
        response.put("msg"  , rows > 0 ? "修改成功" : "修改失败");
        return response.toString();
    }

    /***
     * 删除商品
     */
    @RequestMapping(value = "/deleteShop")
    @ResponseBody
    public String deleteShop(@RequestParam(value = "key", required = false) String  key,@RequestParam(value = "token", required = false) String  token) {

        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }
        // 查询发布者是不是自己，如果是管理员则跳过
        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid  = Integer.parseInt(map.get("uid").toString());
        String group = map.get("group").toString();
        if(!group.equals("administrator")){
            Integer sid = Integer.parseInt(key);
            TypechoShop info = service.selectByKey(sid);
            Integer aid = info.getUid();
            if(!aid.equals(uid)){
                return Result.getResultJson(0,"你无权进行此操作",null);
            }
        }

        int rows =  service.delete(key);
        JSONObject response = new JSONObject();
        response.put("code" , rows);
        response.put("msg"  , rows > 0 ? "操作成功" : "操作失败");
        return response.toString();
    }
    /***
     * 审核商品
     */
    @RequestMapping(value = "/auditShop")
    @ResponseBody
    public String auditShop(@RequestParam(value = "key", required = false) String  key,@RequestParam(value = "token", required = false) String  token) {

        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }
        // 查询发布者是不是自己，如果是管理员则跳过
        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid  = Integer.parseInt(map.get("uid").toString());
        String group = map.get("group").toString();
        if(!group.equals("administrator")){
            Integer sid = Integer.parseInt(key);
            TypechoShop info = service.selectByKey(sid);
            Integer aid = info.getUid();
            if(!aid.equals(uid)){
                return Result.getResultJson(0,"你无权进行此操作",null);
            }
        }
        TypechoShop shop = new TypechoShop();
        shop.setId(Integer.parseInt(key));
        shop.setStatus(1);
        Integer rows = service.update(shop);

        JSONObject response = new JSONObject();
        response.put("code" , rows);
        response.put("msg"  , rows > 0 ? "操作成功" : "操作失败");
        return response.toString();
    }
    /***
     * 购买商品
     */
    @RequestMapping(value = "/buyShop")
    @ResponseBody
    public String buyShop(@RequestParam(value = "sid", required = false) String  sid,@RequestParam(value = "token", required = false) String  token) {

        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }
        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid  = Integer.parseInt(map.get("uid").toString());
        TypechoShop shopinfo = service.selectByKey(sid);
        Integer aid = shopinfo.getUid();

        if(uid.equals(aid)){
            return Result.getResultJson(0,"你不可以买自己的商品",null);
        }
        TypechoUsers usersinfo =usersService.selectByKey(uid.toString());
        Integer price = shopinfo.getPrice();
        Integer oldAssets =usersinfo.getAssets();
        if(price>oldAssets){
            return Result.getResultJson(0,"积分余额不足",null);
        }

        Integer status = shopinfo.getStatus();
        if(!status.equals(1)){
            return Result.getResultJson(0,"该商品已下架",null);
        }

        Integer Assets = oldAssets - price;
        usersinfo.setAssets(Assets);
        //判断商品类型，如果是实体商品需要设置收货地址
        Integer type = shopinfo.getType();
        String address = usersinfo.getAddress();
        if(type.equals(1)&&address==""){
            return Result.getResultJson(0,"购买实体商品前，需要先设置收货地址",null);
        }


        //生成用户日志，判断是否购买，这里的cid用于商品id
        TypechoUserlog log = new TypechoUserlog();
        log.setType("buy");
        log.setUid(uid);
        log.setCid(Integer.parseInt(sid));
        Integer isBuy = userlogService.total(log);
        if(isBuy > 0){
            return Result.getResultJson(0,"你已经购买过了",null);
        }
        log.setNum(Assets);
        log.setToid(aid);
        Long date = System.currentTimeMillis();
        String userTime = String.valueOf(date).substring(0,10);
        log.setCreated(Integer.parseInt(userTime));
        try {
            userlogService.insert(log);
            //修改用户账户
            usersService.update(usersinfo);
            //修改商品剩余数量
            Integer shopnum = shopinfo.getNum();
            shopnum = shopnum - 1;
            shopinfo.setNum(shopnum);
            service.update(shopinfo);
            JSONObject response = new JSONObject();
            response.put("code" , 1);
            response.put("msg"  , "操作成功");
            return response.toString();
        }catch (Exception e){
            JSONObject response = new JSONObject();
            response.put("code" , 0);
            response.put("msg"  , "操作失败");
            return response.toString();
        }


    }
    /**
     * 文章挂载商品
     * */
    @RequestMapping(value = "/mountShop")
    @ResponseBody
    public String mountShop(@RequestParam(value = "cid", required = false) String  cid,@RequestParam(value = "sid", required = false) String  sid,@RequestParam(value = "token", required = false) String  token) {

        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }
        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid  = Integer.parseInt(map.get("uid").toString());
        //判断商品是不是自己的
        TypechoShop shop = new TypechoShop();
        shop.setUid(uid);
        shop.setId(Integer.parseInt(sid));
        Integer num  = service.total(shop);
        if(num < 1){
            return Result.getResultJson(0,"你无权限添加他人的商品",null);
        }
        shop.setCid(Integer.parseInt(cid));
        int rows =  service.update(shop);
        JSONObject response = new JSONObject();
        response.put("code" , rows);
        response.put("msg"  , rows > 0 ? "操作成功" : "操作失败");
        return response.toString();
    }
    /***
     * 查询商品是否已经购买过
     */
    @RequestMapping(value = "/isBuyShop")
    @ResponseBody
    public String isBuyShop(@RequestParam(value = "sid", required = false) String  sid,@RequestParam(value = "token", required = false) String  token) {

        Integer uStatus = UStatus.getStatus(token,this.dataprefix,redisTemplate);
        if(uStatus==0){
            return Result.getResultJson(0,"用户未登录或Token验证失败",null);
        }
        Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
        Integer uid  = Integer.parseInt(map.get("uid").toString());

        TypechoUserlog log = new TypechoUserlog();
        log.setType("buy");
        log.setUid(uid);
        log.setCid(Integer.parseInt(sid));
        int rows =  userlogService.total(log);
        JSONObject response = new JSONObject();
        response.put("code" , rows > 0 ? 1 : 0);
        response.put("msg"  , rows > 0 ? "已购买" : "未购买");
        return response.toString();
    }
}
