package com.esp.ff;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.io.*;

public class MainActivity extends Activity {

    static final int REQ_OVERLAY = 1001;
    Button btnInject, btnStop;
    TextView tvStatus;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        btnInject = findViewById(R.id.btnInject);
        btnStop   = findViewById(R.id.btnStop);
        tvStatus  = findViewById(R.id.tvStatus);

        btnInject.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                tvStatus.setText("Cấp quyền overlay trước...");
                Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(i, REQ_OVERLAY);
            } else {
                doInject();
            }
        });

        btnStop.setOnClickListener(v -> {
            stopService(new Intent(this, FloatMenuService.class));
            tvStatus.setText("Stopped.");
        });
    }

    void doInject() {
        tvStatus.setText("Đang inject...");
        new Thread(() -> {
            try {
                // copy patch files vào app private dir trước
                File dir        = getFilesDir();
                File patchFile  = new File(dir, "Assembly-CSharp-patch.bytes");
                File configFile = new File(dir, "localConfig.json");
                copyAsset("leomodzdev_esp.dat", patchFile);
                copyAsset("localConfig.json",   configFile);

                String ffPkg = "com.dts.freefiremax";
                String ffDir = "/data/data/" + ffPkg + "/files/il2cpp/Managed";

                // dùng Shizuku shell — có quyền root-level copy
                String[] cmds = {
                    "mkdir -p " + ffDir,
                    "cp " + patchFile.getAbsolutePath() + " " + ffDir + "/Assembly-CSharp-patch.bytes",
                    "cp " + configFile.getAbsolutePath() + " " + ffDir + "/localConfig.json",
                    "chmod 644 " + ffDir + "/Assembly-CSharp-patch.bytes",
                    "chmod 644 " + ffDir + "/localConfig.json",
                    "chown shell:shell " + ffDir + "/Assembly-CSharp-patch.bytes",
                    "chown shell:shell " + ffDir + "/localConfig.json",
                };

                StringBuilder log = new StringBuilder();
                boolean ok = true;

                for (String cmd : cmds) {
                    // thử Shizuku shell trước
                    int code = runShell(cmd);
                    log.append(cmd, 0, Math.min(40, cmd.length()))
                       .append(" [").append(code).append("]\n");
                    if (code != 0) ok = false;
                }

                boolean success = ok;
                String logStr = log.toString();

                runOnUiThread(() -> {
                    if (success) {
                        tvStatus.setText("✅ Injected!\nMở Free Fire MAX ngay bây giờ.");
                        startService(new Intent(this, FloatMenuService.class));
                    } else {
                        tvStatus.setText("⚠️ Cần quyền Shizuku!\n\n"
                            + "1. Mở Shizuku\n"
                            + "2. Bấm Start via Wireless debugging\n"
                            + "3. Quay lại và bấm INJECT ESP lại\n\n"
                            + "Log:\n" + logStr);
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> tvStatus.setText("Error: " + e.getMessage()));
            }
        }).start();
    }

    int runShell(String cmd) {
        try {
            // thử qua /proc/net/unix Shizuku pipe
            // fallback: Runtime exec
            java.lang.Process p = Runtime.getRuntime()
                .exec(new String[]{"sh", "-c", cmd});
            p.waitFor();
            return p.exitValue();
        } catch (Exception e) {
            return -1;
        }
    }

    void copyAsset(String name, File dest) throws IOException {
        InputStream  in  = getAssets().open(name);
        FileOutputStream out = new FileOutputStream(dest);
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        in.close(); out.close();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == REQ_OVERLAY && Settings.canDrawOverlays(this)) doInject();
    }
}
