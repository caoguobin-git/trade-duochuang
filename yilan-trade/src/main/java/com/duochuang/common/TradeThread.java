/***********************************************
 * File Name: TradeThread
 * Author: caoguobin
 * mail: caoguobin@live.com
 * Created Time: 26 02 2019 14:06
 ***********************************************/

package com.duochuang.common;

import com.duochuang.entity.FXCMInfoEntity;
import com.duochuang.entity.OpenPositionEntity;
import com.duochuang.entity.OrderEntity;
import com.fxcm.external.api.transport.FXCMLoginProperties;
import com.fxcm.external.api.transport.GatewayFactory;
import com.fxcm.external.api.transport.IGateway;
import com.fxcm.external.api.transport.listeners.IGenericMessageListener;
import com.fxcm.external.api.transport.listeners.IStatusMessageListener;
import com.fxcm.external.api.util.MessageGenerator;
import com.fxcm.external.api.util.OrdStatusRequestType;
import com.fxcm.fix.*;
import com.fxcm.fix.other.UserRequest;
import com.fxcm.fix.posttrade.ClosedPositionReport;
import com.fxcm.fix.posttrade.CollateralReport;
import com.fxcm.fix.posttrade.PositionReport;
import com.fxcm.fix.pretrade.MarketDataSnapshot;
import com.fxcm.fix.trade.*;
import com.fxcm.messaging.ISessionStatus;
import com.fxcm.messaging.ITransportable;
import com.google.common.base.Strings;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@SuppressWarnings("Duplicates")
public class TradeThread implements Runnable {

    private String changePassword;

    private FXCMInfoEntity fxcmInfoEntity;
    private CollateralReport collateralReport;
    private Map<String, OpenPositionEntity> openPositionsMap = new ConcurrentHashMap<>();
    private Map<String, ClosedPositionReport> closedPositionsMap = new ConcurrentHashMap<>();
    private IGateway iGateway;
    private IGenericMessageListener iGenericMessageListener;
    private IStatusMessageListener iStatusMessageListener;
    private static Map<String, MarketDataSnapshot> marketDataSnapshotMap = new ConcurrentHashMap<>();

    private String closeTrueMarketOrderID = null;

    private String createTrueMarketOrderID = null;

    private String createEntryOrderID = null;

    private Set<String> failedOrder = new HashSet<>();

