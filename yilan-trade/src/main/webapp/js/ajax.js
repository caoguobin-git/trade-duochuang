// JavaScript ajax
//请求


function doSetMarketOrder() {
    var symbol = $("#marketOrder .symbol").val();
    symbol = symbol.replace("/", "");
    var side;
    var amount = $("#marketOrder .amount").val();
    side = $('input[name="side"]:checked').val();
    if ("sell" == side) {

        var rate = $("#" + symbol).data("market").bidOpen;
        $(".stop_rate").html(">")
        $(".limit_rate").html("<")
        $("#marketOrder .rate").val(rate)
    } else if ("buy" == side) {
        rate = $("#" + symbol).data("market").askOpen;
        $(".stop_rate").html("<")
        $(".limit_rate").html(">")
        $("#marketOrder .rate").val(rate)
    }
}

function doSomeThing() {
    var id = this.id;
    var symbol = $("#" + id).data("market").instrument.symbol;
    $("#marketOrder .symbol").val(symbol)
}


function getMarketDataSnapshot() {
    var url = "http://192.168.18.8/trade/getMarketDataSnapshot.do"
    $.ajax({
        url: url,
        type: "get",
        dataType: "jsonP",
        success: function (result) {
            $("#allData").data("marketData", result)
            doCreatRows(result)
        }
    })

}

function getClosedPositions() {
    var url = "/trade/getClosedPositions.do";
    $.ajax({
        url: url,
        dataType: "json",
        type: "post",
        success: function (result) {
            $("#allData").data("closedPosition", result.data)
        }
    })
}


function getOpenOrders() {
    var url = "/trade/getOpenOrders.do";
    $.ajax({
        url: url,
        dataType: "json",
        type: "post",
        success: function (result) {
            $("#allData").data("openOrder", result.data)
        }
    })
}

function getOpenPositions() {
    var url = "/trade/getOpenPositions.do"
    $.ajax({
        url: url,
        dataType: "json",
        type: "post",
        success: function (result) {
            // console.log(result.data)
            doCreateAccountRows(result.data);
            $("#allData").data("openPosition", result.data)
        }
    })
}


//统一设置各状态信息
function doSetOptions() {
    var id = $("#cont_select .select_list").val();
    var collateral = $("#allData").data("collateralReport");
    var open = $("#allData").data("openPosition");
    var openOrder = $("#allData").data("openOrder");
    var close = $("#allData").data("closedPosition");
    setOpenPostionsRows(open[id])
    setOpenOrderRows(openOrder[id])
    setCollateralReportRows(collateral[id])
    setClosedPostionsRows(close[id])
}

function setOpenOrderRows(positions) {
    var table = $("#order_list");
    table.empty();
    $.each(positions, function (key, val) {
        var tr = $(
            "<div ><div>" + val.mainOrder.orderID + "</div>" +
            "<div>" + val.mainOrder.account + "</div>" +
            "<div >" + val.mainOrder.fxcmordType.code + "</div>" +
            "<div>" + val.mainOrder.fxcmordStatus.label + "</div>" +
            "<div>" + val.mainOrder.instrument.symbol + "</div>" +
            "<div>" + val.mainOrder.leavesQty + "</div><div>"
        )
        table.append(tr);
    })
}

function setCollateralReportRows(positions) {
    var table = $("#account_info");
    table.empty();
    var val = positions;
    var tr = $("<div class='account_account' style='display: inline-block;margin-left: 10px'>" + val.account + "</div>" +
        "<div class='account_cashOutstanding' style='display: inline-block;margin-left: 10px'>" + val.cashOutstanding + "</div>" +
        "<div class='account_margin' style='display: inline-block;margin-left: 10px'>" + val.fxcmusedMargin + "</div>" +
        "<div class='account_margin_left' style='display: inline-block;margin-left: 10px'>" + (val.cashOutstanding - val.fxcmusedMargin) + "</div>")
    table.append(tr);
}

function setClosedPostionsRows(positions) {
    var table = $("#closed_list");
    table.empty();
    console.log(positions)
    $.each(positions, function (key, val) {
        var tr = (
            "<div>" + val.fxcmposID + "</div>" +
            "<div>" + val.account + "</div>" +
            "<div>" + val.instrument.symbol + "</div>" +
            "<div>" + val.positionQty.qty + "</div>" +
            "<div>" + val.positionQty.side.desc + "</div>" +
            "<div>" + val.settlPrice + "</div>" +
            "<div>" + val.fxcmcloseSettlPrice + "</div>" +
            "<div>" + jisuan(val.settlPrice, val.fxcmcloseSettlPrice, val.positionQty.side.code, val.instrument.fxcmsymPointSize).toFixed(1) + "</div>" +
            "<div>" + val.fxcmposInterest + "</div>" +
            "<br/>"
        )
        table.append(tr);
    })

    function jisuan(a, b, c, d) {
        var result = 0;
        if (c == 1) {
            result = b - a;

        } else {
            result = a - b;
        }
        return result / d;
    }
}

