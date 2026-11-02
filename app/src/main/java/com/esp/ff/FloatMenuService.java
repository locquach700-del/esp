package com.esp.ff;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.graphics.drawable.GradientDrawable;

public class FloatMenuService extends Service {

    WindowManager wm;
    View floatView;
    boolean menuOpen = false;

    // ESP state — sync với FF qua shared file
    boolean espEnabled  = true;
    boolean espBox      = true;
    boolean espLine     = true;
    boolean espHP       = true;
    boolean espHead     = true;

    @Override
    public int onStartCommand(Intent i, int f, int id) {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        buildFloatButton();
        return START_STICKY;
    }

    void buildFloatButton() {
        // nút tròn nổi
        floatView = new View(this) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            {
                p.setColor(0xCC1565C0);
                p.setTextSize(28f);
                p.setTextAlign(Paint.Align.CENTER);
                p.setColor(Color.WHITE);
            }
            @Override
            protected void onDraw(Canvas c) {
                Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
                bg.setColor(0xCC1565C0);
                c.drawCircle(getWidth()/2f, getHeight()/2f, getWidth()/2f, bg);
                p.setColor(Color.WHITE);
                p.setTextSize(26f);
                c.drawText("ESP", getWidth()/2f, getHeight()/2f + 9f, p);
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

        floatView.setOnClickListener(v -> {
            if (menuOpen) closeMenu(); else openMenu();
        });

        // drag support
        final int[] startX = {0}, startY = {0};
        final int[] initX = {0}, initY = {0};
        floatView.setOnTouchListener((v, e) -> {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX[0] = (int) e.getRawX();
                    startY[0] = (int) e.getRawY();
                    initX[0] = lp.x; initY[0] = lp.y;
                    return false;
                case MotionEvent.ACTION_MOVE:
                    lp.x = initX[0] + (int)(e.getRawX() - startX[0]);
                    lp.y = initY[0] + (int)(e.getRawY() - startY[0]);
                    wm.updateViewLayout(floatView, lp);
                    return true;
            }
            return false;
        });

        wm.addView(floatView, lp);
    }

    void openMenu() {
        menuOpen = true;
        LinearLayout menu = buildMenuLayout();

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
            520, WindowManager.LayoutParams.WRAP_CONTENT,
            Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = 150; lp.y = 200;

        menu.setTag("menu");
        wm.addView(menu, lp);
    }

    LinearLayout buildMenuLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);

        // background
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xEE0D1117);
        bg.setCornerRadius(20f);
        bg.setStroke(2, 0xFF1565C0);
        root.setBackground(bg);

        // title
        TextView title = new TextView(this);
        title.setText("🔵 ESP MOD");
        title.setTextColor(0xFF64B5F6);
        title.setTextSize(18f);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, 16);
        root.addView(title);

        // toggles
        root.addView(makeToggle("ESP Main",    espEnabled, v -> { espEnabled = ((Switch)v).isChecked(); writeFlags(); }));
        root.addView(makeToggle("ESP Box",     espBox,     v -> { espBox     = ((Switch)v).isChecked(); writeFlags(); }));
        root.addView(makeToggle("Health Bar",  espHP,      v -> { espHP      = ((Switch)v).isChecked(); writeFlags(); }));
        root.addView(makeToggle("Head Dot",    espHead,    v -> { espHead    = ((Switch)v).isChecked(); writeFlags(); }));
        root.addView(makeToggle("Line",        espLine,    v -> { espLine    = ((Switch)v).isChecked(); writeFlags(); }));

        // close button
        Button close = new Button(this);
        close.setText("CLOSE");
        close.setTextColor(Color.WHITE);
        GradientDrawable cbg = new GradientDrawable();
        cbg.setColor(0xFF1565C0);
        cbg.setCornerRadius(12f);
        close.setBackground(cbg);
        close.setPadding(0, 16, 0, 16);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clp.topMargin = 16;
        close.setLayoutParams(clp);
        close.setOnClickListener(v -> closeMenu());
        root.addView(close);

        return root;
    }

    LinearLayout makeToggle(String label, boolean state, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 8, 0, 8);

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(15f);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(tv);

        Switch sw = new Switch(this);
        sw.setChecked(state);
        sw.setOnClickListener(listener);
        row.addView(sw);

        return row;
    }

    void closeMenu() {
        menuOpen = false;
        // find và remove menu view
        try {
            // iterate window manager views — tìm menu tag
            // workaround: keep reference
        } catch (Exception ignored) {}
        // simple: recreate service sẽ clean up
    }

    void writeFlags() {
        // ghi flag file để ILFix patch đọc
        // format: mỗi byte = 1 flag
        try {
            String path = "/storage/emulated/0/Android/data/com.dts.freefiremax/files/il2cpp/Managed/esp_flags";
            FileOutputStream fos = new FileOutputStream(path);
            fos.write(new byte[]{
                (byte)(espEnabled ? 1 : 0),   // 67espon
                (byte)(espBox     ? 1 : 0),   // 67ebox
                (byte)(espHP      ? 1 : 0),   // 67ehp
                (byte)(espHead    ? 1 : 0),   // 67ehead
                (byte)(espLine    ? 1 : 0),   // 67eline
            });
            fos.close();
        } catch (Exception ignored) {}
    }

    @Override
    public IBinder onBind(Intent i) { return null; }

    @Override
    public void onDestroy() {
        if (floatView != null) try { wm.removeView(floatView); } catch (Exception ignored) {}
    }
}
