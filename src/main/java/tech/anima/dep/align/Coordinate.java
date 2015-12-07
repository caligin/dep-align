package tech.anima.dep.align;

import java.util.Objects;

public class Coordinate {

    public final GroupId groupId;
    public final ArtifactId artifactId;
    public final Version version;

    public Coordinate(GroupId groupId, ArtifactId artifactId, Version version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.groupId, this.artifactId, this.version);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Coordinate == false) {
            return false;
        }
        final Coordinate other = (Coordinate) obj;
        return Objects.equals(this.groupId, other.groupId)
                && Objects.equals(this.artifactId, other.artifactId)
                && Objects.equals(this.version, other.version);
    }

}
