package org.chanwr.controller;

import org.apache.commons.io.IOUtils;
import org.chanwr.api.QueryApi;
import org.chanwr.service.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class QueryApiV1Controller implements QueryApi {

    private final QueryService queryService;

    @Autowired
    public QueryApiV1Controller(QueryService queryService) {
        this.queryService = queryService;
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
    @PostMapping(value = "/api/v1/query/{id}/parse", produces = MediaType.TEXT_PLAIN_VALUE)
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

}
