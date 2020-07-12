package com.example.note;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.bigkoo.pickerview.builder.TimePickerBuilder;
import com.bigkoo.pickerview.listener.OnTimeSelectListener;
import com.bigkoo.pickerview.view.TimePickerView;
import com.example.note.adapter.TodoAdapter;
import com.example.note.db.Todo;
import com.example.note.service.AlarmService;
import com.example.note.util.ThingsReminder;
import com.example.note.util.TodoDbManager;
import com.example.note.util.XunFeiEngine;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechError;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link TodoFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link TodoFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TodoFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;


    private EditText editText;
    private boolean isOldTodo = false;
    private Todo oldTodo;

    private boolean isListening = false;
    private TodoAdapter todoAdapter;
    private TodoDbManager dbManager;
    private List<Todo> todoList;
    private RecyclerView recyclerView;
    private static final String TAG = "TodoFragment";

    private OnFragmentInteractionListener mListener;

    public TodoFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment TodoFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static TodoFragment newInstance(String param1, String param2) {
        TodoFragment fragment = new TodoFragment();
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
        Log.d(TAG, "onResume: ");
        refreshTodos();
        refreshAdapter(todoList);
        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_todo, container, false);

        SearchView searchView = rootView.findViewById(R.id.todo_search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            //单击搜索按钮时激发该方法
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            //用户输入字符时激发该方法
            @Override
            public boolean onQueryTextChange(String newText) {
                refreshAdapter(filter(todoList, newText));
                //Log.d(TAG, "onQueryTextChange: " + newText);
                return false;
            }
        });

        searchView.setOnCloseListener(() -> {
            //先获取笔记内容，再刷新显示
            refreshTodos();
            refreshAdapter(todoList);
            return false;
        });

        dbManager = TodoDbManager.getInstance();
        View popupView = View.inflate(getActivity(), R.layout.todo_edit_popup_window, null);
        //设置空白的背景色
        //final WindowManager.LayoutParams lp = getWindow().getAttributes();
        PopupWindow popupWindow  = new PopupWindow(popupView);
        //ImageView micImage = popupView.findViewById(R.id.iv_pro);
        //TextView recordingTime = popupView.findViewById(R.id.recording_time);

        editText = popupView.findViewById(R.id.todo_edit_text);

        Button remindButton = popupView.findViewById(R.id.remind_button);
        Button calendarRemindButton = popupView.findViewById(R.id.calendar_remind_button);
        TextView remindTextView = popupView.findViewById(R.id.remind_time);
        long remindTime = 0;
        calendarRemindButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //提前保存编辑的todo
                if (!isOldTodo) {
                    Todo todo = new Todo();
                    todo.setContent(editText.getText().toString());
                    todo.setUpdatedTime(System.currentTimeMillis());
                    todo.setUpdateTime(dateToString(System.currentTimeMillis()));
                    todo.setTimeRemind(-1);
                    todo.setCreateTime(System.currentTimeMillis());
                    dbManager.addTodo(todo);
                    isOldTodo = true;
                    oldTodo = todo;
                }
                ThingsReminder.OpenCalendar(getActivity(), oldTodo.getContent());
            }
        });
        remindButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //提前保存编辑的todo
                if (!isOldTodo) {
                    Todo todo = new Todo();
                    todo.setContent(editText.getText().toString());
                    todo.setUpdatedTime(System.currentTimeMillis());
                    todo.setUpdateTime(dateToString(System.currentTimeMillis()));
                    todo.setTimeRemind(-1);
                    todo.setCreateTime(System.currentTimeMillis());
                    dbManager.addTodo(todo);
                    isOldTodo = true;
                    oldTodo = todo;
                }

                if ("取消提醒".equals(remindButton.getText().toString())) {
                    remindButton.setText("提醒我");
                    remindTextView.setText("");

                    Todo tempTodo = new Todo();
                    tempTodo.setTimeRemind(-1);
                    tempTodo.setStatus(1);
                    dbManager.updateTodo(oldTodo.getId(), tempTodo);

                    Intent intentForService= new Intent(getActivity(), AlarmService.class);
                    intentForService.putExtra("cancel", "cancel"); //(key,value)
                    intentForService.putExtra("note", oldTodo.getId());
                    intentForService.putExtra("type","todo");
                    getActivity().startService(intentForService);
                    return;
                }
                TimePickerView timePickerView = new TimePickerBuilder(getActivity(), new OnTimeSelectListener() {
                    @Override
                    public void onTimeSelect(Date date, View v) {

                        if (date.getTime() < System.currentTimeMillis()) {
                            Toast.makeText(getActivity(), "设置的提醒时间不能早于现在", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        remindButton.setText("取消提醒");

                        String remindTimeStr = dateToString(date.getTime());
                        String currentYear = Calendar.getInstance().get(Calendar.YEAR) + "年";
                        if (remindTimeStr.contains(currentYear)) {
                            remindTimeStr = remindTimeStr.replace(currentYear,"");
                        }
                        remindTextView.setText("提醒时间：" + remindTimeStr);

                        Todo tempTodo = new Todo();
                        tempTodo.setTimeRemind(date.getTime());
                        tempTodo.setStatus(1);
                        dbManager.updateTodo(oldTodo.getId(), tempTodo);

                        Intent intentForService = new Intent(getActivity(), AlarmService.class);
                        intentForService.putExtra("date", date.getTime()); //(key,value)
                        intentForService.putExtra("note", oldTodo.getId());
                        intentForService.putExtra("type","todo");
                        getActivity().startService(intentForService);
                        Log.d("timePickerView", "onTimeSelect: " + date);
                    }
                }) .setType(new boolean[]{true, true, true, true, true, true})// 默认全部显示
                        .isDialog(true)//是否显示为对话框样式
                        .build();
                timePickerView.show();
            }
        });
        //editText.requestFocus();
        popupWindow.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        popupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        //popupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        //防止虚拟软键盘被弹出菜单遮住
        popupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.WHITE));//(new ColorDrawable(getResources().getColor(R.color.color_3)));//(new ColorDrawable(Color.WHITE));   //背景色
        popupWindow.setFocusable(true);

        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                restorePopupWindowBackground();
                //内容为空
                if (editText.getText().toString().trim().length() == 0) {
                    //如果清空旧todo代表删除了
                    if (isOldTodo) {
                        //如果有提醒要取消
                        if (oldTodo.getTimeRemind() > System.currentTimeMillis()) {
                            Intent intentForService= new Intent(getActivity(), AlarmService.class);
                            intentForService.putExtra("cancel", "cancel"); //(key,value)
                            intentForService.putExtra("note", oldTodo.getId());
                            intentForService.putExtra("type","todo");
                            getActivity().startService(intentForService);
                        }

                        if (oldTodo.getStatus() == 0) {
                            dbManager.deleteTodo(oldTodo);
                        } else {
                            oldTodo.setStatus(-1);
                            oldTodo.save();
                        }
                    }

                    isOldTodo = false;
                    oldTodo = null;
                    refreshTodos();
                    refreshAdapter(todoList);
                    editText.setText("");
                    remindTextView.setText("");
                    return;
                }

                //如果旧内容没有更改，直接退出
                if (isOldTodo && oldTodo.getContent().equals(editText.getText().toString())) {
                    isOldTodo = false;
                    oldTodo = null;
                    editText.setText("");
                    remindTextView.setText("");
                    //为了更新提醒状态的颜色
                    refreshTodos();
                    refreshAdapter(todoList);
                    return;
                }

                Todo todo = new Todo();
                todo.setContent(editText.getText().toString());
                todo.setUpdatedTime(System.currentTimeMillis());
                todo.setUpdateTime(dateToString(System.currentTimeMillis()));

                if (isOldTodo) {
                    todo.setStatus(1);
                    dbManager.updateTodo(oldTodo.getId(), todo);
                    isOldTodo = false;
                } else {
                    todo.setTimeRemind(-1);
                    todo.setCreateTime(System.currentTimeMillis());
                    dbManager.addTodo(todo);
                }

                refreshTodos();
                refreshAdapter(todoList);
                editText.setText("");
                remindTextView.setText("");
            }
        });
       // popupWindow.setTouchable(false);


        //popupWindow.update();


        //启动到笔记编辑页面
        FloatingActionButton fab = rootView.findViewById(R.id.todo_edit_fab);
        fab.setOnClickListener((view) -> {
            setPopupWindowBackground();
            remindButton.setText("提醒我");
            popupWindow.showAtLocation(View.inflate(getActivity(),R.layout.fragment_todo,null), Gravity.BOTTOM,0,0);
            editText.requestFocus();
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(0, InputMethodManager.SHOW_IMPLICIT);

            //Intent intent = new Intent(getActivity(), NoteNewActivity.class);
           // intent.putExtra("groupName", groupName);
            //intent.putExtra("NewOrEdit","New");
            //startActivity(intent);
        });

        //启动到录音界面
        PopupWindow popupWindowForVoice  = new PopupWindow();
        XunFeiEngine xunFeiEngine = new XunFeiEngine(getActivity());
        FloatingActionButton fabVoice = rootView.findViewById(R.id.todo_voice_fab);
        fabVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isListening) {
                    isListening = true;
                    startRecord(xunFeiEngine, popupWindowForVoice);
                } else {
                    isListening = false;
                    popupWindowForVoice.dismiss();
                    restorePopupWindowBackground();
                    xunFeiEngine.stop();
                }
            }
        });


        refreshTodos();

        todoAdapter = new TodoAdapter(todoList, getActivity());

        //查看笔记
        todoAdapter.setOnItemClickListener((view, todo) -> {
            isOldTodo = true;
            oldTodo = todo;
            editText.setText(todo.getContent());
            editText.setSelection(todo.getContent().length());
            if (todo.getTimeRemind() > 0) {
                remindButton.setText("取消提醒");
                String remindTimeStr = dateToString(todo.getTimeRemind());
                String currentYear = Calendar.getInstance().get(Calendar.YEAR) + "年";
                if (remindTimeStr.contains(currentYear)) {
                    remindTimeStr = remindTimeStr.replace(currentYear,"");
                }
                remindTextView.setText("提醒时间：" + remindTimeStr);
            } else {
                remindButton.setText("提醒我");
            }
            setPopupWindowBackground();
            popupWindow.showAtLocation(View.inflate(getActivity(),R.layout.fragment_todo,null), Gravity.BOTTOM,0,0);
//            Intent intent = new Intent(getActivity(), NoteActivity.class);
//            Bundle bundle = new Bundle();
//            bundle.putSerializable("Note",note);     //序列化,bundle在这里感觉多余
//            intent.putExtra("data",bundle);
//            startActivity(intent);
        });

        todoAdapter.setOnItemLongClickListener((view, todo) -> {
            //弹出一个dialog，用用户选择是否删除
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            AlertDialog alertDialog = builder.setTitle("系统提示：")
                    .setMessage("确定要永久删除该Todo吗？")
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .setPositiveButton("确定",(dialog, which) -> {
                        //如果有提醒要取消
                        if (todo.getTimeRemind() > System.currentTimeMillis()) {
                            Intent intentForService= new Intent(getActivity(), AlarmService.class);
                            intentForService.putExtra("cancel", "cancel"); //(key,value)
                            intentForService.putExtra("note", todo.getId());
                            intentForService.putExtra("type","todo");
                            getActivity().startService(intentForService);
                        }
                        if (todo.getStatus() == 0) {
                            dbManager.deleteTodo(todo);
                        } else {
                            todo.setStatus(-1);
                            todo.save();
                        }

                        refreshTodos();
                        refreshAdapter(todoList);
                    }).create();
            alertDialog.show();
        });

        recyclerView = rootView.findViewById(R.id.todo_recycle_view);

        //根据用户的喜好设置 来刷新 LayoutManager，完成实时刷新
        recyclerView.setAdapter(todoAdapter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);


        return rootView;
    }

    public void refreshDisplay() {
        refreshTodos();
        refreshAdapter(todoList);
    }

    private String dateToString(long ms) {
        Date date = new Date(ms);
        String strDate = "";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault());
        strDate = simpleDateFormat.format(date);
        return strDate;
    }

    //供搜索便签调用
    private LinkedList<Todo> filter(List<Todo> todoList, String text){
        LinkedList<Todo> filterString = new LinkedList<>();

        for(Todo note : todoList){
            if(note.getContent().contains(text)){
                filterString.add(note);
            }
        }
        return filterString;
    }

    private void refreshAdapter(List<Todo> todoSet){
        //这里应该是 输入一个组名 刷新这个组名下的 notes
        //刷新 adapter中的数据
        if (todoAdapter != null) {
            todoAdapter.setTodo(todoSet);
        }
    }

    // 根据组名groupName 刷新数据  notes 对象 ，由于groupName的变化，或者其他增删导致 数据变化 ,合理并不会对搜索框的过滤刷新
    private void refreshTodos(){
        if (dbManager == null) {
            return;
        }
        todoList = dbManager.getAll();
        Log.d(TAG, "refreshNotes: "+todoList.size());
    }

    private void startRecord(XunFeiEngine xunFeiEngine, PopupWindow popupWindow){
        View popupView = View.inflate(getActivity(), R.layout.popup_window, null);
        //设置空白的背景色
        popupWindow.setContentView(popupView);
        ImageView micImage = popupView.findViewById(R.id.recording_img);
        setPopupWindowBackground();
        popupWindow.setWidth(500);
        popupWindow.setHeight(500);
        popupWindow.showAtLocation(popupView, Gravity.CENTER,0,0);

        //String fileName = System.currentTimeMillis() + ".wav";
        xunFeiEngine.setParam(null);


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
                    //因为没有录音，没有文字就退出
                    if (content.toString().trim().length() == 0) {
                        return;
                    }
                    // TODO 最后的结果
                    addTodo(content);
                    refreshTodos();
                    refreshAdapter(todoList);
                    Log.d(TAG, "onResult: 执行了");
                }
            }

            @Override
            public void onError(SpeechError speechError) {
                xunFeiEngine.showTip(speechError.getPlainDescription(true));
                popupWindow.dismiss();
                restorePopupWindowBackground();
            }

            @Override
            public void onEvent(int i, int i1, int i2, Bundle bundle) {

            }
        });

    }

    private void addTodo(StringBuilder mContent) {
        TodoDbManager dbManager = TodoDbManager.getInstance();

        Todo todo = new Todo();
        todo.setContent(mContent.toString());
        todo.setUpdatedTime(System.currentTimeMillis());
        todo.setUpdateTime(dateToString(System.currentTimeMillis()));
        todo.setTimeRemind(-1);
        todo.setCreateTime(System.currentTimeMillis());
        dbManager.addTodo(todo);
        Log.d("mContent:", "用户输入的内容是" + mContent);
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


    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);/*
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
