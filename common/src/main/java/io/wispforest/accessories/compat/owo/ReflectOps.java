package io.wispforest.accessories.compat.owo;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

class ReflectOps {
    public static <F> void set(Field field, Object t, F f) {
        try {
            field.set(t, f);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static <F> F get(Field field, Object t) {
        try {
            return (F) field.get(t);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T defaultConstruct(Class<T> clazz) {
        try {
            return (T) clazz.getConstructors()[0].newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
