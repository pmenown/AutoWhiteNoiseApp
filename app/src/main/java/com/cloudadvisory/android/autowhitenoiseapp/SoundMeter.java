package com.cloudadvisory.android.autowhitenoiseapp;

/**
 * Created by pmeno on 12/04/2017.
 *
 *
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This has been modified from the original google content
 *
 **/


import android.media.MediaRecorder;
import android.util.Log;

import com.google.firebase.crash.FirebaseCrash;

import java.io.IOException;

public class SoundMeter {
    // This file is used to record voice

    private MediaRecorder mRecorder = null;

    public void start() {

        if (mRecorder == null) {

            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mRecorder.setOutputFile("/dev/null");

            try {
                mRecorder.prepare();
            } catch (IllegalStateException e) {
                FirebaseCrash.logcat(Log.ERROR, "mRecorder start() ", "IllegalStateException caught");
                FirebaseCrash.report(e);

                e.printStackTrace();
            } catch (IOException e) {
                FirebaseCrash.logcat(Log.ERROR, "mRecorder start() ", "IOException caught");
                FirebaseCrash.report(e);
                e.printStackTrace();
            }
            mRecorder.start();
        }
    }

    public void stop() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
    }

    public double getAmplitude() {
        if (mRecorder != null)
            return  (mRecorder.getMaxAmplitude()/2700.0);
        else
            return 0;
        }
    }