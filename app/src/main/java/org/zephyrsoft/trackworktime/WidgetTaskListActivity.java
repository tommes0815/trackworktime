/*
 * This file is part of TrackWorkTime (TWT).
 * 
 * TWT is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * TWT is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with TWT. If not, see <http://www.gnu.org/licenses/>.
 */
package org.zephyrsoft.trackworktime;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.model.Task;

import java.util.List;

/**
 * Activity for listing the tasks that the user can select on the widget.
 * 
 * @author Thomas Kühne
 */
public class WidgetTaskListActivity extends AppCompatActivity {

	private DAO dao = null;

	private WorkTimeTrackerActivity parentActivity = null;

	private ArrayAdapter<Task> tasksAdapter;

	@Override
	protected void onPause() {
		dao.close();
		super.onPause();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.widget_task_list);

        ListView tasksListView = findViewById(R.id.task_list_view);

		dao = Basics.getInstance().getDao();
        List<Task> tasks = dao.getAllTasks();
		tasksAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, tasks);
		tasksAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		tasksListView.setAdapter(tasksAdapter);

		tasksListView.setTextFilterEnabled(true);

		tasksListView.setOnItemClickListener((AdapterView<?> adapter, View view, int position, long arg) -> {
            Task selTask = tasksAdapter.getItem(position);
            if (selTask != null) {
                Logger.debug("selected {} ", selTask.getName());
                Context context = getApplicationContext();
                SharedPreferences widgetSharedPref = context.getSharedPreferences(
                    "org.zephyrsoft.trackworktime.widget_pref_file", Context.MODE_PRIVATE);
                SharedPreferences.Editor widgetPrefEditor = widgetSharedPref.edit();
                widgetPrefEditor.putInt("widget_sel_task", selTask.getId());
                widgetPrefEditor.commit();
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                ComponentName thisWidget = new ComponentName(context, WorkTimeTrackerWidget.class);
                int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
                for (int appWidgetId : appWidgetIds) {
                    Logger.debug("updating widget id={}", appWidgetId);
                    context.startService(new Intent(context, WorkTimeTrackerWidget.WidgetUpdateService.class));
                }
            } else {
                Logger.warn("error during task selection, result is null ");
            }
            finish();
        });
	}
}
