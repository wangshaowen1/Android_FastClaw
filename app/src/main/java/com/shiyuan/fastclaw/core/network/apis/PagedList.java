package com.shiyuan.fastclaw.core.network.apis;


public class PagedList<T> extends ListData<T> {

    private int pages;
    private int total;
    private int current;
    //特殊字段(目前只有在分页查询歌曲评论列表中存在，其他地方可能为空)：评论回复总数目（包含一级回复和回复的回复）
    private long currentTotalSize;


    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    public int getTotalSize() {
        return total;
    }

    public void setTotalSize(int totalSize) {
        this.total = totalSize;
    }

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        this.current = current;
    }

    public long getCurrentTotalSize() {
        return currentTotalSize;
    }

    public void setCurrentTotalSize(long currentTotalSize) {
        this.currentTotalSize = currentTotalSize;
    }
}
