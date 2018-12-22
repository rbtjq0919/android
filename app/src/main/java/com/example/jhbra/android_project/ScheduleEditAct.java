package com.example.jhbra.android_project;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

public class ScheduleEditAct extends AppCompatActivity {

    public static final String ARG_SCHEDULE_ID = "SCHEDULE_ID";
    public static final String ARG_TARGET_TIMESTAMP = "TARGET_TIMESTAMP";
    public static final int REQUEST_PLACE_PICKER = 5;

    @Nullable
    private Long mScheduleID;
    @NonNull
    private Date mTargetDate;
    @Nullable
    private Double mLongitude;
    @Nullable
    private Double mLatitude;
    @Nullable
    private Date mDepartureDate;
    @NonNull
    private Transportation mTransport = Transportation.CAR;
    @Nullable
    private Location mCurLocation;

    public ScheduleEditAct() {
        long millis = (System.currentTimeMillis() + 25 * 60 * 60 * 1000);
        // Discard time under hour
        millis -= millis % (60 * 60 * 1000);
        mTargetDate = new Date(millis);
    }

    private void hideSoftKeyboard() {
        InputMethodManager imm
                = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(findViewById(R.id.editTextTitle).getWindowToken(), 0);
        imm.hideSoftInputFromWindow(findViewById(R.id.editTextMemo).getWindowToken(), 0);
    }

    private @Nullable
    ContentValues fetchScheduleByID() {
        Uri uri = ContentUris.withAppendedId(DBProvider.CONTENT_URI, mScheduleID);
        Log.d("ScheduleEditAct", "initViewsFromScheduleID: querying to "
                + uri.toString());

        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(uri, null, null, null, null);

        if (cur != null) {
            if (cur.moveToFirst()) {
                ContentValues cv = new ContentValues();
                DatabaseUtils.cursorRowToContentValues(cur, cv);
                cur.close();
                Log.d("ScheduleEditAct", "initViewsFromScheduleID: just fetched schedule "
                        + String.valueOf(mScheduleID) + ": size of " + cv.size());
                return cv;
            } else {
                Log.e("ScheduleEditAct", "initViewsFromScheduleID: cannot fetch "
                        + "schedule " + String.valueOf(mScheduleID));
                cur.close();
            }
        } else {
            Log.e("ScheduleEditAct", "initViewsFromScheduleID: cursor is null fetching"
                    + String.valueOf(mScheduleID));
        }

        Toast.makeText(
                this,
                "일정을 불러올 수 없습니다.",
                Toast.LENGTH_LONG
        ).show();
        return null;
    }

    private void postToggleAlarm(boolean isChecked) {
        TextView textViewDepartureTime
                = (TextView) findViewById(R.id.textViewDepartureTime);
        if (isChecked) {
            textViewDepartureTime.setTextColor(
                    getResources().getColor(R.color.colorBlack, null));
        } else {
            textViewDepartureTime.setTextColor(
                    getResources().getColor(R.color.colorGray, null));
        }
    }

    /**
     * https://stackoverflow.com/a/38548560
     *
     * @param latitude
     * @param longitude
     * @return string
     */
    public static String getFormattedLocationInDegree(double latitude, double longitude) {
        try {
            int latSeconds = (int) Math.round(latitude * 3600);
            int latDegrees = latSeconds / 3600;
            latSeconds = Math.abs(latSeconds % 3600);
            int latMinutes = latSeconds / 60;
            latSeconds %= 60;

            int longSeconds = (int) Math.round(longitude * 3600);
            int longDegrees = longSeconds / 3600;
            longSeconds = Math.abs(longSeconds % 3600);
            int longMinutes = longSeconds / 60;
            longSeconds %= 60;
            String latDegree = latDegrees >= 0 ? "N" : "S";
            String lonDegrees = longDegrees >= 0 ? "E" : "W";

            return Math.abs(latDegrees) + "°" + latMinutes + "'" + latSeconds
                    + "\"" + latDegree + " " + Math.abs(longDegrees) + "°" + longMinutes
                    + "'" + longSeconds + "\"" + lonDegrees;
        } catch (Exception e) {
            return "" + String.format("%8.5f", latitude) + "  "
                    + String.format("%8.5f", longitude);
        }
    }

    private String geocodeLocation(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        String addressStr = null;
        try {
            List<Address> addressList
                    = geocoder.getFromLocation(latitude, longitude, 1);
            Address address = addressList.get(0);
            StringBuilder sb = new StringBuilder();
            sb.append(address.getCountryName());
            sb.append(' ');
            sb.append(address.getAdminArea());
            sb.append(' ');
            sb.append(address.getSubAdminArea());
            sb.append(' ');
            sb.append(address.getLocality());
            addressStr = sb.toString();
        } catch (IOException e) {
            Log.e("ScheduleEditAct", "geocodeLocation: cannot convert longitude " +
                    "and latitude to a geo-coded address!", e);
            addressStr = getFormattedLocationInDegree(latitude, longitude);
        }
        return addressStr;
    }

