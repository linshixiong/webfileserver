package com.linsx.webserver;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity implements
		OnPreferenceChangeListener {

	private final String KEY_SERVER_PORT = "edittext_server_port";
	private final String KEY_VERSION = "prdference_version";
	private EditTextPreference preferencePort;
	private Preference preferenceVersion;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings_prefs);
		preferencePort = (EditTextPreference) findPreference(KEY_SERVER_PORT);
		preferenceVersion = (Preference) findPreference(KEY_VERSION);
		if (preferencePort != null) {
			preferencePort.setOnPreferenceChangeListener(this);
			updatePreferences();
		}

		if (preferenceVersion != null) {
			try {
				preferenceVersion.setSummary(this.getPackageManager()
						.getPackageInfo(getPackageName(), 0).versionName);
			} catch (Exception e) {

			}
		}

	}

	private void updatePreferences() {
		if (preferencePort != null) {
			preferencePort.setText(String.valueOf(Settings.getPort()));
			preferencePort.setSummary(getResources().getString(
					R.string.server_port_summary, Settings.getPort()));
		}
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (preference.getKey().equals(KEY_SERVER_PORT)) {
			int port = Integer.parseInt(newValue.toString());
			if (port < 1024 || port > 65535) {
				Toast.makeText(this, R.string.server_port_invalid,
						Toast.LENGTH_SHORT).show();
			} else {
				Settings.setPort(port);
				updatePreferences();
			}

		}
		return false;
	}

}
