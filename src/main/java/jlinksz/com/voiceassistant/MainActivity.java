package jlinksz.com.voiceassistant;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;
import android.Manifest;

import com.iflytek.aiui.AIUIAgent;
import com.iflytek.aiui.AIUIConstant;
import com.iflytek.aiui.AIUIEvent;
import com.iflytek.aiui.AIUIListener;
import com.iflytek.aiui.AIUIMessage;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechEvent;
import com.iflytek.cloud.SynthesizerListener;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jlinksz.com.util.FucUtil;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends Activity implements View.OnClickListener {
    private static String TAG = "MainActivity";
    //录音权限
    private String[] permissions = {Manifest.permission.RECORD_AUDIO,Manifest.permission.CALL_PHONE};

    private Toast mToast;
    private EditText mNlpText;

    private AIUIAgent mAIUIAgent = null;

    //交互状态
    private int mAIUIState = AIUIConstant.STATE_IDLE;

    private TtsHelper mTtsHelper;

    private Context mContext;
    private String mSyncSid = "";

    private PowerManager pm;
    private KeyguardManager keyguardManager;
    private PowerManager.WakeLock wl;

    private HashMap<String,String> InstalledAppMap;


    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 0:
                    if( !checkAIUIAgent() ){
                          return;
                    }
                    startVoiceNlp();

                    break;
                case 1:
                    keyguardLock.disableKeyguard(); // 解锁
                    Log.i("call","disableKeyguard");
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        initLayout();
        mContext = this;
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        pm = (PowerManager) mContext
                .getSystemService(Context.POWER_SERVICE);
        keyguardManager = (KeyguardManager) mContext
                .getSystemService(KEYGUARD_SERVICE);
        wl = pm.newWakeLock(
                PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "bright");
        InstalledAppMap = new HashMap<>();
        //getPackages();
        mTtsHelper = new TtsHelper(this);
        mTtsHelper.setmTtsListener(mTtsListener);
        WakeHelper helper = new WakeHelper(this);
        helper.build();
        helper.justWake();
        helper.setWakeupListener(new wakeupListener() {
            @Override
            public void wakeup() {
                wakeUpAndUnlock();
                mTtsHelper.play("Hi");
            }
        });
        requestPermission();

    }



    /**
     * 初始化Layout。
     */
    private void initLayout() {
        findViewById(R.id.nlp_start).setOnClickListener(this);
        findViewById(R.id.playsound).setOnClickListener(this);
        mNlpText = (EditText)findViewById(R.id.nlp_text);

    }



    @Override
    public void onClick(View view) {
        if( !checkAIUIAgent() ){
            return;
        }

        switch (view.getId()) {
            // 开始语音理解
            case R.id.nlp_start:
                startVoiceNlp();
                break;
            case R.id.playsound:
//                String str = mNlpText.getText().toString();
//                if (!TextUtils.isEmpty(str)){
//                    mTtsHelper.play(str);
//                }

                syncQuery();
                break;
            default:
                break;
        }
    }



    /**
     * 读取配置
     */
    private String getAIUIParams() {
        String params = "";

        AssetManager assetManager = getResources().getAssets();
        try {
            InputStream ins = assetManager.open( "cfg/aiui_phone.cfg" );
            byte[] buffer = new byte[ins.available()];

            ins.read(buffer);
            ins.close();

            params = new String(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return params;
    }

    private boolean checkAIUIAgent(){
        if( null == mAIUIAgent ){
            Log.i(TAG, "create aiui agent" );

            //创建AIUIAgent
            mAIUIAgent = AIUIAgent.createAgent( this, getAIUIParams(), mAIUIListener );
        }

        if( null == mAIUIAgent ){
            final String strErrorTip = "创建 AIUI Agent 失败！";
            showTip( strErrorTip );
            this.mNlpText.setText( strErrorTip );
        }

        return null != mAIUIAgent;
    }

    //开始录音
    private void startVoiceNlp(){
        Log.i(TAG, "start voice nlp");
        mNlpText.setText("");

        // 先发送唤醒消息，改变AIUI内部状态，只有唤醒状态才能接收语音输入
        // 默认为oneshot 模式，即一次唤醒后就进入休眠，如果语音唤醒后，需要进行文本语义，请将改段逻辑copy至startTextNlp()开头处
        if( AIUIConstant.STATE_WORKING != 	this.mAIUIState ){
            AIUIMessage wakeupMsg = new AIUIMessage(AIUIConstant.CMD_WAKEUP, 0, 0, "", null);
            mAIUIAgent.sendMessage(wakeupMsg);
        }

        // 打开AIUI内部录音机，开始录音
        String params = "sample_rate=16000,data_type=audio";
        AIUIMessage writeMsg = new AIUIMessage( AIUIConstant.CMD_START_RECORD, 0, 0, params, null );
        mAIUIAgent.sendMessage(writeMsg);

    }

    //AIUI事件监听器
    private AIUIListener mAIUIListener = new AIUIListener() {

        @Override
        public void onEvent(AIUIEvent event) {
            switch (event.eventType) {
                case AIUIConstant.EVENT_WAKEUP:
                    //唤醒事件
                    Log.i( TAG,  "on event: "+ event.eventType );
                    showTip( "进入识别状态" );
                    break;

                case AIUIConstant.EVENT_RESULT: {
                    //结果事件
                    Log.i( TAG,  "on event: "+ event.eventType );
                    try {
                        JSONObject bizParamJson = new JSONObject(event.info);
                        JSONObject data = bizParamJson.getJSONArray("data").getJSONObject(0);
                        JSONObject params = data.getJSONObject("params");
                        JSONObject content = data.getJSONArray("content").getJSONObject(0);
                        Log.i("call","info " + bizParamJson.toString());
                        if (content.has("cnt_id")) {
                            String cnt_id = content.getString("cnt_id");
                            JSONObject cntJson = new JSONObject(new String(event.data.getByteArray(cnt_id), "utf-8"));
                            Log.i("call","cntJson " + cntJson.toString());
                            String sub = params.optString("sub");
                            JSONObject result = cntJson.optJSONObject("intent");
                            if ("nlp".equals(sub) && result.length() > 2) {
                                Log.i("call","result " + result.toString());
                                // 解析得到语义结果
                                String str = "";
                                //在线语义结果
                                if(result.optInt("rc") == 0){
                                    JSONObject semantic = result.optJSONArray("semantic").getJSONObject(0);
                                    if(semantic != null){

                                        if("start_activity".equals(semantic.optString("intent"))){
                                            JSONObject slots = semantic.optJSONArray("slots").getJSONObject(0);

//                                            Log.i("call","slots " + slots.toString());
                                            String appName = slots.optString("value");
                                            if(InstalledAppMap.get(appName) != null){
                                                startThirdActivity(InstalledAppMap.get(appName));
                                            }
                                        }

                                        if("DIAL".equals(semantic.optString("intent"))){
                                            JSONObject slots = semantic.optJSONArray("slots").getJSONObject(0);

//                                            Log.i("call","slots " + slots.toString());
                                            String num = slots.optString("value");
                                            call(num);
                                        }

                                    }
                                    JSONObject answer = result.optJSONObject("answer");
                                    if(answer != null){
                                        str = answer.optString("text");
                                    }



                                }else if(result.optInt("rc") == 3){
                                    JSONObject answer = result.optJSONObject("answer");
                                    if(answer != null){
                                        str = answer.optString("text");
                                    }
                                }/*else if(!"".equals(result.getString("text"))){
                                    String keyword = result.getString("text");
                                    Log.i("call","result1 " + result.getString("text"));
                                    if(InstalledAppMap.get(keyword) != null){
                                        startThirdActivity(InstalledAppMap.get(keyword));
                                    }
//                                    if(keyword.contains("音乐")){
//                                        startThirdActivity("com.android.music","com.android.music.MusicBrowserActivity");
//                                    }
//                                    if(keyword.contains("拨号") || keyword.contains("打电话")){
//                                        startThirdActivity("com.android.dialer","com.android.dialer.DialtactsActivity");
//                                    }
                                }*/
                                else{
                                    str = "rc4，无法识别";
                                }
                                Log.i( TAG,  "str: "+ str );

                                if (!TextUtils.isEmpty(str)){
                                    mNlpText.append( "\n" );
                                    mNlpText.append(str);
                                    mTtsHelper.play(str);
                                }

                            }
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                        mNlpText.append("\n");
                        mNlpText.append(e.getLocalizedMessage());
                    }

                    mNlpText.append( "\n" );
                } break;

                case AIUIConstant.EVENT_ERROR: {
                    //错误事件
                    Log.i( TAG,  "on event: "+ event.eventType );
                    mNlpText.append( "\n" );
                    mNlpText.append( "错误: "+event.arg1+"\n"+event.info );
                } break;

                case AIUIConstant.EVENT_VAD: {
                    //vad事件
                    if (AIUIConstant.VAD_BOS == event.arg1) {
                        //找到语音前端点
                        showTip("找到vad_bos");
                    } else if (AIUIConstant.VAD_EOS == event.arg1) {
                        //找到语音后端点
                        showTip("找到vad_eos");
                    } else {
                        showTip("" + event.arg2);
                    }
                } break;

                case AIUIConstant.EVENT_START_RECORD: {
                    //开始录音事件
                    Log.i( TAG,  "on event: "+ event.eventType );
                    showTip("开始录音");
                } break;

                case AIUIConstant.EVENT_STOP_RECORD: {
                    //停止录音事件
                    Log.i( TAG,  "on event: "+ event.eventType );
                    showTip("停止录音");
                } break;

                case AIUIConstant.EVENT_STATE: {	// 状态事件
                    mAIUIState = event.arg1;

                    if (AIUIConstant.STATE_IDLE == mAIUIState) {
                        // 闲置状态，AIUI未开启
                        showTip("STATE_IDLE");
                    } else if (AIUIConstant.STATE_READY == mAIUIState) {
                        // AIUI已就绪，等待唤醒
                        showTip("STATE_READY");
                    } else if (AIUIConstant.STATE_WORKING == mAIUIState) {
                        // AIUI工作中，可进行交互
                        showTip("STATE_WORKING");
                    }
                } break;
                case AIUIConstant.EVENT_CONNECTED_TO_SERVER:
                    showTip("已连接服务器");
                    Log.i(TAG, "on event: 已连接服务器");
                    //syncContacts();
                    syncApps();
                    break;

                case AIUIConstant.EVENT_SERVER_DISCONNECTED:
                    showTip("与服务器断连");
                    break;
                case AIUIConstant.EVENT_CMD_RETURN: {
                    if (AIUIConstant.CMD_SYNC == event.arg1) {	// 数据同步的返回
                        int dtype = event.data.getInt("sync_dtype", -1);
                        int retCode = event.arg2;

                        switch (dtype) {
                            case AIUIConstant.SYNC_DATA_SCHEMA: {
                                if (AIUIConstant.SUCCESS == retCode) {
                                    // 上传成功，记录上传会话的sid，以用于查询数据打包状态
                                    // 注：上传成功并不表示数据打包成功，打包成功与否应以同步状态查询结果为准，数据只有打包成功后才能正常使用
                                    mSyncSid = event.data.getString("sid");

                                    // 获取上传调用时设置的自定义tag
                                    String tag = event.data.getString("tag");
                                    Log.i("call","SUCCESS tag  " + tag +"  mSyncSid " + mSyncSid);
                                    // 获取上传调用耗时，单位：ms
                                    long timeSpent = event.data.getLong("time_spent", -1);
                                    if (-1 != timeSpent) {
                                        //mTimeSpentText.setText(timeSpent + "ms");
                                    }

                                    showTip("上传成功，sid=" + mSyncSid + "，tag=" + tag + "，你可以试着说“打电话给刘德华”");
                                } else {
                                    mSyncSid = "";
                                    showTip("上传失败，错误码：" + retCode);
                                }

                            } break;
                        }
                    } else if (AIUIConstant.CMD_QUERY_SYNC_STATUS == event.arg1) {	// 数据同步状态查询的返回
                        // 获取同步类型
                        int syncType = event.data.getInt("sync_dtype", -1);
                        if (AIUIConstant.SYNC_DATA_QUERY == syncType) {
                            // 若是同步数据查询，则获取查询结果，结果中error字段为0则表示上传数据打包成功，否则为错误码
                            String result = event.data.getString("result");
                            Log.i( "call",  " result " + result );
                            showTip(result);
                        }
                    }
                } break;

                default:
                    break;
            }
        }

    };

    private void call(String phone) {
        Intent intent=new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phone));
        startActivity(intent);
    }

    public void startThirdActivity(String packageName){
        Log.i("call","startThirdActivity packageName " + packageName );
        Intent Newintent = new Intent();
        Intent resolveIntent = new Intent(Intent.ACTION_MAIN,null);
        resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        resolveIntent.setPackage(packageName);
        List<ResolveInfo> apps =
                getPackageManager().queryIntentActivities(resolveIntent, 0);
        ResolveInfo ri = apps.iterator().next();
        if (ri!=null){
            String className = ri.activityInfo.name;
            Log.i("call","className " + className );
            Newintent.setComponent(new ComponentName(packageName, className));
            startActivity(Newintent);
        }

    }

    KeyguardManager.KeyguardLock keyguardLock;
    /**
     * 唤醒手机屏幕并解锁
     */
    public void wakeUpAndUnlock() {
        // 获取电源管理器对象

        boolean screenOn = pm.isScreenOn();
        if (!screenOn) {
            // 获取PowerManager.WakeLock对象,后面的参数|表示同时传入两个值,最后的是LogCat里用的Tag
            wl.acquire(10000); // 点亮屏幕
            wl.release(); // 释放
        }
        // 屏幕解锁

        keyguardLock = keyguardManager.newKeyguardLock("unLock");

        new Thread(){
            @Override
            public void run() {
                super.run();
                try {
                    Thread.sleep(1000);
                     mHandler.sendEmptyMessage(1);
                }catch (Exception e){

                }

            }
        }.start();

        // 屏幕锁定
       // keyguardLock.reenableKeyguard();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if( null != this.mAIUIAgent ){
            AIUIMessage stopMsg = new AIUIMessage(AIUIConstant.CMD_STOP, 0, 0, null, null);
            mAIUIAgent.sendMessage( stopMsg );

            this.mAIUIAgent.destroy();
            this.mAIUIAgent = null;
        }
        if( null != mTtsHelper ){
            mTtsHelper.stopSpeaking();
            // 退出时释放连接
            mTtsHelper.destroy();
        }
    }


    /**
     * 合成回调监听。
     */
    private SynthesizerListener mTtsListener = new SynthesizerListener() {

        @Override
        public void onSpeakBegin() {
            showTip("开始播放");
        }

        @Override
        public void onSpeakPaused() {
            showTip("暂停播放");
        }

        @Override
        public void onSpeakResumed() {
            showTip("继续播放");
        }

        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos,
                                     String info) {
            // 合成进度
//            mPercentForBuffering = percent;
//            showTip(String.format(mContext.getString(R.string.tts_toast_format),
//                    mPercentForBuffering, mPercentForPlaying));
        }

        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
            // 播放进度
