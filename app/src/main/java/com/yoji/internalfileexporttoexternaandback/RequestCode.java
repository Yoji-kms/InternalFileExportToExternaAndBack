package com.yoji.internalfileexporttoexternaandback;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({RequestCode.TO_EXTERNAL_STORAGE})
public @interface RequestCode {
    int TO_EXTERNAL_STORAGE = 10;
}
