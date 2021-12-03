package com.RuleApi.common;

import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public  class RedisHelp {

    //
    public  void setKey(String key, Map<String, Object> map,Integer time,RedisTemplate redisTemplate) {
        redisTemplate.opsForHash().putAll(key, map);
        redisTemplate.expire(key, time, TimeUnit.SECONDS);
        //redisTemplate.opsForValue().set(key, map,time);
    }

    public void setRedis(String key, String value,Integer time ,RedisTemplate redisTemplate) {
        //　　
        //关于TimeUnit下面有部分源码截图
        redisTemplate.opsForValue().set(key,value,time, TimeUnit.SECONDS);

    }
    public String getRedis(String key,RedisTemplate redisTemplate){
        if(redisTemplate.opsForValue().get(key)==null){
            return null;
        }
        return redisTemplate.opsForValue().get(key).toString();
    }

    //获取一个redis的map
    public  Map<Object, Object> getMapValue(String key,RedisTemplate redisTemplate) {
        return  redisTemplate.opsForHash().entries(key);
    }

    public  Object getValue(String key, String hashKey,RedisTemplate redisTemplate) {
        return  redisTemplate.opsForHash().get(key, hashKey);
    }

    public  void deleteData(List<String> keys,RedisTemplate redisTemplate) {
        // 通过key执行批量删除操作时先序列化template
        redisTemplate.setKeySerializer(new JdkSerializationRedisSerializer());
        redisTemplate.delete(keys);
    }
    public  void delete(String key,RedisTemplate redisTemplate) {
        // 通过key执行批量删除操作时先序列化template
        redisTemplate.delete(key);
    }
    //数据列表的操作，优化文章性能
    /**
     * 缓存List数据
     *
     * @param key      缓存的键值
     * @param dataList 待缓存的List数据
     * @return 缓存的对象
     */
    public <T> long setList(final String key, final List<T> dataList,final Integer time,RedisTemplate redisTemplate) {
        Long count = redisTemplate.opsForList().rightPushAll(key, dataList);
        redisTemplate.expire(key, time, TimeUnit.SECONDS);
        return count == null ? 0 : count;
    }

    /**
     * 获得缓存的list对象
     *
     * @param key 缓存的键值
     * @return 缓存键值对应的数据
     */
    public <T> List<T> getList(final String key,RedisTemplate redisTemplate) {
        return redisTemplate.opsForList().range(key, 0, -1);
    }
}
