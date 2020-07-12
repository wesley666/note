package com.example.note;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcel;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.text.style.TextAppearanceSpan;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.example.note.db.Note;
import com.example.note.db.MediaForNote;
import com.example.note.util.ContentToSpannableString;
import com.example.note.util.GlideForMNImageBrowser;
import com.example.note.util.GlideImageEngine;
import com.example.note.util.NoteDbManager;
import com.example.note.util.SaveTextAppearanceSpan;
import com.example.note.util.SoftKeyBoardListener;
import com.example.note.util.XunFeiEngine;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechError;
import com.lahm.library.EasyProtectorLib;
import com.maning.imagebrowserlibrary.MNImageBrowser;
import com.maning.imagebrowserlibrary.listeners.OnClickListener;
import com.maning.imagebrowserlibrary.listeners.OnLongClickListener;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.internal.entity.CaptureStrategy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import top.zibin.luban.CompressionPredicate;
import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;
import top.zibin.luban.OnRenameListener;

public class NoteNewActivity extends AppCompatActivity {

    private String NewOrEdit;
    private Note oldNote = null;
    private Note newNote = null;
    private SpannableStringBuilder oldNoteSpannableString;
    private String groupName;
    private EditText editText;

    private final int REQUEST_CODE_CHOOSE = 23;  // zhihu设置作为标记的请求码
    private boolean isStart = false;      //判断是否开始录音
    private MediaRecorder mediaRecorder = null;
    List<MediaForNote> mediaForNoteList = null;


    // 声明平移动画
    private TranslateAnimation animation;

    //private Menu menu;
    private boolean isListening;
    private boolean isClickBoldBtn = false;
    private boolean isClickHighlightBtn = false;
    private boolean isClickTitleBtn = false;

    private int boldStyle;
    private int highlightStyle;
    private int titleStyle;
    private float wordSize;

    private static final String TAG = "NoteNewActivity";

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
        //getMenuInflater().inflate(R.menu.note_new_menu, menu);
        //this.menu = menu;
        //return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.new_menu_delete:

                break;

            default:
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        //向数据库中新增一条Note数据
        closeSoftKeyInput();
        editText.clearFocus();
        addNote();
        finish();
        //super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_new);

        //请求权限
        //ActivityCompat.requestPermissions(NoteNewActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO,
        //       Manifest.permission.WRITE_CALENDAR},1);

        getWordsSize();
        groupName = getIntent().getStringExtra("groupName");
        Log.d("groupName","传过来的组名是" + groupName);
        if(groupName.equals("全部")){
            groupName = "未分组";
        }
        NewOrEdit = getIntent().getStringExtra("NewOrEdit");

        Toolbar toolbarNoteNew = findViewById(R.id.toolbar_note_new);
        setSupportActionBar(toolbarNoteNew);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        //左上角返回键监听事件
        toolbarNoteNew.setNavigationOnClickListener((view) ->{ onBackPressed(); });

        editText = findViewById(R.id.note_new_editText);
        editText.setTextSize(wordSize);

        //editText.setText(new SpannableStringBuilder(""));
        mediaForNoteList = new ArrayList<>();
        newNote = new Note();
        if (NewOrEdit.equals("New")) {

        } else {
            int noteId = getIntent().getIntExtra("noteId", 0);
            oldNote = NoteDbManager.getInstance().getNoteById(noteId);
            oldNoteSpannableString = ContentToSpannableString.contentToSpanStr(NoteNewActivity.this, oldNote.getContent(), oldNote.getRestoreSpans());
            editText.append(oldNoteSpannableString);
        }

        //editText.setFocusable(true);
        //editText.setFocusableInTouchMode(true);
        //打开这个activity的时候自动获得焦点 + 自动打开软键盘  当editText获得焦点的时候，软键盘就会打开，相当于你点了一下屏幕
        editText.requestFocus();

        LinearLayout richText = findViewById(R.id.rich_text_edit);
        editText.setOnFocusChangeListener((view, b) -> {
            if(b){
                //richText.setVisibility(View.VISIBLE);
                Log.d("焦点","获得焦点");
            }else{
                //richText.setVisibility(View.GONE);
                Log.d("焦点","失去焦点");
            }
        });

