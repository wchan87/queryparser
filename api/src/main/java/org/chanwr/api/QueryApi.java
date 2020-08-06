package org.chanwr.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "query", description = "Query API")
public interface QueryApi {

    @Operation(summary = "Load SQL queries from files", description = "Load SQL queries from files")
    ResponseEntity<List<Map<String, Integer>>> loadSQLQueriesFromFile(MultipartFile[] files);

    @Operation(summary = "Load SQL query from file", description = "Load SQL query from file")
    ResponseEntity<Map<String, Object>> loadSQLQueryFromFile(MultipartFile file);

    @Operation(summary = "Generate ASTs for SQL queries", description = "Generate ASTs for SQL queries")
    ResponseEntity<List<Map<String, String>>> parseSQLsIntoASTs();

    @Operation(summary = "Generate AST for SQL query", description = "Generate AST for SQL query")
    ResponseEntity<String> parseSQLIntoAST(int id);

    @Operation(summary = "Get SQL queries", description = "Get SQL queries")
    ResponseEntity<List<Map<String, Object>>> getSQLQueries();

    @Operation(summary = "Get SQL query by id", description = "Get SQL query by id")
    ResponseEntity<Map<String, Object>> getSQLQueryById(int id);

    @Operation(summary = "Delete SQL queries", description = "Delete SQL queries")
    ResponseEntity<Map<String, Object>> deleteSQLQueries();

    @Operation(summary = "Delete SQL query by id", description = "Delete SQL query by id")
    ResponseEntity<Map<String, Object>> deleteSqlQueryById(int id);
}
