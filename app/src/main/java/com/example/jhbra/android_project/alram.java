package com.example.jhbra.android_project;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import java.util.Calendar;

public class alram extends AppCompatActivity {
    private static int ONE_MINUTE = 5626;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_edit);
        new AlarmHATT(getApplicationContext()).Alarm();

    }

    public class AlarmHATT {
        private Context context;

        public AlarmHATT(Context context) {
            this.context = context;
        }

        public void Alarm() {
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(alram.this, BroadcastD.class);

            PendingIntent sender = PendingIntent.getBroadcast(alram.this, 0, intent, 0);

            Calendar calendar = Calendar.getInstance();
            //알람시간 calendar에 set해주기

            calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE), 19, 33, 30);

            //알람 예약
            am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), sender);
        }
    }
}