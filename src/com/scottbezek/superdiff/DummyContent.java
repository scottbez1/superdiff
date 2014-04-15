package com.scottbezek.superdiff;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import android.content.res.Resources;

public class DummyContent {

    public static String[] readLines(Resources resources, int rawResourceId) {
        InputStream inputStream = resources.openRawResource(rawResourceId);
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            List<String> lines = new ArrayList<String>();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                lines.add(line);
            }
            return lines.toArray(new String[lines.size()]);
        } catch (IOException e) {
            throw new RuntimeException("Uh oh", e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {}
        }
    }

    public static Scanner getScanner(Resources resources, int rawResourceId) {
        return new Scanner(resources.openRawResource(rawResourceId));
    }
}