//            mPercentForPlaying = percent;
//            showTip(String.format(mContext.getString(R.string.tts_toast_format),
//                    mPercentForBuffering, mPercentForPlaying));
        }

        @Override
        public void onCompleted(SpeechError error) {
            if (error == null) {
                showTip("播放完成");
                mHandler.sendEmptyMessage(0);
            } else if (error != null) {
                showTip(error.getPlainDescription(true));
            }
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            if (SpeechEvent.EVENT_SESSION_ID == eventType) {
                String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
                Log.d("call", "session id =" + sid);
            }
        }
    };

    public List<String> getPackages(){
        List<String> apps = new ArrayList<>();
        List<PackageInfo> packages = getPackageManager().getInstalledPackages(0);
        Log.i("call","size" + packages.size());
        String applable = "";
        if(packages.size() > 0) {
            for (PackageInfo pi : packages) {
//                Log.i("call", "" + pi.applicationInfo.loadLabel(getPackageManager()).toString());
//                Log.i("call","packageName " +pi.packageName);
                applable = pi.applicationInfo.loadLabel(getPackageManager()).toString();
                apps.add(applable);
                InstalledAppMap.put(applable,
                        pi.packageName);
            }
        }

        return apps;
    }



    private void showTip(final String str)
    {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                mToast.setText(str);
                mToast.show();
            }
        });
    }


    private void syncApps() {
        if (null == mAIUIAgent) {
            showTip("AIUIAgent 为空，请先创建");
            return;
        }


        try {

            StringBuilder appsJson = new StringBuilder();
            List<String> apps = getPackages();
            for (String app : apps) {
                String name = app;
                appsJson.append(String.format("{\"name\": \"%s\"}\n",
                        name));
            }
            Log.i("call","appsJson " + appsJson);
            // 数据进行no_wrap Base64编码
            String dataStrBase64 = Base64.encodeToString(appsJson.toString().getBytes("utf-8"), Base64.NO_WRAP);

            JSONObject syncSchemaJson = new JSONObject();
            JSONObject dataParamJson = new JSONObject();

            // 设置id_name为uid，即用户级个性化资源
            // 个性化资源使用方法可参见http://doc.xfyun.cn/aiui_mobile/的用户个性化章节
            dataParamJson.put("id_name", "uid");

            // 设置res_name为联系人
            dataParamJson.put("res_name", "JLINKSZMUSIC.res_appname");

            syncSchemaJson.put("param", dataParamJson);
            syncSchemaJson.put("data", dataStrBase64);

            // 传入的数据一定要为utf-8编码
            byte[] syncData = syncSchemaJson.toString().getBytes("utf-8");

            // 给该次同步加上自定义tag，在返回结果中可通过tag将结果和调用对应起来
            JSONObject paramJson = new JSONObject();
            paramJson.put("tag", "sync-app");

            // 用schema数据同步上传联系人
            // 注：数据同步请在连接服务器之后进行，否则可能失败
            AIUIMessage syncAthena = new AIUIMessage(AIUIConstant.CMD_SYNC,
                    AIUIConstant.SYNC_DATA_SCHEMA, 0, paramJson.toString(), syncData);

            mAIUIAgent.sendMessage(syncAthena);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void syncContacts() {
        if (null == mAIUIAgent) {
            showTip("AIUIAgent 为空，请先创建");
            return;
        }


        try {
            // 从文件中读取联系人示例数据
//            String dataStr = FucUtil.readFile(this, "data_contact.txt", "utf-8");
//            mNlpText.setText(dataStr);
            StringBuilder contactJson = new StringBuilder();
            List<String> contacts = getContacts();
            for (String contact : contacts) {
                String[] nameNumber = contact.split("\\$\\$");
                contactJson.append(String.format("{\"name\": \"%s\", \"phoneNumber\": \"%s\" }\n",
                        nameNumber[0], nameNumber[1]));
            }

            // 数据进行no_wrap Base64编码
            String dataStrBase64 = Base64.encodeToString(contactJson.toString().getBytes("utf-8"), Base64.NO_WRAP);

            JSONObject syncSchemaJson = new JSONObject();
            JSONObject dataParamJson = new JSONObject();

            // 设置id_name为uid，即用户级个性化资源
            // 个性化资源使用方法可参见http://doc.xfyun.cn/aiui_mobile/的用户个性化章节
            dataParamJson.put("id_name", "uid");

            // 设置res_name为联系人
            dataParamJson.put("res_name", "IFLYTEK.telephone_contact");

            syncSchemaJson.put("param", dataParamJson);
            syncSchemaJson.put("data", dataStrBase64);

            // 传入的数据一定要为utf-8编码
            byte[] syncData = syncSchemaJson.toString().getBytes("utf-8");

            // 给该次同步加上自定义tag，在返回结果中可通过tag将结果和调用对应起来
            JSONObject paramJson = new JSONObject();
            paramJson.put("tag", "sync-tag");

            // 用schema数据同步上传联系人
            // 注：数据同步请在连接服务器之后进行，否则可能失败
            AIUIMessage syncAthena = new AIUIMessage(AIUIConstant.CMD_SYNC,
                    AIUIConstant.SYNC_DATA_SCHEMA, 0, paramJson.toString(), syncData);

            mAIUIAgent.sendMessage(syncAthena);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void syncQuery() {
        if (null == mAIUIAgent) {
            showTip("AIUIAgent 为空，请先创建");
            return;
        }


        if (TextUtils.isEmpty(mSyncSid)) {
            showTip("sid 为空");
            return;
        }

        try {
            // 构造查询json字符串，填入同步schema数据返回的sid
            JSONObject queryJson = new JSONObject();
            queryJson.put("sid", mSyncSid);

            // 发送同步数据状态查询消息，设置arg1为schema数据类型，params为查询字符串
            AIUIMessage syncQuery = new AIUIMessage(AIUIConstant.CMD_QUERY_SYNC_STATUS,
                    AIUIConstant.SYNC_DATA_SCHEMA, 0, queryJson.toString(), null);
            mAIUIAgent.sendMessage(syncQuery);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 返回通讯录中记录，格式如下::
     *  姓名$$电话号码
     * @return
     */
    public List<String> getContacts(){
        List<String> contacts = new ArrayList<>();

        ContentResolver cr = mContext.getContentResolver();
        Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        if(cursor != null) {
            while (cursor.moveToNext()) {
                int nameFieldColumnIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
                String personName = cursor.getString(nameFieldColumnIndex);
                String ContactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                Cursor phone = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + ContactId, null, null);

                //只取第一个联系电话
                while (phone.moveToNext()) {
                    String phoneNumber = phone.getString(phone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    phoneNumber = phoneNumber.replace("-", "");
                    phoneNumber = phoneNumber.replace(" ", "");
                    Log.i("call","personName " + personName +" phoneNumber " + phoneNumber);
                    contacts.add(personName + "$$" + phoneNumber);
                    break;
                }
            }

            cursor.close();
        }



        return contacts;
    }


    //申请录音权限
    public void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int i = ContextCompat.checkSelfPermission(this, permissions[0]);
            if (i != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, 321);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 321) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PERMISSION_GRANTED) {
                    this.finish();
                }
            }
        }
    }


}
