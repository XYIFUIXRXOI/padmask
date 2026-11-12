package io.dsh.padmask;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.DisplayMetrics;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * Layer 3: make the target process believe it renders on a large-screen device.
 * Configuration + Display + WindowMetrics are all answered from the profile.
 */
public final class DisplaySpoofer {

    private final Profile p;

    public DisplaySpoofer(Profile p) {
        this.p = p;
    }

    public void apply(ClassLoader cl) throws Throwable {
        if (p.spoofConfiguration) {
            hookConfiguration(cl);
        }
        if (p.spoofDisplay) {
            hookDisplay(cl);
            hookWindowMetrics(cl);
        }
    }

    private void hookConfiguration(ClassLoader cl) throws Throwable {
        XposedHelpers.findAndHookMethod(Resources.class, "getConfiguration", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Object r = param.getResult();
                if (!(r instanceof Configuration)) return;
                Configuration c = new Configuration((Configuration) r);
                c.smallestScreenWidthDp = p.smallestScreenWidthDp;
                c.screenWidthDp = p.screenWidthDp;
                c.screenHeightDp = p.screenHeightDp;
                c.screenLayout = (c.screenLayout & ~Configuration.SCREENLAYOUT_SIZE_MASK)
                        | Configuration.SCREENLAYOUT_SIZE_XLARGE;
                param.setResult(c);
            }
        });

        XposedHelpers.findAndHookMethod(Resources.class, "getDisplayMetrics", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Object r = param.getResult();
                if (!(r instanceof DisplayMetrics)) return;
                DisplayMetrics m = new DisplayMetrics();
                m.setTo((DisplayMetrics) r);
                m.widthPixels = p.widthPixels;
                m.heightPixels = p.heightPixels;
                param.setResult(m);
            }
        });
    }

    private void hookDisplay(ClassLoader cl) throws Throwable {
        XC_MethodHook metricsHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (param.args == null || param.args.length == 0) return;
                Object a = param.args[0];
                if (a instanceof Point) {
                    ((Point) a).x = p.widthPixels;
                    ((Point) a).y = p.heightPixels;
                } else if (a instanceof DisplayMetrics) {
                    DisplayMetrics m = (DisplayMetrics) a;
                    m.widthPixels = p.widthPixels;
                    m.heightPixels = p.heightPixels;
                }
            }
        };
        XposedHelpers.findAndHookMethod("android.view.Display", cl, "getSize", Point.class, metricsHook);
        XposedHelpers.findAndHookMethod("android.view.Display", cl, "getRealSize", Point.class, metricsHook);
        XposedHelpers.findAndHookMethod("android.view.Display", cl, "getMetrics", DisplayMetrics.class, metricsHook);
        XposedHelpers.findAndHookMethod("android.view.Display", cl, "getRealMetrics", DisplayMetrics.class, metricsHook);
    }

    private void hookWindowMetrics(ClassLoader cl) {
        try {
            XposedHelpers.findAndHookMethod("android.view.WindowMetrics", cl, "getBounds",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            param.setResult(new Rect(0, 0, p.widthPixels, p.heightPixels));
                        }
                    });
        } catch (Throwable ignored) {
            // API < 30 or method absent: display hooks still cover it.
        }
    }
}
