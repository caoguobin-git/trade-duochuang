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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("Duplicates")
@Service
public class TradeServiceImpl implements TradeService {

    @Autowired
    private TradeMapper tradeMapper;
    private ObjectMapper objectMapper = new ObjectMapper();
    Logger logger = Logger.getLogger(TradeServiceImpl.class);

    private Map<String, TradeThread> tradeThreadMap = new ConcurrentHashMap<>();
    private Map<String, List<FXCMInfoEntity>> userFollowMap = new ConcurrentHashMap<>();
    private Map<String, Integer> loginStatusMap = new ConcurrentHashMap<>();


    @Override
    public String loginFXCM(String userId, String fxcmAccount, String fxcmPassword) {
        /**
         * 判断用户是否已经完成登录：从map中获取数据
         */
        TradeThread thread = tradeThreadMap.get(userId + fxcmAccount);
        if (thread != null) {
            return "OK";
        }
        //判断用户密码是否正确
        FXCMInfoEntity trader = tradeMapper.findByAccountAndPassword(fxcmAccount, fxcmPassword);
        if (trader == null) {
            return "fxcm账号或密码错误";
        }
        System.out.println("trader:::::" + trader.toString());

        List<FXCMInfoEntity> followerAccounts = null;
        //判断是否交易员账号登录，如果交易员账户登录执行跟单操作，否则正常登录
        if (tradeMapper.isTrader(userId, fxcmAccount)) {
            followerAccounts = tradeMapper.findFollowerAccountsByUserId(userId);
            followerAccounts.forEach(a-> System.out.println(a.toString()));
        }

        if (followerAccounts != null) {
            userFollowMap.put(userId, followerAccounts);
        }

        //交易员登录
        TradeThread tradeThread = new TradeThread(trader);
        new Thread(tradeThread).start();

        String userThreadKey = userId + fxcmAccount;
        tradeThreadMap.put(userThreadKey, tradeThread);

        //设置登录状态
        loginStatusMap.merge(userThreadKey, 1, Integer::sum);

        if (followerAccounts != null) {
            for (FXCMInfoEntity fxcmInfoEntity : followerAccounts) {
                String threadKey = fxcmInfoEntity.getUserId() + fxcmInfoEntity.getFxcmAccount();
                TradeThread tradeThread1 = tradeThreadMap.get(threadKey);
                if (tradeThread1 == null) {
                    tradeThread = new TradeThread(fxcmInfoEntity);
                    new Thread(tradeThread).start();
                    tradeThreadMap.put(threadKey, tradeThread);
                    //设置登录状态
                    loginStatusMap.put(threadKey, loginStatusMap.get(threadKey) == null ? 1 : loginStatusMap.get(threadKey) + 1);
                } else {
                    continue;
                }
            }
        }
        return "OK";
    }

    @Override
    public String createMarketOrder(String userId, String fxcmAccount, String currency, String tradeSide, String tradeAmount, String tradeStop, String tradeLimit) {
        String userKey = userId + fxcmAccount;
        TradeThread tradeThread = tradeThreadMap.get(userKey);
        if (tradeThread == null) {
            return "登录状态异常，请重新登录";
        }
        String secondary = userId + new Date().getTime();
        String trueMarketOrder = tradeThread.createTrueMarketOrder(currency, tradeSide, tradeAmount, tradeStop, tradeLimit, secondary);

        //2.获取跟随者数据
        List<FXCMInfoEntity> fxcmInfoEntities = userFollowMap.get(userId);
        if (fxcmInfoEntities != null) {
            fxcmInfoEntities.forEach((entry) -> {
                TradeThread followerThread = tradeThreadMap.get(entry.getUserId() + entry.getFxcmAccount());
                if (followerThread != null) {
                    followerThread.createTrueMarketOrder(currency, tradeSide, tradeAmount, tradeStop, tradeLimit, secondary);
                }
            });
        }
        return trueMarketOrder;
    }


