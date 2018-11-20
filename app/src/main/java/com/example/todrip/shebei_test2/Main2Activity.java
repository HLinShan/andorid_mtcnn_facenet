package com.example.todrip.shebei_test2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import android.media.MediaPlayer;

import com.example.todrip.shebei_test2.FaceFeature;
import com.example.todrip.shebei_test2.Facenet;
import com.example.todrip.shebei_test2.MTCNN;
import com.example.todrip.shebei_test2.R;
import com.example.todrip.shebei_test2.Utils;
import com.wits.serialport.SerialPort;
import com.wits.serialport.SerialPortManager;

public class Main2Activity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private SurfaceView mView;
    private SurfaceHolder mHolder;
    private Camera mCamera;

    public Bitmap bitmap1;
    public MTCNN mtcnn;
    public Facenet facenet;
    public String[] file_list;
    public Bitmap[] face_bitmap;
    public FaceFeature[] ff_list;
    public boolean timer_list = true;
    public CountDownTimer picture_timer1;
    public CountDownTimer picture_timer2;
    public int t1;
    public int t2;
    public double t3;
    public String t4;
    public  double threshold;
    public boolean is_face;
    public double result_score;
    public int result_index;
    public MediaPlayer player;
    public String upserverurl;
    public String featurefilename;
    public int model_number;
    public float[][] user_idandfeaturelist;
    public int[] user_idlist;
    public float[][] user_featuerlist;

    public byte[] mPreBuffer = null;


    private boolean if_get_face;
    //继电器
    private SerialPortManager mSerialPortManager;
    private InputStream mInputStream4;
    private OutputStream mOutputStream4;
    private Handler handler;
    private static Toast myToast;
    public double takepicture_time;
    public double recognition_time;
    public FaceFeature ff1;
    public FaceTask mFaceTask;

    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        //设置屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //获取从主界面获取的t1,t2,t3,t4
        t1 = Integer.parseInt(getIntent().getStringExtra("t0"));
        t2 = Integer.parseInt(getIntent().getStringExtra("t1"));
        t3 = Double.parseDouble(getIntent().getStringExtra("t2"));
        featurefilename = getIntent().getStringExtra("downloadfeaturefilefromserver");//下载文件
        upserverurl=getIntent().getStringExtra("uploaduseridtoserverurl");

        if_get_face = false;
        threshold = t3;
        //人脸识别部分
        mtcnn = new MTCNN(getAssets());
        facenet = new Facenet(getAssets());



        File objFile = new File(featurefilename);
        if (!objFile.exists()) {   //文件不存在,默认asset中的文件
            model_number=0;
            //        try {
            AssetManager assetManager = getAssets();
            try {
                file_list = assetManager.list("jpg");
            } catch (Exception e) {
                toast(Main2Activity.this,"获取默认数据库异常");

            }
            face_bitmap = new Bitmap[file_list.length];
            ff_list = new FaceFeature[file_list.length];
            Log.i("xxx","用默认asset中的数据库 ");
            //获取数据中的人脸特征
            get_FaceFeature();
        }
        else {
            //如果那个feature文件存在的话
            model_number=1;
            ReadCsv readCsv=new ReadCsv();
            try {
                user_idandfeaturelist=readCsv.get_CsvIdAndFeature(featurefilename);

            } catch (Exception e) {
                toast(Main2Activity.this,"feature文件解析异常，请下载正确的feature文件");
//                finish();

            }
        }

        //相机预览所需要的surfaceView、Holder以及回调函数
        mView = (SurfaceView) findViewById(R.id.surfaceView);
        mHolder = mView.getHolder();
        mHolder.addCallback(this);
        //mCamera.setPreviewCallback(this);
        timer_list = true;  //timert_list true来表示当前执行计时器1，否则执行计时器2
        star_picture_timer1();

        handler = new Handler();
        mSerialPortManager = new SerialPortManager();
        //new Thread(new ScanThread()).start();
//        串口4，继电器控制

