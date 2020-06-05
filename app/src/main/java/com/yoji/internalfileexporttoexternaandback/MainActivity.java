package com.yoji.internalfileexporttoexternaandback;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    private EditText loginEdtTxt;
    private EditText passwordEdtTxt;
    private Button loginBtn;

    private SharedPreferences sharedPreferences;
    private boolean readFromExternalStorage;

    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String login = loginEdtTxt.getText().toString().trim();
            String password = passwordEdtTxt.getText().toString().trim();

            loginBtn.setEnabled(!login.isEmpty() && !password.isEmpty());
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    private View.OnClickListener loginBtnOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String login = loginEdtTxt.getText().toString().trim();
            String password = passwordEdtTxt.getText().toString().trim();
            enteredLoginAndPasswordCheck(login, password, readFromExternalStorage);
        }
    };

    private View.OnClickListener registerBtnOnClickListener = v -> {
        Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
        intent.putExtra(Key.TO_EXTERNAL, readFromExternalStorage);
        startActivityForResult(intent, RequestCode.TO_EXTERNAL_STORAGE);
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RequestCode.TO_EXTERNAL_STORAGE && resultCode == RESULT_OK){
            assert data != null;
            readFromExternalStorage = data.getBooleanExtra(Key.RESULT, false);
            Toast.makeText(this, R.string.toast_message_user_registered, Toast.LENGTH_SHORT).show();

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(Key.TO_EXTERNAL, readFromExternalStorage);
            editor.apply();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences("Save Mode", MODE_PRIVATE);
        readFromExternalStorage = sharedPreferences.getBoolean(Key.TO_EXTERNAL, false);
        initViews();
    }

    private void initViews(){
        loginEdtTxt = findViewById(R.id.loginEdtTxtId);
        passwordEdtTxt = findViewById(R.id.passwordEdtTxtId);
        loginBtn = findViewById(R.id.loginBtnId);
        Button registerBtn = findViewById(R.id.registerBtnId);

        loginEdtTxt.addTextChangedListener(textWatcher);
        passwordEdtTxt.addTextChangedListener(textWatcher);
        loginBtn.setOnClickListener(loginBtnOnClickListener);
        registerBtn.setOnClickListener(registerBtnOnClickListener);
    }

    private void enteredLoginAndPasswordCheck (String login, String password, boolean external){
        try {
            File loginExtFile = new File(this.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), TxtFileName.LOGIN);
            FileInputStream passwordFis = openFileInput(TxtFileName.PASSWORD);
            InputStreamReader passwordIsr = new InputStreamReader(passwordFis);
            BufferedReader passwordBr = new BufferedReader(passwordIsr);
            BufferedReader loginBr;
            if (external && loginExtFile.exists()){
                loginBr = loginBrExt(loginExtFile);
            }else {
                loginBr = loginBrInt();
            }
            String savedLogin;
            String savedPassword;
            assert loginBr != null;
            while ((savedLogin = loginBr.readLine()) != null &&
                    (savedPassword = passwordBr.readLine()) != null){
                if (login.equals(savedLogin)){
                    if (password.equals(savedPassword)){
                        Toast.makeText(this, getString(R.string.toast_message_logged_in)
                                + login, Toast.LENGTH_LONG).show();
                        passwordEdtTxt.setText("");
                        loginEdtTxt.setText("");
                    }else {
                        Toast.makeText(this, getString(R.string.toast_message_wrong_password),
                                Toast.LENGTH_SHORT).show();
                        passwordEdtTxt.setText("");
                    }
                    loginBr.close();
                    passwordBr.close();
                    return;
                }
            }
            passwordEdtTxt.setText("");
            loginEdtTxt.setText("");
            loginBr.close();
            passwordBr.close();
            Toast.makeText(this, getString(R.string.toast_message_user_not_found),
                    Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            passwordEdtTxt.setText("");
            loginEdtTxt.setText("");
            Toast.makeText(this, R.string.toast_message_user_not_found, Toast.LENGTH_SHORT).show();
        }
    }

    private BufferedReader loginBrInt(){
        try {
            FileInputStream loginFis = openFileInput(TxtFileName.LOGIN);
            InputStreamReader loginIsr = new InputStreamReader(loginFis);
            return new BufferedReader(loginIsr);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private BufferedReader loginBrExt(File loginExtFile){
        try {
            return new BufferedReader(new FileReader(loginExtFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}