    @Override
    public String createSLMarketOrder(String userId, String fxcmAccount, String fxcmPosId, String type, String price) {
        String userKey = userId + fxcmAccount;
        TradeThread tradeThread = tradeThreadMap.get(userKey);
        if (tradeThread == null) {
            return "登录状态异常，请重新登录";
        }
        String secondary = tradeThread.createStopLimitMarketOrder(fxcmPosId, type, price, null);

        //2.获取跟随者数据
        List<FXCMInfoEntity> fxcmInfoEntities = userFollowMap.get(userId);
        if (fxcmInfoEntities != null) {
            fxcmInfoEntities.forEach((entry) -> {
                TradeThread followerThread = tradeThreadMap.get(entry.getUserId() + entry.getFxcmAccount());
                if (followerThread != null) {
                    followerThread.createStopLimitMarketOrder(type, price, secondary);
                }
            });
        }
        return secondary;
    }


    @Override
    public String updateSLMarketOrder(String userId, String fxcmAccount, String orderId, String type, String price) {
        String userKey = userId + fxcmAccount;
        TradeThread tradeThread = tradeThreadMap.get(userKey);
        if (tradeThread == null) {
            return "登录状态异常，请重新登录";
        }
        String secondary = userId + new Date().getTime();
        String s = tradeThread.updateStopLimitMarketOrder(orderId, type, price, secondary);

        //2.获取跟随者数据
        List<FXCMInfoEntity> fxcmInfoEntities = userFollowMap.get(userId);
        if (fxcmInfoEntities != null) {
            fxcmInfoEntities.forEach((entry) -> {
                TradeThread followerThread = tradeThreadMap.get(entry.getUserId() + entry.getFxcmAccount());
                if (followerThread != null) {
                    followerThread.updateStopLimitMarketOrder(type, price, s);
                }
            });
        }
        return s;
    }


    @Override
    public String deleteSLMarketOrder(String userId, String fxcmAccount, String orderId, String type) {
        String userKey = userId + fxcmAccount;
        TradeThread tradeThread = tradeThreadMap.get(userKey);
        if (tradeThread == null) {
            return "登录状态异常，请重新登录";
        }
        String secondary = tradeThread.deleteStopLimitMarketOrder(orderId, type, null);

        //2.获取跟随者数据
        List<FXCMInfoEntity> fxcmInfoEntities = userFollowMap.get(userId);
        if (fxcmInfoEntities != null) {
            fxcmInfoEntities.forEach((entry) -> {
                TradeThread followerThread = tradeThreadMap.get(entry.getUserId() + entry.getFxcmAccount());
                if (followerThread != null) {
                    followerThread.deleteStopLimitMarketOrder(type, secondary);
                }
            });
        }
        return secondary;
    }

    @Override
    public String deleteMarketOrder(String userId, String fxcmAccount, String fxcmPosID) {
        String userKey = userId + fxcmAccount;
        TradeThread tradeThread = tradeThreadMap.get(userKey);
        if (tradeThread == null) {
            return "登录状态异常，请重新登录";
        }
        String secondary = tradeThread.deleteTrueMarketOrder(fxcmPosID, null);

        //2.获取跟随者数据
        List<FXCMInfoEntity> fxcmInfoEntities = userFollowMap.get(userId);
        if (fxcmInfoEntities != null) {
            fxcmInfoEntities.forEach((entry) -> {
                TradeThread followerThread = tradeThreadMap.get(entry.getUserId() + entry.getFxcmAccount());
                if (followerThread != null) {
                    followerThread.deleteTrueMarketOrder(secondary);
                }
            });
        }
        return secondary;
    }


