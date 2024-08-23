package utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamGobbler extends Thread {

    BufferedReader br;
    String lineread = "";

    public StreamGobbler(InputStream is) {
        this.br = new BufferedReader(new
                InputStreamReader(is));
    }

    public void run() {
        try {
            while ((lineread = br.readLine()) != null) {
                System.out.println(lineread);
            }
        } catch (IOException ioe) {
            System.out.println("IOException: " + ioe.getMessage());
        }
    }
}