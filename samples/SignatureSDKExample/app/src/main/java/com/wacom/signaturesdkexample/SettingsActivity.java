package com.wacom.signaturesdkexample;

import android.Manifest;
import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;

import com.wacom.signaturesdkexample.persistence.SettingsPreferences;

public class SettingsActivity extends Activity {

	private EditText editWidth, editHeight;
	private ImageView colorSelector, inkWidthSelector;
	private RadioButton encryptionSelector, signatureFormatSelector;
	private EditText encryptionPassword;
	private SettingsPreferences settingsPreferences;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.settings_layout);

		// this is only an example of use of the Wacom Signature SDK, so we don't bother about
		// permissions, this should be handle properly in real apps
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
		}

		settingsPreferences = SettingsPreferences.getInstance(this);

		editWidth = (EditText) findViewById(R.id.editWidth);
		editWidth.setText(Integer.toString(settingsPreferences.getImageWidth()));

		editHeight = (EditText) findViewById(R.id.editHeight);
		editHeight.setText(Integer.toString(settingsPreferences.getImageHeight()));

		encryptionPassword = (EditText) findViewById(R.id.encryption_password);
		encryptionPassword.setText(settingsPreferences.getEncryptionPassword());

        switch (settingsPreferences.getInkColor()) {
			case R.color.color_1:
				colorSelector = (ImageView)findViewById(R.id.color_1);
				break;
			case R.color.color_3:
				colorSelector = (ImageView)findViewById(R.id.color_3);
				break;
			case R.color.color_4:
				colorSelector = (ImageView)findViewById(R.id.color_4);
				break;
			case R.color.color_5:
				colorSelector = (ImageView)findViewById(R.id.color_5);
				break;
			case R.color.color_6:
				colorSelector = (ImageView)findViewById(R.id.color_6);
				break;
			case R.color.color_7:
				colorSelector = (ImageView)findViewById(R.id.color_7);
				break;
		}
		colorSelector.setImageResource(R.drawable.btn_color_selected);
		selectInkColor(colorSelector);

		switch (settingsPreferences.getInkWidth()) {
			case R.dimen.ink_width_thin:
				inkWidthSelector = (ImageView)findViewById(R.id.thickness_thin);
				break;
			case R.dimen.ink_width_med:
				inkWidthSelector = (ImageView)findViewById(R.id.thickness_med);
				break;
			case R.dimen.ink_width_thick:
				inkWidthSelector = (ImageView)findViewById(R.id.thickness_bold);
				break;
		}
		inkWidthSelector.setColorFilter(Color.BLACK);
		selectInkWidth(inkWidthSelector);

		switch (settingsPreferences.getEncryptionMethod()) {
			case NONE:
				encryptionSelector = (RadioButton)findViewById(R.id.encryption_method_none);
				break;
			case PASSWORD:
				encryptionSelector = (RadioButton)findViewById(R.id.encryption_method_password);
				break;
			case CERTIFICATE:
				encryptionSelector = (RadioButton)findViewById(R.id.encryption_method_certificate);
				break;
		}
		encryptionSelector.setChecked(true);
		selectEncryptionMode(encryptionSelector);

		switch (settingsPreferences.getSignatureFormat()) {
			case FSS:
				signatureFormatSelector = (RadioButton)findViewById(R.id.signature_format_fss);
				break;
			case ISO_BINARY:
				signatureFormatSelector = (RadioButton)findViewById(R.id.signature_format_iso_binary);
				break;
			case ISO_XML:
				signatureFormatSelector = (RadioButton)findViewById(R.id.signature_format_iso_xml);
				break;
		}
		signatureFormatSelector.setChecked(true);
		selectSignatureFormat(signatureFormatSelector);
	}

	@Override
	public void onPause() {
		super.onPause();

		int width = SettingsPreferences.DEFAULT_IMAGE_WIDTH;
		try {
			width = Integer.parseInt(editWidth.getText().toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
		settingsPreferences.setImageWidth(width);

		int height = SettingsPreferences.DEFAULT_IMAGE_HEIGHT;
		try {
			height = Integer.parseInt(editHeight.getText().toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
		settingsPreferences.setImageHeight(height);

		settingsPreferences.setEncryptionPassword(encryptionPassword.getText().toString());
	}

	public void selectInkColor(View view) {
		if (!colorSelector.equals(view)) {
			colorSelector.setImageResource(R.drawable.btn_color);
			colorSelector = (ImageView) view;
			colorSelector.setImageResource(R.drawable.btn_color_selected);
		}

		switch (view.getId()) {
			case R.id.color_1:
				settingsPreferences.setInkColor(R.color.color_1);
				break;
			//case R.id.color_2:
			//inkColor = getResources().getColor(R.color.color_2);
			//break;
			case R.id.color_3:
				settingsPreferences.setInkColor(R.color.color_3);
				break;
			case R.id.color_4:
				settingsPreferences.setInkColor(R.color.color_4);
				break;
			case R.id.color_5:
				settingsPreferences.setInkColor(R.color.color_5);
				break;
			case R.id.color_6:
				settingsPreferences.setInkColor(R.color.color_6);
				break;
			case R.id.color_7:
				settingsPreferences.setInkColor(R.color.color_7);
				break;
		}
	}

	public void selectInkWidth(View view) {
		if (!inkWidthSelector.equals(view)) {
			inkWidthSelector.setColorFilter(getResources().getColor(R.color.light_grey));
			inkWidthSelector = (ImageView) view;
			inkWidthSelector.setColorFilter(Color.BLACK);
		}

		switch (view.getId()) {
			case R.id.thickness_thin:
				settingsPreferences.setInkWidth(R.dimen.ink_width_thin);
				break;
			case R.id.thickness_med:
				settingsPreferences.setInkWidth(R.dimen.ink_width_med);
				break;
			case R.id.thickness_bold:
				settingsPreferences.setInkWidth(R.dimen.ink_width_thick);
				break;
		}
	}

	public void selectEncryptionMode(View view) {
		switch (view.getId()) {
			case R.id.encryption_method_none:
				settingsPreferences.setEncryptionMethod(SettingsPreferences.EncryptionMethod.NONE);
				break;
			case R.id.encryption_method_password:
				settingsPreferences.setEncryptionMethod(SettingsPreferences.EncryptionMethod.PASSWORD);
				break;
			case R.id.encryption_method_certificate:
				settingsPreferences.setEncryptionMethod(SettingsPreferences.EncryptionMethod.CERTIFICATE);
				break;
		}
	}

	public void selectSignatureFormat(View view) {
		switch (view.getId()) {
			case R.id.signature_format_fss:
				settingsPreferences.setSignatureFormat(SettingsPreferences.SignatureFormat.FSS);
				break;
			case R.id.signature_format_iso_binary:
				settingsPreferences.setSignatureFormat(SettingsPreferences.SignatureFormat.ISO_BINARY);
				break;
			case R.id.signature_format_iso_xml:
				settingsPreferences.setSignatureFormat(SettingsPreferences.SignatureFormat.ISO_XML);
				break;
		}
	}

}
