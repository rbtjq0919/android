package com.example.jhbra.android_project;

import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class ScheduleListDayAct extends ListActivity {

    public static final String ARG_WHERE = "TARGET_TIMESTAMP";

    ListAdapter adapter;
    int toYear = 0;
    int toMonth = 0;
    int toDay = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_list_day);

        Intent intent = getIntent();
        toDay = intent.getExtras().getInt("TARGET_TIMESTAMP_DAY");
        toMonth = intent.getExtras().getInt("TARGET_TIMESTAMP_MONTH");
        toYear = intent.getExtras().getInt("TARGET_TIMESTAMP_YEAR");

        Button scheduleAdd = (Button) findViewById(R.id.scheduleaddbutton);
        scheduleAdd.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(getApplicationContext(),ScheduleEditAct.class);
                startActivity(intent);
            }
        });



    }


    @Override
    public void onResume() {
        super.onResume();
        Bundle args = getIntent().getExtras();
        if (args != null) {
        }
        loadDB();
    }


    protected void loadDB() {
        ContentResolver cr = getContentResolver();
        String selection = "";

        if (toYear != 0 && toMonth != 0 && toDay != 0) {
            long timestamp = new GregorianCalendar(toYear, toMonth-1, toDay, 0, 0)
                    .getTime().getTime();
            selection = "millis >= " + String.valueOf(timestamp)
                    + " and millis < "
                    + String.valueOf(timestamp + 86400000L);
            System.out.println(selection);

        }

        try {
                // SELECT
                Cursor cur = cr.query(Uri.parse("content://moapp1.gps.calendar/schedule"),
                    null, selection, null, null);
            startManagingCursor(cur);

            // Set ListAdapter
            adapter = new SimpleCursorAdapter(
                    this,
                    R.layout.item_schedule_list,
                    cur,
                    new String[]{"_id", "title", "alarm"},
                    new int[]{
                            R.id.textview_item_person_id,
                            R.id.textview_item_person_name,
                            R.id.textview_item_person_age,
                    },
                    0
            );
            setListAdapter(adapter);

/*                Toast.makeText(
                        this,
                        "Query content://ksbprac08_2.test/people where \"" + mArgWhere + "\"",
                        Toast.LENGTH_SHORT
                ).show();*/

        } catch (Exception e) {
            Intent intent = new Intent(getApplicationContext(),ScheduleEditAct.class);
            startActivity(intent);

            Toast.makeText(
                    this,
                    "일정 추가",
                    Toast.LENGTH_LONG
            ).show();
            finish();
        }
    }
}