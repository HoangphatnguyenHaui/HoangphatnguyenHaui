package com.example.login;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    private TextView textBookedDevices;

    private EditText editStartDatetime, editEndDatetime;
    private CheckBox checkWashingMachine1, checkWashingMachine2, checkWashingMachine3, checkWashingMachine4;
    private CheckBox checkDryer1, checkDryer2, checkDryer3, checkDryer4;
    private TextView textPrice, textMessage;
    private Button buttonConfirm,buttonRefresh;
    private long startTimeInMillis = 0;
    private long endTimeInMillis = 0;
    private long pricePerHour = 10000;
    private DatabaseReference databaseReference;
    private List<String> selectedDevices = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Database
        databaseReference = FirebaseDatabase.getInstance().getReference("bookings");

        // UI Elements
        textBookedDevices = findViewById(R.id.text_booked_devices);
        buttonRefresh = findViewById(R.id.button_refresh);
        editStartDatetime = findViewById(R.id.edit_start_datetime);
        editEndDatetime = findViewById(R.id.edit_end_datetime);
        checkWashingMachine1 = findViewById(R.id.check_washing_machine_1);
        checkWashingMachine2 = findViewById(R.id.check_washing_machine_2);
        checkWashingMachine3 = findViewById(R.id.check_washing_machine_3);
        checkWashingMachine4 = findViewById(R.id.check_washing_machine_4);
        checkDryer1 = findViewById(R.id.check_dryer_1);
        checkDryer2 = findViewById(R.id.check_dryer_2);
        checkDryer3 = findViewById(R.id.check_dryer_3);
        checkDryer4 = findViewById(R.id.check_dryer_4);
        textPrice = findViewById(R.id.text_price);
        textMessage = findViewById(R.id.text_message);
        buttonConfirm = findViewById(R.id.button_confirm);

        // Date and Time pickers
        editStartDatetime.setOnClickListener(v -> pickDateTime(true));
        editEndDatetime.setOnClickListener(v -> pickDateTime(false));

        // Checkbox listeners
        checkWashingMachine1.setOnCheckedChangeListener(this::onDeviceChecked);
        checkWashingMachine2.setOnCheckedChangeListener(this::onDeviceChecked);
        checkWashingMachine3.setOnCheckedChangeListener(this::onDeviceChecked);
        checkWashingMachine4.setOnCheckedChangeListener(this::onDeviceChecked);
        checkDryer1.setOnCheckedChangeListener(this::onDeviceChecked);
        checkDryer2.setOnCheckedChangeListener(this::onDeviceChecked);
        checkDryer3.setOnCheckedChangeListener(this::onDeviceChecked);
        checkDryer4.setOnCheckedChangeListener(this::onDeviceChecked);

        // Confirm button
        buttonConfirm.setOnClickListener(v -> confirmBooking());
        buttonRefresh.setOnClickListener(v -> refreshBookedDevices());
    }
    private void refreshBookedDevices() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                StringBuilder bookedDevices = new StringBuilder("Danh sách các thiết bị đã đặt:\n");
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Booking booking = snapshot.getValue(Booking.class);
                    if (booking != null) {
                        bookedDevices.append("Thời gian: ")
                                .append(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(booking.getStartTime())))
                                .append(" - ")
                                .append(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(booking.getEndTime())))
                                .append("\nThiết bị: ")
                                .append(String.join(", ", booking.getDevice()))
                                .append("\n\n");
                    }
                }
                textBookedDevices.setText(bookedDevices.toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                textMessage.setText("Lỗi khi lấy dữ liệu.");
            }
        });
    }
    private void pickDateTime(boolean isStart) {
        final Calendar currentDate = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            final Calendar date = Calendar.getInstance();
            date.set(year, month, dayOfMonth);
            new TimePickerDialog(MainActivity.this, (view1, hourOfDay, minute) -> {
                date.set(Calendar.HOUR_OF_DAY, hourOfDay);
                date.set(Calendar.MINUTE, minute);

                SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                if (isStart) {
                    startTimeInMillis = date.getTimeInMillis();
                    editStartDatetime.setText(format.format(date.getTime()));
                } else {
                    endTimeInMillis = date.getTimeInMillis();
                    editEndDatetime.setText(format.format(date.getTime()));
                }

                validateBookingTimes();
                checkDeviceAvailability();
            }, currentDate.get(Calendar.HOUR_OF_DAY), currentDate.get(Calendar.MINUTE), true).show();
        }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void validateBookingTimes() {
        if (startTimeInMillis >= endTimeInMillis) {
            textMessage.setText("Thời gian bắt đầu phải trước thời gian kết thúc");
            buttonConfirm.setEnabled(false);
        } else {
            textMessage.setText("");
            buttonConfirm.setEnabled(true);
            checkDeviceAvailability(); // Kiểm tra thiết bị có sẵn ngay sau khi thời gian được chọn
        }
    }


    private void onDeviceChecked(CompoundButton buttonView, boolean isChecked) {
        String device = buttonView.getText().toString();
        if (isChecked) {
            selectedDevices.add(device);
        } else {
            selectedDevices.remove(device);
        }

        // Update price immediately when selecting devices
        updatePrice();
    }

    private void updatePrice() {
        long totalMinutes = (endTimeInMillis - startTimeInMillis) / (1000 * 60); // Tổng số phút
        double totalPrice = (pricePerHour / 60.0) * totalMinutes * selectedDevices.size();
        textPrice.setText(String.format("Giá tiền: %.0f VNĐ", totalPrice));
    }


    private void confirmBooking() {
        if (startTimeInMillis == 0 || endTimeInMillis == 0 || selectedDevices.isEmpty()) {
            textMessage.setText("Vui lòng chọn thời gian và thiết bị.");
            return;
        }

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean canBook = true;
                List<String> conflictingDevices = new ArrayList<>();
                SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

                // Kiểm tra xung đột thiết bị
                for (String device : selectedDevices) {
                    for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
                        Booking booking = bookingSnapshot.getValue(Booking.class);
                        if (booking != null && booking.getDevice().contains(device)) {
                            try {
                                Date bookingStartTime = format.parse(booking.getStartTime());
                                Date bookingEndTime = format.parse(booking.getEndTime());
                                if (startTimeInMillis < bookingEndTime.getTime() && endTimeInMillis > bookingStartTime.getTime()) {
                                    canBook = false;
                                    conflictingDevices.add(device);
                                    break;
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

                if (canBook) {
                    // Tìm ID tiếp theo
                    int nextBookingId = 1;
                    for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
                        String id = bookingSnapshot.getKey();
                        if (id != null && id.startsWith("booking")) {
                            try {
                                int currentId = Integer.parseInt(id.replace("booking", ""));
                                if (currentId >= nextBookingId) {
                                    nextBookingId = currentId + 1;
                                }
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    // Tạo ID mới
                    String bookingId = "booking" + nextBookingId;

                    // Lưu thông tin đơn đặt hàng với ID mới
                    String startTimeString = format.format(new Date(startTimeInMillis));
                    String endTimeString = format.format(new Date(endTimeInMillis));
                    Booking booking = new Booking(selectedDevices, startTimeString, endTimeString, pricePerHour);

                    databaseReference.child(bookingId).setValue(booking);
                    Toast.makeText(MainActivity.this, "Đặt lịch thành công!", Toast.LENGTH_SHORT).show();
                } else {
                    textMessage.setText("Thiết bị " + conflictingDevices + " đã được đặt trong khoảng thời gian này.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Lỗi khi kiểm tra thiết bị", Toast.LENGTH_SHORT).show();
            }
        });
    }



    private void checkDeviceAvailability() {
        if (startTimeInMillis == 0 || endTimeInMillis == 0) {
            textMessage.setText("Vui lòng chọn thời gian trước khi chọn thiết bị.");
            return;
        }

        // Reset trạng thái và danh sách các thiết bị đã đặt
        resetDeviceAvailability();
        StringBuilder bookedDevicesText = new StringBuilder("Danh sách các thiết bị đã đặt:\n");

        // Đọc dữ liệu từ Firebase
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean anyDeviceBooked = false;
                SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

                for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
                    Booking booking = bookingSnapshot.getValue(Booking.class);
                    if (booking != null) {
                        try {
                            // Chuyển đổi từ String sang Date
                            Date bookingStartTime = format.parse(booking.getStartTime());
                            Date bookingEndTime = format.parse(booking.getEndTime());

                            // Kiểm tra nếu thời gian có sự chồng chéo
                            boolean isOverlap = (startTimeInMillis < bookingEndTime.getTime() && endTimeInMillis > bookingStartTime.getTime());
                            if (isOverlap) {
                                anyDeviceBooked = true;
                                // Duyệt qua từng thiết bị trong danh sách và gọi disableDevice cho từng cái
                                for (String device : booking.getDevice()) {
                                    disableDevice(device); // Gọi phương thức disableDevice cho từng thiết bị
                                }

                                bookedDevicesText.append(String.format("Thiết bị %s: từ %s đến %s\n",
                                        booking.getDevice().toString(),
                                        format.format(bookingStartTime),
                                        format.format(bookingEndTime)));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                if (!anyDeviceBooked) {
                    bookedDevicesText.append("Không có thiết bị nào được đặt trong khoảng thời gian này.");
                }
                textBookedDevices.setText(bookedDevicesText.toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Lỗi khi kiểm tra thiết bị", Toast.LENGTH_SHORT).show();
            }
        });
    }



    private void resetDeviceAvailability() {
        // Bật lại tất cả các checkbox trước khi kiểm tra xem có đơn đặt nào trong khoảng thời gian không
        checkWashingMachine1.setEnabled(true);
        checkWashingMachine2.setEnabled(true);
        checkWashingMachine3.setEnabled(true);
        checkWashingMachine4.setEnabled(true);
        checkDryer1.setEnabled(true);
        checkDryer2.setEnabled(true);
        checkDryer3.setEnabled(true);
        checkDryer4.setEnabled(true);
    }


    private void disableDevice(String device) {
        switch (device) {
            case "Máy giặt 1":
                checkWashingMachine1.setEnabled(false);
                checkWashingMachine1.setChecked(false); // Bỏ tích chọn
                selectedDevices.remove(device); // Loại bỏ khỏi danh sách đã chọn
                break;
            case "Máy giặt 2":
                checkWashingMachine2.setEnabled(false);
                checkWashingMachine2.setChecked(false);
                selectedDevices.remove(device);
                break;
            case "Máy giặt 3":
                checkWashingMachine3.setEnabled(false);
                checkWashingMachine3.setChecked(false);
                selectedDevices.remove(device);
                break;
            case "Máy giặt 4":
                checkWashingMachine4.setEnabled(false);
                checkWashingMachine4.setChecked(false);
                selectedDevices.remove(device);
                break;
            case "Máy sấy 1":
                checkDryer1.setEnabled(false);
                checkDryer1.setChecked(false);
                selectedDevices.remove(device);
                break;
            case "Máy sấy 2":
                checkDryer2.setEnabled(false);
                checkDryer2.setChecked(false);
                selectedDevices.remove(device);
                break;
            case "Máy sấy 3":
                checkDryer3.setEnabled(false);
                checkDryer3.setChecked(false);
                selectedDevices.remove(device);
                break;
            case "Máy sấy 4":
                checkDryer4.setEnabled(false);
                checkDryer4.setChecked(false);
                selectedDevices.remove(device);
                break;
        }
        // Cập nhật lại giá sau khi bỏ thiết bị đã bị vô hiệu hóa
        updatePrice();
    }



    private void enableDevice(String device) {
        switch (device) {
            case "Máy giặt 1":
                checkWashingMachine1.setEnabled(true);
                break;
            case "Máy giặt 2":
                checkWashingMachine2.setEnabled(true);
                break;
            case "Máy giặt 3":
                checkWashingMachine3.setEnabled(true);
                break;
            case "Máy giặt 4":
                checkWashingMachine4.setEnabled(true);
                break;
            case "Máy sấy 1":
                checkDryer1.setEnabled(true);
                break;
            case "Máy sấy 2":
                checkDryer2.setEnabled(true);
                break;
            case "Máy sấy 3":
                checkDryer3.setEnabled(true);
                break;
            case "Máy sấy 4":
                checkDryer4.setEnabled(true);
                break;
        }
    }
}
