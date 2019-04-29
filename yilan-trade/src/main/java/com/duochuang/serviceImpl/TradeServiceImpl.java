/***********************************************
 * File Name: TradeServiceImpl
 * Author: caoguobin
 * mail: caoguobin@live.com
 * Created Time: 28 02 2019 13:33
 ***********************************************/

package com.duochuang.serviceImpl;

import com.duochuang.common.TradeThread;
import com.duochuang.entity.FXCMInfoEntity;
import com.duochuang.entity.OpenPositionEntity;
import com.duochuang.entity.OrderEntity;
import com.duochuang.mapper.TradeMapper;
import com.duochuang.service.TradeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fxcm.fix.posttrade.ClosedPositionReport;
import com.fxcm.fix.posttrade.CollateralReport;
import com.fxcm.fix.pretrade.MarketDataSnapshot;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.datatransfer.StringSelection;
import java.util.*;

@SuppressWarnings("Duplicates")
@Service
public class TradeServiceImpl implements TradeService {

    @Autowired
    private TradeMapper tradeMapper;

    private ObjectMapper objectMapper = new ObjectMapper();
    Logger logger = Logger.getLogger(TradeServiceImpl.class);
    List<TradeThread> tradeThreadList = new LinkedList<>();
    List<String> accountList = new LinkedList<>();


    /**
     * 登录福汇账号操作
     * 基本流程：
     * 1.从配置文件中获取交易员账号和跟随者账号信息
     * 2.将交易员账号创建线程进行登录
     * 3.将跟随者账号创建线程进行登录
     * 4.登录完成
     *
     * @return 登录结果
     */
    @Override
    public String loginFXCM() {
//        File file = new File("D:\\sources\\trade-duochuang\\yilan-trade\\src\\main\\resources\\accounts.txt");
//        try {
//            InputStream inputStream = new FileInputStream(file);
//            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
//            String a = null;
//            while ((a = bufferedReader.readLine()) != null) {
//                String[] split = a.split("&");
//                TradeThread tradeThread = new TradeThread(new FXCMInfoEntity(split[0].trim(), split[1].trim(), split[2].trim(), split[3].trim()));
//                tradeThreadList.add(tradeThread);
//                new Thread(tradeThread).start();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        FXCMInfoEntity trader = tradeMapper.findTrader();
        if (!accountList.contains(trader.getFxcmAccount())) {
            TradeThread traderThread = new TradeThread(trader);
            new Thread(traderThread).start();
            tradeThreadList.add(traderThread);
            accountList.add(trader.getFxcmAccount());
        }
        List<FXCMInfoEntity> followerList = tradeMapper.findFollower();
        if (followerList != null && followerList.size() > 0) {
            for (FXCMInfoEntity fxcmInfoEntity : followerList) {
                if (!accountList.contains(fxcmInfoEntity.getFxcmAccount())) {
                    TradeThread followerThread = new TradeThread(fxcmInfoEntity);
                    new Thread(followerThread).start();
                    tradeThreadList.add(followerThread);
                    accountList.add(fxcmInfoEntity.getFxcmAccount());
                }
            }
        }
        return "OK";
    }

    @Override
    public String createMarketOrder(String currency, String tradeSide, String tradeAmount, String tradeStop, String tradeLimit) {
        TradeThread traderThread = tradeThreadList.get(0);
        String secondary = traderThread.getFxcmInfoEntity().getFxcmAccount() + new Date().getTime();
        double cashOutstanding = traderThread.getCollateralReport().getCashOutstanding();
        String trueMarketOrder = traderThread.createTrueMarketOrder(cashOutstanding,currency, tradeSide, tradeAmount, tradeStop, tradeLimit, secondary);


        for (int i = 1; i < tradeThreadList.size(); i++) {
            TradeThread tradeThread = tradeThreadList.get(i);
            tradeThread.createTrueMarketOrder(cashOutstanding,currency, tradeSide, tradeAmount, tradeStop, tradeLimit, secondary);

        }
        return trueMarketOrder;
    }