//        SerialPort serialPort4 = null;
//        try {
//            serialPort4 = mSerialPortManager.getSerialPort4();
//        } catch (IOException e) {
//            toast(Main2Activity.this,"继电器接口问题");
//        }
//        mInputStream4 = serialPort4.getInputStream();
//        mOutputStream4 = serialPort4.getOutputStream();


    }
    //监听返回键，避免闪退
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(timer_list==true)
        {
            picture_timer1.cancel();
        }
        else
        {
            picture_timer2.cancel();
        }
        if(mFaceTask != null){
            switch(mFaceTask.getStatus()){
                case RUNNING:
                    mFaceTask.cancel(false);
                    return;
                case PENDING:
                    mFaceTask.cancel(false);
                    break;
            }
        }
        Main2Activity.this.finish();
    }
    //======================================================================================================================
    //以下几个为显示预览界面必要的回调函数
    @SuppressLint("WrongConstant")
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try{
            mCamera = Camera.open(1);
        }catch (Exception e){
//            e.printStackTrace();
            mCamera=Camera.open(0);
        }
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.setDisplayOrientation(90);
            //mCamera.setOneShotPreviewCallback(MainActivity.this);
            //mCamera.setPreviewCallback(this);

            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(640, 480);




            int size = parameters.getPreviewSize().width* parameters.getPreviewSize().height*3/2;
            if (mPreBuffer == null) {
                mPreBuffer = new byte[size];
            }
            mCamera.addCallbackBuffer(mPreBuffer);
            mCamera.setPreviewCallbackWithBuffer(Main2Activity.this);






            mCamera.startPreview();
        } catch (Exception e) {
            toast(Main2Activity.this,"摄像头异常，请打开摄像头权限");

        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mHolder.removeCallback(this);
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    //每次向相机发送获取一帧的指令所执行的操作
    @SuppressLint("WrongConstant")
    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        //新开线程后台处理模式
        // AsyncTask模式
        mCamera.addCallbackBuffer(bytes);
        if(mFaceTask != null){
            switch(mFaceTask.getStatus()){
                case RUNNING:
                    return;
                case PENDING:
                    mFaceTask.cancel(false);
                    break;
            }
        }
        mFaceTask = new FaceTask(bytes,camera);
        mFaceTask.execute((Void)null);



        //原始代码
//        if(if_get_face==true){
//            return;
//        }

//        byte[] rawImage;
//
//        ByteArrayOutputStream baos;
//        //需要对byte的数据先进行处理
//        Camera.Size previewSize = camera.getParameters().getPreviewSize();
//        BitmapFactory.Options newOpts = new BitmapFactory.Options();
//        YuvImage yuvimage = new YuvImage(
//                bytes,
//                ImageFormat.NV21,
//                previewSize.width,
//                previewSize.height,
//                null);
//        baos = new ByteArrayOutputStream();
//        yuvimage.compressToJpeg(new Rect(0, 0, previewSize.width, previewSize.height), 100, baos);// 80--JPG图片的质量[0-100],100最高
//        rawImage = baos.toByteArray();
//        //将rawImage转换成bitmap
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inPreferredConfig = Bitmap.Config.RGB_565;
//        bitmap1 = BitmapFactory.decodeByteArray(rawImage, 0, rawImage.length, options);
//
//
//        //旋转图像
//        Matrix m = new Matrix();
//        m.postRotate(270);
//        bitmap1 = Bitmap.createBitmap(bitmap1, 0, 0, bitmap1.getWidth(), bitmap1.getHeight(), m, true);
//        //镜像
//        m.setScale(-1,1);
//        bitmap1 = Bitmap.createBitmap(bitmap1, 0, 0, bitmap1.getWidth(), bitmap1.getHeight(), m, true);
//
//        //以上为获取完的一帧数据图，在此部分完成二维码
//        //。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。
//
//        //二维码验证完成
//
//        //以下为对bitmap1进行下一步处理
////        bitmap1 = Utils.resize(bitmap1, 1000);
//        //提取特征，判断是否有人脸
//        //拍照不管是不不是人脸的时间
//        takepicture_time=System.currentTimeMillis();
//        ff1 = compareFaces();
        //=======================原始代码

//        if(ff1 == null)
//        {
//            is_face = false;//当前的图片中不存在人脸
////            Log.e("main2Activity", "onPreviewFarame: "+System.currentTimeMillis() );
//            toast(Main2Activity.this,"没有拍到人");
//            if_get_face = false;
//            //通过timer_list来判断现在正在运行的是哪个计时器
//            if(timer_list == true)
//            {
//                picture_timer1.cancel();
//            }
//            else
//            {
//                picture_timer2.cancel();
//                timer_list = true;
//            }
//            star_picture_timer1();
//        }
//        else
//        {
////            takepicture_time=System.currentTimeMillis();
//            is_face = true;
//            if (model_number==0) {
//                get_result(ff1);
//            }else {
//                get_result_excel(ff1);
//            }
//
//            boolean face_pass = get_visiable(result_score);
////            if(face_pass == false)//人脸不通过，接着取下一帧
////            {
////                if_get_face = false;
////            }
////            else {
////                if_get_face = true;
////                //picture_timer1.cancel();
////                //star_picture_timer1();
////            }
//            if (face_pass == true) //验证通过,倒计时t2s
//            {
//                if(timer_list == true)
//                {
//                    picture_timer1.cancel();
//                    timer_list = false;
//                }
//                else
//                {
//                    picture_timer2.cancel();
//                }
//                star_picture_timer2();
//            }
//            else //人脸不通过，倒计时t1
//            {
//                if(timer_list == true)
//                {
//                    picture_timer1.cancel();
//                }
//                else
//                {
//                    picture_timer2.cancel();
//                    timer_list = true;
//                }
//                star_picture_timer1();
//            }
//        }
    }
