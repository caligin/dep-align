package tech.anima.dep.align;

import java.net.URL;

interface PomDeserializer {

    Project deserializeFromUrl(URL url);
}