    @Override
    public String deleteAllOpenPositions(String userId, String fxcmAccount) {
        String userKey = userId + fxcmAccount;
        TradeThread tradeThread = tradeThreadMap.get(userKey);
        if (tradeThread == null) {
            return "登录状态异常，请重新登录";
        }
        List<String> secondary = tradeThread.deleteAllOpenPositions();

        //2.获取跟随者数据
        List<FXCMInfoEntity> fxcmInfoEntities = userFollowMap.get(userId);
        if (fxcmInfoEntities != null) {
            fxcmInfoEntities.forEach((entry) -> {
                TradeThread followerThread = tradeThreadMap.get(entry.getUserId() + entry.getFxcmAccount());
                if (followerThread != null) {
                    secondary.forEach((s) -> followerThread.deleteAllOpenPositions(s));
                }
            });
        }
        return "全部平仓OK";
    }


    @Override
    public String createEntryOrder(String userId, String fxcmAccount, String price, String type, String amount, String side, String currency, String stop, String limit) {
        String userKey = userId + fxcmAccount;
        TradeThread tradeThread = tradeThreadMap.get(userKey);
        if (tradeThread == null) {
            return "登录状态异常，请重新登录";
        }
        String secondary = userId + new Date().getTime();
        String result = tradeThread.createEntryOrder(price, type, amount, side, currency, stop, limit, secondary);

        //2.获取跟随者数据
        List<FXCMInfoEntity> fxcmInfoEntities = userFollowMap.get(userId);
        if (fxcmInfoEntities != null) {
            fxcmInfoEntities.forEach((entry) -> {
                TradeThread followerThread = tradeThreadMap.get(entry.getUserId() + entry.getFxcmAccount());
                if (followerThread != null) {
                    followerThread.createEntryOrder(price, type, amount, side, currency, stop, limit, secondary);
                }
            });
        }
        return result;
    }

    @Override
    public String updateEntryOrder(String userId, String fxcmAccount, String orderId, String amount, String price) {
        String userKey = userId + fxcmAccount;
        TradeThread tradeThread = tradeThreadMap.get(userKey);
        if (tradeThread == null) {
            return "登录状态异常，请重新登录";
        }

        String secondary = tradeThread.updateEntryOrder(orderId, amount, price, null);

        //2.获取跟随者数据
        List<FXCMInfoEntity> fxcmInfoEntities = userFollowMap.get(userId);
        if (fxcmInfoEntities != null) {
            fxcmInfoEntities.forEach((entry) -> {
                TradeThread followerThread = tradeThreadMap.get(entry.getUserId() + entry.getFxcmAccount());
                if (followerThread != null) {
                    followerThread.updateEntryOrder(amount, price, secondary);
                }
            });
        }
        return secondary;
    }


    @Override
    public String deleteEntryOrder(String userId, String fxcmAccount, String orderId) {
        String userKey = userId + fxcmAccount;
        TradeThread tradeThread = tradeThreadMap.get(userKey);
        if (tradeThread == null) {
            return "登录状态异常，请重新登录";
        }

        String secondary = tradeThread.deleteEntryOrder(orderId, null);

        //2.获取跟随者数据
        List<FXCMInfoEntity> fxcmInfoEntities = userFollowMap.get(userId);
        if (fxcmInfoEntities != null) {
            fxcmInfoEntities.forEach((entry) -> {
                TradeThread followerThread = tradeThreadMap.get(entry.getUserId() + entry.getFxcmAccount());
                if (followerThread != null) {
                    followerThread.deleteEntryOrder(secondary);
                }
            });
        }
        return secondary;
    }

    @Override
    public String deleteAllEntryOrders(String userId, String fxcmAccount) {
        String userKey = userId + fxcmAccount;
        TradeThread tradeThread = tradeThreadMap.get(userKey);
        if (tradeThread == null) {
            return "登录状态异常，请重新登录";
        }

        List<String> secondary = tradeThread.deleteAllEntryOrders();

        //2.获取跟随者数据
        List<FXCMInfoEntity> fxcmInfoEntities = userFollowMap.get(userId);
        if (fxcmInfoEntities != null) {
            fxcmInfoEntities.forEach((entry) -> {
                TradeThread followerThread = tradeThreadMap.get(entry.getUserId() + entry.getFxcmAccount());
                if (followerThread != null) {
                    followerThread.deleteAllEntryOrders(secondary);
                }
            });
        }
        return null;
    }