    @Override
    public String createSLMarketOrder(String fxcmPosId, String type, String price) {

        TradeThread traderThread = tradeThreadList.get(0);
        String secondary = traderThread.createStopLimitMarketOrder(fxcmPosId, type, price, null);

        for (int i = 1; i < tradeThreadList.size(); i++) {
            TradeThread tradeThread = tradeThreadList.get(i);
            tradeThread.createStopLimitMarketOrder(type, price, secondary);
        }
        return secondary;
    }

    @Override
    public String updateSLMarketOrder(String orderId, String type, String price) {
        TradeThread tradeThread = tradeThreadList.get(0);
        String secondary = tradeThread.updateStopLimitMarketOrder(orderId, type, price, null);

        for (int i = 1; i < tradeThreadList.size(); i++) {
            tradeThreadList.get(i).updateStopLimitMarketOrder(type, price, secondary);
        }
        return secondary;
    }

    @Override
    public String deleteSLMarketOrder(String orderId, String type) {
        TradeThread tradeThread = tradeThreadList.get(0);

        String secondary = tradeThread.deleteStopLimitMarketOrder(orderId, type, null);

        for (int i = 1; i < tradeThreadList.size(); i++) {
            tradeThreadList.get(i).deleteStopLimitMarketOrder(type, secondary);
        }
        return secondary;
    }

    @Override
    public String deleteMarketOrder(String fxcmPosID) {
        TradeThread tradeThread = tradeThreadList.get(0);
        String secondary = tradeThread.deleteTrueMarketOrder(fxcmPosID, null);

        for (int i = 0; i < tradeThreadList.size(); i++) {
            tradeThreadList.get(i).deleteTrueMarketOrder(secondary);
        }
        return secondary;
    }

    @Override
    public String deleteAllOpenPositions() {
        if (tradeThreadList.size() == 0) {
            return null;
        }
        TradeThread tradeThread = tradeThreadList.get(0);

        List<String> secondary = tradeThread.deleteAllOpenPositions();

        for (int i = 1; i < tradeThreadList.size(); i++) {
            for (String s : secondary) {
                tradeThreadList.get(i).deleteAllOpenPositions(s);
            }
        }
        return "全部平仓OK";
    }

    @Override
    public String createEntryOrder(String price, String type, String amount, String side, String currency, String stop, String limit) {
        TradeThread tradeThread = tradeThreadList.get(0);

        String secondary = tradeThread.getFxcmInfoEntity().getFxcmAccount() + new Date().getTime();
        String result = tradeThread.createEntryOrder(price, type, amount, side, currency, stop, limit, secondary);

        for (int i = 0; i < tradeThreadList.size(); i++) {
            tradeThreadList.get(i).createEntryOrder(price, type, amount, side, currency, stop, limit, secondary);
        }
        return result;
    }

    @Override
    public String updateEntryOrder(String orderId, String amount, String price) {
        TradeThread tradeThread = tradeThreadList.get(0);

        String secondary = tradeThread.updateEntryOrder(orderId, amount, price, null);

        for (int i = 1; i < tradeThreadList.size(); i++) {
            tradeThreadList.get(i).updateEntryOrder(amount, price, secondary);
        }
        return secondary;
    }

    @Override
    public String deleteEntryOrder(String orderId) {
        TradeThread tradeThread = tradeThreadList.get(0);
        String secondary = tradeThread.deleteEntryOrder(orderId, null);

        for (int i = 1; i < tradeThreadList.size(); i++) {
            tradeThreadList.get(i).deleteEntryOrder(secondary);
        }

        return secondary;
    }

    @Override
    public String deleteAllEntryOrders() {
        TradeThread tradeThread = tradeThreadList.get(0);
        List<String> secondary = tradeThread.deleteAllEntryOrders();

        for (int i = 0; i < tradeThreadList.size(); i++) {
            tradeThreadList.get(i).deleteAllEntryOrders(secondary);
        }

        return "删除全部挂单成功";
    }


