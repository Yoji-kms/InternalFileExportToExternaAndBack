package com.yoji.internalfileexporttoexternaandback;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    private EditText loginEdtTxt;
    private EditText passwordEdtTxt;
    private EditText confirmPasswordEdtTxt;
    private TextView passwordsNotEqualTxtView;
    private Button registerBtn;
    private Switch toExternalStorageSwitch;

    private boolean initialSwitchState;
    private boolean switchStateChanged;
    private SharedPreferences sharedPreferences;
    private String path;
    private String loginToSave;
    private Uri loginExtUri;
    private boolean uriExists;
    private final int CREATE_EXTERNAL_TXT_FILE = 11;


    private final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String login = loginEdtTxt.getText().toString().trim();
            String password = passwordEdtTxt.getText().toString().trim();
            String passwordConfirmation = confirmPasswordEdtTxt.getText().toString().trim();

            passwordsNotEqualTxtView.setVisibility(password.equals(passwordConfirmation) ? View.GONE : View.VISIBLE);
            registerBtn.setEnabled(!login.isEmpty() && !password.isEmpty() && password.equals(passwordConfirmation));
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    private final View.OnClickListener registerBtnOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            loginToSave = loginEdtTxt.getText().toString().trim();
            String password = passwordEdtTxt.getText().toString().trim();
            saveUser(password);
        }
    };

    private final View.OnClickListener cancelBtnOnClickListener = v -> {
        setResult(RESULT_CANCELED);
        finish();
    };

    private final CompoundButton.OnCheckedChangeListener toExternalStorageOnCheckedChangeListener =
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    switchStateChanged = initialSwitchState != toExternalStorageSwitch.isChecked();
                }
            };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        init();
        initialSwitchState = getIntent().getBooleanExtra(Key.TO_EXTERNAL, false);
        toExternalStorageSwitch.setChecked(initialSwitchState);
        path = sharedPreferences.getString(Key.URI_PATH, "");
        uriExists = !path.equals("");
        loginExtUri = Uri.parse(path);
    }

    private void init() {
        loginEdtTxt = findViewById(R.id.setLoginEdtTxtId);
        passwordEdtTxt = findViewById(R.id.setPasswordEdtTxtId);
        confirmPasswordEdtTxt = findViewById(R.id.confirmPasswordEdtTxtId);
        passwordsNotEqualTxtView = findViewById(R.id.passwordsNotEqualTxtViewId);
        registerBtn = findViewById(R.id.registerUserBtnId);
        Button cancelBtn = findViewById(R.id.cancelBtnId);
        toExternalStorageSwitch = findViewById(R.id.saveLoginToExternalStorageSwitchId);

        loginEdtTxt.addTextChangedListener(textWatcher);
        passwordEdtTxt.addTextChangedListener(textWatcher);
        confirmPasswordEdtTxt.addTextChangedListener(textWatcher);
        registerBtn.setOnClickListener(registerBtnOnClickListener);
        cancelBtn.setOnClickListener(cancelBtnOnClickListener);
        toExternalStorageSwitch.setOnCheckedChangeListener(toExternalStorageOnCheckedChangeListener);

        sharedPreferences = getSharedPreferences("Uri", MODE_PRIVATE);
    }

    private void saveUser(String password) {
        if (toExternalStorageSwitch.isChecked()) {
            if (!uriFileExists(loginExtUri)) {
                createExtFile();
                saveToInternalFile(password, TxtFileName.PASSWORD);
                return;
            } else {
                if (switchStateChanged) {
                    clearExtFile(loginExtUri);
                    fillExternalFileFromInternal(Uri.parse(path));
                }
            }
            saveToExternalFile(loginToSave, loginExtUri);
        } else {
            if (switchStateChanged) {
                clearIntFile();
                fillInternalFileFromExternal(loginExtUri);
            }
            saveToInternalFile(loginToSave, TxtFileName.LOGIN);
        }
        saveToInternalFile(password, TxtFileName.PASSWORD);
        returnResult();
    }

    private void saveToInternalFile(String string, String fileName) {
        try (FileOutputStream fileOutputStream = openFileOutput(fileName, MODE_APPEND);
             OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
             BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter)) {
            bufferedWriter.append(string).append("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveToExternalFile(String string, Uri uri) {
        try (ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "wa");
             FileOutputStream fos = new FileOutputStream(Objects.requireNonNull(pfd).getFileDescriptor())) {
            fos.write((string + "\n").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createExtFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, TxtFileName.LOGIN);
        startActivityForResult(intent, CREATE_EXTERNAL_TXT_FILE);
    }

    private void fillInternalFileFromExternal(Uri uri) {
        try (FileInputStream loginIntFis = openFileInput(TxtFileName.LOGIN);
             InputStreamReader loginIsr = new InputStreamReader(loginIntFis);
             BufferedReader loginIntBr = new BufferedReader(loginIsr);
             InputStream loginExtIs = getContentResolver().openInputStream(uri)) {
            String[] savedLoginExt = IOUtils.toString(Objects.requireNonNull(loginExtIs),
                    StandardCharsets.UTF_8).split("\n");
            for (String loginExt : savedLoginExt) {
                String savedLoginInt = loginIntBr.readLine();
                if (!loginExt.equals(savedLoginInt)) {
                    saveToInternalFile(loginExt, TxtFileName.LOGIN);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fillExternalFileFromInternal(Uri uri) {
        try (InputStream loginExtIs = getContentResolver().openInputStream(uri);
             FileInputStream loginIntFis = openFileInput(TxtFileName.LOGIN);
             InputStreamReader loginIsr = new InputStreamReader(loginIntFis);
             BufferedReader loginIntBr = new BufferedReader(loginIsr)) {
            String savedLoginInt;
            int i = 0;
            String[] savedLoginExt = IOUtils.toString(Objects.requireNonNull(loginExtIs),
                    StandardCharsets.UTF_8).split("\n");
            while ((savedLoginInt = loginIntBr.readLine()) != null) {
                if (i < savedLoginExt.length) {
                    if (!savedLoginInt.equals(savedLoginExt[i++])) {
                        saveToExternalFile(savedLoginInt, uri);
                    }
                } else {
                    saveToExternalFile(savedLoginInt, uri);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clearExtFile(Uri uri) {
        try (ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w");
             FileOutputStream fos = new FileOutputStream(Objects.requireNonNull(pfd).getFileDescriptor())) {
            fos.write(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clearIntFile() {
        try (FileOutputStream fileOutputStream = openFileOutput(TxtFileName.LOGIN, MODE_PRIVATE);
             OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
             BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter)) {
            bufferedWriter.write("");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CREATE_EXTERNAL_TXT_FILE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();

                path = Objects.requireNonNull(uri).toString();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(Key.URI_PATH, path);
                editor.apply();

                loginExtUri = uri;
                fillExternalFileFromInternal(uri);
                saveToExternalFile(loginToSave, uri);
                returnResult();
            }
        }
    }

    private void returnResult() {
        boolean toExternal = toExternalStorageSwitch.isChecked();
        Intent intent = new Intent();
        intent.putExtra(Key.RESULT, toExternal);
        intent.putExtra(Key.URI_PATH, path);
        setResult(RESULT_OK, intent);
        finish();
    }


    private boolean uriFileExists(Uri uri) {
        if (uriExists) {
            try (ParcelFileDescriptor pfd = getContentResolver().
                    openFileDescriptor(uri, "wa");
                 FileOutputStream ignored = new FileOutputStream
                         (Objects.requireNonNull(pfd).getFileDescriptor())) {
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        } else return false;
    }
}