        SoftKeyBoardListener.setListener(this, new SoftKeyBoardListener.OnSoftKeyBoardChangeListener() {
            @Override
            public void keyBoardShow(int height) {
                richText.setVisibility(View.VISIBLE);
                //Toast.makeText(Nthis, "键盘显示 高度" + height, Toast.LENGTH_SHORT).show();
                //introl_iv.setBackground(null);  //使LOGO消失
            }
            @Override
            public void keyBoardHide(int height) {
                richText.setVisibility(View.GONE);
                editText.clearFocus();
                //Toast.makeText(this, "键盘隐藏 高度" + height, Toast.LENGTH_SHORT).show();
                //introl_iv.setBackgroundResource(R.drawable.intro_logo);//设置
            }
        });


        //editText.setMovementMethod(ScrollingMovementMethod.getInstance());

        editText.setOnTouchListener(new View.OnTouchListener() {
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

        });

        Boolean isRunningInEmulator = EasyProtectorLib.checkIsRunningInEmulator(NoteNewActivity.this,null);

        TextWatcher watcher = new TextWatcher() {
            private int spanStart = -1;
            private int spanEnd = -1;
            Editable editable = editText.getText();
            int startPosition;
            int textCount;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                editable = editText.getText();
                Log.d(TAG, "beforeTextChanged: " + start + " "+count +" "+ after + " "+s.toString()+" "+editable.toString());
                if (start <= 0) {
                    return;
                }
                //真机不需要这段代码也能正确删除
                if (isRunningInEmulator || count > after) {
                    ImageSpan[] spans = editable.getSpans(0, start + count, ImageSpan.class);
                    if (spans == null || spans.length == 0) {
                        return;
                    }
                    ImageSpan span = spans[spans.length - 1];
                    int end = editable.getSpanEnd(span);
                    if (end != start + count) {
                        return;
                    }
                    spanStart = editable.getSpanStart(span);
                    spanEnd = editable.getSpanEnd(span);
                    editable.removeSpan(span);

                    /*
                    for (ImageSpan span : spans) {
                        //int start = editText.getEditableText().getSpanStart(span);
                        Log.d(TAG, "beforeTextChanged: "+editable.getSpanEnd(span)+" "+editable.getSpanStart(span));
                        int end = editable.getSpanEnd(span);
                        if (end != start + count) {
                            continue;
                        }
                        spanStart = editable.getSpanStart(span);
                        spanEnd = editable.getSpanEnd(span);
                                //String text = span.getSource();
                        //spanLength = text.length() - 1;
                        editable.removeSpan(span);
                    }*/
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d(TAG, "onTextChanged: " + start + " "+before +" "+ " "+count + " " + s.toString()+" "+editable.toString() + " long ");
                if (spanEnd > -1 && spanStart > -1 && isRunningInEmulator) {
                    editText.removeTextChangedListener(this);
                    editable.replace(start - (spanEnd - spanStart - 1), start, "");
                    editText.addTextChangedListener(this);
                    Log.d(TAG, "onTextChanged: " + spanStart +" " + spanEnd);
                    spanStart = -1;
                    spanEnd = -1;
                }
                startPosition = start;
                textCount = count;
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (startPosition < 0) {
                    return;
                }
                int id;

                if (isClickBoldBtn) {
                     id = boldStyle;
                } else if (isClickHighlightBtn) {
                    id = highlightStyle;
                } else if (isClickTitleBtn) {
                    id = titleStyle;
                } else {
                    return;
                }
                setSpan(s, startPosition, startPosition + textCount, id);
            }

        };
        editText.addTextChangedListener(watcher);


/*
        FloatingActionButton buttonNoteComplete = findViewById(R.id.button_note_new_complete);
        buttonNoteComplete.setOnClickListener((view) -> {
            //向数据库中新增一条Note数据
            closeSoftKeyInput();
            editText.clearFocus();
            AddNote();
        }); */

        ImageButton boldImageButton = findViewById(R.id.btn_bold);
        ImageButton highlightImageButton = findViewById(R.id.btn_highlight);
        ImageButton titleImageButton = findViewById(R.id.btn_title);
        ImageButton voiceImageButton = findViewById(R.id.btn_microphone);
        ImageButton photoImageButton = findViewById(R.id.btn_photo);

