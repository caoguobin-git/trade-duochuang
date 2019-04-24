/***********************************************
 * File Name: TradeMapper
 * Author: caoguobin
 * mail: caoguobin@live.com
 * Created Time: 28 02 2019 13:33
 ***********************************************/
package com.duochuang.mapper;

import com.duochuang.entity.FXCMInfoEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TradeMapper {
    List<FXCMInfoEntity> findFollowerAccountsByUserId(@Param("userId") String userId);

    FXCMInfoEntity findByAccountAndPassword(@Param("account") String fxcmAccount, @Param("password") String fxcmPassword);

    boolean isTrader(@Param("userId") String userId, @Param("fxcmAccount") String fxcmAccount);
}
