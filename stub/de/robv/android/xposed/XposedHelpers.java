package de.robv.android.xposed;

/** Compile-time stub only. */
public final class XposedHelpers {
    public static Object findAndHookMethod(String className, ClassLoader classLoader,
            String methodName, Object... parameterTypesAndCallback) { return null; }
    public static Object findAndHookMethod(Class<?> clazz, String methodName,
            Object... parameterTypesAndCallback) { return null; }
    public static void setStaticObjectField(Class<?> clazz, String fieldName, Object value) { }
    public static void setObjectField(Object obj, String fieldName, Object value) { }
    public static Object getObjectField(Object obj, String fieldName) { return null; }
}
