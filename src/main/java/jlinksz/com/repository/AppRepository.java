package jlinksz.com.repository;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by allen on 18年5月21日.
 */
public class AppRepository {

    public static final String LAST_VERSION_KEY = "app_last_version";
    private Activity mActivity;
    private String mLastVersion;
    private String mCurrentVersion;
    private HashMap<String,String> InstalledAppMap;

    public AppRepository(Activity mActivity) {
        this.mActivity = mActivity;
        mLastVersion = mActivity.getPreferences(Context.MODE_PRIVATE).
                getString(LAST_VERSION_KEY, "0");
        InstalledAppMap = new HashMap<>();
    }

    /**
     * 返回自上次查询后通讯录是否有变化（仅可调用一次）
     * @return
     */
    public boolean hasChanged(){
        mCurrentVersion = calculateContactVersion();
        boolean changed = !mCurrentVersion.equals(mLastVersion);
        SharedPreferences.Editor editor = mActivity.getPreferences(Context.MODE_PRIVATE).edit();
        editor.putString(LAST_VERSION_KEY, mCurrentVersion);
        editor.commit();
        mLastVersion = mCurrentVersion;

        return changed;
    }

    public List<String> getPackages(){
        List<String> apps = new ArrayList<>();
        List<PackageInfo> packages = mActivity.getPackageManager().getInstalledPackages(0);
        Log.i("call", "size" + packages.size());
        String applable = "";
        if(packages.size() > 0) {
            for (PackageInfo pi : packages) {
//                Log.i("call", "" + pi.applicationInfo.loadLabel(getPackageManager()).toString());
//                Log.i("call","packageName " +pi.packageName);
                applable = pi.applicationInfo.loadLabel(mActivity.getPackageManager()).toString();
                apps.add(applable);
                InstalledAppMap.put(applable,
                        pi.packageName);
            }
        }

        return apps;
    }


    private String calculateContactVersion(){
        return stringToMd5(join(getPackages(), ""));
    }

    /**
     * 将字符串version转换成MD5格式的
     *
     * @param s
     * @return
     */
    private String stringToMd5(String s) {
        byte[] value = s.getBytes();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(value);
            byte[] temp = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : temp) {
                sb.append(Integer.toHexString(b & 0xff));
            }
            String md5Version = sb.toString();

            return md5Version;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String join(List<String> source, String separator){
        StringBuilder builder = new StringBuilder();
        for(int index=0;index<source.size();index++){
            builder.append(source.get(index));
            if(index != source.size()-1){
                builder.append(separator);
            }
        }

        return builder.toString();
    }

}
