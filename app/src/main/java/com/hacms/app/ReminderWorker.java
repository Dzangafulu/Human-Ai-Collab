package com.hacms.app;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/** WorkManager fallback for persistent local reminders and study session boundary alerts. */
public class ReminderWorker extends Worker {
    public ReminderWorker(@NonNull Context context,@NonNull WorkerParameters parameters){
        super(context,parameters);
    }
    @NonNull @Override public Result doWork(){
        long due=getInputData().getLong("scheduledAt",0);
        if(due>System.currentTimeMillis()+10_000L)return Result.retry();
        StudyNotificationPublisher.post(getApplicationContext(),getInputData().getString("uid"),
            getInputData().getString("key"),getInputData().getString("title"),
            getInputData().getString("body"),getInputData().getString("phase"),due);
        return Result.success();
    }
}
