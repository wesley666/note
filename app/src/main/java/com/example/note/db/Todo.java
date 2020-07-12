package com.example.note.db;


import org.litepal.crud.LitePalSupport;

public class Todo extends LitePalSupport {
    private int id;         //笔记ID
    private boolean checkBoxFlag;
    private String content; //笔记内容
    private long createTime; //笔记创建时间
    private long updatedTime;
    private String updateTime;
    private long timeRemind;     //-1表示没有设置提醒，单位毫秒

    private int status;  //用来标识记录的状态，0本地新增， -1，标记删除   1 本地更新   2 已经同步
    private long anchor; //记录服务端同步过来的时间戳,默认为0

    public Todo(){

    }

    public Todo(Todo todo) {
        this.id = 0;
        this.checkBoxFlag = todo.checkBoxFlag;
        this.content = todo.content;
        this.createTime = todo.createTime;
        this.updatedTime = todo.updatedTime;
        this.updateTime = todo.updateTime;
        this.timeRemind = todo.timeRemind;
        this.status = todo.status;
        this.anchor = todo.anchor;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isCheckBoxFlag() {
        return checkBoxFlag;
    }

    public void setCheckBoxFlag(boolean checkBoxFlag) {
        this.checkBoxFlag = checkBoxFlag;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public long getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(long updatedTime) {
        this.updatedTime = updatedTime;
    }

    public long getTimeRemind() {
        return timeRemind;
    }

    public void setTimeRemind(long timeRemind) {
        this.timeRemind = timeRemind;
    }


    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getAnchor() {
        return anchor;
    }

    public void setAnchor(long anchor) {
        this.anchor = anchor;
    }
}
