package org.chanwr;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.neo4j.driver.Values.parameters;

public class QueryParser implements AutoCloseable {

    private final Driver driver;

    public QueryParser(String uri, String username, String password) {
        driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password));
    }

    public void loadSqlFiles() throws IOException {
        try (Session session = driver.session()) {
            File tsqlDir = new File(getClass().getClassLoader().getResource("tsql").getFile());
            if (tsqlDir.isDirectory()) {
                for (File f : tsqlDir.listFiles()) {
                    var sql = Files.readString(Paths.get(f.getPath()));
                    session.run("MERGE (f:SqlFile {name: $name}) SET f.sql = $sql",
                            parameters("name", f.getName(), "sql", sql));
                }
            }
        }
    }

    public void parseSqls() {
        try (Session session = driver.session()) {
            var result = session.run("MATCH (f:SqlFile) RETURN f.sql");
            result.forEachRemaining(o -> {
                o.get("f.sql");
            });
        }
    }

    @Override
    public void close() throws Exception {
        driver.close();
    }
}
