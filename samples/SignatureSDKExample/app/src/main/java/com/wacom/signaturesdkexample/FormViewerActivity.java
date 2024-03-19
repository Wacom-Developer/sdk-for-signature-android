package com.wacom.signaturesdkexample;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.TextView;

import com.wacom.signaturesdkexample.persistence.SettingsPreferences;
import com.wacom.ink.willformat.FileUtils;
import com.wacom.signature.sdk.AdditionalImportIsoData;
import com.wacom.signature.sdk.DataStatus;
import com.wacom.signature.sdk.Hash;
import com.wacom.signature.sdk.ImageType;
import com.wacom.signature.sdk.IntegrityStatus;
import com.wacom.signature.sdk.Key;
import com.wacom.signature.sdk.Signature;
import com.wacom.signature.sdk.SignatureFormat;
import com.wacom.signature.sdk.activity.DynamicCaptureActivity;
import com.wacom.signature.sdk.exception.SignatureSDKException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import ua.com.vassiliev.androidfilebrowser.FileBrowserActivity;

public class FormViewerActivity extends Activity {

    public static final String FORM_PATH = "form_path";
    public static final String LICENSE = "eyJhbGciOiJSUzUxMiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJMTVMiLCJleHAiOjE1NTk5ODEyNzQsImlhdCI6MTUyODQ0NzQxMiwicmlnaHRzIjpbIlNJR19TREtfQ09SRSIsIlNJR05BVFVSRV9TREtfQUNDRVNTIiwiU0lHX1NES19JU08iXSwiZGV2aWNlcyI6WyJXQUNPTV9BTlkiXSwidHlwZSI6ImV2YWwiLCJsaWNfbmFtZSI6IklTTyBJbnN0cnVtZW50ZWQgdGVzdCIsIndhY29tX2lkIjoiMTUzYmU4OWYtOTYzNy00YWI0LTk5OTktOTRlMGM4ZTBkMmEzIiwibGljX3VpZCI6IjIwOGM1NjkyLWJlNjctNDM2MS05N2ZmLTgxMTU3MzYyZmY5YiIsImFwcHNfd2luZG93cyI6W10sImFwcHNfaW9zIjpbXSwiYXBwc19hbmRyb2lkIjpbImNvbS53YWNvbS5zaWduYXR1cmVzZGtleGFtcGxlIl0sIm1hY2hpbmVfaWRzIjpbIjAwNTA1NkMwMDAwMSIsIjAwNTA1NkMwMDAwOCJdfQ.uqHfTZN_S29onO6AQIyNDKliy0EZp6IqyPTCvR7uUo04vfVxibmMHGxkJN2Fh-IqehpJNk5ZqJTAj5SHz53M4q_3HZSD9SokE8wItlG8qcQbii-GkKGAge4AgTsKk1C0yytarNC7iIGtMdn1egD13oss8TiimcBY9mlLEH2h7LVUheTCl-K2v_FZYJhMDervCWKnn4KTaeyrn1XlmMK0a71QKdzqRQHyay7aOZz3ggbBQZ4BbB8R6K-uA3hHW-2NPX7A9_qSyQrf_LLZD-t4ZDzSj66s7yLUvbyNEgTpBXrLEFl65XfFj4vNpZrJaC6pJvbC69GvD6NeP6hEy6yDPQ";

    private static final float CAPTURE_WINDOW_MARGINS_PERCENT = 0.05f;
    private static final float MAX_PORTRAIT_WIDTH_INCH = 3.0f;
    private static final float MAX_PORTRAIT_HEIGHT_INCH = 3f;
    private static final float MAX_LANDSCAPE_WIDTH_INCH = 3.5f;
    private static final float MAX_LANDSCAPE_HEIGHT_INCH = 2.5f;
    private static final float BUTTONS_INCHES = 1f;

    private static final int DYNAMIC_CAPTURE_ACTION = 1;
    private static final int OPEN_FILE_ACTION = 2;

