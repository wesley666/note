package com.example.note;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.example.note.adapter.NoteAdapter;
import com.example.note.db.MediaForNote;
import com.example.note.db.Note;
import com.example.note.db.Todo;
import com.example.note.service.AlarmService;
import com.example.note.util.DeleteMedia;
import com.example.note.util.NoteDbManager;
import com.example.note.util.SynWithWebDav;
import com.example.note.util.TodoDbManager;
import com.example.note.util.XunFeiEngine;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechError;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.listener.OnRefreshListener;
import com.thegrizzlylabs.sardineandroid.DavResource;
import com.thegrizzlylabs.sardineandroid.Sardine;
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine;

import org.litepal.LitePal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link NoteFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link NoteFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NoteFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;


    private NoteAdapter noteAdapter;
    private NoteAdapter noteAdapterForListMode;
    private String groupName = "全部";
    private NoteDbManager dbManager;
    private List<Note> notes;
    private RecyclerView recyclerView;
    private boolean isListening = false;

    private FloatingActionButton fab;
    private FloatingActionButton fabVoice;
    private static final String TAG = "NoteFragment";

    public NoteFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment NoteFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static NoteFragment newInstance(String param1, String param2) {
        NoteFragment fragment = new NoteFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshNotes();
        refreshAdapter(notes);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_note, container, false);

        RefreshLayout refreshLayout = rootView.findViewById(R.id.smart_refresh);
        //设置 Header 为 贝塞尔雷达 样式
        //refreshLayout.setRefreshHeader(new BezierRadarHeader(getActivity()).setEnableHorizontalDrag(true));
        //设置 Footer 为 球脉冲 样式
       // refreshLayout.setRefreshFooter(new BallPulseFooter(getContext()).setSpinnerStyle(SpinnerStyle.Scale));
        //refreshLayout.setEnableLoadMore(true);//是否启用上拉加载功能
        refreshLayout.setDisableContentWhenRefresh(true);//是否在刷新的时候禁止列表的操作
        refreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(@NonNull RefreshLayout refreshLayout) {
                //通过getSystemService()方法得到connectionManager这个系统服务类，专门用于管理网络连接
                ConnectivityManager connectionManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connectionManager.getActiveNetworkInfo();
                if (networkInfo == null) {
                    Toast.makeText(getActivity(), "网络不可用", Toast.LENGTH_SHORT).show();
                    refreshLayout.finishRefresh();
                    return;
                }

                SharedPreferences cloudService = getActivity().getSharedPreferences("CloudService", MODE_PRIVATE);
                String userName = cloudService.getString("UserName", "");
                String userPassword = cloudService.getString("UserPassword", "");
                if (userName.equals("") || userPassword.equals("")) {
                    Toast.makeText(getActivity(), "同步失败，请检查账号密码是否正确", Toast.LENGTH_SHORT).show();
                    refreshLayout.finishRefresh();
                    return;
                }

                ((MainActivity)getActivity()).setTouchEventFlag(false);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Sardine sardine = new OkHttpSardine();
                            sardine.setCredentials(userName, userPassword);
                            sardine.list("https://dav.jianguoyun.com/dav/");//如果是目录一定别忘记在后面加上一个斜杠
                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.d(TAG, "run: error");
                            getActivity().runOnUiThread(() -> {
                                refreshLayout.finishRefresh();
                                ((MainActivity) getActivity()).setTouchEventFlag(true);
                                Toast.makeText(getActivity(), "同步失败，请检查账号密码是否正确", Toast.LENGTH_SHORT).show();
                            });
                            return;
                        }

                        SynWithWebDav synWithWebDav = new SynWithWebDav(userName, userPassword);
                        List<MediaForNote> mediaForNotes = LitePal.where("status = ?", "0").find(MediaForNote.class);
                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                for (MediaForNote mediaForNote : mediaForNotes) {
                                    Log.d(TAG, "run: mediaForNote" + mediaForNote.getMediaName());
                                    String filePath;
                                    if (mediaForNote.isPhoto()) {
                                        filePath = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/";
                                    } else {
                                        filePath = getActivity().getExternalFilesDir(Environment.DIRECTORY_RINGTONES) + "/";
                                    }
                                    long ret = synWithWebDav.uploadFile("media", mediaForNote.getMediaName(), filePath + mediaForNote.getMediaName());
                                    if (ret > 0) {
                                        mediaForNote.setStatus(2);
                                        mediaForNote.save();
                                    }
                                }
                            }
                        });
                        thread.start();

                        SharedPreferences prefs = getActivity().getSharedPreferences("RecentSynTime", MODE_PRIVATE);
                        long recentSynTime = prefs.getLong("RecentSynTime", 0);
                        Log.d(TAG, "run: recentSynTime" + recentSynTime);


                        List<DavResource> resourceList = synWithWebDav.getResources("database");
                        long modifiedTime = 0;
                        for (DavResource davResource : resourceList) {
                            Log.d(TAG, "run: " + davResource.getDisplayName() + davResource.getModified());
                            if (davResource.getDisplayName().contains("noteBackup.db")) {
                                modifiedTime = davResource.getModified().getTime();
                            }
                        }

                        boolean isSynSuccess = false;
                        //获取最新云文件
                        if (modifiedTime > recentSynTime && synWithWebDav.isFileExist("noteBackup.db", "database")) {
                            InputStream inputStream = synWithWebDav.getFile("noteBackup.db", "database");
                            try {
                                Log.d(TAG, "run: " + inputStream.toString());
                                byte[] bytes = new byte[1024];
                                //getActivity().openFileOutput("noteBackup.db", MODE_PRIVATE);
                                File dir = new File(getActivity().getExternalFilesDir("") + "/databases/");
                                dir.mkdirs();
                                FileOutputStream outStream = new FileOutputStream(new File(dir, "noteBackup.db"));
                                int index;
                                while ((index = inputStream.read(bytes)) != -1) {
                                    Log.d(TAG, "run: " + index);
                                    outStream.write(bytes, 0, index);
                                    outStream.flush();
                                }
                                outStream.close();
                                inputStream.close();
                                isSynSuccess = true;
                            } catch (Exception e) {
                                e.printStackTrace();
                                isSynSuccess = false;
                            }
                        }
                        //云文件有更新或者没有更新,网络错误将立即返回
                        if (isSynSuccess || modifiedTime <= recentSynTime) {
                            if (isSynSuccess) {
                                synWithWebDav.updateLocalNotes(recentSynTime);
                                synWithWebDav.updateLocalTodos(recentSynTime);
                            }

                            NoteDbManager noteDbManager = NoteDbManager.getInstance();
                            TodoDbManager todoDbManager = TodoDbManager.getInstance();

                            List<Note> noteList = LitePal.where("status = ? or status = ?", "0", "1").find(Note.class);
                            List<Todo> todoList = LitePal.where("status = ? or status = ?", "0", "1").find(Todo.class);
                            synWithWebDav.updateNotes(noteList);
                            synWithWebDav.updateTodos(todoList);

                            List<Note> deleteNoteList = noteDbManager.getNoteByStatus(-1);
                            List<Todo> deleteTodoList = todoDbManager.getTodoByStatus(-1);
                            synWithWebDav.deleteNotes(deleteNoteList);
                            synWithWebDav.deleteTodos(deleteTodoList);

                            File file = new File(getActivity().getExternalFilesDir("") + "/databases/", "noteBackup.db");
                            Log.d(TAG, "run: file" + file.lastModified());
                            if (file.lastModified() > recentSynTime) {
                                Log.d(TAG, "run: " + getActivity().getFilesDir() + " " + getActivity().getExternalFilesDir(""));
                                long ret = synWithWebDav.uploadFile("database", "noteBackup.db", getActivity().getExternalFilesDir("") + "/" + "databases/noteBackup.db");
                                if (ret > 0) {
                                    SharedPreferences.Editor editor = prefs.edit();
                                    editor.putLong("RecentSynTime", ret);
                                    editor.apply();
                                    for (Note note : deleteNoteList) {
                                        DeleteMedia.deleteMedia(getActivity(), note.getContent());
                                        noteDbManager.deleteNote(note);
                                    }
                                    //LitePal.deleteAll(Note.class, "status = ?", "-1");
                                    LitePal.deleteAll(Todo.class, "status = ?", "-1");

                                } else { //如果网络出错，而下次同步的时候刚好又获取了最新的云文件，那么之前存储的数据将丢失，所以这里要补救，这里失败概率低
                                    for (Note note : noteList) {
                                        note.setStatus(1);
                                        note.save();
                                    }
                                    for (Todo todo : todoList) {
                                        todo.setStatus(1);
                                        todo.save();
                                    }
                                    try {
                                        thread.join();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            refreshNotes();
                                            refreshAdapter(notes);
                                            refreshLayout.finishRefresh();
                                            ((MainActivity) getActivity()).setTouchEventFlag(true);
                                            Toast.makeText(getActivity(), "同步失败", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                    return;
                                }
                            }
                        } else {
                            try {
                                thread.join();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    refreshLayout.finishRefresh();
                                    ((MainActivity) getActivity()).setTouchEventFlag(true);
                                    Toast.makeText(getActivity(), "同步失败", Toast.LENGTH_SHORT).show();
                                }
                            });
                            return;
                        }


                        try {
                            thread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                refreshNotes();
                                refreshAdapter(notes);
                                refreshLayout.finishRefresh();
                                ((MainActivity) getActivity()).setTouchEventFlag(true);
                                Toast.makeText(getActivity(), "同步成功", Toast.LENGTH_SHORT).show();
                            }
                        });

                    }
                }).start();
            }
        });

