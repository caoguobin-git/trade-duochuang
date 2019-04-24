package com.duochuang.service;

/***********************************************
 * File Name: TradeService
 * Author: caoguobin
 * mail: caoguobin@live.com
 * Created Time: 28 02 2019 13:33
 ***********************************************/



public interface TradeService {

    String loginFXCM(String userId, String fxcmAccount, String fxcmPassword);

    String createMarketOrder(String userId, String fxcmAccount, String currency, String tradeSide, String tradeAmount, String tradeStop, String tradeLimit);

    String createSLMarketOrder(String userId, String fxcmAccount, String fxcmPosId, String type, String price);

    String updateSLMarketOrder(String userId, String fxcmAccount, String orderId, String type, String price);

    String deleteSLMarketOrder(String userId, String fxcmAccount, String orderId, String type);

    String deleteMarketOrder(String userId, String fxcmAccount, String fxcmPosID);

    String deleteAllOpenPositions(String userId, String fxcmAccount);

    String createEntryOrder(String userId, String fxcmAccount, String price, String type, String amount, String side, String currency, String stop, String limit);

    String updateEntryOrder(String userId, String fxcmAccount, String orderId, String amount, String price);

    String deleteEntryOrder(String userId, String fxcmAccount, String orderId);

    String deleteAllEntryOrders(String userId, String fxcmAccount);

    String createSLEntryOrder(String userId, String fxcmAccount, String orderId, String price, String type);

    String updateSLEntryOrder(String userId, String fxcmAccount, String orderId, String type, String price);

    String deleteSLEntryOrder(String userId, String fxcmAccount, String orderId, String type);

    String changeFXCMPassword(String userId, String fxcmAccount, String password, String newPassword);

    String logoutFXCM(String userId, String fxcmAccount);

    String getOpenPositions(String userId, String fxcmAccount);

    String getOpenOrders(String userId, String fxcmAccount);

    String getClosedPositions(String userId, String fxcmAccount);

    String getCollateralReport(String userId, String fxcmAccount);

    String getOrderExecutionReport(String userId, String fxcmAccount, String listId);

    String getMarketDataSnapshot(String userId, String fxcmAccount);
}
