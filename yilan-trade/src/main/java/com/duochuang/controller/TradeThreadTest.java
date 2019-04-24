/***********************************************
 * File Name: TradeThreadTest
 * Author: caoguobin
 * mail: caoguobin@live.com
 * Created Time: 02 04 2019 9:54
 ***********************************************/

package com.duochuang.controller;

import com.duochuang.common.TradeThread;
import com.duochuang.entity.FXCMInfoEntity;
import com.duochuang.entity.OpenPositionEntity;
import com.duochuang.entity.OrderEntity;
import com.fxcm.fix.posttrade.ClosedPositionReport;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class TradeThreadTest {
    public static void main(String[] args) {
        FXCMInfoEntity fxcmInfoEntity = new FXCMInfoEntity("123", "701116547", "890128", true, 0.3, "Demo", "http://www.fxcorporate.com/Hosts.jsp", false);
        FXCMInfoEntity follower = new FXCMInfoEntity("12", "701159888", "9147", true, 0.3, "Demo", "http://www.fxcorporate.com/Hosts.jsp", false);
        TradeThread tradeThread = new TradeThread(fxcmInfoEntity);
        TradeThread tradeThread1 = new TradeThread(follower);
        new Thread(tradeThread).start();
        new Thread(tradeThread1).start();
        while (true) {
            String command = new Scanner(System.in).nextLine();
            if ("cmo".equalsIgnoreCase(command)) {
                //开仓，设置secondaryId
                String secondary = fxcmInfoEntity.getFxcmAccount() + new Date().getTime();
                String s = tradeThread.createTrueMarketOrder("1000", "sell", "EUR/USD", secondary);
                tradeThread1.createTrueMarketOrder("1000", "sell", "EUR/USD", secondary);
                System.out.println(s);
            } else if ("deletemarketorder".equalsIgnoreCase(command)) {
                System.out.println("input the pos id");
                String posid = new Scanner(System.in).nextLine();

                //平仓 获取secondaryClOrdID，然后进行平仓操作
                String secondaryClOrdID = tradeThread.deleteTrueMarketOrder(posid, null);
                tradeThread1.deleteTrueMarketOrder(secondaryClOrdID);
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            } else if ("closeAll".equalsIgnoreCase(command)) {
                List<String> list = tradeThread.deleteAllOpenPositions();
                //获取secondaryId的list集合
                //进行平仓操作
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                for (String secondary : list) {
                    tradeThread1.deleteAllOpenPositions(secondary);
                }
            } else if ("openlist".equalsIgnoreCase(command)) {
                Map<String, OpenPositionEntity> openPositionsMap = tradeThread.getOpenPositionsMap();
                openPositionsMap.forEach((k, v) -> System.out.println(v));
            } else if ("closelist".equalsIgnoreCase(command)) {
                Map<String, ClosedPositionReport> closedPositionsMap = tradeThread.getClosedPositionsMap();
                closedPositionsMap.forEach((k, v) -> System.out.println(v));
            } else if ("openorders".equalsIgnoreCase(command)) {
                Map<String, OrderEntity> openOrderMap = tradeThread.getOpenOrderMap();
                openOrderMap.forEach((k, v) -> System.out.println("open order:" + v));
            } else if ("entry".equalsIgnoreCase(command)) {
                //创建订单
                String secon = fxcmInfoEntity.getFxcmAccount() + new Date().getTime();
                String entryOrder = tradeThread.createEntryOrder("1285.80", "stop", "1", "sell", "XAU/USD", secon);
                tradeThread1.createEntryOrder("1285.80", "stop", "1", "sell", "XAU/USD", secon);
                System.out.println(entryOrder);
            } else if ("status".equalsIgnoreCase(command)) {
                String massrequestid = new Scanner(System.in).nextLine();
                String status = tradeThread.getStatus(massrequestid);
                System.out.println(status);
            } else if ("deleteEntry".equalsIgnoreCase(command)) {

                String orderId = new Scanner(System.in).nextLine();
                String hehhe = tradeThread.deleteEntryOrder(orderId, "852585");
                tradeThread1.deleteEntryOrder(hehhe);

                System.out.println(hehhe);
            } else if ("deleteAllentry".equalsIgnoreCase(command)) {
                //获取所有secondaryid 然后遍历删除
                List<String> hahahah = tradeThread.deleteAllEntryOrders();
                tradeThread1.deleteAllEntryOrders(hahahah);
            } else if ("updateOrder".equalsIgnoreCase(command)) {

                String orderid = new Scanner(System.in).nextLine();
                String amount = new Scanner(System.in).nextLine();
                String price = new Scanner(System.in).nextLine();
                String secondary = tradeThread.updateEntryOrder(orderid, price, null);
                System.out.println(secondary);
                String s = tradeThread1.updateEntryOrder(amount, price, secondary);
                System.out.println(s);
            } else if ("op".equalsIgnoreCase(command)) {
                String openOrders = tradeThread.getOpenOrders();
                System.out.println(openOrders);
            } else if ("createslentry".equalsIgnoreCase(command)) {
                Scanner scanner = new Scanner(System.in);
                String orderId = scanner.nextLine();
                String price = scanner.nextLine();
                String type = scanner.nextLine();
                String secondary = tradeThread.createStopLimitEntryOrder(orderId, price, type, "hello");
                tradeThread1.createStopLimitEntryOrder(price, type, secondary);
            } else if ("updateslentry".equalsIgnoreCase(command)) {
                Scanner scanner = new Scanner(System.in);
                String orderId = scanner.nextLine();
                String type = scanner.nextLine();
                String price = scanner.nextLine();
                String secondary = tradeThread.updateStopLimitEntryOrder(orderId, type, price, null);
                System.out.println(secondary);
                tradeThread1.updateStopLimitEntryOrder(type, price, secondary);

            } else if ("deleteslentry".equalsIgnoreCase(command)) {
                Scanner scanner = new Scanner(System.in);
                String orderId = scanner.nextLine();
                String type = scanner.nextLine();
                String secondary = tradeThread.deleteStopLimitEntryOrder(orderId, type, null);
                tradeThread1.deleteStopLimitEntryOrder(type, secondary);
                System.out.println(secondary);
            } else if ("createslmarket".equalsIgnoreCase(command)) {
                Scanner scanner = new Scanner(System.in);
                String fxcmPosId = scanner.nextLine();
                String type = scanner.nextLine();
                String price = scanner.nextLine();
                String secondary = tradeThread.createStopLimitMarketOrder(fxcmPosId, type, price, null);
                tradeThread1.createStopLimitMarketOrder(type, price, secondary);
            } else if ("updateslmarket".equalsIgnoreCase(command)) {
                Scanner scanner = new Scanner(System.in);
                String orderId = scanner.nextLine();
                String type = scanner.nextLine();
                String price = scanner.nextLine();
                String secondary = tradeThread.updateStopLimitMarketOrder(orderId, type, price, null);
                tradeThread1.updateStopLimitMarketOrder(type, price, secondary);
            } else if ("deleteslmarket".equalsIgnoreCase(command)) {
                Scanner scanner = new Scanner(System.in);
                String orderId = scanner.nextLine();
                String type = scanner.nextLine();
                String secondary = tradeThread.deleteStopLimitMarketOrder(orderId, type, null);
                tradeThread1.deleteStopLimitMarketOrder(type, secondary);
            } else if ("changepassword".equalsIgnoreCase(command)) {
                Scanner scanne = new Scanner(System.in);
                String username = scanne.nextLine();
                String password = scanne.nextLine();
                String newpassword = scanne.nextLine();

                tradeThread.changePassword(username, password, newpassword);
            } else if ("cmowhithsl".equalsIgnoreCase(command)) {
                Scanner scanner = new Scanner(System.in);
                String currency = scanner.nextLine();
                String side1 = scanner.nextLine();
                String amount1 = scanner.nextLine();
                String stop1 = scanner.nextLine();
                String limit1 = scanner.nextLine();
                String secondary = String.valueOf(new Date().getTime());
                String trueMarketOrder = tradeThread.createTrueMarketOrder(currency, side1, amount1, stop1, limit1, secondary);
                String trueMarketOrder1 = tradeThread1.createTrueMarketOrder(currency, side1, amount1, stop1, limit1, secondary);

                System.out.println(trueMarketOrder);
            } else if ("slewsl".equalsIgnoreCase(command)) {
                Scanner scanne = new Scanner(System.in);
                String price = scanne.nextLine();
                String type = scanne.nextLine();
                String amount = scanne.nextLine();
                String side = scanne.nextLine();
                System.out.println("git h345ub test");
                String currency = scanne.nextLine();
                String stop = scanne.nextLine();
                String limit = scanne.nextLine();
                String secondary = String.valueOf(new Date().getTime());
                String entryOrder = tradeThread.createEntryOrder(price, type, amount, side, currency, stop, limit, secondary);
                String entryOrder1 = tradeThread1.createEntryOrder(price, type, amount, side, currency, stop, limit, secondary);
                System.out.println(entryOrder);
                System.out.println("git test");
                System.out.println(entryOrder1);
            }
        }
    }
}

