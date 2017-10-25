package com.cloudadvisory.android.autowhitenoiseapp;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.IdRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Switch;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.analytics.FirebaseAnalytics;

import static com.cloudadvisory.android.autowhitenoiseapp.R.color.accent;
import static com.cloudadvisory.android.autowhitenoiseapp.R.color.icons;
import static com.cloudadvisory.android.autowhitenoiseapp.R.raw.fan;
import static com.cloudadvisory.android.autowhitenoiseapp.R.raw.tele;
import static com.cloudadvisory.android.autowhitenoiseapp.R.raw.waves;

public class MainActivity extends AppCompatActivity {

    //declare variables
    public MediaPlayer mPlayer;
    public SeekBar mVolumeSeekbar = null;
    private AudioManager audioManager = null;
    String soundType;
    boolean isOn = true;
    int duration = 1;

    //firebase variables
    FirebaseAnalytics mFirebaseAnalytics;

    // Top Banner Adview variable
    AdView mAdView;

    //constants
    private static final int POLL_INTERVAL = 500;

    //permission request result handler
    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 0;

//    running state
    private boolean mRunning = false;

//    config state
    public static int mThreshold;
    public static String mDefaultSoundFile;
    public static String mDefaultSoundDuration;

    private PowerManager.WakeLock mWakeLock;

    private Handler mHandler = new Handler();

    //Sensor variable
    private SoundMeter mSensor;

    // Create runnable thread to Monitor Voice
    private Runnable mPollTask = new Runnable() {
        public void run() {

            Switch autoDectectSwitch = (Switch) findViewById(R.id.autodetect_Switch);
            if (autoDectectSwitch.isChecked()) {

                Log.i("mPollTask Auto-detect", "" + autoDectectSwitch.isChecked());
                if (mPlayer == null) {

                    // Runnable(mPollTask) will again execute after POLL_INTERVAL
                    mHandler.postDelayed(mPollTask, POLL_INTERVAL);
                    Log.i("runnable", "PostDelayed mPollTask");

                    double amp = mSensor.getAmplitude();
                    Log.i("runnable", "amplitude = " + amp);
                    // updateDisplay("Monitoring Voice...", amp);

                    if ((amp > mThreshold)) {
                        Log.i("Noise", "==== onCreate ===");
                        initPlayWhiteNoise();
                    }

                } else {
                    Log.i("mPollTask Auto-detect", "Sound is on!"+mPlayer);
                }

            } else {

                Log.i("Runnable Auto-detect", "" + autoDectectSwitch.isChecked());
                stopNoiseMonitor();
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            // Respond to a click on the "settings" menu option
            case R.id.action_settings:
                clearSound();

                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);

                return true;
            // Respond to a click on the "Rate app" menu option
            case R.id.action_rate_app:
                //launch app page in play store or webpage
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("market://details?id=" + getPackageName()));
                    startActivity(intent);
                } catch (ActivityNotFoundException e){
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
                }

                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // Navigate back to parent activity (CatalogActivity)
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i("Noise", "==== onCreate ===");
        checkPreferenceSettings();
        setSoundTypeRadioButtonCheck();
        setSoundDurationRadioButtonCheck();

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        //on button changing to default background on start up
        Button playButton = (Button) findViewById(R.id.play_Button);
        playButton.getBackground().setColorFilter(getResources().getColor(icons), PorterDuff.Mode.MULTIPLY);

        //initialising volume control
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        initControls();
        mVolumeSeekbar.setProgress(9);

        // Create SoundMeter object
        mSensor = new SoundMeter();

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "Noise Alert stay on");

        //top banner admob advert
        MobileAds.initialize(this, "ca-app-pub-7497403023351367~1525318338");
        mAdView = (AdView) findViewById(R.id.TopBanner);
        AdRequest adRequest = new AdRequest.Builder()
                //.addTestDevice("532B8648EAFB3B292E29C90033F6C626")
                .build();
        mAdView.loadAd(adRequest);

    }

    //TODO - IMPORTANT
    //TODO - set analytics linked to peoples default settings
    //TODO - set remote config up for share preference defaults
    //TODO - check meewok app for audio priority

