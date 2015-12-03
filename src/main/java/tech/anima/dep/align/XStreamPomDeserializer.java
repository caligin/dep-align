package tech.anima.dep.align;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.mapper.MapperWrapper;
import java.net.URL;
import tech.anima.dep.align.Project.Dependency;

public class XStreamPomDeserializer implements PomDeserializer {

    private final XStream xStream = new XStream() {
        @Override
        protected MapperWrapper wrapMapper(MapperWrapper next) {
            return new MapperWrapper(next) {
                @Override
                public boolean shouldSerializeMember(Class definedIn, String fieldName) {
                    if (definedIn == Object.class) {
                        return false;
                    }
                    return super.shouldSerializeMember(definedIn, fieldName);
                }
            };
        }
    };

    public XStreamPomDeserializer() {
        xStream.alias("project", Project.class);
        xStream.alias("dependency", Dependency.class);
    }

    public Project deserializeFromUrl(URL url) {
        return (Project) xStream.fromXML(url);
    }

}