//=========================================================================================================


    //计时器1
    //构造一个倒计时CountDownTimer（命名为picture_timer），用来倒计时，每间隔t1s保存一张图片
    public void star_picture_timer1() {
        if (picture_timer1 == null) {
            //总倒计时时间为1s，倒计时间隔为1s
            picture_timer1 = new CountDownTimer(t1, 100) {
                @Override
                public void onTick(long millisUntilFinished) {

                }

                @Override
                public void onFinish() {
//                    //
                    mCamera.setOneShotPreviewCallback(Main2Activity.this);

//                    if_get_face = false;
//                    picture_timer1.cancel();
                }
            };
            picture_timer1.start();
        } else {
            picture_timer1.start();
        }
    }

    //计时器2，每隔t2s执行一次
    public void star_picture_timer2() {
        if (picture_timer2 == null) {
            //总倒计时时间为2s，倒计时间隔为1s
            picture_timer2 = new CountDownTimer(t2, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {

                }

                @Override
                public void onFinish() {

                    mCamera.setOneShotPreviewCallback(Main2Activity.this);

                }
            };
            picture_timer2.start();
        } else {
            picture_timer2.start();
        }
    }


    //获取数据中的人脸特征
    public void get_FaceFeature() {
        for (int i = 0; i < file_list.length; i++) {
            Log.d("TAG", Integer.toString(i));

            face_bitmap[i] = readFromAssets(file_list[i]);
            face_bitmap[i] = Utils.resize(face_bitmap[i], 1000);


            //Bitmap bm = Utils.copyBitmap(face_bitmap[i]);
            Rect rect = mtcnn.getBiggestFace(face_bitmap[i], face_bitmap[i].getWidth()/6);

            //MTCNN检测到的人脸框，再上下左右扩展margin个像素点，再放入facenet中。
            int margin = 20; //20这个值是facenet中设置的。自己应该可以调整。
            Utils.rectExtend(face_bitmap[i], rect, margin);

            //要比较的两个人脸，加厚Rect
            Utils.drawRect(face_bitmap[i], rect, 1 + face_bitmap[i].getWidth() / 100);

            //(2)裁剪出人脸(只取第一张)
            Bitmap face = Utils.crop(face_bitmap[i], rect);

            //(3)特征提取
            ff_list[i] = facenet.recognizeImage(face);
            // face_bitmap[i] = bm;
        }
    }

    //从assets中读取图片
    private Bitmap readFromAssets(String filename) {
        Bitmap bitmap;
        AssetManager asm = getAssets();
        try {
            InputStream is = asm.open("jpg/" + filename);
            bitmap = BitmapFactory.decodeStream(is);
            is.close();
        } catch (Exception e) {
            Log.e("MainActivity", "[*]failed to open " + filename);
//
            return null;
        }
        return Utils.copyBitmap(bitmap);
    }

    //提取特征，判断是否有人脸
    public FaceFeature compareFaces(){
        Rect rect1=mtcnn.getBiggestFace(bitmap1,bitmap1.getWidth()/4);
        if (rect1==null)
        {
            return null;
        }
        //MTCNN检测到的人脸框，再上下左右扩展margin个像素点，再放入facenet中。
        int margin=20; //20这个值是facenet中设置的。自己应该可以调整。
        Utils.rectExtend(bitmap1,rect1,margin);
        //要比较的两个人脸，加厚Rect
        Utils.drawRect(bitmap1,rect1,1+bitmap1.getWidth()/100 );
        //(2)裁剪出人脸(只取第一张)

        Bitmap face1=Utils.crop(bitmap1,rect1);

        //(3)特征提取
        FaceFeature ff1=facenet.recognizeImage(face1);

        return ff1;
    }

    //在主线程获取结果
    public void get_result(FaceFeature ff1){
        //(4)比较
        double score;
        double min_score = ff1.compare(ff_list[0]);;
        int min_index = 0;
        //执行1000次循环比较
        for(int i=1; i<2000;i++){
            int id = i%file_list.length;
            score = ff1.compare(ff_list[id]);
            if(score<min_score){
                min_score = score;
                min_index = id;
            }
        }

        if(min_score>=0 && min_score<threshold)
        {
            result_score = min_score;
            result_index = min_index;
            Log.i("ddd", "和" + result_index+ "对比，成绩" +result_score );

            return;
        }
        result_score = 2;
        result_index = 0;
        return;
    }

    //在主线程获取结果并且显示结果 访问的是excel表
    public void get_result_excel(FaceFeature ff1) {
        //(4)比较
        double score;
        double min_score = Double.MAX_VALUE;
        int user_index = -1;
        float[] feature=new float[512];
        for (int i = 0; i < user_idandfeaturelist.length; i++) {

            System.arraycopy(user_idandfeaturelist[i],1,feature,0,511);

//            score = ff1.compare_float(user_featuerlist[i]);
            score = ff1.compare_float(feature);

            if (score >= 0 && score < threshold) {
                if (score < min_score) {
                    min_score = score;
                    user_index = i;
                }
                Log.i("ddd", "和" + (int)user_idandfeaturelist[i][0] + "对比，成绩" + score);
            }
        }

        if(min_score>=0 && min_score<threshold)
        {
            result_score = min_score;
            result_index = user_index;
            return;
        }
        result_score = 2;
        result_index = 0;
        //refreshCamera();
        return;
    }


    @SuppressLint("WrongConstant")
    public boolean get_visiable(double score)
    {
        if (score>=0 && score<threshold)
        {
            if (model_number==1) {
                //TODO 先开门 发声音 传日志
                recognition_time=System.currentTimeMillis();
                toast(Main2Activity.this,"feature中：是谁>特征表中:"+(int)user_idandfeaturelist[result_index][0]+"分数:"+result_score+"  拍照时间"+takepicture_time+"   识别时间"+recognition_time+" 时间差"+(recognition_time-takepicture_time));

                //传日志
                try {
//                    Log.e("main2Activity", "get_visiable: open_door--"+System.currentTimeMillis() );
                    open_door();//打开继电器
                    get_pass();//打开声音
                    UpDownfile updownfile = new UpDownfile();
                    updownfile.uploadDataToServer(upserverurl, (int)user_idandfeaturelist[result_index][0]);//上传日志userid

                }catch (Exception e){
//
                    toast(Main2Activity.this,"签到信息没有上传成功");
                }
            }
            else if (model_number==0){
//
//                toast(Main2Activity.this,"使用默认数据库，是默认数据库中的人");
                recognition_time=System.currentTimeMillis();
                toast(Main2Activity.this,"默认数据库中，是谁:"+result_index+"分数:"+result_score+"  拍照时间"+takepicture_time+"   识别时间"+recognition_time+" 时间差"+(recognition_time-takepicture_time));
                get_pass();//打开声音
                open_door();//打开继电器

            }
            return true;
        }
        else
        {
//            get_verification();
//
            toast(Main2Activity.this,"拍到人但不是数据库中的人");
            return false;
        }
    }

    //获取通过的声音
    public void get_pass(){
        if(player != null)
        {
            player.stop();
            player.reset();
            player.release();
            player = null;
        }
        try {
            player = new MediaPlayer();
            AssetManager assetManager = getAssets();
            AssetFileDescriptor fileDescriptor = assetManager.openFd("pass_voice/tg.mp3");
//            player.setDataSource(fileDescriptor.getFileDescriptor(),fileDescriptor.getStartOffset(),
//                    fileDescriptor.getStartOffset());
            player.setDataSource(fileDescriptor.getFileDescriptor(), fileDescriptor.getStartOffset(), fileDescriptor.getLength());
            player.setLooping(false);
            player.prepare();
            player.start();


        } catch (Exception e) {

            toast(Main2Activity.this,"声音获取异常");

        }
    }

    //获取请重新验证的声音
    public void get_verification(){
        if(player != null)
        {
            player.stop();
            player.reset();
            player.release();
            player = null;
        }
        try {
            player = new MediaPlayer();
            AssetManager assetManager = getAssets();
            AssetFileDescriptor fileDescriptor = assetManager.openFd("verification_voice/yz.mp3");
//            player.setDataSource(fileDescriptor.getFileDescriptor(),fileDescriptor.getStartOffset(),
//                    fileDescriptor.getStartOffset());
            player.setDataSource(fileDescriptor.getFileDescriptor(), fileDescriptor.getStartOffset(), fileDescriptor.getLength());
            player.setLooping(false);
            player.prepare();
            player.start();

        } catch (Exception e) {

            toast(Main2Activity.this,"声音获取异常");
        }
    }

    @SuppressLint("WrongConstant")
    public void open_door() {
        if (mOutputStream4 == null) {

//            toast(Main2Activity.this,"请打开串口");
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
            toast(Main2Activity.this,"继电器打开异常");

        }
    }


    public static void toast(Context context, String text){
        if (myToast != null) {
            myToast.cancel();
            myToast=Toast.makeText(context,text,Toast.LENGTH_SHORT);
        }else{
            myToast=Toast.makeText(context,text,Toast.LENGTH_SHORT);
        }

//        LinearLayout layout = (LinearLayout) myToast.getView();
//        layout.setBackgroundColor(Color.parseColor("#FFFFFF"));
//        TextView v = (TextView) myToast.getView().findViewById(android.R.id.message);
//        v.setTextColor(Color.BLACK);
//        v.setTextSize(25);

        myToast.setGravity(Gravity.NO_GRAVITY,0,0);
        myToast.show();
    }


    //=============================================新线程处理数据
    public class FaceTask extends AsyncTask{
        private byte[] bytes;
        Camera camera;
        //构造函数
        FaceTask(byte[]bytes , Camera camera)
        {
            this.bytes = bytes;
            this.camera = camera;

        }
        @Override
        protected Void doInBackground(Object[] params) {
            //=====================================================以下为原始onpreview中的代码
            byte[] rawImage;
            ByteArrayOutputStream baos;
            //需要对byte的数据先进行处理
            Camera.Size previewSize = camera.getParameters().getPreviewSize();
            BitmapFactory.Options newOpts = new BitmapFactory.Options();
            YuvImage yuvimage = new YuvImage(
                    bytes,
                    ImageFormat.NV21,
                    previewSize.width,
                    previewSize.height,
                    null);
            baos = new ByteArrayOutputStream();
            yuvimage.compressToJpeg(new Rect(0, 0, previewSize.width, previewSize.height), 100, baos);// 80--JPG图片的质量[0-100],100最高
            rawImage = baos.toByteArray();
            //将rawImage转换成bitmap
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            bitmap1 = BitmapFactory.decodeByteArray(rawImage, 0, rawImage.length, options);


            //旋转图像
            Matrix m = new Matrix();
            m.postRotate(270);
            bitmap1 = Bitmap.createBitmap(bitmap1, 0, 0, bitmap1.getWidth(), bitmap1.getHeight(), m, true);
            //镜像
            m.setScale(-1,1);
            bitmap1 = Bitmap.createBitmap(bitmap1, 0, 0, bitmap1.getWidth(), bitmap1.getHeight(), m, true);

            //以下为对bitmap1进行下一步处理
//        bitmap1 = Utils.resize(bitmap1, 1000);
            //提取特征，判断是否有人脸
            //拍照不管是不不是人脸的时间
            takepicture_time=System.currentTimeMillis();
            ff1 = compareFaces();
            return null;
        }




        @Override

        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            if(ff1 == null)
            {
                is_face = false;//当前的图片中不存在人脸
//            Log.e("main2Activity", "onPreviewFarame: "+System.currentTimeMillis() );
                toast(Main2Activity.this,"没有拍到人");
                if_get_face = false;
                //通过timer_list来判断现在正在运行的是哪个计时器
                if(timer_list == true)
                {
                    picture_timer1.cancel();
                }
                else
                {
                    picture_timer2.cancel();
                    timer_list = true;
                }
                star_picture_timer1();
            }
            else
            {
//            takepicture_time=System.currentTimeMillis();
                is_face = true;
                if (model_number==0) {
                    get_result(ff1);
                }else {
                    get_result_excel(ff1);
                }

                boolean face_pass = get_visiable(result_score);
//            if(face_pass == false)//人脸不通过，接着取下一帧
//            {
//                if_get_face = false;
//            }
//            else {
//                if_get_face = true;
//                //picture_timer1.cancel();
//                //star_picture_timer1();
//            }
                if (face_pass == true) //验证通过,倒计时t2s
                {
                    if(timer_list == true)
                    {
                        picture_timer1.cancel();
                        timer_list = false;
                    }
                    else
                    {
                        picture_timer2.cancel();
                    }
                    star_picture_timer2();
                }
                else //人脸不通过，倒计时t1
                {
                    if(timer_list == true)
                    {
                        picture_timer1.cancel();
                    }
                    else
                    {
                        picture_timer2.cancel();
                        timer_list = true;
                    }
                    star_picture_timer1();
                }
            }
        }
    }
    //扫描线程模式
    class ScanThread implements Runnable{

        public void run() {
            // TODO Auto-generated method stub
            while(!Thread.currentThread().isInterrupted()){
                try {
                    if( mCamera!=null)
                    {
                //myCamera.autoFocus(myAutoFocusCallback);
                        mCamera.setOneShotPreviewCallback(Main2Activity.this);
                        Thread.sleep(500);
                    }
                    //如果没拍到人脸，线程暂停300毫秒，如果拍到人脸，线程暂停3s
                    if(if_get_face == false) {Thread.sleep(500);}
                    else
                        {
                            if_get_face = false;
                            Thread.sleep(5000);

                        }
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            }

        }
    }
}
