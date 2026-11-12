package io.dsh.padmask;

/**
 * Central profile: every spoofed value and every layer switch lives here.
 * Nothing is written to the system; it only affects the target app's process.
 */
public final class Profile {

    public static final String PKG_WECHAT = "com.tencent.mm";
    public static final String PKG_QQ = "com.tencent.mobileqq";

    /* ---------------- layer switches ---------------- */
    public boolean spoofBuildProps = true;   // Build.MODEL / BRAND / ...
    public boolean spoofSystemProps = true;  // SystemProperties.get("ro.*")
    public boolean spoofConfiguration = true;// Resources Configuration (sw dp / size class)
    public boolean spoofDisplay = true;      // Display / WindowMetrics pixel size

    /* ---------------- tablet identity ---------------- */
    public String model = "Lenovo TB-9707F";
    public String brand = "Lenovo";
    public String manufacturer = "LENOVO";
    public String device = "TB-9707F";
    public String product = "TB-9707F";
    public String characteristics = "tablet";
    /** null = leave the real fingerprint untouched (safer). */
    public String fingerprint = null;

    /* ---------------- display shape ---------------- */
    public int smallestScreenWidthDp = 900;
    public int screenWidthDp = 1600;
    public int screenHeightDp = 1000;
    public int widthPixels = 2560;
    public int heightPixels = 1600;

    private Profile() { }

    public static boolean isTarget(String pkg) {
        return PKG_WECHAT.equals(pkg) || PKG_QQ.equals(pkg);
    }

    public static Profile forPackage(String pkg) {
        Profile p = new Profile();
        if (PKG_QQ.equals(pkg)) {
            // QQ 目前只做屏幕/配置伪装，不动机型，先观察效果。
            p.spoofBuildProps = false;
            p.spoofSystemProps = false;
        }
        return p;
    }
}