    @Override
    public String createSLEntryOrder(String userId, String fxcmAccount, String orderId, String price, String type) {
        String userKey = userId + fxcmAccount;
        TradeThread tradeThread = tradeThreadMap.get(userKey);
        if (tradeThread == null) {
            return "登录状态异常，请重新登录";
        }
        String secondary = tradeThread.createStopLimitEntryOrder(orderId, price, type, null);

        //2.获取跟随者数据
        List<FXCMInfoEntity> fxcmInfoEntities = userFollowMap.get(userId);
        if (fxcmInfoEntities != null) {
            fxcmInfoEntities.forEach((entry) -> {
                TradeThread followerThread = tradeThreadMap.get(entry.getUserId() + entry.getFxcmAccount());
                if (followerThread != null) {
                    followerThread.createStopLimitEntryOrder(price, type, secondary);
                }
            });
        }
        return secondary;
    }


    @Override
    public String updateSLEntryOrder(String userId, String fxcmAccount, String orderId, String type, String price) {
        String userKey = userId + fxcmAccount;
        TradeThread tradeThread = tradeThreadMap.get(userKey);
        if (tradeThread == null) {
            return "登录状态异常，请重新登录";
        }
        String secondary = tradeThread.updateStopLimitEntryOrder(orderId, type, price, null);

        //2.获取跟随者数据
        List<FXCMInfoEntity> fxcmInfoEntities = userFollowMap.get(userId);
        if (fxcmInfoEntities != null) {
            fxcmInfoEntities.forEach((entry) -> {
                TradeThread followerThread = tradeThreadMap.get(entry.getUserId() + entry.getFxcmAccount());
                if (followerThread != null) {
                    followerThread.updateStopLimitEntryOrder(type, price, secondary);
                }
            });
        }
        return secondary;
    }


    @Override
    public String deleteSLEntryOrder(String userId, String fxcmAccount, String orderId, String type) {
        String userKey = userId + fxcmAccount;
        TradeThread tradeThread = tradeThreadMap.get(userKey);
        if (tradeThread == null) {
            return "登录状态异常，请重新登录";
        }
        String secondary = tradeThread.deleteStopLimitEntryOrder(orderId, type, null);

        //2.获取跟随者数据
        List<FXCMInfoEntity> fxcmInfoEntities = userFollowMap.get(userId);
        if (fxcmInfoEntities != null) {
            fxcmInfoEntities.forEach((entry) -> {
                TradeThread followerThread = tradeThreadMap.get(entry.getUserId() + entry.getFxcmAccount());
                if (followerThread != null) {
                    followerThread.deleteStopLimitEntryOrder(type, secondary);
                }
            });
        }
        return secondary;
    }

    @Override
    public String changeFXCMPassword(String userId, String fxcmAccount, String password, String newPassword) {
        String userKey = userId + fxcmAccount;
        TradeThread tradeThread = tradeThreadMap.get(userKey);
        if (tradeThread == null) {
            return "登录状态异常，请重新登录";
        }
        String secondary = tradeThread.changePassword(fxcmAccount, password, newPassword);
        return secondary;
    }

