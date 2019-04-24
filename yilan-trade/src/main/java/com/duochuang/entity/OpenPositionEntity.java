/***********************************************
 * File Name: OpenPositionEntity
 * Author: caoguobin
 * mail: caoguobin@live.com
 * Created Time: 04 04 2019 11:56
 ***********************************************/

package com.duochuang.entity;

import com.fxcm.fix.posttrade.PositionReport;
import com.fxcm.fix.trade.ExecutionReport;

public class OpenPositionEntity {
    private PositionReport position;
    private ExecutionReport stop;
    private ExecutionReport limit;

    public PositionReport getPosition() {
        return position;
    }

    public void setPosition(PositionReport position) {
        this.position = position;
    }

    public ExecutionReport getStop() {
        return stop;
    }

    public void setStop(ExecutionReport stop) {
        this.stop = stop;
    }

    public ExecutionReport getLimit() {
        return limit;
    }

    public void setLimit(ExecutionReport limit) {
        this.limit = limit;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"position\":")
                .append(position);
        sb.append(",\"stop\":")
                .append(stop);
        sb.append(",\"limit\":")
                .append(limit);
        sb.append('}');
        return sb.toString();
    }
}
