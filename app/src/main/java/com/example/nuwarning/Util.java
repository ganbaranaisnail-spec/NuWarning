package com.example.nuwarning;

import static android.content.Context.MODE_PRIVATE;

import static java.security.AccessController.getContext;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Util {

    private static final String PREFERENCES_NAME = "LogPreferences";
    public static final String PREFERENCES_NOTIFY = " NotificationPrefs";
    private static final String LOG_KEY = "log";
    private static final String TAG = "Util";
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String APP_SETTINGS = "AppSettings";
    public static final String APP_SETTINGS_KEY_STATE = "monitoring_state";
    private static final String APP_IS_ALIVE = "app_is_alive";
    private static final String APP_IS_TC = "app_is_tc";

    // SharedPreferences に保存されるログの最大行数
    private static int maxLogLines = 1000; // デフォルトでは最大300行保存

    // SimpleDateFormat のインスタンスを static final で定義
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT);

    public static String getDateSt() {
        // 現在の時刻を取得し、指定されたフォーマットで文字列に変換
        return SIMPLE_DATE_FORMAT.format(new Date());
    }

    /**
     * ログ出力メソッド (Log.d の代替)
     *
     * @param tag     ログのタグ
     * @param msg     ログのメッセージ
     * @param context コンテキスト (SharedPreferences へのアクセスに使用)
     */
    public static void sLogd(String tag, String msg, Context context) {
        String logMessage = String.format("%s %s: %s", getDateSt(), tag, msg);
        saveLogToPreferences(logMessage, context);
        Log.d(tag, msg); // 必要に応じて実際の Log.d も出力
    }

    /**
     * SharedPreferences にログを保存するメソッド
     *
     * @param logMessage ログメッセージ
     * @param context    コンテキスト
     */
    private static void saveLogToPreferences(String logMessage, Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        String existingLogs = prefs.getString(LOG_KEY, "");

        // ログメッセージを追加し、最大行数を超えた場合は古いログを削除
        String newLogs = existingLogs + logMessage + "\n";
        String[] logsArray = newLogs.split("\n"); // 改行コードは \n に統一
        if (logsArray.length > maxLogLines) {
            StringBuilder trimmedLogs = new StringBuilder();
            for (int i = logsArray.length - maxLogLines; i < logsArray.length; i++) {
                trimmedLogs.append(logsArray[i]).append("\n");
            }
            newLogs = trimmedLogs.toString();
        }

        prefs.edit().putString(LOG_KEY, newLogs).apply();
    }


    public static void saveAppIsAlive(boolean isAlive, Context context) {
        SharedPreferences prefs = context.getSharedPreferences(APP_SETTINGS, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(APP_IS_ALIVE, isAlive);
        editor.apply();
    }

    public static boolean isAppAlive(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(APP_SETTINGS, Context.MODE_PRIVATE);
        boolean ret = prefs.getBoolean(APP_IS_ALIVE, false); // デフォルト値は false
        Log.d(TAG, "isAppAlive: ret = " + ret);
        return ret;
    }

    public static void saveAppIsTC(boolean isTC, Context context) {
        SharedPreferences prefs = context.getSharedPreferences(APP_SETTINGS, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(APP_IS_TC, isTC);
        editor.apply();
    }

    public static boolean isAppTC(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(APP_SETTINGS, Context.MODE_PRIVATE);
        boolean ret = prefs.getBoolean(APP_IS_TC, false); // デフォルト値は false
        Log.d(TAG, "isAppTC: ret = " + ret);
        return ret;
    }

    /**
     * ログの最大行数を設定するメソッド
     *
     * @param maxLines 設定する最大行数
     */
    public static void setMaxLogLines(int maxLines) {
        Util.maxLogLines = maxLines;
    }

    /**
     * SharedPreferences から保存されたログを取得するメソッド
     *
     * @param context コンテキスト
     * @return 保存されたログ
     */
    public static String getSavedLogs(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        return prefs.getString(LOG_KEY, "");
    }

    /**
     * 文字列 str1 に str2 が含まれているかをチェックするメソッド
     *
     * @param str1 チェック対象の文字列
     * @param str2 検索する文字列
     * @return str1 に str2 が含まれていれば true を返す
     */
    public static boolean containsString(String str1, String str2) {
        return str1 != null && str2 != null && str1.contains(str2);
    }
}