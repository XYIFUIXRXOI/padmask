package io.dsh.padmask;

import android.os.Build;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/** Layer 1: rewrite Build.* constants inside the target process only. */
public final class BuildSpoofer {

    private final Profile p;

    public BuildSpoofer(Profile p) {
        this.p = p;
    }

    public void apply(ClassLoader cl) throws Throwable {
        if (!p.spoofBuildProps) return;
        Class<?> build = Class.forName("android.os.Build", false, cl);
        set(build, "MODEL", p.model);
        set(build, "BRAND", p.brand);
        set(build, "MANUFACTURER", p.manufacturer);
        set(build, "DEVICE", p.device);
        set(build, "PRODUCT", p.product);
        if (p.fingerprint != null) {
            set(build, "FINGERPRINT", p.fingerprint);
        }
    }

    private void set(Class<?> build, String field, String value) {
        if (value == null) return;
        try {
            XposedHelpers.setStaticObjectField(build, field, value);
        } catch (Throwable t) {
            XposedBridge.log("PadMask: Build." + field + " failed: " + t);
        }
    }
}
