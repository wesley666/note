package com.example.note.db;

import org.litepal.crud.LitePalSupport;

public class MediaForNote extends LitePalSupport {

    private long id;
    private String mediaName;
    //属于哪个笔记的资源
    private long noteCreateTime;
    private boolean isPhoto;  //区分上传时路径选择问题

    //用于云同步
    private int status;  //用来标识记录的状态，0本地新增， -1，标记删除   1 本地更新   2 已经同步
    private long anchor; //记录服务端同步过来的时间戳,默认为0

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getMediaName() {
        return mediaName;
    }

    public void setMediaName(String mediaName) {
        this.mediaName = mediaName;
    }

    public long getNoteCreateTime() {
        return noteCreateTime;
    }

    public void setNoteCreateTime(long noteCreateTime) {
        this.noteCreateTime = noteCreateTime;
    }

    public void setPhoto(boolean photo) {
        isPhoto = photo;
    }

    public boolean isPhoto() {
        return isPhoto;
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
