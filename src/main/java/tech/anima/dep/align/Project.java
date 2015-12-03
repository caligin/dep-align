package tech.anima.dep.align;

import java.util.List;

public class Project {

    public String groupId;
    public List<Dependency> dependencies;

    public static class Dependency {

        public String groupId;
        public String artifactId;
        public String version;
        public String scope;
    }

}
