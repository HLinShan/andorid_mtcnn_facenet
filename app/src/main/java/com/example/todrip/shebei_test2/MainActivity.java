package com.example.todrip.shebei_test2;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import com.wits.serialport.SerialPort;
import com.wits.serialport.SerialPortManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    //上传下载
    private static final int CONNECT_TIMEOUT = 12;
    private static final int WRITE_TIMEOUT = 10;
    private static final int READ_TIMEOUT = 10;
    private static final String CONTENT_TYPE = "application/octet-stream";
    private static OkHttpClient client;
    private Handler mHandler = new Handler();
    static {
        client = new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .build();
    }

    Button bt_startfacenet;
    Button bt_downfeature;
    Button bt_deletefeature;
    Button bt_senttoserver;
    Button bt_savepara;
    Button bt_open;
    Button bt_downinfofile;
    EditText et_senttoserver;
    public EditText t0,t1,t2,t3,t4,t5;
    private static Toast myToast;
    //保存参数到apk中
    public  Para para=new Para();
    public SharedPreferences sp;


    public UpDownfile updownfile=new UpDownfile();
    String outputfiledir;
    String environmentDir;
    String info_downfilename;
    String feature_downfilename;
    private FileHandler fileHandler;

    public int[] user_idlist=new int[1500];
    public float[][] user_featuerlist=new float[1500][];

    public  ReadTxt readtxt=new ReadTxt();
    private SerialPortManager mSerialPortManager;
    private InputStream mInputStream4;
    private OutputStream mOutputStream4;
    private Handler handler;
    public static final int DOWNLOAD_TXT_SUCCESS = 100001;
    private static final int DOWNLOAD_TXT_FAILURE = 100002;
    public static final int DOWNLOAD_CSV_SUCCESS = 100003;
    public static final int DOWNLOAD_CSV_FAILURE = 100004;
    public static final int UPLOAD_ID_SUCCESS = 100005;
    public static final int UPLOAD_ID_FAILURE = 100006;


    public  int BUTTON_DOWNTXT= 1;
    public  int BUTTON_DOWNCSV=1;
//    public  String serverurl="http://youkangbao.cn/ykb/back/checkin/add.php";
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @SuppressLint({"WrongViewCast", "WrongConstant"})
    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        outputfiledir = get_OutPutDownLoadDir();
        environmentDir = get_EnvironmentDir();
        //get button edit_text
        bt_downfeature = (Button) findViewById(R.id.bt_downfeature);
        bt_deletefeature = (Button) findViewById(R.id.bt_deletefeature);
        bt_startfacenet = (Button) findViewById(R.id.bt_startfacenet);
        bt_open = findViewById(R.id.bt_open);
        bt_senttoserver = (Button) findViewById(R.id.bt_senttoserver);
        bt_savepara=findViewById(R.id.bt_savapara);

        bt_downinfofile = findViewById(R.id.bt_downinfofile);
        t0 = (EditText) findViewById(R.id.ed1);//计时器1
        t1 = (EditText) findViewById(R.id.ed2);//计时器2
        t2 = (EditText) findViewById(R.id.ed3);//比对阈值
        t3 = (EditText) findViewById(R.id.et_getInfofileurl);
        t4 = (EditText) findViewById(R.id.et_getfeaturefileurl);
        t5 = (EditText) findViewById(R.id.et_getupfileurl);
        et_senttoserver = (EditText) findViewById(R.id.et_senttoserver);
        //    按键监听
        bt_downfeature.setOnClickListener(this);
        bt_deletefeature.setOnClickListener(this);
        bt_startfacenet.setOnClickListener(this);
        bt_downinfofile.setOnClickListener(this);
        bt_open.setOnClickListener(this);
        bt_senttoserver.setOnClickListener(this);
        bt_savepara.setOnClickListener(this);

        sp = getSharedPreferences("User", Context.MODE_PRIVATE);
        //每次开始获取之前保存的配置
        para.get_para(sp,t0,t1,t2,t3,t4,t5);


//        继电器
        handler = new Handler();
        mSerialPortManager = new SerialPortManager();
        //串口4，继电器控制
//        SerialPort serialPort4 = null;
//        try {
//            serialPort4 = mSerialPortManager.getSerialPort4();
//        } catch (Exception e) {
//            toast(MainActivity.this,"继电器问题");
//        }
//        mInputStream4 = serialPort4.getInputStream();
//        mOutputStream4 = serialPort4.getOutputStream();


        //开机初始化开始下载info文件
        fileHandler = new FileHandler(this);
