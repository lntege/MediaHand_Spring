package com.intege.mediahand.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class FetchingUtil {

    public static URL followRedirects(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setInstanceFollowRedirects(false); // Do not automatically follow redirects
        connection.connect();

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
            String redirectUrl = connection.getHeaderField("Location");
            return followRedirects(new URL(redirectUrl)); // Recursively follow redirects
        }

        return url; // Return the final URL
    }

    public static File downloadFileFromUrl(String fileUrl) throws IOException {
        File file = new File("tmp");
        URL url = new URL(fileUrl);
        try (BufferedInputStream in = new BufferedInputStream(url.openStream()); FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
            return file;
        }
    }

}
