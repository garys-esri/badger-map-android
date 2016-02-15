package com.esri.idt.badgermap;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;

import com.esri.android.map.MapView;
import com.esri.android.oauth.OAuthView;
import com.esri.android.runtime.ArcGISRuntime;
import com.esri.core.io.UserCredentials;
import com.esri.core.map.CallbackListener;
import com.esri.core.portal.Portal;
import com.esri.core.portal.WebMap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class MainActivity extends AppCompatActivity {

    public static final String PREF_CREDENTIALS = "credentials";

    private MapView mapView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ArcGISRuntime.setClientId(getString(R.string.arcgis_client_id));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        String credString = prefs.getString(PREF_CREDENTIALS, null);
        UserCredentials creds = null;
        if (null != credString) {
            try {
                creds = (UserCredentials) deserializeObject(credString.getBytes());
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        if (null != creds) {
            try {
                instantiateMapView(creds);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            CallbackListener<UserCredentials> callbackListener = new CallbackListener<UserCredentials>() {

                @Override
                public void onCallback(final UserCredentials userCredentials) {
                    SharedPreferences.Editor prefsEditor = prefs.edit();
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    String credString = null;
                    try {
                        credString = new String(serializeObject(userCredentials));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (null != credString) {
                        prefsEditor.putString(PREF_CREDENTIALS, credString);
                        prefsEditor.apply();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                findViewById(R.id.viewGroup_login).setVisibility(View.GONE);
                                findViewById(R.id.viewGroup_main).setVisibility(View.VISIBLE);
                                try {
                                    instantiateMapView(userCredentials);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    System.out.println("D'oh: " + throwable.toString());
                }

            };
            OAuthView oAuthView = new OAuthView(
                    this,
                    getString(R.string.arcgis_portal_url),
                    getString(R.string.arcgis_client_id),
                    -1,
                    callbackListener);
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            oAuthView.setLayoutParams(params);
            ((ViewGroup) findViewById(R.id.viewGroup_login)).addView(oAuthView);
            findViewById(R.id.viewGroup_main).setVisibility(View.GONE);
            findViewById(R.id.viewGroup_login).setVisibility(View.VISIBLE);
        }
    }

    private void instantiateMapView(UserCredentials creds) throws Exception {
        new AsyncTask<UserCredentials, Integer, Void>() {

            @Override
            protected Void doInBackground(UserCredentials... credsArray) {
                Portal portal = new Portal(getString(R.string.arcgis_portal_url), credsArray[0]);
                try {
                    final WebMap webMap = WebMap.newInstance(getString(R.string.web_map_item_id), portal);
                    if (null != webMap) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mapView = new MapView(MainActivity.this, webMap, null, null);
                                ViewGroup viewGroup_mapContainer = (ViewGroup) findViewById(R.id.viewGroup_mapContainer);
                                viewGroup_mapContainer.removeAllViews();
                                viewGroup_mapContainer.addView(mapView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute(creds);
    }

    /**
     * Adapted from http://stackoverflow.com/a/2836659/720773
     * @param obj
     * @return
     * @throws IOException
     */
    private static byte[] serializeObject(Serializable obj) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(obj);
            byte[] bytes = bos.toByteArray();
            return Base64.encode(bytes, Base64.DEFAULT);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ex) {
                // ignore close exception
            }
            try {
                bos.close();
            } catch (IOException ex) {
                // ignore close exception
            }
        }
    }

    /**
     * Adapted from http://stackoverflow.com/a/2836659/720773
     * @param bytes
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private static Object deserializeObject(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(Base64.decode(bytes, Base64.DEFAULT));
        ObjectInput in = null;
        try {
            in = new ObjectInputStream(bis);
            Object o = in.readObject();
            return o;
        } finally {
            try {
                bis.close();
            } catch (IOException ex) {
                // ignore close exception
            }
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                // ignore close exception
            }
        }
    }

}
