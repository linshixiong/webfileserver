package com.linsx.webserver;

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity implements
		OnPreferenceChangeListener {

	private final String KEY_SERVER_PORT = "edittext_server_port";
	private final String KEY_VERSION = "prdference_version";
	private final String KEY_AUTO_START="key_auto_start";
	private EditTextPreference preferencePort;
	private Preference preferenceVersion;
	private CheckBoxPreference preferenceAutoStart;
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings_prefs);
		preferencePort = (EditTextPreference) findPreference(KEY_SERVER_PORT);
		preferenceVersion = (Preference) findPreference(KEY_VERSION);
		preferenceAutoStart=(CheckBoxPreference)findPreference(KEY_AUTO_START);
		if (preferencePort != null) {
			preferencePort.setOnPreferenceChangeListener(this);
		}

		if (preferenceVersion != null) {
			try {
				preferenceVersion.setSummary(this.getPackageManager()
						.getPackageInfo(getPackageName(), 0).versionName);
			} catch (Exception e) {

			}
		}
		if(preferenceAutoStart!=null){
			preferenceAutoStart.setOnPreferenceChangeListener(this);	
		}
		updatePreferences();
	}

	private void updatePreferences() {
		if (preferencePort != null) {
			preferencePort.setText(String.valueOf(Settings.getPort()));
			preferencePort.setSummary(getResources().getString(
					R.string.server_port_summary, Settings.getPort()));
		}
		if(preferenceAutoStart!=null){
			preferenceAutoStart.setChecked(Settings.isServerAutoStart());
		}
		
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (KEY_SERVER_PORT.equals(preference.getKey())) {
			int port = Integer.parseInt(newValue.toString());
			if (port < 1024 || port > 65535) {
				Toast.makeText(this, R.string.server_port_invalid,
						Toast.LENGTH_SHORT).show();
			} else {
				Settings.setPort(port);
				updatePreferences();
				Intent intent=new Intent(Intents.ACTION_RESTART_SERVER);
				sendBroadcast(intent);
				
			}

		}
		if(KEY_AUTO_START.equals(preference.getKey())){
			boolean checked=Boolean.parseBoolean(newValue.toString());
			Settings.setServerAutoStart(checked);
			updatePreferences();
		}
		return false;
	}

}