//自动下载 然后到第二个界面
        new Thread(new Runnable() {
            @Override
            public void run() {
                String infourl = t3.getText().toString();
                info_downfilename = load_DownInfoTxtFromServer(infourl, outputfiledir);
            }
        }).start();


    }

    //对下载文件的回调操作
    public class FileHandler extends Handler {
        final WeakReference<MainActivity> mWeakReference;


        public FileHandler(MainActivity activity) {
            this.mWeakReference = new WeakReference<>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mWeakReference.get();
            super.handleMessage(msg);
            switch (msg.what) {
                case DOWNLOAD_TXT_SUCCESS:
                    //TODO info下载成功呢 ，跳转feature下载
                    toast(activity,"INFO下载成功");
                    try {
                        readtxt.get_info(info_downfilename, t0, t1, t2, t3, t4, t5);
                    } catch (Exception e) {
                        toast(activity,"解析info文件失败");
                    }
                    //下载feature文件
                    if (BUTTON_DOWNTXT==1) {
                        try {
                            String url = t4.getText().toString();
                            feature_downfilename = load_DownCsvFileFromServer(url, outputfiledir);
                        } catch (Exception e) {
                            toast(activity, "下载info文件失败");
                        }
                    }
                    break;
                case DOWNLOAD_TXT_FAILURE:
                    //TODO 跳转人脸识别界面用默认的feature配置
                    toast(activity,"INFO下载失败");
                    if(BUTTON_DOWNTXT==1 && BUTTON_DOWNCSV==1){
                        init_turn2facenet();
                    }
                    break;

                case DOWNLOAD_CSV_SUCCESS:
                    //TODO 跳转人脸识别界面 用已下载的feature
                    toast(activity,"FEATURE下载成功");
                    if(BUTTON_DOWNTXT==1 && BUTTON_DOWNCSV==1){//开机自己下载的就跳转
                        init_turn2facenet();
                    }
                    break;

                case DOWNLOAD_CSV_FAILURE:
                    //TODO 跳转人脸识别界面用默认feature配置
                    toast(activity,"FEATURE下载失败");
                    if(BUTTON_DOWNCSV==1){//开机自己下载的就跳转，用默认的feature数据
                        init_turn2facenet();
                    }
                    break;

                case UPLOAD_ID_SUCCESS:
                    // 上传ID成功
                    toast(activity,"ID上传成功");
                    break;

                case UPLOAD_ID_FAILURE:
                    // 上传ID失败
                    toast(activity,"ID上传失败");
                    break;

            }
        }
    }


    //    点击操作
    @SuppressLint("WrongConstant")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_savapara:
                int para_result_0 = para.save_para(sp,t0,t1,t2,t3,t4,t5);
                Log.i("para","保存para");
                //根据返回的int在toast中输出结果
                switch (para_result_0){

                    case 1:
                        toast(MainActivity.this,"计时器1的有效设置范围为0-800毫秒");
                        break;
                    case 2:
                        toast(MainActivity.this,"计时器2的有效设置范围为1000-5000毫秒");
                        break;
                    case 3:
                        toast(MainActivity.this,"阈值的有效设置范围为0-1.1");
                        break;
                    case 4:
                        toast(MainActivity.this,"保存成功");
                        break;
                }
                break;

            case R.id.bt_startfacenet://转到打开摄像头第二个界面
                init_turn2facenet();
                    break;

            case R.id.bt_downinfofile://下载Info

                BUTTON_DOWNTXT=0;

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String infourl = t3.getText().toString();
                        info_downfilename = load_DownInfoTxtFromServer(infourl, outputfiledir);
                    }
                }).start();
                break;


            case R.id.bt_downfeature://下载feature
                BUTTON_DOWNCSV=0;
                //下载feature文件
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String featureurl = t4.getText().toString();
                        info_downfilename = load_DownCsvFileFromServer(featureurl, outputfiledir);
                    }
                }).start();

                break;

            case R.id.bt_deletefeature://删除所有downloadfiledir
                Log.i("deletefeature","1234");
                File objFile1 = new File(outputfiledir);
                if (!objFile1.exists()) {   //文件不存在
                    break;
                }
                //如果文件删除结束
                if(updownfile.del_AllFiles(new File(outputfiledir))){

                    toast(MainActivity.this,"目标文件已被删除");
                }else {
                    toast(MainActivity.this,"目标文件夹没有目标文件");

                };
                break;

            case  R.id.bt_senttoserver://向服务器发送签到ID
                //String url1="http://youkangbao.cn/ykb/back/checkin/add.php";
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String uploadurl=t5.getText().toString();
                            int idnumber=Integer.parseInt(et_senttoserver.getText().toString());
                            uploadDataToServer(uploadurl,idnumber);
                        }
                    }).start();

                break;


            case R.id.bt_open://打开串口 继电器
                try {
                    open_door();
                    toast(MainActivity.this,"打开继电器成功");

                }catch (Exception e )
                {
                    toast(MainActivity.this,"打开继电器失败");
                }
                break;
        }
    }



    public void open_door(){
        if (mOutputStream4 == null) {

            toast(MainActivity.this,"请先打开串口");
            return;
        }
        try {
            byte[] bytes1 = SlecProtocol.hexStringToBytes(new String[]{
                            "00555555",  //用户id
                            "00000000",//用户卡号
                            "0001"}//开门间隔
                    , true);
            byte[] bytes = SlecProtocol.commandAndDataToAscii(
                    ((byte) 0x01),
                    bytes1
            );
            mOutputStream4.write(bytes);

        } catch (Exception e) {
            toast(MainActivity.this,"出现异常不能打开继电器");

        }
    }

    public void init_turn2facenet()
    {
        //如果feature 和 info 都在的话就跳转到下一个界面人脸识别
        int para_result;
        try {
            para_result = readtxt.judge_Info(t0,t1,t2);
            //根据返回的int在toast中输出结果
            switch (para_result){
                case 1:
                    toast(MainActivity.this,"计时器1的有效设置范围为0-800毫秒");
                    break;
                case 2:
                    toast(MainActivity.this,"计时器2的有效设置范围为1000-5000毫秒");

                    break;
                case 3:
                    toast(MainActivity.this,"阈值的有效设置范围为0-1.1");

                    break;
                case 4:
                    toast(MainActivity.this,"参数正确");
                    Intent intent = new Intent(MainActivity.this,Main2Activity.class);
                    //传递字符串
//                String passString = et_geturl.getText().toString();
                    //给第二个界面传 文件地址
                    String downloadfilefromserver=set_DownCsvFileName(outputfiledir);
                    File file=new File(downloadfilefromserver);
                    if (file.exists()){
                        toast(MainActivity.this,"跳转界面时：feature文件已下载");

                    }
                    else{
                        toast(MainActivity.this,"feature文件未下载，使用默认数据库");
                    }
                    //保存参数 TODO
                    para.save_para(sp,t0,t1,t2,t3,t4,t5);
                    Log.i("para","保存para");
                    //转界面 传参数 和 已下载的特征文件地址
                    intent.putExtra("downloadfeaturefilefromserver", downloadfilefromserver);
                    intent.putExtra("uploaduseridtoserverurl", t5.getText().toString());
                    intent.putExtra("t0", t0.getText().toString());
                    intent.putExtra("t1", t1.getText().toString());
                    intent.putExtra("t2", t2.getText().toString());
                    startActivity(intent);
                    break;
            }
        } catch (Exception e) {
            toast(MainActivity.this,"跳转界面异常");
        }
    }


    //获取存放下载的代码外部内存地址environment+包名
    public String get_EnvironmentDir(){
        File environmentdir = new File(Environment.getExternalStorageDirectory(), getPackageName());

        if (!environmentdir.exists()) {
            environmentdir.mkdir();
            Log.i("TAG1","create dir"+environmentdir.getAbsolutePath());
        }
        String get_environmentdir=environmentdir.getAbsolutePath() + File.separatorChar;

        return get_environmentdir ;
    }

    //获取下载的地址envirment +包名+downloadfiledir
    public String get_OutPutDownLoadDir(){
        String environmentdir=get_EnvironmentDir();
        String get_outputfiledir;
        get_outputfiledir=environmentdir+"DownloadFileDir";
        File outputfiledir=new File(get_outputfiledir);
        if (!outputfiledir.exists()) {
            outputfiledir.mkdir();
            Log.i("TAG1","create dir"+outputfiledir.getAbsolutePath());
        }
        return get_outputfiledir + File.separatorChar;
    }
