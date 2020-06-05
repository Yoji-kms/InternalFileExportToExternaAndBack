package com.yoji.internalfileexporttoexternaandback;

import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@StringDef({TxtFileName.LOGIN, TxtFileName.PASSWORD})
public @interface TxtFileName {
    String PASSWORD = "password.txt";
    String LOGIN = "login.txt";
}
