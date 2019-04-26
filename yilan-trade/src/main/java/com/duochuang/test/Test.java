/***********************************************
 * File Name: Test
 * Author: caoguobin
 * mail: caoguobin@live.com
 * Created Time: 26 04 2019 9:50
 ***********************************************/

package com.duochuang.test;

import java.io.*;

public class Test {
    public static void main(String[] args) throws IOException {
        File file=new File("D:\\sources\\trade-duochuang\\yilan-trade\\src\\main\\resources\\accounts.txt");
        InputStream inputStream=new FileInputStream(file);
        BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(inputStream));
        String a= null;
        while ((a=bufferedReader.readLine())!=null){
            String[] split = a.split("&");
            for (String s : split) {
                System.out.println(s);
            }
        }
    }
}
