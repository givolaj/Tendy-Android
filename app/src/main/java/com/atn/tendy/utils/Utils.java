package com.atn.tendy.utils;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.AnyRes;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.AnticipateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.atn.tendy.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.nostra13.universalimageloader.core.DisplayImageOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static com.atn.tendy.chat.ChatActivity.CHAT_ID_SEPARATOR;
import static com.facebook.FacebookSdk.getApplicationContext;

/**
 * Created by Admin on 01/05/2017.
 */

public class Utils {
    public static void setFontToViewGroup(Context context, ViewGroup parent, String fontName) {
        for (int i = parent.getChildCount() - 1; i >= 0; i--) {
            final View child = parent.getChildAt(i);
            if (child instanceof ViewGroup) {
                setFontToViewGroup(context, (ViewGroup) child, fontName);
            } else if (child instanceof TextView) {
                ((TextView) child).setTypeface(getFont(context, fontName), ((TextView) child).getTypeface().getStyle());
            }
        }
    }

    private static Typeface getFont(Context context, String fontName) {
        return Typeface.createFromAsset(context.getAssets(), "fonts/" + fontName + ".ttf");
    }

    public static void setTextColor(ViewGroup parent, int color){
        for(int i=0;i<parent.getChildCount();i++){
            if(parent.getChildAt(i) instanceof TextView){
                ((TextView) parent.getChildAt(i)).setTextColor(color);
            } else if(parent.getChildAt(i) instanceof ViewGroup){
                setTextColor((ViewGroup) parent.getChildAt(i), color);
            }
        }
    }

    public static void bubbleUpAnimation(Context context, final View v, int duration, int delay) {
        Animation animation = AnimationUtils.loadAnimation(context, R.anim.bubble);
        animation.setInterpolator(new BubbleInterpolator(0.2, 20));
        animation.setStartOffset(delay);
        animation.setDuration(duration);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                v.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        v.startAnimation(animation);
    }

    public static void bubbleDownAnimation(Context context, final View v, int duration, int delay, final Runnable r) {
        if (v.getVisibility() == View.GONE) return;
        Animation animation = AnimationUtils.loadAnimation(context, R.anim.vanish);
        animation.setInterpolator(new AnticipateInterpolator());
        animation.setStartOffset(delay);
        animation.setDuration(duration);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                v.setVisibility(View.GONE);
                if (r != null) r.run();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        v.startAnimation(animation);
    }

    public static void fadeInAnimation(Context context, final View v, int duration, int delay) {
        Animation animation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
        animation.setStartOffset(delay);
        animation.setDuration(duration);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                v.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        v.startAnimation(animation);
    }

    public static void fadeOutAnimation(Context context, final View v, int duration, int delay) {
        if (v.getVisibility() == View.GONE) return;
        Animation animation = AnimationUtils.loadAnimation(context, R.anim.fade_out);
        animation.setInterpolator(new AnticipateInterpolator());
        animation.setStartOffset(delay);
        animation.setDuration(duration);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                v.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        v.startAnimation(animation);
    }

    public static void slideUpInAnimation(Context context, final View v, int duration, int delay) {
        Animation animation = AnimationUtils.loadAnimation(context, R.anim.slide_up_in);
        animation.setStartOffset(delay);
        animation.setDuration(duration);
        animation.setInterpolator(new BubbleInterpolator(0.2, 5));
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                v.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        v.startAnimation(animation);
    }

    public static void slideRightOutAnimation(Context context, final View v, int duration, int delay, final Runnable r) {
        if (v.getVisibility() == View.GONE) return;
        Animation animation = AnimationUtils.loadAnimation(context, R.anim.slide_right_out);
        animation.setInterpolator(new AnticipateInterpolator());
        animation.setStartOffset(delay);
        animation.setDuration(duration);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                v.setVisibility(View.GONE);
                if (r != null) r.run();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        v.startAnimation(animation);
    }

