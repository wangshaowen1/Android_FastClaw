package com.shiyuan.fastclaw.core.network.apis.request;


import com.shiyuan.fastclaw.core.User;

public class PagedBody {

    private int pageNum;
    private int pageSize;




    public PagedBody(int pageNum, int pageSize) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    public PagedBody(String userToken, int pageNum, int pageSize) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }


    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
