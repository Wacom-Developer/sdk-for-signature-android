package com.wacom.signaturesdkexample.persistence;

import android.content.Context;
import android.content.SharedPreferences;

import com.wacom.signaturesdkexample.R;

public class SettingsPreferences {

    public enum EncryptionMethod {
        NONE, PASSWORD, CERTIFICATE;
    }

    public enum SignatureFormat {
        FSS, ISO_BINARY, ISO_XML;
    }

    private static final String SETTINGS_PREF_NAME = ".pref.settings";

    private static final String IMAGE_WIDTH = "image_width";
    private static final String IMAGE_HEIGHT = "image_height";
    private static final String INK_COLOR = "ink_color";
    private static final String INK_WIDTH = "ink_width";
    private static final String ENCRYPTION_METHOD = "encryption_method";
    private static final String ENCRYPTION_PASSWORD = "encryption_password";
    private static final String SIGNATURE_FORMAT = "signature_format";

    public static final int DEFAULT_IMAGE_WIDTH = 400;
    public static final int DEFAULT_IMAGE_HEIGHT = 400;
    private static final int DEFAULT_INK_COLOR = R.color.color_4;
    private static final int DEFAULT_INK_WIDTH = R.dimen.ink_width_med;
    private static final EncryptionMethod DEFAULT_ENCRYPTION_METHOD = EncryptionMethod.NONE;
    private static final SignatureFormat DEFAULT_SIGNATURE_FORMAT = SignatureFormat.FSS;

    private static SettingsPreferences instance;
    private SharedPreferences settingsPreferences;

    public static SettingsPreferences getInstance(Context context) {
        if (instance == null) {
            instance = new SettingsPreferences(context);
        }
        return instance;
    }

    private SettingsPreferences(Context context) {
        settingsPreferences = context.getSharedPreferences(context.getPackageName()+SETTINGS_PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setImageWidth(int width) {
        SharedPreferences.Editor editor = settingsPreferences.edit();
        editor.putInt(IMAGE_WIDTH, width);
        editor.apply();
    }

    public int getImageWidth() {
        return settingsPreferences.getInt(IMAGE_WIDTH, DEFAULT_IMAGE_WIDTH);
    }

    public void setImageHeight(int height) {
        SharedPreferences.Editor editor = settingsPreferences.edit();
        editor.putInt(IMAGE_HEIGHT, height);
        editor.apply();
    }

    public int getImageHeight() {
        return settingsPreferences.getInt(IMAGE_HEIGHT, DEFAULT_IMAGE_HEIGHT);
    }

    public void setInkColor(int color) {
        SharedPreferences.Editor editor = settingsPreferences.edit();
        editor.putInt(INK_COLOR, color);
        editor.apply();
    }

    public int getInkColor() {
        return settingsPreferences.getInt(INK_COLOR, DEFAULT_INK_COLOR);
    }

    public void setInkWidth(int width) {
        SharedPreferences.Editor editor = settingsPreferences.edit();
        editor.putInt(INK_WIDTH, width);
        editor.apply();
    }

    public int getInkWidth() {
        return settingsPreferences.getInt(INK_WIDTH, DEFAULT_INK_WIDTH);
    }

    public void setEncryptionMethod(EncryptionMethod encryptionMethod) {
        SharedPreferences.Editor editor = settingsPreferences.edit();
        editor.putString(ENCRYPTION_METHOD, encryptionMethod.name());
        editor.apply();
    }

    public EncryptionMethod getEncryptionMethod() {
        return EncryptionMethod.valueOf(settingsPreferences.getString(ENCRYPTION_METHOD, DEFAULT_ENCRYPTION_METHOD.name()));
    }

    public void setEncryptionPassword(String password) {
        SharedPreferences.Editor editor = settingsPreferences.edit();
        editor.putString(ENCRYPTION_PASSWORD, password);
        editor.apply();
    }

    public String getEncryptionPassword() {
        return settingsPreferences.getString(ENCRYPTION_PASSWORD, "");
    }

    public void setSignatureFormat(SignatureFormat signatureFormat) {
        SharedPreferences.Editor editor = settingsPreferences.edit();
        editor.putString(SIGNATURE_FORMAT, signatureFormat.name());
        editor.apply();
    }

    public SignatureFormat getSignatureFormat() {
        return SignatureFormat.valueOf(settingsPreferences.getString(SIGNATURE_FORMAT, DEFAULT_SIGNATURE_FORMAT.name()));
    }
}