    public static ArrayList<JSONObject> listFromJSONArray(JSONArray arr) {
        try {
            ArrayList<JSONObject> list = new ArrayList<>();
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    list.add(arr.getJSONObject(i));
                }
            }
            return list;
        } catch (Exception e) {
            return null;
        }
    }

    public static Handler runWithDelay(Runnable r, long delay) {
        Handler h = new Handler(Looper.getMainLooper());
        h.postDelayed(r, delay);
        return h;
    }

    public static boolean checkGooglePlayServiceAvailability(Context context, int versionCode) {
        int statusCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        return (statusCode == ConnectionResult.SUCCESS)
                && (GooglePlayServicesUtil.GOOGLE_PLAY_SERVICES_VERSION_CODE >= versionCode);

    }

    public static DisplayImageOptions displayImageOptions = new DisplayImageOptions.Builder()
            .showImageOnLoading(R.drawable.ic_stub)
            .showImageForEmptyUri(R.drawable.ic_stub)
            .showImageOnFail(R.drawable.ic_stub)
            .delayBeforeLoading(0)
            .resetViewBeforeLoading(true)
            .cacheOnDisk(true)
            .cacheInMemory(true)
            .build();

    public static boolean haveInternetConnection(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static Bitmap getImageWithPortraitOrientation(Bitmap image, Uri imageUri){
        try {
            ExifInterface exif = new ExifInterface(imageUri.getPath());
            if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("6")) {
                image = rotate(image, 90);
            } else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("8")) {
                image = rotate(image, 270);
            } else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("3")) {
                image = rotate(image, 180);
            }
            return image;
        } catch (Exception e){
            e.printStackTrace();
            return image;
        }
    }

    public static Bitmap resizeBitmap(Bitmap bitmap, int maxWidthOrHeight) {
        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            float excessSizeRatio = width > height ? width / maxWidthOrHeight : height / maxWidthOrHeight;
            int newWidth = (int)(width / excessSizeRatio);
            int newHeight = (int) (height / excessSizeRatio);
            Bitmap scaledBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);
            float ratioX = newWidth / (float) bitmap.getWidth();
            float ratioY = newHeight / (float) bitmap.getHeight();
            float middleX = newWidth / 2.0f;
            float middleY = newHeight / 2.0f;

            Matrix scaleMatrix = new Matrix();
            scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

            Canvas canvas = new Canvas(scaledBitmap);
            canvas.setMatrix(scaleMatrix);
            canvas.drawBitmap(bitmap, middleX - bitmap.getWidth() / 2, middleY - bitmap.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG));

            return scaledBitmap;
        } catch (Exception e){
            return bitmap;
        }
    }

    public static float dpToPx(Context context, float valueInDp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valueInDp, metrics);
    }

    public static Bitmap getCircleBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getWidth());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getWidth() / 2,
                bitmap.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    public static String getChatId(String me, String buddy) {
        String chatId = "bug";
        try {
            if (me.compareTo(buddy) > 0) {
                chatId = buddy + CHAT_ID_SEPARATOR + me;
            } else {
                chatId = me + CHAT_ID_SEPARATOR + buddy;
            }
            chatId = chatId.replace(".", "_");
        } catch (Exception e){e.printStackTrace();}
        return chatId;
    }

    public static long SECOND = 1000;
    public static long MINUTE = 60 * SECOND;
    public static long HOUR = 60 * MINUTE;
    public static String getTimeForTimer(long millis) {
        String time = "";
        long hours = (millis / HOUR);
        long minutes = (millis % HOUR) / MINUTE;
        long seconds = (millis % MINUTE) / SECOND;
        time += hours < 10 ? ("0" + hours) : hours;
        time += ":";
        time += minutes < 10 ? ("0" + minutes) : minutes;
        time += ":";
        time += seconds < 10 ? ("0" + seconds) : seconds;
        return time;
    }

    public static int countSubstring(final String string, final String substring)
    {
        int count = 0;
        int idx = 0;

        while ((idx = string.indexOf(substring, idx)) != -1)
        {
            idx++;
            count++;
        }

        return count;
    }

    public static void sendNotification(Context context, RemoteViews contentView, RemoteViews bigContentView, RemoteViews headUpContentView, PendingIntent pendingIntent, int id, boolean autoCancel){
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_profile)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVibrate(new long[]{250, 250})
                .setContent(contentView)
                .setCustomBigContentView(bigContentView)
                .setCustomHeadsUpContentView(headUpContentView)
                .setStyle(new NotificationCompat.BigTextStyle())
                .setSound(defaultSoundUri)
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_LIGHTS | NotificationCompat.DEFAULT_SOUND)
                .setAutoCancel(autoCancel)
                .setContentIntent(pendingIntent);
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, notificationBuilder.build());
    }

    public static final Uri getUriToDrawable(@NonNull Context context,
                                             @AnyRes int drawableId) {
        Uri imageUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
                "://" + context.getResources().getResourcePackageName(drawableId)
                + '/' + context.getResources().getResourceTypeName(drawableId)
                + '/' + context.getResources().getResourceEntryName(drawableId) );
        return imageUri;
    }

    public static void closeKeyboard(Activity activity){
        try {
            View view = activity.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }catch (Exception e){e.printStackTrace();}
    }

    public static File createImageFile(Context context) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
            File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File image = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );
            return image;
        } catch (Exception e){
            return null;
        }
    }

    private static final String IMAGES_DIRECTORY = "/tendy";
    public static void saveImageToExternal(Context context, String imgName, Bitmap bm) throws IOException {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES+IMAGES_DIRECTORY); //Creates app specific folder
        path.mkdirs();
        File imageFile = new File(path, imgName + ".png");
        if (imageFile.exists())
            return;
        imageFile.createNewFile();
        FileOutputStream out = new FileOutputStream(imageFile);
        try{
            bm.compress(Bitmap.CompressFormat.PNG, 100, out); // Compress Image
            out.flush();
            out.close();

            MediaScannerConnection.scanFile(context,
                    new String[] { imageFile.getAbsolutePath() },
                    null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                public void onScanCompleted(String path, Uri uri) {
                    Logs.log("ExternalStorage", "Scanned " + path + ":");
                    Logs.log("ExternalStorage", "-> uri=" + uri);
                }
            });
        } catch(Exception e) {
            throw new IOException();
        }
    }

    public static int uniqueNumberFromString(String sender) {
        int n = 0;
        for(int i=0;i<sender.length();i++)
            n += sender.charAt(i)-'0';
        return n;
    }

    public static long getUnifiedTime(){
        Logs.log("current", System.currentTimeMillis() + "");
        Logs.log("timeDiff", PushServer.timeDiff + "");
        return System.currentTimeMillis() + PushServer.timeDiff;
    }

    public static long parseLong(String l) {
        try{
            if(l.contains("."))
                return Long.parseLong(l.split(".")[0]);
            else
                return Long.parseLong(l);
        } catch (Exception e){
            return 0;
        }
    }

    public static int parseInt(String l) {
        try{
            if(l.contains("."))
                return Integer.parseInt(l.split(".")[0]);
            else
                return Integer.parseInt(l);
        } catch (Exception e){
            return 0;
        }
    }

    public static void enableWifiIfNeeded(){
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if(!wifiManager.isWifiEnabled())
            wifiManager.setWifiEnabled(true);
    }

    public static boolean isNotificationServiceRunning(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        String enabledNotificationListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners");
        String packageName = context.getPackageName();
        return enabledNotificationListeners != null && enabledNotificationListeners.contains(packageName);
    }

    public static boolean isRTL() {
        return isRTL(Locale.getDefault());
    }

    public static boolean isRTL(Locale locale) {
        final int directionality = Character.getDirectionality(locale.getDisplayName().charAt(0));
        return directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
                directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC;
    }

    public static String getSha1(Context context){
        PackageManager pm = context.getPackageManager();
        String packageName = context.getPackageName();
        int flags = PackageManager.GET_SIGNATURES;
        PackageInfo packageInfo = null;
        try {
            packageInfo = pm.getPackageInfo(packageName, flags);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        Signature[] signatures = packageInfo.signatures;
        byte[] cert = signatures[0].toByteArray();
        InputStream input = new ByteArrayInputStream(cert);
        CertificateFactory cf = null;
        try {
            cf = CertificateFactory.getInstance("X509");
        } catch (Exception e) {
            e.printStackTrace();
        }
        X509Certificate c = null;
        try {
            c = (X509Certificate) cf.generateCertificate(input);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String hexString = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] publicKey = md.digest(c.getEncoded());
            hexString = byte2HexFormatted(publicKey);
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hexString;
    }

    public static String byte2HexFormatted(byte[] arr) {
        StringBuilder str = new StringBuilder(arr.length * 2);
        for (int i = 0; i < arr.length; i++) {
            String h = Integer.toHexString(arr[i]);
            int l = h.length();
            if (l == 1) h = "0" + h;
            if (l > 2) h = h.substring(l - 2, l);
            str.append(h.toUpperCase());
            if (i < (arr.length - 1)) str.append(':');
        }
        return str.toString();
    }

    public static String getHashKey(Context context) {
        try {
            String res = "";
            PackageInfo info = context.getPackageManager().getPackageInfo("com.atn.tendy", PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                String hashKey = new String(Base64.encode(md.digest(), Base64.DEFAULT));
                res += hashKey + ",";
            }
            return res;
        }  catch (Exception e) {

        }
        return "failed";
    }

    public static String getAgeFromDate(String DDMMYYYY){
        try {
            Logs.log(DDMMYYYY);
            String[] parts = DDMMYYYY.split("/");
            int year = (int) Utils.parseLong(parts[2]);
            int month = (int) Utils.parseLong(parts[0]);
            int day = (int) Utils.parseLong(parts[1]);

            Calendar dob = Calendar.getInstance();
            Calendar today = Calendar.getInstance();
            dob.set(year, month, day);

            int age = today.get(Calendar.YEAR) - year;
            if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }
            Integer ageInt = new Integer(age);
            String ageS = ageInt.toString();

            return ageS;
        } catch (Exception e){
            return "";
        }
    }

    public static Bitmap rotate(Bitmap bitmap, int degree) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mtx = new Matrix();
        mtx.postRotate(degree);

        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }

    public static void requestDisableDoze(Context context){
        if (Build.VERSION.SDK_INT >= 23) {
            Intent intent = new Intent();
            String packageName = context.getPackageName();
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm.isIgnoringBatteryOptimizations(packageName))
                intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            else {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
            }
            context.startActivity(intent);
        }
    }

    public static int getAgeFromFbObject(JSONObject object)
    {
        try{
            String[] bd = object.getString("birthday").split("/");
            Calendar dob = Calendar.getInstance();
            Calendar today = Calendar.getInstance();

            dob.set(Utils.parseInt(bd[2]), Utils.parseInt(bd[1]) - 1, Utils.parseInt(bd[0]));

            int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);

            if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)){
                age--;
            }
            return age;
        }catch (Exception e){e.printStackTrace(); return 0;}
    }
}
