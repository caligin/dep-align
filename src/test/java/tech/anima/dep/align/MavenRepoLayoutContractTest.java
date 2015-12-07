package tech.anima.dep.align;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Assert;
import org.junit.Test;
import tech.anima.dep.align.MavenRepoLayoutContractTest.DeclaredDependency.Scope;

public class MavenRepoLayoutContractTest {

    private static final Coordinate ttCoords = new Coordinate(new GroupId("tech.anima"), new ArtifactId("tinytypes"), new Version("1.1.0"));

    @Test
    public void canQueryForVersions() {
        final MavenRepository repo = new CentralMavenRepository();
        final List<Coordinate> available = repo.availableVersions(ttCoords.groupId, ttCoords.artifactId);
        final List<Version> availableVersions = available.stream().map(c -> c.version).collect(Collectors.toList());
        Assert.assertTrue(availableVersions.contains(ttCoords.version));
    }

    @Test
    public void canQueryForLatest() {
        final MavenRepository repo = new CentralMavenRepository();
        final Optional<Coordinate> latest = repo.latestVersion(ttCoords.groupId, ttCoords.artifactId);
        Assert.assertEquals(ttCoords, latest.get());
    }

    @Test
    public void canRetrievePom() {
        final MavenRepository repo = new CentralMavenRepository();
        final Optional<POM> pomForTT = repo.pomFor(ttCoords);
        Assert.assertTrue(pomForTT.isPresent());
    }

    @Test
    public void canReadDeclaredDependencies() {
        final MavenRepository repo = new CentralMavenRepository();
        final List<DeclaredDependency> declaredDependencies = repo.pomFor(ttCoords).get().declaredDependencies();
        final DeclaredDependency junitInTestScope = new DeclaredDependency(new GroupId("junit"), new ArtifactId("junit"), new Version("4.12"), Scope.TEST);
        Assert.assertTrue(declaredDependencies.contains(junitInTestScope));
    }

    @Test
    public void retrievePomYieldsEmptyWhenNotFound() throws MalformedURLException, JsonProcessingException, IOException {
        final MavenRepository repo = new CentralMavenRepository();
        final Coordinate coordsOfNothing = new Coordinate(new GroupId("nope.nope"), new ArtifactId("nope"), new Version("0.0.0-nope"));
        final Optional<POM> pomForTT = repo.pomFor(coordsOfNothing);
        Assert.assertFalse(pomForTT.isPresent());
    }

    public interface MavenRepository {

        Optional<POM> pomFor(Coordinate coordinates);

        List<Coordinate> availableVersions(GroupId groupId, ArtifactId artifactId);

        Optional<Coordinate> latestVersion(GroupId groupId, ArtifactId artifactId);

    }

    public interface POM {

        List<DeclaredDependency> declaredDependencies();

    }

    public static class DeclaredDependency {

        public static enum Scope {

            COMPILE, TEST, RUNTIME, SYSTEM, PROVIDED;
        }
        public final GroupId groupId;
        public final ArtifactId artifactId;
        // TODO: this is optional, might be specified in dependencyManagement or in parent
        public final Version version;
        public final Scope scope;

