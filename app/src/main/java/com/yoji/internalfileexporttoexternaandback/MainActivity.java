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
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private EditText loginEdtTxt;
    private EditText passwordEdtTxt;
    private Button loginBtn;

    private SharedPreferences sharedPreferences;
    private boolean readFromExternalStorage;
    private String path;
    private Uri loginExtUri;
    private boolean uriExists;
    private final int REGISTER = 10;

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
            if (readFromExternalStorage && uriFileExists(loginExtUri)){
                enteredLoginAndPasswordCheckExt(login, password);
            }else{
                enteredLoginAndPasswordCheckInt(login, password);
            }
        }
    };

    private View.OnClickListener registerBtnOnClickListener = v -> {
        Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
        intent.putExtra(Key.TO_EXTERNAL, readFromExternalStorage);
        startActivityForResult(intent, REGISTER);
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REGISTER && resultCode == RESULT_OK){
            readFromExternalStorage = Objects.requireNonNull(data).getBooleanExtra(Key.RESULT, false);
            path = Objects.requireNonNull(data).getStringExtra(Key.URI_PATH);
            uriExists = !Objects.requireNonNull(path).equals("");
            Toast.makeText(this, R.string.toast_message_user_registered, Toast.LENGTH_SHORT).show();

            loginExtUri = Uri.parse(path);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(Key.TO_EXTERNAL, readFromExternalStorage);
            editor.putString(Key.URI_PATH, path);
            editor.apply();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences("Save Mode", MODE_PRIVATE);
        readFromExternalStorage = sharedPreferences.getBoolean(Key.TO_EXTERNAL, false);
        path = sharedPreferences.getString(Key.URI_PATH, "");
        uriExists = !path.equals("");
        loginExtUri = Uri.parse(path);
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

    private void enteredLoginAndPasswordCheckInt (String login, String password){
        try (FileInputStream passwordFis = openFileInput(TxtFileName.PASSWORD);
            InputStreamReader passwordIsr = new InputStreamReader(passwordFis);
            FileInputStream loginFis = openFileInput(TxtFileName.LOGIN);
            InputStreamReader loginIsr = new InputStreamReader(loginFis);
            BufferedReader passwordBr = new BufferedReader(passwordIsr);
            BufferedReader loginBr = new BufferedReader(loginIsr)){
            String savedLogin;
            String savedPassword;
            while ((savedLogin = Objects.requireNonNull(loginBr).readLine()) != null &&
                    (savedPassword = Objects.requireNonNull(passwordBr).readLine()) != null){
                if (login.equals(savedLogin)){
                    if (password.equals(savedPassword)){
                        Toast.makeText(this, getString(R.string.toast_message_logged_in)
                                + login, Toast.LENGTH_LONG).show();
                        clearEdtTxtForms();
                    }else {
                        Toast.makeText(this, getString(R.string.toast_message_wrong_password),
                                Toast.LENGTH_SHORT).show();
                        passwordEdtTxt.setText("");
                    }
                    return;
                }
            }
            clearEdtTxtForms();
            Toast.makeText(this, getString(R.string.toast_message_user_not_found),
                    Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            clearEdtTxtForms();
            Toast.makeText(this, R.string.toast_message_user_not_found, Toast.LENGTH_SHORT).show();
        }
    }

    private void enteredLoginAndPasswordCheckExt (String login, String password){
        try (FileInputStream passwordFis = openFileInput(TxtFileName.PASSWORD);
             InputStreamReader passwordIsr = new InputStreamReader(passwordFis);
             BufferedReader passwordBr = new BufferedReader(passwordIsr);
             InputStream loginExtIs = getContentResolver().openInputStream(loginExtUri)){
            String[] savedLoginExt = IOUtils.toString(Objects.requireNonNull(loginExtIs),
                    StandardCharsets.UTF_8).split("\n");
            String savedPassword;
            for (String savedLogin : savedLoginExt){
                savedPassword = Objects.requireNonNull(passwordBr).readLine();
                if (login.equals(savedLogin)){
                    if (password.equals(savedPassword)){
                        Toast.makeText(this, getString(R.string.toast_message_logged_in)
                                + login, Toast.LENGTH_LONG).show();
                        clearEdtTxtForms();
                    }else {
                        Toast.makeText(this, getString(R.string.toast_message_wrong_password),
                                Toast.LENGTH_SHORT).show();
                        passwordEdtTxt.setText("");
                    }
                    return;
                }
            }
            clearEdtTxtForms();
            Toast.makeText(this, getString(R.string.toast_message_user_not_found),
                    Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            clearEdtTxtForms();
            Toast.makeText(this, R.string.toast_message_user_not_found, Toast.LENGTH_SHORT).show();
        }
    }

    private void clearEdtTxtForms(){
        passwordEdtTxt.setText("");
        loginEdtTxt.setText("");
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
        }else return false;
    }
}