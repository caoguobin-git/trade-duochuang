package com.duochuang.service;

import com.duochuang.entity.OpenPositionEntity;
import com.fxcm.fix.pretrade.MarketDataSnapshot;

import java.util.Map;

/***********************************************
 * File Name: TradeService
 * Author: caoguobin
 * mail: caoguobin@live.com
 * Created Time: 28 02 2019 13:33
 ***********************************************/



public interface TradeService {

    String loginFXCM();

    String createMarketOrder( String currency, String tradeSide, String tradeAmount, String tradeStop, String tradeLimit);

    String createSLMarketOrder( String fxcmPosId, String type, String price);

    String updateSLMarketOrder(String orderId, String type, String price);

    String deleteSLMarketOrder(String orderId, String type);

    String deleteMarketOrder( String fxcmPosID);

    String deleteAllOpenPositions();

    String createEntryOrder(String price, String type, String amount, String side, String currency, String stop, String limit);

    String updateEntryOrder(String orderId, String amount, String price);

    String deleteEntryOrder(String orderId);

    String deleteAllEntryOrders();

    String createSLEntryOrder( String orderId, String price, String type);

    String updateSLEntryOrder(String orderId, String type, String price);

    String deleteSLEntryOrder( String orderId, String type);

    String changeFXCMPassword(String password, String newPassword);

    String logoutFXCM();

    Map<String, Map<String, OpenPositionEntity>> getOpenPositions();

    String getOpenOrders();

    String getClosedPositions();

    String getCollateralReport();

    String getOrderExecutionReport(String listId);

    Map<String, MarketDataSnapshot> getMarketDataSnapshot();
}
