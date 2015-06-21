package com.angelhack.nellie;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;

import android.content.Context;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.kairos.Kairos;
import com.kairos.KairosListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class MainActivity extends Activity implements SurfaceHolder.Callback {

    /** New **/
    Camera camera;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;

    Camera.PictureCallback jpegCallback;
    File mCurrentPhotoPath;
    Kairos myKairos;
    KairosListener listener;

    /** End New **/

    private static final String GALLERY_ID = "NELLIE";

    private PebbleKit.PebbleDataReceiver mReceiver;
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

        /** New **/
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        surfaceHolder.addCallback(this);

        // deprecated setting, but required on Android versions prior to 3.0
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        jpegCallback = new Camera.PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera) {
                File pictureFile = getOutputMediaFile();
                if (pictureFile == null){
                    Log.d("CAMERA", "Error creating media file, check storage permissions.");
                    return;
                }

                try {
                    FileOutputStream fos = new FileOutputStream(pictureFile);
                    fos.write(data);
                    fos.close();
                    mCurrentPhotoPath = pictureFile;
                } catch (FileNotFoundException e) {
                    Log.d("CAMERA", "File not found: " + e.getMessage());
                } catch (IOException e) {
                    Log.d("CAMERA", "Error accessing file: " + e.getMessage());
                }

                refreshCamera();
            }
        };
        /** End New **/

        // listener
        if (listener == null) {
            listener = new KairosListener() {

                @Override
                public void onSuccess(String response) {
                    Log.d("KAIROS DEMO SUCCESS", response);
                }

                @Override
                public void onFail(String response) {
                    Log.d("KAIROS DEMO FAIL", response);
                }
            };
        }


        /* * * instantiate a new kairos instance * * */
        if (myKairos == null) {

            myKairos = new Kairos();

            /* * * set authentication * * */
            String app_id = "de2652f9";
            String api_key = "c4754c1ec92893a25918db365bb82f1e";
            myKairos.setAuthentication(this, app_id, api_key);

            try {
                myKairos.listGalleries(listener);
            } catch (Exception e) {
                // Handle Exception
            }
        }
    }

    /** New **/
    public void captureImage() {
        try {
            Bitmap image = takePicture();
            String subjectId = "Acquaintance Name";
            myKairos.enroll(image, subjectId, GALLERY_ID, null, null, null, listener);
            Toast.makeText(getApplicationContext(), mCurrentPhotoPath.getAbsolutePath() + " enrolled successfully", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            // Handle Exceptions
        }
    }

    public Bitmap takePicture() {
        camera.takePicture(null, null, jpegCallback);
        try {
            Bitmap image = BitmapFactory.decodeFile(mCurrentPhotoPath.getAbsolutePath());
            if (getResources().getConfiguration().orientation == getResources().getConfiguration().ORIENTATION_PORTRAIT) {
                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                Bitmap rotatedBitmap = Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);
                return rotatedBitmap;
            }
            else {
                return image;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public void recallImage() {
        try {
            Bitmap image = takePicture();
            myKairos.recognize(image, GALLERY_ID, null, null, null, null, listener);
        } catch (Exception e) {
            // Handle Exception
        }
    }

    public void refreshCamera() {
        if (surfaceHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            camera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here
        // start preview with new settings
        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (Exception e) {

        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        refreshCamera();
    }

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            // open the camera

            camera = Camera.open();

        } catch (Exception e) {
            // check for exceptions
            System.err.println(e);
            return;
        }
        /*Camera.Parameters param;
        param = camera.getParameters();
        camera.setParameters(param);*/
        try {
            // The Surface has been created, now tell the camera where to draw
            // the preview.
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (Exception e) {
            // check for exceptions
            System.err.println(e);
            return;
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // stop preview and release camera
        camera.stopPreview();
        camera.release();
        camera = null;
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                "IMG_"+ timeStamp + ".jpg");

        return mediaFile;
    }
    /** End New **/

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
                            //startActivity(new Intent(getApplicationContext(), PictureAction.class));
                            captureImage();
                            break;
                        case BUTTON_EVENT_DOWN:
                            recallImage();
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
