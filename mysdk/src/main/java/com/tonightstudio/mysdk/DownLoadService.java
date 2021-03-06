package com.tonightstudio.mysdk;

import android.app.DownloadManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.io.File;

/**
 * 更新APP
 *
 * <uses-permission android:name="android.permission.INTERNET" />
 * <service android:name=".DownLoadService" />
 *
 * 使用：
 * String apkUrl = "https://apkegg.mumayi.com/cooperation/2017/08/25/0/1/mumayishichangMumayiMarket_V4.1_mumayi_8611e.apk";
 * DownLoadService.start(this, apkUrl);
 */
public class DownLoadService extends Service {
    private static final String TAG = DownLoadService.class.getSimpleName();

    private DownloadManager manager;
    private DownloadCompleteReceiver receiver;
    private long mDownloadId;
    private String mMimeType;

    public static void start(Context context, String apkUrl) {
        Intent service = new Intent(context, DownLoadService.class);
        service.putExtra("apkUrl", apkUrl);
        context.startService(service);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String apkUrl = intent.getStringExtra("apkUrl");
        String fileName = apkUrl.substring(apkUrl.lastIndexOf("/") + 1, apkUrl.length());
        mMimeType = getMIMEType(apkUrl);


        manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        receiver = new DownloadCompleteReceiver();
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
        request.setTitle(fileName);
        request.setMimeType(mMimeType);
        mDownloadId = manager.enqueue(request);
        registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (receiver != null)
            unregisterReceiver(receiver);
        super.onDestroy();
    }

    class DownloadCompleteReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.isEmpty(action)) {
                return;
            }
            if (action.equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
                long downId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                Log.e(TAG, "mDownloadId:" + mDownloadId + "，downId:" + downId);
                if (mDownloadId != downId) {
                    return;
                }
                Uri downloadedFile = manager.getUriForDownloadedFile(downId);
                if (downloadedFile != null) {
                    File file = new File(getRealFilePath(context, downloadedFile));
                    if (file.exists()) {
                        openFile(file, context);
                    } else {
                        Toast.makeText(context, "下载失败", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, "下载失败", Toast.LENGTH_SHORT).show();
                }
                DownLoadService.this.stopSelf();
            }
        }
    }

    public String getRealFilePath(Context context, Uri uri) {
        if (null == uri) return null;
        final String scheme = uri.getScheme();
        String data = null;
        if (scheme == null)
            data = uri.getPath();
        else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            data = uri.getPath();
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.Images.ImageColumns.DATA}, null, null, null);
            if (null != cursor) {
                if (cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                    if (index > -1) {
                        data = cursor.getString(index);
                    }
                }
                cursor.close();
            }
        }
        return data;
    }

    /**
     * 重点在这里
     */
    public void openFile(File file, Context var1) {
        Intent var2 = new Intent(Intent.ACTION_VIEW);
        var2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Uri uriForFile = FileProvider.getUriForFile(var1, var1.getApplicationContext().getPackageName() + ".provider", file);
            var2.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            var2.setDataAndType(uriForFile, var1.getContentResolver().getType(uriForFile));
        } else {
            var2.setDataAndType(Uri.fromFile(file), mMimeType);//"application/vnd.android.package-archive"
        }
        try {
            var1.startActivity(var2);
        } catch (Exception var5) {
            var5.printStackTrace();
            Toast.makeText(var1, "没有找到打开此类文件的程序", Toast.LENGTH_SHORT).show();
        }
    }

    public static String getMIMEType(String apkUrl) {
        String fileName = apkUrl.substring(apkUrl.lastIndexOf("/") + 1, apkUrl.length());
        String var3 = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length()).toLowerCase();
        String var1 = MimeTypeMap.getSingleton().getMimeTypeFromExtension(var3);
        return var1;
    }

    public static String getMIMEType(File var0) {
        String var2 = var0.getName();
        String var3 = var2.substring(var2.lastIndexOf(".") + 1, var2.length()).toLowerCase();
        String var1 = MimeTypeMap.getSingleton().getMimeTypeFromExtension(var3);
        return var1;
    }
}