/***********************************************
 * File Name: AccountMapper
 * Author: caoguobin
 * mail: caoguobin@live.com
 * Created Time: 08 05 2019 16:00
 ***********************************************/
package com.duochuang.mapper;

import com.duochuang.entity.FXCMInfoEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AccountMapper {
    List<FXCMInfoEntity> findAllAccounts();

    int saveNewAccount(@Param("account") String account,@Param("password") String password,@Param("role") String role,@Param("type") String type);

    int findTrader();

    int findAccountByAccount(@Param("account") String account);

    int deleteAccountById(@Param("account") String account);
}
