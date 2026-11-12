package io.dsh.padmask;

import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/** Layer 2: answer ro.* property reads from Java with tablet values. */
public final class PropSpoofer {

    private final Profile p;
    private final Map<String, String> overrides = new HashMap<>();

    public PropSpoofer(Profile p) {
        this.p = p;
        overrides.put("ro.product.model", p.model);
        overrides.put("ro.product.brand", p.brand);
        overrides.put("ro.product.manufacturer", p.manufacturer);
        overrides.put("ro.product.device", p.device);
        overrides.put("ro.product.name", p.product);
        overrides.put("ro.build.characteristics", p.characteristics);
        if (p.fingerprint != null) {
            overrides.put("ro.build.fingerprint", p.fingerprint);
        }
    }

    public void apply(ClassLoader cl) throws Throwable {
        if (!p.spoofSystemProps) return;
        final XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.args == null || param.args.length == 0) return;
                Object key = param.args[0];
                if (!(key instanceof String)) return;
                String v = overrides.get((String) key);
                if (v != null) param.setResult(v);
            }
        };
        XposedHelpers.findAndHookMethod("android.os.SystemProperties", cl, "get", String.class, hook);
        XposedHelpers.findAndHookMethod("android.os.SystemProperties", cl, "get",
                String.class, String.class, hook);
    }
}
