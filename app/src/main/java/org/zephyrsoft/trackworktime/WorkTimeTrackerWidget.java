package org.zephyrsoft.trackworktime;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
//import android.R.drawable;
//import android.content.SharedPreferences;
//import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.view.View;
import android.content.Intent;
import android.app.PendingIntent;
import android.app.IntentService;
import android.widget.RemoteViews;
import android.content.ComponentName;
import org.pmw.tinylog.Logger;

//import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.model.PeriodEnum;
import org.zephyrsoft.trackworktime.model.Task;
import org.zephyrsoft.trackworktime.timer.TimeCalculator;
import org.zephyrsoft.trackworktime.timer.TimerManager;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;
import org.zephyrsoft.trackworktime.R.drawable;

import static org.zephyrsoft.trackworktime.Constants.INTENT_EXTRA_TASK;

//import org.zephyrsoft.trackworktime.util.PreferencesUtil;

/**
 * Implementation of App Widget functionality.
 */
public class WorkTimeTrackerWidget extends AppWidgetProvider {

    static void staticUpdateWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

//        // Create the task selection preference (if necessary)
//        SharedPreferences widgetSharedPref = context.getSharedPreferences(
//                "org.zephyrsoft.trackworktime.widget_pref_file", Context.MODE_PRIVATE);
//        if (!widgetSharedPref.contains("widget_sel_task")) {
//            DAO dao =  Basics.getInstance().getDao();
//            Task defTask = dao.getDefaultTask();
//            SharedPreferences.Editor widgetPrefEditor = widgetSharedPref.edit();
//            if (defTask != null) {
//                widgetPrefEditor.putInt("widget_sel_task", defTask.getId());
//            } else {
//                widgetPrefEditor.putInt("widget_sel_task", 0);
//            }
//            widgetPrefEditor.commit();
//        }

