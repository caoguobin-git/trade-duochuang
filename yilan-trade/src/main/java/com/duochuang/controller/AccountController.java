/***********************************************
 * File Name: AccountController
 * Author: caoguobin
 * mail: caoguobin@live.com
 * Created Time: 08 05 2019 15:58
 ***********************************************/

package com.duochuang.controller;

import com.duochuang.entity.FXCMInfoEntity;
import com.duochuang.service.AccountService;
import com.duochuang.vo.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/account")
public class AccountController {

@Autowired
private AccountService accountService;

    @ResponseBody
    @RequestMapping("/findAllAccounts")
    public JsonResult findAllAccounts(){
        List<FXCMInfoEntity> list=accountService.findAllAccounts();
        return new JsonResult(list);
    }

    @RequestMapping("/index")
    public String index(){
        return "accounts";
    }

    @ResponseBody
    @RequestMapping("/saveNewAccount")
    public JsonResult saveNewAccount(String account,String password,String role,String type){
        account=account.trim();
        password=password.trim();
        role=role.trim();
        type=type.trim();
        System.out.println(account);
        System.out.println(password);
        System.out.println(role);
        System.out.println(type);
        String result = accountService.saveNewAccount(account,password,role,type);
        if ("ok".equalsIgnoreCase(result)){
            return  new JsonResult("新增用户成功！");
        }else {
            return new JsonResult("401","新增失败",result);
        }
    }

    @ResponseBody
    @RequestMapping("/deleteAccountById")
    public JsonResult deleteAccountById(String account){
        String result=accountService.deleteAccountById(account);
        if ("ok".equalsIgnoreCase(result)){
            return new JsonResult("删除成功");
        }
        else {
            return new JsonResult("401","删除失败",result);
        }
    }

}
