package com.atn.tendy.utils;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.atn.tendy.Dtos;
import com.atn.tendy.R;
import com.wang.avi.AVLoadingIndicatorView;


//uses compile 'com.afollestad.material-dialogs:core:0.9.4.5'
public class Dialogs {

    public static void showDialog(Context context, String title, String text) {
        MaterialDialog m = new MaterialDialog.Builder(context)
                .title(title)
                .content(text)
                .positiveText(R.string.gotIt)
                .positiveColor(context.getResources().getColor(R.color.green))
                .typeface("open.ttf", "open.ttf")
                .theme(Theme.DARK)
                .build();
        showDialog(m);
    }

    public static void showDialog(Context context, String title, String text, final Runnable completion) {
        MaterialDialog m = new MaterialDialog.Builder(context)
                .title(title)
                .content(text)
                .positiveText(R.string.gotIt)
                .positiveColor(context.getResources().getColor(R.color.green))
                .dismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (completion != null)
                            completion.run();
                    }
                })
                .typeface("open.ttf", "open.ttf")
                .theme(Theme.DARK)
                .build();
        showDialog(m);
    }

    public static void showCustomDialogOneButton(Context context, int resId, final Runnable onDismiss) {
        View v = View.inflate(context, resId, null);
        Utils.setFontToViewGroup(context, (ViewGroup) v, "open");
        final MaterialDialog m = new MaterialDialog.Builder(context)
                .customView(v, false)
                .dismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (onDismiss != null) onDismiss.run();
                    }
                })
                .typeface("open.ttf", "open.ttf")
                .build();
        m
                .getCustomView()
                .findViewById(R.id.button)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        m.dismiss();
                    }
                });
        showDialog(m);
    }

    public static MaterialDialog showProgressIndicator(Context context) {
        try {
            View v = View.inflate(context, R.layout.progress_indicator, null);
            Utils.setFontToViewGroup(context, (ViewGroup) v, "open");
            AVLoadingIndicatorView avi = (AVLoadingIndicatorView) v.findViewById(R.id.avi);
            avi.show();
            final MaterialDialog m = new MaterialDialog.Builder(context)
                    .customView(v, false)
                    .cancelable(false)
                    .typeface("open.ttf", "open.ttf")
                    .build();
            showDialog(m);
            return m;
        } catch (Exception e) {
            return null;
        }
    }

    public static void showNoInternetDialog(final Context context, final Runnable onInternetBack) {
        try {
            if(Utils.haveInternetConnection(context)){
                onInternetBack.run();
                return;
            }
            View v = View.inflate(context, R.layout.no_internet_indicator, null);
            Utils.setFontToViewGroup(context, (ViewGroup) v, "open");
            AVLoadingIndicatorView avi = (AVLoadingIndicatorView) v.findViewById(R.id.avi);
            avi.show();
            final MaterialDialog m = new MaterialDialog.Builder(context)
                    .customView(v, false)
                    .cancelable(false)
                    .typeface("open.ttf", "open.ttf")
                    .build();
            showDialog(m);
            final Handler handler = new Handler(Looper.getMainLooper());
            Runnable loop = new Runnable() {
                @Override
                public void run() {
                    if(Utils.haveInternetConnection(context)){
                        onInternetBack.run();
                        handler.removeCallbacks(this);
                        dismissDialog(m);
                        return;
                    } else{
                        handler.postDelayed(this, 3000);
                    }
                }
            };
            loop.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static MaterialDialog showSuccessfullySaved(Context context) {
        View v = View.inflate(context, R.layout.successfully_saved, null);
        Utils.setFontToViewGroup(context, (ViewGroup) v, "open");
        final MaterialDialog m = new MaterialDialog.Builder(context)
                .customView(v, false)
                .cancelable(false)
                .build();
        showDialog(m);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                m.dismiss();
            }
        }, 1500);
        return m;
    }

    public static MaterialDialog showSuccessfullySaved(Context context, final Runnable completion) {
        View v = View.inflate(context, R.layout.successfully_saved, null);
        Utils.setFontToViewGroup(context, (ViewGroup) v, "open");
        final MaterialDialog m = new MaterialDialog.Builder(context)
                .customView(v, false)
                .cancelable(false)
                .build();
        showDialog(m);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                m.dismiss();
                completion.run();
            }
        }, 1500);
        return m;
    }

    public static void showYesNoDialog(Context context,
                                       String title,
                                       String text,
                                       String yes,
                                       String no,
                                       final Runnable onYes,
                                       final Runnable onNo) {
        MaterialDialog.SingleButtonCallback callback = new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                if (which == DialogAction.POSITIVE && onYes != null) onYes.run();
                if (which == DialogAction.NEGATIVE && onNo != null) onNo.run();
            }
        };
        MaterialDialog m = new MaterialDialog.Builder(context)
                .title(title)
                .content(text)
                .positiveText(yes)
                .positiveColor(context.getResources().getColor(R.color.green))
                .onPositive(callback)
                .negativeText(no)
                .negativeColor(context.getResources().getColor(R.color.white))
                .onNegative(callback)
                .typeface("open.ttf", "open.ttf")
                .theme(Theme.DARK)
                .build();
        showDialog(m);
    }

    public static MaterialDialog showProfileDialog(Context context, Dtos.Profile profile) {
        View v = profile.setupDiscoveryItem(context, null);
        v.findViewById(R.id.date).setVisibility(View.GONE);
        final MaterialDialog m = new MaterialDialog.Builder(context)
                .customView(v, false)
                .cancelable(true)
                .build();
        showDialog(m);
        return m;
    }

    public static void dismissDialog(final Dialog d) {

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    Logs.log("dismissDialog", "dismissDialog");
                    if (d != null)
                        d.dismiss();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 300);

    }

    public static void showDialog(final Dialog d) {
        try {
            d.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static MaterialDialog showInputAndButtonDialog(Context context,
                                                String title,
                                                String subtitle,
                                                final InputAndButtonDialogClickListener inputAndButtonDialogClickListener) {
        View v = View.inflate(context, R.layout.input_and_button_dialog, null);
        Utils.setFontToViewGroup(context, (ViewGroup) v, "open");
        final MaterialDialog m = new MaterialDialog.Builder(context)
                .customView(v, false)
                .typeface("open.ttf", "open.ttf")
                .build();
        ((TextView)m.getCustomView().findViewById(R.id.title)).setText(title);
        ((TextView)m.getCustomView().findViewById(R.id.subtitle)).setText(subtitle);
        final EditText input = (EditText) m.getCustomView().findViewById(R.id.input);
        m.getCustomView().findViewById(R.id.send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inputAndButtonDialogClickListener.onClick(input.getText().toString());
            }
        });
        showDialog(m);
        return m;
    }

    public static MaterialDialog showImageConfirmationDialog(Context context,
                                                             final Bitmap bitmap,
                                                             final Runnable completion) {
        View v = View.inflate(context, R.layout.image_confirmation_dialog, null);
        final MaterialDialog m = new MaterialDialog.Builder(context)
                .customView(v, false)
                .cancelable(true)
                .build();

        com.jsibbold.zoomage.ZoomageView img = m.getCustomView().findViewById(R.id.image);
        img.setImageBitmap(bitmap);
        m.getCustomView().findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(completion != null) completion.run();
                m.dismiss();
            }
        });
        showDialog(m);
        return m;
    }


    public interface InputAndButtonDialogClickListener {
        public void onClick(String inputString);
    }
}