//toast
    public static void toast(Context context, String text){
        if (myToast != null) {
            myToast.cancel();
            myToast=Toast.makeText(context,text,Toast.LENGTH_SHORT);
        }else{
            myToast=Toast.makeText(context,text,Toast.LENGTH_SHORT);
        }
        myToast.show();
    }

    //从服务器下载文件CSV文件
    public void downloadCsvFile(String url, final File file) {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("download","failed to download file to "+call.request().url(), e);
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
//                        init_turn2facenet();
                        //法二：向主线程传下载失败信号
                        Message message = Message.obtain();
                        message.what = DOWNLOAD_CSV_FAILURE;
                        fileHandler.sendMessage(message);
                    }
                });
            }
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onResponse(Call call, Response response) throws IOException {
//                Preconditions.checkArgument(response.isSuccessful(), "HTTP请求失败");
//                Preconditions.checkNotNull(response.body(), "HTTP请求响应体为空");

                Files.write(response.body().bytes(), file);

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                    }
                });
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        //法二：向主线程传下载成功信号
                        Message message = Message.obtain();
                        message.what = DOWNLOAD_CSV_SUCCESS;
                        fileHandler.sendMessage(message);

                                                    }
                                                });
                closeSilently(response);
                Log.i("download","successful upload file "+ file.getAbsolutePath()+" to "+call.request().url());
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private static void closeSilently(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }
    //csv设置名字
    public String set_DownCsvFileName(String outputfiledir) {
        String filename;
        filename = "UserInfo_downloadfile" + ".csv";
        String outputFileName = outputfiledir + filename;
        return outputFileName;
    }
    //下载csv文件操作 TODO
    public String load_DownCsvFileFromServer(String serverurl,String outputfiledir){
        String load_downexcelfile=set_DownCsvFileName(outputfiledir);
        downloadCsvFile(serverurl, new File(load_downexcelfile));
        return load_downexcelfile;
    }


    //从服务器下载文件info文件
    public void downloadInfoFile(String url, final File file) {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
//                Toast.makeText(MainActivity.this,"1文件下载失败",Toast.LENGTH_SHORT);
                Log.e("download","failed to download file to "+call.request().url(), e);
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        //法二下载失败
                        Message message = Message.obtain();
                        message.what = DOWNLOAD_TXT_FAILURE;
                        fileHandler.sendMessage(message);
                    }
                });
            }
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onResponse(Call call, Response response) throws IOException {
//                Preconditions.checkArgument(response.isSuccessful(), "HTTP请求失败");
//                Preconditions.checkNotNull(response.body(), "HTTP请求响应体为空");
                Files.write(response.body().bytes(), file);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        //  Toast不能放在这里
                    }
                });
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        //法二：向主线程传下载成功信号
                        Message message = Message.obtain();
                        message.what = DOWNLOAD_TXT_SUCCESS;
                        fileHandler.sendMessage(message);

                    }
                });
                closeSilently(response);
                Log.i("download","successful upload file "+ file.getAbsolutePath()+" to "+call.request().url());
            }
        });
    }

    //下载Info名字
    public String set_DownInfoTxtFileName(String outputfiledir){
        String filename;
        filename="ParameterInfo_downloadfile"+".txt";
        String outputFileName=outputfiledir +filename;
        return outputFileName;
    }
    //下载info文件操作 TODO
    public String load_DownInfoTxtFromServer(String serverurl,String outputfiledir){
        String load_downexcelfile=set_DownInfoTxtFileName(outputfiledir);
        downloadInfoFile(serverurl, new File(load_downexcelfile));
        return load_downexcelfile;
    }

    //上传id账号
    public  void uploadDataToServer(String url,int id)   {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("id",String.valueOf(id))
                .build();
//                .addFormDataPart("checktime",checktime)
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("upload","failed to upload file to "+call.request().url(), e);
                Message message = Message.obtain();
                message.what = UPLOAD_ID_FAILURE;
                fileHandler.sendMessage(message);
            }
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Preconditions.checkArgument(response.isSuccessful(), "HTTP请求失败");
                Preconditions.checkNotNull(response.body(), "HTTP请求响应体为空");
                if (response.isSuccessful()) {
                    Log.i("upload", "onResponse: " + response.body().string());
                    Message message = Message.obtain();
                    message.what = UPLOAD_ID_SUCCESS;
                    fileHandler.sendMessage(message);
                }
                closeSilently(response);
                Log.i("upload","successful upload file ");

            }
        });
    }



}
