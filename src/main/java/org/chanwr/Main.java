package org.chanwr;

public class Main {

    public static void main(String[] args) throws Exception {
        try (QueryParser parser = new QueryParser("bolt://localhost:7687", "neo4j", "Passw0rd!")) {
            parser.loadSqlFiles();
            parser.parseSqls();
        }
    }

}
