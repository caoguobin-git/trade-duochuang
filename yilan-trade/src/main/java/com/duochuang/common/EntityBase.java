/***********************************************
 * File Name: EntityBase
 * Author: caoguobin
 * mail: caoguobin@live.com
 * Created Time: 28 02 2019 15:01
 ***********************************************/

package com.duochuang.common;

import java.sql.Timestamp;

public class EntityBase {
    private Timestamp createtime;
    private Timestamp modifiedtime;
    private boolean delete_status;

    public Timestamp getCreatetime() {
        return createtime;
    }

    public void setCreatetime(Timestamp createtime) {
        this.createtime = createtime;
    }

    public Timestamp getModifiedtime() {
        return modifiedtime;
    }

    public void setModifiedtime(Timestamp modifiedtime) {
        this.modifiedtime = modifiedtime;
    }

    public boolean isDelete_status() {
        return delete_status;
    }

    public void setDelete_status(boolean delete_status) {
        this.delete_status = delete_status;
    }
}
