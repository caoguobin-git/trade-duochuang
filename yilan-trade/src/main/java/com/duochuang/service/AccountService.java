/***********************************************
 * File Name: AccountService
 * Author: caoguobin
 * mail: caoguobin@live.com
 * Created Time: 08 05 2019 15:59
 ***********************************************/

package com.duochuang.service;

import com.duochuang.entity.FXCMInfoEntity;

import java.util.List;

public interface AccountService {
    List<FXCMInfoEntity> findAllAccounts();

    String saveNewAccount(String account, String password, String role, String type);

    String deleteAccountById(String account);
}