        PopupWindow popupWindow  = new PopupWindow();
        XunFeiEngine xunFeiEngine = new XunFeiEngine(NoteNewActivity.this);
        voiceImageButton.setOnClickListener(v -> {
            if (!isListening) {
                isListening = true;
                startRecord(xunFeiEngine, popupWindow);
            } else {
                isListening = false;
                popupWindow.dismiss();
                xunFeiEngine.stop();
            }
        });

        photoImageButton.setOnClickListener(v -> callGallery());


        boldImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int max = editText.getSelectionEnd();
                int min = editText.getSelectionStart();
                if (max < min) {
                    int temp = max;
                    max = min;
                    min = temp;
                }
                if (isClickBoldBtn) {
                    boldImageButton.setBackgroundColor(Color.parseColor("#ffffff"));
                    isClickBoldBtn = false;

                    //min和max相等时，getspans会把min前一个位置也当做范围
                    removeSpan(editText.getText(), min, max);

                } else {
                    isClickHighlightBtn = false;
                    isClickTitleBtn = false;
                    highlightImageButton.setBackgroundColor(Color.parseColor("#ffffff"));
                    titleImageButton.setBackgroundColor(Color.parseColor("#ffffff"));

                    boldImageButton.setBackgroundColor(Color.parseColor("#808080"));
                    isClickBoldBtn = true;

                    setSpan(editText.getText(), min, max, boldStyle);
                }

            }
        });


        //高亮
        highlightImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int max = editText.getSelectionEnd();
                int min = editText.getSelectionStart();
                if (max < min) {
                    int temp = max;
                    max = min;
                    min = temp;
                }
                if (isClickHighlightBtn) {
                    highlightImageButton.setBackgroundColor(Color.parseColor("#ffffff"));
                    isClickHighlightBtn = false;

                    removeSpan(editText.getText(), min, max);

                } else {
                    isClickBoldBtn = false;
                    isClickTitleBtn = false;
                    boldImageButton.setBackgroundColor(Color.parseColor("#ffffff"));
                    titleImageButton.setBackgroundColor(Color.parseColor("#ffffff"));

                    highlightImageButton.setBackgroundColor(Color.parseColor("#808080"));
                    isClickHighlightBtn = true;

                    setSpan(editText.getText(), min, max, highlightStyle);
                }

            }
        });

        titleImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int max = editText.getSelectionEnd();
                int min = editText.getSelectionStart();
                if (max < min) {
                    int temp = max;
                    max = min;
                    min = temp;
                }
                if (isClickTitleBtn) {
                    titleImageButton.setBackgroundColor(Color.parseColor("#ffffff"));
                    isClickTitleBtn = false;
                    removeSpan(editText.getText(), min, max);

                } else {
                    isClickHighlightBtn = false;
                    isClickBoldBtn = false;
                    highlightImageButton.setBackgroundColor(Color.parseColor("#ffffff"));
                    boldImageButton.setBackgroundColor(Color.parseColor("#ffffff"));

                    titleImageButton.setBackgroundColor(Color.parseColor("#808080"));
                    isClickTitleBtn = true;

                    setSpan(editText.getText(), min, max, titleStyle);
                }

            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (data != null) {
                switch (requestCode) {
                    // 图库选择图片
                    case REQUEST_CODE_CHOOSE:
                        //List<Uri> uriList = Matisse.obtainResult(data);
                        List<String> pathList = Matisse.obtainPathResult(data);
                        for (String path : pathList) {
                            //for (Uri uri : uriList) {
                            //List<String> imgliststr = new ArrayList<>();
                            //for (Uri uri : mSelected) {
                            //   imgliststr.add(ImageUriUtil.getPhotoPathFromContentUri(this, uri));
                            // }
                            //LogUtil.Logd("onActivityResult拿到了" + imgliststr.size() + "张图片:" + imgliststr);
                            // 这里仅仅选取一张图片
                            //Uri nSelected = mSelected.get(0);
                            //用Uri的string来构造spanStr，不知道能不能获得图片
                            //  ## +  string +  ##  来标识图片  <img src=''>

                           //String path = UriToRealPathUtil.getImageAbsolutePath(this, uri);
                            //Log.d("图片Path", uri.toString());
                           // String path = UriToRealPathUtil.getRealFilePath2(this, uri);
                            Log.d("图片Path", path);

                            try {
                                //根据Uri 获得 drawable资源
                                //Drawable drawable = Drawable.createFromStream(this.getContentResolver().openInputStream(uri), null);
                                //drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());

                                //BitmapFactory.Options options = new BitmapFactory.Options();
                                //options.inJustDecodeBounds = true;
                                //options.inSampleSize = 4;
                                //Bitmap bitmap = BitmapFactory.decodeStream(this.getContentResolver().openInputStream(uri), null, options);
                                String fileName = System.currentTimeMillis() + ".jpg";
                                saveBitmapToAPPDirectroy(path, fileName);

//                            //改成保存图片的名字
//                            SpannableString spannableString = new SpannableString("<img src='" + fileName + "'/>");
//                            Log.d("图片Uri",tempUri.toString());
//                            //BitmapDrawable bd = (BitmapDrawable) drawable;
//                            //Bitmap bp = bd.getBitmap();
//                            //bp.setDensity(160);
//                            ImageSpan span = new ImageSpan(this, bitmap,ImageSpan.ALIGN_BASELINE);
//                            spannableString.setSpan(span,0,spannableString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//                            Log.d("spanString：",spannableString.toString());
//
//                            editText.append("\n");
//                            int cursor = editText.getSelectionStart();
//                            editText.getText().insert(cursor, spannableString);

                            } catch (Exception FileNotFoundException) {
                                Log.d("异常", "无法根据Uri找到图片资源");
                            }
                        }
                        break;

                    default:
                }
            }
        }
    }

    private void removeSpan(Editable editable, int start, int end) {
        if (start == end) {
            return;
        }
        TextAppearanceSpan[] spans = editable.getSpans(start, end, TextAppearanceSpan.class);
        Log.d("richtext", "onClick: "+spans.length);

        for (TextAppearanceSpan span : spans) {
            editable.removeSpan(span);
        }
    }

    private void setSpan(Editable editable, int start, int end, int styleId) {
        if(start >= 0 && end >= 0 && start != end) {
            removeSpan(editable, start, end);
            for (int i = start; i < end; i++) {
                editable.setSpan(new TextAppearanceSpan(NoteNewActivity.this, styleId), i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    private void addNote() {
        NoteDbManager dbManager = NoteDbManager.getInstance();
        if (editText.getText().toString().trim().length() <= 0) {
            if(oldNote != null){
                dbManager.deleteNote(oldNote);
            }
            finish();
            return;
        }
        if (oldNote != null && editText.getText().equals(oldNoteSpannableString)) {
            finish();
            return;
        }
        Editable editableContent = editText.getText();
        ArrayList<byte[]> spanArrayList = new ArrayList<>();
        ArrayList<Integer> spanStartList = new ArrayList<>();
        ArrayList<Integer> spanEndList = new ArrayList<>();
        TextAppearanceSpan[] spans = editText.getText().getSpans(0,editText.length(), TextAppearanceSpan.class);
        //spanArrayList.addAll(Arrays.asList(spans));  这个内部类没有实现add()、remove()方法

        for (int i = 0; i < spans.length; i++) {
            Parcel parcel = Parcel.obtain();
            spans[i].writeToParcel(parcel,0);
            spanArrayList.add(i, parcel.marshall());
            spanStartList.add(i,editableContent.getSpanStart(spans[i]));
            spanEndList.add(i, editableContent.getSpanEnd(spans[i]));
            parcel.recycle();
        }
        Log.d("二进制", "AddNote: " + spans.length +" " + spanArrayList.size());
        Parcel parcel = Parcel.obtain();
        parcel.writeParcelable(new SaveTextAppearanceSpan(spanArrayList,spanStartList,spanEndList),0);
        byte[] bytes = parcel.marshall();
        Log.d("二进制", "AddNote: " + new String(Base64.encode(parcel.marshall(),Base64.DEFAULT)));
        parcel.recycle();

        String mContent = editText.getText().toString();



        int i;
        String tempContent = mContent.replaceAll("<img src='(.*?)'/>","").replaceAll("<voice src='(.*?)'/>","");
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

        Note note = newNote;
        note.setTitle(title);
        note.setSubContent(subContent);
        note.setContent(mContent);
        note.setUpdateTime(dateToString(System.currentTimeMillis()));
        note.setUpdatedTime(System.currentTimeMillis());

        note.setGroupName(groupName);
        note.setRestoreSpans(bytes);

        //当用户确定完成编辑之后， 意味着将旧的便签删除
        if(oldNote != null) {
            //旧笔记不需要记录创建时间
            //更新笔记状态
            note.setStatus(1);
            dbManager.updateNote(oldNote.getId(), note);
        } else {
            note.setCreateTime(dateToString(System.currentTimeMillis()));
            note.setCreatedTime(System.currentTimeMillis());
            dbManager.addNote(note);
        }

        //保存图片或者语音到数据库
        for (MediaForNote mediaForNote : mediaForNoteList) {
            if (note.getContent().contains(mediaForNote.getMediaName())) {
                mediaForNote.setNoteCreateTime(note.getCreatedTime());
                mediaForNote.save();
            }
        }
        //LitePal.saveAll(mediaForNoteList);

        //MenuItem menuItem = menu.findItem(R.id.new_menu_delete);
        //menuItem.setIcon(R.drawable.ic_delete_black_24dp);
    }

    //关闭软键盘
    private void closeSoftKeyInput() {
        /*
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        //boolean isOpen=imm.isActive();//isOpen若返回true，则表示输入法打开
        if (imm.isActive()) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }*/
        //拿到InputMethodManager
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        //如果window上view获取焦点 && view不为空
        if (imm.isActive() && getCurrentFocus() != null) {
            //拿到view的token 不为空
            if (getCurrentFocus().getWindowToken() != null) {
                //表示软键盘窗口总是隐藏，除非开始时以SHOW_FORCED显示。
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    }

    private void callGallery() {
        Matisse.from(NoteNewActivity.this)
                .choose(MimeType.ofImage())   //照片显示
                .countable(true)
                .maxSelectable(9)
                .capture(true) //是否提供拍照功能
                .captureStrategy(new CaptureStrategy(true,"com.example.note.fileprovider"))
                //.addFilter(new GifSizeFilter(320, 320, 5 * Filter.K * Filter.K))
                .gridExpectedSize(getResources().getDimensionPixelSize(R.dimen.grid_expected_size))
                .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                .thumbnailScale(0.85f)  // 缩略图的比例
                .imageEngine(new GlideImageEngine()) //使用的图片加载引擎
                //.showPreview(false) // Default is `true`
                .forResult(REQUEST_CODE_CHOOSE);  // 设置作为标记的请求码
    }

    //保存图片至app私有目录
    private void saveBitmapToAPPDirectroy(String path, String fileName) {
        File file = null;
        //File fileDirectory = C.FilePath.getPicturesDirectory();//即：/storage/emulated/0/Android/data/包名/files/Pictures/
        file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName);

        //保存图片名字
        MediaForNote mediaForNote = new MediaForNote();
        mediaForNote.setMediaName(fileName);
        mediaForNote.setPhoto(true);
        mediaForNoteList.add(mediaForNote);


        Luban.with(this)
                .load(path)
                .ignoreBy(1)
                .setTargetDir(getExternalFilesDir(Environment.DIRECTORY_PICTURES)+"/")
                .filter(new CompressionPredicate() {
                    @Override
                    public boolean apply(String path) {
                        return !(TextUtils.isEmpty(path) || path.toLowerCase().endsWith(".gif"));
                    }
                })
                .setRenameListener(new OnRenameListener() {
                    @Override
                    public String rename(String filePath) {
                        return fileName;
                    }
                })
                .setCompressListener(new OnCompressListener() {
                    @Override
                    public void onStart() {
                        // TODO 压缩开始前调用，可以在方法内启动 loading UI
                    }

                    @Override
                    public void onSuccess(File file) {
                        // TODO 压缩成功后调用，返回压缩后的图片文件
                        //改成保存图片的名字
                        Uri imageUri;
                        if (Build.VERSION.SDK_INT < 24) {
                            imageUri = Uri.fromFile(file);
                        } else {
                            imageUri = FileProvider.getUriForFile(NoteNewActivity.this, "com.example.note.fileprovider", file);
                        }
                        Drawable drawable = null;
                        try {
                            drawable = Drawable.createFromStream(getContentResolver().openInputStream(imageUri),null);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                        int width = ContentToSpannableString.getScreenRealWidth(NoteNewActivity.this);
                        drawable.setBounds(0,0, width, drawable.getIntrinsicHeight() * width / drawable.getIntrinsicWidth());
                        //drawable.setBounds(0,0, drawable.getIntrinsicWidth() , drawable.getIntrinsicHeight());
                        SpannableString spannableString = new SpannableString("<img src='" + fileName + "'/>");
                        //Log.d("图片Uri",tempUri.toString());
                        //BitmapDrawable bd = (BitmapDrawable) drawable;
                        //Bitmap bp = bd.getBitmap();
                        //bp.setDensity(160);
                        ImageSpan span = new ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM);
                        spannableString.setSpan(span,0,spannableString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        Log.d("spanString：",spannableString.toString());

                        ClickableSpan clickableSpan = new ClickableSpan() {
                            @Override
                            public void onClick(View view) {
                                Toast.makeText(NoteNewActivity.this, "长按保存,单击分享", Toast.LENGTH_SHORT).show();
                                MNImageBrowser.with(NoteNewActivity.this)
                                        //设置隐藏指示器
                                        .setIndicatorHide(false)
                                        //必须-图片加载用户自己去选择
                                        .setImageEngine(new GlideForMNImageBrowser())
                                        //必须（setImageList和setImageUrl二选一，会覆盖）-图片集合
                                        .setImageUrl(imageUri.toString())
                                        .setOnClickListener(new OnClickListener() {
                                            @Override
                                            public void onClick(FragmentActivity activity, ImageView view, int position, String url) {
                                                Intent shareIntent = new Intent();
                                                shareIntent.setAction(Intent.ACTION_SEND);
                                                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(url));
                                                shareIntent.setType("image/jpeg");
                                                startActivity(Intent.createChooser(shareIntent, "分享图片"));

                                            }
                                        })
                                        //非必须-图片长按监听
                                        .setOnLongClickListener(new OnLongClickListener() {
                                            @Override
                                            public void onLongClick(FragmentActivity activity, ImageView imageView, int position, String url) {
                                                //长按监听
                                                Bitmap bitmap = null;
                                                Toast.makeText(NoteNewActivity.this, "图片已保存", Toast.LENGTH_SHORT).show();
                                                try {
                                                    bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(Uri.parse(url)));
                                                } catch (FileNotFoundException e) {
                                                    e.printStackTrace();
                                                }
                                                String fileName = System.currentTimeMillis() + ".jpg";
                                                String insertImageUrl = MediaStore.Images.Media.insertImage(getContentResolver(),
                                                        bitmap, fileName, null);
                                                // 最后通知图库更新
                                                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse(insertImageUrl)));
                                            }
                                        })
                                        .show(view);
                            }
                        };
                        spannableString.setSpan(clickableSpan, 0, spannableString.length()-1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        editText.append("\n");
                        int cursor = editText.getSelectionStart();
                        editText.getText().insert(cursor, spannableString);
                        editText.append("\n");
                    }

                    @Override
                    public void onError(Throwable e) {
                        // TODO 当压缩过程出现问题时调用
                    }
                }).launch();
    }

    //保存录音至app私有目录
    private Uri saveAudioToAPPDirectroy(String fileName) {
        File file = null;
        //File fileDirectory = C.FilePath.getPicturesDirectory();//即：/storage/emulated/0/Android/data/包名/files/Pictures/
        file = new File(getExternalFilesDir(Environment.DIRECTORY_RINGTONES), fileName);
        Uri AudioUri;
        if (Build.VERSION.SDK_INT < 24) {
            AudioUri = Uri.fromFile(file);
        } else {
            AudioUri = FileProvider.getUriForFile(this, "com.example.note.fileprovider", file);
        }
        return AudioUri;
    }

    private void startRecord(XunFeiEngine xunFeiEngine, PopupWindow popupWindow){
        View popupView = View.inflate(this, R.layout.popup_window, null);

        popupWindow.setContentView(popupView);
        ImageView micImage = popupView.findViewById(R.id.recording_img);

        popupWindow.setWidth(500);
        popupWindow.setHeight(500);
        popupWindow.showAtLocation(popupView, Gravity.CENTER,0,0);

        String fileName = System.currentTimeMillis() + ".wav";
        xunFeiEngine.setParam(fileName);

        //记录录音
        MediaForNote mediaForNote = new MediaForNote();
        mediaForNote.setMediaName(fileName);
        mediaForNoteList.add(mediaForNote);

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
                popupWindow.dismiss();
            }

            @Override
            public void onResult(RecognizerResult recognizerResult, boolean b) {
                Uri audioUri = saveAudioToAPPDirectroy(fileName);
                int cursor = editText.getSelectionStart();
                editText.getText().insert(cursor, xunFeiEngine.printResult(recognizerResult));
                xunFeiEngine.clearIatResults();
                if (b) {
                    // TODO 最后的结果
                    //改成保存图片的名字
                    SpannableString spannableString = new SpannableString("<voice src='" + fileName + "'/>");
                    Drawable drawable = ContextCompat.getDrawable(getApplicationContext(), R.drawable.audio); //context.getResources().getDrawable(R.drawable.user_img, null);
                    //我们发现SpannableStringBuilder + ImageSpan可以实现将图片自动换行，并且如果剩余空间不足时图片会自动换行
                    drawable.setBounds(0,0, 70, drawable.getIntrinsicHeight() * 70 / drawable.getIntrinsicWidth());
                    spannableString.setSpan(new ContentToSpannableString.MyImageSpan(drawable),0, spannableString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);



                    Log.d(TAG, "onResult: 执行了");
                    ClickableSpan clickableSpan = new ClickableSpan() {
                        @Override
                        public void onClick(@NonNull View widget) {
                            //实现点击事件
                            Log.d("voice能否点击","能够点击");
                            MediaPlayer mediaPlayer = new MediaPlayer();
                            //Uri voiceUri = fileToUri(voiceName, context, false);
                            try {
                                mediaPlayer.setDataSource(getExternalFilesDir(Environment.DIRECTORY_RINGTONES) + "/" + fileName);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            mediaPlayer.setOnCompletionListener((mp) -> {
                                if (mp != null) {
                                    mp.stop();
                                    mp.release();
                                    mp = null;
                                }
                            });
                            try {
                                mediaPlayer.prepare();
                                mediaPlayer.start();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    };
                    spannableString.setSpan(clickableSpan, 0, spannableString.length() - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    editText.getText().insert(editText.getSelectionStart(), spannableString);
                }
            }

            @Override
            public void onError(SpeechError speechError) {
                xunFeiEngine.showTip(speechError.getPlainDescription(true));
                popupWindow.dismiss();
            }

            @Override
            public void onEvent(int i, int i1, int i2, Bundle bundle) {

            }
        });
        /*
        if(mediaRecorder == null){
            File dir = new File(Environment.getExternalStorageDirectory(),"sounds");
            if (!dir.exists()){
                dir.mkdir();
            }
            File soundFile = new File(dir, System.currentTimeMillis() + ".amr");
            if(!soundFile.exists()){
                try {
                    soundFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_WB);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);
            mediaRecorder.setOutputFile(soundFile.getAbsolutePath());

            editText.append("<voice src='" + soundFile.getAbsolutePath() + "'/>");

            try {
                mediaRecorder.prepare();
                mediaRecorder.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/
    }

    private String dateToString(long ms) {
        Date date = new Date(ms);
        String strDate = "";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault());
        strDate = simpleDateFormat.format(date);
        return strDate;
    }

    private void getWordsSize() {
        SharedPreferences sharedPreferences = getSharedPreferences("Setting", MODE_PRIVATE);
        String wordSizePrefs = sharedPreferences.getString("WordSize", "正常");
        int[] boldStyles = {R.style.BoldStyle, R.style.BoldStyle2, R.style.BoldStyle3};
        int[] highlightStyles = {R.style.HighlightStyle, R.style.HighlightStyle2, R.style.HighlightStyle3};
        int[] titleStyles = {R.style.TitleStyle, R.style.TitleStyle2, R.style.TitleStyle3};

        switch (wordSizePrefs) {
            case "正常":
                boldStyle = boldStyles[0];
                highlightStyle = highlightStyles[0];
                titleStyle = titleStyles[0];
                wordSize = 20;
                break;
            case "大":
                boldStyle = boldStyles[1];
                highlightStyle = highlightStyles[1];
                titleStyle = titleStyles[1];
                wordSize = 25;
                break;
            case "超大":
                boldStyle = boldStyles[2];
                highlightStyle = highlightStyles[2];
                titleStyle = titleStyles[2];
                wordSize = 30;
                break;
        }
    }

}
