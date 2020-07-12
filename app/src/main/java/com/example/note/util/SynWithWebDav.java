package com.example.note.util;

import android.util.Log;

import com.example.note.application.MyApplication;
import com.example.note.db.Note;
import com.example.note.db.Todo;
import com.thegrizzlylabs.sardineandroid.DavResource;
import com.thegrizzlylabs.sardineandroid.Sardine;
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine;

import org.litepal.LitePal;
import org.litepal.LitePalDB;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class SynWithWebDav {
    private static final String TAG = "SynWithWebDav";
    private Sardine sardine;
    private LitePalDB litePalDB;

    public SynWithWebDav(String userName, String passWord) {
        sardine = new OkHttpSardine();
        sardine.setCredentials(userName, passWord);
        litePalDB = new LitePalDB("noteBackup", 1);
        litePalDB.setStorage("external");
        litePalDB.addClassName(Note.class.getName());
        litePalDB.addClassName(Todo.class.getName());
    }

    //返回空表代表失败
    public List<DavResource> getResources(String path) {
        List<DavResource> resourceList = new ArrayList<>();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    List<DavResource>  resources = sardine.list("https://dav.jianguoyun.com/dav/" + path);//如果是目录一定别忘记在后面加上一个斜杠
                    resourceList.addAll(resources);
//                    for (DavResource res : resources) {
//                        resourceList.add(res);
//                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return resourceList;
    }

    public boolean isFileExist(String name, String path) {
        final Boolean[] flag = {false};
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (sardine.exists("https://dav.jianguoyun.com/dav/" + path+ "/" + name)) {
                        flag[0] = true;
                    }
                } catch (IOException e) {
                    flag[0] = false;
                    e.printStackTrace();
                }
            }
        });

        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return flag[0];
    }

    public InputStream getFile(String name, String path) {
        final InputStream[] inputStream = {null};
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    inputStream[0] = sardine.get("https://dav.jianguoyun.com/dav/" + path + "/" + name);
                } catch (IOException e) {
                    inputStream[0] = null;
                    e.printStackTrace();
                }
            }
        });

        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return inputStream[0];
    }

    public void deleteFile(String name, String path) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sardine.delete("https://dav.jianguoyun.com/dav/" + path + "/" + name);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }
    public long uploadFile(String path, String fileName, String filePath) {
        final long[] recentSynTime = new long[]{0};
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                File file = new File(filePath);
                int size = (int) file.length();
                byte[] bytes = new byte[size];
                try {
                    BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
                    buf.read(bytes, 0, bytes.length);
                    buf.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }//判断路径是否存在
                try {
                    if (!sardine.exists("https://dav.jianguoyun.com/dav/" + path)) {
                        sardine.createDirectory("https://dav.jianguoyun.com/dav/" + path);
                    }
                    sardine.put(" https://dav.jianguoyun.com/dav/" + path +"/" + fileName, bytes);
                    recentSynTime[0] = System.currentTimeMillis();
                } catch (IOException e) {
                    recentSynTime[0] = 0;
                    e.printStackTrace();
                }
            }
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return recentSynTime[0];
    }

    public void updateLocalNotes(long recentSynTime) {
        Log.d(TAG, "updateLocalNotes: ");
        NoteDbManager noteDbManager = NoteDbManager.getInstance();
        LitePal.use(litePalDB);
        List<Note> noteList = noteDbManager.getNotesGreaterThanAnchor(recentSynTime);
        List<Note> deleteNoteList = noteDbManager.getNoteByStatus(-1);
        //Log.d(TAG, "updateLocalNotes: " + noteList.size() + noteList.get(0).getContent());
        LitePal.useDefault();

        //不能直接用note，要构造新的,否则不生效。
        for (Note note : noteList) {
            Note tempNote = new Note(note);
            if (noteDbManager.updateNoteByTime(tempNote.getCreatedTime(), tempNote) <= 0) {
                boolean ret = noteDbManager.addNote(tempNote);
                Log.d(TAG, "updateLocalNotes: " + ret);
            }
        }

        for (Note note : deleteNoteList) {
            DeleteMedia.deleteMedia(MyApplication.getInstance().getContext(), note.getContent());
            noteDbManager.deleteNote(note);
        }

        Log.d(TAG, "updateLocalNotes: " + noteDbManager.getAll().size());
        //while (true);
    }

    public void updateNotes(List<Note> noteList) {
        NoteDbManager noteDbManager = NoteDbManager.getInstance();
        //更新状态
        for (Note note : noteList) {
            note.setAnchor(System.currentTimeMillis());
            note.setStatus(2);
            note.save();
        }
        LitePal.use(litePalDB);
        for (Note note : noteList) {
            Note tempNote = new Note(note);
            if (noteDbManager.updateNoteByTime(tempNote.getCreatedTime(), tempNote) <= 0) {
                boolean ret = noteDbManager.addNote(tempNote);
                Log.d(TAG, "updateNotes: " + ret + tempNote.getStatus());
            }
        }
        LitePal.useDefault();
    }

    public void deleteNotes(List<Note> noteList) {
        NoteDbManager noteDbManager = NoteDbManager.getInstance();
        //delete local
        //LitePal.deleteAll(Note.class, "status = ?", "-1");
        LitePal.use(litePalDB);
        //delete web
        for (Note note : noteList) {
            Note tempNote = new Note();
            tempNote.setStatus(-1);
            noteDbManager.updateNoteByTime(note.getCreatedTime(), tempNote);
            //noteDbManager.deleteNote(note);   //不能直接删除，否则别的设备不知道，这样貌似就要把这部分数据永远保留了
        }
        LitePal.useDefault();
    }

    public void updateLocalTodos(long recentSynTime) {
        TodoDbManager todoDbManager = TodoDbManager.getInstance();
        LitePal.use(litePalDB);
        List<Todo> deleteTodoList = todoDbManager.getTodoByStatus(-1);
        List<Todo> todoList = todoDbManager.getTodosGreaterThanAnchor(recentSynTime);
        LitePal.useDefault();

        Log.d(TAG, "updateLocalTodos: " + todoList.size());

        //不能直接用note，要构造新的,否则不生效。
        for (Todo todo : todoList) {
            Todo tempTodo = new Todo(todo);
            if (todoDbManager.updateTodoByTime(tempTodo.getCreateTime(), tempTodo) <= 0) {
                todoDbManager.addTodo(tempTodo);
                Log.d(TAG, "updateLocalTodos: yes");
            }
        }

        for (Todo todo : deleteTodoList) {
            todoDbManager.deleteTodo(todo);
        }

        Log.d(TAG, "updateLocalTodos: " + todoDbManager.getAll().size());
        //while (true);
    }

    public void updateTodos(List<Todo> todoList) {
        if (todoList.size() <= 0) {
            return;
        }
        TodoDbManager todoDbManager = TodoDbManager.getInstance();
        //更新状态
        for (Todo todo : todoList) {
            todo.setAnchor(System.currentTimeMillis());
            todo.setStatus(2);
            todo.save();
        }
        LitePal.use(litePalDB);
        for (Todo todo : todoList) {
            Todo tempTodo = new Todo(todo);
            if (todoDbManager.updateTodoByTime(tempTodo.getCreateTime(), tempTodo) <= 0) {
                todoDbManager.addTodo(tempTodo);
                Log.d(TAG, "updateNotes: " + tempTodo.getStatus());
            }
        }
        LitePal.useDefault();
    }

    public void deleteTodos(List<Todo> todoList) {
        if (todoList.size() <= 0) {
            return;
        }
        TodoDbManager todoDbManager = TodoDbManager.getInstance();
        //delete local
        //LitePal.deleteAll(Todo.class, "status = ?", "-1");
        LitePal.use(litePalDB);
        //delete web
        for (Todo todo : todoList) {
            Todo tempTodo = new Todo();
            tempTodo.setStatus(-1);
            todoDbManager.updateTodoByTime(todo.getCreateTime(), tempTodo);
            //LitePal.deleteAll(Todo.class, "createTime = ?", Long.toString(todo.getCreateTime()));
        }
        LitePal.useDefault();
    }
}