//TODO - Sleep log,
//TODO - number text view on settings screen,
//TODO - volume to work with system controls,
//TODO - create ability for user to record custom sound for use
    //TODO - add instructions
    //TODO - create schedule auto-detect scheduler


    //check record audio permission when switch pressed
    public void checkPermission(View view) {
        Log.i("checkPermission", "comes on");
        // Assume thisActivity is the current activity


        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i("checkPermission", "permission not granted");

            Switch autoDectectSwitch = (Switch) findViewById(R.id.autodetect_Switch);
            autoDectectSwitch.setChecked(false);

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {

                Log.i("checkPermission", "explanation");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.
                Log.i("checkPermission", "no explanation");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_REQUEST_RECORD_AUDIO);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }

        } else {
            Log.i("checkPermission", "permission granted");
            startNoiseMonitor();
        }
    }

    //handle the permissions request result
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_RECORD_AUDIO: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // Audio-related task you need to do.
                    Switch autoDectectSwitch = (Switch) findViewById(R.id.autodetect_Switch);
                    autoDectectSwitch.setChecked(true);

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Switch autoDectectSwitch = (Switch) findViewById(R.id.autodetect_Switch);
                    autoDectectSwitch.setChecked(false);
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    // button click activating white noise sound
    public void playWhiteNoiseButton(View view) {

        //TODO - switchoff auto-detect if turned off manually
//        if (!isOn) {
//            Switch autoDectectSwitch = (Switch) findViewById(R.id.autodetect_Switch);
//            autoDectectSwitch.setChecked(false);
//        }
        initPlayWhiteNoise();

    }

    public void initPlayWhiteNoise() {

        mSensor.stop();
        mHandler.removeCallbacks(mPollTask);
        mRunning = false;
        if (isOn) {
            Button playButton = (Button) findViewById(R.id.play_Button);
            durationMethod();
            Log.i("tag", "initPlay after duration method=" + duration);
            whiteNoiseTypeMethod();
            loopPlayWhiteNoise();
            Log.i("tag", "initPlay after loopPlayWhiteNoise metheod duration =" + duration);
            isOn = false;
            playButton.getBackground().setColorFilter(getResources().getColor(accent), PorterDuff.Mode.MULTIPLY);

        } else

        {
            soundStop();
            /***Switch autoDectectSwitch = (Switch) findViewById(R.id.autodetect_Switch);
            autoDectectSwitch.setChecked(false);***/

        }
    }

    public void loopPlayWhiteNoise() {

        Log.i("Loop init", "duration =" + duration);

        if (duration > 0) {
            Log.i("Loop start Duration > 0", "duration =" + duration);
            mPlayer.start();
            Log.i("Loop SetOnComplt", "duration =" + duration);

            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    if (duration > 0) {
                        Log.i("Loop OnCompletion", "duration =" + duration);
                        duration = duration - 1;
                        loopPlayWhiteNoise();
                    } else {

                        soundStop();
                    }
                }
            });
        } else {
            soundStop();
        }
    }

    //invoked when sound should stop.
    public void soundStop() {
        duration = 0;
        Log.i("tag", "duration =" + duration);
        isOn = true;
        clearSound();

        //Enable the sound type radio button group
        RadioGroup radioGroupType = (RadioGroup) findViewById(R.id.radioGroupType);
        for (int i = 0; i < radioGroupType.getChildCount(); i++) {
            ((RadioButton) radioGroupType.getChildAt(i)).setEnabled(true);
        }

        //change button background to default
        Button playButton = (Button) findViewById(R.id.play_Button);
        playButton.getBackground().setColorFilter(getResources().getColor(icons), PorterDuff.Mode.MULTIPLY);

        //go to noise monitor start method
        startNoiseMonitor();

    }
