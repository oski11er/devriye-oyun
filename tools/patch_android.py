"""Capacitor'un ürettiği Android projesini oyun için ayarlar: yatay ekran, tam ekran, ekran açık kalsın."""
import re, pathlib
root = pathlib.Path('android/app/src/main')
man = root / 'AndroidManifest.xml'
s = man.read_text()
if 'screenOrientation' not in s:
    s = s.replace('android:name=".MainActivity"', 'android:name=".MainActivity"\n            android:screenOrientation="sensorLandscape"\n            android:hardwareAccelerated="true"', 1)
man.write_text(s)
st = root / 'res/values/styles.xml'
x = st.read_text()
x = x.replace('</style>', '    <item name="android:windowFullscreen">true</item>\n    </style>')
st.write_text(x)
act = root / 'java/com/devriye/oyun/MainActivity.java'
act.write_text('''package com.devriye.oyun;

import android.os.Bundle;
import android.view.WindowManager;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideBars();
    }
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideBars();
    }
    private void hideBars() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat c = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        c.hide(WindowInsetsCompat.Type.systemBars());
        c.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }
}
''')
print('Android projesi ayarlandı')
