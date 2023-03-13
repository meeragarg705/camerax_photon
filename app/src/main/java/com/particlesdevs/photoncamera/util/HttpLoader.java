package com.particlesdevs.photoncamera.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpLoader {
    public static BufferedReader readURL(String addr,int timeout) throws IOException {
        URL supportedList = new URL(addr);
        HttpURLConnection conn = (HttpURLConnection) supportedList.openConnection();
        conn.setConnectTimeout(timeout); // timing out in a timeout
        return new BufferedReader(new InputStreamReader(conn.getInputStream()));
    }
}
