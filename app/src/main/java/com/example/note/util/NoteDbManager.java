package com.example.note.util;

import android.util.Log;

import com.example.note.db.Note;

import org.litepal.LitePal;

import java.util.List;

public class NoteDbManager {

    private static NoteDbManager dbManager = null;
    private static final String TAG = "NoteDbManager";

    public synchronized static NoteDbManager getInstance(){
        if(dbManager == null){
            dbManager = new NoteDbManager();
        }
        return dbManager;
    }

    //获得所有便签 用于展示 “所有便签” 这个虚拟分组
    public List<Note> getAll(){
        return LitePal.where("groupName != ? and status != ?","回收站", "-1").order("updatedTime desc").find(Note.class);
        //return LitePal.findAll(Note.class);
    }

    public List<Note> getNoteByStatus(int status) {
        return LitePal.where("status = ?", Integer.toString(status)).find(Note.class);
    }

    public List<Note> getNotesGreaterThanAnchor(long anchor) {
        return LitePal.where("anchor > ?", Long.toString(anchor)).find(Note.class);
    }

    //获得一个组的所有便签
    public List<Note> getNotesByGroup(String GroupName){
        //Cursor cursor=db.query(NotesDatabaseHelper.TABLE.NOTE,null,"groupName = ?",new String[]{GroupName},null,null,null);
        return LitePal.where("groupName = ? and status != ?", GroupName, "-1").order("updatedTime desc").find(Note.class);
    }

    public boolean addNote(Note note){
        return note.save();
    }

    public void addNotes(List<Note> noteList) {
        LitePal.saveAll(noteList);
    }

    public void updateNote(int id, Note note) {
        //包装类型才有tostring
        String id1 = Integer.toString(id);
        note.updateAll("id = ?",id1);
    }

    public int updateNoteByTime(long createdTime, Note note) {
        //包装类型才有tostring
        String id1 = Long.toString(createdTime);
        return note.updateAll("createdTime = ?", id1);
    }

    public void deleteNote(Note note){
        //db = mHelper.getWritableDatabase();
        //Integer id = note.getId();   //包装类型才有tostring
        //String id1 = id.toString();

        String timeStr = Long.toString(note.getCreatedTime());

        LitePal.deleteAll(Note.class, "createdTime = ?", timeStr);

        //db.delete(NotesDatabaseHelper.TABLE.NOTE,"id = ?",new String[]{id1});
    }

    public String getNoteContentById(int id){
        String id1 = Integer.toString(id);
        List<Note> notesCollections = LitePal.where("id = ?",id1).find(Note.class);

        if (notesCollections.isEmpty()) {
            Log.d("根据id查找便签内容","找不到");
            return "";
        }

        return notesCollections.get(0).getContent();
    }

    public Note getNoteById(int id){
        String id1 = Integer.toString(id);

        List<Note> notesCollections = LitePal.where("id = ?",id1).find(Note.class);

        if (notesCollections.isEmpty()) {
            Log.d("根据id查找便签","找不到");
            return null;
        } else  {
            Log.d("根据id查找便签","找到" + notesCollections.size());
        }

        return notesCollections.get(0);
    }
}
