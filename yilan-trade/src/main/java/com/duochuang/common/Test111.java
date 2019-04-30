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
import java.util.Scanner;
import java.util.Set;

public class Test111 {
    public static void main(String[] args) throws IOException {
        double trader=26000.0;
        double follower=50000.0;
        while (true) {
            double x=new Scanner(System.in).nextDouble();
            double a=getBullionAmount(trader, x);
            System.out.println(x+"   "+a);
        }
    }

    private static double getBullionAmount(double trader, double amount) {
        double follower=50000.0;
        return ((int)(follower/trader*amount*100000))/1000*1000;
    }
}
