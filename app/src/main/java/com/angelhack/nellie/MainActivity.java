package com.angelhack.nellie;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
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
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.kairos.Kairos;
import com.kairos.KairosListener;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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
    HashMap<String, File> savedPeople;
    boolean searchInProgress = false;
    boolean recordingInProgress = false;

    /** End New **/

    private static final String GALLERY_ID = "NELLIEA";
    private static int clickCount = 0;
    private String currentName = "";

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
                        if (response.contains("5002")) {
                            // No face detected or first pebble click.
                            PebbleDictionary dict = new PebbleDictionary();
                            dict.addString(5, "No Face Seen");
                            PebbleKit.sendDataToPebble(getApplicationContext(), appUUID, dict);
                        }
                        else if (response.contains("candidates")) {
                            // Person is recognised!
                            String name = response.substring(response.indexOf("subject") + 10, response.indexOf("\",\"width"));
                            Log.d("NAME FOUND", name);
                            PebbleDictionary dict = new PebbleDictionary();
                            dict.addString(5, name);
                            PebbleKit.sendDataToPebble(getApplicationContext(), appUUID, dict);
                        }
                        else if (response.contains("No match found")) {
                            // found face but did not recognise
                            PebbleDictionary dict = new PebbleDictionary();
                            dict.addString(5, "Face Not Known");
                            PebbleKit.sendDataToPebble(getApplicationContext(), appUUID, dict);

                        }
                        else {
                            // new person recognised
                            savedPeople.put(currentName, mCurrentPhotoPath);
                            PebbleDictionary dict = new PebbleDictionary();
                            dict.addString(5, currentName + "Added");
                            PebbleKit.sendDataToPebble(getApplicationContext(), appUUID, dict);
                        }
                    searchInProgress = false;
                }

                @Override
                public void onFail(String response) {
                    Log.d("KAIROS DEMO FAIL", response);
                }
            };

            outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording.3gp";

            myAudioRecorder = new MediaRecorder();
            myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            myAudioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
            myAudioRecorder.setOutputFile(outputFile);

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

        if (savedPeople == null) {
            savedPeople = new HashMap<String, File>();
        }
    }

    /** New **/
    public void captureImage() {
        searchInProgress = true;
        try {
            Bitmap image = takePicture();
            currentName = "Acquaintance Name";
            myKairos.enroll(image, currentName, GALLERY_ID, null, null, null, listener);
        } catch (Exception e) {
            // Handle Exceptions
        }
    }

    public Bitmap takePicture() {
        camera.takePicture(null, null, jpegCallback);
        try {
            Bitmap image = BitmapFactory.decodeFile(mCurrentPhotoPath.getAbsolutePath());
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(image,image.getWidth(),image.getHeight(),true);
                Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, image.getWidth(), image.getHeight(), matrix, true);
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
        searchInProgress = true;
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
            startFaceDetection();
        } catch (Exception e) {

        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        refreshCamera();
    }

    public void startFaceDetection(){
        // Try starting Face Detection
        Camera.Parameters params = camera.getParameters();

        // start face detection only *after* preview has started
        if (params.getMaxNumDetectedFaces() > 0){
            // camera supports face detection, so can start it:
            camera.startFaceDetection();
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            // open the camera

            camera = Camera.open();
            camera.setFaceDetectionListener(new MyFaceDetectionListener());

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
                            ++clickCount;
                            captureImage();
                            break;
                        case BUTTON_EVENT_DOWN:
                            ++clickCount;
                            recallImage();
                            break;
                        case BUTTON_EVENT_SELECT:
                            if(!recordingInProgress) {
                                recordingInProgress = true;
                                try {
                                    myAudioRecorder.prepare();
                                    myAudioRecorder.start();
                                } catch (IllegalStateException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }
                            else {
                                recordingInProgress = false;
                                myAudioRecorder.stop();
                                myAudioRecorder.release();
                                myAudioRecorder = null;

                                if(outputFile != null) {
                                    new UploadToApiTask().execute(speechUrl);
                                }
                            }
                            break;
                    }

                    PebbleDictionary dict = new PebbleDictionary();
                    if (!searchInProgress) {
                        dict.addString(2, "Press To Scan");
                    }
                    else {
                        dict.addString(2, "Please wait");
                    }
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


    private MediaRecorder myAudioRecorder;
    private String outputFile = null;
    private static String apikey = "d9093dd5-c73b-4494-927b-10b0f7fe36f0";
    private static String speechUrl = "https://api.idolondemand.com/1/api/async/recognizespeech/v1";
    private static String jobUrl = "https://api.idolondemand.com/1/job/result/";
    private static String nameUrl = "https://api.idolondemand.com/1/api/sync/extractentities/v1";


    private class UploadToApiTask extends AsyncTask<String, Integer, String> {

        protected String doInBackground(String... urls) {
            MultipartEntityBuilder speechEntity = MultipartEntityBuilder.create();
            speechEntity.addPart("file", new FileBody(new File(outputFile)));
            speechEntity.addPart("apikey", new StringBody(apikey, ContentType.TEXT_PLAIN));
            speechEntity.addPart("language", new StringBody("en-GB", ContentType.TEXT_PLAIN));
            HttpEntity speechEntities = speechEntity.build();

            MultipartEntityBuilder jobEntity = MultipartEntityBuilder.create();
            jobEntity.addPart("apikey", new StringBody(apikey, ContentType.TEXT_PLAIN));
            HttpEntity jobEntities = jobEntity.build();

            DefaultHttpClient httpClient = new DefaultHttpClient();

            HttpPost httpPostApi = new HttpPost(urls[0]);
            httpPostApi.setEntity(speechEntities);
            JSONObject apiResponse = exectutePost(httpClient, httpPostApi);

            String jobId = "";
            try{jobId = apiResponse.getString("jobID");} catch(JSONException e){e.printStackTrace();};
            Log.i("far", "Job ID: " + jobId);

            HttpPost httpPostJob = new HttpPost(jobUrl+jobId);
            httpPostJob.setEntity(jobEntities);
            JSONObject response = exectutePost(httpClient, httpPostJob);

            String speech = "";

            try {
                String actionsStr = (new JSONArray(response.getString("actions"))).getString(0);
                JSONObject actionsArr = new JSONObject(actionsStr);
                String resultStr = actionsArr.getString("result");
                JSONObject resultObj = new JSONObject(resultStr);
                JSONObject documentObj = resultObj.getJSONArray("document").getJSONObject(0);
                speech = documentObj.getString("content");

                Log.i("far", response.getString("actions"));
                Log.i("far", "Speech: " + speech);
            } catch (JSONException e){
                e.printStackTrace();
            }

            MultipartEntityBuilder nameEntity = MultipartEntityBuilder.create();
            nameEntity.addPart("text", new StringBody(speech, ContentType.TEXT_PLAIN));
            nameEntity.addPart("entity_type", new StringBody("person_name_component_eng", ContentType.TEXT_PLAIN));
            nameEntity.addPart("unique_entities", new StringBody("true", ContentType.TEXT_PLAIN));
            nameEntity.addPart("apikey", new StringBody(apikey, ContentType.TEXT_PLAIN));
            HttpEntity nameEntities = nameEntity.build();
            HttpPost httpPostName = new HttpPost(nameUrl);
            httpPostName.setEntity(nameEntities);

            JSONObject nameResponse = exectutePost(httpClient, httpPostName);

            Log.i("far", nameResponse.toString());
            String name = "";
            try{name = nameResponse.getJSONArray("entities").getJSONObject(0).getString("normalized_text");} catch(JSONException e){e.printStackTrace();}

            Log.i("far", "Name: " + name);
            return name;
        }



        protected void onPostExecute(String result) {
            Log.i("far", "Downloaded " + result + " bytes");
            currentName = result;
            PebbleDictionary dict = new PebbleDictionary();
            dict.addString(5, currentName);
            PebbleKit.sendDataToPebble(getApplicationContext(), appUUID, dict);
        }
    }

    private JSONObject exectutePost(DefaultHttpClient httpClient, HttpPost httpPost){
        JSONObject json = null;
        try {
            Log.i("far", "try0");
            HttpResponse httpResponse = httpClient.execute(httpPost);
            Log.i("far", "try1");
            HttpEntity httpEntity = httpResponse.getEntity();
            Log.i("far", "try2");
            String response = EntityUtils.toString(httpEntity);
            Log.i("far", "try3 " + response);
            json = new JSONObject(response);
            Log.i("far", "try4");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }
}