//        SwipeRefreshLayout swipeRefreshLayout = rootView.findViewById(R.id.swipe_refresh);
//        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
//            @Override
//            public void onRefresh() {



                        //File fileDirectory = C.FilePath.getPicturesDirectory();//即：/storage/emulated/0/Android/data/包名/files/Pictures/
                       // file = new File(getActivity().getFilesDir(), "noteBackup.db");

//                        boolean doNeedUpdateNotes = false;
//                        Log.d(TAG, "run: " + recentSynTime);
//                        SynWithBmob synWithBmob = SynWithBmob.getInstance();
//                        //long recentSyn = recentSynTime;
//                        synWithBmob.updateNotesFromBmob(recentSynTime);
//
//                        NoteDbManager noteDbManager = NoteDbManager.getInstance();
//                        //本地更新
//                        List<Note> noteList = noteDbManager.getNoteByStatus(1);
//                        Log.d(TAG, "run: " + noteList.size());
//                        synWithBmob.updateNotes(noteList);
//
//                        //本地新增
//                        noteList = noteDbManager.getNoteByStatus(0);
//                        Log.d(TAG, "run: " + noteList.size());
//                        synWithBmob.addNotes(noteList);
//
//                        noteList = noteDbManager.getNoteByStatus(-1);
//                        synWithBmob.deleteNotes(noteList);
//
//                        while (!synWithBmob.isUpdateNotesFromBmobSuccess()) {
//                            try {
//                                Thread.sleep(1000);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                        synWithBmob.setUpdateNotesFromBmobSuccess(false);

