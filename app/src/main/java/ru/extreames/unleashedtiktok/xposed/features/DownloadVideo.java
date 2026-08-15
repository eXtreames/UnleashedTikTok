package ru.extreames.unleashedtiktok.xposed.features;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import ru.extreames.unleashedtiktok.xposed.utils.Utils;

public class DownloadVideo {
    private static String SAVED_VIDEO_URL = null;
    private static String SAVED_VIDEO_AUTHOR = null;

    public static void initialize(final XC_LoadPackage.LoadPackageParam lpParam) {
        Class<?> Aweme = XposedHelpers.findClass("com.ss.android.ugc.aweme.feed.model.Aweme", lpParam.classLoader);
        Class<?> ACLCommonShare = XposedHelpers.findClass("com.ss.android.ugc.aweme.feed.model.ACLCommonShare", lpParam.classLoader);
        Class<?> WeakDownloadHandler = XposedHelpers.findClass("com.ss.android.socialbase.downloader.thread.WeakDownloadHandler", lpParam.classLoader);

        Utils.retConst(Aweme, "isPreventDownload", false);
        Utils.retConst(Aweme, "needTTSWatermarkWhenDownload", false);

        Utils.retConst(ACLCommonShare, "getCode", 0);
        Utils.retConst(ACLCommonShare, "getShowType", 2);
        Utils.retConst(ACLCommonShare, "getTranscode", 1);

        XposedHelpers.findAndHookMethod(Aweme,
                "getAwemeACLShareInfo",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            String shareUrl = (String) XposedHelpers.getObjectField(param.thisObject, "shareUrl");
                            if (shareUrl == null)
                                return;

                            Object video = XposedHelpers.getObjectField(param.thisObject, "video");  // another way to call toString (which have this information)
                            if (video == null)
                                return;

                            Object playAddr = XposedHelpers.getObjectField(video, "playAddr");
                            if (playAddr == null)
                                return;

                            List<?> urlList = (List<?>) XposedHelpers.getObjectField(playAddr, "urlList");
                            if (urlList == null)
                                return;

                            for (Object url : urlList) {
                                SAVED_VIDEO_URL = (String) url;
                                Utils.log(Utils.DEBUG_LEVEL.INFO, "Sniffed video url - " + SAVED_VIDEO_URL);
                            }

                            Object author = XposedHelpers.getObjectField(param.thisObject, "author");
                            if (author != null)
                                SAVED_VIDEO_AUTHOR = (String) XposedHelpers.getObjectField(author, "uniqueId");
                            else
                                SAVED_VIDEO_AUTHOR = null;

                            if (SAVED_VIDEO_AUTHOR == null)
                                Utils.log(Utils.DEBUG_LEVEL.WARNING, "Error while getting `author`");

                        } catch (Exception e) {
                            Utils.log(Utils.DEBUG_LEVEL.ERROR, "Error while calling `getAwemeACLShareInfo` - " + e);
                        }
                    }
                });

        // TODO: Fix lagging message "Downloading... 0%"
        XposedBridge.hookAllMethods(
                WeakDownloadHandler,
                "handleMessage",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        Message msg = (Message) param.args[0];
                        // Utils.log(Utils.DEBUG_LEVEL.INFO, msg.toString());

                        if (msg.what == 1) { // STATUS_PREPARE
                            downloadVideo();

                            Message successMsg = Message.obtain(msg);
                            successMsg.what = -3; // STATUS_SUCCESS
                            successMsg.arg1 = msg.arg1;
                            successMsg.arg2 = msg.arg2;
                            XposedHelpers.callMethod(param.thisObject, "handleMessage", successMsg);

                            param.setResult(null);
                        }
                    }
                });
    }

    private static void downloadVideo() {
        if (SAVED_VIDEO_URL == null) {
            return;
        }

        final String generatedFileName = generateFileName(SAVED_VIDEO_AUTHOR);
        final String savedUrl = SAVED_VIDEO_URL;
        final Context context = getApplicationContext();

        if (context == null) {
            Utils.log(Utils.DEBUG_LEVEL.ERROR, "Context == null");
            return;
        }

        new Thread(() -> {
            ContentResolver resolver = context.getContentResolver();
            ContentValues values = new ContentValues();
            long currentTime = System.currentTimeMillis();

            values.put(MediaStore.Video.Media.DISPLAY_NAME, generatedFileName);
            values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            values.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + File.separator + "TikTok");

            values.put(MediaStore.Video.Media.DATE_ADDED, currentTime / 1000L);
            values.put(MediaStore.Video.Media.DATE_MODIFIED, currentTime / 1000L);
            values.put(MediaStore.Video.Media.DATE_TAKEN, currentTime);

            Uri uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                Utils.log(Utils.DEBUG_LEVEL.ERROR, "uri == null");
                return;
            }

            try {
                URL url = new URL(savedUrl);

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36");

                try (InputStream is = conn.getInputStream(); OutputStream os = resolver.openOutputStream(uri)) {
                    if (os == null)
                        throw new Exception("OutputStream == null");

                    byte[] buffer = new byte[8192];
                    int len;

                    while ((len = is.read(buffer)) != -1) {
                        os.write(buffer, 0, len);
                    }
                    os.flush();

                    new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, "Video successfully downloaded", Toast.LENGTH_SHORT).show());
                } catch (Exception e) {
                    resolver.delete(uri, null, null);
                    Utils.log(Utils.DEBUG_LEVEL.ERROR, "Error while writing video - " + e);
                }
            } catch (Exception e) {
                Utils.log(Utils.DEBUG_LEVEL.ERROR, "Error while downloading video - " + e);
            }
        }).start();

        SAVED_VIDEO_URL = null;
        SAVED_VIDEO_AUTHOR = null;
    }

    private static Context getApplicationContext() {
        try {
            Class<?> activityThreadClass = XposedHelpers.findClass("android.app.ActivityThread", null);

            Object activityThread = XposedHelpers.callStaticMethod(activityThreadClass, "currentActivityThread");
            if (activityThread == null)
                return null;

            return (Context) XposedHelpers.callMethod(activityThread, "getApplication");
        } catch (Throwable ignored) { }

        return null;
    }

    public static String generateFileName(String prefix) {
        long currentTimeMillis = System.currentTimeMillis();

        if (prefix == null)
            prefix = "";

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);
        String formattedDate = dateFormat.format(new Date(currentTimeMillis));

        return String.format(Locale.US, "%s_%s_%d.mp4", prefix, formattedDate, currentTimeMillis);
    }
}
