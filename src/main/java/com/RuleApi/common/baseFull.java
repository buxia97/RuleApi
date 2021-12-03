package com.RuleApi.common;

//常用数据处理类

import java.util.ArrayList;
import java.util.List;

public class baseFull {

    public Object[] threeClear(Object[] arr){
        List list = new ArrayList();
        for(int i=0;i<arr.length;i++){
            if(!list.contains(arr[i])){
                list.add(arr[i]);
            }
        }
        return list.toArray();
    }
}
