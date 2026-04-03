package de.robv.android.xposed;

public final class XposedHelpers {
    private XposedHelpers() {
    }

    public static XC_MethodHook.Unhook findAndHookMethod(
            Class<?> clazz,
            String methodName,
            Object... parameterTypesAndCallback
    ) {
        return new XC_MethodHook.Unhook();
    }
}
