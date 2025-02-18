/**
 * Original work by Ola Aronsson 2020
 * Courtesy of nollettnoll AB &copy; 2012 - 2020
 * <p>
 * Licensed under the Creative Commons Attribution 4.0 International (the "License")
 * you may not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * <p>
 * https://creativecommons.org/licenses/by/4.0/
 * <p>
 * The software is provided “as is”, without warranty of any kind, express or
 * implied, including but not limited to the warranties of merchantability,
 * fitness for a particular purpose and noninfringement. In no event shall the
 * authors or copyright holders be liable for any claim, damages or other liability,
 * whether in an action of contract, tort or otherwise, arising from, out of or
 * in connection with the software or the use or other dealings in the software.
 */
package com.github.catchitcozucan.supervision.controllers;

import com.github.catchitcozucan.supervision.api.DemoMode;
import com.github.catchitcozucan.supervision.api.DepartmentProcessSummary;
import com.github.catchitcozucan.supervision.api.DepartmentSummary;
import com.github.catchitcozucan.supervision.api.DomainSummary;
import com.github.catchitcozucan.supervision.api.HierarchyResponse;
import com.github.catchitcozucan.supervision.api.Histogram;
import com.github.catchitcozucan.supervision.api.RequestKey;
import com.github.catchitcozucan.supervision.api.SourceDto;
import com.github.catchitcozucan.supervision.api.SourceTestResponseDto;
import com.github.catchitcozucan.supervision.exception.CatchitSupervisionRuntimeException;
import com.github.catchitcozucan.supervision.service.DataProcessingService;
import com.github.catchitcozucan.supervision.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/supervision")
@RequiredArgsConstructor
public class SupervisionController {

    private static final String JSON_CHARSET_UTF_8 = "application/json; charset=UTF-8";
    private static final String NO_DEP = "NO_DEP";
    private static final String NO_DOMAIN = "NO_DOMAIN";

    private final DataProcessingService dataProcessingService;

    @GetMapping(value = "/demoMode")
    public DemoMode isInDemoMode() {
        return DemoMode.builder().isDemoMode(dataProcessingService.weAreInDemoMode()).build();
    }

    @GetMapping(value = "/hierarchy")
    public HierarchyResponse getHierarchy() {
        return HierarchyResponse.builder().hierarchy(dataProcessingService.getHierarchy()).build();
    }

    @GetMapping(value = "/domainSummaries")
    public List<DomainSummary> getDomainSummaries() {
        return dataProcessingService.getDomainSummaries();
    }

    @GetMapping(value = "/getLastestResult/requestKey/{requestKey}/flip/{flip}/failOnly/{failonly}")
    public SourceTestResponseDto getLastResult(@PathVariable String requestKey, @PathVariable String flip, @PathVariable String failonly) {
        SourceTestResponseDto result = dataProcessingService.getLastResult(UUID.fromString(requestKey));
        Histogram histogram = result.getHistogram();
        Histogram histogramTrans = histogram.transForm(StringUtils.looksLikeTrue(flip), StringUtils.looksLikeTrue(failonly));
        return SourceTestResponseDto.builder().requestKey(result.getRequestKey()).lastSuccessfulFetch(result.getLastSuccessfulFetch()).state(result.getState()).updatedWhen(result.getUpdatedWhen()).execTime(result.getExecTime()).histogram(histogramTrans).build();
    }

    @GetMapping(value = "/departmentSummaries/domain/{domain}", produces = JSON_CHARSET_UTF_8)
    public List<DepartmentSummary> getDepartmentSummaries(@PathVariable(required = false, name = "domain") String domain) {
        if (!StringUtils.hasContents(domain)) {
            domain = NO_DOMAIN;
        }
        return dataProcessingService.getDepartmentSummaries(domain);
    }

    @GetMapping(value = "/departmentProcesses/domain/{domain}/department/{department}", produces = JSON_CHARSET_UTF_8)
    public List<DepartmentProcessSummary> getDepartmentProcesses(@PathVariable(required = false, name = "domain") String domain, @PathVariable(required = false, name = "department") String department) {
        if (!StringUtils.hasContents(domain)) {
            domain = NO_DOMAIN;
        }
        if (!StringUtils.hasContents(department)) {
            department = NO_DEP;
        }
        return dataProcessingService.getDepartmentProcesses(domain, department);
    }

    @GetMapping(value = "/histogram/domain/{domain}/department/{department}/process/{process}", produces = JSON_CHARSET_UTF_8)
    public Histogram getHistogram(@PathVariable(value = "domain", required = false) String domain, @PathVariable(value = "department", required = false) String department, @PathVariable(value = "process", required = false) String process, @RequestParam(value = "flipFailures", required = false, defaultValue = "false") String flipfailures, @RequestParam(value = "returnOnlyFailures", required =
            false, defaultValue = "false") String returnOnlyFailures) {

        boolean flipFailurez = false;
        boolean returnOnlyFailurez = false;
        if (StringUtils.looksLikeTrue(flipfailures)) {
            flipFailurez = true;
        }
        if (StringUtils.looksLikeTrue(returnOnlyFailures)) {
            returnOnlyFailurez = true;
        }
        if (!StringUtils.hasContents(domain)) {
            domain = NO_DOMAIN;
        }
        if (!StringUtils.hasContents(department)) {
            department = NO_DEP;
        }
        Optional<Histogram> possibleHistogram = dataProcessingService.getHistogram(domain, department, process, flipFailurez, returnOnlyFailurez);
        if (possibleHistogram.isPresent()) {
            return possibleHistogram.get();
        } else {
            return null;
        }
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping(value = "/sources")
    public List<SourceDto> getSources() {
        return dataProcessingService.getSources();
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping(value = "/testsource")
    public SourceTestResponseDto testSource(@RequestBody SourceDto sourceDto) {
        return dataProcessingService.testSource(sourceDto);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping(value = "/savesource")
    public SourceDto saveSource(@RequestBody SourceDto sourceToTest) {
        try {
            return dataProcessingService.saveSource(sourceToTest);
        } catch (Exception e) {
            throw new CatchitSupervisionRuntimeException(e);
        }
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping(value = "/deletesource/{key}")
    public RequestKey deleteSource(@PathVariable(required = false, name = "key") String key) {
        try {
            return dataProcessingService.deleteSource(key);
        } catch (Exception e) {
            throw new CatchitSupervisionRuntimeException(e);
        }
    }

}
