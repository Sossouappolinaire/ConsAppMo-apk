package com.consappmo.app;

import android.app.Activity;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Network;
import android.net.TrafficStats;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

public class MainActivity extends Activity {

    private WebView web;

    /** Pont appelé par ConsAppMo.html : window.ConsAppNative.stats() */
    public class Bridge {
        @JavascriptInterface
        public String stats() {
            try {
                JSONObject ifaces = new JSONObject();

                long mobileRx = TrafficStats.getMobileRxBytes();
                long mobileTx = TrafficStats.getMobileTxBytes();
                if (mobileRx >= 0 && mobileTx >= 0) {
                    ifaces.put("Données mobiles", pair(mobileRx, mobileTx));
                }

                long totalRx = TrafficStats.getTotalRxBytes();
                long totalTx = TrafficStats.getTotalTxBytes();
                if (totalRx >= 0 && totalTx >= 0) {
                    ifaces.put("Tout le téléphone", pair(totalRx, totalTx));
                }

                JSONObject out = new JSONObject();
                out.put("t", System.currentTimeMillis() / 1000.0);
                out.put("ifaces", ifaces);
                return out.toString();
            } catch (Exception e) {
                return "{\"t\":0,\"ifaces\":{}}";
            }
        }

        /** Wi-Fi connecté ? et son nom : window.ConsAppNative.wifi() */
        @JavascriptInterface
        public String wifi() {
            try {
                boolean on = false;
                ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                Network n = cm.getActiveNetwork();
                if (n != null) {
                    NetworkCapabilities c = cm.getNetworkCapabilities(n);
                    on = c != null && c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
                }
                String ssid = "";
                if (on) {
                    WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wi = wm.getConnectionInfo();
                    if (wi != null && wi.getSSID() != null) {
                        ssid = wi.getSSID().replace("\"", "");
                        if (ssid.equals("<unknown ssid>")) ssid = "";
                    }
                }
                JSONObject o = new JSONObject();
                o.put("wifi", on);
                o.put("ssid", ssid);
                return o.toString();
            } catch (Exception e) {
                return "{\"wifi\":false,\"ssid\":\"\"}";
            }
        }

        private JSONObject pair(long rx, long tx) throws Exception {
            JSONObject p = new JSONObject();
            p.put("rx", rx);
            p.put("tx", tx);
            return p;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);

        web.setWebViewClient(new WebViewClient());
        web.addJavascriptInterface(new Bridge(), "ConsAppNative");
        web.loadUrl("file:///android_asset/ConsAppMo.html");
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
