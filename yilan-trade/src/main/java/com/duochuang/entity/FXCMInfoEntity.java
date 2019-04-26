/***********************************************
 * File Name: FXCMInfoEntity
 * Author: caoguobin
 * mail: caoguobin@live.com
 * Created Time: 28 02 2019 14:57
 ***********************************************/

package com.duochuang.entity;

import com.duochuang.common.EntityBase;

public class FXCMInfoEntity extends EntityBase {
    private String userId;
    private boolean accountRole;
    private String fxcmAccount;
    private String fxcmPassword;
    private boolean followType;
    private double followParam;
    private String accountType;
    private boolean orderChoice;
    private boolean sellChoice;
    private String hostAddr;
    private boolean deleteStatus;

    public FXCMInfoEntity() {
    }

    public FXCMInfoEntity(String fxcmAccount, String fxcmPassword, String accountType, String hostAddr) {
        this.fxcmAccount = fxcmAccount;
        this.fxcmPassword = fxcmPassword;
        this.accountType = accountType;
        this.hostAddr = hostAddr;
    }

    public FXCMInfoEntity(String userId, String fxcmAccount, String fxcmPassword, boolean followType, double followParam, String accountType, String hostAddr, boolean deleteStatus) {
        this.userId = userId;
        this.fxcmAccount = fxcmAccount;
        this.fxcmPassword = fxcmPassword;
        this.followType = followType;
        this.followParam = followParam;
        this.accountType = accountType;
        this.hostAddr = hostAddr;
        this.deleteStatus = deleteStatus;
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

    public boolean isDeleteStatus() {
        return deleteStatus;
    }

    public void setDeleteStatus(boolean deleteStatus) {
        this.deleteStatus = deleteStatus;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    public boolean isFollowType() {
        return followType;
    }

    public void setFollowType(boolean followType) {
        this.followType = followType;
    }

    public double getFollowParam() {
        return followParam;
    }

    public void setFollowParam(double followParam) {
        this.followParam = followParam;
    }

    public boolean isAccountRole() {
        return accountRole;
    }

    public void setAccountRole(boolean accountRole) {
        this.accountRole = accountRole;
    }

    public boolean isOrderChoice() {
        return orderChoice;
    }

    public void setOrderChoice(boolean orderChoice) {
        this.orderChoice = orderChoice;
    }

    public boolean isSellChoice() {
        return sellChoice;
    }

    public void setSellChoice(boolean sellChoice) {
        this.sellChoice = sellChoice;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"userId\":\"")
                .append(userId).append('\"');
        sb.append(",\"accountRole\":")
                .append(accountRole);
        sb.append(",\"fxcmAccount\":\"")
                .append(fxcmAccount).append('\"');
        sb.append(",\"fxcmPassword\":\"")
                .append(fxcmPassword).append('\"');
        sb.append(",\"followType\":")
                .append(followType);
        sb.append(",\"followParam\":")
                .append(followParam);
        sb.append(",\"accountType\":\"")
                .append(accountType).append('\"');
        sb.append(",\"orderChoice\":")
                .append(orderChoice);
        sb.append(",\"sellChoice\":")
                .append(sellChoice);
        sb.append(",\"hostAddr\":\"")
                .append(hostAddr).append('\"');
        sb.append(",\"deleteStatus\":")
                .append(deleteStatus);
        sb.append('}');
        return sb.toString();
    }
}
