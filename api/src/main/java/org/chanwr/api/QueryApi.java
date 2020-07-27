package org.chanwr.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.InputStreamSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "query", description = "Query API")
public interface QueryApi {

    @Operation(summary = "Load SQL query from file", description = "Load SQL query from file")
    ResponseEntity<Map<String, Object>> loadSQLQueryFromFile(MultipartFile file);

    @Operation(summary = "Generate AST for SQL query", description = "Generate AST for SQL query")
    ResponseEntity<String> parseSQLIntoAST(int id);

    @Operation(summary = "Get SQL queries", description = "Get SQL queries")
    ResponseEntity<List<Map<String, Object>>> getSQLQueries();

    @Operation(summary = "Get SQL query by id", description = "Get SQL query by id")
    ResponseEntity<Map<String, Object>> getSQLQueryById(int id);

}
