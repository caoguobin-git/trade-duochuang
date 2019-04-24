/***********************************************
 * File Name: GetHistoryDataTest
 * Author: caoguobin
 * mail: caoguobin@live.com
 * Created Time: 09 04 2019 15:39
 ***********************************************/

package com.duochuang.controller;

import com.fxcm.external.api.transport.FXCMLoginProperties;
import com.fxcm.external.api.transport.GatewayFactory;
import com.fxcm.external.api.transport.IGateway;
import com.fxcm.external.api.transport.listeners.IGenericMessageListener;
import com.fxcm.external.api.transport.listeners.IStatusMessageListener;
import com.fxcm.external.api.util.MessageGenerator;
import com.fxcm.fix.*;
import com.fxcm.fix.other.UserResponse;
import com.fxcm.fix.pretrade.MarketDataRequest;
import com.fxcm.fix.pretrade.MarketDataSnapshot;
import com.fxcm.fix.pretrade.TradingSessionStatus;
import com.fxcm.fix.trade.OrderSingle;
import com.fxcm.messaging.ISessionStatus;
import com.fxcm.messaging.ITransportable;
import com.google.common.base.Strings;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Scanner;

public class GetHistoryDataTest {
    private static String reqId;
    private static TradingSessionStatus mTradingSessionStatus;
    private static MarketDataSnapshot marketDataSnapshot;

    public static void main(String[] args) {
        IGateway iGateway = GatewayFactory.createGateway();
        iGateway.registerGenericMessageListener(new IGenericMessageListener() {
            @Override
            public void messageArrived(ITransportable iMessage) {
                if (!Strings.isNullOrEmpty(reqId) && reqId.equalsIgnoreCase(iMessage.getRequestID())) {
                    MarketDataSnapshot a = (MarketDataSnapshot) iMessage;
                    System.out.println("a== asdfa456sdf==" + a);
                } else if (iMessage instanceof MarketDataSnapshot) {
                    MarketDataSnapshot marketDataSnapshot1 = (MarketDataSnapshot) iMessage;
//                    try {
//                        System.out.println("符号"+marketDataSnapshot1.getInstrument().getSymbol());
//                    } catch (NotDefinedException e) {
//                        e.printStackTrace();
//                    }
                    try {
                        if ("EUR/USD".equalsIgnoreCase(marketDataSnapshot1.getInstrument().getSymbol())) {
                            marketDataSnapshot = marketDataSnapshot1;
//                            System.out.println(marketDataSnapshot);
                        }
                    } catch (NotDefinedException e) {
                        e.printStackTrace();
                    }
                    {

                    }

                }
                if (iMessage instanceof UserResponse) {
                    UserResponse userResponse = (UserResponse) iMessage;
                    System.out.println(userResponse);
                }
                if (iMessage instanceof TradingSessionStatus) {
                    mTradingSessionStatus = (TradingSessionStatus) iMessage;
                    System.out.println("tradingsessionstatus::::" + mTradingSessionStatus);
                }
            }
        });
        iGateway.registerStatusMessageListener(new IStatusMessageListener() {
            @Override
            public void messageArrived(ISessionStatus iSessionStatus) {

            }
        });

        FXCMLoginProperties properties = new FXCMLoginProperties("701116547", "890128", "Demo", "http://www.fxcorporate.com/Hosts.jsp");
        try {
            iGateway.login(properties);
        } catch (Exception e) {
            e.printStackTrace();
        }
        iGateway.requestAccounts();
        iGateway.requestOpenOrders();
        iGateway.requestOpenPositions();
        iGateway.requestTradingSessionStatus();
        iGateway.requestClosedPositions();

        while (true) {
            String command = new Scanner(System.in).nextLine();
            if (command.equalsIgnoreCase("mk")) {
                MarketDataRequest mdr = getMarketDataRequest();
                try {
                    reqId = iGateway.sendMessage(mdr);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }else if ("order".equalsIgnoreCase(command)){
                OrderSingle orderSingle= MessageGenerator.generateOpenOrder(1.1, "1001195792", 1000, SideFactory.SELL, "EUR/USD", "hello");
                try {
                    iGateway.sendMessage(orderSingle);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }

    }

    private static MarketDataRequest getMarketDataRequest() {
        GregorianCalendar instance = (GregorianCalendar) GregorianCalendar.getInstance();
        instance.roll(Calendar.MONTH, -1);
        instance.roll(Calendar.YEAR, -1);
        UTCDate mStartDate = new UTCDate(instance.getTime());
        UTCTimeOnly mStartTime = new UTCTimeOnly(instance.getTime());
        while (marketDataSnapshot == null) {
            System.out.println(mStartDate);
            System.out.println(mStartTime);
        }

        GregorianCalendar instance1 = (GregorianCalendar) GregorianCalendar.getInstance();
        instance1.roll(Calendar.MONTH, -1);
        UTCDate endDate = new UTCDate(instance1.getTime());
        UTCTimeOnly endTime = new UTCTimeOnly(instance1.getTime());

        MarketDataRequest mdr = new MarketDataRequest();
        mdr.setSubscriptionRequestType(SubscriptionRequestTypeFactory.SNAPSHOT);
        mdr.setResponseFormat(IFixDefs.MSGTYPE_FXCMRESPONSE);
        mdr.setFXCMTimingInterval(FXCMTimingIntervalFactory.MIN30);
        mdr.setMDEntryTypeSet(MarketDataRequest.MDENTRYTYPESET_ALL);
        mdr.setFXCMEndDate(endDate);
        mdr.setFXCMEndTime(endTime);
        mdr.setCandleType(1);
        System.out.println("execute...........................................................");
        mdr.addRelatedSymbol(mTradingSessionStatus.getSecurity("EUR/USD"));
        System.out.println("发送请求成功！");
        return mdr;
    }
}
