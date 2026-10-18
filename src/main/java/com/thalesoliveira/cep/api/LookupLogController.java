package com.thalesoliveira.cep.api;

import com.thalesoliveira.cep.api.dto.LookupLogResponse;
import com.thalesoliveira.cep.application.LookupLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lookups")
@Tag(name = "Log de consultas")
public class LookupLogController {

    private final LookupLogService lookupLogService;

    public LookupLogController(LookupLogService lookupLogService) {
        this.lookupLogService = lookupLogService;
    }

    @GetMapping
    @Operation(summary = "Lista as últimas 50 consultas gravadas, com filtro opcional por CEP")
    public List<LookupLogResponse> recent(@RequestParam(required = false) String cep) {
        return lookupLogService.recent(cep).stream().map(LookupLogResponse::from).toList();
    }
}
