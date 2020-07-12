package com.example.note;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


import com.example.note.adapter.NoteAdapter;
import com.example.note.db.Note;
import com.example.note.db.Todo;
import com.example.note.service.AlarmService;
import com.example.note.util.NoteDbManager;
import com.example.note.util.TodoDbManager;
import com.example.note.util.TtsUtil;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;
import com.simple.spiderman.SpiderMan;
import com.thegrizzlylabs.sardineandroid.Sardine;
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    NoteAdapter noteAdapter;
    NoteAdapter noteAdapterForListMode;
    NoteDbManager dbManager;
    private boolean enableTouchMode = true;
    private LinkedList<Note> notes;
    private String groupName = "全部";
    RecyclerView recyclerView;
    private static final String TAG = "MainActivity";

    // 语音听写对象
    private SpeechRecognizer mIat;
    // 用HashMap存储听写结果
    private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();
    private Toast mToast;
    // 引擎类型
    private String mEngineType = SpeechConstant.TYPE_CLOUD;
    private SharedPreferences mSharedPreferences;

    private TabLayout tabLayout;
    private ViewPager viewPager;
    private List<Fragment> fragmentList;
    private MyAdapter myAdapter;
    private String[] titles = {"笔记","待办事项"};

    class MyAdapter extends FragmentPagerAdapter {

        private Fragment currentFragment;

        public MyAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return fragmentList.get(position);
        }

        @Override
        public int getCount() {
            return fragmentList.size();
        }

        //重写这个方法，将设置每个Tab的标题
        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            //return super.getPageTitle(position);
            return titles[position];
        }

        @Override
        public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            currentFragment = (Fragment)object;
            super.setPrimaryItem(container, position, object);
            Log.d("MyAdapter", "setPrimaryItem: " + position + (object instanceof NoteFragment) + (object instanceof TodoFragment));
        }

        private Fragment getCurrentFragment(){
            return currentFragment;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbarMain = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbarMain); //使用ToolBar控件替代ActionBar控件

        //重新启动应用时，为未过期的笔记设置提醒。
        new Thread(new Runnable() {
            @Override
            public void run() {
                NoteDbManager noteDbManager = NoteDbManager.getInstance();
                List<Note> noteList = noteDbManager.getAll();
                for (Note note : noteList) {
                    if (note.getTimeRemind() >= System.currentTimeMillis()) {
                        Intent intentForService= new Intent(MainActivity.this, AlarmService.class);
                        intentForService.putExtra("date", note.getTimeRemind());//(key,value)
                        intentForService.putExtra("note", note.getId());
                        intentForService.putExtra("type", "note");
                        startService(intentForService);
                    }
                }

                TodoDbManager todoDbManager = TodoDbManager.getInstance();
                List<Todo> todoList = todoDbManager.getAll();
                for (Todo todo : todoList) {
                    if (todo.getTimeRemind() >= System.currentTimeMillis()) {
                        Intent intentForService = new Intent(MainActivity.this, AlarmService.class);
                        intentForService.putExtra("date", todo.getTimeRemind());//(key,value)
                        intentForService.putExtra("note", todo.getId());
                        intentForService.putExtra("type", "todo");
                        startService(intentForService);
                    }
                }
            }
        }).start();

        viewPager = findViewById(R.id.view_pager);
        tabLayout = findViewById(R.id.tab_layout);

        fragmentList = new ArrayList<>();
        fragmentList.add(new NoteFragment());
        fragmentList.add(new TodoFragment());

        myAdapter = new MyAdapter(getSupportFragmentManager());
        viewPager.setAdapter(myAdapter);
        tabLayout.setupWithViewPager(viewPager);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                Log.d("MyAdapter", "onPageScrolled: ");
            }

            //每当页面跳转完成后就会执行这个方法,先于getCurrentFragment方法
            @Override
            public void onPageSelected(int position) {
                if ( myAdapter.getItem(position)  instanceof TodoFragment) {
                    ((TodoFragment)myAdapter.getItem(position)).refreshDisplay();
                }
                Log.d("MyAdapter", "onPageSelected: " + position + (myAdapter.getCurrentFragment() instanceof NoteFragment) + (myAdapter.getCurrentFragment() instanceof TodoFragment));
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                Log.d("MyAdapter", "onPageScrollStateChanged: ");
            }
        });

        //初始化讯飞语音
        SpeechUtility.createUtility(this, SpeechConstant.APPID +"=");  //请在冒号里面添加讯飞开发者网站申请的密钥
        TtsUtil.getInstance(this);

        applyPermission();

        //弹出崩溃信息展示界面
        SpiderMan.init(this);

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbarMain, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                Log.d(TAG, "onDrawerOpened: ");
                super.onDrawerOpened(drawerView);
            }
        };
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView =findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        /** 设置MenuItem默认选中项 **/
        navigationView.getMenu().getItem(0).setChecked(true);

        View headerLayout = navigationView.getHeaderView(0);
        Button loginBtn = headerLayout.findViewById(R.id.login_btn);
        EditText userName = headerLayout.findViewById(R.id.user_name);
        EditText userPassword = headerLayout.findViewById(R.id.user_password);

        SharedPreferences cloudService = getSharedPreferences("CloudService", MODE_PRIVATE);
        userName.setText(cloudService.getString("UserName", ""));
        userPassword.setText(cloudService.getString("UserPassword", ""));

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = userName.getText().toString();
                String password = userPassword.getText().toString();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Sardine sardine = new OkHttpSardine();
                            sardine.setCredentials(name, password);
                            sardine.list("https://dav.jianguoyun.com/dav/");//如果是目录一定别忘记在后面加上一个斜杠
                            runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                            });
                            SharedPreferences.Editor editor = cloudService.edit();
                            editor.putString("UserName", name);
                            editor.putString("UserPassword", password);
                            editor.apply();         //editor.commit();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.d(TAG, "run: error");
                            runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this, "登录失败，请检查账号密码是否正确", Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                }).start();
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        Log.d(TAG, "onNavigationItemSelected: ");
        int id = menuItem.getItemId();

        if (id == R.id.nav_all) {
            // Handle the camera action
            groupName = "全部";
        } else if (id == R.id.nav_unGrouped) {
            groupName = "未分组";
        } else if (id == R.id.nav_life) {
            groupName = "生活";
        } else if (id == R.id.nav_work) {
            groupName = "工作";
        } else if (id == R.id.nav_recycle) {
            groupName = "回收站";
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        if (myAdapter.getCurrentFragment() instanceof NoteFragment) {
            ((NoteFragment)myAdapter.getCurrentFragment()).changedGroup(groupName);
        }
        //changedGroup();
        return true;
    }

    //对于Activity 可以单独获取Back键的按下事件
    //关闭侧滑菜单
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //return super.onCreateOptionsMenu(menu);
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.toolbar_main, menu);
        Log.d("Menu", "onCreateOptionsMenu: ");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        //return super.onOptionsItemSelected(item);
        if (myAdapter.getCurrentFragment() instanceof TodoFragment) {
            return false;
        }
        SharedPreferences prefs = getSharedPreferences("Setting",MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String showNotesModel;
        switch (item.getItemId()) {
            case R.id.list_mode :
                showNotesModel = "列表模式";
                editor.putString("ShowNotesModel",showNotesModel);
                editor.apply();         //editor.commit();
                break;
            case R.id.grid_model :
                showNotesModel = "宫格模式";
                //SharedPreferences prefs = getSharedPreferences("Setting",MODE_PRIVATE);
                //SharedPreferences.Editor editor = prefs.edit();
                editor.putString("ShowNotesModel",showNotesModel);
                editor.apply();         //editor.commit();
                break;
            default:
                showNotesModel = "列表模式";
        }
        ((NoteFragment)myAdapter.getCurrentFragment()).refreshLayoutManager(showNotesModel);

        //refreshLayoutManager();
        return  true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (enableTouchMode) {
            return super.dispatchTouchEvent(ev);
        }
        return true;
    }

    public void setTouchEventFlag(boolean touchEventFlag) {
        enableTouchMode = touchEventFlag;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 123) {
            for (int grantResult : grantResults) {
                if (grantResult == PackageManager.PERMISSION_DENIED) {
                    //发现有未通过权限
                    Toast.makeText(this, "您拒绝了软件正常运行所必要的权限，软件将退出",Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            }
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume: ");
        super.onResume();
        //refreshNotes();
        //refreshAdapter(notes);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if( null != mIat ){
            // 退出时释放连接
            mIat.cancel();
            mIat.destroy();
        }
        Intent intentForService = new Intent(MainActivity.this, AlarmService.class);
        stopService(intentForService);
    }


    //动态权限申请
    private void applyPermission() {
        String[] permissionList = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        ArrayList<String> applyList = new ArrayList<>();

        for(String permission : permissionList) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, permission)) {
                applyList.add(permission);
            }
        }

        if (!applyList.isEmpty()) {
            ActivityCompat.requestPermissions(this, applyList.toArray(new String[0]), 123);
        }
    }


}