        public DeclaredDependency(GroupId groupId, ArtifactId artifactId, Version version, Scope scope) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.scope = scope;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.groupId, this.artifactId, this.version, this.scope);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof DeclaredDependency == false) {
                return false;
            }
            final DeclaredDependency other = (DeclaredDependency) obj;
            return Objects.equals(this.groupId, other.groupId)
                    && Objects.equals(this.artifactId, other.artifactId)
                    && Objects.equals(this.version, other.version)
                    && Objects.equals(this.scope, other.scope);
        }

    }

    public static class JsoupBackedPOM implements POM {

        private final Document document;

        public JsoupBackedPOM(Document document) {
            if (document == null) {
                throw new IllegalArgumentException("document cannot be null");
            }
            this.document = document;
        }

        @Override
        public List<DeclaredDependency> declaredDependencies() {
            return document.select("project > dependencies > dependency").stream()
                    .map(JsoupBackedPOM::deserializeDeclaredDependency)
                    .collect(Collectors.toList());
        }

        private static DeclaredDependency deserializeDeclaredDependency(Element element) {
            final GroupId groupId = new GroupId(element.select("groupId").text());
            final ArtifactId artifactId = new ArtifactId(element.select("artifactId").text());
            final Version version = new Version(element.select("version").text());
            final Scope scope = Scope.valueOf(element.select("scope").text().toUpperCase());
            return new DeclaredDependency(groupId, artifactId, version, scope);
        }

    }

    public static class CentralMavenRepository implements MavenRepository {

        private static final String baseUri = "https://repo1.maven.org/maven2/";
        private static final List<String> blacklisted = Arrays.asList("../", "maven-metadata.xml", "maven-metadata.xml.md5", "maven-metadata.xml.sha1");

        @Override
        public Optional<POM> pomFor(Coordinate coordinates) {
            final String filename = String.format("%s-%s.pom", coordinates.artifactId.value, coordinates.version.value);
            final String[] groupIdPath = coordinates.groupId.value.split("\\.");
            final String pomUrl = Stream.concat(Stream.concat(Stream.of(baseUri), Stream.of(groupIdPath)), Stream.of(coordinates.artifactId.value, coordinates.version.value, filename)).collect(Collectors.joining("/"));
            try {
                final Document pomDocument = Jsoup.connect(pomUrl).get();
                return Optional.of(new JsoupBackedPOM(pomDocument));
            } catch (HttpStatusException ex) {
                if (ex.getStatusCode() == 404) {
                    return Optional.empty();
                }
                throw new RuntimeException(ex);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public List<Coordinate> availableVersions(GroupId groupId, ArtifactId artifactId) {
            final String[] groupIdPath = groupId.value.split("\\.");
            final String versionsPageUrl = Stream.concat(Stream.concat(Stream.of(baseUri), Stream.of(groupIdPath)), Stream.of(artifactId.value, "")).collect(Collectors.joining("/"));
            try {
                final Document versionsDocument = Jsoup.connect(versionsPageUrl).get();
                return versionsDocument.select("body a").stream()
                        .map(Dysfunctional.curryMethod(Element::attr, "href"))
                        .filter(Dysfunctional.not(blacklisted::contains))
                        .map(versionHref -> versionHref.replace("/", ""))
                        .map(Version::new)
                        .map(v -> new Coordinate(groupId, artifactId, v))
                        .collect(Collectors.toList());
            } catch (HttpStatusException ex) {
                if (ex.getStatusCode() == 404) {
                    return Collections.emptyList();
                }
                throw new RuntimeException(ex);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public Optional<Coordinate> latestVersion(GroupId groupId, ArtifactId artifactId) {
            final String[] groupIdPath = groupId.value.split("\\.");
            final String versionsPageUrl = Stream.concat(Stream.concat(Stream.of(baseUri), Stream.of(groupIdPath)), Stream.of(artifactId.value, "")).collect(Collectors.joining("/"));
            try {
                final Document versionsDocument = Jsoup.connect(versionsPageUrl).get();
                return versionsDocument.select("body a").stream()
                        .map(Dysfunctional.curryMethod(Element::attr, "href"))
                        .filter(Dysfunctional.not(blacklisted::contains))
                        .map(versionHref -> versionHref.replace("/", ""))
                        .map(com.github.zafarkhaja.semver.Version::valueOf)
                        .max(com.github.zafarkhaja.semver.Version::compareTo)
                        .map(semver -> new Coordinate(groupId, artifactId, new Version(semver.toString())));
            } catch (HttpStatusException ex) {
                if (ex.getStatusCode() == 404) {
                    return Optional.empty();
                }
                throw new RuntimeException(ex);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

        }

    }

}
