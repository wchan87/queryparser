package org.chanwr.controller;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.chanwr.api.QueryApi;
import org.chanwr.service.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class QueryApiV1Controller implements QueryApi {

    private final QueryService queryService;

    @Autowired
    public QueryApiV1Controller(QueryService queryService) {
        this.queryService = queryService;
    }

    @Override
    @PostMapping(value = "/api/v1/queries/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Integer>>> loadSQLQueriesFromFile(@RequestPart("files") MultipartFile[] files) {
        try {
            var inputs = Arrays.stream(files).map(this::getNameAndSqlFromFile).collect(Collectors.toList());
            return new ResponseEntity<>(inputs.stream().map(o -> queryService.loadSqlFile(o.getLeft(), o.getRight()))
                    .map(o -> Map.of("id", o)).collect(Collectors.toList()), HttpStatus.OK);
        } catch (RuntimeException ex) {
            // TODO there may be other RuntimeException so create a custom one
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    // https://dzone.com/articles/exception-handling-in-java-streams
    private Pair<String, String> getNameAndSqlFromFile(MultipartFile file) {
        try {
            return Pair.of(file.getOriginalFilename(), IOUtils.toString(file.getInputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @PostMapping(value = "/api/v1/query/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> loadSQLQueryFromFile(@RequestPart("file") MultipartFile file) {
        try {
            var name = file.getOriginalFilename();
            var sql = IOUtils.toString(file.getInputStream(), StandardCharsets.UTF_8);
            return new ResponseEntity<>(Map.of("id", queryService.loadSqlFile(name, sql)),
                    HttpStatus.OK);
        } catch (IOException ex) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    @PutMapping(value = "/api/v1/queries/parse", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, String>>> parseSQLsIntoASTs() {
        return new ResponseEntity<>(queryService.getInventory().stream()
                .map(queryService::generateAST)
                .map(o -> Map.of("ast", o))
                .collect(Collectors.toList()), HttpStatus.OK);
    }

    @Override
    @PutMapping(value = "/api/v1/query/{id}/parse", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> parseSQLIntoAST(@PathVariable(name = "id") int id) {
        return new ResponseEntity<>(queryService.generateAST(id), HttpStatus.OK);
    }

    @Override
    @GetMapping(value = "/api/v1/queries", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getSQLQueries() {
        return new ResponseEntity<>(queryService.getAll(), HttpStatus.OK);
    }

    @Override
    @GetMapping(value = "/api/v1/query/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getSQLQueryById(@PathVariable(name = "id") int id) {
        return new ResponseEntity<>(queryService.get(id), HttpStatus.OK);
    }

    @Override
    @DeleteMapping(value = "/api/v1/queries", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> deleteSQLQueries() {
        return new ResponseEntity<>(queryService.deleteAll(), HttpStatus.OK);
    }

    @Override
    @DeleteMapping(value = "/api/v1/query/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> deleteSqlQueryById(@PathVariable(name = "id") int id) {
        return new ResponseEntity<>(queryService.delete(id), HttpStatus.OK);
    }

}
