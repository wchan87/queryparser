package org.chanwr.service;

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
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.neo4j.driver.Values.parameters;

@Service
public class QueryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryService.class);

    private final Driver driver;

    @Autowired
    public QueryService(Driver driver) {
        this.driver = driver;
    }

    public int loadSqlFile(String name, String sql) {
        try (var session = driver.session()) {
            return session.run("MERGE (f:SqlFile {name: $name}) SET f.sql = $sql RETURN id(f)",
                    parameters("name", name, "sql", sql)).single().get("id(f)").asInt();
        }
    }

    public List<Map<String, Object>> getAll() {
        try (var session = driver.session()) {
            var result = session.run("MATCH (n:SqlFile) RETURN id(n), n.name, n.sql");
            return result.stream().map(this::createQueryModel)
                    .collect(Collectors.toList());
        }
    }

    public String generateAST(int id) {
        try (var session = driver.session()) {
            var result = session.run("MATCH (f:SqlFile) WHERE id(f) = $id RETURN f.sql", Map.of("id", id));
            // https://github.com/antlr/antlr4/blob/master/doc/case-insensitive-lexing.md#custom-character-streams-approach
            TSqlParser parser = getTSqlParser(result.single().get("f.sql").asString());
            // tsql_file is the root of the grammar
            ParseTree tree = parser.tsql_file();
            // https://github.com/antlr/antlr4/blob/antlr4-master-4.8-1/runtime/Java/src/org/antlr/v4/runtime/tree/Trees.java#L39
            List<String> ruleNames = Arrays.asList(parser.getRuleNames());
            // TODO log error on else
            if (parser.getNumberOfSyntaxErrors() == 0) {
                parseAndSaveToGraphDb(session, id, 0, ruleNames, tree);
            }
            // TODO cleanup with MATCH (n) WHERE any (x in labels(n) WHERE x <> 'SqlFile') DETACH DELETE n

            // https://stackoverflow.com/questions/23809005/how-to-display-antlr-tree-gui
            // openTreeViewer(ruleNames, tree);
            return printStringTree(ruleNames, tree);
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
                // TODO add source interval to indicate where the tag is applicable
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

    public Map<String, Object> get(int id) {
        try (var session = driver.session()) {
            var result = session.run("MATCH (n:SqlFile) WHERE id(n) = $id RETURN id(n), n.name, n.sql",
                    Map.of("id", id));
            var map = new HashMap<>(createQueryModel(result.single()));
            TSqlParser parser = getTSqlParser(map.get("sql").toString());
            ParseTree tree = parser.tsql_file();
            // https://github.com/antlr/antlr4/blob/antlr4-master-4.8-1/runtime/Java/src/org/antlr/v4/runtime/tree/Trees.java#L39
            List<String> ruleNames = Arrays.asList(parser.getRuleNames());
            map.put("ast", printStringTree(ruleNames, tree));
            return map;
        }
    }

    private TSqlParser getTSqlParser(String sql) {
        CharStream s = CharStreams.fromString(sql);
        CaseChangingCharStream upper = new CaseChangingCharStream(s, true);
        Lexer lexer = new TSqlLexer(upper);
        TokenStream tokens = new CommonTokenStream(lexer);
        return new TSqlParser(tokens);
    }

    private String printStringTree(List<String> ruleNames, ParseTree tree) {
        return Trees.toStringTree(tree, ruleNames);
    }

    private void openTreeViewer(List<String> ruleNames, ParseTree tree) {
        TreeViewer viewer = new TreeViewer(ruleNames, tree);
        viewer.open();
    }

    private Map<String, Object> createQueryModel(Record r) {
        return Map.of("id", r.get("id(n)").asInt(),
                "name", r.get("n.name").asString(),
                "sql", r.get("n.sql").asString());
    }

}
