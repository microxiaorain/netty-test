package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

public class CurlDataGenerator {
    public static void main(String[] args) throws IOException {
        File file = new File("/Users/microxiaorain/Documents/curl.txt");
        FileWriter fw = new FileWriter(file);
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String line = null;
        while( !"end".equals((line = br.readLine()))) {
            line = line.replace("\"", "\\\"");
            fw.write(line + "\n");
            fw.flush();
        }
        fw.close();
    }
}
