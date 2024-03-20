package com.wacom.signature.sdk.example;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.wacom.signature.sdk.*;
import com.wacom.signature.sdk.activity.AboutSignatureSDKActivity;
import com.wacom.signature.sdk.example.persistence.SettingsPreferences;
import com.wacom.signature.sdk.example.utils.ILoadSignature;
import com.wacom.signature.sdk.example.utils.SignatureFormatInfo;
import com.wacom.signature.sdk.example.utils.SignatureUtils;
import com.wacom.signature.sdk.exception.SignatureSDKException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class FormListActivity extends Activity {

    private static final int SAVE_FILE_ACTION = 1;

    private FormArrayAdapter adapter;
    private List<String> formFiles = new ArrayList<String>();
    private byte[] exportData;
    private static int GRANTED_WRITE_CODE = 45;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.form_list_layout);

        Button newFormBtn = (Button) findViewById(R.id.new_form_btn);
        newFormBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                newForm();
            }
        });

        ImageButton settingsBtn = (ImageButton) findViewById(R.id.settings_btn);
        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openSettings();
            }
        });

        ImageButton aboutBtn = (ImageButton) findViewById(R.id.about_btn);
        aboutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openAboutScreen();
            }
        });

        ListView listView = (ListView) findViewById(R.id.listview);
        adapter = new FormArrayAdapter(this, R.layout.form_list_item_layout, formFiles);
        listView.setAdapter(adapter);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SAVE_FILE_ACTION) {
            if (resultCode == RESULT_OK) {
                try {
                    OutputStream outputStream = getContentResolver().openOutputStream(data.getData());
                    if (outputStream != null) {
                        outputStream.write(exportData);
                        outputStream.flush();
                        outputStream.close();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        String[] files = getFiles();
        if (files != null) {
            formFiles.clear();
            for (int i = 0; i < files.length; i++) {
                formFiles.add(files[i]);
            }
            adapter.notifyDataSetChanged();
        }
    }

    private class FormArrayAdapter extends ArrayAdapter<String> {

        private int mResource;
        private List<String> mFiles;

        public FormArrayAdapter(Context context, int resource, List<String> files) {
            super(context, resource, files);
            mResource = resource;
            mFiles = files;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(getContext()).inflate(mResource, parent, false);
            }
            TextView txtName = (TextView) view.findViewById(R.id.form_name_txt);
            txtName.setText(mFiles.get(position));
            txtName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openForm(mFiles.get(position));
                }
            });

            ImageButton exportBtn = (ImageButton) view.findViewById(R.id.export_signature);
            exportBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    exportSignature(mFiles.get(position));
                }
            });

            ImageButton deleteBtn = (ImageButton) view.findViewById(R.id.delete_form);
            deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    deleteForm(mFiles.get(position));
                }
            });

            ImageButton editBtn = (ImageButton) view.findViewById(R.id.edit_form);
            editBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    editForm(mFiles.get(position));
                }
            });

            return view;
        }
    }

    private String[] getFiles() {
        File root = new File(getFilesDir()+"/forms");
        return root.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".html");
            }
        });
    }

    private void openForm(String formName) {
        Intent intent = new Intent(this, FormViewerActivity.class);
        String formPath = Uri.fromFile(new File(getFilesDir()+"/forms/"+formName)).toString();
        intent.putExtra(FormViewerActivity.FORM_PATH, formPath);
        startActivity(intent);
    }

    private void editForm(String formName) {
        Intent intent = new Intent(this, TextEditorActivity.class);
        String formPath = new File(getFilesDir()+"/forms/"+formName).toString();
        intent.putExtra(TextEditorActivity.FILE_PATH, formPath);
        startActivity(intent);
    }

    private void deleteForm(final String formName) {
        new AlertDialog.Builder(this)
                .setTitle("Delete form")
                .setMessage("Do you really want to delete the form "+formName+"?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        File file = new File(getFilesDir()+"/forms/"+formName);
                        file.delete();
                        formFiles.remove(formName);
                        adapter.notifyDataSetChanged();
                    }})
                .setNegativeButton(android.R.string.no, null).show();
    }

    private void newForm() {
        Intent intent = new Intent(this, FormViewerActivity.class);
        startActivity(intent);
    }

    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void exportSignature(final String formName) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Export Signature");
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.export_dialog, null);
        dialogBuilder.setView(dialogView);

        final AlertDialog dialog = dialogBuilder.create();

        RadioButton currentFormat = dialogView.findViewById(R.id.signature_format_as_acquired);
        currentFormat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                exportAsAcquired(formName);
            }
        });

        RadioButton fssFormat = (RadioButton) dialogView.findViewById(R.id.signature_format_fss);
        fssFormat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                exportTo(SignatureFormat.FSS, formName);
            }
        });

        RadioButton isoFormat = (RadioButton) dialogView.findViewById(R.id.signature_format_iso_binary);
        isoFormat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                exportTo(SignatureFormat.ISO_BINARY, formName);
            }
        });

        RadioButton iso2014Format = (RadioButton) dialogView.findViewById(R.id.signature_format_iso_2014_binary);
        iso2014Format.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                exportTo(SignatureFormat.ISO_2014_BINARY, formName);
            }
        });

        RadioButton xmlFormat = (RadioButton) dialogView.findViewById(R.id.signature_format_iso_xml);
        xmlFormat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                exportTo(SignatureFormat.ISO_XML, formName);
            }
        });

        dialog.show();
    }

    private byte[] readSignatureData(String formName) {
        File file = new File(getFilesDir()+"/forms/"+formName.replace(".html", ""));
        int size = (int) file.length();
        byte[] bytes = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(bytes, 0, bytes.length);
            buf.close();

            return bytes;
        } catch (IOException e) {
            openDialog(e.getMessage());
        }

        return null;
    }

    private void exportAsAcquired(String formName) {
        byte[] signatureData = readSignatureData(formName);
        if (signatureData != null) {
            saveToFile(signatureData, ".fss");
        }
    }

    private void exportTo(final SignatureFormat signatureFormat, String formName) {
        byte[] signatureData = readSignatureData(formName);
        if (signatureData != null) {
            SignatureUtils.loadSignature(this, FormViewerActivity.LICENSE, signatureData, null, new ILoadSignature() {
                @Override
                public void onSignatureLoaded(SignatureFormatInfo signatureFormatInfo) {
                    try {
                        //reset encryption
                        signatureFormatInfo.getSignature().setEncryptionPassword(null);
                        signatureFormatInfo.getSignature().setPublicKey(null);
                        switch (signatureFormat) {
                            case FSS: {
                                byte[] fss = signatureFormatInfo.getSignature().getSigData();
                                saveToFile(fss, ".fss");
                            }
                            break;
                            case ISO_BINARY: {
                                byte[] iso = signatureFormatInfo.getSignature().exportISO(ISOMode.ISO19784_7_BINARY).getBinaryValue();
                                saveToFile(iso, ".bin");
                            }
                            break;
                            case ISO_2014_BINARY: {
                                byte[] iso = signatureFormatInfo.getSignature().exportISO(ISOMode.ISO19784_7_2014_BINARY).getBinaryValue();
                                saveToFile(iso, ".bin");
                            }
                            break;
                            case ISO_XML: {
                                String xml = signatureFormatInfo.getSignature().exportISO(ISOMode.ISO19785_3_XML).getStringValue();
                                saveToFile(xml.getBytes(), ".xml");
                            }
                            break;
                        }

                    } catch (SignatureSDKException e) {
                        openDialog(e.getMessage());
                    }
                }
            });
        }
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

    private void saveToFile(byte[] data, String extension) {
        exportData = data;
        int permissionCheck = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        );
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    GRANTED_WRITE_CODE
            );
            return;
        }
        launchPicker(SAVE_FILE_ACTION, Intent.ACTION_CREATE_DOCUMENT);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        launchPicker(SAVE_FILE_ACTION, Intent.ACTION_CREATE_DOCUMENT);
    }

    private void launchPicker(int action, String intentAction) {
        Intent intent = new Intent(intentAction);
        intent.setType("*/*");
        startActivityForResult(intent, action);
    }

    private void openAboutScreen() {
        Intent intent = new Intent(this, AboutSignatureSDKActivity.class);
        intent.putExtra(AboutSignatureSDKActivity.LICENSE, FormViewerActivity.LICENSE.getBytes());
        startActivity(intent);
    }


}
