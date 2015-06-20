package com.angelhack.nellie;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;

import android.content.Context;
import android.widget.TextView;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.UUID;

public class MainActivity extends Activity {

    private PebbleKit.PebbleDataReceiver mReceiver;
    private TextView buttonView;
    private UUID appUUID = UUID.fromString("036b24a1-7fa5-4acc-aef1-76296ab4d984");

    private static final int
            KEY_BUTTON_EVENT = 0,
            BUTTON_EVENT_UP = 1,
            BUTTON_EVENT_DOWN = 2,
            BUTTON_EVENT_SELECT = 3,
            KEY_VIBRATION = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        boolean connected = PebbleKit.isWatchConnected(getApplicationContext());
        Log.i(getLocalClassName(), "Pebble is " + (connected ? "connected" : "not connected"));

        PebbleKit.startAppOnPebble(getApplicationContext(), appUUID);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mReceiver = new PebbleKit.PebbleDataReceiver(appUUID) {

            @Override
            public void receiveData(Context context, int transactionId, PebbleDictionary data) {
                //ACK the message
                PebbleKit.sendAckToPebble(context, transactionId);

                Log.i(getLocalClassName(), "Pebble is communicating");


                //Check the key exists
                if (data.getUnsignedIntegerAsLong(KEY_BUTTON_EVENT) != null) {
                    int button = data.getUnsignedIntegerAsLong(KEY_BUTTON_EVENT).intValue();

                    switch (button) {
                        case BUTTON_EVENT_UP:
                            startActivity(new Intent(getApplicationContext(), PictureAction.class));
                            break;
                        case BUTTON_EVENT_DOWN:
                            break;
                        case BUTTON_EVENT_SELECT:
                            break;
                    }

                    PebbleDictionary dict = new PebbleDictionary();
                    dict.addString(5, "Samantha");
                    PebbleKit.sendDataToPebble(context, appUUID, dict);

                }
            }

        };

        PebbleKit.registerReceivedDataHandler(this, mReceiver);
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(mReceiver);
    }


}
