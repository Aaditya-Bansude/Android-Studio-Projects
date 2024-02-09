package com.example.unohome.data;

import java.util.ArrayList;

public class RoomEntity {
    public String roomName = "";
    public String ssid = "";
    public String password = "";
    public ArrayList<String> appliances = null;

    public RoomEntity(String roomName, String ssid, String password, ArrayList<String> appliances){
        this.roomName = roomName;
        this.ssid = ssid;
        this.password = password;
        this.appliances = appliances;
    }

    public String getRoomName(){
        return roomName;
    }
    public String getSSID(){
        return ssid;
    }
    public String getPassword() { return password; }
    public ArrayList<String> getAppliances(){ return appliances; }
}

