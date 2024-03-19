package com.wacom.signaturesdkexample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;

import com.wacom.ink.willformat.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class TextEditorActivity extends Activity {

    public static final String FILE_PATH = "file_path";

    private EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.text_editor_layout);

        Intent intent = getIntent();
        final String formPath = intent.getStringExtra(FILE_PATH);

        editText = (EditText)findViewById(R.id.edit_canvas);

        ImageView saveBtn = (ImageView)findViewById(R.id.save_btn);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveFile(formPath);
            }
        });

        try {
            byte[] data = FileUtils.readFile(new File(formPath));
            String str = new String(data);
            editText.setText(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveFile(String filename) {
        try (FileOutputStream fos = new FileOutputStream(filename)) {
            fos.write(editText.getText().toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