//
//                        while (!synWithBmob.isUpdateNotesSuccess() || !synWithBmob.isAddNotesSuccess()) {
//                            try {
//                                Thread.sleep(1000);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                        synWithBmob.setUpdateNotesSuccess(false);
//                        synWithBmob.setAddNotesSuccess(false);
//                        //SharedPreferences prefs = getActivity().getSharedPreferences("RecentSynTime",MODE_PRIVATE);
//
//                    }
//                }).start();
//            }
//        });

        SearchView searchView = rootView.findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            //单击搜索按钮时激发该方法
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            //用户输入字符时激发该方法
            @Override
            public boolean onQueryTextChange(String newText) {
                refreshAdapter(search(notes, newText));
                //Log.d(TAG, "onQueryTextChange: " + newText);
                return false;
            }
        });

        searchView.setOnCloseListener(() -> {
            //先获取笔记内容，再刷新显示
            refreshNotes();
            refreshAdapter(notes);
            return false;
        });

        //启动到笔记编辑页面
        fab = rootView.findViewById(R.id.fab);
        fab.setOnClickListener((view) -> {
            Intent intent = new Intent(getActivity(), NoteNewActivity.class);
            intent.putExtra("groupName", groupName);
            intent.putExtra("NewOrEdit","New");
            startActivity(intent);
        });

        //启动到录音界面
        XunFeiEngine xunFeiEngine = new XunFeiEngine(getActivity());
        PopupWindow popupWindow  = new PopupWindow();
        fabVoice = rootView.findViewById(R.id.fab_voice);
        fabVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isListening) {
                    isListening = true;
                    startRecord(xunFeiEngine, popupWindow);
                } else {
                    isListening = false;
                    popupWindow.dismiss();
                    restorePopupWindowBackground();
                    xunFeiEngine.stop();
                }

                //Intent intent = new Intent(getActivity(), NoteVoiceActivity.class);
                //startActivity(intent);
                /*
                checkSoIsInstallSucceed();
                mIatResults.clear();
                // 设置参数
                setParam();
                //初始化识别无UI识别对象
                //使用SpeechRecognizer对象，可根据回调消息自定义界面；
                // 不显示听写对话框
                //开始识别，并设置监听器
                int ret = mIat.startListening(mRecognizerListener);
                if (ret != ErrorCode.SUCCESS) {
                    showTip("听写失败,错误码：" + ret);
                } else {
                    //showTip(getString(R.string.text_begin));
                    showTip("请开始说话…");
                } */
            }
        });

        dbManager = NoteDbManager.getInstance();
        refreshNotes();

        noteAdapter = new NoteAdapter(notes, getActivity(), R.layout.note_item);

        noteAdapterForListMode = new NoteAdapter(notes, getActivity(), R.layout.note_item_list_mode);

        //查看笔记
        noteAdapter.setOnItemClickListener((view, note) -> {
            Intent intent = new Intent(getActivity(), NoteActivity.class);
            //Bundle bundle = new Bundle();
            //bundle.putSerializable("Note",note);     //序列化,bundle在这里感觉多余
            //intent.putExtra("data",bundle);
            intent.putExtra("noteId", note.getId());
            startActivity(intent);
        });

        noteAdapter.setOnItemLongClickListener((view, note) -> {
            //弹出一个dialog，用用户选择是否删除
           popupDialog(note);
        });

        //查看笔记
        noteAdapterForListMode.setOnItemClickListener((view, note) -> {
            Intent intent = new Intent(getActivity(),NoteActivity.class);
            //Bundle bundle = new Bundle();
            //bundle.putSerializable("Note",note);     //序列化,bundle在这里感觉多余
            //intent.putExtra("data",bundle);
            intent.putExtra("noteId", note.getId());
            startActivity(intent);
        });

        noteAdapterForListMode.setOnItemLongClickListener((view, note) -> {
            //弹出一个dialog，用用户选择是否删除
            popupDialog(note);
        });

        recyclerView = rootView.findViewById(R.id.recycle_view);

        //根据用户的喜好设置 来刷新 LayoutManager，完成实时刷新
        //用户喜好  首页面 notes布局显示  第一次默认 宫格模式
        SharedPreferences prefs = getActivity().getSharedPreferences("Setting", MODE_PRIVATE);
        String showNotesModel = prefs.getString("ShowNotesModel", "宫格模式");
        refreshLayoutManager(showNotesModel);

        return rootView;
    }

    private void popupDialog(Note note) {
        //弹出一个dialog，用用户选择是否删除
        String message = "确定要将该便签放到回收站吗？";
        String neutralMessage = "放入回收站";
        if ("回收站".equals(groupName)) {
            message = "确定要将该便签恢复吗？";
            neutralMessage = "恢复";
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        AlertDialog alertDialog = builder.setTitle("系统提示：")
                .setMessage(message)
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setNeutralButton(neutralMessage, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Note tempNote = new Note();
                        String tempName = "回收站";
                        if ("回收站".equals(groupName)) {
                            tempName = "未分组";
                        }
                        tempNote.setGroupName(tempName);
                        tempNote.setStatus(1);  //本地更新
                        dbManager.updateNote(note.getId(), tempNote);

                        refreshNotes();
                        refreshAdapter(notes);
                    }
                })
                .setPositiveButton("永久删除",(dialog, which) -> {
                    //如果没有同步直接删除
                    if (note.getStatus() != 2) {
                        DeleteMedia.deleteMedia(getActivity(), note.getContent());
                        dbManager.deleteNote(note);
                    } else {
                        Note tempNote = new Note();
                        tempNote.setStatus(-1);  //标记删除
                        dbManager.updateNote(note.getId(), tempNote);
                    }
                    //如果有提醒就要取消
                    if (note.getTimeRemind() > System.currentTimeMillis()) {
                        Intent intentForService= new Intent(getActivity(), AlarmService.class);
                        intentForService.putExtra("cancel", "cancel"); //(key,value)
                        intentForService.putExtra("note", note.getId());
                        intentForService.putExtra("type", "note");
                        getActivity().startService(intentForService);
                    }

                    refreshNotes();
                    refreshAdapter(notes);
                }).create();
        alertDialog.show();
    }

    //供搜索便签调用
    private LinkedList<Note> search(List<Note> noteList, String queryStr){
        LinkedList<Note> filterResults = new LinkedList<>();
        for(Note note : noteList){
            if (note.getContent().contains(queryStr)) {
                filterResults.add(note);
            }
        }
        return filterResults;
    }

    private void refreshAdapter(List<Note> noteSet){
        //这里应该是 输入一个组名 刷新这个组名下的 notes
        //刷新 adapter中的数据
        noteAdapter.setNotes(noteSet);
        noteAdapterForListMode.setNotes(noteSet);
    }

    // 根据组名groupName 刷新数据  notes 对象 ，由于groupName的变化，或者其他增删导致 数据变化 ,合理并不会对搜索框的过滤刷新
    private void refreshNotes(){
        if(groupName.equals("全部")){
            //不包括回收站
            notes = dbManager.getAll();
        } else {
            notes = dbManager.getNotesByGroup(groupName);
        }
        Log.d(TAG, "refreshNotes: "+notes.size());
    }

    private void startRecord(XunFeiEngine xunFeiEngine, PopupWindow popupWindow) {
        View popupView = View.inflate(getActivity(), R.layout.popup_window, null);
        //设置空白的背景色
        final WindowManager.LayoutParams lp = getActivity().getWindow().getAttributes();
        popupWindow.setContentView(popupView);
        ImageView micImage = popupView.findViewById(R.id.recording_img);
        setPopupWindowBackground();
        popupWindow.setWidth(500);
        popupWindow.setHeight(500);
        popupWindow.showAtLocation(popupView, Gravity.CENTER,0,0);

        String fileName = System.currentTimeMillis() + ".wav";
        xunFeiEngine.setParam(fileName);

        //记录录音
        MediaForNote mediaForNote = new MediaForNote();
        mediaForNote.setMediaName(fileName);
        StringBuilder content = new StringBuilder();

        xunFeiEngine.mIat.startListening(new RecognizerListener() {
            @Override
            public void onVolumeChanged(int volume, byte[] bytes) {
                //根据分贝值来设置录音时话筒图标的上下波动
                //ltime=time;
                Log.d(TAG, "onVolumeChanged: "+volume);
                micImage.getDrawable().setLevel((int) ( 3000 + 7000 / 40 * volume));
                //recordingTime.setText(TimeUtils.getDateCoverString(time));

            }

            @Override
            public void onBeginOfSpeech() {
                // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
                xunFeiEngine.showTip("开始说话");
                //editText.append("\n");
            }

            @Override
            public void onEndOfSpeech() {
                // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
                xunFeiEngine.showTip("结束说话");
                restorePopupWindowBackground();
                popupWindow.dismiss();
            }

            @Override
            public void onResult(RecognizerResult recognizerResult, boolean b) {
                content.append(xunFeiEngine.printResult(recognizerResult));
                xunFeiEngine.clearIatResults();
                if (b) {
                    // TODO 最后的结果
                    content.append("<voice src='" + fileName + "'/>");
                    addNote(content, mediaForNote);
                    refreshNotes();
                    refreshAdapter(notes);
                    Log.d(TAG, "onResult: 执行了");
                    //editText.append("<voice src='" + fileName + "'/>");
                    //editText.append("\uD83C\uDFA4");
                    //editText.append("\n");
                }
            }

            @Override
            public void onError(SpeechError speechError) {
                xunFeiEngine.showTip(speechError.getPlainDescription(true));
                restorePopupWindowBackground();
                popupWindow.dismiss();
            }

            @Override
            public void onEvent(int i, int i1, int i2, Bundle bundle) {

            }
        });

    }

    private void setPopupWindowBackground() {
        WindowManager.LayoutParams layoutParams = getActivity().getWindow().getAttributes();
        layoutParams.alpha = 0.5f; //设置透明度
        //layoutParams.dimAmount = 0f;
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getActivity().getWindow().setAttributes(layoutParams);
    }

    private void restorePopupWindowBackground() {
        WindowManager.LayoutParams layoutParams = getActivity().getWindow().getAttributes();
        layoutParams.alpha = 1f;
        //layoutParams.dimAmount = 0f;
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getActivity().getWindow().setAttributes(layoutParams);
    }

    private void addNote(StringBuilder mContent, MediaForNote mediaForNote) {
        NoteDbManager dbManager = NoteDbManager.getInstance();
        
        int i;
        String tempContent = mContent.toString().replaceAll("<voice src='(.*?)'/>","");
        //拿14个连续字符做标题
        String title;
        String subContent;
        for(i = 0; i < tempContent.length(); i++){
            if (tempContent.charAt(i) != ' ' && tempContent.charAt(i) != '\n'){
                break;
            }
        }
        if (i + 14 < tempContent.length()) {
            title = tempContent.substring(i, i + 14);
            subContent = tempContent.substring(i + 14); //剩下内容当副标题，显示不全有省略号
        } else {
            title = tempContent.substring(i);
            subContent = "";
        }

        Log.d("mContent:", "用户输入的内容是" + mContent);

        Note note = new Note();
        note.setTitle(title);
        note.setSubContent(subContent);
        note.setContent(mContent.toString());
        note.setUpdateTime(dateToString(System.currentTimeMillis()));
        note.setUpdatedTime(System.currentTimeMillis());
        if ("全部".equals(groupName)) {
            note.setGroupName("未分组");
        } else {
            note.setGroupName(groupName);
        }
        note.setCreateTime(dateToString(System.currentTimeMillis()));
        note.setCreatedTime(System.currentTimeMillis());
        dbManager.addNote(note);


        //保存图片或者语音到数据库
        if (note.getContent().contains(mediaForNote.getMediaName())) {
            mediaForNote.setNoteCreateTime(note.getCreatedTime());
            mediaForNote.save();
        }

    }

    private String dateToString(long ms) {
        Date date = new Date(ms);
        String strDate = "";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault());
        strDate = simpleDateFormat.format(date);
        return strDate;
    }

    public void refreshLayoutManager(String showNotesModel) {
        if (showNotesModel.equals("列表模式")) {
            recyclerView.setAdapter(noteAdapterForListMode);
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
            recyclerView.setLayoutManager(linearLayoutManager);
        } else if (showNotesModel.equals("宫格模式")) {
            //两列，纵向排列
            recyclerView.setAdapter(noteAdapter);
            StaggeredGridLayoutManager staggeredGridLayoutManager = new StaggeredGridLayoutManager(2,StaggeredGridLayoutManager.VERTICAL);
        //    staggeredGridLayoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_NONE);
//            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
//                @Override
//                public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
//                    super.onScrollStateChanged(recyclerView, newState);
//                    staggeredGridLayoutManager.invalidateSpanAssignments();
//                }
//            });

//            ((DefaultItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
//            ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
//            recyclerView.getItemAnimator().setChangeDuration(0);
            recyclerView.setLayoutManager(staggeredGridLayoutManager);
        }
    }

    public void changedGroup(String groupName){
        this.groupName = groupName;
        if ("回收站".equals(groupName)) {
            fab.setVisibility(View.GONE);
            fabVoice.setVisibility(View.GONE);
        } else {
            fab.setVisibility(View.VISIBLE);
            fabVoice.setVisibility(View.VISIBLE);
        }
        //select 语句
        //刷新数据
        refreshNotes();
        refreshAdapter(notes);
        //notifacation
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        /*
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }*/
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
