package com.cazi.imggenerator;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cazi.animator.RevealAnimator;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private DatabaseReference mRef;
    private RevealAnimator revealAnimator;
    private RelativeLayout fabToolbar, textContainer;
    private LinearLayout toolbarContainer;
    private SensorManager sensorManager;
    private Sensor sensor;
    private TextView n1, n2, n3;

    private static OnStartProcessing callback;
    public interface OnStartProcessing {
        void startProcessing();
        void stopProcessing();
    }

    public static void setOnStartProcessingListener(OnStartProcessing l) {
        callback = l;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setTitle("ComKey");

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        mRef = database.getReference();

        FloatingActionButton fab = findViewById(R.id.menu_button);
        fabToolbar = findViewById(R.id.fab_toolbar);
        n1 = findViewById(R.id.num1);
        n2 = findViewById(R.id.num2);
        n3 = findViewById(R.id.num3);
        revealAnimator = new RevealAnimator(getApplicationContext(), fab, fabToolbar, 0);
        toolbarContainer = findViewById(R.id.toolbar_container);
        toolbarContainer.setVisibility(View.GONE);
        textContainer = findViewById(R.id.textContainer);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        Camera2Fragment camera2Fragment = new Camera2Fragment();
        fragmentTransaction.add(R.id.fragment_container, camera2Fragment);
        fragmentTransaction.commit();

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (revealAnimator.isFab()) {
                    revealAnimator.showToolbarAnimation();
                }
            }
        });
        revealAnimator.setOnEndTransform(new RevealAnimator.OnEndTransform() {
            @Override
            public void onEnd(boolean isFab) {
                if (!isFab) {
                    AlphaAnimation alphaAnimation = new AlphaAnimation(1f, 0f);
                    alphaAnimation.setStartOffset(3500);
                    alphaAnimation.setDuration(500);
                    alphaAnimation.setFillAfter(true);
                    alphaAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
                    fabToolbar.startAnimation(alphaAnimation);

                    countDown();
                }
            }
        });

        ValueEventListener eventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child("prediction_num").exists()) {
                    Log.d("Hack19", dataSnapshot.child("prediction_num").getValue().toString());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        };
        mRef.addValueEventListener(eventListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this, sensor);

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        double azimuth = Math.toDegrees(event.values[0]);
        double pitch = Math.toDegrees(event.values[1]);

        if (azimuth < Math.abs(10) && pitch < Math.abs(10) && !revealAnimator.isFab()) {
            revealAnimator.setFab(true);
            AlphaAnimation alphaAnimation = new AlphaAnimation(0f, 1f);
            alphaAnimation.setDuration(500);
            alphaAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
            alphaAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) { }
                @Override
                public void onAnimationEnd(Animation animation) {
                    revealAnimator.hideToolbarAnimation();
                }
                @Override
                public void onAnimationRepeat(Animation animation) { }
            });
            fabToolbar.startAnimation(alphaAnimation);
            exitProcessing();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    private void countDown() {
        toolbarContainer.setVisibility(View.VISIBLE);
        n1.setVisibility(View.VISIBLE);
        n2.setVisibility(View.VISIBLE);
        n3.setVisibility(View.VISIBLE);

        AlphaAnimation a = new AlphaAnimation(1f, 0f);
        a.setFillAfter(true);
        a.setDuration(100);
        a.setStartOffset(1000);
        a.setInterpolator(new AccelerateDecelerateInterpolator());
        n1.startAnimation(a);
        a.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) { }
            @Override
            public void onAnimationEnd(Animation animation) {
                final AlphaAnimation a = new AlphaAnimation(1f, 0f);
                a.setFillAfter(true);
                a.setDuration(100);
                a.setStartOffset(1000);
                a.setInterpolator(new AccelerateDecelerateInterpolator());
                n1.setVisibility(View.INVISIBLE);
                n2.startAnimation(a);
                a.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) { }
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        final AlphaAnimation a = new AlphaAnimation(1f, 0f);
                        a.setFillAfter(true);
                        a.setDuration(100);
                        a.setStartOffset(1000);
                        a.setInterpolator(new AccelerateDecelerateInterpolator());
                        n2.setVisibility(View.INVISIBLE);
                        n3.startAnimation(a);
                        a.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) { }
                            @Override
                            public void onAnimationEnd(Animation animation) {
                                startProcessing();
                            }
                            @Override
                            public void onAnimationRepeat(Animation animation) { }
                        });
                    }
                    @Override
                    public void onAnimationRepeat(Animation animation) { }
                });
            }
            @Override
            public void onAnimationRepeat(Animation animation) { }
        });
    }

    private void startProcessing() {
        callback.startProcessing();
    }

    private void exitProcessing() {
        callback.stopProcessing();
        toolbarContainer.setVisibility(View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Toast.makeText(getApplicationContext(), "ASD", Toast.LENGTH_SHORT).show();
        return super.onOptionsItemSelected(item);
    }
}