/***********************************************
 * File Name: Test111
 * Author: caoguobin
 * mail: caoguobin@live.com
 * Created Time: 29 04 2019 14:01
 ***********************************************/

package com.duochuang.common;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

public class Test111 {
    public static void main(String[] args) throws IOException {
        URL url=new URL("http://www.fxyilan.cn:8080/pretrade/getMarketDataSnapshot.do");
        InputStream inputStream=url.openStream();
        BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(inputStream));
        String a =null;
        String result = null;
        while ((a=bufferedReader.readLine())!=null){
            result=a;
            System.out.println(a);
        }
        ObjectMapper objectMapper=new ObjectMapper();
        LinkedHashMap linkedHashMap = objectMapper.readValue(result, LinkedHashMap.class);
        LinkedHashMap<Object,Object > data = (LinkedHashMap<Object, Object>) linkedHashMap.get("data");
        Set<Object> objects = data.keySet();
        for (Object object : objects) {
            String x = (String) object;
            System.out.println("<option>"+x+"</option>");
        }


    }
}
