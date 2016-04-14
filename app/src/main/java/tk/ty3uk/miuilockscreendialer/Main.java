package tk.ty3uk.miuilockscreendialer;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.Context;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.XModuleResources;
import android.util.Property;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class Main implements IXposedHookZygoteInit, IXposedHookLoadPackage, IXposedHookInitPackageResources {
    private static String MODULE_PATH = null;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        MODULE_PATH = startupParam.modulePath;
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {
        if (!resparam.packageName.equals("com.android.keyguard"))
            return;

        XModuleResources modRes = XModuleResources.createInstance(MODULE_PATH, resparam.res);
        resparam.res.setReplacement("com.android.keyguard", "drawable", "remote_center_img", modRes.fwd(R.mipmap.remote_center_img));
        resparam.res.setReplacement("com.android.keyguard", "drawable", "remote_center_img_dark", modRes.fwd(R.mipmap.remote_center_img_dark));
    }

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("com.android.keyguard")) {
            try {
                final Class<?> MiuiDefaultLockScreen = XposedHelpers.findClass("com.android.keyguard.MiuiDefaultLockScreen", lpparam.classLoader);
                final Class<?> MiuiKeyguardUpdateMonitor = XposedHelpers.findClass("com.android.keyguard.MiuiKeyguardUpdateMonitor", lpparam.classLoader);
                final Class<?> MiuiKeyguardScreenCallback = XposedHelpers.findClass("com.android.keyguard.MiuiKeyguardScreenCallback", lpparam.classLoader);
                final Class<?> LockPatternUtils = XposedHelpers.findClass("com.android.internal.widget.LockPatternUtils", lpparam.classLoader);

                final XSharedPreferences pref = new XSharedPreferences("tk.ty3uk.miuilockscreendialer", "dialer");
                pref.makeWorldReadable();

                try {
                    XposedHelpers.findAndHookConstructor(MiuiDefaultLockScreen, Context.class, Configuration.class, LockPatternUtils, MiuiKeyguardUpdateMonitor, MiuiKeyguardScreenCallback, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            XposedHelpers.setBooleanField(param.thisObject, "mRemoteCenterAvailable", true);
                            Intent mRemoteCenterIntent = (Intent) XposedHelpers.getObjectField(param.thisObject, "mRemoteCenterIntent");
                            mRemoteCenterIntent.setComponent(
                                    new ComponentName(pref.getString("packageName", "com.android.contacts"),
                                            pref.getString("packageActivity", "com.android.contacts.activities.TwelveKeyDialer"))
                            );
                        }
                    });
                } catch (NoSuchMethodError e) {
                    XposedBridge.log("No such constructor: MiuiDefaultLockScreen");
                }

                try {
                    XposedHelpers.findAndHookMethod(MiuiDefaultLockScreen, "triggerStartRemoteCenterAction", new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            XposedBridge.log("com.android.keyguard.MiuiDefaultLockScreen.triggerStartRemoteCenterAction is called");

                            Object mKeyguardScreenCallback = XposedHelpers.getObjectField(param.thisObject, "mKeyguardScreenCallback");
                            XposedHelpers.callMethod(mKeyguardScreenCallback, "goToUnlockScreen");

                            XposedHelpers.setBooleanField(param.thisObject, "mStartingRemoteCenter", true);

                            Object localMainLockView = XposedHelpers.getObjectField(param.thisObject, "mLockView");
                            Property localProperty = View.TRANSLATION_X;

                            float[] arrayOfFloat = new float[2];
                            arrayOfFloat[0] = (Float) XposedHelpers.callMethod(localMainLockView, "getTranslationX");
                            arrayOfFloat[1] = (Integer) XposedHelpers.callMethod(param.thisObject, "getWidth");

                            ObjectAnimator localObjectAnimator = ObjectAnimator.ofFloat(localMainLockView, localProperty, arrayOfFloat);
                            localObjectAnimator.setDuration(100L);
                            localObjectAnimator.setInterpolator(new DecelerateInterpolator());
                            localObjectAnimator.start();

                            Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                            Intent mRemoteCenterIntent = (Intent) XposedHelpers.getObjectField(param.thisObject, "mRemoteCenterIntent");

                            mContext.startActivity(mRemoteCenterIntent);
                            return null;
                        }
                    });
                } catch (NoSuchMethodError e) {
                    XposedBridge.log("No such constructor: triggerStartRemoteCenterAction");
                }
            } catch (XposedHelpers.ClassNotFoundError e) {
                e.printStackTrace();
            }
        }
    }
}