    @Override
    public String createSLEntryOrder(String orderId, String price, String type) {
        TradeThread tradeThread = tradeThreadList.get(0);
        String secondary = tradeThread.createStopLimitEntryOrder(orderId, price, type, null);

        for (int i = 1; i < tradeThreadList.size(); i++) {
            tradeThreadList.get(i).createStopLimitEntryOrder(price, type, secondary);
        }
        return secondary;
    }


    @Override
    public String updateSLEntryOrder(String orderId, String type, String price) {
        TradeThread tradeThread = tradeThreadList.get(0);
        String secondary = tradeThread.updateStopLimitEntryOrder(orderId, type, price, null);

        for (int i = 1; i < tradeThreadList.size(); i++) {
            tradeThreadList.get(i).updateStopLimitEntryOrder(type, price, secondary);
        }

        return secondary;
    }


    @Override
    public String deleteSLEntryOrder(String orderId, String type) {
        TradeThread tradeThread = tradeThreadList.get(0);
        String secondary = tradeThread.deleteStopLimitEntryOrder(orderId, type, null);
        for (int i = 1; i < tradeThreadList.size(); i++) {
            tradeThreadList.get(i).deleteStopLimitEntryOrder(type, secondary);
        }

        return secondary;
    }

    @Override
    public String changeFXCMPassword(String password, String newPassword) {
        TradeThread tradeThread = tradeThreadList.get(0);
        String secondary = tradeThread.changePassword(tradeThread.getFxcmInfoEntity().getAccountType(), password, newPassword);
        return secondary;
    }

    @Override
    public Map<String, Map<String, OpenPositionEntity>> getOpenPositions() {
        Map<String, Map<String, OpenPositionEntity>> openPositionMap = new LinkedHashMap<>();
        for (TradeThread tradeThread : tradeThreadList) {
            openPositionMap.put(tradeThread.getFxcmInfoEntity().getFxcmAccount(), tradeThread.getOpenPositionsMap());
        }
        return openPositionMap;
    }

    @Override
    public Map<String, Map<String, OrderEntity>> getOpenOrders() {

       Map<String,Map<String, OrderEntity>> openOrderMap = new LinkedHashMap<>();
        for (TradeThread thread : tradeThreadList) {
            openOrderMap.put(thread.getFxcmInfoEntity().getFxcmAccount(),thread.getOpenOrderMap());

        }
        return openOrderMap;
    }

    @Override
    public Map<String, Map<String, ClosedPositionReport>> getClosedPositions() {

        Map<String,Map<String, ClosedPositionReport>> closedPositions = new LinkedHashMap<>();
        for (TradeThread tradeThread : tradeThreadList) {
            closedPositions.put(tradeThread.getFxcmInfoEntity().getFxcmAccount(), tradeThread.getClosedPositionsMap());
        }
        return closedPositions;
    }

    @Override
    public String getCollateralReport() {
        TradeThread tradeThread = tradeThreadList.get(0);
        if (tradeThread == null) {
            Map map = new HashMap();
            map.put("message", "登录状态异常，请重新登录");
            try {
                return objectMapper.writeValueAsString(map);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        CollateralReport collateralReport = tradeThread.getCollateralReport();
        Map map = new HashMap();
        map.put("collateralReport", collateralReport);
        String result = null;
        try {
            result = objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public String logoutFXCM() {
        for (TradeThread tradeThread : tradeThreadList) {
            tradeThread.logout();
        }
        return "logout ok";
    }

    @Override
    public Map<String, MarketDataSnapshot> getMarketDataSnapshot() {
        if (tradeThreadList.size() == 0) {
            return null;
        }
        TradeThread tradeThread = tradeThreadList.get(0);

        Map<String, MarketDataSnapshot> marketDataSnapshotMap = tradeThread.getMarketDataSnapshotMap();

        return marketDataSnapshotMap;
    }

    @Override
    public String getOrderExecutionReport(String listId) {
        String result = null;

        try {
            if (tradeThreadList.get(0) == null) {
                return null;
            }
            if (tradeThreadList.get(0).getOrderExecutionReport(listId) == null) {
                return null;
            }
            result = objectMapper.writeValueAsString(tradeThreadList.get(0).getOrderExecutionReport(listId));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return result;
    }
}
