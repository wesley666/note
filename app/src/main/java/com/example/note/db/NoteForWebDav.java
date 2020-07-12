package com.example.note.db;

public class NoteForWebDav {
    //private Integer id;         //笔记ID
    private String content; //笔记内容
    private String groupName;    //笔记所属组的名字
    private String createTime; //笔记创建时间
    private String updateTime;
    private String title;  //   这里的title 指的是  笔记的第一行 一般都是纲要 用于显示纲要
    private String subContent; //这里的subContent指的是 笔记的第二行，用于反应除了用户的开头   相当于内容的缩写
    private Byte[] restoreSpans;
    private Long timeRemind;     //-1表示没有设置提醒，单位毫秒
    private Long createdTime;
    private Long updatedTime;

    private Long modifiedTime;   //时间戳，用于同步


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

    public Byte[] getRestoreSpans() {
        return restoreSpans;
    }

    public void setRestoreSpans(Byte[] restoreSpans) {
        this.restoreSpans = restoreSpans;
    }

    public long getTimeRemind() {
        return timeRemind;
    }

    public void setTimeRemind(long timeRemind) {
        this.timeRemind = timeRemind;
    }

    public long getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(long modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public Long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Long createdTime) {
        this.createdTime = createdTime;
    }

    public Long getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(Long updatedTime) {
        this.updatedTime = updatedTime;
    }
}
