package com.nordman.big.simplestcompass;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity implements SensorEventListener {
    public static final long TICK_INTERVAL = 2000;

    // define the display assembly compass picture
    private ImageView image;
    TextView tvHeading;

    // device sensor manager
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;

    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];
    private float mCurrentDegree = 0f;
    private float mAzimuthDegree = 0f;

    Timer tick = null; // Таймер, использующийся в MainActivity для плавной анимации компаса

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        image = (ImageView) findViewById(R.id.imageViewArrow);
        // TextView that will tell the user what degree is he heading
        tvHeading = (TextView) findViewById(R.id.tvHeading);

        // initialize your android device sensor capabilities
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // создаем таймер если еще не создан
        if (tick==null){
            tick = new Timer();
            tick.schedule(new UpdateTickTask(), 0, TICK_INTERVAL); //тикаем каждую секунду
        }

        // for the system's orientation sensor registered listeners
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (tick!=null) {
            tick.cancel();
            tick = null;
        }

        // to stop the listener and save battery
        mSensorManager.unregisterListener(this, mAccelerometer);
        mSensorManager.unregisterListener(this, mMagnetometer);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // при изменении сенсора сохранить в глобальной переменной mAzimuthDegree значение азимута в градусах
        if (event.sensor == mAccelerometer) {
            System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
            mLastAccelerometerSet = true;
        } else if (event.sensor == mMagnetometer) {
            System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
        }
        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(mR, mOrientation);
            float azimuthInRadians = mOrientation[0];
            mAzimuthDegree = - round((float) (Math.toDegrees(azimuthInRadians) + 360) % 360, 1);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d("LOG", "...Accuracy=" + accuracy + "...");
        // not in use
    }

    public static float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

    private class UpdateTickTask extends TimerTask {
        public void run() {
            tickHandler.sendEmptyMessage(0);
        }
    }

    final Handler tickHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            RotateAnimation ra;

            if (Math.abs(mCurrentDegree - mAzimuthDegree)>320) {
                if (Math.abs(mCurrentDegree)>Math.abs(mAzimuthDegree)) {
                    // c 360 до 0
                    ra = new RotateAnimation(
                            mCurrentDegree,
                            -360,
                            Animation.RELATIVE_TO_SELF, 0.5f,
                            Animation.RELATIVE_TO_SELF,
                            0.5f);

                    ra.setDuration(TICK_INTERVAL);
                    ra.setFillAfter(true);
                    image.startAnimation(ra);

                    mCurrentDegree = 0;
                } else {
                    // c 0 на 360
                    ra = new RotateAnimation(
                            mCurrentDegree,
                            0,
                            Animation.RELATIVE_TO_SELF, 0.5f,
                            Animation.RELATIVE_TO_SELF,
                            0.5f);

                    ra.setDuration(TICK_INTERVAL);
                    ra.setFillAfter(true);
                    image.startAnimation(ra);

                    mCurrentDegree = -360;
                }
            } else {
                ra = new RotateAnimation(
                        mCurrentDegree,
                        mAzimuthDegree,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF,
                        0.5f);

                ra.setDuration(TICK_INTERVAL);
                ra.setFillAfter(true);
                image.startAnimation(ra);
                mCurrentDegree = mAzimuthDegree;
            }

            //tvHeading.setText("Heading: " + Float.toString(-mCurrentDegree) + " degrees");
            tvHeading.setText(String.format("%.0f°",-mCurrentDegree));

            return false;
        }
    });
}