/*
    setting the duration of the sound file for the loop method
    */
    public void durationMethod() {

        //Analytics select event initialisation
        String analId = "Duration_Method";
        String name = "";
        Bundle bundle = new Bundle();


        RadioGroup radioGroupMins = (RadioGroup) findViewById(R.id.radioGroupMins);
        int id = radioGroupMins.getCheckedRadioButtonId();
        RadioButton radioButton = (RadioButton) findViewById(id);
        if (radioButton.getId() == R.id.radioButton10) {

            duration = 2;
            name = "10";
            Log.i("tag", "duration method selected =" + duration);

        } else if (radioButton.getId() == R.id.radioButton20) {

            duration = 4;
            name = "20";
            Log.i("tag", "duration method selected =" + duration);

        } else if (radioButton.getId() == R.id.radioButton40) {

            duration = 8;
            name = "40";
            Log.i("tag", "duration method selected =" + duration);

        }

        radioGroupMins.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int id) {
                durationMethod();
            }
        });
        Log.i("tag", "duration method because exiting duration =" + duration);

        //Analytics select event broadcast
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, analId);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, name);
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    //Choosing sound file from radio button
    private void whiteNoiseTypeMethod() {

        //Analytics select event initialisation
        String analId = "Type_Method";
        String name = "";
        Bundle bundle = new Bundle();

        RadioGroup radioGroupType = (RadioGroup) findViewById(R.id.radioGroupType);
        int id = radioGroupType.getCheckedRadioButtonId();
        RadioButton radioButton = (RadioButton) findViewById(id);
        if (radioButton.getId() == R.id.radioButtonWaves) {

            mPlayer = MediaPlayer.create(this, waves);
            name = "waves";

        } else if (radioButton.getId() == R.id.radioButtonFan) {

            mPlayer = MediaPlayer.create(this, fan);
            name = "fan";

        } else if (radioButton.getId() == R.id.radioButtonTele) {

            mPlayer = MediaPlayer.create(this, tele);
            name = "tele";

        }

        Log.i("tag", "Type =" + soundType);
        /*** Onchange listener for radio group
        radioGroupType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int id) {
                whiteNoiseTypeMethod();
            }
        });***/

        //disable button once sound started to prevent multiple noises at once
        for (int i = 0; i < radioGroupType.getChildCount(); i++) {
            ((RadioButton) radioGroupType.getChildAt(i)).setEnabled(false);
        }

        //Analytics select event broadcast
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, analId);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, name);
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }


    //SEEKBAR VOLUME
    private void initControls() {
        try {
            mVolumeSeekbar = (SeekBar) findViewById(R.id.seekBar);
            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            mVolumeSeekbar.setMax(audioManager
                    .getStreamMaxVolume(AudioManager.STREAM_MUSIC));
            mVolumeSeekbar.setProgress(audioManager
                    .getStreamVolume(AudioManager.STREAM_MUSIC));


            mVolumeSeekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                @Override
                public void onStopTrackingTouch(SeekBar arg0) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar arg0) {
                }

                @Override
                public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                            progress, 0);
                }
            });



        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //SEEKBAR VOLUME END


    //on resuming the app from sleep state
    @Override
    public void onResume() {
        super.onResume();
        Log.i("Noise", "==== onResume ===");

        //check status of sound monitor option switch
        Switch autoDectectSwitch = (Switch) findViewById(R.id.autodetect_Switch);
        if (autoDectectSwitch.isChecked()) {

            Log.i("onResume Auto-detect", "" + autoDectectSwitch.isChecked());

            if (mPlayer != null) {
                Log.i("onResume mPlayer null?", "" + mPlayer);

                if (!mRunning) {
                    Log.i("onResume mRunning", "" + mRunning);
                    startNoiseMonitor();

                } else {

                    Log.i("onResume Auto-detect", "" + autoDectectSwitch.isChecked());
                    stopNoiseMonitor();
                }
            } else {
                Log.i("onResume mPlayer null?", "" + mPlayer);
            }
        } else {

            Log.i("onResume Auto-detect", "" + autoDectectSwitch.isChecked());
            stopNoiseMonitor();
        }
    }
    //start noise monitor method
    public void startNoiseMonitor() {
        mSensor.stop();
        Switch autoDectectSwitch = (Switch) findViewById(R.id.autodetect_Switch);
        if (autoDectectSwitch.isChecked()) {

            Log.i("Start Auto-detect", "" + autoDectectSwitch.isChecked());

            if (mPlayer == null) {
                Log.i("Start mPlayer null?", "" + mPlayer);

                if (!mRunning) {
                    Log.i("Start mRunning", "" + mRunning);

                    mRunning = true;

                    Log.i("Noise", "==== start ===");
                    mHandler.postDelayed(mPollTask, POLL_INTERVAL);
                    mSensor.start();

                    if (!mWakeLock.isHeld()) {
                        Log.i("Start mWakeLock", "IsNotHeld");
                        mWakeLock.acquire();
                    }

                    //Noise monitoring start
                    // Runnable(mPollTask) will execute after POLL_INTERVAL


                } else {
                    Log.i("Start mRunning", "" + mRunning);
                }
            } else {
                Log.i("Start Auto-detect", "Sound is on!"+mPlayer);
            }

        } else {

            Log.i("Start Auto-detect", "" + autoDectectSwitch.isChecked());
            stopNoiseMonitor();
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i("Noise", "==== onPause ===");

        //Stop noise monitoring when app is closed

    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i("Noise", "==== onStop ===");

        //un-register content register (Volume button detection)

        //Stop noise monitoring when app is closed
        stopNoiseMonitor();
    }

    private void stopNoiseMonitor() {
        Log.i("Noise", "==== Stop Noise Monitoring===");
        Log.i("stop mPlayer null?", "" + mPlayer);

        //mHandler.removeCallbacks(mSleepTask);
        mHandler.removeCallbacks(mPollTask);
        mSensor.stop();
        mRunning = false;

    }

    //clear down media player resource
    private void clearSound(){
        if (mPlayer !=null){
            mPlayer.release();
            mPlayer = null;}
    }

    //loads the preference keys into useable variables
    private void checkPreferenceSettings(){
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        //Microphone sensitivity setting
        mThreshold = sharedPrefs.getInt(
                getString(R.string.settings_sensitivity_key),R.string.settings_sensitivity_default);
        //Sound file setting
        mDefaultSoundFile = sharedPrefs.getString(
                getString(R.string.default_sound_file_key),getString(R.string.default_sound_file_default));
        //Sound duration setting
        mDefaultSoundDuration = sharedPrefs.getString(
                getString(R.string.default_sound_duration_key),getString(R.string.default_sound_duration_default));

        Log.i("checkPreferenceSet","mThreshold is "+ mThreshold);
        Log.i("checkPreferenceSet","mDefaultSoundFile is "+ mDefaultSoundFile);
        Log.i("checkPreferenceSet","mDefaultSoundDuration is "+ mDefaultSoundDuration);
    }

    private void setSoundTypeRadioButtonCheck(){

        if (mDefaultSoundFile.equals("waves")) {
            RadioButton radioButton = (RadioButton) findViewById(R.id.radioButtonWaves);
            radioButton.setChecked(true);

        } else {if (mDefaultSoundFile.equals("fan")) {
            RadioButton radioButtonfan = (RadioButton) findViewById(R.id.radioButtonFan);
            radioButtonfan.setChecked(true);


        } else {if (mDefaultSoundFile.equals("tele")){
            RadioButton radioButtonTele = (RadioButton) findViewById(R.id.radioButtonTele);
            radioButtonTele.setChecked(true);
        }
        }
        }
    }

    private void setSoundDurationRadioButtonCheck(){

        if (mDefaultSoundDuration.equals("10")) {
            RadioButton radioButton = (RadioButton) findViewById(R.id.radioButton10);
            radioButton.setChecked(true);

        } else {if (mDefaultSoundDuration.equals("20")) {
            RadioButton radioButton20 = (RadioButton) findViewById(R.id.radioButton20);
            radioButton20.setChecked(true);


        } else {if (mDefaultSoundDuration.equals("40")){
            RadioButton radioButton40 = (RadioButton) findViewById(R.id.radioButton40);
            radioButton40.setChecked(true);
        }
        }
        }
    }

}