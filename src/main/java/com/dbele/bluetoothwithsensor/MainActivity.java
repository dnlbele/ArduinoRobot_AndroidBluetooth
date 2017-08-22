package com.dbele.bluetoothwithsensor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SensorEventListener {


    private static final int ALLOWED_DISTANCE = 30;

    private enum State {
        STALE,
        FORWARD,
        BACKWARD,
        LEFT,
        RIGHT,
        FORWARD_LEFT,
        BACKWARD_LEFT,
        FORWARD_RIGHT,
        BACKWARD_RIGHT
    }


    private static final int SENSITIVITY = 2;

    private State state = State.STALE;

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;

    private long lastReading = 0;
    private boolean bluetoothDevicepaired = false;

    private static final int DELAY = 600;

    private ImageView ivAccelerometer;


    @Override
    protected void onResume() {
        super.onResume();
        setupSensor();
        setupBluetooth();
    }

    private void setupBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter.getBondedDevices().size()==0) {
            Toast.makeText(this, R.string.no_bluetooth, Toast.LENGTH_LONG).show();
            return;
        }
        bluetoothDevicepaired = true;
        BluetoothDevice device = mBluetoothAdapter.getBondedDevices().iterator().next();
        UUID uuid = device.getUuids()[0].getUuid();
        try {
            btSocket = device.createRfcommSocketToServiceRecord(uuid);
            btSocket.connect();
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
            }
        }
        beginListenForData();

    }

    private void setupSensor() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(this, R.string.no_accelerometer, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this, accelerometer);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ivAccelerometer = (ImageView) findViewById(R.id.ivAccelerometer);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (!bluetoothDevicepaired) {
            return;
        }
        long now = System.currentTimeMillis();
        long timePassed = now - lastReading;
        //acer
        //float newX = event.values[0];
        float newX = -event.values[0];
        float newY = -event.values[1];


        StringBuffer report = new StringBuffer();
        report.append(newX+"");
        report.append(":");
        report.append(newY+"");
        report.append("\n\r");

        if (timePassed < DELAY) {
            return;
        }
        lastReading = now;

        writeData(report.toString());
        Log.e(getClass().getName(), report.toString());

        //acer
        //handlePicture(newY, newX);
        handlePicture(newX, newY);
    }

    private void handlePicture(float newX, float newY) {

        state = State.STALE;


        if (newY > SENSITIVITY) {
            setImage(R.drawable.arrow);
            Log.e(getClass().getName(), "ULAZIM U GORE");
            state = State.FORWARD;
            if (newX > SENSITIVITY) {
                Log.e(getClass().getName(), "ULAZIM U GOREDESNO");
                state = State.FORWARD_RIGHT;
            } else if (newX < -SENSITIVITY) {
                Log.e(getClass().getName(), "ULAZIM U GORELIJEVO");
                state = State.FORWARD_LEFT;
            }
            handlePictureOrientation();
            return;
        }

        if (newY < -SENSITIVITY) {
            Log.e(getClass().getName(), "ULAZIM U DOLJE");
            state = State.BACKWARD;
            setImage(R.drawable.arrow);
            if (newX > SENSITIVITY) {
                Log.e(getClass().getName(), "ULAZIM U DOLJEDESNO");

                state = State.BACKWARD_RIGHT;
            } else if (newX < -SENSITIVITY) {
                Log.e(getClass().getName(), "ULAZIM U DOLJELIJEVO");

                state = State.BACKWARD_LEFT;
            }
            handlePictureOrientation();
            return;
        }

        if (newX > SENSITIVITY) {
            Log.e(getClass().getName(), "ULAZIM U DESNO");

            state = State.RIGHT;
            setImage(R.drawable.arrow);
            handlePictureOrientation();
            return;
        }
        if (newX < -SENSITIVITY) {
            Log.e(getClass().getName(), "ULAZIM U LIJEVO");
            state = State.LEFT;
            setImage(R.drawable.arrow);
            handlePictureOrientation();
            return;
        }

        //setImage(R.drawable.android_aruino);
        //ivAccelerometer.setRotation(0);

    }

    int currentRotation = 0;
    private void handlePictureOrientation() {
        switch (state) {
            case FORWARD:
                currentRotation = 0;
                break;
            case BACKWARD:
                currentRotation = 180;
                break;
            /*
            case FORWARD_LEFT:
                currentRotation = -45;
                break;
            case FORWARD_RIGHT:
                currentRotation = 45;
                break;
            case BACKWARD_LEFT:
                currentRotation = 215;
                break;
            case BACKWARD_RIGHT:
                currentRotation = 135;
                break;
            */
            case LEFT:
                currentRotation = -90;
                break;
            case RIGHT:
                currentRotation = 90;
                break;
        }
        //Log.e(getClass().getName(), "currentRotationcurrent:" + currentRotation);
        ivAccelerometer.setRotation(currentRotation);
        performAnimation();
    }

    private void performAnimation() {
        Animation animationPopUp = AnimationUtils.loadAnimation(this, R.anim.pop_out);
        Animation animationPopIn = AnimationUtils.loadAnimation(this, R.anim.pop_in);

        AnimationSet growShrink = new AnimationSet(true);
        growShrink.addAnimation(animationPopUp);
        growShrink.addAnimation(animationPopIn);
        ivAccelerometer.startAnimation(growShrink);

    }

    private int previousImageId;
    private void setImage(int imageId) {
        if (imageId != previousImageId) {
            ivAccelerometer.setImageResource(imageId);
            previousImageId = imageId;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void writeData(String data) {
        try {
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
        }

        String message = data;
        byte[] msgBuffer = message.getBytes();

        try {
            outStream.write(msgBuffer);
        } catch (IOException e) {
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopWorker = true;
        try {
            if (bluetoothDevicepaired) {
                mmInputStream.close();
                outStream.close();
                btSocket.close();
            }
        } catch (IOException e) {
        }
    }



    InputStream mmInputStream;
    private Thread workerThread;
    private byte[] readBuffer;
    private int readBufferPosition;
    private int counter;
    private volatile boolean stopWorker;
    private TextView tvReport;


    void beginListenForData()
    {
        try {
            mmInputStream = btSocket.getInputStream();
        } catch (IOException e) {
            Log.e(getClass().getName(), "INPUTSTREAM broken");
        }
        tvReport = (TextView) findViewById(R.id.tvReport);
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            int distance = 0;
                                            try {
                                                distance = Integer.parseInt(data);
                                            } catch (NumberFormatException e) {
                                                Log.e(getClass().getName(), e.getMessage());
                                            }

                                            if (distance > 0) {
                                                tvReport.setText(data + "cm");
                                                if (distance < ALLOWED_DISTANCE) {
                                                    tvReport.setTextColor(Color.RED);
                                                    ivAccelerometer.setRotation(0);
                                                    setImage(R.drawable.stop);
                                                    performAnimation();
                                                } else {
                                                    tvReport.setTextColor(Color.GREEN);
                                                }
                                            }
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }




}
