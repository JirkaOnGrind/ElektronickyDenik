package com.jirka.denik;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.JavascriptInterface;

public class AndroidBridge {
    private Context context;
    private SharedPreferences prefs;

    public AndroidBridge(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences("DenikOfflineData", Context.MODE_PRIVATE);
    }

    // Tuto metodu zavolá JavaScript: Android.saveData('klic', 'data')
    @JavascriptInterface
    public void saveData(String key, String value) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.apply();
    }

    // Tuto metodu zavolá JavaScript: var data = Android.getData('klic')
    @JavascriptInterface
    public String getData(String key) {
        return prefs.getString(key, "[]"); // Vrátí prázdné pole [], pokud nic nenajde
    }
}