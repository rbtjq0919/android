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
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import java.util.Calendar;

public class ScheduleListAct extends ListActivity {

        public static final String ARG_WHERE = "WHERE";
        public static final String SQL_WHERE_DEFAULT = "";

        String mArgWhere = SQL_WHERE_DEFAULT;
        ListAdapter adapter;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_schedule_list);


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
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        System.out.println("----------------"+position);

    }



        @Override
        public void onResume() {
            super.onResume();
            Bundle args = getIntent().getExtras();
            if (args != null) {
                mArgWhere = args.getString(ARG_WHERE, SQL_WHERE_DEFAULT);
            }
            loadDB();
        }


    protected void loadDB() {
            ContentResolver cr = getContentResolver();

            try {
                // SELECT
                Cursor cur = cr.query(Uri.parse("content://moapp1.gps.calendar/schedule"),
                        null, mArgWhere, null, null);
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
                intent.putExtra("TARGET_TIMESTAMP",0);
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