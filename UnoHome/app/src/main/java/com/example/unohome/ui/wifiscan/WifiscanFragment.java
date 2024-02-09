package com.example.unohome.ui.wifiscan;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
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
import java.util.ArrayList;
import java.util.List;

public class WifiscanFragment extends Fragment {

    private static final int REQUEST_PERMISSION_FINE_LOCATION = 1;
    private static final int REQUEST_PERMISSION_CHANGE_WIFI_STATE = 2;
    private static final int WIFI_SETTINGS_REQUEST = 123;
    private ListView wifiList;
    private WifiManager wifiManager;
    private LocationManager locationManager;
    private final ArrayList<String> wifiInfoList = new ArrayList<>();
    private String ssid = null;
    private String password = "";
    private Bundle bundle;
    private ArrayList<RoomEntity> roomData = new ArrayList<>();
    Type RoomListType = new TypeToken<ArrayList<RoomEntity>>() {}.getType();

    @RequiresApi(api = Build.VERSION_CODES.P)
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        WifiscanViewModel WifiscanViewModel = ViewModelProviders.of(this).get(WifiscanViewModel.class);
        View root = inflater.inflate(R.layout.fragment_wifiscan, container, false);
        WifiscanViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                //textView.setText(s);
            }
        });

        wifiList = root.findViewById(R.id.wifi_device_list);
        Button refreshButton = root.findViewById(R.id.refresh_button);
        Button connectButton = root.findViewById(R.id.connect_button);
        wifiManager = (WifiManager) requireContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        locationManager = (LocationManager) requireContext().getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        bundle = getArguments();
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
                scanWifiDevices();
            }
        }

        wifiList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            View previous = null;
            String selected;
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (previous != null && previous != view) {
                    CheckedTextView previousChecked = (CheckedTextView) previous;
                    previousChecked.setChecked(false);
                }
                CheckedTextView checked = (CheckedTextView) view;
                checked.setChecked(!checked.isChecked());
                if(checked.isChecked()){
                    selected = (String) parent.getItemAtPosition(position);
                    String[] selected_device = selected.split("\n");
                    ssid = selected_device[1].replace("SSID: ", "");
                } else {
                    ssid = null;
                }
                previous = view;
            }
        });

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isEnabled()){
                    scanWifiDevices();
                }
            }
        });

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ssid != null) {
                    connectToWifi();
                } else {
                    Toast.makeText(requireContext(), "Please select Uno Device", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return root;
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


    private void scanWifiDevices() {
        assert wifiManager != null;
        wifiManager.startScan();
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_FINE_LOCATION);
            return;
        }
        List<ScanResult> scanResults = wifiManager.getScanResults();
        wifiInfoList.clear();
        for (ScanResult scanResult : scanResults) {
            String wifiInfo = "\nSSID: " + scanResult.SSID +
                              "\nBSSID: " + scanResult.BSSID + "    RSS: " + scanResult.level + " dBm\n";
            wifiInfoList.add(wifiInfo);
        }
        CustomAdapter adapter = new CustomAdapter(requireContext(), android.R.layout.simple_list_item_checked, wifiInfoList);
        wifiList.setAdapter(adapter);
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
            if(connectedSSID.equals(ssid)){
                saveToInternalStorage();
            }
            else{
                Toast.makeText(requireContext(), "Couldn't connect to the desired device", Toast.LENGTH_SHORT).show();
            }
        }
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
        loadFromInternalStorage();
        String roomName = bundle.getString("RoomName");
        ArrayList<String> appliances = bundle.getStringArrayList("Appliances");
        RoomEntity room = new RoomEntity(roomName, ssid, password, appliances);
        roomData.add(room);

        String roomDataJson = new Gson().toJson(roomData);
        try {
            File file = new File(requireContext().getFilesDir(), "room_data.json");
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(roomDataJson.getBytes());
            fileOutputStream.close();
            bundle.putString("Status", "Room Added");
            Toast.makeText(requireContext(), "New room added successfully.", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(requireView()).popBackStack(R.id.navigation_saved_room, false);
            Navigation.findNavController(requireView()).navigate(R.id.navigation_saved_room, bundle);
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Couldn't able to save room!", Toast.LENGTH_SHORT).show();
        }
    }

    public static class CustomAdapter extends ArrayAdapter<String> {
        CustomAdapter(Context context, int resource, List<String> items) {
            super(context, resource, items);
        }
        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            CheckedTextView checkedTextView = (CheckedTextView) view;
            checkedTextView.setChecked(checkedTextView.isChecked());
            return view;
        }
    }
}
