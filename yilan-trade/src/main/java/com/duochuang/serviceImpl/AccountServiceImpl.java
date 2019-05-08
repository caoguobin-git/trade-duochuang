/***********************************************
 * File Name: AccountServiceImpl
 * Author: caoguobin
 * mail: caoguobin@live.com
 * Created Time: 08 05 2019 16:00
 ***********************************************/

package com.duochuang.serviceImpl;

import com.duochuang.entity.FXCMInfoEntity;
import com.duochuang.mapper.AccountMapper;
import com.duochuang.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountServiceImpl implements AccountService {

    @Autowired
    private AccountMapper accountMapper;

    @Override
    public List<FXCMInfoEntity> findAllAccounts() {
        return accountMapper.findAllAccounts();
    }

    @Override
    public String saveNewAccount(String account, String password, String role, String type) {
        if ("trader".equalsIgnoreCase(role)){
            int a = accountMapper.findTrader();
            return "管理员已经存在";
        }
        int b =accountMapper.findAccountByAccount(account);
        if (b>0){
            return "账号已存在！";
        }
        int result = accountMapper.saveNewAccount(account,password,role,type);
        if (result>0){
            return "ok";
        }
        return "新增失败";
    }

    @Override
    public String deleteAccountById(String account) {
        int a = accountMapper.deleteAccountById(account);
        if (a>0){
            return "ok";
        }
        return "删除失败";
    }


}
