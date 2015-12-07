package tech.anima.dep.align;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.github.mustachejava.resolver.ClasspathResolver;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.junit.Test;
import tech.anima.tinytypes.jackson.TinyTypesModule;

public class MainTest {

    @Test
    public void bah() throws MalformedURLException, JsonProcessingException, IOException {
        PomDeserializer pd = new XStreamPomDeserializer();
// probably want to resolve whole tree as seeing conflicts of declared dep on common framework might be interesting
        List<Pair<Coordinate, List<Coordinate>>> collect = Stream.of(
                new Pair<>(new GroupId("tech.anima"), new ArtifactId("tinytypes")),
                new Pair<>(new GroupId("tech.anima"), new ArtifactId("tinytypes-jersey")),
                new Pair<>(new GroupId("tech.anima"), new ArtifactId("jackson-datatype-tinytypes")),
                new Pair<>(new GroupId("tech.anima"), new ArtifactId("tinytypes-testing")),
                new Pair<>(new GroupId("tech.anima"), new ArtifactId("tinytypes-meta")))
                .map(ga -> new Coordinate(ga.fst, ga.snd, discovermostRecentlyDeployedVersion(ga.fst, ga.snd)))
                .map(a -> new Pair<>(a, compileDependenciesFor(pd, a)))
                .collect(Collectors.toList());
        final String dependencies = new ObjectMapper().registerModule(new TinyTypesModule()).writeValueAsString(collect);

        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile("index.mustache");

        LinesIterator d3Lines = new LinesIterator(new BufferedReader(new ClasspathResolver().getReader("lib/d3.min.js")));
        String d3Min = StreamSupport.stream(Dysfunctional.oneTimeIterable(d3Lines).spliterator(), false).collect(Collectors.joining("\n"));
        final File outFile = new File("/tmp/dep-align.html");

        Files.deleteIfExists(outFile.toPath());

        mustache.execute(new PrintWriter(outFile), new PageContext(d3Min, dependencies)).flush();
    }

    public static class PageContext {

        public final String d3;
        public final String dependencies;

        public PageContext(String d3, String dependencies) {
            this.d3 = d3;
            this.dependencies = dependencies;
        }

    }

    public static Version discovermostRecentlyDeployedVersion(GroupId g, ArtifactId a) {
        final String query = String.format("http://search.maven.org/solrsearch/select?q=g:%%22%s%%22+AND+a:%%22%s%%22&core=gav&rows=20&wt=json", g.value, a.value);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode tree = Softening.readTree(objectMapper, Softening.url(query));
        JsonNode response = tree.get("response");
        Iterator<JsonNode> docs = response.get("docs").elements();
        return StreamSupport.stream(Dysfunctional.oneTimeIterable(docs).spliterator(), false)
                .max((lhs, rhs) -> Instant.ofEpochSecond(lhs.get("timestamp").asLong()).compareTo(Instant.ofEpochSecond(rhs.get("timestamp").asLong())))
                .map(doc -> doc.get("v").asText())
                .map(Version::new)
                .get();
    }

    private static List<Coordinate> compileDependenciesFor(PomDeserializer pd, Coordinate artifact) {
        URL url = Softening.url(String.format("https://repo1.maven.org/maven2/%s/%s/%s/%s-%s.pom", artifact.groupId.value.replace(".", "/"), artifact.artifactId.value, artifact.version.value, artifact.artifactId.value, artifact.version.value));
        Project unmarshal = pd.deserializeFromUrl(url);

        /// waaahhhh, version might not be there if specified in dependencyManagement!!! must resolve effective pom... /cry
        return unmarshal.dependencies.stream()
                .filter(d -> d.scope == null || "compile".equals(d.scope))
                .map(d -> new Coordinate(new GroupId(d.groupId), new ArtifactId(d.artifactId), new Version(d.version)))
                .collect(Collectors.toList());

    }


}
