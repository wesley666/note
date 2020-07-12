package com.example.note.db;

import org.litepal.crud.LitePalSupport;

import java.io.Serializable;

public class Note extends LitePalSupport implements Serializable {
    private int id;         //笔记ID
    private String content; //笔记内容
    private String groupName;    //笔记所属组的名字
    private String createTime; //笔记创建时间
    private long createdTime; //以空间换时间
    private String updateTime;
    private long updatedTime;
    private String title;  //   这里的title 指的是  笔记的第一行 一般都是纲要 用于显示纲要
    private String subContent; //这里的subContent指的是 笔记的第二行，用于反应除了用户的开头   相当于内容的缩写
    private byte[] restoreSpans;
    private long timeRemind;     //-1表示没有设置提醒，单位毫秒

    //用于云同步
    //private String objectId; //用于bmob
    private int status;  //用来标识记录的状态，0本地新增， -1，标记删除   1 本地更新   2 已经同步
    private long anchor; //记录服务端同步过来的时间戳,默认为0

    public Note() {

    }

    public Note(Note note) {
        this.id = 0;
        this.content = note.content;
        this.groupName = note.groupName;
        this.createTime = note.createTime;
        this.updateTime = note.updateTime;
        this.title = note.title;
        this.subContent = note.subContent;
        this.restoreSpans = note.restoreSpans;
        this.timeRemind = note.timeRemind;
        this.createdTime = note.createdTime;
        this.updatedTime = note.updatedTime;
        this.status = note.status;
        this.anchor = note.anchor;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubContent() {
        return subContent;
    }

    public void setSubContent(String subContent) {
        this.subContent = subContent;
    }

    public byte[] getRestoreSpans() {
        return restoreSpans;
    }

    public void setRestoreSpans(byte[] restoreSpans) {
        this.restoreSpans = restoreSpans;
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

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public long getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(long updatedTime) {
        this.updatedTime = updatedTime;
    }
}