function setOpenPostionsRows(positions) {
    var table = $("#open_list");
    table.empty();
    $.each(positions, function (key, val) {

        var tr = $("<div>" +
            "<div>" + key + "</div>" +
            "<div>" + val.position.account + "</div>" +
            "<div>" + val.position.instrument.symbol + "</div>" +
            "<div>" + val.position.positionQty.qty + "</div>" +
            "<div>" + val.position.positionQty.side.desc + "</div>" +
            "<div>" + val.position.settlPrice + "</div>" +
            "<div>" + (val.stop == null ? "" : val.stop.price) + "</div>" +
            "<div>" + (val.limit == null ? "" : val.limit.price) + "</div>" +
            "<div>" + val.position.fxcmusedMargin + "</div>" +
            "<div>" + getDateTime(val.position.fxcmposOpenTime.time) + "</div>" +
            "</div>")
        table.append(tr);
    })
}

function getDateTime(a) {
    var date = new Date(a);
    var result = "";
    result += date.getFullYear() + "/";
    result += date.getMonth() + 1;
    result += "/" + date.getDate() + "  ";
    result += date.getHours() + ":";
    result += date.getMinutes();
    return result;
}

function doCreateAccountRows(openPositionMap) {
    var select = $("#cont_select .select_list");
    $.each(openPositionMap, function (key, item) {
        if (isOptionExist(select, key)) {
        } else {
            select.append("<option value='" + key + "'>" + key + "</option>")
        }
    })
}

function isOptionExist(select, key) {
    var isExist = false;
    for (var i = 0; i < select[0].length; i++) {
        if (select[0][i].value == key) {
            isExist = true;
            break;
        }
    }
    return isExist;
}

function deleteSLEntryOrder() {
    var url = "/trade/deleteSLEntryOrder.do";
    var param = {
        "userToken": "247F1A35FCA49D6443B489951AA1B877",
        "fxcmAccount": 701116547,
        "orderId": 104563467,
        "type": "limit"
    }
    sendFxcmRequest(url, param)
}

function updateSLEntryOrder() {
    var url = "/trade/updateSLEntryOrder.do";
    var param = {
        "userToken": "247F1A35FCA49D6443B489951AA1B877",
        "fxcmAccount": 701116547,
        "orderId": 104563467,
        "type": "limit",
        "price": 1.15
    }
    sendFxcmRequest(url, param)
}

function createSLEntryOrder() {
    var url = "/trade/createSLEntryOrder.do";
    var param = {
        "userToken": "247F1A35FCA49D6443B489951AA1B877",
        "fxcmAccount": 701116547,
        "orderId": 104563452,
        "type": "stop",
        "price": 0.95
    }
    sendFxcmRequest(url, param)
}

function deleteAllEntryOrders() {
    var url = "/trade/deleteAllEntryOrders.do";
    var param = {
        "userToken": "247F1A35FCA49D6443B489951AA1B877",
        "fxcmAccount": 701116547
    }
    sendFxcmRequest(url, param)
}

function deleteEntryOrder() {
    var url = "/trade/deleteEntryOrder.do";
    var param = {
        "userToken": "247F1A35FCA49D6443B489951AA1B877",
        "fxcmAccount": 701116547,
        "orderId": 104562911
    }
    sendFxcmRequest(url, param)
}

function updateEntryOrder() {
    var url = "/trade/updateEntryOrder.do";
    var param = {
        "userToken": "247F1A35FCA49D6443B489951AA1B877",
        "fxcmAccount": 701116547,
        "orderId": 104562911,
        "amount": 1000,
        "price": 1.05,
    }
    sendFxcmRequest(url, param)
}

function deleteMarketOrder() {
    var url = "/trade/deleteMarketOrder.do";
    var param = {
        "userToken": "247F1A35FCA49D6443B489951AA1B877",
        "fxcmAccount": 701116547,
        "fxcmPosID": 63825945
    }
    sendFxcmRequest(url, param)
}

