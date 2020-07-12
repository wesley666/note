package com.example.note.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.note.application.MyApplication;
import com.example.note.db.Note;
import com.example.note.db.NoteForBmob;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import cn.bmob.v3.BmobBatch;
import cn.bmob.v3.BmobObject;
import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.datatype.BatchResult;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.QueryListListener;
import cn.bmob.v3.listener.SaveListener;
/*
public class SynWithBmob {
    private static final String TAG = "SynWithBmob";
    private static SynWithBmob synWithBmob = null;
    private boolean updateNotesFromBmobSuccess = false;
    private boolean updateNotesSuccess = false;
    private boolean addNotesSuccess = false;
    private long synTime = 0;
    //private Context context;

    public synchronized static SynWithBmob getInstance(){
        if(synWithBmob == null){
            synWithBmob = new SynWithBmob();
        }
        return synWithBmob;
    }



    //先向云端请求更新数据，更新成功后，才更新最近同步时间，才能向服务端更新数据。
    public void updateNotesFromBmob(long recentSynTime) {
        //用户喜好  首页面 notes布局显示  第一次默认 宫格模式
        //SharedPreferences prefs = getSharedPreferences("RecentSynTime", MODE_PRIVATE);
        //showNotesModel = prefs.getString("ShowNotesModel", "宫格模式");
        //NoteForBmob noteForBmob = new NoteForBmob();
        //noteForBmob.update()

        BmobQuery<NoteForBmob> noteForBmobBmobQuery = new BmobQuery<>();
        noteForBmobBmobQuery.addWhereGreaterThan("modifiedTime", recentSynTime);
        noteForBmobBmobQuery.findObjects(new FindListener<NoteForBmob>() {
            @Override
            public void done(List<NoteForBmob> list, BmobException e) {
                if (e == null) {
                    Log.d(TAG, "done: " + "查询成功：" + list.size());
                    NoteDbManager noteDbManager = NoteDbManager.getInstance();
                    for (NoteForBmob noteForBmob : list) {
                        Note note = setNote(noteForBmob);
                        //如果更新失败，即数据库没有这个记录。则添加一条数据。
                        if (noteDbManager.updateNoteByTime(note.getCreatedTime(), note) <= 0) {
                            noteDbManager.addNote(note);
                        }
                        if(note.getAnchor() > synTime) {
                            synTime = note.getAnchor();
                        }
                    }
                    updateNotesFromBmobSuccess = true;
                } else {
                    Log.e("BMOB", e.toString());
                    Log.d(TAG, "done: " + e.getMessage());
                }
            }
        });
    }

    public void updateNotes(List<Note> noteList) {
        List<BmobObject> noteForBmobList = new ArrayList<>();
        List<Long> modifiedTimeList = new ArrayList<>();

        for (Note note : noteList) {
            NoteForBmob noteForBmob = setNoteForBmob(note, modifiedTimeList);
            noteForBmob.setObjectId(note.getObjectId());
            noteForBmobList.add(noteForBmob);
        }

        new BmobBatch().updateBatch(noteForBmobList).doBatch(new QueryListListener<BatchResult>() {
            @Override
            public void done(List<BatchResult> list, BmobException e) {
                if (e == null) {
                    NoteDbManager noteDbManager = NoteDbManager.getInstance();
                    for (int i = 0; i < list.size(); i++) {
                        BatchResult result = list.get(i);
                        BmobException ex = result.getError();
                        if (ex == null) {
                            Log.d(TAG, "第" + i + "个数据批量更新成功：" + result.getCreatedAt() + "," + result.getObjectId() + "," + result.getUpdatedAt());
                            Note note = new Note();
                            //note.setObjectId(result.getObjectId());
                            note.setStatus(2);
                            note.setAnchor(modifiedTimeList.get(i));
                            synTime = modifiedTimeList.get(i);
                            noteDbManager.updateNoteByTime(noteList.get(i).getCreatedTime(), note);
                        } else {
                            Log.d(TAG, "第" + i + "个数据批量更新失败：" + ex.getMessage() + "," + ex.getErrorCode());
                        }
                    }
                    updateNotesSuccess = true;
                } else {
                    Log.d(TAG, "失败：" + e.getMessage() + "," + e.getErrorCode());
                }
            }
        });
    }

    public void addNotes(List<Note> noteList) {
        List<BmobObject> noteForBmobList = new ArrayList<>();
        List<Long> modifiedTimeList = new ArrayList<>();

        for (Note note : noteList) {
            noteForBmobList.add(setNoteForBmob(note, modifiedTimeList));
        }
        new BmobBatch().insertBatch(noteForBmobList).doBatch(new QueryListListener<BatchResult>() {
            @Override
            public void done(List<BatchResult> results, BmobException e) {
                if (e == null) {
                    NoteDbManager noteDbManager = NoteDbManager.getInstance();
                    for (int i = 0; i < results.size(); i++) {
                        BatchResult result = results.get(i);
                        BmobException ex = result.getError();
                        if (ex == null) {
                            Log.d(TAG, "第" + i + "个数据批量添加成功：" + result.getCreatedAt() + "," + result.getObjectId() + "," + result.getUpdatedAt());
                            Note note = new Note();
                            note.setObjectId(result.getObjectId());
                            note.setStatus(2);
                            note.setAnchor(modifiedTimeList.get(i));
                            synTime = modifiedTimeList.get(i);
                            //Log.d(TAG, "done: recentSynTime" + modifiedTimeList.get(i) +" " + recentSynTime[0]);
                            noteDbManager.updateNoteByTime(noteList.get(i).getCreatedTime(), note);
                        } else {
                            Log.d(TAG, "done: " + "第" + i + "个数据批量添加失败：" + ex.getMessage() + "," + ex.getErrorCode());
                        }
                    }
                    addNotesSuccess = true;
                } else {
                    Log.d(TAG, "失败：" + e.getMessage() + "," + e.getErrorCode());
                }
            }
        });
    }


    public void deleteNotes(List<Note> noteList) {
        List<BmobObject> noteForBmobList = new ArrayList<>();
        for (Note note : noteList) {
            NoteForBmob noteForBmob = new NoteForBmob();
            noteForBmob.setObjectId(note.getObjectId());
            noteForBmobList.add(noteForBmob);
        }
        new BmobBatch().deleteBatch(noteForBmobList).doBatch(new QueryListListener<BatchResult>() {
            @Override
            public void done(List<BatchResult> results, BmobException e) {
                NoteDbManager noteDbManager = NoteDbManager.getInstance();
                if (e == null) {
                    for (int i = 0; i < results.size(); i++) {
                        BatchResult result = results.get(i);
                        BmobException ex = result.getError();
                        if (ex == null) {
                            noteDbManager.deleteNote(noteList.get(i));
                            Log.d(TAG, "第" + i + "个数据批量删除成功：" + result.getCreatedAt() + "," + result.getObjectId() + "," + result.getUpdatedAt());
                        } else {
                            Log.d(TAG, "第" + i + "个数据批量删除失败：" + ex.getMessage() + "," + ex.getErrorCode());
                        }
                    }
                } else {
                    Log.d(TAG, "失败：" + e.getMessage() + "," + e.getErrorCode());
                }
            }
        });
    }

    public void setSynTime(long synTime) {
        this.synTime = synTime;
    }

    public long getSynTime() {
        return synTime;
    }

    public void setUpdateNotesFromBmobSuccess(boolean updateNotesFromBmobSuccess) {
        this.updateNotesFromBmobSuccess = updateNotesFromBmobSuccess;
    }

    public void setUpdateNotesSuccess(boolean updateNotesSuccess) {
        this.updateNotesSuccess = updateNotesSuccess;
    }

    public void setAddNotesSuccess(boolean addNotesSuccess) {
        this.addNotesSuccess = addNotesSuccess;
    }

    public boolean isUpdateNotesFromBmobSuccess() {
        return updateNotesFromBmobSuccess;
    }

    public boolean isUpdateNotesSuccess() {
        return updateNotesSuccess;
    }

    public boolean isAddNotesSuccess() {
        return addNotesSuccess;
    }

    private NoteForBmob setNoteForBmob(Note note, List<Long> modifiedTimeList) {
        NoteForBmob noteForBmob = new NoteForBmob();

        noteForBmob.setId(note.getId());
        noteForBmob.setContent(note.getContent());
        noteForBmob.setGroupName(note.getGroupName());
        noteForBmob.setCreateTime(note.getCreateTime());
        noteForBmob.setUpdateTime(note.getUpdateTime());
        noteForBmob.setTitle(note.getTitle());
        noteForBmob.setSubContent(note.getSubContent());
        noteForBmob.setRestoreSpans(byteToByte(note.getRestoreSpans()));
        noteForBmob.setTimeRemind(note.getTimeRemind());

        noteForBmob.setCreatedTime(note.getCreatedTime());
        noteForBmob.setUpdatedTime(note.getUpdatedTime());

        long currentTime = System.currentTimeMillis();
        noteForBmob.setModifiedTime(currentTime);
        modifiedTimeList.add(currentTime);

        return noteForBmob;
    }

    private Note setNote(NoteForBmob noteForBmob) {
        Note note = new Note();

        //note.setId(noteForBmob.getId());
        note.setContent(noteForBmob.getContent());
        note.setGroupName(noteForBmob.getGroupName());
        note.setCreateTime(noteForBmob.getCreateTime());
        note.setUpdateTime(noteForBmob.getUpdateTime());
        note.setTitle(noteForBmob.getTitle());
        note.setSubContent(noteForBmob.getSubContent());
        note.setRestoreSpans(toPrimitives(noteForBmob.getRestoreSpans()));
        note.setTimeRemind(noteForBmob.getTimeRemind());
        note.setCreatedTime(noteForBmob.getCreatedTime());
        note.setUpdatedTime(noteForBmob.getUpdatedTime());

        note.setObjectId(noteForBmob.getObjectId());
        note.setStatus(2);
        note.setAnchor(noteForBmob.getModifiedTime());

        return note;

    }

    private Byte[] byteToByte(byte[] bytes) {
        Byte[] byteObjects = new Byte[bytes.length];
        int i = 0;
      // Associating Byte array values with bytes. (byte[] to Byte[])
        for(byte b: bytes) {
            byteObjects[i++] = b;  // Autoboxing.
            // }
        }
        return byteObjects;
    }

    private byte[] toPrimitives(Byte[] byteObjects) {
        byte[] bytes= new byte[byteObjects.length];
        int i = 0;
        // Unboxing Byte values. (Byte[] to byte[])
        for(Byte b: byteObjects) {
            bytes[i++] = b.byteValue();
        }
        return bytes;
    }
}
*/