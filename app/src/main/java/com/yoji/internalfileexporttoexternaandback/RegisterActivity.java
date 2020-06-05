package com.yoji.internalfileexporttoexternaandback;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class RegisterActivity extends AppCompatActivity {

    private EditText loginEdtTxt;
    private EditText passwordEdtTxt;
    private EditText confirmPasswordEdtTxt;
    private TextView passwordsNotEqualTxtView;
    private Button registerBtn;
    private Switch toExternalStorageSwitch;

    private File loginExtFile;
    private boolean initialSwitchState;
    private boolean switchStateChanged;

    private TextWatcher textWatcher = new TextWatcher() {
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

    private View.OnClickListener registerBtnOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            boolean toExternal = toExternalStorageSwitch.isChecked();
            String login = loginEdtTxt.getText().toString().trim();
            String password = passwordEdtTxt.getText().toString().trim();
            saveUser(login, password);
            Intent intent = new Intent();
            intent.putExtra(Key.RESULT, toExternal);
            setResult(RESULT_OK, intent);
            finish();
        }
    };

    private View.OnClickListener cancelBtnOnClickListener = v -> {
        setResult(RESULT_CANCELED);
        finish();
    };

    private CompoundButton.OnCheckedChangeListener toExternalStorageOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            switchStateChanged = initialSwitchState != toExternalStorageSwitch.isChecked();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();
        initialSwitchState = getIntent().getBooleanExtra(Key.TO_EXTERNAL, false);
        toExternalStorageSwitch.setChecked(initialSwitchState);
        loginExtFile = new File(this.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), TxtFileName.LOGIN);
    }

    private void initViews() {
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
    }

    private void saveUser(String login, String password) {
        if (toExternalStorageSwitch.isChecked()) {
            if (!loginExtFile.exists()){
                createExtFile();
                fillExternalFileFromInternal();
            }else {
                if(switchStateChanged){
                    clearExtFile();
                    fillExternalFileFromInternal();
                }
            }
            saveToExternalFile(login);
        } else {
            if (switchStateChanged){
                clearIntFile();
                fillInternalFileFromExternal();
            }
            saveToInternalFile(login, TxtFileName.LOGIN);
        }
        saveToInternalFile(password, TxtFileName.PASSWORD);
    }

    private void saveToInternalFile(String string, String fileName) {
        try {
            FileOutputStream fileOutputStream = openFileOutput(fileName, MODE_APPEND);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
            bufferedWriter.append(string).append("\n");
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveToExternalFile(String string) {
        try {
            FileWriter fileWriter = new FileWriter(loginExtFile, true);
            fileWriter.append(string).append("\n");
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void createExtFile() {
        try {
            loginExtFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fillInternalFileFromExternal() {
        try {
            FileInputStream loginIntFis = openFileInput(TxtFileName.LOGIN);
            InputStreamReader loginIsr = new InputStreamReader(loginIntFis);
            BufferedReader loginIntBr = new BufferedReader(loginIsr);
            BufferedReader loginExtBr = new BufferedReader(new FileReader(loginExtFile));
            String savedLoginExt;
            while ((savedLoginExt = loginExtBr.readLine()) != null) {
                String savedLoginInt = loginIntBr.readLine();
                if (!savedLoginExt.equals(savedLoginInt)) {
                    saveToInternalFile(savedLoginExt, TxtFileName.LOGIN);
                }
            }
            loginIntBr.close();
            loginExtBr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fillExternalFileFromInternal() {
        try {
            FileInputStream loginIntFis = openFileInput(TxtFileName.LOGIN);
            InputStreamReader loginIsr = new InputStreamReader(loginIntFis);
            BufferedReader loginIntBr = new BufferedReader(loginIsr);
            BufferedReader loginExtBr = new BufferedReader(new FileReader(loginExtFile));
            String savedLoginInt;
            while ((savedLoginInt = loginIntBr.readLine()) != null) {
                String savedLoginExt = loginExtBr.readLine();
                if (!savedLoginInt.equals(savedLoginExt)) {
                    saveToExternalFile(savedLoginInt);
                }
            }
            loginIntBr.close();
            loginExtBr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clearExtFile() {
        try {
            FileWriter fileWriter = new FileWriter(loginExtFile);
            fileWriter.write("");
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clearIntFile() {
        try {
            FileOutputStream fileOutputStream = openFileOutput(TxtFileName.LOGIN, MODE_PRIVATE);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
            bufferedWriter.write("");
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
