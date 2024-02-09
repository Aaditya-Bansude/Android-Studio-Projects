package com.example.unohome.ui.addroom;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
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

public class AddroomFragment extends Fragment {

    private ArrayList<String> appliances;
    private ArrayList<CheckboxValue> checkboxValues;
    private ArrayList<RoomEntity> roomData = new ArrayList<>();
    Type RoomListType = new TypeToken<ArrayList<RoomEntity>>() {}.getType();

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        AddroomViewModel AddroomViewModel = ViewModelProviders.of(this).get(AddroomViewModel.class);
        View root = inflater.inflate(R.layout.fragment_addroom, container, false);
        EditText inputRoomName = root.findViewById(R.id.input_room_name);
        AddroomViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                //textView.setText(s);
            }
        });

        appliances = new ArrayList<>();
        checkboxValues = new ArrayList<>();

        int[] checkboxIds = {R.id.checkbox_appliance1, R.id.checkbox_appliance2, R.id.checkbox_appliance3,
                R.id.checkbox_appliance4, R.id.checkbox_appliance5, R.id.checkbox_appliance6};
        int[] spinnerIds = {R.id.spinner_quantity1, R.id.spinner_quantity2, R.id.spinner_quantity3,
                R.id.spinner_quantity4, R.id.spinner_quantity5, R.id.spinner_quantity6};
        Button doneButton = root.findViewById(R.id.done_button);

        for (int i = 0; i < checkboxIds.length; i++) {
            final CheckBox checkBox = root.findViewById(checkboxIds[i]);
            final Spinner spinner = root.findViewById(spinnerIds[i]);

            CheckboxValue checkboxValue = new CheckboxValue();
            checkboxValue.setCheckBox(checkBox);
            checkboxValue.setValue(0);
            checkboxValues.add(checkboxValue);

            ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(requireContext(), R.array.quantities, android.R.layout.simple_spinner_dropdown_item);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(spinnerAdapter);
            spinner.setEnabled(false);

            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    spinner.setEnabled(isChecked);
                }
            });

            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    checkboxValue.setValue(Integer.parseInt(parent.getItemAtPosition(position).toString()));
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String roomName = inputRoomName.getText().toString();
                if (!roomName.equals("")){
                    if(isAvailable(roomName)) {
                        for (CheckboxValue checkboxValue : checkboxValues) {
                            if (checkboxValue.getCheckBox().isChecked()) {
                                for (int i = 0; i < checkboxValue.getValue(); i++) {
                                    appliances.add(checkboxValue.getCheckBox().getText().toString() + " " + (i + 1));
                                }
                            }
                        }
                        if (appliances.size() > 0){
                            Bundle bundle = new Bundle();
                            bundle.putString("RoomName", roomName);
                            bundle.putStringArrayList("Appliances", appliances);
                            Navigation.findNavController(v).popBackStack(R.id.navigation_wifi_scan, false);
                            Navigation.findNavController(v).navigate(R.id.navigation_wifi_scan, bundle);
                        } else {
                            Toast.makeText(requireContext(), "Please select Appliances", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(requireContext(), "Room Name already taken", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "Please provide Room Name", Toast.LENGTH_SHORT).show();
                }
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

    private boolean isAvailable(String roomName){
        loadFromInternalStorage();
        for (RoomEntity room : roomData){
            if (roomName.equals(room.getRoomName())){
                return false;
            }
        }
        return true;
    }

    public static class CheckboxValue {
        private CheckBox checkBox;
        private int value;
        public void setCheckBox(CheckBox checkBox){
            this.checkBox = checkBox;
        }
        public CheckBox getCheckBox() {
            return checkBox;
        }
        public void setValue(int value){
            this.value = value;
        }
        public int getValue() {
            return value;
        }
    }
}
