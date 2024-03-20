package com.wacom.signature.sdk.example;

import static com.wacom.signature.sdk.example.persistence.SettingsPreferences.EncryptionMethod.PASSWORD;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.TextView;

import com.wacom.ink.willformat.FileUtils;
import com.wacom.signature.sdk.AdditionalImportIsoData;
import com.wacom.signature.sdk.DataStatus;
import com.wacom.signature.sdk.EncryptionType;
import com.wacom.signature.sdk.EncryptionUtils;
import com.wacom.signature.sdk.Hash;
import com.wacom.signature.sdk.ISOMode;
import com.wacom.signature.sdk.ImageType;
import com.wacom.signature.sdk.IntegrityStatus;
import com.wacom.signature.sdk.Key;
import com.wacom.signature.sdk.Signature;
import com.wacom.signature.sdk.SignatureFormat;
import com.wacom.signature.sdk.activity.DynamicCaptureActivity;
import com.wacom.signature.sdk.example.persistence.SettingsPreferences;
import com.wacom.signature.sdk.example.utils.ILoadSignature;
import com.wacom.signature.sdk.example.utils.SignatureFormatInfo;
import com.wacom.signature.sdk.example.utils.SignatureUtils;
import com.wacom.signature.sdk.exception.SignatureSDKException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class FormViewerActivity extends Activity {

    public static final String FORM_PATH = "form_path";
    public static final String LICENSE = "PUT YOUR LICENCE STRING HERE";

    private static final float CAPTURE_WINDOW_MARGINS_PERCENT = 0.05f;
    private static final float MAX_PORTRAIT_WIDTH_INCH = 3.0f;
    private static final float MAX_PORTRAIT_HEIGHT_INCH = 3f;
    private static final float MAX_LANDSCAPE_WIDTH_INCH = 3.5f;
    private static final float MAX_LANDSCAPE_HEIGHT_INCH = 2.5f;
    private static final float BUTTONS_INCHES = 1f;

    private static final int DYNAMIC_CAPTURE_ACTION = 1;
    private static final int OPEN_FILE_ACTION = 2;
    private static final int GRANTED_READ_CODE = 44;

    private boolean captureSignatureWindowOpened;
    private String imageName;
    private String formName;
    private String signatureName;
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
            signatureName = formName.replace(".html", "");
        }

        webView = (WebView)findViewById(R.id.webView);
        webView.setWebChromeClient(new WebChromeClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
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
                            String sigDataString = data.getStringExtra(DynamicCaptureActivity.SIGNATURE_DATA);
                            byte[] sigDataBytes = data.getByteArrayExtra(DynamicCaptureActivity.SIGNATURE_DATA);
                            if (sigDataString != null) {
                                saveForm(sigDataString.getBytes());
                            }
                            else {
                                saveForm(sigDataBytes);
                            }

                        }
                    }
                }
            }
        } else if (requestCode == OPEN_FILE_ACTION) {
            if (resultCode == RESULT_OK) {
                try {
                    InputStream inputStream = getContentResolver().openInputStream(data.getData());
                    byte[] fileBytes = new byte[inputStream.available()];
                    inputStream.read(fileBytes);
                    inputStream.close();
                    loadSignature(fileBytes);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void pickFile() {
        int permissionCheck = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
        );
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                    GRANTED_READ_CODE
            );
            return;
        }
        launchPicker(OPEN_FILE_ACTION, Intent.ACTION_GET_CONTENT);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        launchPicker(OPEN_FILE_ACTION, Intent.ACTION_GET_CONTENT);
    }

    private void launchPicker(int action, String intentAction) {
        Intent intent = new Intent(intentAction);
        intent.setType("*/*");
        startActivityForResult(intent, action);
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
        Key key = new Key(Key.Type.KeySHA512);
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
        captureIntent.putExtra(DynamicCaptureActivity.TOUCH_ENABLE, true);
        captureIntent.putExtra(DynamicCaptureActivity.OUT_OF_WINDOW_LISTENER, new OutOfWindowListenerImp());

        switch (prefs.getSignatureFormat()) {
            case FSS:
                captureIntent.putExtra(DynamicCaptureActivity.SIGNATURE_FORMAT, SignatureFormat.FSS);
                break;
            case ISO_BINARY:
                captureIntent.putExtra(DynamicCaptureActivity.SIGNATURE_FORMAT, SignatureFormat.ISO_BINARY);
                break;
            case ISO_2014_BINARY:
                captureIntent.putExtra(DynamicCaptureActivity.SIGNATURE_FORMAT, SignatureFormat.ISO_2014_BINARY);
                break;
            case ISO_XML:
                captureIntent.putExtra(DynamicCaptureActivity.SIGNATURE_FORMAT, SignatureFormat.ISO_XML);
                break;
        }

        switch (prefs.getEncryptionMethod()) {
            case CERTIFICATE:
                captureIntent.putExtra(DynamicCaptureActivity.PUBLIC_KEY, SignatureUtils.readAsset(this, "public_key.pem"));
                break;
            case PASSWORD:
                captureIntent.putExtra(DynamicCaptureActivity.ENCRYPTION_PASSWORD, prefs.getEncryptionPassword());
                break;
        }

        Hash hash = new Hash(Hash.HashType.KeySHA512);
        //hash.add(form.getBytes());

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            hash.setHash(md.digest(form.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

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
            String rootName = name+"_"+System.currentTimeMillis();
            imageName = rootName + ".png";
            formName = rootName + ".html";
            signatureName = rootName;

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

    private void saveForm(final byte[] signatureData) {
        // save the form as html
        final File parent = new File(getFilesDir()+"/forms");
        if (!parent.exists()) {
            parent.mkdir();
        }

        final SettingsPreferences prefs = SettingsPreferences.getInstance(this);
        String password = prefs.getEncryptionPassword();
        SignatureUtils.loadSignature(this, LICENSE, signatureData, password, new ILoadSignature() {
            @Override
            public void onSignatureLoaded(SignatureFormatInfo signatureFormatInfo) {
                try {
                    // generate the signature image
                    int inkColor = ContextCompat.getColor(FormViewerActivity.this, prefs.getInkColor());

                    TypedValue typedValue = new TypedValue();
                    getResources().getValue(prefs.getInkWidth(), typedValue, true);
                    float inkWidth = typedValue.getFloat();

                    int signatureFlags = Signature.RenderBackgroundTransparent | Signature.RenderColorARGB_8888;// | Signature.RenderEncodeData;

                    signatureFormatInfo.getSignature().renderBitmapToFile(parent.getAbsolutePath()+"/"+imageName,
                            prefs.getImageWidth(), prefs.getImageHeight(), ImageType.PNG,
                            inkWidth, inkColor, Color.WHITE, 0, 0, signatureFlags);

                    writeFile(parent.getAbsolutePath()+"/"+formName, form);
                    //writeFile(parent.getAbsolutePath()+"/"+signatureName, new String(signatureData));

                    FileOutputStream fos = new FileOutputStream(new File(parent.getAbsolutePath()+"/"+signatureName));
                    fos.write(signatureData);
                    fos.close();

                    // load the new form
                    webView.loadUrl(Uri.fromFile(new File(parent.getAbsoluteFile()+"/"+formName)).toString());
                } catch (Exception e) {
                    openDialog("Error generating signature: " + e.getMessage());
                }
            }
        });
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
            sig.setLicense(LICENSE);

            File formFile = new File(imagePath.replace(".png", ""));
            ByteBuffer buffer = FileUtils.loadFile(formFile);
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            SignatureUtils.loadSignature(this, LICENSE, data, null, new ILoadSignature() {
                @Override
                public void onSignatureLoaded(SignatureFormatInfo signatureFormatInfo) {
                    try {
                        verifyHash(signatureFormatInfo.getSignature());
                    } catch (SignatureSDKException e) {
                        openDialog(e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            openDialog(e.getMessage());
        }
    }

    private void verifyHash(Signature sig) throws SignatureSDKException {
        // first of all we check the integrity of the signature, to be sure that
        // it has not been tampered
        IntegrityStatus integrityStatus = sig.checkIntegrity(new Key(Key.Type.KeySHA512));
        if (integrityStatus == IntegrityStatus.OK) {
            File formFile = new File(getFilesDir() + "/forms/" + formName);
            try {
                ByteBuffer buffer = FileUtils.loadFile(formFile);
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);
                DataStatus formStatus = sig.checkSignedData(Hash.HashType.KeySHA512, data);
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
        pickFile();
    }
    private void loadSignature(final byte[] signatureData) {
        SignatureUtils.loadSignature(this, LICENSE, signatureData, null, new ILoadSignature() {
            @Override
            public void onSignatureLoaded(SignatureFormatInfo signatureFormatInfo) {
                try {
                    saveForm(signatureFormatInfo.getSignature());
                } catch (SignatureSDKException e) {
                    openDialog(e.getMessage());
                }
            }
        });
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
        File parent = new File(getFilesDir()+"/forms");
        if (!parent.exists()) {
            parent.mkdir();
        }

        writeFile(parent.getAbsolutePath()+"/"+formName, form);

        SettingsPreferences prefs = SettingsPreferences.getInstance(this);
        int inkColor = ContextCompat.getColor(this, prefs.getInkColor());

        TypedValue typedValue = new TypedValue();
        getResources().getValue(prefs.getInkWidth(), typedValue, true);
        float inkWidth = typedValue.getFloat();

        int signatureFlags = Signature.RenderBackgroundTransparent | Signature.RenderColorARGB_8888; // | Signature.RenderEncodeData;

        signature.renderBitmapToFile(parent.getAbsolutePath()+"/"+imageName,
                                     prefs.getImageWidth(), prefs.getImageHeight(), ImageType.PNG,
                                     inkWidth, inkColor, Color.WHITE, 0, 0, signatureFlags);

        // load the new form
        webView.loadUrl(Uri.fromFile(new File(parent.getAbsoluteFile()+"/"+formName)).toString());

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
