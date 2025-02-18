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
package com.github.catchitcozucan.supervision.service;

import com.github.catchitcozucan.core.impl.CatchIt;
import com.github.catchitcozucan.core.impl.TaskBase;
import com.github.catchitcozucan.core.interfaces.IsolationLevel;
import com.github.catchitcozucan.supervision.api.RequestKey;
import com.github.catchitcozucan.supervision.api.SourceDto;
import com.github.catchitcozucan.supervision.exception.CatchitSupervisionRuntimeException;
import com.github.catchitcozucan.supervision.repository.SourceDetailHeaderRepository;
import com.github.catchitcozucan.supervision.repository.SourceRepository;
import com.github.catchitcozucan.supervision.repository.SourcelResponseResultRepository;
import com.github.catchitcozucan.supervision.repository.enteties.SourceDetailEntity;
import com.github.catchitcozucan.supervision.repository.enteties.SourceHeaderEntity;
import com.github.catchitcozucan.supervision.repository.enteties.SourcelEntity;
import com.github.catchitcozucan.supervision.repository.enteties.SourcelResponseResultEntity;
import com.github.catchitcozucan.supervision.service.conversion.SourceConversionService;
import com.github.catchitcozucan.supervision.utils.IOUtils;
import com.github.catchitcozucan.supervision.utils.StringUtils;
import com.github.catchitcozucan.supervision.utils.json.GsonWrapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ThreadUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.github.catchitcozucan.supervision.exception.ErrorCodes.*;
import static com.github.catchitcozucan.supervision.service.DataProcessingService.*;
import static com.github.catchitcozucan.supervision.utils.IOUtils.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SourcesProvider {
    private static final String COULD_NOT_LOCATE_CONFIG_RESOURCE_S = "Could not locate config resource %s";
    private static final String YIKES_I_HAVE_NO_SOURCES = "Yikes - I have no sources!";
    private static final String DOT = ".";
    private static final String WE_WILL_DELETING_SOURCE_S = "We will be deleting source %s";
    private static final String WE_HAVE_SAVED_SOURCE_S = "We have saved the new source %s";
    private static final String AT = "@";

    private List<SourceDto> demoSources = new ArrayList<>();
    private boolean weAreInDemoMode;

    @Value("${catchit.demoResource}")
    private Resource demoResource;

    private final SourceRepository sourceRepository;
    private final SourcelResponseResultRepository sourcelResponseResultRepository;
    private final SourceConversionService sourceConversionService;
    private final HistogramFetcher histogramFetcher;
    private final SourceDetailHeaderRepository sourceDetailHeaderRepository;
    private final GsonWrapper gson;

    public static final String TASK = "_TASK";
    private static final String DEMO_MODE_IS_ACTIVATED_ALL_HISTOGRAMS_WILL_BE_GENERATED = "Demo mode is activated - all histograms will be generated!";
    private Object deleteLock = new Object();

    @PostConstruct
    public void init() {
        try {
            List<SourcelEntity> sources = sourceRepository.findAllByDisabledIsFalse();
            if (sources.isEmpty()) {
                setInDemoMode(true);
                Optional<InputStream> in = resourceToStream(demoResource);
                if (in.isPresent()) {
                    addDemoSources(gson.toObjects(StringUtils.fromStreamCloseUponFinish(in.get()), SourceDto.class));
                    IOUtils.closeQuietly(in.get());
                } else {
                    throw new CatchitSupervisionRuntimeException(String.format(COULD_NOT_LOCATE_CONFIG_RESOURCE_S, demoResource.getFilename()), COULD_NOT_LOCATE_STAT_SOURCES);
                }
            } else {
                setInDemoMode(false);
            }
        } catch (Exception e) {
            throw new CatchitSupervisionRuntimeException(YIKES_I_HAVE_NO_SOURCES, e, COULD_NOT_LOCATE_STAT_SOURCES);
        }
    }

    public List<SourceDto> provideSourcesPerDomainAndDepartment(String domain, String department) {
        return getSourcesNonDisabled().stream().filter(s -> {
            if (s.getDepartment().equalsIgnoreCase(department) && s.getDomain().equalsIgnoreCase(domain)) {
                return true;
            } else {
                return false;
            }
        }).collect(Collectors.toList());
    }

    public List<SourceDto> provideSourcesPerDomain(String domain) {
        return getSourcesNonDisabled().stream().filter(s -> {
            if (s.getDomain().equalsIgnoreCase(domain)) {
                return true;
            } else {
                return false;
            }
        }).collect(Collectors.toList());
    }

    public Optional<SourceDto> provideSourcesPerDomainAndDepartmentAndProcess(String domain, String department, String processName) {
        return getSourcesNonDisabled().stream().filter(s -> {
            if (s.getProcessName().equalsIgnoreCase(processName) && s.getDepartment().equalsIgnoreCase(department) && s.getDomain().equalsIgnoreCase(domain)) {
                return true;
            } else {
                return false;
            }
        }).findFirst();
    }

    public void refreshResults() {
        List<Integer> toDelete = new ArrayList<>();
        synchronized (deleteLock) {
            getAllSources().stream().forEach(source -> {
                if (!source.isDisabled()) {
                    CatchIt.getInstance().submitTask(new TaskBase() {
                        @Override
                        public String name() {
                            String key = source.toString();
                            if (source.getRequestKey() != null && StringUtils.hasContents(source.getRequestKey().getKey())) {
                                key = source.getRequestKey().getKey();
                            }
                            return new StringBuilder(key).append(TASK).toString();
                        }

                        @Override
                        public void run() {
                            histogramFetcher.fetchHistogram(UUID.fromString(source.getRequestKey().getKey()), HistogramFetcher.AccessType.WRITE);
                        }

                        @Override
                        public IsolationLevel.Level provideIsolationLevel() {
                            return IsolationLevel.Level.KIND_EXCLUSIVE;
                        }
                    });
                } else {
                    final UUID uuid = UUID.fromString(source.getRequestKey().getKey());
                    Optional<SourcelEntity> sourceEntity = sourceRepository.findFirstByAccessKey(uuid);
                    if (sourceEntity.isPresent()) {
                        toDelete.add(sourceEntity.get().getId());
                    }
                }
            });
        }

        if (!toDelete.isEmpty()) {
            synchronized (deleteLock) {
                waitUntilCatchItIsNotExecuting();
                List<SourcelEntity> sourcesToDelete = sourceRepository.findAllById(toDelete);
                sourcesToDelete.stream().forEach(source -> {
                    SourceDetailEntity detail = source.getSourceDetailEntity();
                    detail.setHeaders(null);
                    source.removeDetail();
                    Optional<SourcelResponseResultEntity> result = sourcelResponseResultRepository.findFirstByAccessKey(source.getAccessKey());
                    if (result.isPresent()) {
                        sourcelResponseResultRepository.delete(result.get());
                    }
                    log.info(String.format(WE_WILL_DELETING_SOURCE_S, getSourceInfo(source)));
                });
                sourceRepository.deleteAllById(toDelete);
            }
        }
    }

    public boolean isInDemoMode() {
        return weAreInDemoMode;
    }

    public void setInDemoMode(boolean inDemoMode) {
        this.weAreInDemoMode = inDemoMode;
        if (inDemoMode) {
            log.info(DEMO_MODE_IS_ACTIVATED_ALL_HISTOGRAMS_WILL_BE_GENERATED);
        }
    }

    public void addDemoSources(Collection<SourceDto> sources) {
        sources.stream().forEach(s -> {
            if (s.getDomain() == null) {
                s.setDomain(NO_DOMAIN);
            }
            if (s.getDepartment() == null) {
                s.setDepartment(NO_DEPARTMENT);
            }
            if (!weAreInDemoMode) {
                s.setState(SourceDto.State.INITIALIZED);
            } else {
                s.setState(SourceDto.State.DEMO_MODE);
            }
        });
        demoSources.addAll(sources);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SourceDto saveSource(SourceDto sourceDto) {
        SourcelEntity saved;
        if (sourceDto.getRequestKey() != null || sourceDto.getId() != null) {
            Optional<SourcelEntity> entityPoss = sourceRepository.findFirstByAccessKey(UUID.fromString(sourceDto.getRequestKey().getKey()));
            if (entityPoss.isPresent()) {
                SourcelEntity entity = entityPoss.get();
                SourcelEntity entityNew = convertAndSaveHeaders(sourceDto);
                entity.override(entityNew);
                saved = sourceRepository.saveAndFlush(entity);
            } else {
                saved = sourceRepository.saveAndFlush(convertAndSaveHeaders(sourceDto));
            }
        } else {
            saved = sourceRepository.saveAndFlush(convertAndSaveHeaders(sourceDto));
        }
        try {
            setInDemoMode(false);
            if (StringUtils.hasContents(saved.getAccessUrl())) {
                log.info(String.format(WE_HAVE_SAVED_SOURCE_S, getSourceInfo(saved)));
            }
            return sourceConversionService.convertForward(saved);
        } catch (Exception e) {
            String errMsg = String.format("Failed to save source %s", sourceDto.getRequestKey().getKey());
            CatchitSupervisionRuntimeException t = new CatchitSupervisionRuntimeException(errMsg, e);
            log.error(errMsg, t);
            throw t;
        }
    }

    private SourcelEntity convertAndSaveHeaders(SourceDto sourceDto) {
        SourcelEntity converted = sourceConversionService.convertBackward(sourceDto);
        if (converted.getSourceDetailEntity().getHeaders() != null && !converted.getSourceDetailEntity().getHeaders().isEmpty()) {
            List<SourceHeaderEntity> headers = converted.getSourceDetailEntity().getHeaders();
            headers.forEach(h -> {
                sourceDetailHeaderRepository.saveAndFlush(h);
            });
        }
        return converted;
    }

    public RequestKey disableSource(String key) {
        final UUID uuid = UUID.fromString(key);
        AtomicInteger affected = new AtomicInteger(0);
        sourceRepository.findFirstByAccessKey(uuid).ifPresent(source -> {
            source.disable();
            sourceRepository.saveAndFlush(source);
            Optional<SourcelResponseResultEntity> result = sourcelResponseResultRepository.findFirstByAccessKey(source.getAccessKey());
            if (result.isPresent()) {
                SourcelResponseResultEntity resultToMod = result.get();
                resultToMod.setState(SourceDto.State.DISABLED.name());
                sourcelResponseResultRepository.saveAndFlush(resultToMod);
            }
            affected.incrementAndGet();
        });
        if (affected.get() == 1) {
            log.info("Disabled Source for key {}", key);
            return RequestKey.builder().key(key).build();
        } else {
            log.info("Nothing disabled for key {} - it does not exist", key);
        }
        return null;
    }

    public Collection<SourceDto> provideAllSources() {
        if (isInDemoMode()) {
            if (demoSources == null || demoSources.isEmpty()) {
                return new ArrayList<>();
            } else {
                return demoSources;
            }
        } else {
            return getAllNonDisabledSources();
        }
    }

    private Collection<SourceDto> getSourcesNonDisabled() {
        if (isInDemoMode()) {
            return demoSources;
        } else {
            return getAllNonDisabledSources();
        }
    }

    private Collection<SourceDto> getAllNonDisabledSources() {
        return sourceConversionService.convertForwards(sourceRepository.findAllByDisabledIsFalse());
    }

    private Collection<SourceDto> getAllSources() {
        return sourceConversionService.convertForwards(sourceRepository.findAll());
    }

    private void waitUntilCatchItIsNotExecuting() {
        while (CatchIt.getInstance().isExecuting()) {
            try {
                ThreadUtils.sleep(Duration.of(300, ChronoUnit.MILLIS));
            } catch (InterruptedException ignore) {
            }
        }
    }

    private String getSourceInfo(SourcelEntity source) {
        StringBuilder sourceInfo = new StringBuilder();
        sourceInfo.append(source.getDomain()).append(DOT);
        sourceInfo.append(source.getDepartment()).append(DOT);
        sourceInfo.append(source.getProcessName()).append(AT);
        sourceInfo.append(source.getAccessUrl());
        return sourceInfo.toString();
    }
}