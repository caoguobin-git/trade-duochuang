package com.duochuang.entity;

import com.fxcm.fix.trade.ExecutionReport;

public class OrderEntity {
    private ExecutionReport mainOrder;
    private ExecutionReport limit;
    private ExecutionReport stop;

    public ExecutionReport getMainOrder() {
        return mainOrder;
    }

    public void setMainOrder(ExecutionReport mainOrder) {
        this.mainOrder = mainOrder;
    }

    public ExecutionReport getLimit() {
        return limit;
    }

    public void setLimit(ExecutionReport limit) {
        this.limit = limit;
    }

    public ExecutionReport getStop() {
        return stop;
    }

    public void setStop(ExecutionReport stop) {
        this.stop = stop;
    }

    @Override
    public String toString() {
        return "OrderEntity{" +
                "mainOrder=" + mainOrder +
                ", limit=" + limit +
                ", stop=" + stop +
                '}';
    }
}
