package com.example.wearable1;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.wearable1.GlobalEnums.MusicCommand;
import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.data.Quaternion;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.SensorFusionBosch;
import com.mbientlab.metawear.module.Settings;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.VideoResult;
import com.otaliastudios.cameraview.controls.Mode;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;
import bolts.Continuation;
import bolts.Task;
public class MainActivity extends AppCompatActivity implements SensorEventListener, ServiceConnection{
    public static BluetoothSPP bt;
    public static boolean connected = false;
    private BtleService.LocalBinder serviceBinder;
    TextView textStatus, textRead, textGPS, textIMU;
    Menu menu;
    Iterator[] schedules;
    String device_name;
    String gpsData = "";
    Handler handler = new Handler();

    public double lat, lon, alt, bea, spe;

    private static final String TAG = "TAG";
    private SensorManager sensorManager;
    private LocationManager locationManager;
    Sensor accelerometer, gyroscope, magnetometer, temperature, humidity, rotation, orientation;
    public float accelX, accelY, accelZ;
    public float gyroX, gyroY, gyroZ;
    public float magneX, magneY, magneZ;
    public float rotX, rotY, rotZ, rotS, temp, humi;
    public float gravX, gravY, gravZ;
    public float mFieldX, mFieldY, mFieldZ;
    double azimuth = 0;
    double pitch = 0;
    double roll = 0;
    public String imuData, imuCSV, gpsCSV;

