package com.godpalace.data.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface LocalDatabase {
    String path();

    long autoSaveTime() default 0;
    TimeUnit autoSaveTimeUnit() default TimeUnit.SECONDS;
}
