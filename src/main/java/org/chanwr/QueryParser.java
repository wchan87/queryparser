package org.chanwr;

import org.antlr.v4.gui.TreeViewer;
import org.antlr.v4.runtime.CaseChangingCharStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.Trees;
import org.chanwr.parser.TSqlLexer;
import org.chanwr.parser.TSqlParser;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
            var result = session.run("MATCH (f:SqlFile) RETURN id(f), f.sql");
            result.forEachRemaining(o -> {
                // https://github.com/antlr/antlr4/blob/master/doc/case-insensitive-lexing.md#custom-character-streams-approach
                CharStream s = CharStreams.fromString(o.get("f.sql").asString());
                CaseChangingCharStream upper = new CaseChangingCharStream(s, true);
                Lexer lexer = new TSqlLexer(upper);
                TokenStream tokens = new CommonTokenStream(lexer);
                TSqlParser parser = new TSqlParser(tokens);
                // tsql_file is the root of the grammar
                ParseTree tree = parser.tsql_file();
                // https://github.com/antlr/antlr4/blob/antlr4-master-4.8-1/runtime/Java/src/org/antlr/v4/runtime/tree/Trees.java#L39
                List<String> ruleNames = Arrays.asList(parser.getRuleNames());
                // TODO log error on else
                if (parser.getNumberOfSyntaxErrors() == 0) {
                    parseAndSaveToGraphDb(session, o.get("id(f)").asInt(), 0, ruleNames, tree);
                }
                // TODO cleanup with MATCH (n) WHERE any (x in labels(n) WHERE x <> 'SqlFile') DETACH DELETE n

                // https://stackoverflow.com/questions/23809005/how-to-display-antlr-tree-gui
                // printStringTree(ruleNames, tree);
                // openTreeViewer(ruleNames, tree);
            });
        }
    }

    private void parseAndSaveToGraphDb(Session session, int id, int order, List<String> ruleNames, ParseTree tree) {
        String text = Trees.getNodeText(tree, ruleNames);
        if (tree instanceof RuleContext) {
            // insert current node and return
            int currentId = session.run("MATCH (n) WHERE id(n) = $id CREATE (m:tsql_" + text +
                            ")-[r:IS_CHILD_OF]->(n) SET r.order = $order RETURN id(m)",
                    Map.of("id", id, "order", order)).single().get("id(m)").asInt();
            if (tree.getChildCount() != 0 && IntStream.range(0, tree.getChildCount())
                    .noneMatch(i -> tree.getChild(i) instanceof RuleContext)) {
                String currentText = IntStream.range(0, tree.getChildCount())
                        .mapToObj(i -> Trees.getNodeText(tree.getChild(i), ruleNames))
                        .collect(Collectors.joining());
                // removes an extra level of nesting of a terminal node
                session.run("MATCH (n) WHERE id(n) = $id SET n.text = $text",
                        Map.of("id", currentId, "text", currentText));
            } else {
                for (int i = 0; i < tree.getChildCount(); i++) {
                    parseAndSaveToGraphDb(session, currentId, i, ruleNames, tree.getChild(i));
                }
            }
        } else {
            // TODO appears to have no practical value
            /*
            int currentId = session.run("MATCH (n) WHERE id(n) = $id CREATE (m:tsql_VALUE)-[r:IS_CHILD_OF]->(n) SET r.order = $order, m.text = $text RETURN id(m)",
                    Map.of("id", id, "order", order, "text", text)).single().get("id(m)").asInt();
            for (int i = 0; i < tree.getChildCount(); i++) {
                parseAndSaveToGraphDb(session, currentId, i, ruleNames, tree.getChild(i));
            }
             */
        }
    }

    private void printStringTree(List<String> ruleNames, ParseTree tree) {
        System.out.println(Trees.toStringTree(tree, ruleNames));
    }

    private void openTreeViewer(List<String> ruleNames, ParseTree tree) {
        TreeViewer viewer = new TreeViewer(ruleNames, tree);
        viewer.open();
    }

    @Override
    public void close() throws Exception {
        driver.close();
    }
}