    private boolean initValuesAndViews() {
        TextView textViewToolbar = (TextView) findViewById(R.id.textViewToolbar);
        EditText editTextTitle = (EditText) findViewById(R.id.editTextTitle);
        EditText editTextMemo = (EditText) findViewById(R.id.editTextMemo);
        TextView textViewTargetDate = (TextView) findViewById(R.id.textViewTargetDate);
        TimePicker timePicker = (TimePicker) findViewById(R.id.timePickerTargetTime);
        TextView textViewDestinationAddress
                = (TextView) findViewById(R.id.textViewDestinationAddress);
        TextView textViewTransport = (TextView) findViewById(R.id.textViewTransportation);

        if (mScheduleID != null) {
            // Set toolbar label
            textViewToolbar.setText("일정 수정");

            ContentValues cv = fetchScheduleByID();
            if (cv == null) {
                return false;
            }

            String title = cv.getAsString("title");
            editTextTitle.setText(title);

            long millis = cv.getAsLong("millis");
            mTargetDate = new Date(millis);

            try {
                double longitude = cv.getAsDouble("longitude");
                double latitude = cv.getAsDouble("latitude");
                mLongitude = Double.valueOf(longitude);
                mLatitude = Double.valueOf(latitude);
            } catch (NullPointerException e) {
            }

            // Set transportation
            String tranportDBText = cv.getAsString("transport");
            Transportation transportSelected = null;
            for (Transportation t : Transportation.values()) {
                if (t.getDBText().equals(tranportDBText)) {
                    transportSelected = t;
                    break;
                }
            }
            if (transportSelected != null) {
                mTransport = transportSelected;
            } else {
                Log.e("ScheduleEditAct", "initValuesAndViews: "
                        + "Cannot comprehend DB column \"transport\": " + tranportDBText);
                return false;
            }

            // Set departure time
            Long departureTime = cv.getAsLong("departure");
            String departureTimeStr = null;
            if (departureTime != null) {
                mDepartureDate = new Date(departureTime);
                departureTimeStr = DateFormat.format("yyyy-MM-dd hh:mm aa", mDepartureDate)
                        .toString();
                // Set move duration
                long moveDuration = millis - departureTime;
                if (moveDuration >= 0) {
                    if (moveDuration > 525600) {
                        moveDuration = 525600;
                    }
                    EditText editTextMoveDuration
                            = (EditText) findViewById(R.id.editTextMoveDuration);
                    editTextMoveDuration.setText(String.valueOf(moveDuration));
                }
            } else {
                departureTimeStr = "미정";
            }
            TextView textViewDepartureTime = (TextView) findViewById(R.id.textViewDepartureTime);
            textViewDepartureTime.setText(departureTimeStr);

            Integer alarmEnabledInteger = cv.getAsInteger("alarm");
            Switch switchToggleAlarm = (Switch) findViewById(R.id.switchToggleAlarm);
            if (alarmEnabledInteger.intValue() == 1) {
                switchToggleAlarm.setChecked(true);
                postToggleAlarm(true);
            } else {
                switchToggleAlarm.setChecked(false);
                postToggleAlarm(false);
            }

            String memo = cv.getAsString("memo");
            editTextMemo.setText(memo);
        } else {
            // Set toolbar label
            textViewToolbar.setText("일정 추가");
        }

        // Set date/time views
        String dateStr = DateFormat.format("yyyy-MM-dd", mTargetDate).toString();
        textViewTargetDate.setText(dateStr);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(mTargetDate);
        timePicker.setHour(calendar.get(Calendar.HOUR_OF_DAY));
        timePicker.setMinute(calendar.get(Calendar.MINUTE));

        // Set destination address view
        if (mLongitude != null && mLatitude != null) {
            textViewDestinationAddress.setText(geocodeLocation(mLatitude, mLongitude));
        }

        // Set transportation view
        String[] transports = getResources().getStringArray(R.array.transportation);
        textViewTransport.setText(transports[mTransport.getNum()]);

        return true;
    }