    public String CSV_HEADER = String.format("sensor,x_axis,y_axis,z_axis");
    private String filename = "Wearable1_IMU_Data_.csv";
    private File filePath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);

    private static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
    public GeomagneticField geoField;
    public float heading;

    private Runnable runGPS = new Runnable() {
        @Override
        public void run() {
            handler.postDelayed(this,1000);

            GPScomponent g = new GPScomponent(getApplicationContext());

            Location l = g.getLocation();
            if(l != null){
                lat = round(l.getLatitude(), 3);
                lon = round(l.getLongitude(), 3);
                alt = round(l.getAltitude(), 2);
                bea = round(l.getBearing(), 2);
                spe = round(l.getSpeed(), 2);

                geoField = new GeomagneticField((float)lat, (float)lon, (float)alt, System.currentTimeMillis());
                heading += geoField.getDeclination();
                gpsData = "Altitude: " + alt  +  "\n\nSpeed: " + spe;
                //textGPS.setText(gpsData);

                //Log.i("run", "\n LAT: "+lat+"\n LON:"+lon+"\n ALT: "+alt+"\n BEA: "+bea+"\n SPE: "+spe);
            }
        }
    };

    CameraView camera;
    private Led ledModule;
    private Accelerometer mw_accel;
    private SensorFusionBosch sensorFusion;
    private Settings boardSettings;
    public boolean record = false;
    public static float xData;
    public static float yData;
    public static float zData;
    public static TensorflowClassifier classifier;
    public ArrayList<Float> x = new ArrayList<Float>();
    public ArrayList<Float> y = new ArrayList<Float>();
    public ArrayList<Float> z = new ArrayList<Float>();
    public static float[] data_array;
    public static float[] results = new float[19];
    public int max;
    ImageView iv;
    TextView tv, activity, timer;
    float[] accel_read;
    float[] magnetic_read;
    private float current_degree=0f;
    public boolean workout = false;
    public long tStart, tEnd, tDelta, seconds, minutes;
    public double elapsedSeconds;
    public String timeString;
    public String qw, qx, qy, qz, wRoll, wPitch, wYaw;

    private Recetor recetor;

    TextView notificationView;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 123);
        handler.post(runGPS);
        camera = findViewById(R.id.camera);
        camera.setLifecycleOwner(this);

        final Button rec = (Button) findViewById(R.id.record_button);

        timer=(TextView)findViewById(R.id.timerView);
        activity=(TextView)findViewById(R.id.activityView);

        ///< Bind the service when the activity is created
        getApplicationContext().bindService(new Intent(this, BtleService.class),
                (ServiceConnection) this, Context.BIND_AUTO_CREATE);

        notificationView = findViewById(R.id.notificationView);
        //fillTextView();
        //String txt = notificationView.getText() + "\n" + "Teste:";
        //notificationView.append(txt);

        checkListenerIsListed();

        recetor = new Recetor();
        IntentFilter intentFilter = new IntentFilter("Register");
        registerReceiver(recetor, intentFilter);

        classifier = new TensorflowClassifier(this);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        sensorManager = (SensorManager) getSystemService((Context.SENSOR_SERVICE));

        // setup for accelerometer
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if(accelerometer != null)
            sensorManager.registerListener(MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        else
            Log.i(TAG, "Accelerometer not supported.");

        // setup for gyroscope
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if(gyroscope != null)
            sensorManager.registerListener(MainActivity.this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        else
            Log.i(TAG, "Gyroscope not supported.");

        // Setup for magnetic field sensor
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if(magnetometer != null)
            sensorManager.registerListener(MainActivity.this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        else
            Log.i(TAG, "Magnetometer not supported.");

        // Setup for temperature sensor
        temperature = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        if(temperature != null)
            sensorManager.registerListener(MainActivity.this, temperature, SensorManager.SENSOR_DELAY_NORMAL);
        else
            Log.i(TAG, "Temperature not supported.");

        // Setup humidity sensor
        humidity = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
        if(humidity != null)
            sensorManager.registerListener(MainActivity.this, humidity, SensorManager.SENSOR_DELAY_NORMAL);
        else
            Log.i(TAG, "Humidity not supported.");

        // Setup rotation vecotr sensor
        rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if(rotation != null)
            sensorManager.registerListener(MainActivity.this, rotation, SensorManager.SENSOR_DELAY_NORMAL);
        else
            Log.i(TAG, "Rotation not supported.");

        //final TextView myLabel = (TextView) findViewById(R.id.btResult);
        //final Button playPauseButton = (Button) findViewById(R.id.playPauseButton);
        //final Button nextButton = (Button) findViewById(R.id.nextButton);
        //final Button prevButton = (Button) findViewById(R.id.prevButton);


        final Button voiceAssistButton = (Button) findViewById(R.id.voiceAssistButton);

        //textRead = findViewById(R.id.textRead);
        textStatus = findViewById(R.id.textStatus);
        //textGPS = findViewById(R.id.GPSView);
        //textIMU = findViewById(R.id.IMUView);

        camera.addCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(PictureResult result) {
                // A Picture was taken!
            }
            @Override
            public void onVideoTaken(VideoResult result) {
                // A Video was taken!
            }
            @Override
            public void onVideoRecordingStart() {
                // Notifies that the actual video recording has started.
                // Can be used to show some UI indicator for video recording or counting time.
                rec.setText(R.string.stop_record);
                rec.setTextColor(Color.parseColor("#FF1100"));
                record = true;
            }
            @Override
            public void onVideoRecordingEnd() {
                // Notifies that the actual video recording has ended.
                // Can be used to remove UI indicators added in onVideoRecordingStart.
                rec.setText(R.string.record);
                rec.setTextColor(Color.parseColor("#000000"));
                record = false;
            }
        });

        bt = new BluetoothSPP(this);
        if (!bt.isBluetoothAvailable()) {
            Toast.makeText(getApplicationContext()
                    , "Bluetooth is not available"
                    , Toast.LENGTH_SHORT).show();
            finish();
        }

        bt.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() {
            public void onDataReceived(byte[] data, String message) {
                //textRead.append("Received: " + message + "\n");
                List<String> result = Arrays.asList(message.split("\\s*,\\s*"));
                //Log.i("MainActivity", "Received: " + result.get(0) + ", " + (result.get(0) == "E"));
                if(result.get(0).equals("C")){
                    Toast.makeText(MainActivity.this, result.get(1), Toast.LENGTH_SHORT).show();
                    if(result.get(1).equals("0"))
                        RunMusicCommand(MusicCommand.PLAY_PAUSE);
                    if(result.get(1).equals("1"))
                        RunMusicCommand(MusicCommand.NEXT);
                    if(result.get(1).equals("2"))
                        RunMusicCommand(MusicCommand.PREV);
                    if(result.get(1).equals("3"))
                        RunMusicCommand(MusicCommand.VUP);
                    if(result.get(1).equals("4"))
                        RunMusicCommand(MusicCommand.VDOWN);
                    if(result.get(1).equals("5"))
                        voiceAssistButton.performClick();
                    if(result.get(1).equals("6"));
                    //backButton.performClick();
                    if(result.get(1).equals("record")){
                        camera.setMode(Mode.VIDEO);
                        String filename = "chest_cam_" + System.currentTimeMillis() + ".mp4";
                        //camera.takeVideo(Objects.requireNonNull(getExternalFilesDir(Environment.DIRECTORY_MOVIES)));
                        camera.takeVideo(new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), filename));
                    /*
                    Toast toast = Toast.makeText(getApplicationContext(), "Recording Started", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.TOP, 0, 30);
                    toast.show();*/
                    }
                    if(result.get(1).equals("stop")){
                        camera.stopVideo();
                    /*
                    Toast toast = Toast.makeText(getApplicationContext(), "Recording Stopped", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.TOP, 0, 30);
                    toast.show();
                    */
                    }
                }
                if(result.get(0).equals("I")){
                    qw = result.get(1);
                    qx = result.get(2);
                    qy = result.get(3);
                    qz = result.get(4);
                    wRoll = result.get(5);
                    wPitch = result.get(6);
                    wYaw = result.get(7);
                    Log.i("MainActivity", "Wrist Euler: " + qw + ", " + qx + ", " + qy + ", " + qz + ", " + wRoll + ", " + wPitch + ", " + wYaw);
                }
            }
        });

        bt.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {
            public void onDeviceDisconnected() {
                textStatus.setTextColor(Color.RED);
                textStatus.setText("Status : Not connect");
                menu.clear();
                getMenuInflater().inflate(R.menu.menu_main, menu);
            }

            public void onDeviceConnectionFailed() {
                textStatus.setText("Status : Connection failed");
            }

            public void onDeviceConnected(String name, String address) {
                connected = true;
                textStatus.setTextColor(Color.GREEN);
                device_name = name;
                textStatus.setText("Status : Connected to " + name);
                handler.post(runSendData);
                menu.clear();
                getMenuInflater().inflate(R.menu.menu_main, menu);
            }
        });

        rec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!record){
                    camera.setMode(Mode.VIDEO);
                    String filename = "chest_cam_" + System.currentTimeMillis() + ".mp4";
                    //camera.takeVideo(Objects.requireNonNull(getExternalFilesDir(Environment.DIRECTORY_MOVIES)));
                    camera.takeVideo(new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), filename));
                    Toast toast = Toast.makeText(getApplicationContext(), "Recording Started", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.TOP, 0, 30);
                    toast.show();
                    //rec.setText(R.string.stop_record);
                    //record = true;
                }
                else{
                    camera.stopVideo();
                    //rec.setText(R.string.record);
                    Toast toast = Toast.makeText(getApplicationContext(), "Recording Stopped", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.TOP, 0, 30);
                    toast.show();
                    //record = false;
                }
            }
        });

        //start light off button handler
        /*voiceAssistButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                promptSpeechInput();

                Toast toast = Toast.makeText(getApplicationContext(), "Try Saying: ContactName, AppName, Torch, Wifi, Bluetooth, Calculator...",
                        Toast.LENGTH_LONG);
                toast.setGravity(Gravity.TOP, 0, 30);
                toast.show();
            }});*/


        if (DeviceSetupActivityFragment.mwConnected)
            startActivity();
    }

    @Override
    public void onResume(){
        super.onResume();
        camera.open();
        Log.i("run", "dfdf");
        //bt.send("I'm Back!", false);
        //mGLSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        camera.close();
        //mGLSurfaceView.onPause();
    }

    public void onDestroy() {
        super.onDestroy();
        camera.destroy();
        getApplicationContext().unbindService((ServiceConnection) this);
        bt.stopService();
        ///< Unbind the service when the activity is destroyed
        getApplicationContext().unbindService(this);
        unregisterReceiver(recetor);
    }

    public void onStart() {
        super.onStart();
        if (!bt.isBluetoothEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, BluetoothState.REQUEST_ENABLE_BT);
        } else {
            if (!bt.isServiceAvailable()) {
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_ANDROID);
                setup();
            }
        }
    }

    private Runnable runSendData = new Runnable() {
        @Override
        public void run() {
            handler.postDelayed(this,1000);
            //bt.send(lat+","+lon+","+alt+","+bea+","+spe+",\n", false);
            //Log.i("run", "\n LAT: "+lat+"\n LON:"+lon+"\n ALT: "+alt+"\n BEA: "+bea+"\n SPE: "+spe);
            //Toast.makeText(getApplicationContext(), "LAT: "+lat+" \n LON:"+lon, Toast.LENGTH_LONG).show();
        }
    };

    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_device_connect) {
            bt.setDeviceTarget(BluetoothState.DEVICE_OTHER);
            Intent intent = new Intent(getApplicationContext(), DeviceList.class);
            startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
        } else if (id == R.id.menu_disconnect) {
            if (bt.getServiceState() == BluetoothState.STATE_CONNECTED)
                bt.disconnect();
        } else if (id == R.id.menu_metawear_connect) {
            Intent mw_intent = new Intent(getApplicationContext(), MWConnectActivity.class);
            startActivity(mw_intent);
        } else if (id == R.id.workout) {
            if(!workout){
                tStart = SystemClock.elapsedRealtime();
                workout = true;
            } else {
                workout = false;
            }

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop(){
        super.onStop();
    }

    public void setup() {
        /*Button btnSend = findViewById(R.id.btnSend);
        btnSend.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (bt.getServiceState() == BluetoothState.STATE_CONNECTED)
                    bt.send("shoot", false);
                else
                    Toast.makeText(MainActivity.this, "Sorry, you need to connect first", Toast.LENGTH_SHORT).show();
            }
        });*/
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
            if (resultCode == Activity.RESULT_OK)
                bt.connect(data);
        } else if (requestCode == BluetoothState.REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_ANDROID);
                setup();
            } else {
                Toast.makeText(getApplicationContext()
                        , "Bluetooth was not enabled."
                        , Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void RunMusicCommand(MusicCommand mode) {
        //Core.Services.Vibration.Vibrate();

        //AudioManager mAudioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        String keyCommand = "input keyevent ";

        switch (mode)
        {
            case PLAY_PAUSE:
                //KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                //mAudioManager.dispatchMediaKeyEvent(event);
                keyCommand += KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
                break;

            case NEXT:
                //KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT);
                //mAudioManager.dispatchMediaKeyEvent(event);
                keyCommand += KeyEvent.KEYCODE_MEDIA_NEXT;
                break;

            case PREV:
                //KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                //mAudioManager.dispatchMediaKeyEvent(event);
                keyCommand += KeyEvent.KEYCODE_MEDIA_PREVIOUS;
                break;

            case VUP:
                //KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                //mAudioManager.dispatchMediaKeyEvent(event);
                keyCommand += KeyEvent.KEYCODE_VOLUME_UP;
                break;

            case VDOWN:
                //KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                //mAudioManager.dispatchMediaKeyEvent(event);
                keyCommand += KeyEvent.KEYCODE_VOLUME_DOWN;
                break;

            case VBACK:
                //KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                //mAudioManager.dispatchMediaKeyEvent(event);
                keyCommand += KeyEvent.KEYCODE_VOICE_ASSIST;
                break;

        }

        try
        {
            Runtime.getRuntime().exec(keyCommand);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void promptSpeechInput() {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        i.putExtra(RecognizerIntent.EXTRA_PROMPT,"Say Something !");

        try {
            startActivityForResult(i, 100);
        }catch(ActivityNotFoundException a)
        {
            Toast.makeText(MainActivity.this, "Sorry Your Device Doesn't Support This Feature !", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        tv=(TextView)findViewById(R.id.text2);
        iv=(ImageView)findViewById(R.id.img);

        if(sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            //Log.i(TAG, "Accel-X: " + event.values[0] + ", Accel-Y: " + event.values[1] + ", Accel-Z: " + event.values[2]);
            accel_read=event.values;
            accelX = event.values[0];
            accelY = event.values[1];
            accelZ = event.values[2];
        } else if(sensor.getType() == Sensor.TYPE_GYROSCOPE){
            //Log.i(TAG, "Gyro-X: " + event.values[0] + ", Gyro-Y: " + event.values[1] + ", Gyro-Z: " + event.values[2]);
            gyroX = event.values[0];
            gyroY = event.values[1];
            gyroZ = event.values[2];
        } else if(sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
            //Log.i(TAG, "Magne-X: " + event.values[0] + ", Magne-Y: " + event.values[1] + ", Magne-Z: " + event.values[2]);
            magnetic_read=event.values;
            magneX = event.values[0];
            magneY = event.values[1];
            magneZ = event.values[2];
        }  else if(sensor.getType() == Sensor.TYPE_ROTATION_VECTOR){
            //Log.i(TAG, "Rot-X: " + event.values[0] + ", Rot-Y: " + event.values[1] + ", Rot-Z: " + event.values[2] + ", Rot-Scalar: " + event.values[3]);
            rotX = event.values[0];
            rotY = event.values[1];
            rotZ = event.values[2];
            rotS = event.values[3];
            azimuth = Math.toDegrees(rotX);
            pitch = Math.toDegrees(rotY);
            roll = Math.toDegrees(rotZ);
            //Log.i("wearable1", "Gravity: " + azimuth + ", " + pitch + ", " + roll);

        } else if(sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE){
            //Log.i(TAG, "Temp: " + event.values[0]);
            temp = event.values[0];
        } else if(sensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY){
            //Log.i(TAG, "Humidity: " + event.values[0]);
            humi = event.values[0];
        }

        if(accel_read!=null && magnetic_read!=null){
            float [] R=new float[9];
            float [] I=new float[9];
            boolean successful_read=SensorManager.getRotationMatrix(R,I,accel_read,magnetic_read);
            if(successful_read){
                float[] orientation=new float[3];
                SensorManager.getOrientation(R,orientation);
                double azimuth_angle = Math.toDegrees(orientation[0]);
                double pitch_angle = Math.toDegrees(orientation[1]);
                double roll_angle = Math.toDegrees(orientation[2]);
                //float degrees=((azimuth_angle *180f)/3.14f);
                Log.i(TAG, "Orientation: " + azimuth_angle + ", " + pitch_angle + ", " + roll_angle);
                //bt.send("Orientation," + azimuth_angle + ", " + pitch_angle + ", " + roll_angle + ",\n", false);
            }
        }

        imuData = "AccelX: " + accelX + "\nAccelY: " + accelY + "\nAccelZ: " + accelZ +
                "\n\nGyroX: " + gyroX + "\nGyroY: " + gyroY + "\nGyroZ: " + gyroZ +
                "\n\nMagneX: " + magneX + "\nMagneY: " + magneY + "\nMagneZ: " + magneZ +
                "\n\nRotX: " + rotX + "\nRotY: " + rotY + "\nRotZ: " + rotZ + "\nRotS: " + rotS;

        // CSV writing
        bt.send("GPS," + lat + "," + lon + "," + alt + "," + bea + "," + spe + ",\n", false);
        //Log.i(TAG, "GPS," + lat + "," + lon + "," + alt + "," + temp);
        imuCSV = accelX + "," + accelY + "," + accelZ +
                "," + gyroX + "," + gyroY + "," + gyroZ +
                "," + magneX + "," + magneY + "," + magneZ +
                "," + rotX + "," + rotY + "," + rotZ + "," + rotS;
        OutputStream out;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        ///< Typecast the binder to the service's LocalBinder class
        serviceBinder = (BtleService.LocalBinder) service;
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) { }

    public static Task<Void> reconnect(final MetaWearBoard board) {
        return board.connectAsync().continueWithTask(task -> task.isFaulted() ? reconnect(board) : task);
    }

    public  void startActivity(){
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ///< Bind the service when the activity is created
        getApplicationContext().bindService(new Intent(this, BtleService.class), this, BIND_AUTO_CREATE);

        ledModule = DeviceSetupActivityFragment.mwBoard.getModule(Led.class);
        mw_accel = DeviceSetupActivityFragment.mwBoard.getModule(Accelerometer.class);
        sensorFusion = DeviceSetupActivityFragment.mwBoard.getModule(SensorFusionBosch.class);
        boardSettings = DeviceSetupActivityFragment.mwBoard.getModule(Settings.class);
        mw_accel.configure()
                .odr(200f)       // Set sampling frequency to 25Hz, or closest valid ODR
                .range(16f)      // Set data range to +/-4g, or closet valid range
                .commit();

        sensorFusion.configure()
                .mode(SensorFusionBosch.Mode.NDOF)
                .accRange(SensorFusionBosch.AccRange.AR_16G)
                .gyroRange(SensorFusionBosch.GyroRange.GR_2000DPS)
                .commit();

        configureChannel(ledModule.editPattern(Led.Color.BLUE));
        ledModule.play();

        sensorFusion.quaternion().addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.stream(new Subscriber() {
                    @Override
                    public void apply(Data data, Object... env) {
                        Log.i("MainActivity", "Euler Angles: " + data.value(Quaternion.class));
                    }
                });
            }
        }).continueWith(new Continuation<Route, Void>() {
            @Override
            public Void then(Task<Route> task) throws Exception {
                sensorFusion.quaternion().start();
                sensorFusion.start();
                return null;
            }
        });

        mw_accel.acceleration().addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.stream(new Subscriber() {
                    @Override
                    public void apply(Data data, Object... env) {
                        //Log.i("MainActivity", data.value(Acceleration.class).toString());
                        xData = data.value(Acceleration.class).x();
                        yData = data.value(Acceleration.class).y();
                        zData = data.value(Acceleration.class).z();
                        x.add(xData);
                        y.add(yData);
                        z.add(zData);
                        //Log.i("MainActivity", String.valueOf(x.size()));
                        if(x.size() == 120){
                            List<Float> accel = new ArrayList<>();
                            accel.addAll(x);
                            accel.addAll(y);
                            accel.addAll(z);
                            data_array = toFloatArray(accel);
                            float[] data_array2 = toFloatArray(accel);
                            x.clear();
                            y.clear();
                            z.clear();
                            results = classifier.predictProbabilities(data_array);
                            max = indexOfLargest(results);

                            if(max == 0)
                                Log.i("serious", "fire");
                            else if(max == 1)
                                Log.i("serious", "other");
                            else if(max == 2)
                                Log.i("serious", "rack");
                        }
                    }
                });
            }
        }).continueWith(new Continuation<Route, Void>() {
            @Override
            public Void then(Task<Route> task) throws Exception {
                mw_accel.acceleration().start();
                mw_accel.start();
                return null;
            }
        });
    }

    private void configureChannel(Led.PatternEditor editor) {
        final short PULSE_WIDTH= 1000;
        editor.highIntensity((byte) 31).lowIntensity((byte) 31)
                .highTime((short) (PULSE_WIDTH >> 1)).pulseDuration(PULSE_WIDTH)
                .repeatCount((byte) -1).commit();
    }

    private float[] toFloatArray(@NonNull List<Float> list) {
        int i = 0;
        float[] array = new float[list.size()];

        for (Float f : list) {
            array[i++] = (f != null ? f : Float.NaN);
        }
        return array;
    }

    public int indexOfLargest(float[] results){
        int maxAt = 0;
        for (int i = 0; i < results.length; i++) {
            maxAt = results[i] > results[maxAt] ? i : maxAt;
        }
        return maxAt;
    }

    public class Recetor extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            //String not_received = notificationView.getText() + "\n" + "hgnhgnghnhgnhghgjhg" + intent.getStringExtra("Register");
            String noti = intent.getStringExtra("Register");

            //notificationView.setText(not);
            if(noti != null){
                Log.i(TAG, noti);
                //bt.send("noti," + noti + ",\n", false);
            }

            SharedPreferences sharedPreferences = getSharedPreferences("Register", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            String mensagens = sharedPreferences.getString("Register", "");
            mensagens = mensagens + "\n" + intent.getStringExtra("Register");
            editor.putString("Register", mensagens);
            editor.apply();
        }
    }

    private void checkListenerIsListed() {
        String notificationListenerString = android.provider.Settings.Secure.getString(this.getContentResolver(),"enabled_notification_listeners");
        //Check notifications access permission
        if (notificationListenerString == null || !notificationListenerString.contains(getPackageName()))
        {
            //The notification access has not acquired yet!
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getResources().getString(R.string.AVISO_NOT_LIST))
                    .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                        }
                    })
                    .setNegativeButton("Não", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    }).create().show();
        }
    }

    private void fillTextView() {
        SharedPreferences sharedPreferences = getSharedPreferences("Register", MODE_PRIVATE);
        notificationView.setText(sharedPreferences.getString("Register", ""));
        //Log.i(TAG, sharedPreferences.getString("Register", ""));
    }
}