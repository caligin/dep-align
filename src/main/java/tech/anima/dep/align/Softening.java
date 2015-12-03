package tech.anima.dep.align;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class Softening {

    public static URL url(String str) {
        try {
            return new URL(str);
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    public static JsonNode readTree(ObjectMapper om, URL url) {
        try {
            return om.readTree(url);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