    private @Nullable
    ContentValues buildContentValuesWithoutID() {
        ContentValues cv = new ContentValues();
        EditText editTextTitle = (EditText) findViewById(R.id.editTextTitle);
        EditText editTextMemo = (EditText) findViewById(R.id.editTextMemo);

        String title = editTextTitle.getText().toString().trim();
        if ("".equals(title)) {
            Toast.makeText(
                    this,
                    "제목을 입력해주세요.",
                    Toast.LENGTH_LONG
            ).show();
            return null;
        }
        cv.put("title", title);

        long millis = mTargetDate.getTime();
        cv.put("millis", millis);

        if (mDepartureDate != null) {
            cv.put("departure", mDepartureDate.getTime());
        } else {
            cv.putNull("departure");
        }

        if (mLongitude != null && mLatitude != null) {
            cv.put("longitude", mLongitude);
            cv.put("latitude", mLatitude);
        } else {
            cv.putNull("longitude");
            cv.putNull("latitude");
        }

        cv.put("transport", mTransport.getDBText());

        Switch switchToggleAlarm = (Switch) findViewById(R.id.switchToggleAlarm);
        cv.put("alarm", switchToggleAlarm.isChecked() ? 1 : 0);

        String memo = editTextMemo.getText().toString().trim();
        cv.put("memo", memo);

        return cv;
    }

