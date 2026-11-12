package io.dsh.padmask;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Entry point. Runs inside every process; only target apps get hooked.
 * Each layer is installed independently and its failure never blocks the others.
 */
public class Entry implements IXposedHookLoadPackage {

    private static final String TAG = "PadMask";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) throws Throwable {
        if (lp == null || lp.packageName == null || lp.classLoader == null) return;
        if (!Profile.isTarget(lp.packageName)) return;

        Profile profile = Profile.forPackage(lp.packageName);
        XposedBridge.log(TAG + ": loading into " + lp.packageName);

        safeInstall(TAG, "build", () -> new BuildSpoofer(profile).apply(lp.classLoader));
        safeInstall(TAG, "props", () -> new PropSpoofer(profile).apply(lp.classLoader));
        safeInstall(TAG, "config", () -> new DisplaySpoofer(profile).apply(lp.classLoader));
    }

    private static void safeInstall(String tag, String layer, Installer r) {
        try {
            r.install();
            XposedBridge.log(TAG + ": layer ok -> " + layer);
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": layer failed -> " + layer);
            XposedBridge.log(t);
        }
    }

    private interface Installer {
        void install() throws Throwable;
    }
}