        // Create intent that launches the main app
        Intent appStartIntent = new Intent(context, WorkTimeTrackerActivity.class);
        PendingIntent appStartPendingIntent = PendingIntent.getActivity(
                context, 0, appStartIntent, 0);

//        // Create intent that starts the tracking
//        Intent clockInOutIntent = new Intent("org.zephyrsoft.trackworktime.ClockIn");
//        PendingIntent clockInOutPendingIntent = PendingIntent.getBroadcast(
//                context, 0, clockInOutIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        // Create intent that launches the task selection popup activity
        Intent taskListIntent = new Intent(context, WorkTimeTrackerWidget.class);
        taskListIntent.setAction("org.zephyrsoft.trackworktime.WidgetShowTasks");
        PendingIntent taskListPendingIntent = PendingIntent.getBroadcast(
                context, 0, taskListIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.work_time_tracker_widget);
        views.setOnClickPendingIntent(R.id.appImage, appStartPendingIntent);
//        views.setOnClickPendingIntent(R.id.widgetButton, clockInOutPendingIntent);
        views.setOnClickPendingIntent(R.id.widgetTask, taskListPendingIntent);

//        views.setTextViewText(R.id.widgetTask, "Default");
//        views.setTextViewText(R.id.widgetTime, "0:00");
//        views.setViewVisibility(R.id.widgetTime, View.INVISIBLE);
//        views.setImageViewResource(R.id.widgetButton, drawable.ic_media_play);
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            staticUpdateWidget(context, appWidgetManager, appWidgetId);
            context.startService(new Intent(context, WidgetUpdateService.class));
        }
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        // If the intent is the one that signals to launch the task selection
        // we launch the activity
        String recAction = intent.getAction();
        if(recAction != null && recAction.equals("org.zephyrsoft.trackworktime.WidgetShowTasks")) {
            Intent showTasksIntent = new Intent(context, WidgetTaskListActivity.class);
            showTasksIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(showTasksIntent);
        } {
            Logger.warn("Unknown intent action {} for WorkTimeTrackerWidget", intent.getAction());
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    public static class WidgetUpdateService extends IntentService
    {
//        private SharedPreferences preferences = null;
        private DAO dao = null;
        private TimerManager timerManager = null;
        private TimeCalculator timeCalculator = null;

        public WidgetUpdateService() {
            super("WorkTimeTrackerWidget$WidgetUpdateService");
        }

        @Override
        public void onCreate() {
            super.onCreate();
            timerManager = Basics.getInstance().getTimerManager();
            timeCalculator = Basics.getInstance().getTimeCalculator();
            dao =  Basics.getInstance().getDao();
//            preferences = PreferenceManager.getDefaultSharedPreferences(this);
//            dao = new DAO(this);
//            timerManager = new TimerManager(dao, preferences, this);
//            timeCalculator = new TimeCalculator(dao, timerManager);
        }

        @Override
        public void onDestroy() {
//            dao.close();
            super.onDestroy();
        }

        @Override
        public void onHandleIntent(Intent intent) {
            ComponentName me = new ComponentName(this, WorkTimeTrackerWidget.class);
            AppWidgetManager mgr = AppWidgetManager.getInstance(this);
            mgr.updateAppWidget(me, buildUpdate(this));
        }

        private RemoteViews buildUpdate(Context context) {
            Intent widgetButtonIntent;
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.work_time_tracker_widget);
            String taskName = "none";
            int taskId = -1;

            if (timerManager.isTracking()) {
                Task currTask = timerManager.getCurrentTask();
                if (currTask != null) {
                    taskId = currTask.getId();
                } else {
                    Logger.debug("tracking is running, but no task reported");
                }
                widgetButtonIntent = new Intent("org.zephyrsoft.trackworktime.ClockOut");
                views.setImageViewResource(R.id.widgetButton, drawable.ic_media_stop);
                views.setViewVisibility(R.id.widgetTime, View.VISIBLE);
                views.setViewVisibility(R.id.widgetTimePre, View.VISIBLE);
                views.setViewVisibility(R.id.widgetTimePost, View.VISIBLE);
                String timeSoFar = timerManager.calculateTimeSum(DateTimeUtil.getCurrentDateTime(), PeriodEnum.DAY).toString();
                views.setTextViewText(R.id.widgetTime, timeSoFar);
            } else {
                widgetButtonIntent = new Intent("org.zephyrsoft.trackworktime.ClockIn");

                SharedPreferences widgetSharedPref = context.getSharedPreferences(
                        "org.zephyrsoft.trackworktime.widget_pref_file", Context.MODE_PRIVATE);
                taskId = widgetSharedPref.getInt("widget_sel_task", -1);
                if (taskId == -1) { // preference not yet stored, get default task
                    Task defaultTask = dao.getDefaultTask();
                    if (defaultTask != null) {
                        taskId = defaultTask.getId();
                    } else {
                              Logger.warn("no default task and none selected");
                    }
                }

                views.setImageViewResource(R.id.widgetButton, drawable.ic_media_play);
                views.setViewVisibility(R.id.widgetTime, View.INVISIBLE);
                views.setViewVisibility(R.id.widgetTimePre, View.INVISIBLE);
                views.setViewVisibility(R.id.widgetTimePost, View.INVISIBLE);
                views.setTextViewText(R.id.widgetTime, "0:00");
            }
            if (taskId == -1) { // no task which can be used by the widget
                views.setBoolean(R.id.widgetButton, "setEnabled", false);
            } else {
                Task selTask = dao.getTask(taskId);
                if (selTask != null) {
                    taskName = selTask.getName();
                    views.setBoolean(R.id.widgetButton, "setEnabled", true);
                    widgetButtonIntent.putExtra(INTENT_EXTRA_TASK, taskId);
                    views.setOnClickPendingIntent(R.id.widgetButton, PendingIntent.getBroadcast(context, 0, widgetButtonIntent, PendingIntent.FLAG_UPDATE_CURRENT));
                } else {
                    Logger.warn("valid task id but could not get name");
                    views.setBoolean(R.id.widgetButton, "setEnabled", false);
                }
            }
            views.setTextViewText(R.id.widgetTask, taskName);

            return views;
        }
    }

}

