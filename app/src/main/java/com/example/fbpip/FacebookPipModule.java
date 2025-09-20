package com.example.fbpip;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import android.app.Activity;
import android.os.Build;
import android.util.Log;

public class FacebookPipModule implements IXposedHookLoadPackage {

    private static final String TAG = "FBPiP";

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // Target Facebook official app
        if (!"com.facebook.katana".equals(lpparam.packageName)) return;

        Log.i(TAG, "Facebook package loaded: " + lpparam.packageName);

        // Hook Activity.onUserLeaveHint to request PiP
        try {
            XposedHelpers.findAndHookMethod(Activity.class, "onUserLeaveHint", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Activity act = (Activity) param.thisObject;
                    try {
                        if (act != null) {
                            if (Build.VERSION.SDK_INT >= 26 && act.canEnterPictureInPictureMode()) {
                                boolean ok = act.enterPictureInPictureMode();
                                Log.i(TAG, "Requested enterPictureInPictureMode for " + act.getClass().getName() + " result=" + ok);
                            } else {
                                Log.i(TAG, "PiP not supported or SDK < 26 for " + act.getClass().getName());
                            }
                        }
                    } catch (Throwable t) {
                        Log.e(TAG, "Error in PiP hook", t);
                    }
                }
            });
        } catch (Throwable t) {
            Log.e(TAG, "Hook(Activity.onUserLeaveHint) setup failed", t);
        }

        // Hook android.media.MediaPlayer.pause to block pauses inside Facebook process
        try {
            XposedHelpers.findAndHookMethod("android.media.MediaPlayer", lpparam.classLoader, "pause", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    // prevent MediaPlayer.pause() from executing inside FB process
                    Log.i(TAG, "Blocked MediaPlayer.pause()");
                    param.setResult(null); // cancel original method
                }
            });
        } catch (Throwable t) {
            Log.e(TAG, "Hook(MediaPlayer.pause) failed", t);
        }

        // Hook WebView.onPause (some video players use WebView)
        try {
            XposedHelpers.findAndHookMethod("android.webkit.WebView", lpparam.classLoader, "onPause", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Log.i(TAG, "Blocked WebView.onPause()");
                    param.setResult(null); // cancel original method
                }
            });
        } catch (Throwable t) {
            Log.e(TAG, "Hook(WebView.onPause) failed", t);
        }

        // Hook ExoPlayer's setPlayWhenReady(boolean) to block pause requests (common in ExoPlayer)
        String[] exoCandidates = new String[] {
            "com.google.android.exoplayer2.SimpleExoPlayer",
            "com.google.android.exoplayer2.ExoPlayerImpl",
            "com.google.android.exoplayer2.Player"
        };
        for (String exoCls : exoCandidates) {
            try {
                XposedHelpers.findAndHookMethod(exoCls, lpparam.classLoader, "setPlayWhenReady", boolean.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (param.args != null && param.args.length > 0 && param.args[0] instanceof Boolean) {
                            boolean playWhenReady = (Boolean) param.args[0];
                            if (!playWhenReady) {
                                Log.i(TAG, "Blocked ExoPlayer.setPlayWhenReady(false) for " + exoCls);
                                param.setResult(null); // cancel pause
                            }
                        }
                    }
                });
            } catch (Throwable t) {
                Log.i(TAG, "ExoPlayer hook not present for " + exoCls + " - " + t.getMessage());
            }
        }

        // Hook common methods that may stop or release playback
        String[][] methodCandidates = new String[][] {
            {"com.google.android.exoplayer2.SimpleExoPlayer", "release"},
            {"com.google.android.exoplayer2.ExoPlayerImpl", "release"},
            {"android.media.MediaPlayer", "stop"},
            {"android.media.MediaPlayer", "release"}
        };
        for (String[] pair : methodCandidates) {
            String cls = pair[0];
            String mtd = pair[1];
            try {
                XposedHelpers.findAndHookMethod(cls, lpparam.classLoader, mtd, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Log.i(TAG, "Blocked " + cls + "." + mtd + "()");
                        param.setResult(null);
                    }
                });
            } catch (Throwable t) {
                Log.i(TAG, "Hook("+cls+"."+mtd+") not present - " + t.getMessage());
            }
        }

        Log.i(TAG, "FBPiP module hooks installed (best-effort).");
    }
}
