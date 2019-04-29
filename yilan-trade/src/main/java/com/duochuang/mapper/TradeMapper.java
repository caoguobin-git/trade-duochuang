/***********************************************
 * File Name: TradeMapper
 * Author: caoguobin
 * mail: caoguobin@live.com
 * Created Time: 28 02 2019 13:33
 ***********************************************/
package com.duochuang.mapper;

import com.duochuang.entity.FXCMInfoEntity;

import java.util.List;

public interface TradeMapper {

    FXCMInfoEntity findTrader();

    List<FXCMInfoEntity> findFollower();
}
