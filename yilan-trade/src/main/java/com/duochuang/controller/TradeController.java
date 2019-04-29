/***********************************************
 * File Name: IndexController
 * Author: caoguobin
 * mail: caoguobin@live.com
 * Created Time: 27 02 2019 11:13
 ***********************************************/

package com.duochuang.controller;


import com.duochuang.entity.OpenPositionEntity;
import com.duochuang.entity.OrderEntity;
import com.duochuang.service.TradeService;
import com.duochuang.vo.JsonResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fxcm.fix.posttrade.ClosedPositionReport;
import com.fxcm.fix.pretrade.MarketDataSnapshot;
import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Pattern;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Controller
@RequestMapping("/trade")
public class TradeController {
    private ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private TradeService tradeService;


    @RequestMapping("/index")
    public String index(String path) {
        return path;
    }

    /**
     * 登录福汇账
     */
    @RequestMapping(value = "/loginFXCM", method = RequestMethod.POST)
    @ResponseBody
    public JsonResult loginFXCM() {
        String loginResult = tradeService.loginFXCM();
        if ("OK".equalsIgnoreCase(loginResult)) {
            return new JsonResult(loginResult);
        } else {
            return new JsonResult("400", "login failed", null);
        }
    }

    @RequestMapping("/getMarketDataSnapshot")
    @ResponseBody
    public String getMarketDataSnapshot(HttpServletRequest request, HttpServletResponse response) {
        String callback = request.getParameter("callback");
        response.setHeader("Access-Control-Allow-Origin", "*");
        Map<String, MarketDataSnapshot> marketDatas = tradeService.getMarketDataSnapshot();
        if (marketDatas == null) {
            return null;
        }
        String result = null;
        try {
            result = objectMapper.writeValueAsString(marketDatas);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return callback + "(" + result + ")";
    }

    private JsonResult getJsonResult(String marketDatas) {
        Map map = null;
        try {
            map = objectMapper.readValue(marketDatas, Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new JsonResult(map);
    }


    /**
     * 创建市价单
     *
     * @param currency    货币符号
     * @param tradeSide   交易方向
     * @param tradeAmount 交易数量
     * @param tradeStop   止损价格（不设置则传0）
     * @param tradeLimit  止盈价格（不设置则传0）
     * @return 查询id
     */
    @RequestMapping(value = "createMarketOrder", method = RequestMethod.POST)
    @ResponseBody
    public JsonResult createTrueMarketOrder(String userToken, String fxcmAccount, String currency, String tradeSide, String tradeAmount, String tradeStop, String tradeLimit) {

        if (!isDoubleOrFloat(tradeAmount, tradeStop, tradeLimit)) {
            return new JsonResult("401", "wrong params", null);
        }
        String userId = userToken;

        String result = tradeService.createMarketOrder(currency, tradeSide, tradeAmount, tradeStop, tradeLimit);

        return new JsonResult(result);

    }

    /**
     * 为指定仓位添加止盈或止损
     *
     * @param fxcmPosId 需要设置止盈/止损的仓位ID
     * @param type      止盈？止损？（stop/limit）
     * @param price     价格
     * @return secondaryId
     */
    @RequestMapping(value = "/createSLMarketOrder", method = RequestMethod.POST)
    @ResponseBody
    public JsonResult createStopLimitMarketOrder(String userToken, String fxcmAccount, String fxcmPosId, String type, String price) {
        if (!isDoubleOrFloat(price)) {
            return new JsonResult("401", "wrong params", null);
        }
        String userId = userToken;
        String result = tradeService.createSLMarketOrder(fxcmPosId, type, price);
        //TODO
        return new JsonResult(result);
    }


    /**
     * 修改指定仓位的止盈/止损价格
     *
     * @param orderId 要修改的止盈/止损挂单ID
     * @param type    止盈/止损 （stop/limit）
     * @param price   价格
     * @return secondaryId
     */
    @RequestMapping(value = "/updateSLMarketOrder", method = RequestMethod.POST)
    @ResponseBody
    public JsonResult updateStopLimitMarketOrder(String userToken, String fxcmAccount, String orderId, String type, String price) {
        if (!isDoubleOrFloat(price)) {
            return new JsonResult("401", "wrong params", null);
        }
        String userId = userToken;

        String result = tradeService.updateSLMarketOrder(orderId, type, price);
        return new JsonResult(result);
    }

    /**
     * 删除仓位指定的止盈/止损挂单
     *
     * @param orderId 要删除的止盈/止损挂单Id
     * @param type    止盈/止损（stop/limit）
     * @return secondaryId
     */
    @RequestMapping(value = "/deleteSLMarketOrder", method = RequestMethod.POST)
    @ResponseBody
    public JsonResult deleteStopLimitMarketOrder(String userToken, String fxcmAccount, String orderId, String type) {
        String userId = userToken;

        String result = tradeService.deleteSLMarketOrder(orderId, type);
        return new JsonResult(result);
    }

    /**
     * 市价单平仓
     *
     * @param fxcmPosID 需要平仓的positionId
     * @return
     */
    @RequestMapping(value = "/deleteMarketOrder", method = RequestMethod.POST)
    @ResponseBody
    public JsonResult deleteTrueMarketOrder(String userToken, String fxcmAccount, String fxcmPosID) {
        String userId = userToken;

        String result = tradeService.deleteMarketOrder(fxcmPosID);
        return new JsonResult(result);
    }

    /**
     * 关闭所有开仓位置(交易员调用)
     *
     * @return
     */
    @RequestMapping(value = "/deleteAllOpenPositions", method = RequestMethod.POST)
    @ResponseBody
    public JsonResult deleteAllOpenPositions(String userToken, String fxcmAccount) {
        String userId = userToken;

        String result = tradeService.deleteAllOpenPositions();
        return new JsonResult(result);

    }


    /**
     * 创建挂单
     *
     * @param price    挂单价格
     * @param type     挂单方式
     * @param amount   挂单数量
     * @param side     交易方向
     * @param currency 交易货币
     * @param stop     止损价格（未设置传0）
     * @param limit    止盈价格（未设置传0）
     * @return secondaryId
     */
    @RequestMapping(value = "createEntryOrder", method = RequestMethod.POST)
    @ResponseBody
    public JsonResult createEntryOrder(String userToken, String fxcmAccount, String price, String type, String amount, String side, String currency, String stop, String limit) {
        if (!isDoubleOrFloat(price, amount, stop, limit)) {
            return new JsonResult("401", "wrong params", null);
        }
        String userId = userToken;

        String result = tradeService.createEntryOrder(price, type, amount, side, currency, stop, limit);
        return new JsonResult(result);
    }

    /**
     * 更改挂单价格（交易员调用）
     *
     * @param orderId 要更改的订单id
     * @param price   要更改的价格
     * @return secondaryId
     */
    @RequestMapping(value = "updateEntryOrder", method = RequestMethod.POST)
    @ResponseBody
    public JsonResult updateEntryOrder(String userToken, String fxcmAccount, String orderId, String amount, String price) {
        if (!isDoubleOrFloat(price, amount)) {
            return new JsonResult("401", "wrong params", null);
        }
        String userId = userToken;

        String result = tradeService.updateEntryOrder(orderId, amount, price);
        return new JsonResult(result);
    }

    /**
     * 删除挂单(交易员调用)
     *
     * @param orderId 订单Id
     * @return secondaryId
     */
    @RequestMapping(value = "deleteEntryOrder", method = RequestMethod.POST)
    @ResponseBody
    public JsonResult deleteEntryOrder(String userToken, String fxcmAccount, String orderId) {
        String userId = userToken;

        String result = tradeService.deleteEntryOrder(orderId);
        return new JsonResult(result);
    }

    /**
     * 取消所有挂单(交易员调用)
     *
     * @return
     */
    @RequestMapping(value = "/deleteAllEntryOrders", method = RequestMethod.POST)
    @ResponseBody
    public JsonResult deleteAllEntryOrders(String userToken, String fxcmAccount) {
        String userId = userToken;

        String result = tradeService.deleteAllEntryOrders();
        return new JsonResult(result);
    }

    /**
     * 为指定挂单添加止盈或者止损（交易员调用）
     *
     * @param orderId 需要修改的订单id
     * @param price   需要设置的价格
     * @param type    stop/limit
     * @return
     */
    @RequestMapping(value = "/createSLEntryOrder", method = RequestMethod.POST)
    @ResponseBody
    public JsonResult createSLEntryOrder(String userToken, String fxcmAccount, String orderId, String price, String type) {
        if (!isDoubleOrFloat(price)) {
            return new JsonResult("401", "wrong params", null);
        }
        String userId = userToken;

        String result = tradeService.createSLEntryOrder(orderId, price, type);
        return new JsonResult(result);
    }

    /**
     * 更新挂单的止盈/止损价格（交易员调用）
     *
     * @param orderId 订单Id
     * @param type    止盈？止损？（stop/limit）
     * @param price   止盈或者止损的价格
     * @return secondaryId
     */
    @RequestMapping(value = "/updateSLEntryOrder", method = RequestMethod.POST)
    @ResponseBody
    public JsonResult updateSLEntryOrder(String userToken, String fxcmAccount, String orderId, String type, String price) {
        if (!isDoubleOrFloat(price)) {
            return new JsonResult("401", "wrong params", null);
        }
        String userId = userToken;
        String result = tradeService.updateSLEntryOrder(orderId, type, price);
        return new JsonResult(result);
    }

    /**
     * 删除挂单的止盈/止损（交易员调用）
     *
     * @param orderId 止盈/止损单的Id
     * @param type    止盈？止损？（stop/limit）
     * @return secondaryId
     */
    @RequestMapping(value = "/deleteSLEntryOrder", method = RequestMethod.POST)
    @ResponseBody
    public JsonResult deleteSLEntryOrder(String userToken, String fxcmAccount, String orderId, String type) {
        String userId = userToken;
        String result = tradeService.deleteSLEntryOrder(orderId, type);
        return new JsonResult(result);
    }


    /**
     * 修改fxcm账号密码
     *
     * @param fxcmAccount fxcm登录账号
     * @param password    原密码
     * @param newPassword 新密码
     * @return
     */
    @RequestMapping(value = "/changeFXCMPassword", method = RequestMethod.POST)
    @ResponseBody
    public JsonResult changePassword(String userToken, String fxcmAccount, String password, String newPassword) {
        String userId = userToken;
        String result = tradeService.changeFXCMPassword(password, newPassword);
        return new JsonResult(result);
    }


    /**
     * 获取开仓位置
     *
     * @return 开仓位置
     */
    @RequestMapping(value = "/getOpenPositions", method = RequestMethod.POST)
    @ResponseBody
    public JsonResult getOpenPositions() {
        Map<String, Map<String, OpenPositionEntity>> result = tradeService.getOpenPositions();
        return new JsonResult(result);
    }

    /**
     * 获取所有挂单信息
     *
     * @return 所有挂单信息
     */
    @RequestMapping(value = "/getOpenOrders", method = RequestMethod.POST)
    @ResponseBody
    public JsonResult getOpenOrders() {
        Map<String, Map<String, OrderEntity>> openOrders = tradeService.getOpenOrders();
        return new JsonResult(openOrders);
    }

    /**
     * 获取所有已关闭仓位信息
     * @return 所有已关闭仓位
     */
    @RequestMapping(value = "/getClosedPositions", method = RequestMethod.POST)
    @ResponseBody
    public JsonResult getClosedPositions() {
        Map<String, Map<String, ClosedPositionReport>> closedPositions = tradeService.getClosedPositions();
        return new JsonResult(closedPositions);
    }

    /**
     * 获取保证金报告
     *
     * @param userToken   userToken
     * @param fxcmAccount 福汇账号
     * @return
     */
    @RequestMapping(value = "/getCollateralReport", method = RequestMethod.POST)
    @ResponseBody
    public JsonResult getCollateralReport(String userToken, String fxcmAccount) {
        String userId = userToken;
        String result = tradeService.getCollateralReport();
        return getJsonResult(result);
    }


    /**
     * 登出福汇账号
     *
     * @param userToken   userToken
     * @param fxcmAccount 福汇账号
     * @return logout result
     */
    @RequestMapping(value = "/logoutFXCM", method = RequestMethod.POST)
    @ResponseBody
    public JsonResult logoutFXCM(String userToken, String fxcmAccount) {
        String userId = userToken;
        String result = tradeService.logoutFXCM();
        return new JsonResult(result);
    }

    /**
     * 注册福汇虚拟账号
     *
     * @param firstName firstName
     * @param lastName  lastName
     * @param email     电子邮箱
     * @param mobile    手机号
     * @return 注册结果：虚拟账号和密码
     */
    @RequestMapping(value = "registerFXCMDemo", method = RequestMethod.POST)
    @ResponseBody
    public JsonResult registerFXCMDemo(String firstName, String lastName, String email, String mobile) {
        long callback = new Date().getTime();
        String result = null;
        try {
            URL url = null;
            url = new URL("https://secure4.fxcorporate.com/tr-demo/form/service/?callback=jQuery" + callback + "&format=jsonp&demo.firstname=" + firstName + "&demo.lastname=" + lastName + "&demo.email_address=" + email + "&demo.phone=%2B86" + mobile + "&demo.country=china&rb=fxcmau_zh_standard&DB=CFDDEMO01&elqFormName=fxcmau_zh_standard&demo.email_me_training_material=yes&demo.consent_sms=yes&elqSiteID=202&_=" + new Date().getTime());
            InputStream inputStream = null;
            inputStream = url.openStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String a = null;
            while ((a = bufferedReader.readLine()) != null) {
                result = a;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        result = result.substring(result.indexOf("(") + 1, result.indexOf(")"));
        Map map = null;
        try {
            map = objectMapper.readValue(result, Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (map.get("username") != null) {
            map.remove("webtsurl");
            map.remove("errors");
        }
        return new JsonResult(map);
    }

    @RequestMapping(value = "/getOrderExecutionReport")
    @ResponseBody
    public JsonResult getOrderExecutionReport(String userToken, String fxcmAccount, String listId) {
        String orderExecutionReport = tradeService.getOrderExecutionReport(listId);
        if (Strings.isNullOrEmpty(orderExecutionReport)) {
            return new JsonResult(null);
        }
        try {
            LinkedList linkedList = objectMapper.readValue(orderExecutionReport, LinkedList.class);
            return new JsonResult(linkedList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean isDoubleOrFloat(String... args) {
        boolean flag = true;
        Pattern pattern = Pattern.compile("^[-\\+]?[.\\d]*$");
        for (String s : args) {
            if (Strings.isNullOrEmpty(s) || !pattern.matcher(s).matches()) {
                flag = false;
                break;
            }
        }
        return flag;
    }


}
