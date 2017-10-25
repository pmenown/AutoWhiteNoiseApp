package com.cloudadvisory.android.autowhitenoiseapp;

/**
 * Created by pmeno on 06/09/2017.
 */

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
    }

    public static class WhiteNoisePreferenceFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings_main);

            Preference micSensitivity = findPreference(getString(R.string.settings_sensitivity_key));
            bindPreferenceSummaryToValue(micSensitivity);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {

            Log.w("onPreferenceChange ","Preference change is "+ value);
            return true;
        }

        private void bindPreferenceSummaryToValue(Preference preference) {
            preference.setOnPreferenceChangeListener(this);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(preference.getContext());
            Integer preferenceInt = preferences.getInt(preference.getKey(), 0);
            onPreferenceChange(preference, preferenceInt);
        }
    }
    public void resetToDefault(View view){
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        //clears all preferences back to default state
        sharedPrefs.edit().clear().apply();

        //restarts the activity to update the preference appearance
        Intent intent = getIntent();
        overridePendingTransition(0, 0);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        finish();
        overridePendingTransition(0, 0);
        startActivity(intent);
    }
}