    private void updateDepartureTimeAndView() {
        TextView textViewDepartureTime
                = (TextView) findViewById(R.id.textViewDepartureTime);
        EditText editTextMoveDuration = (EditText) findViewById(R.id.editTextMoveDuration);

        String moveDurationStr = editTextMoveDuration.getText().toString().trim();
        if ("".equals(moveDurationStr)) {
            return;
        }

        String departureTimeStr;
        int moveDuration = 0;
        try {
            // Convert to a minute integer
            moveDuration = Integer.parseInt(moveDurationStr);
        } catch (NumberFormatException e) {
            Log.e("ScheduleEditAct", "updateDepartureTimeAndView: cannot convert \""
                    + moveDurationStr + "\" to an integer!");
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(mTargetDate);
        calendar.add(Calendar.MINUTE, -1 * moveDuration);
        mDepartureDate = calendar.getTime();

        Log.d("ScheduleEditAct", "updateDepartureTimeAndView: mDepartureDate is "
                + mDepartureDate.toString());

        departureTimeStr
                = DateFormat.format("yyyy-MM-dd hh:mm aa", mDepartureDate)
                .toString();
        textViewDepartureTime.setText(departureTimeStr);
    }

    private void calculateAndUpdateDistance() {
        if (mCurLocation == null || mLongitude == null || mLatitude == null) {
            return;
        }
        Location destination = new Location("destination");
        destination.setLongitude(mLongitude);
        destination.setLatitude(mLatitude);
        float distance = destination.distanceTo(mCurLocation);
        int moveDuration = (int) Math.ceil(distance / (mTransport.getVelocity() * 1000.0 / 60.0));
        if (moveDuration < 3) {
            moveDuration = 3;
        }
        Log.i("ScheduleEditAct", String.format("calculateAndUpdateDistance: distance = %.2f m, "
                + "velocity = %.2f km/h, moveDuration = %d min", distance, mTransport.getVelocity(),
                moveDuration));
        EditText editTextMoveDuration
                = (EditText) findViewById(R.id.editTextMoveDuration);
        String moveDurationStr = String.valueOf(moveDuration);
        editTextMoveDuration.setText(moveDurationStr);
        updateDepartureTimeAndView();
        Toast.makeText(this, "예상 소요 시간이 자동으로 갱신되었습니다.",
                Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_edit);

        Bundle args = getIntent().getExtras();
        if (args != null) {
            Log.i("ScheduleEditAct", "Intent bundle is given");
            // mScheduleID
            try {
                long scheduleID = args.getLong(ARG_SCHEDULE_ID, -1);
                if (scheduleID != -1) {
                    mScheduleID = Long.valueOf(scheduleID);
                }
            } catch (Exception e) {
                Log.e("ScheduleEditAct", "onCreate: wrong bundle! invalid "
                        + ARG_SCHEDULE_ID, e);
            }
            // mTargetDate
            try {
                long millis = args.getLong(ARG_TARGET_TIMESTAMP);
                if (millis != 0) {
                    // Discard time under hour
                    millis -= millis % (60 * 60 * 1000);
                    mTargetDate = new Date(millis);
                }
            } catch (Exception e) {
                Log.e("ScheduleEditAct", "onCreate: wrong bundle! invalid "
                        + ARG_TARGET_TIMESTAMP, e);
            }
        } else {
            Log.i("ScheduleEditAct", "No intent bundle!");
        }

        // Testing update mode (forced)
        //mScheduleID = 1L;

        // Log values
        if (mScheduleID != null) {
            Log.d("ScheduleEditAct", "onCreate: mScheduleID is "
                    + String.valueOf(mScheduleID));
        } else {
            Log.d("ScheduleEditAct", "onCreate: mScheduleID is null");
        }
        Log.d("ScheduleEditAct", "onCreate: mTargetTime is "
                + DateFormat.format("yyyy-MM-dd'T'HH:mm:ss", mTargetDate).toString());

        if (!initValuesAndViews()) {
            mScheduleID = null;
            initValuesAndViews();
        }

        // Register TimePicker
        TimePicker timePicker = (TimePicker) findViewById(R.id.timePickerTargetTime);
        timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(mTargetDate);
                mTargetDate = new GregorianCalendar(
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH),
                        hourOfDay,
                        minute).getTime();
                Log.d("ScheduleEditAct", "onTimeChanged: mTargetDate is "
                        + mTargetDate.toString());
                updateDepartureTimeAndView();
            }
        });

        // Register Switch to toggle alarm
        Switch switchToggleAlarm = (Switch) findViewById(R.id.switchToggleAlarm);
        switchToggleAlarm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                hideSoftKeyboard();
                postToggleAlarm(isChecked);
            }
        });
        TextView textViewDepartureTime = (TextView) findViewById(R.id.textViewDepartureTime);
        textViewDepartureTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Switch switchToggleAlarm = (Switch) findViewById(R.id.switchToggleAlarm);
                switchToggleAlarm.toggle();
            }
        });

        // Set TextWatcher for editTextMoveDuration
        EditText editTextMoveDuration = (EditText) findViewById(R.id.editTextMoveDuration);
        TextWatcher moveDurationTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                TextView textViewDepartureTime
                        = (TextView) findViewById(R.id.textViewDepartureTime);
                String ss = s.toString().trim();
                String departureTimeStr = "미정";

                if (!"".equals(ss)) {
                    int moveDuration = 0;
                    try {
                        // Convert to a minute integer
                        moveDuration = Integer.parseInt(ss);
                        // Check range
                        if (moveDuration > 525600) {
                            throw new NumberFormatException();
                        }
                    } catch (NumberFormatException e) {
                        moveDuration = 525600;
                        EditText editTextMoveDuration
                                = (EditText) findViewById(R.id.editTextMoveDuration);
                        String moveDurationStr = String.valueOf(moveDuration);
                        editTextMoveDuration.setText(moveDurationStr);
                        editTextMoveDuration.setSelection(moveDurationStr.length());
                    }
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(mTargetDate);
                    calendar.add(Calendar.MINUTE, -1 * moveDuration);
                    mDepartureDate = calendar.getTime();
                    Log.d("ScheduleEditAct", "afterTextChanged: mDepartureDate is "
                            + mDepartureDate.toString());
                    departureTimeStr
                            = DateFormat.format("yyyy-MM-dd hh:mm aa", mDepartureDate)
                            .toString();
                }
                textViewDepartureTime.setText(departureTimeStr);
            }
        };
        editTextMoveDuration.addTextChangedListener(moveDurationTextWatcher);

        // Get current location
        LocationManager locationManager
                = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.d("ScheduleEditAct", "onLocationChanged: got the location => "
                        + "longitude = " + location.getLongitude() + ", latitude = "
                        + location.getLatitude());
                mCurLocation = location;
                // Update view
                TextView textViewCurLocation
                        = (TextView) findViewById(R.id.textViewCurrentLocation);
                textViewCurLocation.setText(
                        geocodeLocation(location.getLatitude(), location.getLongitude()));
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
            return;

        if (locationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        }

        if (locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PLACE_PICKER) {
            Log.d("ScheduleEditAct", "onActivityResult: requestCode = " +
                    "REQUEST_PLACE_PICKER, resultCode = " + resultCode);
            if (resultCode == RESULT_OK) {
                // Get place
                Place place = PlacePicker.getPlace(this, data);
                // Set TextView
                TextView textViewDestinationAddress
                        = (TextView) findViewById(R.id.textViewDestinationAddress);
                textViewDestinationAddress.setText(place.getName());
                // Set longitude and latitude
                LatLng latLng = place.getLatLng();
                mLongitude = latLng.longitude;
                mLatitude = latLng.latitude;
                Log.d("ScheduleEditAct", "onActivityResult: place selected => " +
                        "longitude = " + mLongitude + ", latitude = " + mLatitude);
                Toast.makeText(this, "장소를 선택했습니다.",
                        Toast.LENGTH_SHORT).show();
                // Update move duration
                calculateAndUpdateDistance();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "장소 선택을 취소했습니다.",
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.d("ScheduleEditAct", "onActivityResult: requestCode = " +
                    "unknown (" + requestCode + "), resultCode = " + resultCode);
        }
    }

    public void onClickTargetDate(View v) {
        hideSoftKeyboard();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(mTargetDate);

        final TextView textViewTargetDate = (TextView) findViewById(R.id.textViewTargetDate);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                ScheduleEditAct.this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(mTargetDate);
                        mTargetDate = new GregorianCalendar(year, month, dayOfMonth,
                                calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
                                .getTime();
                        textViewTargetDate.setText(String.format("%04d-%02d-%02d", year, month + 1,
                                dayOfMonth));
                        Log.d("ScheduleEditAct", "onDateSet: mTargetDate is "
                                + mTargetDate.toString());
                        updateDepartureTimeAndView();
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    public void onClickTextViewDestinationAddress(View v) {
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
        try {
            startActivityForResult(builder.build(this), REQUEST_PLACE_PICKER);
        } catch (Exception e) {
            Log.e("ScheduleEditAct", "onClickTextViewDestinationAddress: failed to " +
                    "startActivityForResult()", e);
        }
    }

    public void onClickTransportation(View v) {
        hideSoftKeyboard();

        new AlertDialog.Builder(this)
                .setTitle("이동수단을 선택하세요")
                .setItems(R.array.transportation,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String[] transports
                                        = getResources().getStringArray(R.array.transportation);
                                Transportation transportSelected = null;
                                for (Transportation t : Transportation.values()) {
                                    if (t.getNum() == which) {
                                        transportSelected = t;
                                        break;
                                    }
                                }
                                if (transportSelected != null) {
                                    mTransport = transportSelected;
                                    TextView textViewTransport
                                            = (TextView) findViewById(R.id.textViewTransportation);
                                    textViewTransport.setText(transports[which]);
                                    Log.d("ScheduleEditAct", "onClick (transportation): "
                                            + "Selected " + transportSelected.getDBText()
                                            + " (" + transports[which] + ")");
                                } else {
                                    Log.w("ScheduleEditAct", "onClick (transportation): "
                                            + "Cannot comprehend selection " + which
                                            + " (" + transports[which] + ")");
                                }
                                calculateAndUpdateDistance();
                            }
                        }
                )
                .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int button) {
                        switch (button) {
                            case DialogInterface.BUTTON_NEGATIVE:
                                Toast.makeText(ScheduleEditAct.this,
                                        "이동수단 선택을 취소했습니다.", Toast.LENGTH_SHORT).show();
                                break;
                        }
                    }
                })
                .setCancelable(false)
                .show();
    }

    public void onClickCancelOnToolbar(View v) {
        finish();
    }

    public void onClickSaveOnToolbar(View v) {
        hideSoftKeyboard();

        ContentValues cv = buildContentValuesWithoutID();
        if (cv == null) {
            return;
        }

        ContentResolver cr = getContentResolver();
        if (mScheduleID != null) {
            // Update mode
            Log.d("ScheduleEditAct", "onClickSaveOnToolbar: started to update "
                    + String.valueOf(mScheduleID));

            Uri uri = ContentUris.withAppendedId(DBProvider.CONTENT_URI, mScheduleID);
            cv.put("_id", mScheduleID);

            try {
                int count = cr.update(uri, cv, null, null);
                if (count < 0) {
                    throw new RuntimeException();
                }

                Log.d("ScheduleEditAct", "onClickSaveOnToolbar: just updated "
                        + uri);
                Toast.makeText(
                        this,
                        "일정을 수정했습니다!",
                        Toast.LENGTH_SHORT
                ).show();

                finish();
            } catch (Exception e) {
                Log.d("ScheduleEditAct", "onClickSaveOnToolbar: failed to update", e);
                Toast.makeText(
                        this,
                        "일정 수정에 실패했습니다!",
                        Toast.LENGTH_SHORT
                ).show();
            }
        } else {
            // Insert mode
            Log.d("ScheduleEditAct", "onClickSaveOnToolbar: started to insert");

            try {
                Uri resultUri = cr.insert(DBProvider.CONTENT_URI, cv);
                if (resultUri == null) {
                    throw new RuntimeException();
                }

                Log.d("ScheduleEditAct", "onClickSaveOnToolbar: just inserted to "
                        + resultUri.toString());
                Toast.makeText(
                        this,
                        "일정을 추가했습니다!",
                        Toast.LENGTH_SHORT
                ).show();

                finish();
            } catch (Exception e) {
                Log.d("ScheduleEditAct", "onClickSaveOnToolbar: failed to insert", e);
                Toast.makeText(
                        this,
                        "일정 추가에 실패했습니다!",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

}
