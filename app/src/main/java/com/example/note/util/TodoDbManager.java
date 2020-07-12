package com.example.note.util;

import android.util.Log;

import com.example.note.db.Todo;

import org.litepal.LitePal;

import java.util.List;

public class TodoDbManager {

    private static TodoDbManager dbManager = null;

    public synchronized static TodoDbManager getInstance(){
        if(dbManager == null){
            dbManager = new TodoDbManager();
        }
        return dbManager;
    }

    //获得所有便签 用于展示 “所有便签” 这个虚拟分组
    public List<Todo> getAll(){
        return LitePal.where("status != ?", "-1").order("updatedTime desc").find(Todo.class);
    }

    public List<Todo> getTodoByStatus(int status) {
        return LitePal.where("status = ?", Integer.toString(status)).find(Todo.class);
    }

    public List<Todo> getTodosGreaterThanAnchor(long anchor) {
        return LitePal.where("anchor > ?", Long.toString(anchor)).find(Todo.class);
    }

    public void addTodo(Todo todo){
        todo.save();
    }

    public void deleteTodo(Todo todo){
        //db = mHelper.getWritableDatabase();
        //Long id = todo.getCreateTime();   //包装类型才有tostring
        //String id1 = id.toString();

        LitePal.deleteAll(Todo.class, "createTime = ?", Long.toString(todo.getCreateTime()));

        //db.delete(NotesDatabaseHelper.TABLE.NOTE,"id = ?",new String[]{id1});
    }

    public void updateTodo(int id, Todo todo) {
        //包装类型才有tostring
        String id1 = Integer.toString(id);
        todo.updateAll("id = ?",id1);
    }

    public int updateTodoByTime(long createdTime, Todo todo) {
        //包装类型才有tostring
        if (!todo.isCheckBoxFlag()) {
            todo.setToDefault("checkBoxFlag");
        }
        return todo.updateAll("createTime = ?", Long.toString(createdTime));
    }

    public String getTodoContentById(int id){
        String id1 = Integer.toString(id);
        //db = mHelper.getReadableDatabase();
        //Cursor cursor=db.query(NotesDatabaseHelper.TABLE.NOTE,null,"id = ?",new String[]{id1},null,null,null);

        List<Todo> todosCollections = LitePal.where("id = ?",id1).find(Todo.class);
        if (todosCollections.isEmpty()) {
            Log.d("根据id查找便签内容","找不到");
            return "";
        }
        return todosCollections.get(0).getContent();
    }

    public Todo getTodoById(int id){
        String id1 = Integer.toString(id);
        //db = mHelper.getReadableDatabase();
        //Cursor cursor=db.query(NotesDatabaseHelper.TABLE.NOTE,null,"id = ?",new String[]{id1},null,null,null);

        List<Todo> todosCollections = LitePal.where("id = ?",id1).find(Todo.class);

        if (todosCollections.isEmpty()) {
            Log.d("根据id查找便签","找不到");
            return null;
        }
        return todosCollections.get(0);
    }
}