    private HashMap<String, LinkedList<ExecutionReport>> orderHistory = new LinkedHashMap() {
        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > 10;
        }
    };


    private Map<String, OrderEntity> openOrderMap = new ConcurrentHashMap<>();
    private boolean exit = false;
    private boolean loggedIn = true;
    private String requestId = null;

    private String rId = null;

    Logger logger = Logger.getLogger(TradeThread.class);

    public TradeThread(FXCMInfoEntity fxcmInfoEntity) {
        this.fxcmInfoEntity = fxcmInfoEntity;
    }

    public boolean isExit() {
        return exit;
    }

    public void setExit(boolean exit) {
        this.exit = exit;
    }

    public CollateralReport getCollateralReport() {
        return collateralReport;
    }

    public Map<String, OpenPositionEntity> getOpenPositionsMap() {
        return openPositionsMap;
    }

    public Map<String, ClosedPositionReport> getClosedPositionsMap() {
        return closedPositionsMap;
    }

    public IGateway getiGateway() {
        return iGateway;
    }

    public Map<String, MarketDataSnapshot> getMarketDataSnapshotMap() {
        return marketDataSnapshotMap;
    }


    public Map<String, OrderEntity> getOpenOrderMap() {
        return openOrderMap;
    }

    public FXCMInfoEntity getFxcmInfoEntity() {
        return fxcmInfoEntity;
    }

    public void setFxcmInfoEntity(FXCMInfoEntity fxcmInfoEntity) {
        this.fxcmInfoEntity = fxcmInfoEntity;
    }

    @Override
    public void run() {
        startTradeSession();
    }

    private void startTradeSession() {
        iGateway = GatewayFactory.createGateway();

        logger.info("当前iGateway：===>" + iGateway);
        iGenericMessageListener = new IGenericMessageListener() {
            @Override
            public void messageArrived(ITransportable iMessage) {

                if (!(iMessage instanceof MarketDataSnapshot)) {
//                    System.out.println(iMessage.getClass().getName());
//                    System.out.println("iMessage::::::" + iMessage);
                }
                if (iMessage instanceof MarketDataSnapshot) {
                    MarketDataSnapshot marketDataSnapshot = (MarketDataSnapshot) iMessage;
                    try {
                        marketDataSnapshotMap.put(marketDataSnapshot.getInstrument().getSymbol(), marketDataSnapshot);
                    } catch (NotDefinedException e) {
                        e.printStackTrace();
                    }
                } else if (iMessage instanceof CollateralReport) {
                    CollateralReport collateral = (CollateralReport) iMessage;
                    System.out.println("当前账户：==>" + collateralReport);
                    iGateway.requestOpenPositions();
                    iGateway.requestClosedPositions();
                    iGateway.requestOpenOrders();
                    collateralReport = collateral;
                } else if (iMessage instanceof ClosedPositionReport) {
                    ClosedPositionReport closedPositionReport = (ClosedPositionReport) iMessage;
                    closedPositionsMap.put(closedPositionReport.getFXCMPosID(), closedPositionReport);
                    openPositionsMap.remove(closedPositionReport.getFXCMPosID());
                } else if (iMessage instanceof PositionReport) {
                    PositionReport positionReport = (PositionReport) iMessage;

                    openOrderMap.remove(positionReport.getFXCMPosID());
                    OpenPositionEntity openPositionEntity = openPositionsMap.get(positionReport.getFXCMPosID());
                    if (openPositionEntity == null) {
                        openPositionEntity = new OpenPositionEntity();
                        openPositionEntity.setPosition(positionReport);
                        openPositionsMap.put(positionReport.getFXCMPosID(), openPositionEntity);
                    } else {
                        openPositionEntity.setPosition(positionReport);
                    }
                } else if (iMessage instanceof ExecutionReport) {
                    ExecutionReport executionReport = (ExecutionReport) iMessage;

                    //订单执行失败，推送失败原因
//                    if (executionReport.getFXCMOrdStatus().getCode().equalsIgnoreCase("R")) {
//
//                        if (!failedOrder.contains(executionReport.getListID()) && !"Limit".equalsIgnoreCase(executionReport.getOrdType().getDesc()) && !"stop".equalsIgnoreCase(executionReport.getOrdType().getDesc())) {
//                            failedOrder.add(executionReport.getListID());
//                            String reject = null;
//                            try {
//                                reject = executionReport.getFXCMErrorDetails();
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                            }
//                            Map<String, String> params = new HashMap<>();
//                            params.put("token", "888888");
//                            params.put("msg", reject);
//                            params.put("title", "下单失败");
//                            JPush.jpushOrderFailed(params);
//                        } else if (!failedOrder.contains(executionReport.getListID())) {
//                            if ("Limit".equalsIgnoreCase(executionReport.getOrdType().getDesc())) {
//                                String reject = null;
//                                try {
//                                    reject = executionReport.getFXCMErrorDetails();
//                                } catch (Exception e) {
//                                    e.printStackTrace();
//                                }
//                                Map<String, String> params = new HashMap<>();
//                                params.put("token", "888888");
//                                params.put("msg", reject);
//                                params.put("title", "设置止盈失败");
//                                JPush.jpushOrderFailed(params);
//                            }
//                            if ("Stop".equalsIgnoreCase(executionReport.getOrdType().getDesc())) {
//                                String reject = null;
//                                try {
//                                    reject = executionReport.getFXCMErrorDetails();
//                                } catch (Exception e) {
//                                    e.printStackTrace();
//                                }
//                                Map<String, String> params = new HashMap<>();
//                                params.put("token", "888888");
//                                params.put("msg", reject);
//                                params.put("title", "设置止损失败");
//                                JPush.jpushOrderFailed(params);
//                            }
//                        }
//                    }

                    if (!Strings.isNullOrEmpty(createTrueMarketOrderID) && createTrueMarketOrderID.equalsIgnoreCase(executionReport.getListID()) && "R".equalsIgnoreCase(executionReport.getFXCMOrdStatus().getCode())) {
                        if (orderHistory.get(createTrueMarketOrderID) == null) {
                            LinkedList<ExecutionReport> linkedList = new LinkedList<>();
                            linkedList.add(executionReport);
                            orderHistory.put(createTrueMarketOrderID, linkedList);
                        } else {
                            LinkedList<ExecutionReport> linkedList = orderHistory.get(createTrueMarketOrderID);
                            linkedList.add(executionReport);
                            orderHistory.put(createTrueMarketOrderID, linkedList);
                        }
                    }

                    if (executionReport.getFXCMPosID() != null) {
                        if (openPositionsMap.get(executionReport.getFXCMPosID()) == null && closedPositionsMap.get(executionReport.getFXCMPosID()) == null) {
//                            if (openOrderMap.get(executionReport.getFXCMPosID()) == null && executionReport.getOrdStatus().getCode().equalsIgnoreCase("0")) {
                            if (Strings.isNullOrEmpty(executionReport.getFXCMContingencyID()) || "null".equalsIgnoreCase(executionReport.getFXCMContingencyID())) {
                                if (((executionReport.getFXCMOrdStatus().getCode().equalsIgnoreCase("0") || "5".equalsIgnoreCase(executionReport.getFXCMOrdStatus().getCode()) || "w".equalsIgnoreCase(executionReport.getFXCMOrdStatus().getCode())))) {
                                    OrderEntity orderEntity = openOrderMap.get(executionReport.getFXCMPosID());
                                    if (orderEntity == null) {
                                        orderEntity = new OrderEntity();
                                        orderEntity.setMainOrder(executionReport);
                                        openOrderMap.put(executionReport.getFXCMPosID(), orderEntity);
                                    } else if (orderEntity != null) {
                                        orderEntity.setMainOrder(executionReport);
                                    }
                                } else if ("r".equalsIgnoreCase(executionReport.getFXCMOrdStatus().getCode())) {
                                    openOrderMap.remove(executionReport.getFXCMPosID());
                                    getOpenOrders();

                                } else {
                                    openOrderMap.remove(executionReport.getFXCMPosID());
                                }
                            } else if (executionReport.getFXCMContingencyID() != null) {
                                if ("S".equalsIgnoreCase(executionReport.getFXCMOrdType().getCode())) {
                                    if (((executionReport.getFXCMOrdStatus().getCode().equalsIgnoreCase("0") || "5".equalsIgnoreCase(executionReport.getFXCMOrdStatus().getCode()) || "w".equalsIgnoreCase(executionReport.getFXCMOrdStatus().getCode())))) {
                                        OrderEntity orderEntity = openOrderMap.get(executionReport.getFXCMPosID());
                                        if (orderEntity == null) {
                                            orderEntity = new OrderEntity();
                                            orderEntity.setStop(executionReport);
                                            openOrderMap.put(executionReport.getFXCMPosID(), orderEntity);
                                        } else if (orderEntity != null) {
                                            orderEntity.setStop(executionReport);
                                        }
                                    } else {
                                        openOrderMap.get(executionReport.getFXCMPosID()).setStop(null);
                                    }
                                } else if ("L".equalsIgnoreCase(executionReport.getFXCMOrdType().getCode())) {
                                    if (((executionReport.getFXCMOrdStatus().getCode().equalsIgnoreCase("0") || "5".equalsIgnoreCase(executionReport.getFXCMOrdStatus().getCode()) || "w".equalsIgnoreCase(executionReport.getFXCMOrdStatus().getCode())))) {
                                        OrderEntity orderEntity = openOrderMap.get(executionReport.getFXCMPosID());
                                        if (orderEntity == null) {
                                            orderEntity = new OrderEntity();
                                            orderEntity.setLimit(executionReport);
                                            openOrderMap.put(executionReport.getFXCMPosID(), orderEntity);
                                        } else if ((orderEntity != null)) {
                                            orderEntity.setLimit(executionReport);
                                        }
                                    } else {
                                        openOrderMap.get(executionReport.getFXCMPosID()).setLimit(null);

                                    }
                                }
                            }
                        } else {
                            if (!Strings.isNullOrEmpty(String.valueOf(executionReport.getFXCMOrdType())) && "S".equalsIgnoreCase(executionReport.getFXCMOrdType().getCode())) {
                                if (((executionReport.getFXCMOrdStatus().getCode().equalsIgnoreCase("0") || "5".equalsIgnoreCase(executionReport.getFXCMOrdStatus().getCode()) || "w".equalsIgnoreCase(executionReport.getFXCMOrdStatus().getCode())))) {
                                    OpenPositionEntity openPositionEntity = openPositionsMap.get(executionReport.getFXCMPosID());
                                    if (openPositionEntity == null) {
                                        openPositionEntity = new OpenPositionEntity();
                                        openPositionEntity.setStop(executionReport);
                                        openPositionsMap.put(executionReport.getFXCMPosID(), openPositionEntity);
                                    } else if (openPositionEntity != null) {
                                        openPositionEntity.setStop(executionReport);
                                    }
                                } else {
                                    if (openPositionsMap.get(executionReport.getFXCMPosID()) != null) {
                                        openPositionsMap.get(executionReport.getFXCMPosID()).setStop(null);
                                    }
                                }
                            } else if (!Strings.isNullOrEmpty(String.valueOf(executionReport.getFXCMOrdType())) && "L".equalsIgnoreCase(executionReport.getFXCMOrdType().getCode())) {
                                if (((executionReport.getFXCMOrdStatus().getCode().equalsIgnoreCase("0") || "5".equalsIgnoreCase(executionReport.getFXCMOrdStatus().getCode()) || "w".equalsIgnoreCase(executionReport.getFXCMOrdStatus().getCode())))) {
                                    OpenPositionEntity openPositionEntity = openPositionsMap.get(executionReport.getFXCMPosID());
                                    if (openPositionEntity == null) {
                                        openPositionEntity = new OpenPositionEntity();
                                        openPositionEntity.setLimit(executionReport);
                                        openPositionsMap.put(executionReport.getFXCMPosID(), openPositionEntity);
                                    } else if (openPositionEntity != null) {
                                        openPositionEntity.setLimit(executionReport);
                                    }
                                } else {
                                    if (executionReport.getFXCMPosID() != null) {
                                        if (openPositionsMap.get(executionReport.getFXCMPosID()) != null) {
                                            openPositionsMap.get(executionReport.getFXCMPosID()).setLimit(null);
                                        }
                                    }

                                }
                            }
                        }
                    }
                    if ("REJECTED".equalsIgnoreCase(executionReport.getExecType().getLabel())) {
                        logger.trace("订单被拒绝===>" + executionReport);
                    } else {
                        System.out.println("订单执行报告===>" + executionReport);
                    }
                }
            }
        };
        iStatusMessageListener = new IStatusMessageListener() {
            @Override
            public void messageArrived(ISessionStatus aStatus) {
                switch (aStatus.getStatusCode()) {
                    case ISessionStatus.STATUSCODE_READY:
                    case ISessionStatus.STATUSCODE_SENDING:
                    case ISessionStatus.STATUSCODE_RECIEVING:
                    case ISessionStatus.STATUSCODE_PROCESSING:
                    case ISessionStatus.STATUSCODE_WAIT:
                        break;
                    case ISessionStatus.STATUSCODE_LOGGEDIN:
                        System.out.println("已连接到：" + iGateway);
                        break;
                    case ISessionStatus.STATUSCODE_ERROR:
                        break;
                    default:
                        System.out.println((
                                "client: inc status msg = ["
                                        + aStatus.getStatusCode()
                                        + "] ["
                                        + aStatus.getStatusName()
                                        + "] ["
                                        + aStatus.getStatusMessage()
                                        + "]").toUpperCase());
                        if (aStatus.getStatusCode() == ISessionStatus.STATUSCODE_DISCONNECTED) {
                            try {

                                if (!loggedIn) {
                                    iGateway.relogin();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();

                            }
                        }
                        break;
                }
            }
        };
        iGateway.registerGenericMessageListener(iGenericMessageListener);
        iGateway.registerStatusMessageListener(iStatusMessageListener);
        login();
    }

    private void login() {
        try {
            FXCMLoginProperties properties = new FXCMLoginProperties(fxcmInfoEntity.getFxcmAccount(), fxcmInfoEntity.getFxcmPassword(), fxcmInfoEntity.getAccountType(), fxcmInfoEntity.getHostAddr());

            System.out.println("fxcmAccount: " + fxcmInfoEntity.getFxcmAccount());
            System.out.println("fxcmPassword: " + fxcmInfoEntity.getFxcmPassword());
            System.out.println("fxcmAccountType: " + fxcmInfoEntity.getAccountType());
            System.out.println("fxcmHostAddress: " + fxcmInfoEntity.getHostAddr());
            System.out.println("properties:" + properties.toString());
            iGateway.login(properties);
            iGateway.requestTradingSessionStatus();
            iGateway.requestAccounts();
            iGateway.requestOpenPositions();
            iGateway.requestClosedPositions();
            iGateway.requestOpenOrders();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("登录失败");
            startTradeSession();
        }
        loggedIn = true;
    }

    public String logout() {
        try {
            exit = true;
            iGateway.removeGenericMessageListener(iGenericMessageListener);
            iGateway.removeStatusMessageListener(iStatusMessageListener);
            iGateway.logout();
        } catch (Exception e) {
            return e.getMessage();
        }
        return "logout OK";

    }


    /**
     * 创建市价单
     *
     * @param amount           交易数量
     * @param side             交易方向（sell/buy）
     * @param currency         交易货币符号（EUR/USD）
     * @param secondaryClOrdID 重要！！！（用于识别跟单操作）
     * @return 返回requestID，可以调用status方法查询订单执行状态
     */
    @Deprecated
    public String createTrueMarketOrder(String amount, String side, String currency, String secondaryClOrdID) {
        ISide iSide = null;
        if ("sell".equalsIgnoreCase(side)) {
            iSide = SideFactory.SELL;
        } else if ("buy".equalsIgnoreCase(side)) {
            iSide = SideFactory.BUY;
        } else {
            return "wrong param";
        }

        OrderSingle orderSingle = MessageGenerator.generateMarketOrder(collateralReport.getAccount(), Double.parseDouble(amount), iSide, currency, secondaryClOrdID);
        try {
            createTrueMarketOrderID = iGateway.sendMessage(orderSingle);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return createTrueMarketOrderID;
    }

    /**
     * 创建带有止盈/止损的市价单
     *
     * @param currency    货币符号
     * @param tradeSide   交易方向
     * @param tradeAmount 交易数量
     * @param tradeStop   止损价格（不设置则传0）
     * @param tradeLimit  止盈价格（不设置则传0）
     * @param secondary   secondaryId
     * @return 查询id
     */
    public String createTrueMarketOrder(double cashOutStanding, String currency, String tradeSide, String tradeAmount, String tradeStop, String tradeLimit, String secondary) {
        double amount = Double.parseDouble(tradeAmount);
        double stop = Double.parseDouble(tradeStop);
        double limit = Double.parseDouble(tradeLimit);


        /**
         * 判断交易种类
         * 1.外汇乘以100000 取1000的整数倍
         * 2.黄金/白银 取factor的整数倍
         */
        MarketDataSnapshot marketDataSnapshot = marketDataSnapshotMap.get(currency);
        int product = marketDataSnapshot.getInstrument().getProduct();
        int factor = marketDataSnapshot.getInstrument().getFactor();
        if (product==2){
            amount=getBullionAmount(cashOutStanding,amount,factor);
        }else if (product==4){
            amount=getForexAmount(cashOutStanding,amount);
        }

        if (amount<=0){
            return null;
        }

        ISide side = null;
        ISide slSide = null;


        if ("sell".equalsIgnoreCase(tradeSide)) {
            side = SideFactory.SELL;
            slSide = SideFactory.BUY;

        } else if ("buy".equalsIgnoreCase(tradeSide)) {
            side = SideFactory.BUY;
            slSide = SideFactory.SELL;
        } else {
            return "wrong side";
        }

        OrderList orderList = new OrderList();
        orderList.setContingencyType(ContingencyTypeFactory.ELS);


        OrderSingle primary = MessageGenerator.generateMarketOrder(
                collateralReport.getAccount(),
                amount,
                side,
                currency,
                secondary);
        primary.setClOrdLinkID(IFixDefs.CLORDLINKID_PRIMARY);
        orderList.addOrder(primary);

        System.out.println(amount+" "+side+" "+currency+" "+secondary);
        if (stop != 0) {
            OrderSingle stopOrder = MessageGenerator.generateStopLimitEntry(
                    stop,
                    OrdTypeFactory.STOP,
                    collateralReport.getAccount(),
                    amount,
                    slSide,
                    currency,
                    secondary);
            stopOrder.setClOrdLinkID(IFixDefs.CLORDLINKID_CONTINGENT);
            orderList.addOrder(stopOrder);
        }

        if (limit != 0) {
            OrderSingle limitOrder = MessageGenerator.generateStopLimitEntry(
                    limit,
                    OrdTypeFactory.LIMIT,
                    collateralReport.getAccount(),
                    amount,
                    slSide,
                    currency,
                    secondary);
            limitOrder.setClOrdLinkID(IFixDefs.CLORDLINKID_CONTINGENT);
            orderList.addOrder(limitOrder);
        }

        try {
            createTrueMarketOrderID = iGateway.sendMessage(orderList);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return createTrueMarketOrderID;
    }

    //获取外汇交易数量
    private double getForexAmount(double cashOutStanding, double amount) {
        double trader=cashOutStanding;
        double follower=collateralReport.getCashOutstanding();
        return  ((int)(follower/trader*amount*100000))/1000*1000;
    }

    //获取黄金和白银的交易数量
    private double getBullionAmount(double cashOutStanding, double amount, int factor) {
        double trader=cashOutStanding;
        double follower = collateralReport.getCashOutstanding();
        double result=((int)(follower/trader*amount))/factor*factor;
        return result;
    }


    /**
     * 市价单平仓
     *
     * @param fxcmPosID 需要平仓的positionId
     * @param secondary 操作的secondary
     * @return
     */
    public String deleteTrueMarketOrder(String fxcmPosID, String secondary) {
        if (openPositionsMap.get(fxcmPosID) == null) {
            return "wrong fxcmPos";
        }
        PositionReport positionReport = openPositionsMap.get(fxcmPosID).getPosition();
        if (positionReport == null) {
            return "wrong fxcmPosID";
        }
        String posid = positionReport.getFXCMPosID();
        String account = positionReport.getAccount();
        double amount = positionReport.getPositionQty().getQty();
        ISide side = null;
        String posSide = positionReport.getPositionQty().getSide().getLabel();
        secondary = positionReport.getSecondaryClOrdID();
        if ("sell".equalsIgnoreCase(posSide)) {
            side = SideFactory.BUY;
        } else if ("buy".equalsIgnoreCase(posSide)) {
            side = SideFactory.SELL;
        }
        String currency = null;
        try {
            currency = positionReport.getInstrument().getSymbol();
        } catch (NotDefinedException e) {
            e.printStackTrace();
        }
        OrderSingle orderSingle = MessageGenerator.generateCloseMarketOrder(
                posid,
                account,
                amount,
                side,
                currency,
                secondary);
        try {
            requestId = iGateway.sendMessage(orderSingle);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return secondary;
    }

    /**
     * 根据secondary进行市价单平仓
     *
     * @param secondary
     * @return
     */
    public String deleteTrueMarketOrder(String secondary) {
        if (Strings.isNullOrEmpty(secondary)) {
            return null;
        }

        for (Map.Entry<String, OpenPositionEntity> entry : openPositionsMap.entrySet()) {
            if (entry.getValue() != null) {
                if (entry.getValue().getPosition() != null) {
                    if (entry.getValue().getPosition().getSecondaryClOrdID() != null) {
                        if (secondary.equalsIgnoreCase(entry.getValue().getPosition().getSecondaryClOrdID())) {
                            closeTrueMarketOrderID = deleteTrueMarketOrder(entry.getValue().getPosition().getFXCMPosID(), secondary);
                        }
                    }
                }
            }
        }
        return closeTrueMarketOrderID;
    }

    /**
     * TODO 改用orderlist？
     * 关闭所有开仓位置(交易员调用)
     *
     * @return
     */
    public List<String> deleteAllOpenPositions() {
        List<String> secondarylist = new LinkedList<>();
        if (openPositionsMap.size() > 0) {
            for (Map.Entry<String, OpenPositionEntity> entry : openPositionsMap.entrySet()) {
                String second = deleteTrueMarketOrder(entry.getValue().getPosition().getFXCMPosID(), new Date().toString());
                secondarylist.add(second);
            }
        }
        return secondarylist;
    }

    /**
     * 根据secondary进行全部平仓操作（跟随者调用）
     * 用于对跟随者账号进行平仓操作
     *
     * @param secondary secondaryId
     * @return secondaryId
     */
    public String deleteAllOpenPositions(String secondary) {
        if (Strings.isNullOrEmpty(secondary)) {
            return null;
        }
        String result = null;
        if (openPositionsMap.size() > 0) {
            for (Map.Entry<String, OpenPositionEntity> entry : openPositionsMap.entrySet()) {
                if (entry.getValue().getPosition() != null) {
                    if (entry.getValue().getPosition().getSecondaryClOrdID() != null) {
                        if (secondary.equalsIgnoreCase(entry.getValue().getPosition().getSecondaryClOrdID())) {
                            result = deleteTrueMarketOrder(secondary);
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * 新建挂单
     *
     * @param price     挂单价格
     * @param type      挂单方式（stop/limit）
     * @param amount    挂单数量
     * @param side      交易方向
     * @param currency  货币符号
     * @param secondary 交易secondaryId
     * @return
     */
    public String createEntryOrder(String price, String type, String amount, String side, String currency, String secondary) {
        double price1 = Double.parseDouble(price);
        IOrdType ordType = null;
        if ("limit".equalsIgnoreCase(type)) {
            ordType = OrdTypeFactory.LIMIT;
        } else if ("stop".equalsIgnoreCase(type)) {
            ordType = OrdTypeFactory.STOP;
        } else {
            return "wrong param";
        }
        Double amount1 = Double.parseDouble(amount);
        ISide iSide = null;
        if ("sell".equalsIgnoreCase(side)) {
            iSide = SideFactory.SELL;
        } else if ("buy".equalsIgnoreCase(side)) {
            iSide = SideFactory.BUY;
        } else {
            return "wrong param";
        }

        OrderSingle orderSingle = MessageGenerator.generateStopLimitEntry(
                price1,
                ordType,
                collateralReport.getAccount(),
                amount1,
                iSide,
                currency,
                secondary);
        try {
            createEntryOrderID = iGateway.sendMessage(orderSingle);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return createEntryOrderID;
    }

    /**
     * 创建带S/L的entryorder
     *
     * @param price     挂单价格
     * @param type      挂单方式
     * @param amount    挂单数量
     * @param side      交易方向
     * @param currency  交易货币
     * @param stop      止损价格（未设置传0）
     * @param limit     止盈价格（未设置传0）
     * @param secondary secondaryId
     * @return secondaryId
     */
    public String createEntryOrder(String price, String type, String amount, String side, String currency, String stop, String limit, String secondary) {
        double price1 = Double.parseDouble(price);
        double stopPrice = Double.parseDouble(stop);
        double limitPrice = Double.parseDouble(limit);
        Double amount1 = Double.parseDouble(amount);
        IOrdType ordType = null;
        if ("limit".equalsIgnoreCase(type)) {
            ordType = OrdTypeFactory.LIMIT;
        } else if ("stop".equalsIgnoreCase(type)) {
            ordType = OrdTypeFactory.STOP;
        } else {
            return "wrong param";
        }
        ISide iSide = null;
        ISide slSide = null;
        if ("sell".equalsIgnoreCase(side)) {
            iSide = SideFactory.SELL;
            slSide = SideFactory.BUY;
        } else if ("buy".equalsIgnoreCase(side)) {
            iSide = SideFactory.BUY;
            slSide = SideFactory.SELL;
        } else {
            return "wrong param";
        }
        OrderList orderList = new OrderList();
        orderList.setContingencyType(ContingencyTypeFactory.ELS);

        OrderSingle orderSingle = MessageGenerator.generateStopLimitEntry(
                price1,
                ordType,
                collateralReport.getAccount(),
                amount1,
                iSide,
                currency,
                secondary);
        orderSingle.setClOrdLinkID(IFixDefs.CLORDLINKID_PRIMARY);
        orderList.addOrder(orderSingle);

        if (stopPrice != 0) {
            OrderSingle stopOrder = MessageGenerator.generateStopLimitEntry(
                    stopPrice,
                    OrdTypeFactory.STOP,
                    collateralReport.getAccount(),
                    amount1,
                    slSide,
                    currency,
                    secondary);
            stopOrder.setClOrdLinkID(IFixDefs.CLORDLINKID_CONTINGENT);
            orderList.addOrder(stopOrder);
        }

        if (limitPrice != 0) {
            OrderSingle limitOrder = MessageGenerator.generateStopLimitEntry(
                    limitPrice,
                    OrdTypeFactory.LIMIT,
                    collateralReport.getAccount(),
                    amount1,
                    slSide,
                    currency,
                    secondary);
            limitOrder.setClOrdLinkID(IFixDefs.CLORDLINKID_CONTINGENT);
            orderList.addOrder(limitOrder);
        }

        try {
            createEntryOrderID = iGateway.sendMessage(orderList);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return createEntryOrderID;
    }

    /**
     * 更改挂单价格（交易员调用）
     *
     * @param orderId   要更改的订单id
     * @param price     要更改的价格
     * @param secondary secondary标识
     * @return secondaryId
     */
    public String updateEntryOrder(String orderId, String amount, String price, String secondary) {
        ExecutionReport executionReport = null;
        for (Map.Entry<String, OrderEntity> entry : openOrderMap.entrySet()) {
            if (orderId.equalsIgnoreCase(entry.getValue().getMainOrder().getOrderID())) {
                executionReport = entry.getValue().getMainOrder();
            }
        }
        secondary = executionReport.getSecondaryClOrdID();
        updateEntryMethod(amount, price, secondary, executionReport);
        return secondary;

    }

    /**
     * 根据secondaryId更改挂单价格（跟随者调用）
     *
     * @param price     目标价格
     * @param secondary secondaryId
     * @return secondaryId
     */
    public String updateEntryOrder(String amount, String price, String secondary) {
        if (Strings.isNullOrEmpty(secondary)) {
            return null;
        }
        ExecutionReport executionReport = null;
        for (Map.Entry<String, OrderEntity> entry : openOrderMap.entrySet()) {
            if (entry.getValue() != null) {
                if (entry.getValue().getMainOrder() != null) {
                    if (entry.getValue().getMainOrder().getSecondaryClOrdID() != null) {
                        if (secondary.equalsIgnoreCase(entry.getValue().getMainOrder().getSecondaryClOrdID())) {
                            executionReport = entry.getValue().getMainOrder();
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
            } else {
                continue;
            }
        }
        return updateEntryMethod(amount, price, secondary, executionReport);
    }

    /**
     * 更改挂单价格具体实现方法
     *
     * @param price
     * @param secondary
     * @param executionReport
     * @return
     */
    private String updateEntryMethod(String amount, String price, String secondary, ExecutionReport executionReport) {
        double price1 = Double.parseDouble(price);
        if (price1 == 0) {
            price1 = executionReport.getPrice();
        }
        double amount1 = Double.parseDouble(amount);
        if (amount1 == 0) {
            amount1 = executionReport.getOrderQty();
        }


        String s = null;
        OrderCancelReplaceRequest orderCancelReplaceRequest = MessageGenerator.generateOrderReplaceRequest(
                secondary,
                executionReport.getOrderID(),
                executionReport.getSide(),
                executionReport.getOrdType(),
                price1,
                collateralReport.getAccount());

        orderCancelReplaceRequest.setOrderID(executionReport.getOrderID());
        orderCancelReplaceRequest.setOrigClOrdID(executionReport.getClOrdID());
        orderCancelReplaceRequest.setSide(executionReport.getSide());
        orderCancelReplaceRequest.setInstrument(executionReport.getInstrument());
        orderCancelReplaceRequest.setOrderQty(amount1);
        orderCancelReplaceRequest.setSecondaryClOrdID(executionReport.getSecondaryClOrdID());

        try {
            s = iGateway.sendMessage(orderCancelReplaceRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return s;
    }

    /**
     * 删除挂单(交易员调用)
     *
     * @param orderId   订单Id
     * @param secondary secondaryId
     * @return secondaryId
     */
    public String deleteEntryOrder(String orderId, String secondary) {
        ExecutionReport aExe = null;
        for (Map.Entry<String, OrderEntity> entry : openOrderMap.entrySet()) {
            if (entry.getValue().getMainOrder().getOrderID().equalsIgnoreCase(orderId)) {
                aExe = entry.getValue().getMainOrder();
            }
        }
        if (aExe == null) {
            return "wrong orderId";
        }
        String secon = aExe.getSecondaryClOrdID();
        OrderCancelRequest ocr = new OrderCancelRequest();
        ocr.setSecondaryClOrdID(secondary);
        ocr.setOrderID(aExe.getOrderID());
        ocr.setOrigClOrdID(aExe.getClOrdID());
        ocr.setSide(aExe.getSide());
        ocr.setInstrument(aExe.getInstrument());
        ocr.setAccount(aExe.getAccount());
        ocr.setOrderQty(aExe.getOrderQty());
        String s = null;
        try {
            s = iGateway.sendMessage(ocr);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return secon;
    }

    /**
     * 删除挂单（跟随者调用）
     *
     * @param secondary secondaryId，用于分辨是否为跟随跟随者下单
     * @return secondaryId
     */
    public String deleteEntryOrder(String secondary) {
        if (Strings.isNullOrEmpty(secondary)) {
            return null;
        }
        for (Map.Entry<String, OrderEntity> entry : openOrderMap.entrySet()) {
            if (entry.getValue() != null) {
                if (entry.getValue().getMainOrder() != null) {
                    if (entry.getValue().getMainOrder().getSecondaryClOrdID() != null) {
                        if (entry.getValue().getMainOrder().getSecondaryClOrdID().equalsIgnoreCase(secondary)) {
                            deleteEntryOrder(entry.getValue().getMainOrder().getOrderID(), secondary);
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
            } else {
                continue;
            }
        }
        return null;
    }

    /**
     * 取消所有挂单(交易员调用)
     *
     * @return
     */
    public List<String> deleteAllEntryOrders() {
        List<String> list = new LinkedList<>();
        for (Map.Entry<String, OrderEntity> entry : openOrderMap.entrySet()) {
            String s = deleteEntryOrder(entry.getValue().getMainOrder().getOrderID(), null);
            list.add(s);
            if (entry.getValue().getLimit() != null) {
                deleteEntryOrder(entry.getValue().getLimit().getOrderID(), null);
            }
            if (entry.getValue().getStop() != null) {
                deleteEntryOrder(entry.getValue().getStop().getOrderID(), null);
            }
        }
        return list;
    }

    /**
     * 删除所有挂单（跟随者调用）
     *
     * @param list secondary集合
     * @return
     */
    public List<String> deleteAllEntryOrders(List<String> list) {
        List<String> list1 = new LinkedList<>();
        for (Map.Entry<String, OrderEntity> entry : openOrderMap.entrySet()) {
            if (entry.getValue() != null) {
                if (entry.getValue().getMainOrder() != null) {
                    if (entry.getValue().getMainOrder().getSecondaryClOrdID() != null) {
                        if (list.contains(entry.getValue().getMainOrder().getSecondaryClOrdID())) {
                            String s = deleteEntryOrder(entry.getValue().getMainOrder().getOrderID(), null);
                            list.add(s);
                            if (entry.getValue().getLimit() != null) {
                                deleteEntryOrder(entry.getValue().getLimit().getOrderID(), null);
                            }
                            if (entry.getValue().getStop() != null) {
                                deleteEntryOrder(entry.getValue().getStop().getOrderID(), null);
                            }
                        }
                    }
                }
            }
        }
        return list;
    }

    /**
     * 为指定挂单添加止盈或者止损（交易员调用）
     *
     * @param orderId   需要修改的订单id
     * @param price     需要设置的价格
     * @param type      stop/limit
     * @param secondary 二级订单
     * @return
     */
    public String createStopLimitEntryOrder(String orderId, String price, String type, String secondary) {
        ExecutionReport executionReport = null;
        for (Map.Entry<String, OrderEntity> entry : openOrderMap.entrySet()) {
            if (entry.getValue().getMainOrder().getOrderID().equalsIgnoreCase(orderId)) {
                executionReport = entry.getValue().getMainOrder();
            }
        }
        if (executionReport == null) {
            return "wrong orderId or not exists";
        }
        double price1 = Double.parseDouble(price);
        IOrdType ordType = null;
        if ("limit".equalsIgnoreCase(type)) {
            ordType = OrdTypeFactory.LIMIT;
        } else if ("stop".equalsIgnoreCase(type)) {
            ordType = OrdTypeFactory.STOP;
        } else {
            return "wrong type";
        }

        secondary = executionReport.getSecondaryClOrdID();
        String s = null;
        try {
            OrderSingle orderSingle = MessageGenerator.generateStopLimitClose(
                    price1,
                    executionReport.getFXCMPosID(),
                    ordType,
                    executionReport.getAccount(),
                    executionReport.getOrderQty(),
                    executionReport.getSide(),
                    executionReport.getInstrument().getSymbol(),
                    executionReport.getSecondaryClOrdID());

            orderSingle.setSecondaryClOrdID(secondary);
            s = iGateway.sendMessage(orderSingle);
        } catch (NotDefinedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return secondary;

    }

    /**
     * 为跟随者挂单添加止盈/止损（跟随者调用）
     *
     * @param price     目标价格
     * @param type      订单类型：stop/limit
     * @param secondary secondaryId
     * @return secondaryId
     */
    public String createStopLimitEntryOrder(String price, String type, String secondary) {
        if (Strings.isNullOrEmpty(secondary)) {
            return null;
        }
        for (Map.Entry<String, OrderEntity> x : openOrderMap.entrySet()) {
            if (x.getValue() != null) {
                if (x.getValue().getMainOrder() != null) {
                    if (x.getValue().getMainOrder().getSecondaryClOrdID() != null) {
                        if (secondary.equalsIgnoreCase(x.getValue().getMainOrder().getSecondaryClOrdID())) {
                            createStopLimitEntryOrder(x.getValue().getMainOrder().getOrderID(), price, type, secondary);
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
            } else {
                continue;
            }
        }
        return null;
    }

    /**
     * 更新挂单的止盈/止损价格（交易员调用）
     *
     * @param orderId   订单Id
     * @param type      止盈？止损？（stop/limit）
     * @param price     止盈或者止损的价格
     * @param secondary secondaryId
     * @return secondaryId
     */
    public String updateStopLimitEntryOrder(String orderId, String type, String price, String secondary) {

        ExecutionReport aExe = null;
        if ("limit".equalsIgnoreCase(type)) {
            for (Map.Entry<String, OrderEntity> entry : openOrderMap.entrySet()) {
                if (entry.getValue() != null) {
                    if (entry.getValue().getLimit() != null) {

                        if (orderId.equalsIgnoreCase(entry.getValue().getLimit().getOrderID())) {
                            aExe = entry.getValue().getLimit();
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
            }
        } else if ("stop".equalsIgnoreCase(type)) {
            for (Map.Entry<String, OrderEntity> entry : openOrderMap.entrySet()) {
                if (entry.getValue() != null) {
                    if (entry.getValue().getStop() != null) {

                        if (orderId.equalsIgnoreCase(entry.getValue().getStop().getOrderID())) {
                            aExe = entry.getValue().getStop();
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
            }
        } else {
            return "wrong type";
        }
        if (aExe == null) {
            return "wrong orderId or not exists";
        }
        secondary = aExe.getSecondaryClOrdID();
        double price1 = Double.parseDouble(price);
        OrderCancelReplaceRequest os = MessageGenerator.generateOrderReplaceRequest(
                secondary,
                aExe.getOrderID(),
                aExe.getSide(),
                aExe.getOrdType(),
                price1,
                aExe.getAccount());
        os.setOrderID(aExe.getOrderID());
        os.setOrigClOrdID(aExe.getClOrdID());
        os.setSide(aExe.getSide());
        os.setInstrument(aExe.getInstrument());
        os.setOrderQty(aExe.getOrderQty());
        os.setSecondaryClOrdID(aExe.getSecondaryClOrdID());
        String a = null;
        try {
            a = iGateway.sendMessage(os);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return secondary;
    }

    /**
     * 更新挂单的止盈/止损价格（跟随者调用）
     *
     * @param type      止盈？止损？（stop/limit）
     * @param price     止盈/止损价格
     * @param secondary secondaryId
     * @return secondaryId
     */
    public String updateStopLimitEntryOrder(String type, String price, String secondary) {
        if (Strings.isNullOrEmpty(secondary)) {
            return null;
        }
        ExecutionReport aExe = null;
        if ("limit".equalsIgnoreCase(type)) {
            for (Map.Entry<String, OrderEntity> entry : openOrderMap.entrySet()) {
                if (entry.getValue() != null) {
                    if (entry.getValue().getLimit() != null) {
                        if (entry.getValue().getLimit().getSecondaryClOrdID() != null) {
                            if (secondary.equalsIgnoreCase(entry.getValue().getLimit().getSecondaryClOrdID())) {
                                aExe = entry.getValue().getLimit();
                            } else {
                                continue;
                            }
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
            }
        } else if ("stop".equalsIgnoreCase(type)) {
            for (Map.Entry<String, OrderEntity> entry : openOrderMap.entrySet()) {
                if (entry.getValue() != null) {
                    if (entry.getValue().getStop() != null) {
                        if (entry.getValue().getStop().getSecondaryClOrdID() != null) {
                            if (secondary.equalsIgnoreCase(entry.getValue().getStop().getSecondaryClOrdID())) {
                                aExe = entry.getValue().getStop();
                            } else {
                                continue;
                            }
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
            }
        } else {
            return "wrong type";
        }
        if (aExe == null) {
            return null;
        }
        String s = updateStopLimitEntryOrder(aExe.getOrderID(), type, price, secondary);
        return s;
    }

    /**
     * 删除挂单的止盈/止损（交易员调用）
     *
     * @param orderId   止盈/止损单的Id
     * @param type      止盈？止损？（stop/limit）
     * @param secondary secondaryId
     * @return secondaryId
     */
    public String deleteStopLimitEntryOrder(String orderId, String type, String secondary) {
        ExecutionReport aExe = null;
        if ("limit".equalsIgnoreCase(type)) {
            for (Map.Entry<String, OrderEntity> entry : openOrderMap.entrySet()) {
                if (entry.getValue() != null) {
                    if (entry.getValue().getLimit() != null) {
                        if (orderId.equalsIgnoreCase(entry.getValue().getLimit().getOrderID())) {
                            aExe = entry.getValue().getLimit();
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
            }
        } else if ("stop".equalsIgnoreCase(type)) {
            for (Map.Entry<String, OrderEntity> entry : openOrderMap.entrySet()) {
                if (entry.getValue() != null) {
                    if (entry.getValue().getStop() != null) {
                        if (orderId.equalsIgnoreCase(entry.getValue().getStop().getOrderID())) {
                            aExe = entry.getValue().getStop();
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
            }
        } else {
            return "wrong type";
        }
        if (aExe == null) {
            return "wrong orderId or not exists";
        }
        secondary = aExe.getSecondaryClOrdID();

        OrderCancelRequest ocr = MessageGenerator.generateOrderCancelRequest(
                secondary,
                aExe.getOrderID(),
                aExe.getSide(),
                aExe.getAccount());
        ocr.setOrderID(aExe.getOrderID());
        ocr.setOrigClOrdID(aExe.getClOrdID());
        ocr.setSide(aExe.getSide());
        ocr.setInstrument(aExe.getInstrument());
        ocr.setAccount(aExe.getAccount());
        ocr.setOrderQty(aExe.getOrderQty());
        String s = null;
        try {
            s = iGateway.sendMessage(ocr);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return secondary;
    }

    /**
     * 删除挂单的止盈/止损（跟随者调用）
     *
     * @param type      止盈？止损？（stop/limit）
     * @param secondary secondaryId
     * @return secondaryId
     */
    public String deleteStopLimitEntryOrder(String type, String secondary) {
        if (Strings.isNullOrEmpty(secondary)) {
            return null;
        }
        ExecutionReport aExe = null;
        if ("limit".equalsIgnoreCase(type)) {
            for (Map.Entry<String, OrderEntity> entry : openOrderMap.entrySet()) {
                if (entry.getValue() != null) {
                    if (entry.getValue().getLimit() != null) {
                        if (entry.getValue().getLimit().getSecondaryClOrdID() != null) {
                            if (secondary.equalsIgnoreCase(entry.getValue().getLimit().getSecondaryClOrdID())) {
                                aExe = entry.getValue().getLimit();
                            } else {
                                continue;
                            }
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
            }
        } else if ("stop".equalsIgnoreCase(type)) {
            for (Map.Entry<String, OrderEntity> entry : openOrderMap.entrySet()) {
                if (entry.getValue() != null) {
                    if (entry.getValue().getStop() != null) {
                        if (entry.getValue().getStop().getSecondaryClOrdID() != null) {
                            if (secondary.equalsIgnoreCase(entry.getValue().getStop().getSecondaryClOrdID())) {
                                aExe = entry.getValue().getStop();
                            } else {
                                continue;
                            }
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
            }
        } else {
            return "wrong type";
        }
        if (aExe == null) {
            return null;
        }
        String s = deleteStopLimitEntryOrder(aExe.getOrderID(), type, secondary);

        return s;
    }

    /**
     * 为指定仓位添加止盈或止损（交易员调用）
     *
     * @param fxcmPosId 需要设置止盈/止损的仓位ID
     * @param type      止盈？止损？（stop/limit）
     * @param price     价格
     * @param secondary secondaryId（不需要）
     * @return secondaryId
     */
    public String createStopLimitMarketOrder(String fxcmPosId, String type, String price, String secondary) {
        PositionReport positionReport = null;
        if (openPositionsMap.get(fxcmPosId) == null) {
            return null;
        }
        positionReport = openPositionsMap.get(fxcmPosId).getPosition();
        if (positionReport == null) {
            return null;
        }
        IOrdType ordType = null;
        if ("stop".equalsIgnoreCase(type)) {
            ordType = OrdTypeFactory.STOP;
        } else if ("limit".equalsIgnoreCase(type)) {
            ordType = OrdTypeFactory.LIMIT;
        }
        double price1 = Double.parseDouble(price);
        String s = null;
        secondary = positionReport.getSecondaryClOrdID();
        try {
            OrderSingle order = MessageGenerator.generateStopLimitClose(
                    price1,
                    positionReport.getFXCMPosID(),
                    ordType,
                    positionReport.getAccount(),
                    positionReport.getPositionQty().getQty(),
                    positionReport.getPositionQty().getSide(),
                    positionReport.getInstrument().getSymbol(),
                    secondary);
            s = iGateway.sendMessage(order);
        } catch (NotDefinedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return secondary;
    }

    /**
     * 为指定仓位添加止盈/止损（跟随者调用）
     *
     * @param type      止盈/止损
     * @param price     价格
     * @param secondary secondaryId
     * @return secondaryId
     */
    public String createStopLimitMarketOrder(String type, String price, String secondary) {
        if (Strings.isNullOrEmpty(secondary)) {
            return null;
        }
        PositionReport positionReport = null;

        for (Map.Entry<String, OpenPositionEntity> entry : openPositionsMap.entrySet()) {
            if (entry.getValue() != null) {
                if (entry.getValue().getPosition() != null) {
                    if (entry.getValue().getPosition().getSecondaryClOrdID() != null) {
                        if (secondary.equalsIgnoreCase(entry.getValue().getPosition().getSecondaryClOrdID())) {
                            positionReport = entry.getValue().getPosition();
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
            } else {
                continue;
            }
        }
        IOrdType ordType = null;
        if ("stop".equalsIgnoreCase(type)) {
            ordType = OrdTypeFactory.STOP;
        } else if ("limit".equalsIgnoreCase(type)) {
            ordType = OrdTypeFactory.LIMIT;
        }
        double price1 = Double.parseDouble(price);
        String s = null;
        if (positionReport == null) {
            return null;
        }
        secondary = positionReport.getSecondaryClOrdID();
        try {
            OrderSingle order = MessageGenerator.generateStopLimitClose(
                    price1,
                    positionReport.getFXCMPosID(),
                    ordType,
                    positionReport.getAccount(),
                    positionReport.getPositionQty().getQty(),
                    positionReport.getPositionQty().getSide(),
                    positionReport.getInstrument().getSymbol(),
                    secondary);
            s = iGateway.sendMessage(order);
        } catch (NotDefinedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return secondary;
    }

    /**
     * 修改指定仓位的止盈/止损价格(交易员调用)
     *
     * @param orderId   要修改的止盈/止损挂单ID
     * @param type      止盈/止损 （stop/limit）
     * @param price     价格
     * @param secondary secondaryId
     * @return secondaryId
     */
    public String updateStopLimitMarketOrder(String orderId, String type, String price, String secondary) {

        ExecutionReport aExe = null;
        if ("limit".equalsIgnoreCase(type)) {
            for (Map.Entry<String, OpenPositionEntity> entry : openPositionsMap.entrySet()) {
                if (entry.getValue() != null) {
                    if (entry.getValue().getLimit() != null) {
                        if (entry.getValue().getLimit().getOrderID() != null) {
                            if (orderId.equalsIgnoreCase(entry.getValue().getLimit().getOrderID())) {
                                aExe = entry.getValue().getLimit();
                            } else {
                                continue;
                            }
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }

            }
        } else if ("stop".equalsIgnoreCase(type)) {
            for (Map.Entry<String, OpenPositionEntity> entry : openPositionsMap.entrySet()) {
                if (entry.getValue() != null) {
                    if (entry.getValue().getStop() != null) {
                        if (entry.getValue().getStop().getOrderID() != null) {
                            if (orderId.equalsIgnoreCase(entry.getValue().getStop().getOrderID())) {
                                aExe = entry.getValue().getStop();
                            } else {
                                continue;
                            }
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
            }
        } else {
            return "wrong type";
        }
        if (aExe == null) {
            return "wrong orderId or not exists";
        }
        secondary = aExe.getSecondaryClOrdID();
        double price1 = Double.parseDouble(price);
        OrderCancelReplaceRequest os = MessageGenerator.generateOrderReplaceRequest(
                secondary,
                aExe.getOrderID(),
                aExe.getSide(),
                aExe.getOrdType(),
                price1,
                aExe.getAccount());
        os.setOrderID(aExe.getOrderID());
        os.setOrigClOrdID(aExe.getClOrdID());
        os.setSide(aExe.getSide());
        os.setInstrument(aExe.getInstrument());
        os.setOrderQty(aExe.getOrderQty());
        os.setSecondaryClOrdID(aExe.getSecondaryClOrdID());
        String a = null;
        try {
            a = iGateway.sendMessage(os);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return secondary;
    }

    /**
     * 修改指定仓位的止盈/止损价格（跟随者调用）
     *
     * @param type      止盈/止损（stop/limit）
     * @param price     价格
     * @param secondary secondaryId
     * @return secondaryId
     */
    public String updateStopLimitMarketOrder(String type, String price, String secondary) {
        if (Strings.isNullOrEmpty(secondary)) {
            return null;
        }
        ExecutionReport aExe = null;
        if ("limit".equalsIgnoreCase(type)) {
            for (Map.Entry<String, OpenPositionEntity> entry : openPositionsMap.entrySet()) {
                if (entry.getValue() != null) {
                    if (entry.getValue().getLimit() != null) {
                        if (entry.getValue().getLimit().getSecondaryClOrdID() != null) {
                            if (secondary.equalsIgnoreCase(entry.getValue().getLimit().getSecondaryClOrdID())) {
                                aExe = entry.getValue().getLimit();
                            } else {
                                continue;
                            }
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
            }
        } else if ("stop".equalsIgnoreCase(type)) {
            for (Map.Entry<String, OpenPositionEntity> entry : openPositionsMap.entrySet()) {
                if (entry.getValue() != null) {
                    if (entry.getValue().getStop() != null) {
                        if (entry.getValue().getStop().getSecondaryClOrdID() != null) {
                            if (secondary.equalsIgnoreCase(entry.getValue().getStop().getSecondaryClOrdID())) {
                                aExe = entry.getValue().getStop();
                            } else {
                                continue;
                            }
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
            }
        } else {
            return "wrong type";
        }
        if (aExe == null) {
            return null;
        }
        secondary = aExe.getSecondaryClOrdID();
        double price1 = Double.parseDouble(price);
        OrderCancelReplaceRequest os = MessageGenerator.generateOrderReplaceRequest(
                secondary,
                aExe.getOrderID(),
                aExe.getSide(),
                aExe.getOrdType(),
                price1,
                aExe.getAccount());
        os.setOrderID(aExe.getOrderID());
        os.setOrigClOrdID(aExe.getClOrdID());
        os.setSide(aExe.getSide());
        os.setInstrument(aExe.getInstrument());
        os.setOrderQty(aExe.getOrderQty());
        os.setSecondaryClOrdID(aExe.getSecondaryClOrdID());
        String a = null;
        try {
            a = iGateway.sendMessage(os);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return secondary;
    }

    /**
     * 删除仓位指定的止盈/止损挂单（交易员调用）
     *
     * @param orderId   要删除的止盈/止损挂单Id
     * @param type      止盈/止损（stop/limit）
     * @param secondary secondaryId
     * @return secondaryId
     */
    public String deleteStopLimitMarketOrder(String orderId, String type, String secondary) {
        ExecutionReport aExe = null;
        if ("limit".equalsIgnoreCase(type)) {
            for (Map.Entry<String, OpenPositionEntity> entry : openPositionsMap.entrySet()) {
                if (entry.getValue() != null) {
                    if (entry.getValue().getLimit() != null) {
                        if (orderId.equalsIgnoreCase(entry.getValue().getLimit().getOrderID())) {
                            aExe = entry.getValue().getLimit();
                            System.out.println("aexe=====" + aExe);
                            break;
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
            }
        } else if ("stop".equalsIgnoreCase(type)) {
            for (Map.Entry<String, OpenPositionEntity> entry : openPositionsMap.entrySet()) {
                if (entry.getValue() != null) {
                    if (entry.getValue().getStop() != null) {
                        if (orderId.equalsIgnoreCase(entry.getValue().getStop().getOrderID())) {
                            aExe = entry.getValue().getStop();
                            break;
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
            }
        } else {
            return "wrong type";
        }
        if (aExe == null) {
            return "wrong order id or not exists";
        }
        secondary = aExe.getSecondaryClOrdID();
        OrderCancelRequest ocr = MessageGenerator.generateOrderCancelRequest(
                secondary,
                aExe.getOrderID(),
                aExe.getSide(),
                aExe.getAccount());
        ocr.setOrderID(aExe.getOrderID());
        ocr.setOrigClOrdID(aExe.getClOrdID());
        ocr.setSide(aExe.getSide());
        ocr.setInstrument(aExe.getInstrument());
        ocr.setAccount(aExe.getAccount());
        ocr.setOrderQty(aExe.getOrderQty());
        String s = null;
        try {
            s = iGateway.sendMessage(ocr);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return secondary;
    }

    /**
     * 删除仓位指定的止盈/止损挂单（跟随者调用）
     *
     * @param type      止盈/止损（stop/limit）
     * @param secondary secondaryId
     * @return secondaryId
     */
    public String deleteStopLimitMarketOrder(String type, String secondary) {
        if (Strings.isNullOrEmpty(secondary)) {
            return null;
        }
        ExecutionReport aExe = null;
        if ("limit".equalsIgnoreCase(type)) {
            for (Map.Entry<String, OpenPositionEntity> entry : openPositionsMap.entrySet()) {
                if (entry.getValue() != null) {
                    if (entry.getValue().getLimit() != null) {
                        if (entry.getValue().getLimit().getSecondaryClOrdID() != null) {
                            if (secondary.equalsIgnoreCase(entry.getValue().getLimit().getSecondaryClOrdID())) {
                                aExe = entry.getValue().getLimit();
                            } else {
                                continue;
                            }
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
            }
        } else if ("stop".equalsIgnoreCase(type)) {
            for (Map.Entry<String, OpenPositionEntity> entry : openPositionsMap.entrySet()) {
                if (entry.getValue() != null) {
                    if (entry.getValue().getStop() != null) {
                        if (entry.getValue().getStop().getSecondaryClOrdID() != null) {
                            if (secondary.equalsIgnoreCase(entry.getValue().getStop().getSecondaryClOrdID())) {
                                aExe = entry.getValue().getStop();
                            } else {
                                continue;
                            }
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
            }
        } else {
            return "wrong type";
        }
        if (aExe == null) {
            return null;
        }
        secondary = aExe.getSecondaryClOrdID();
        OrderCancelRequest ocr = MessageGenerator.generateOrderCancelRequest(
                secondary,
                aExe.getOrderID(),
                aExe.getSide(),
                aExe.getAccount());
        ocr.setOrderID(aExe.getOrderID());
        ocr.setOrigClOrdID(aExe.getClOrdID());
        ocr.setSide(aExe.getSide());
        ocr.setInstrument(aExe.getInstrument());
        ocr.setAccount(aExe.getAccount());
        ocr.setOrderQty(aExe.getOrderQty());
        String s = null;
        try {
            s = iGateway.sendMessage(ocr);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return secondary;
    }

    /**
     * 修改fxcm账号密码
     *
     * @param account     fxcm登录账号
     * @param password    原密码
     * @param newPassword 新密码
     * @return
     */
    public String changePassword(String account, String password, String newPassword) {
        //修改密码
        UserRequest userRequest = new UserRequest();
        userRequest.setUserRequestType(IFixValueDefs.USERREQUESTTYPE_CHANGEPASSWORD);
        userRequest.setUsername(account);
        userRequest.setPassword(password);
        userRequest.setNewPassword(newPassword);
        try {
            changePassword = iGateway.sendMessage(userRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return changePassword;
    }

    public String getStatus(String requestId) {
        this.requestId = iGateway.requestOrderStatus(requestId, OrdStatusRequestType.CLORDID, collateralReport.getAccount());
        return this.requestId;
    }

    public String getOpenOrders() {
        rId = iGateway.requestOpenOrders();
        return rId;
    }

    public LinkedList<ExecutionReport> getOrderExecutionReport(String listId) {
        if (Strings.isNullOrEmpty(listId)) {
            return null;
        }
        System.out.println("size::::" + orderHistory.size());
        return orderHistory.get(listId);
    }
}
