package com.example.jhbra.android_project;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class CalendarActivity extends Activity {

    private TextView tvDate;
    private GridAdapter gridAdapter;
    private ArrayList<String> dayList;
    private GridView gridView;
    private Calendar mCal;

    int Year;
    int Month;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);
        tvDate = (TextView)findViewById(R.id.tv_date);
        gridView = (GridView)findViewById(R.id.gridview);
        long now = System.currentTimeMillis();
        final Date date = new Date(now);
        
        final SimpleDateFormat curYearFormat = new SimpleDateFormat("yyyy", Locale.KOREA);
        final SimpleDateFormat curMonthFormat = new SimpleDateFormat("MM", Locale.KOREA);
        final SimpleDateFormat curDayFormat = new SimpleDateFormat("dd", Locale.KOREA);

        Year =  Integer.parseInt(curYearFormat.format(date));
        Month =  Integer.parseInt(curMonthFormat.format(date));


        tvDate.setText(Year + " " + Month);
        
        dayList = new ArrayList<String>();
        dayList.add("일");
        dayList.add("월");
        dayList.add("화");
        dayList.add("수");
        dayList.add("목");
        dayList.add("금");
        dayList.add("토");
        
        mCal = Calendar.getInstance();
        
        
        mCal.set(Integer.parseInt(curYearFormat.format(date)), Integer.parseInt(curMonthFormat.format(date)) - 1, 1);
        int dayNum = mCal.get(Calendar.DAY_OF_WEEK);
        //1일 - 요일 매칭 시키기 위해 공백 add
        for (int i = 1; i < dayNum; i++) {
            dayList.add("");
        }
        setCalendarDate(mCal.get(Calendar.MONTH) + 1);

        gridAdapter = new GridAdapter(getApplicationContext(), dayList);
        gridView.setAdapter(gridAdapter);
        gridView.setOnItemClickListener(mItemClickListener);

        Button scheduleAllList = (Button) findViewById(R.id.list);
        scheduleAllList.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(),ScheduleListAct.class);
                startActivity(intent);
            }
        });
    }


    private void setCalendarDate(int month) {
        mCal.set(Calendar.MONTH, month - 1);

        for (int i = 0; i < mCal.getActualMaximum(Calendar.DAY_OF_MONTH); i++) {
            dayList.add("" + (i + 1));
        }
    }
    private AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long l_position) {
            int magin = mCal.get(Calendar.DAY_OF_WEEK) + 5;
            if(position > magin) {
                Intent intent = new Intent(getApplicationContext(), ScheduleListDayAct.class);

                int day = position - magin;

                intent.putExtra("TARGET_TIMESTAMP_DAY", day);
                intent.putExtra("TARGET_TIMESTAMP_MONTH", Month);
                intent.putExtra("TARGET_TIMESTAMP_YEAR", Year);

                startActivity(intent);
            }
        }
    };

    private class GridAdapter extends BaseAdapter {
        private final List<String> list;
        private final LayoutInflater inflater;

        public GridAdapter(Context context, List<String> list) {
            this.list = list;
            this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public String getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public int getDB(){
            long toDay = System.currentTimeMillis();

            Calendar mCalendar = Calendar. getInstance();
            int getDay  = mCalendar.get(Calendar.DAY_OF_MONTH) + mCal.get(Calendar.DAY_OF_WEEK) + 5 +1;

            return getDay;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder holder = null;

            if (convertView == null) {
                    convertView = inflater.inflate(R.layout.item_calendar_gridview, parent, false);
                    holder = new ViewHolder();

                holder.tvItemGridView = (TextView)convertView.findViewById(R.id.tv_item_gridview);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder)convertView.getTag();
            }
            holder.tvItemGridView.setText("" + getItem(position));

            mCal = Calendar.getInstance();
            Integer today = mCal.get(Calendar.DAY_OF_MONTH);
            String sToday = String.valueOf(today);

            // 일요일
            if (position%7 == 0){
                holder.tvItemGridView.setTextColor(getResources().getColor(R.color.colorRed));
            }
            // 토요일
            if (position%7 == 6){
                holder.tvItemGridView.setTextColor(getResources().getColor(R.color.colorRipple));
            }

            // 오늘 날짜
            if (sToday.equals(getItem(position))) {
                holder.tvItemGridView.setTextColor(getResources().getColor(R.color.colorBlack));
                holder.tvItemGridView.setTextSize(17);
            }

            return convertView;
        }
    }

    private class ViewHolder {
        TextView tvItemGridView;
    }
}
