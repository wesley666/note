package com.example.note;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.ContentLoadingProgressBar;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.SpannedString;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bigkoo.pickerview.builder.TimePickerBuilder;
import com.bigkoo.pickerview.listener.OnTimeSelectListener;
import com.bigkoo.pickerview.view.TimePickerView;
import com.example.note.db.Note;
import com.example.note.service.AlarmService;
import com.example.note.util.ContentToSpannableString;
import com.example.note.util.DeleteMedia;
import com.example.note.util.NoteDbManager;
import com.example.note.util.ThingsReminder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

//show the notes
public class NoteActivity extends AppCompatActivity {

    private static final String TAG = "NoteActivity";
    private boolean firstInFlag = false;
    private Note note = null;
    private TextView textView;
    private TextView createTimeTextView;
    private TextView remindTimeTextView;
    private String wordSizePrefs;
    private int checkedItem;
    //private AlarmManager alarmManager;
    //private PendingIntent pendingIntent;
    //private long date;

    private Menu menu;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //return super.onCreateOptionsMenu(menu);
        Log.d(TAG, "onCreateOptionsMenu: ");
        getMenuInflater().inflate(R.menu.note_show_menu, menu);
        this.menu = menu;
        if (note.getTimeRemind() > 0) {
            MenuItem menuItem = menu.findItem(R.id.show_menu_remind);
            menuItem.setTitle("取消提醒");
        }
        if (note.getGroupName().equals("回收站")) {
            MenuItem menuItem = menu.findItem(R.id.show_menu_delete);
            menuItem.setTitle("恢复笔记");
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //return super.onOptionsItemSelected(item);
        Log.d(TAG, "onOptionsItemSelected: ");
        switch (item.getItemId()) {
            case R.id.show_menu_change_group :
                final String[] groups =  new String[]{"回收站","未分组","生活","工作"};
                int cur = 1;
                if (note.getGroupName().equals("回收站")) {
                    cur = 0;
                } else if (note.getGroupName().equals("生活")) {
                    cur =2;
                } else if(note.getGroupName().equals("工作")) {
                    cur = 3;
                }
                int finalCur = cur;
                AlertDialog changeGroupAlertDialog = new AlertDialog.Builder(NoteActivity.this).setTitle("选择分组")
                        .setSingleChoiceItems(groups, cur, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (i == finalCur) {
                                    return;
                                }
                                String tempName = groups[i];
                                Note tempNote = new Note();
                                tempNote.setGroupName(tempName);
                                tempNote.setStatus(1);  //本地更新
                                NoteDbManager.getInstance().updateNote(note.getId(), tempNote);
                                note = NoteDbManager.getInstance().getNoteById(note.getId()); //更新笔记状态
                            }
                        }).create();
                changeGroupAlertDialog.show();
                break;

            case R.id.show_menu_delete :
                popupDialog();
                break;