    private boolean captureSignatureWindowOpened;
    private String imageName;
    private String formName;
    private String form;
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.form_viewer_layout);

        Intent intent = getIntent();
        String formPath = intent.getStringExtra(FORM_PATH);
        if (formPath == null) {
            formPath = "file:///android_asset/form.html";
        } else {
            File file = new File(formPath);
            formName = file.getName();
            imageName = formName.replace(".html", ".png");
        }

        webView = (WebView)findViewById(R.id.webView);
        webView.setWebChromeClient(new WebChromeClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new JavaScriptInferface(), "javaInterface");
        webView.loadUrl(formPath);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == DYNAMIC_CAPTURE_ACTION) {
            captureSignatureWindowOpened = false;
            if ((requestCode == DYNAMIC_CAPTURE_ACTION) && (data != null)) {
                Exception exception = (Exception) data.getSerializableExtra(DynamicCaptureActivity.CAPTURE_EXCEPTION);
                if (exception != null) {
                    openDialog(exception.toString());
                } else {
                    if (resultCode == Activity.RESULT_OK) {
                        if ((data != null) && (data.hasExtra(DynamicCaptureActivity.SIGNATURE_DATA))) {
                            saveForm(data.getByteArrayExtra(DynamicCaptureActivity.SIGNATURE_DATA));
                        }
                    }
                }
            }
        } else if (requestCode == OPEN_FILE_ACTION) {
            if (resultCode == RESULT_OK) {
                loadSignature(data.getStringExtra(FileBrowserActivity.returnFileParameter));
            }
        }
    }

    private void generateSignature(String who, String why) {
        if (captureSignatureWindowOpened) {
            return;
        }

        SettingsPreferences prefs = SettingsPreferences.getInstance(this);
        int inkColor = ContextCompat.getColor(this, prefs.getInkColor());

        TypedValue typedValue = new TypedValue();
        getResources().getValue(prefs.getInkWidth(), typedValue, true);
        float inkWidth = typedValue.getFloat();

        //Key key = new Key(Key.Type.KeyMD5MAC,"qnscAdgRlkIhAUPY44oiexBKtQbGY0orf7OV1I50".getBytes());
        Key key = new Key(Key.Type.KeySHA256);
        Point dimensions = getCaptureWindowDimensions();
        // we assume that the space for the buttons is a constant percent always
        int portraitAvailableSpace = dimensions.x - (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_IN, BUTTONS_INCHES, getResources().getDisplayMetrics());

        Intent captureIntent = new Intent(this, DynamicCaptureActivity.class);
        captureIntent.putExtra(DynamicCaptureActivity.LICENSE, LICENSE);
        captureIntent.putExtra(DynamicCaptureActivity.WHO, getClipText(who, portraitAvailableSpace));
        captureIntent.putExtra(DynamicCaptureActivity.WHY, why);
        captureIntent.putExtra(DynamicCaptureActivity.WIDTH, dimensions.x);
        captureIntent.putExtra(DynamicCaptureActivity.HEIGHT, dimensions.y);
        captureIntent.putExtra(DynamicCaptureActivity.INK_COLOR, inkColor);
        captureIntent.putExtra(DynamicCaptureActivity.INK_WIDTH, inkWidth);
        captureIntent.putExtra(DynamicCaptureActivity.KEY, key);

        switch (prefs.getSignatureFormat()) {
            case FSS:
                captureIntent.putExtra(DynamicCaptureActivity.SIGNATURE_FORMAT, SignatureFormat.FSS);
                break;
            case ISO_BINARY:
                captureIntent.putExtra(DynamicCaptureActivity.SIGNATURE_FORMAT, SignatureFormat.ISO_BINARY);
                break;
            case ISO_XML:
                captureIntent.putExtra(DynamicCaptureActivity.SIGNATURE_FORMAT, SignatureFormat.ISO_XML);
                break;
        }


        Hash hash = new Hash(Hash.HashType.KeySHA256, form.getBytes());
        captureIntent.putExtra(DynamicCaptureActivity.HASH, hash);

        startActivityForResult(captureIntent, DYNAMIC_CAPTURE_ACTION);
        captureSignatureWindowOpened = true;
    }

    private Point getCaptureWindowDimensions() {
        // firstly we get the device screen dimensions in inches
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);

        float inchWidth = size.x/dm.xdpi;
        float inchHeight = size.y/dm.ydpi;
        float marginX = CAPTURE_WINDOW_MARGINS_PERCENT * inchWidth;
        float marginY = CAPTURE_WINDOW_MARGINS_PERCENT * inchHeight;
        float windowWidthInch = inchWidth - (marginX*2);
        float windowHeightInch = inchHeight - (marginY*2);

        if (inchWidth < inchHeight) {
            // PORTRAIT DIMENSIONS
            // In portrait dimensions the y axis is bigger than x axis

            // We converted to inch in order to compare with the max sizes
            // independent of the screen device
            if (windowWidthInch > MAX_PORTRAIT_WIDTH_INCH) {
                windowWidthInch = MAX_PORTRAIT_WIDTH_INCH;
            }

            if (windowHeightInch > MAX_PORTRAIT_HEIGHT_INCH) {
                windowHeightInch = MAX_PORTRAIT_HEIGHT_INCH;
            }

        } else {
            // LANDSCAPE DIMENSIONS
            // In landscape dimensions the x axis is bigger than y axis

            if (windowWidthInch > MAX_LANDSCAPE_WIDTH_INCH) {
                windowWidthInch = MAX_LANDSCAPE_WIDTH_INCH;
            }

            if (windowHeightInch > MAX_LANDSCAPE_HEIGHT_INCH) {
                windowHeightInch = MAX_LANDSCAPE_HEIGHT_INCH;
            }

        }

        // We convert again to pixels
        int windowWidth = (int)(windowWidthInch * dm.xdpi);
        int windowHeight = (int)(windowHeightInch * dm.ydpi);

        return new Point(windowWidth, windowHeight);
    }

    private String getClipText(String text, int availableSpace) {
        TextView textView = (TextView)findViewById(R.id.clipped_text);
        Rect bounds = new Rect();
        Paint textPaint = textView.getPaint();
        int length = text.length();
        String dots = "...";

        // if the text don't fix at first we add 3 dots.
        textPaint.getTextBounds(text, 0, text.length(), bounds);
        if (bounds.width() > availableSpace) {
            textPaint.getTextBounds(dots, 0, dots.length(), bounds);
            availableSpace -= bounds.width();

            do {
                textPaint.getTextBounds(text, 0, length--, bounds);
            } while (bounds.width() > availableSpace);

        }

        String clippedText = null;
        if (length == text.length()) {
            clippedText = text;
        } else {
            clippedText = text.substring(0, length) + dots;
        }

        return clippedText;
    }

    private void openDialog(String message) {
        AlertDialog.Builder fingerSignAlertBuilder = new AlertDialog.Builder(this);
        fingerSignAlertBuilder.setMessage(message);
        fingerSignAlertBuilder.setCancelable(false);
        fingerSignAlertBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
            }
        });

        AlertDialog fingerAlert = fingerSignAlertBuilder.create();
        fingerAlert.show();
    }

    private void generateFilledForm(String name, String reason, String comments) {
        try (InputStream is = getAssets().open("form.html")) {
            String rootName = name+"_"+ System.currentTimeMillis();
            imageName = rootName + ".png";
            formName = rootName + ".html";

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = factory.newDocumentBuilder();
            Document doc = dBuilder.parse(is);

            doc.getElementById("nameField").setAttribute("value", name);
            doc.getElementById("nameField").setAttribute("readonly", "true");
            doc.getElementById("reasonField").setAttribute("value", reason);
            doc.getElementById("reasonField").setAttribute("readonly", "true");
            doc.getElementById("commentsField").setTextContent(comments);
            doc.getElementById("commentsField").setAttribute("readonly", "true");

            doc.getElementById("buttonDiv").removeChild(doc.getElementById("import"));
            Element button = doc.getElementById("button");
            button.setTextContent("Verify form");
            button.setAttribute("onclick", "javascript:verifyForm()");

            Element element = doc.createElement("img");
            element.setAttribute("src", imageName);
            doc.getElementById("signatureDiv").appendChild(element);

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            //initialize StreamResult with File object to save to file
            StreamResult result = new StreamResult(new StringWriter());
            DOMSource source = new DOMSource(doc);
            transformer.transform(source, result);
            form = result.getWriter().toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveForm(byte[] signatureData) {
        // save the form as html
        File parent = new File(Environment.getExternalStorageDirectory()+"/forms");
        if (!parent.exists()) {
            parent.mkdir();
        }

        writeFile(parent.getAbsolutePath()+"/"+formName, form);

        // generate the signature image
        Signature sig = new Signature(this);
        try {
            sig.setLincense(LICENSE);

            SettingsPreferences prefs = SettingsPreferences.getInstance(this);
            switch (prefs.getSignatureFormat()) {
                case FSS:
                    sig.setSigData(signatureData);
                    break;
                case ISO_BINARY:
                    sig.importFromBinaryISO(signatureData);
                    break;
                case ISO_XML:
                    String s1 = new String(signatureData);
                    System.out.println(s1);
                    sig.importFromXmlISO(new String(signatureData));
                    break;
            }

            switch (prefs.getEncryptionMethod()){
                case PASSWORD:
                    String password = prefs.getEncryptionPassword();
                    if ((password != null) && (!password.isEmpty())) {
                        sig.setEncryptionPassword(password);
                    }
                    break;
                case CERTIFICATE:
                    // the signature is encrypted with the public key
                    sig.setPublicKey(readAsset(this, "public_key.pem"));
                    break;
            }

            int inkColor = ContextCompat.getColor(this, prefs.getInkColor());

            TypedValue typedValue = new TypedValue();
            getResources().getValue(prefs.getInkWidth(), typedValue, true);
            float inkWidth = typedValue.getFloat();

            int signatureFlags = Signature.RenderBackgroundTransparent | Signature.RenderColorARGB_8888 | Signature.RenderEncodeData;

            sig.renderBitmapToFile(parent.getAbsolutePath()+"/"+imageName,
                    prefs.getImageWidth(), prefs.getImageHeight(), ImageType.PNG,
                    inkWidth, inkColor, Color.WHITE, 0, 0, signatureFlags);

        } catch (SignatureSDKException e) {
            e.printStackTrace();
        }

        // load the new form
        webView.loadUrl(Uri.fromFile(new File(parent.getAbsoluteFile()+"/"+formName)).toString());

    }

    private void writeFile(String filePath, String fileText) {
        try (PrintWriter out = new PrintWriter(filePath)) {
            out.print(fileText);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void verifyHash(String imagePath) {
        try {
            Signature sig = new Signature(this);
            sig.setLincense(LICENSE);
            sig.readEncodedBitmapFile(imagePath);

            boolean passwordNeeded = false;

            if (sig.isEncrypted()) {
                // in this example we have both encryption by password and certificate
                // in order to know which one it has been use we do this.
                // Note that this is only for teaching purposes, not using on real production apps
                try {
                    sig.setPrivateKey(readAsset(this, "private_key.pem"));
                } catch (SignatureSDKException e) {
                    passwordNeeded =  true;
                }
            }

            if (passwordNeeded) {
                openPasswordDialog(sig);
            } else {
                verifyHash(sig);
            }
        } catch (SignatureSDKException e) {
            e.printStackTrace();
        }
    }

    private void verifyHash(Signature sig) throws SignatureSDKException {
        // first of all we check the integrity of the signature, to be sure that
        // it has not been tampered
        IntegrityStatus integrityStatus = sig.checkIntegrity(new Key(Key.Type.KeySHA256));
        if (integrityStatus == IntegrityStatus.OK) {
            File formFile = new File(Environment.getExternalStorageDirectory() + "/forms/" + formName);
            try {
                DataStatus formStatus = sig.checkSignedData(Hash.HashType.KeySHA256, FileUtils.readFile(formFile));
                if (formStatus == DataStatus.GOOD) {
                    openDialog("The signed form is valid");
                } else {
                    openDialog("The form has been changed since it was signed");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (integrityStatus == IntegrityStatus.MISSING) {
            openDialog("The signature has not integrity data. When saved as ISO, the signature has only coordinate data");
        } else {
            openDialog("The signature has been tampered");
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

    private void openPasswordDialog(final Signature sig) {
        final EditText passwordEditText = new EditText(this);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Password required")
                .setMessage("The signature is encrypted with password, please enter the password:")
                .setView(passwordEditText)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            sig.setEncryptionPassword(passwordEditText.getText().toString());
                            verifyHash(sig);
                        } catch (SignatureSDKException e) {
                            openDialog("Invalid password");
                        }
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .create();
        dialog.show();
    }

    private void openFile() {
        Intent openFile = new Intent(
                FileBrowserActivity.INTENT_ACTION_SELECT_FILE,
                null,
                this, FileBrowserActivity.class);
        startActivityForResult(openFile, OPEN_FILE_ACTION);
    }

    private void loadSignature(String filename) {
        try {
            byte[] signatureData = FileUtils.readFile(new File(filename));
            final Signature signature = new Signature(this);
            signature.setLincense(LICENSE);

            boolean loaded = false;

            // first we try to load the file as FSS
            try {
                signature.setSigData(signatureData);
                checkEncryption(signature, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            saveForm(signature);
                        } catch (SignatureSDKException e) {
                            e.printStackTrace();
                        }
                    }
                });

                loaded = true;
            } catch (SignatureSDKException e1) {
                e1.printStackTrace();
            }

            if (!loaded) {
                // try to load the file as Binary ISO
                try {
                    AdditionalImportIsoData additionalImportIsoData = new AdditionalImportIsoData();
                    additionalImportIsoData.setWho("User imported from Binary ISO");
                    additionalImportIsoData.setWhy("Signature imported from Binary ISO");
                    signature.importFromBinaryISO(signatureData, additionalImportIsoData);
                    saveForm(signature);
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
                    signature.importFromXmlISO(new String(signatureData), additionalImportIsoData);
                    saveForm(signature);
                    loaded = true;
                } catch (SignatureSDKException e3) {
                    e3.printStackTrace();
                }
            }

            if (!loaded) {
                openDialog("The imported signature has an unsupported format");
            }

        } catch (IOException | SignatureSDKException e) {
            openDialog(e.getMessage());
        }

    }

    private void saveForm(Signature signature) throws SignatureSDKException {

        String who = signature.getWho();
        String why = signature.getWhy();
        if ((who == null) || (who.isEmpty())) {
            who = "Default user";
            why = "Default reason";
        }
        generateFilledForm(who, why, "");


        // save the form as html
        File parent = new File(Environment.getExternalStorageDirectory()+"/forms");
        if (!parent.exists()) {
            parent.mkdir();
        }

        writeFile(parent.getAbsolutePath()+"/"+formName, form);

        SettingsPreferences prefs = SettingsPreferences.getInstance(this);
        int inkColor = ContextCompat.getColor(this, prefs.getInkColor());

        TypedValue typedValue = new TypedValue();
        getResources().getValue(prefs.getInkWidth(), typedValue, true);
        float inkWidth = typedValue.getFloat();

        int signatureFlags = Signature.RenderBackgroundTransparent | Signature.RenderColorARGB_8888 | Signature.RenderEncodeData;

        signature.renderBitmapToFile(parent.getAbsolutePath()+"/"+imageName,
                                     prefs.getImageWidth(), prefs.getImageHeight(), ImageType.PNG,
                                     inkWidth, inkColor, Color.WHITE, 0, 0, signatureFlags);

        // load the new form
        webView.loadUrl(Uri.fromFile(new File(parent.getAbsoluteFile()+"/"+formName)).toString());

    }

    private void checkEncryption(Signature signature, Runnable runnable) {
        boolean passwordNeeded = false;
        if (signature.isEncrypted()) {
            // in this example we have both encryption by password and certificate
            // in order to know which one it has been use we do this.
            // Note that this is only for teaching purposes, not using on real production apps
            try {
                signature.setPrivateKey(FormViewerActivity.readAsset(this, "private_key.pem"));
            } catch (SignatureSDKException e) {
                passwordNeeded =  true;
            }
        }

        if (passwordNeeded) {
            openPasswordDialog(signature, runnable);
        } else {
            runnable.run();
        }
    }

    private void openPasswordDialog(final Signature sig, final Runnable runnable) {
        final EditText passwordEditText = new EditText(this);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Password required")
                .setMessage("The signature is encrypted with password, please enter the password:")
                .setView(passwordEditText)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            sig.setEncryptionPassword(passwordEditText.getText().toString());
                            runnable.run();
                        } catch (SignatureSDKException e) {
                            openDialog("Invalid password");
                        }
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .create();
        dialog.show();
    }

    private class JavaScriptInferface {
        @JavascriptInterface
        public void signForm(final String name, final String reason, final String comments) {
            generateFilledForm(name, reason, comments);
            generateSignature(name, reason);
        }

        @JavascriptInterface
        public void verifyForm(final String imagePath) {
            Uri uri = Uri.parse(imagePath);
            verifyHash(uri.getPath());
        }

        @JavascriptInterface
        public void importSignature() {
            openFile();
        }
    }

}
