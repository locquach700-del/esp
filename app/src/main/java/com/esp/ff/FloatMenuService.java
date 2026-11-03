package com.esp.ff;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.FileOutputStream;

public class FloatMenuService extends Service {

    WindowManager wm;
    View floatBtn;
    LinearLayout menuView;
    WindowManager.LayoutParams menuLp;
    boolean menuOpen = false;

    boolean espEnabled = true;
    boolean espBox     = true;
    boolean espLine    = true;
    boolean espHP      = true;
    boolean espHead    = true;

    @Override
    public int onStartCommand(Intent i, int f, int id) {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        buildFloatButton();
        return START_STICKY;
    }

    void buildFloatButton() {
        floatBtn = new View(this) {
            Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
            Paint tx = new Paint(Paint.ANTI_ALIAS_FLAG);
            {
                bg.setColor(0xCC1565C0);
                tx.setColor(Color.WHITE);
                tx.setTextSize(26f);
                tx.setTextAlign(Paint.Align.CENTER);
            }
            @Override
            protected void onDraw(Canvas c) {
                c.drawCircle(getWidth()/2f, getHeight()/2f, getWidth()/2f, bg);
                c.drawText("ESP", getWidth()/2f, getHeight()/2f + 9f, tx);
            }
        };

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
            120, 120,
            Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = 20; lp.y = 300;

        final int[] sX = {0}, sY = {0}, iX = {0}, iY = {0};
        floatBtn.setOnTouchListener((v, e) -> {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    sX[0] = (int) e.getRawX(); sY[0] = (int) e.getRawY();
                    iX[0] = lp.x; iY[0] = lp.y;
                    return false;
                case MotionEvent.ACTION_MOVE:
                    lp.x = iX[0] + (int)(e.getRawX() - sX[0]);
                    lp.y = iY[0] + (int)(e.getRawY() - sY[0]);
                    wm.updateViewLayout(floatBtn, lp);
                    return true;
            }
            return false;
        });
        floatBtn.setOnClickListener(v -> { if (menuOpen) closeMenu(); else openMenu(); });
        wm.addView(floatBtn, lp);
    }

    void openMenu() {
        menuOpen = true;
        menuView = new LinearLayout(this);
        menuView.setOrientation(LinearLayout.VERTICAL);
        menuView.setPadding(28, 28, 28, 28);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xEE0D1117);
        bg.setCornerRadius(20f);
        bg.setStroke(2, 0xFF1565C0);
        menuView.setBackground(bg);

        TextView title = new TextView(this);
        title.setText("ESP MOD FF");
        title.setTextColor(0xFF64B5F6);
        title.setTextSize(18f);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, 0, 0, 16);
        menuView.addView(title);

        menuView.addView(makeToggle("ESP Main",   espEnabled, v -> { espEnabled = ((Switch)v).isChecked(); writeFlags(); }));
        menuView.addView(makeToggle("ESP Box",    espBox,     v -> { espBox     = ((Switch)v).isChecked(); writeFlags(); }));
        menuView.addView(makeToggle("Health Bar", espHP,      v -> { espHP      = ((Switch)v).isChecked(); writeFlags(); }));
        menuView.addView(makeToggle("Head Dot",   espHead,    v -> { espHead    = ((Switch)v).isChecked(); writeFlags(); }));
        menuView.addView(makeToggle("Line",       espLine,    v -> { espLine    = ((Switch)v).isChecked(); writeFlags(); }));

        Button close = new Button(this);
        close.setText("CLOSE");
        close.setTextColor(Color.WHITE);
        GradientDrawable cbg = new GradientDrawable();
        cbg.setColor(0xFF1565C0);
        cbg.setCornerRadius(12f);
        close.setBackground(cbg);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clp.topMargin = 16;
        close.setLayoutParams(clp);
        close.setOnClickListener(v -> closeMenu());
        menuView.addView(close);

        menuLp = new WindowManager.LayoutParams(
            520, WindowManager.LayoutParams.WRAP_CONTENT,
            Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        menuLp.gravity = Gravity.TOP | Gravity.START;
        menuLp.x = 150; menuLp.y = 200;
        wm.addView(menuView, menuLp);
    }

    void closeMenu() {
        menuOpen = false;
        if (menuView != null) {
            try { wm.removeView(menuView); } catch (Exception ignored) {}
            menuView = null;
        }
    }

    LinearLayout makeToggle(String label, boolean state, View.OnClickListener l) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 10, 0, 10);

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(15f);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(tv);

        Switch sw = new Switch(this);
        sw.setChecked(state);
        sw.setOnClickListener(l);
        row.addView(sw);
        return row;
    }

    void writeFlags() {
        try {
            String path = "/storage/emulated/0/Android/data/com.dts.freefiremax/files/il2cpp/Managed/esp_flags";
            FileOutputStream fos = new FileOutputStream(path);
            fos.write(new byte[]{
                (byte)(espEnabled ? 1 : 0),
                (byte)(espBox     ? 1 : 0),
                (byte)(espHP      ? 1 : 0),
                (byte)(espHead    ? 1 : 0),
                (byte)(espLine    ? 1 : 0),
            });
            fos.close();
        } catch (Exception ignored) {}
    }

    @Override
    public IBinder onBind(Intent i) { return null; }

    @Override
    public void onDestroy() {
        closeMenu();
        if (floatBtn != null) try { wm.removeView(floatBtn); } catch (Exception ignored) {}
    }
}
