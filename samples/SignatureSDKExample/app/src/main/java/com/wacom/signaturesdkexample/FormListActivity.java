package com.wacom.signaturesdkexample;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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

import com.wacom.signature.sdk.Signature;
import com.wacom.signature.sdk.activity.AboutSignatureSDKActivity;
import com.wacom.signature.sdk.exception.SignatureSDKException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import ua.com.vassiliev.androidfilebrowser.FileBrowserActivity;

public class FormListActivity extends Activity {

    private static final int SAVE_FILE_ACTION = 1;

    private FormArrayAdapter adapter;
    private List<String> formFiles = new ArrayList<String>();
    private byte[] exportData;
    private String fileExtension;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.form_list_layout);

        // this is only an example of use of the Wacom Signature SDK, so we don't bother about
        // permissions, this should be handle properly in real apps
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

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

        // using the function Arrays.asList does not allow to remove elements
        //formFiles = Arrays.asList(getFiles());

        adapter = new FormArrayAdapter(this, R.layout.form_list_item_layout, formFiles);
        listView.setAdapter(adapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SAVE_FILE_ACTION) {
            if (resultCode == RESULT_OK) {
                String dirname = data.getStringExtra(FileBrowserActivity.returnDirectoryParameter);
                openFilenameDialog(dirname);
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
        File root = new File(Environment.getExternalStorageDirectory()+"/forms");
        return root.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".html");
            }
        });
    }

    private void openForm(String formName) {
        Intent intent = new Intent(this, FormViewerActivity.class);
        String formPath = Uri.fromFile(new File(Environment.getExternalStorageDirectory()+"/forms/"+formName)).toString();
        intent.putExtra(FormViewerActivity.FORM_PATH, formPath);
        startActivity(intent);
    }

    private void editForm(String formName) {
        Intent intent = new Intent(this, TextEditorActivity.class);
        String formPath = new File(Environment.getExternalStorageDirectory()+"/forms/"+formName).toString();
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
                        File file = new File(Environment.getExternalStorageDirectory()+"/forms/"+formName);
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

        RadioButton fssFormat = (RadioButton) dialogView.findViewById(R.id.signature_format_fss);
        fssFormat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                exportToFSS(formName);
            }
        });

        RadioButton isoFormat = (RadioButton) dialogView.findViewById(R.id.signature_format_iso_binary);
        isoFormat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                exportToISOBinary(formName);
            }
        });

        RadioButton xmlFormat = (RadioButton) dialogView.findViewById(R.id.signature_format_iso_xml);
        xmlFormat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                exportToISOXml(formName);
            }
        });

        dialog.show();
    }

    private void exportToFSS(String formName) {
        File file = new File(Environment.getExternalStorageDirectory()+"/forms/"+formName.replace(".html", ".png"));
        final Signature signature = new Signature(this);
        try {
            signature.setLincense(FormViewerActivity.LICENSE);
            signature.readEncodedBitmapFile(file.getAbsolutePath());
            checkEncryption(signature, new Runnable() {
                @Override
                public void run() {
                    try {
                        byte[] fss = signature.getSigData();
                        saveToFile(fss, ".fss");
                    } catch (SignatureSDKException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (SignatureSDKException e) {
            openDialog(e.getMessage());
        }
    }

    private void exportToISOBinary(String formName) {
        File file = new File(Environment.getExternalStorageDirectory()+"/forms/"+formName.replace(".html", ".png"));
        final Signature signature = new Signature(this);
        try {
            signature.setLincense(FormViewerActivity.LICENSE);
            signature.readEncodedBitmapFile(file.getAbsolutePath());
            checkEncryption(signature, new Runnable() {
                @Override
                public void run() {
                    try {
                        byte[] iso = signature.exportToBinaryISO();
                        saveToFile(iso, ".bin");
                    } catch (SignatureSDKException e) {
                        openDialog(e.getMessage());
                    }
                }
            });
        } catch (SignatureSDKException e) {
            openDialog(e.getMessage());
        }
    }

    private void exportToISOXml(String formName) {
        File file = new File(Environment.getExternalStorageDirectory()+"/forms/"+formName.replace(".html", ".png"));
        final Signature signature = new Signature(this);
        try {
            signature.setLincense(FormViewerActivity.LICENSE);
            signature.readEncodedBitmapFile(file.getAbsolutePath());
            checkEncryption(signature, new Runnable() {
                @Override
                public void run() {
                    try {
                        String xml = signature.exportToXmlISO();
                        saveToFile(xml.getBytes(), ".xml");
                    } catch (SignatureSDKException e) {
                        openDialog(e.getMessage());
                    }
                }
            });
        } catch (SignatureSDKException e) {
            openDialog(e.getMessage());
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
        fileExtension = extension;
        Intent openDir = new Intent(FileBrowserActivity.INTENT_ACTION_SELECT_DIR,
                                    null, this, FileBrowserActivity.class);
        startActivityForResult(openDir, SAVE_FILE_ACTION);
    }

    private void openFilenameDialog(final String dirname) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("File name");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                saveSignature(dirname, input.getText().toString());
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void saveSignature(String dirname, String filename) {
        if ((filename != null) && (!filename.isEmpty())) {
            File file = new File(dirname, filename+fileExtension);
            try {
                FileOutputStream fOut = new FileOutputStream(file);
                fOut.write(exportData);
                fOut.flush();
                fOut.close();
                openDialog("Signature exported to file");
            } catch (Exception e) {
                openDialog(e.toString());
            }
        }
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

    private void openAboutScreen() {
        Intent intent = new Intent(this, AboutSignatureSDKActivity.class);
        intent.putExtra(AboutSignatureSDKActivity.LICENSE, FormViewerActivity.LICENSE.getBytes());
        startActivity(intent);
    }
}
