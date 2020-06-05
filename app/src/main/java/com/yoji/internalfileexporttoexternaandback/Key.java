package com.yoji.internalfileexporttoexternaandback;

import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@StringDef({Key.TO_EXTERNAL, Key.RESULT})
public @interface Key {
    String TO_EXTERNAL = "to_external_key";
    String RESULT = "result";
}
