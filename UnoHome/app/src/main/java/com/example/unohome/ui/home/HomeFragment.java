package com.example.unohome.ui.home;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class HomeFragment extends Fragment {
    private GridLayout gridLayout;
    private ArrayList<RoomEntity> roomData = new ArrayList<>();
    Type RoomListType = new TypeToken<ArrayList<RoomEntity>>() {}.getType();

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel = ViewModelProviders.of(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        homeViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                loadFromInternalStorage();
            }
        });

        Bundle bundle = getArguments();
        gridLayout = root.findViewById(R.id.grid_layout);
        if (bundle != null && bundle.containsKey("Status")){
            loadFromInternalStorage();
        }

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
            updateLayout(gridLayout);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("RtlHardcoded")
    private void updateLayout(GridLayout gridLayout){
        boolean even = false;
        int row = 0;
        int col = 0;
        for (int i = 0; i < roomData.size(); i++) {
            RoomEntity room = roomData.get(i);
            String roomName = room.getRoomName();
            Button roomButton = new Button(requireContext());
            roomButton.setText(roomName);
            roomButton.setTextSize(20);
            roomButton.setHeight(300);
            roomButton.setWidth(350);

            ViewGroup parentLayout = (ViewGroup) roomButton.getParent();
            if (parentLayout != null) {
                parentLayout.removeView(roomButton);
            }
            if (even) {
                col = 1;
            } else {
                col = 0;
            }
            GridLayout.Spec rowSpec = GridLayout.spec(row);
            GridLayout.Spec colSpec = GridLayout.spec(col);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(rowSpec, colSpec);
            params.width = GridLayout.LayoutParams.WRAP_CONTENT;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            if (even) {
                params.setGravity(Gravity.RIGHT);
                row = row + 1;
            } else {
                params.setGravity(Gravity.LEFT);
            }

            roomButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Bundle roomInfo = new Bundle();
                    roomInfo.putString("RoomName", room.getRoomName());
                    roomInfo.putString("SSID", room.getSSID());
                    roomInfo.putString("Password", room.getPassword());
                    roomInfo.putStringArrayList("Appliances", room.getAppliances());
                    Navigation.findNavController(v).popBackStack(R.id.navigation_device_control, false);
                    Navigation.findNavController(v).navigate(R.id.navigation_device_control, roomInfo);
                }
            });

            gridLayout.addView(roomButton, params);
            even = !even;
        }
    }
}
