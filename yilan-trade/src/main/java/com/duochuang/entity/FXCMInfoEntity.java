/***********************************************
 * File Name: FXCMInfoEntity
 * Author: caoguobin
 * mail: caoguobin@live.com
 * Created Time: 28 02 2019 14:57
 ***********************************************/

package com.duochuang.entity;

import com.duochuang.common.EntityBase;

public class FXCMInfoEntity {
    private String fxcmAccount;
    private String fxcmPassword;
    private String accountRole;
    private String accountType;
    private String hostAddr;

    public FXCMInfoEntity() {
    }

    public FXCMInfoEntity(String fxcmAccount, String fxcmPassword, String accountType, String hostAddr) {
        this.fxcmAccount = fxcmAccount;
        this.fxcmPassword = fxcmPassword;
        this.accountType = accountType;
        this.hostAddr = hostAddr;
    }

    public String getFxcmAccount() {
        return fxcmAccount;
    }

    public void setFxcmAccount(String fxcmAccount) {
        this.fxcmAccount = fxcmAccount;
    }

    public String getFxcmPassword() {
        return fxcmPassword;
    }

    public void setFxcmPassword(String fxcmPassword) {
        this.fxcmPassword = fxcmPassword;
    }

    public String getAccountRole() {
        return accountRole;
    }

    public void setAccountRole(String accountRole) {
        this.accountRole = accountRole;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getHostAddr() {
        return hostAddr;
    }

    public void setHostAddr(String hostAddr) {
        this.hostAddr = hostAddr;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"fxcmAccount\":\"")
                .append(fxcmAccount).append('\"');
        sb.append(",\"fxcmPassword\":\"")
                .append(fxcmPassword).append('\"');
        sb.append(",\"accountRole\":\"")
                .append(accountRole).append('\"');
        sb.append(",\"accountType\":\"")
                .append(accountType).append('\"');
        sb.append(",\"hostAddr\":\"")
                .append(hostAddr).append('\"');
        sb.append('}');
        return sb.toString();
    }
}
