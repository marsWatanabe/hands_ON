package com.aojt.mgmt.controller;

import com.aojt.mgmt.dto.CenterRequest;
import com.aojt.mgmt.dto.CenterResponse;
import com.aojt.mgmt.dto.CenterSearchRequest;
import com.aojt.mgmt.dto.PageResponse;
import com.aojt.mgmt.service.InventoryCenterService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/centers")
public class InventoryCenterController {

    private final InventoryCenterService service;

    public InventoryCenterController(InventoryCenterService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<CenterResponse> list(@Valid @ParameterObject @ModelAttribute CenterSearchRequest request) {
        return service.search(request);
    }

    @PostMapping
    public CenterResponse create(@Valid @RequestBody CenterRequest request) {
        return service.create(request);
    }

    @PutMapping("/{centerId}")
    public CenterResponse update(@PathVariable Integer centerId, @Valid @RequestBody CenterRequest request) {
        return service.update(centerId, request);
    }

    @DeleteMapping("/{centerId}")
    public Map<String, String> delete(@PathVariable Integer centerId) {
        return service.delete(centerId);
    }
}
