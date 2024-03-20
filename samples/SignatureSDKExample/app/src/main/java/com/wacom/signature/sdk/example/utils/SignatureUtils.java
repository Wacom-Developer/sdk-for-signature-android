package com.wacom.signature.sdk.example.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.EditText;

import com.wacom.signature.sdk.AdditionalImportIsoData;
import com.wacom.signature.sdk.EncryptionType;
import com.wacom.signature.sdk.EncryptionUtils;
import com.wacom.signature.sdk.ISOMode;
import com.wacom.signature.sdk.Signature;
import com.wacom.signature.sdk.example.FormViewerActivity;
import com.wacom.signature.sdk.exception.SignatureSDKException;

import java.io.IOException;
import java.io.InputStream;

public class SignatureUtils {

    public static void loadSignature(final Context context, String licence, final byte[] signatureData, final String password, final ILoadSignature listener) {
        try {
            final Signature signature = new Signature(context);
            signature.setLicense(licence);

            final SignatureFormatInfo signatureFormatInfo = new SignatureFormatInfo();
            signatureFormatInfo.setSignature(signature);

            if (EncryptionUtils.isEncrypted(signatureData)) {
                try {
                    decryptSignature(context, signature, signatureFormatInfo, signatureData, password, new Runnable() {
                        @Override
                        public void run() {
                            if (loadDecryptedSignature(context, signatureFormatInfo, signatureData)) {
                                listener.onSignatureLoaded(signatureFormatInfo);
                            }
                        }
                    });
                } catch (SignatureSDKException e) {
                    openDialog(context,"Error decrypting signature "+e.getMessage());
                }
            } else {
                signatureFormatInfo.setEncryption(SignatureFormatInfo.Encryption.NONE);
                if (loadDecryptedSignature(context, signatureFormatInfo, signatureData)) {
                    listener.onSignatureLoaded(signatureFormatInfo);
                }
            }
        } catch (SignatureSDKException e) {
            openDialog(context, e.getMessage());
        }
    }

    public static String readAsset(Context context, String file) {
        try (InputStream is = context.getAssets().open(file)) {
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            return new String(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void decryptSignature(Context context, Signature signature, SignatureFormatInfo signatureFormatInfo, byte[] signatureData, String password, Runnable runnable) throws SignatureSDKException {
        if (EncryptionUtils.getEncryptionType(signatureData) == EncryptionType.RSA) {
            signatureFormatInfo.setEncryption(SignatureFormatInfo.Encryption.ASYMMETRIC);
            signature.setPrivateKey(readAsset(context, "private_key.pem"));
            runnable.run();
        } else if (EncryptionUtils.getEncryptionType(signatureData) == EncryptionType.AES) {
            signatureFormatInfo.setEncryption(SignatureFormatInfo.Encryption.SYMMETRIC);
            if ((password != null) && (!password.isEmpty())) {
                try {
                    signatureFormatInfo.setPassword(password);
                    signature.setEncryptionPassword(password);
                    runnable.run();
                } catch (SignatureSDKException e) {
                    openDialog(context,"Invalid password");
                }
            } else {
                openPasswordDialog(context, signature, signatureFormatInfo, runnable);
            }
        } else {
            runnable.run();
        }
    }

    private static void openPasswordDialog(final Context context, final Signature sig, final SignatureFormatInfo signatureFormatInfo, final Runnable runnable) {
        final EditText passwordEditText = new EditText(context);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Password required")
                .setMessage("The signature is encrypted with password, please enter the password:")
                .setView(passwordEditText)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            String password = passwordEditText.getText().toString();
                            signatureFormatInfo.setPassword(password);
                            sig.setEncryptionPassword(password);
                            runnable.run();
                        } catch (SignatureSDKException e) {
                            openDialog(context,"Invalid password");
                        }
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .create();
        dialog.show();
    }

    private static void openDialog(Context context, String message) {
        AlertDialog.Builder signAlertBuilder = new AlertDialog.Builder(context);
        signAlertBuilder.setMessage(message);
        signAlertBuilder.setCancelable(false);
        signAlertBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
            }
        });

        AlertDialog alert = signAlertBuilder.create();
        alert.show();
    }

    private static boolean loadDecryptedSignature(Context context, SignatureFormatInfo signatureFormatInfo, byte[] signatureData) {
        boolean loaded = false;

        // first we try to load the file as FSS
        try {
            signatureFormatInfo.getSignature().setSigData(signatureData);
            signatureFormatInfo.setFormat(SignatureFormatInfo.Format.FSS);
            loaded = true;
        } catch (SignatureSDKException e1) {
            e1.printStackTrace();
        }

        if (!loaded) {
            // try to load text file
            try {
                signatureFormatInfo.getSignature().setSigText(new String(signatureData));
                signatureFormatInfo.setFormat(SignatureFormatInfo.Format.FSS);
                loaded = true;
            } catch (SignatureSDKException e1) {
                e1.printStackTrace();
            }
        }

        if (!loaded) {
            // try to load the file as Binary ISO
            try {
                AdditionalImportIsoData additionalImportIsoData = new AdditionalImportIsoData();
                additionalImportIsoData.setWho("User imported from Binary ISO");
                additionalImportIsoData.setWhy("Signature imported from Binary ISO");
                ISOMode isoMode = EncryptionUtils.isEncrypted(signatureData) ? ISOMode.ISO9784_7_ENCRYPTED_BINARY : ISOMode.ISO19784_7_BINARY;
                signatureFormatInfo.getSignature().importISO(signatureData, isoMode, additionalImportIsoData);
                signatureFormatInfo.setFormat(SignatureFormatInfo.Format.ISO_2007);
                loaded = true;
            } catch (SignatureSDKException e2) {
                e2.printStackTrace();
            }
        }

        if (!loaded) {
            // try to load the file as Binary ISO 2014
            try {
                AdditionalImportIsoData additionalImportIsoData = new AdditionalImportIsoData();
                additionalImportIsoData.setWho("User imported from Binary ISO");
                additionalImportIsoData.setWhy("Signature imported from Binary ISO");
                ISOMode isoMode = EncryptionUtils.isEncrypted(signatureData) ? ISOMode.ISO19784_7_2014_ENCRYPTED_BINARY : ISOMode.ISO19784_7_2014_BINARY;
                signatureFormatInfo.getSignature().importISO(signatureData, isoMode, additionalImportIsoData);
                signatureFormatInfo.setFormat(SignatureFormatInfo.Format.ISO_2014);
                loaded = true;
            } catch (SignatureSDKException e2) {
                e2.printStackTrace();
            }
        }

        if (!loaded) {
            // try to load the file as XML ISO
            try {
                AdditionalImportIsoData additionalImportIsoData = new AdditionalImportIsoData();
                additionalImportIsoData.setWho("User imported from XML ISO");
                additionalImportIsoData.setWhy("Signature imported from XML ISO");
                signatureFormatInfo.getSignature().importISO(new String(signatureData), ISOMode.ISO19785_3_XML, additionalImportIsoData);
                signatureFormatInfo.setFormat(SignatureFormatInfo.Format.ISO_XML);
                loaded = true;
            } catch (SignatureSDKException e3) {
                e3.printStackTrace();
            }
        }

        if (!loaded) {
            openDialog(context,"The imported signature has an unsupported format");
        }

        return loaded;
    }
}
