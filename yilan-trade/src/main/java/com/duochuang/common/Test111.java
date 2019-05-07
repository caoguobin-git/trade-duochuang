/***********************************************
 * File Name: Test111
 * Author: caoguobin
 * mail: caoguobin@live.com
 * Created Time: 29 04 2019 14:01
 ***********************************************/

package com.duochuang.common;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringEscapeUtils;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;

public class Test111 {
    public static void main(String[] args) throws IOException {
        String a ="[{\"B2\":\"4\",\"B3\":\"95\",\"B4\":\"0\",\"B5\":\"0\",\"B1\":\"F500003F3D\"},{\"B2\":\"5\",\"B3\":\"45\",\"B4\":\"0\",\"B5\":\"0\",\"B1\":\"F600004292\"}]";
        a= StringEscapeUtils.unescapeJava(a);
        ObjectMapper objectMapper=new ObjectMapper();
        LinkedList linkedList = objectMapper.readValue(a, LinkedList.class);
        linkedList.forEach(x->{
            LinkedHashMap o= (LinkedHashMap) x;
            System.out.println(o);
            o.forEach((k,v)-> System.out.println(k+"  "+v));
        });
    }
}