function deleteSLMarketOrder() {
    var url = "/trade/deleteSLMarketOrder.do";
    var param = {
        "userToken": "247F1A35FCA49D6443B489951AA1B877",
        "fxcmAccount": 701116547,
        "orderId": 104560731,
        "type": "limit"
    }
    sendFxcmRequest(url, param)
}

function updateSLMarketOrder() {
    var url = "/trade/updateSLMarketOrder.do";
    var param = {
        "userToken": "247F1A35FCA49D6443B489951AA1B877",
        "fxcmAccount": 701116547,
        "orderId": 104560731,
        "type": "limit",
        "price": 1.03
    }
    sendFxcmRequest(url, param)
}

function createSLMarketOrder() {
    var url = "/trade/createSLMarketOrder.do";
    var param = {
        "userToken": "247F1A35FCA49D6443B489951AA1B877",
        "fxcmAccount": 701116547,
        "fxcmPosId": 63825913,
        "type": "limit",
        "price": 1.05
    }
    sendFxcmRequest(url, param)

}

function loginFXCM() {
    var url = "/trade/loginFXCM.do";
    var param = {
        "userToken": "247F1A35FCA49D6443B489951AA1B877",
        "fxcmAccount": 701116547,
        "fxcmPassword": 890128
    }
    sendFxcmRequest(url, param)

}

function deleteAllOpenPositions() {
    var url = "/trade/deleteAllOpenPositions.do";
    var param = {
        "userToken": "247F1A35FCA49D6443B489951AA1B877",
        "fxcmAccount": 701116547
    }
    $("#open_window").hide();
    sendFxcmRequest(url, param)

}

function createMarketOrder() {
    var url = "/trade/createMarketOrder.do";
    var stop_price = 0;
    var limit_price = 0;
    if ($("#marketOrder .stop_check").prop("checked")) {
        stop_price = $("#marketOrder .stop_price").val();
        console.log("stop_price:" + stop_price)
    }
    if ($("#marketOrder .limit_check").prop("checked")) {
        limit_price = $("#marketOrder .limit_price").val();
        console.log("limit price:" + limit_price)
    }

    var param = {
        "currency": $("#marketOrder .symbol").val(),
        "tradeSide": $('input[name="side"]:checked').val(),
        "tradeAmount": $("#marketOrder .amount").val(),
        "tradeStop": stop_price,
        "tradeLimit": limit_price
    }
    console.log(param)
    sendFxcmRequest(url, param);
}

function createEntryOrder() {
    var url = "/trade/createEntryOrder.do";
    var param = {
        "userToken": "247F1A35FCA49D6443B489951AA1B877",
        "fxcmAccount": "701116547",
        "currency": "EUR/USD",
        "price": 1.1,
        "type": "limit",
        "amount": 1000,
        "side": "buy",
        "stop": 0,
        "limit": 0
    }
    sendFxcmRequest(url, param);
}

function sendFxcmRequest(url, param) {
    $.ajax({
        url: url,
        data: param,
        type: "post",
        success: function (result) {
            // console.log(result)
        }
    })

}

function getCollateralReport() {
    var url = "/trade/getCollateralReport.do";
    $.ajax({
        url: url,
        dataType: "json",
        type: "post",
        success: function (result) {
            $("#allData").data("collateralReport", result.data)
        }
    })

}


function doCreatRows(resultMap) {
    $.each(resultMap, function (key, item) {
        var pointSize = item.instrument.fxcmsymPrecision;

        var _key = key.replace("/", "");
        //将市场数据放到div中保存
        //todo 将market数据放到
        $("#" + _key).data("market", item);

        $("#" + _key + " .symbol").html(item.instrument.symbol);
        $("#" + _key + " .sell_price").html(item.bidOpen.toFixed(pointSize));
        $("#" + _key + " .buy_price").html(item.askOpen.toFixed(pointSize));
        $("#" + _key + " .spread").html(((item.askOpen - item.bidOpen) / item.instrument.fxcmsymPointSize).toFixed(2));
        $("#" + _key + " .high_price").html(item.high.toFixed(pointSize));
        $("#" + _key + " .low_price").html(item.low.toFixed(pointSize));
        $("#" + _key + " .time").html(new Date(item.tickTime.time).getHours() + ":" + ((new Date(item.tickTime.time).getMinutes()).toString().length > 1 ? new Date(item.tickTime.time).getMinutes() : ("0" + new Date(item.tickTime.time).getMinutes())) + ":" + new Date(item.tickTime.time).getSeconds());

    })
}