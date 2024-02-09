package com.example.unohome.ui.devicecontrol;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.Formatter;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.Navigation;

import com.example.unohome.R;
import com.example.unohome.data.RoomEntity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;

public class DeviceControlFragment extends Fragment {

    private static final int REQUEST_PERMISSION_FINE_LOCATION = 1;
    private static final int REQUEST_PERMISSION_CHANGE_WIFI_STATE = 2;
    private static final int WIFI_SETTINGS_REQUEST = 123;
    private WifiManager wifiManager;
    private LocationManager locationManager;
    private byte[] buf;
    private SupplicantState supplicantState;
    private Bundle bundle;
    private GridLayout gridLayout;
    private TextView textView;
    private TextView textView2;
    private String RoomName;
    private String SSID;
    private String password;
    private ArrayList<String> appliances;
    private String roomInfo;
    private ArrayList<RoomEntity> roomData = new ArrayList<>();
    Type RoomListType = new TypeToken<ArrayList<RoomEntity>>() {}.getType();

    @SuppressLint("MissingInflatedId")
    @RequiresApi(api = Build.VERSION_CODES.P)
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        DeviceControlViewModel DeviceControlViewModel = ViewModelProviders.of(this).get(DeviceControlViewModel.class);
        View root = inflater.inflate(R.layout.fragment_device_control, container, false);
        DeviceControlViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
            }
        });

        bundle = getArguments();
        wifiManager = (WifiManager) requireContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        locationManager = (LocationManager) requireContext().getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        buf = new byte[1024];
        supplicantState = wifiManager.getConnectionInfo().getSupplicantState();
        textView = root.findViewById(R.id.textView);
        textView2 = root.findViewById(R.id.textView2);
        gridLayout = root.findViewById(R.id.gridLayout);
        assert bundle != null;
        RoomName = bundle.getString("RoomName");
        SSID = bundle.getString("SSID");
        password = bundle.getString("Password");
        appliances = bundle.getStringArrayList("Appliances");
        roomInfo = "Uno Device: " + SSID + "\nConnection Status: " + supplicantState;
        Button refreshButton = root.findViewById(R.id.refresh_button);
        Button deleteButton = root.findViewById(R.id.delete_button);
        textView.setText(RoomName);
        boolean locationPermission = false, wifiPermission = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && requireContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_FINE_LOCATION);
        } else {
            locationPermission = true;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && requireContext().checkSelfPermission(Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CHANGE_WIFI_STATE}, REQUEST_PERMISSION_CHANGE_WIFI_STATE);
        } else {
            wifiPermission = true;
        }
        if (wifiPermission && locationPermission) {
            if(isEnabled()){
                if(isConnected()){
                    textView2.setText(roomInfo);
                    showAppliancesList();
                }
                else {
                    connectToWifi();
                }
            }
        }

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isEnabled()){
                    if(!isConnected()){
                        textView2.setText(roomInfo);
                        showAppliancesList();
                    }
                    else {
                        connectToWifi();
                    }
                }
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle("Are you sure you want to delete?");
                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteFromInternalStorage();
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.show();
            }
        });

        return root;
    }

    private void loadFromInternalStorage() {
        try {
            File file = new File(requireContext().getFilesDir(), "room_data.json");
            if (!file.exists()) {
                return;
            }

            FileInputStream fileInputStream = new FileInputStream(file);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            fileInputStream.close();
            String roomDataJson = stringBuilder.toString();
            roomData = new Gson().fromJson(roomDataJson, RoomListType);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveToInternalStorage() {
        String roomDataJson = new Gson().toJson(roomData);
        try {
            File file = new File(requireContext().getFilesDir(), "room_data.json");
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(roomDataJson.getBytes());
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteFromInternalStorage() {
        loadFromInternalStorage();
        Iterator<RoomEntity> iterator = roomData.iterator();
        while (iterator.hasNext()) {
            RoomEntity room = iterator.next();
            if (RoomName.equals(room.getRoomName())) {
                iterator.remove();
            }
        }
        saveToInternalStorage();
        bundle.putString("Status", "Room Deleted");
        Toast.makeText(requireContext(), "Room deleted successfully.", Toast.LENGTH_SHORT).show();
        Navigation.findNavController(requireView()).popBackStack(R.id.navigation_saved_room, false);
        Navigation.findNavController(requireView()).navigate(R.id.navigation_saved_room, bundle);
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private boolean isEnabled() {
        boolean isWiFiEnable = false;
        boolean isLocationEnable = false;
        if (wifiManager != null && !wifiManager.isWifiEnabled()) {
            Toast.makeText(requireContext(), "Please enable Wi-Fi Service", Toast.LENGTH_SHORT).show();
        } else {
            isWiFiEnable = true;
        }
        if (locationManager != null && !locationManager.isLocationEnabled()) {
            Toast.makeText(requireContext(), "Please enable Location Services", Toast.LENGTH_SHORT).show();
        } else {
            isLocationEnable = true;
        }

        return isWiFiEnable && isLocationEnable;
    }

    private boolean isConnected(){
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            String connectedSSID = wifiInfo.getSSID().replace("\"", ""); // SSID may be surrounded by double quotes
            return connectedSSID.equals(SSID);
        }
        return false;
    }

    private void connectToWifi(){
        Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
        startActivityForResult(intent, WIFI_SETTINGS_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == WIFI_SETTINGS_REQUEST) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            String connectedSSID = wifiInfo.getSSID().replace("\"", "");
            if(connectedSSID.equals(SSID)){
                textView2.setText(roomInfo);
                showAppliancesList();
            }
            else{
                Toast.makeText(requireContext(), "Couldn't connect to the desired device", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showAppliancesList() {
        SupplicantState connectionState = wifiManager.getConnectionInfo().getSupplicantState();
        if (connectionState == SupplicantState.COMPLETED) {
            roomInfo = "Uno Device: " + SSID + "\nConnection Status: " + connectionState;
            textView.setText(RoomName);
            textView2.setText(roomInfo);
            gridLayout.removeAllViews();
            for (int row = 0; row < appliances.size(); row++) {
                @SuppressLint("UseSwitchCompatOrMaterialCode")
                Switch appliance = new Switch(requireContext());
                appliance.setText(appliances.get(row));
                appliance.setTextSize(20);
                appliance.setHeight(150);
                appliance.setWidth(300);

                GridLayout.Spec rowSpec = GridLayout.spec(row);
                GridLayout.Spec colSpec = GridLayout.spec(0);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams(rowSpec, colSpec);
                params.width = GridLayout.LayoutParams.WRAP_CONTENT;
                params.height = GridLayout.LayoutParams.WRAP_CONTENT;
                params.setGravity(Gravity.CENTER_HORIZONTAL);

                int applianceNumber = row+1;
                appliance.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (appliance.isChecked()) {
                            Client c = new Client();
                            buf = null;
                            buf = (applianceNumber+"-1").getBytes();
                            c.execute();
                        } else {
                            Client c = new Client();
                            buf = null;
                            buf = (applianceNumber+"-0").getBytes();
                            c.execute();
                        }
                    }
                });

                gridLayout.addView(appliance, params);
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    public class Client extends AsyncTask<Void, Void, Void> {
        private final static String SERVER_ADDRESS = "192.168.4.1";
        private final static int SERVER_PORT = 6377;

        @Override
        public Void doInBackground(Void... voids) {
            InetAddress serverAddress;
            DatagramPacket packet;
            DatagramSocket socket;

            try {
                serverAddress = InetAddress.getByName(SERVER_ADDRESS);
                socket = new DatagramSocket();
                packet = new DatagramPacket(buf, buf.length, serverAddress, SERVER_PORT);
                socket.send(packet);
                socket.close();
            } catch (IOException e) {
                requireActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            return null;
        }
    }
}