    @Override
    public String getOpenPositions(String userId, String fxcmAccount) {
        String userKey = userId + fxcmAccount;
        TradeThread tradeThread = tradeThreadMap.get(userKey);
        if (tradeThread == null) {
            Map map = new HashMap();
            map.put("message", "登录状态异常，请重新登录");
            try {
                return objectMapper.writeValueAsString(map);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        Map<String, OpenPositionEntity> openPositionsMap = tradeThread.getOpenPositionsMap();
        String s = null;
        try {
            s = objectMapper.writeValueAsString(openPositionsMap);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return s;
    }

    @Override
    public String getOpenOrders(String userId, String fxcmAccount) {
        String userKey = userId + fxcmAccount;
        TradeThread tradeThread = tradeThreadMap.get(userKey);

        if (tradeThread == null) {
            Map map = new HashMap();
            map.put("message", "登录状态异常，请重新登录");
            try {
                return objectMapper.writeValueAsString(map);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        Map<String, OrderEntity> openOrderMap = tradeThread.getOpenOrderMap();
        String result = null;
        try {
            result = objectMapper.writeValueAsString(openOrderMap);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public String getClosedPositions(String userId, String fxcmAccount) {
        String userKey = userId + fxcmAccount;
        TradeThread tradeThread = tradeThreadMap.get(userKey);
        if (tradeThread == null) {
            Map map = new HashMap();
            map.put("message", "登录状态异常，请重新登录");
            try {
                return objectMapper.writeValueAsString(map);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        Map<String, ClosedPositionReport> openOrderMap = tradeThread.getClosedPositionsMap();
        String result = null;
        try {
            result = objectMapper.writeValueAsString(openOrderMap);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public String getCollateralReport(String userId, String fxcmAccount) {
        String userKey = userId + fxcmAccount;
        TradeThread tradeThread = tradeThreadMap.get(userKey);
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

    /**
     * 主要步骤：
     * 1.判断用户数量（此账户当前有几个人登录，如果大于一个，则不退出，如果只有一个，则退出）
     * 2.如果第一步为真，则调用logout方法退出福汇账号
     * 3.结束线程
     * 4.从ThreadMap中移除相关账号
     *
     * @param userId      userID
     * @param fxcmAccount fxcm账号
     * @return 退出登录交易账号
     */
    @Override
    public String logoutFXCM(String userId, String fxcmAccount) {
        //TODO  退出程序需要判断是否为交易员，对其跟随者进行相同判定和操作
        String key = userId + fxcmAccount;
        System.out.println("k01::::" + loginStatusMap.get(key));
        if (loginStatusMap.get(key) == null || loginStatusMap.get(key) == 0) {
            return "尚未登录，无需退出";
        }
        if (loginStatusMap.get(key) == 1) {
            System.out.println("k02::::" + loginStatusMap.get(key));
            TradeThread remove = tradeThreadMap.remove(key);
            String logout = remove.logout();
            loginStatusMap.remove(key);
            return logout;
        } else {
            loginStatusMap.put(key, loginStatusMap.get(key) - 1);
            System.out.println(loginStatusMap.get(key));
            return "logout OK";
        }
    }

    @Override
    public String getMarketDataSnapshot(String userId, String fxcmAccount) {
        String userKey = userId + fxcmAccount;
        TradeThread tradeThread = tradeThreadMap.get(userKey);
        if (tradeThread == null) {
            Map map = new HashMap();
            map.put("message", "登录状态异常，请重新登录");
            try {
                return objectMapper.writeValueAsString(map);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        Map<String, MarketDataSnapshot> marketDataSnapshotMap = tradeThread.getMarketDataSnapshotMap();
        Map map = new HashMap();
        map.put("marketDataSnapshot", marketDataSnapshotMap);
        String result = null;
        try {
            result = objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public String getOrderExecutionReport(String userId, String fxcmAccount, String listId) {
        String result = null;

        try {
            if (tradeThreadMap.get(userId + fxcmAccount) == null) {
                return null;
            }
            if (tradeThreadMap.get(userId + fxcmAccount).getOrderExecutionReport(listId) == null) {
                return null;
            }
            result = objectMapper.writeValueAsString(tradeThreadMap.get(userId + fxcmAccount).getOrderExecutionReport(listId));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return result;
    }
}