            case R.id.show_menu_wordSize:
                final String[] wordSizes =  new String[]{"正常","大","超大"};
                AlertDialog.Builder builder = new AlertDialog.Builder(NoteActivity.this);
                AlertDialog alertDialog = builder.setTitle("选择字体大小")
                        .setSingleChoiceItems(wordSizes, checkedItem, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                wordSizePrefs = wordSizes[i];
                                float wordSize = getWordSize(wordSizePrefs);
                                //textView.setTextSize(WordSize);
                                SharedPreferences prefs = getSharedPreferences("Setting",MODE_PRIVATE);
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString("WordSize",wordSizePrefs);
                                editor.apply();         // editor.commit();
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        SpannableStringBuilder spannableStringBuilder = ContentToSpannableString.contentToSpanStr(NoteActivity.this, note.getContent(), note.getRestoreSpans());
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                //ScrollView scrollView = findViewById(R.id.scrollView);
                                                //scrollView.setVisibility(View.VISIBLE);
                                                textView.setText(spannableStringBuilder);
                                                textView.setTextSize(wordSize);
                                                //((MainActivity) getActivity()).setTouchEventFlag(true);
                                            }
                                        });
                                    }
                                }).start();
                            }
                        }).create();
                alertDialog.show();
                break;

            case R.id.show_menu_share:

                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, note.getContent().replaceAll("<img src='(.*?)'/>","[图片]").replaceAll("<voice src='(.*?)'/>","[语音]"));
                startActivity(Intent.createChooser(intent, "分享到"));
                break;

            case R.id.show_menu_date :
                //后台 service
                //计时 AlarmManager
                //发送 notification
                ThingsReminder.OpenCalendar(this, note.getTitle());
                //alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
                //Intent intent1 = new Intent(NoteActivity.this, AlarmReceiver.class);
               // intent1.putExtra("NoteContent",note.getContent());
              //  pendingIntent = PendingIntent.getBroadcast(NoteActivity.this, 0, intent1, 0);
                //setReminder();
                break;

            case R.id.show_menu_remind :
                MenuItem menuItem = menu.findItem(R.id.show_menu_remind);
                NoteDbManager noteDbManager = NoteDbManager.getInstance();

                if ("取消提醒".equals(menuItem.getTitle())) {
                    menuItem.setTitle("提醒我");
                    remindTimeTextView.setText("");

                    Note tempNote = new Note();
                    tempNote.setTimeRemind(-1);
                    tempNote.setStatus(1);
                    noteDbManager.updateNote(note.getId(), tempNote);

                    Intent intentForService= new Intent(NoteActivity.this, AlarmService.class);
                    intentForService.putExtra("cancel", "cancel"); //(key,value)
                    intentForService.putExtra("note", note.getId());
                    intentForService.putExtra("type", "note");
                    startService(intentForService);
                    break;
                }

                TimePickerView timePickerView = new TimePickerBuilder(NoteActivity.this, new OnTimeSelectListener() {
                    @Override
                    public void onTimeSelect(Date date, View v) {



                        if (date.getTime() < System.currentTimeMillis()) {
                            Toast.makeText(NoteActivity.this, "设置的提醒时间不能早于现在", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        menuItem.setTitle("取消提醒");
                        String currentYear = java.util.Calendar.getInstance().get(Calendar.YEAR) + "年";
                        remindTimeTextView.setText("提醒时间：" + dateToString(date.getTime()).replace(currentYear,""));

                        Note tempNote = new Note();
                        tempNote.setTimeRemind(date.getTime());
                        tempNote.setStatus(1);
                        noteDbManager.updateNote(note.getId(), tempNote);

                        Intent intentForService= new Intent(NoteActivity.this, AlarmService.class);
                        intentForService.putExtra("date",date.getTime());//(key,value)
                        intentForService.putExtra("note", note.getId());
                        intentForService.putExtra("type","note");
                        startService(intentForService);
                        Log.d("timePickerView", "onTimeSelect: "+date);
                    }
                }) .setType(new boolean[]{true, true, true, true, true, true})// 默认全部显示
                        .build();
                timePickerView.show();
                //后台 service
                //计时 AlarmManager
                //发送 notification

                //Intent intent1 = new Intent(NoteActivity.this, AlarmReceiver.class);
                // intent1.putExtra("NoteContent",note.getContent());
                //  pendingIntent = PendingIntent.getBroadcast(NoteActivity.this, 0, intent1, 0);
                break;
            default:
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);
        Log.d(TAG, "onCreate: ");
        //get the note from the intent
        firstInFlag = true;
        Intent intent = getIntent();
        NoteDbManager dbManager = NoteDbManager.getInstance();
        int noteId = intent.getIntExtra("noteId", 0);
        //Bundle bundle = intent.getBundleExtra("data");
        //note = (Note) bundle.getSerializable("Note");
        note = dbManager.getNoteById(noteId);
        if(note == null) {
            finish();
            return;
        }

        //字体大小默认是20dp  正常       25dp  对应 大    30dp对应超大
        SharedPreferences sharedPreferences = getSharedPreferences("Setting", MODE_PRIVATE);
        wordSizePrefs = sharedPreferences.getString("WordSize", "正常");

        Toolbar toolbarNoteShow = findViewById(R.id.toolbar_note_show);
        TextView toolbarTitle = findViewById(R.id.toolbar_title);
        toolbarTitle.setText(note.getTitle());
        toolbarNoteShow.setTitle("");
        setSupportActionBar(toolbarNoteShow);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        toolbarNoteShow.setNavigationOnClickListener((view) -> {
            finish();
        });

        createTimeTextView = findViewById(R.id.note_show_create_time);
        remindTimeTextView = findViewById(R.id.note_show_remind_time);

        String currentYear = java.util.Calendar.getInstance().get(Calendar.YEAR) + "年";
        if (note.getUpdateTime().contains(currentYear)) {
            createTimeTextView.setText(note.getUpdateTime().replace(currentYear,""));
        } else {
            createTimeTextView.setText(note.getUpdateTime());
        }
        if (note.getTimeRemind() > 0) {
            if (dateToString(note.getTimeRemind()).contains(currentYear)) {
                remindTimeTextView.setText("提醒时间：" + dateToString(note.getTimeRemind()).replace(currentYear,""));
            } else {
                remindTimeTextView.setText("提醒时间：" + dateToString(note.getTimeRemind()));
            }
        }

        textView = findViewById(R.id.TextView_showNote);
        float wordSize = getWordSize(wordSizePrefs);
        //textView .setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        String content = note.getContent();
        ContentLoadingProgressBar contentLoadingProgressBar = findViewById(R.id.loadingView);
        contentLoadingProgressBar.show();

//        LVEatBeans lvCircularRing = findViewById(R.id.LVCircularRing);
//        lvCircularRing.setEyeColor(Color.BLACK);
//        lvCircularRing.setViewColor(Color.BLACK);
//        lvCircularRing.startAnim(3500);
        new Thread(new Runnable() {
            @Override
            public void run() {
                SpannableStringBuilder spannableStringBuilder = ContentToSpannableString.contentToSpanStr(NoteActivity.this, content, note.getRestoreSpans());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        contentLoadingProgressBar.hide();
                        ScrollView scrollView = findViewById(R.id.scrollView);
                        scrollView.setVisibility(View.VISIBLE);
                        textView.setText(spannableStringBuilder);
                        textView.setTextSize(wordSize);
                        //((MainActivity) getActivity()).setTouchEventFlag(true);
                    }
                });
            }
        }).start();


        //textView.setMovementMethod(LinkMovementMethod.getInstance());
        //textView.setMovementMethod(ScrollingMovementMethod.getInstance());

        textView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();

                TextView tv = (TextView) v;
                CharSequence text = tv.getText();
                Log.d("onTouch", "onTouch: 1");
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
                    Log.d("onTouch", "onTouch: 2");
                    int x = (int) event.getX();
                    int y = (int) event.getY();

                    x -= tv.getTotalPaddingLeft();
                    y -= tv.getTotalPaddingTop();

                    x += tv.getScrollX();
                    y += tv.getScrollY();

                    Layout layout = tv.getLayout();
                    int line = layout.getLineForVertical(y);
                    int off = layout.getOffsetForHorizontal(line, x);

                    ClickableSpan[] link = ((SpannedString)text).getSpans(off, off, ClickableSpan.class);
                    if (link.length != 0) {
                        if (action == MotionEvent.ACTION_UP) {
                            // 只处理点击事件
                            link[0].onClick(tv);
                        }
                        return true;
                    }

                }
                //return Touch.onTouchEvent(tv, (Spannable) text, event);
                return false;
            }
        });

        /*textView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();

                EditText tv = (EditText) v;
                CharSequence text = tv.getText();
                if (text instanceof SpannableStringBuilder) {
                    if (action == MotionEvent.ACTION_UP) {
                        int x = (int) event.getX();
                        int y = (int) event.getY();

                        x -= tv.getTotalPaddingLeft();
                        y -= tv.getTotalPaddingTop();

                        x += tv.getScrollX();
                        y += tv.getScrollY();

                        Layout layout = tv.getLayout();
                        int line = layout.getLineForVertical(y);
                        int off = layout.getOffsetForHorizontal(line, x);

                        ClickableSpan[] link = ((SpannableStringBuilder)text).getSpans(off, off, ClickableSpan.class);
                        if (link.length != 0) {
                            link[0].onClick(tv);
                            return true;
                        } else {
                            return  false;
                            //do textview click event
                        }
                    }
                }

                return false;
            }

        });*/


        FloatingActionButton buttonNoteEdit = findViewById(R.id.button_note_edit);
        buttonNoteEdit.setOnClickListener((view) -> {
            //跳转到新建页面，编辑意味着 删除原来的， 新建一个新的，只是新的这个的content继承自旧的。
            Intent intent1 = new Intent(NoteActivity.this, NoteNewActivity.class);
            //告诉 是编辑页面 editText需要继承旧的东西
            intent1.putExtra("NewOrEdit","Edit");
            intent1.putExtra("noteId", note.getId());
            intent1.putExtra("groupName", note.getGroupName());
            startActivity(intent1);
            finish();
        });
    }

    /**
     * 当启动模式为singletop的，第二次打开这个activity的时候，就不再onCreate（），而是走onNewIntent（），这样才能获取到新的数据
     * @param intent
     */
    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(TAG, "onNewIntent: ");
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        Intent intent = getIntent();
        Log.d(TAG, "onResume: ");
        super.onResume();

        //第一次进入的时候，menu还没有初始化
        if (firstInFlag) {
            firstInFlag = false;
            return;
        }

        NoteDbManager dbManager = NoteDbManager.getInstance();
        int noteId = intent.getIntExtra("noteId", 0);
        int serviceFlag = intent.getIntExtra("serviceFlag", -1);
        int beforeNoteId = note.getId();

        //来自服务的intent,当处于观看其他笔记而不是提醒笔记时不需要更新页面
        if (serviceFlag == 1 && noteId != beforeNoteId) {
            return;
        }

        note = dbManager.getNoteById(noteId);
        String currentYear = java.util.Calendar.getInstance().get(Calendar.YEAR) + "年";
        if (note.getTimeRemind() > 0) {
            if (dateToString(note.getTimeRemind()).contains(currentYear)) {
                remindTimeTextView.setText("提醒时间：" + dateToString(note.getTimeRemind()).replace(currentYear,""));
            } else {
                remindTimeTextView.setText("提醒时间：" + dateToString(note.getTimeRemind()));
            }
        } else {
            remindTimeTextView.setText("");
            MenuItem menuItem = menu.findItem(R.id.show_menu_remind);
            menuItem.setTitle("提醒我");
        }

        if (noteId == beforeNoteId) {
            return;
        }
        createTimeTextView.setText(note.getCreateTime());

        String content = note.getContent();
        SpannableStringBuilder spannableStringBuilder = ContentToSpannableString.contentToSpanStr(NoteActivity.this, content,note.getRestoreSpans());
        textView.setText(spannableStringBuilder);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private float getWordSize(String str){
        switch (str) {
            case "正常":
                checkedItem = 0;
                return 20;
            case "大":
                checkedItem = 1;
                return 25;
            case "超大":
                checkedItem = 2;
                return 30;
        }
        return 20;
    }

    private void popupDialog() {
        NoteDbManager dbManager = NoteDbManager.getInstance();
        //弹出一个dialog，用用户选择是否删除
        String message = "确定要将该便签放到回收站吗？";
        String neutralMessage = "放入回收站";
        if ("回收站".equals(note.getGroupName())) {
            message = "确定要将该便签恢复吗？";
            neutralMessage = "恢复";
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
                        MenuItem menuItem = menu.findItem(R.id.show_menu_delete);
                        String tempName = "回收站";
                        menuItem.setTitle("恢复笔记");
                        if ("回收站".equals(note.getGroupName())) {
                            tempName = "全部";
                            menuItem.setTitle("删除");
                        }
                        tempNote.setGroupName(tempName);
                        tempNote.setStatus(1);  //本地更新
                        dbManager.updateNote(note.getId(), tempNote);
                        note = dbManager.getNoteById(note.getId()); //更新笔记状态
                    }
                })
                .setPositiveButton("永久删除",(dialog, which) -> {
                    //如果没有同步直接删除
                    if (note.getStatus() != 2) {
                        DeleteMedia.deleteMedia(this, note.getContent());
                        dbManager.deleteNote(note);
                    } else {
                        Note tempNote = new Note();
                        tempNote.setStatus(-1);  //标记删除
                        dbManager.updateNote(note.getId(), tempNote);
                    }
                    //如果有提醒就要取消
                    if (note.getTimeRemind() > System.currentTimeMillis()) {
                        Intent intentForService= new Intent(this, AlarmService.class);
                        intentForService.putExtra("cancel", "cancel"); //(key,value)
                        intentForService.putExtra("note", note.getId());
                        intentForService.putExtra("type", "note");
                        startService(intentForService);
                    }
                    finish();//关闭页面
                }).create();
        alertDialog.show();
    }


    private String dateToString(long ms) {
        Date date = new Date(ms);
        String strDate = "";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault());
        strDate = simpleDateFormat.format(date);
        return strDate;
    }
}
