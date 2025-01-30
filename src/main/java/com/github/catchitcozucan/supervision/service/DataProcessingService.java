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

import static com.github.catchitcozucan.supervision.service.conversion.SourceConversionService.UNTESTED;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.github.catchitcozucan.core.internal.util.domain.StringUtils;
import com.github.catchitcozucan.supervision.api.DepartmentProcessSummary;
import com.github.catchitcozucan.supervision.api.DepartmentSummary;
import com.github.catchitcozucan.supervision.api.DomainSummary;
import com.github.catchitcozucan.supervision.api.Histogram;
import com.github.catchitcozucan.supervision.api.RequestKey;
import com.github.catchitcozucan.supervision.api.SourceDto;
import com.github.catchitcozucan.supervision.api.SourceTestResponseDto;
import com.github.catchitcozucan.supervision.config.SecurityAndAppConfig;
import com.github.catchitcozucan.supervision.demo.DemoHistogramMaker;
import com.github.catchitcozucan.supervision.exception.CatchitSupervisionRuntimeException;
import com.github.catchitcozucan.supervision.repository.SourcelResponseResultRepository;
import com.github.catchitcozucan.supervision.repository.enteties.SourcelResponseResultEntity;
import com.github.catchitcozucan.supervision.service.conversion.SourceResponseResultConversionService;
import com.github.catchitcozucan.supervision.utils.json.GsonWrapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.REQUIRED)
@Service
@Slf4j
@RequiredArgsConstructor
public class DataProcessingService {

	public static final String NO_DOMAIN = "NO_DOMAIN";
	public static final String NO_DEPARTMENT = "NO_DEPARTMENT";

	private static final Logger LOGGER = LoggerFactory.getLogger(DataProcessingService.class);
	private static final String SOURCE_FOR_PROCESS_NAMED_S_WITHIN_GROUP_S_COULD_NOT_BE_MATCHED_TO_ANY_KNOWN_HOSTOGRAM_SOURCE = "Source for process named %s within group %s could not be matched to any known histogram source";
	private static final String SOURCE_FOR_PROCESS_NAMED_COULD_NOT_BE_MATCHED_TO_ANY_KNOWN_HOSTOGRAM_SOURCE = "Source for process named %s with no group specified could not be matched to any known histogram source";
	private static final String WE_ARE_IN_DEMO_MODE_AUTO_REFRESH_IS_NOT_ENABLED = "We are in demo mode - auto-refresh is not enabled";
	private static final String WE_ARE_IN_LIVE_MODE_AUTO_REFRESH_IS_ENABLED = "We are in live mode - auto-refresh is enabled";
	private static final String THREE_MILLIS = "3 milliseconds";
	private static final String FIRST_DATE_EVER = "1970-01-01 00:00:00";
	private static final String REFRESH_NOT_ACTIVE_UNTIL_WE_ARE_COMPLETELY_INITIALIZED = "Refresh not active until we are completely initialized)";
	private static final String RUNMODE_COULD_NOT_BE_DETERMINED = "Runmode could not be determined..";

	@Value("${catchit.hierarchy}")
	private String catchitHierarchy;
	private String domainLabel;
	private String departmentLabel;

	private final SecurityAndAppConfig.CompletetelyInitiatedindicator completetelyInitiatedindicator;
	private final HistogramFetcher histogramFetcher;
	private final SourcesProvider sourcesProvider;
	private final SourcelResponseResultRepository sourcelResponseResultRepository;
	private final SourceResponseResultConversionService sourceResponseResultConversionService;
	private final GsonWrapper gson;

	private enum Runmode {
		DEMO, LIVE, UNKNOWN
	}

	private Runmode runmode = Runmode.UNKNOWN;
	private Runmode lastRunMode = Runmode.UNKNOWN;

	@PostConstruct
	public void postConstruct() {
		String domainLabelRaw = catchitHierarchy.split("\\.")[0];
		String departmentLabelRaw = catchitHierarchy.split("\\.")[1];
		domainLabel = domainLabelRaw.substring(0, 1).toUpperCase() + domainLabelRaw.substring(1).toLowerCase();
		departmentLabel = departmentLabelRaw.substring(0, 1).toUpperCase() + departmentLabelRaw.substring(1).toLowerCase();
	}

	public Optional<Histogram> getHistogram(String domain, String department, String process, boolean flipFailures, boolean returnOnlyFailures) {
		Optional<SourceDto> source = sourcesProvider.provideSourcesPerDomainAndDepartmentAndProcess(domain, department, process);
		if (source.isPresent()) {

			if (weAreInDemoMode()) {
				return Optional.of(DemoHistogramMaker.generate(source.get().getRequestKey().getKey(), domain, department, process, flipFailures, returnOnlyFailures));
			}

			SourceDto sourceToUse = source.get();
			Optional<SourcelResponseResultEntity> result = sourcelResponseResultRepository.findFirstByAccessKey(UUID.fromString(sourceToUse.getRequestKey().getKey()));
			Optional<Histogram> histogram = result.isPresent() ? Optional.of((gson.toObject(result.get().getHistogramAsJson(), Histogram.class)).transForm(flipFailures, returnOnlyFailures)) : Optional.empty();
			return histogram;
		} else {
			if (StringUtils.hasContents(department)) {
				LOGGER.info(String.format(SOURCE_FOR_PROCESS_NAMED_S_WITHIN_GROUP_S_COULD_NOT_BE_MATCHED_TO_ANY_KNOWN_HOSTOGRAM_SOURCE, process, department));
			} else {
				LOGGER.info(String.format(SOURCE_FOR_PROCESS_NAMED_COULD_NOT_BE_MATCHED_TO_ANY_KNOWN_HOSTOGRAM_SOURCE, process));
			}
		}
		return Optional.empty();
	}

	public List<SourceDto> getSources() {
		Collection<SourceDto> s = sourcesProvider.provideAllSources();
		List<SourceDto> sources = new ArrayList<>();
		s.stream().forEach(ss -> {
			ss.setDepartmentLabel(departmentLabel);
			ss.setDomainLabel(domainLabel);
			tryComplementWithState(ss);
			sources.add(ss);
		});
		return sources;
	}

	public String getHierarchy() {
		return catchitHierarchy;
	}

	public SourceTestResponseDto testSource(UUID requestKey) {
		return histogramFetcher.fetchHistogram(requestKey, true);
	}

	public SourceDto saveSource(SourceDto sourceToSave) {
		SourceDto saved = null;
		boolean triggerResultUponSave = false;
		if (weAreInDemoMode() || !StringUtils.hasContents(sourceToSave.getProcessName()) || sourceToSave.getProcessName().startsWith(UNTESTED)) {
			if (weAreInDemoMode() || sourceToSave.getRequestKey() == null) {
				UUID newRequestKey = UUID.randomUUID();
				sourceToSave.setRequestKey(RequestKey.builder().key(newRequestKey.toString()).build());
			}
			try {
				saved = sourcesProvider.saveSource(sourceToSave);
				UUID toUse = UUID.fromString(sourceToSave.getRequestKey().getKey());
				triggerResultUponSave = !sourcelResponseResultRepository.findFirstByAccessKey(toUse).isPresent();
				SourceTestResponseDto resp = testSource(toUse);
				if (resp.getHistogram() != null) {
					saved.setProcessName(resp.getHistogram().getEntityNames());
				}
			} catch (Exception e) {
				LOGGER.warn("Problems while pre-save-testing source", e);
			} finally {
				if (saved != null) {
					saved = sourcesProvider.saveSource(saved);
				} else {
					String errMsg = "Problems while post-testing-saving source : nothing to save..";
					throw new CatchitSupervisionRuntimeException(errMsg);
				}
			}
		} else {
			saved = sourcesProvider.saveSource(sourceToSave);
		}
		if (triggerResultUponSave) {
			SourceTestResponseDto result = histogramFetcher.fetchHistogram(UUID.fromString(saved.getRequestKey().getKey()), false);
			saved.setState(result.getState());
		}
		return saved;
	}

	public SourceTestResponseDto testSource(SourceDto sourceDto) {
		return histogramFetcher.fetchHistogramViaUrlReadOnly(sourceDto);
	}

	public RequestKey deleteSource(String key) {
		return sourcesProvider.disableSource(key);
	}

	public SourceTestResponseDto getLastResult(UUID uuid) {
		if (sourcesProvider.isInDemoMode()) {
			Histogram histogram = DemoHistogramMaker.generate(uuid.toString(), null, null, null, false, false);
			return SourceTestResponseDto.builder().histogram(histogram).execTime(THREE_MILLIS).state(SourceDto.State.DEMO_MODE).updatedWhen(FIRST_DATE_EVER).build();
		}

		Optional<SourcelResponseResultEntity> result = sourcelResponseResultRepository.findFirstByAccessKey(uuid);
		if (result.isPresent()) {
			return sourceResponseResultConversionService.convertForward(result.get());
		} else {
			return testSource(uuid);
		}
	}

	@Scheduled(cron = "*/5 * * * * *")
	public void refreshAllResults() {
		if (completetelyInitiatedindicator.isFullyInitialized()) {
			lastRunMode = Runmode.valueOf(runmode.name());
			if (!determineRunModeReturnDemoState()) {
				sourcesProvider.refreshResults();
			}
			if (!lastRunMode.equals(runmode)) {
				if (runmode.equals(Runmode.LIVE)) {
					LOGGER.info(WE_ARE_IN_LIVE_MODE_AUTO_REFRESH_IS_ENABLED);
				} else if (runmode.equals(Runmode.DEMO)) {
					LOGGER.info(WE_ARE_IN_DEMO_MODE_AUTO_REFRESH_IS_NOT_ENABLED);
				} else {
					LOGGER.warn(RUNMODE_COULD_NOT_BE_DETERMINED);
				}
			}
		} else {
			log.info(REFRESH_NOT_ACTIVE_UNTIL_WE_ARE_COMPLETELY_INITIALIZED);
		}
	}

	public boolean determineRunModeReturnDemoState() {
		boolean isDemo = weAreInDemoMode();
		if (isDemo) {
			runmode = Runmode.DEMO;
		} else {
			runmode = Runmode.LIVE;
		}
		return isDemo;
	}

	public boolean weAreInDemoMode() {
		return sourcesProvider.isInDemoMode();
	}

	public List<DomainSummary> getDomainSummaries() {
		List<SourcelResponseResultEntity> availables = sourcelResponseResultRepository.findAllByStateIs(SourceDto.State.AVAILABLE.name());
		List<UUID> availKeys = availables.stream().map(SourcelResponseResultEntity::getAccessKey).collect(Collectors.toList());
		Map<String, List<SourceDto>> domainMappings = sourcesProvider.provideAllSources().stream().filter(e -> (e.getState() != null && e.getState().equals(SourceDto.State.DEMO_MODE)) || availKeys.contains(UUID.fromString(e.getRequestKey().getKey()))).collect(Collectors.groupingBy(SourceDto::getDomain));
		Set<String> keyz = new HashSet<>(domainMappings.keySet());
		List<SourceDto> genericSourcesWithNoDomains = new ArrayList<>();
		if (keyz.stream().filter(k -> k.equals(NO_DOMAIN)).findFirst().isPresent()) {
			genericSourcesWithNoDomains.addAll(domainMappings.get(NO_DOMAIN));
			keyz.remove(NO_DOMAIN);
		}
		List<String> keys = new ArrayList<>(keyz);
		Collections.sort(keys);
		List<DomainSummary> domainSummariesPerDomain = new ArrayList<>();
		keys.stream().forEach(k -> {
			List<SourceDto> res = domainMappings.get(k);
			AtomicLong inFinishedState = new AtomicLong(0);
			AtomicLong inFailedState = new AtomicLong(0);
			AtomicLong inProcState = new AtomicLong(0);
			final String[] dom = new String[1];
			res.stream().forEach(source -> {

				if (dom[0] == null) {
					dom[0] = source.getDomain();
				}

				Optional<Histogram> h;
				if (sourcesProvider.isInDemoMode()) {
					h = Optional.of(DemoHistogramMaker.generate(source.getRequestKey().getKey(), source.getDomain(), source.getDepartment(), source.getProcessName(), false, false));
				} else {
					h = Optional.of(histogramFetcher.fetchHistogram(UUID.fromString(source.getRequestKey().getKey()), true).getHistogram());
				}
				if (h.isPresent()) {
					Histogram histogram = h.get();
					inFinishedState.getAndAdd(histogram.getHistogramz()[0].getActuallyFinished());
					inFailedState.getAndAdd(histogram.getHistogramz()[0].getNumberOfSubjectsInFailstate());
					inProcState.getAndAdd(histogram.getHistogramz()[0].getSum() - histogram.getHistogramz()[0].getNumberOfSubjectsInFailstate() - histogram.getHistogramz()[0].getActuallyFinished());
				}
			});
			domainSummariesPerDomain.add(DomainSummary.builder().domain(dom[0]).domainLabel(domainLabel).inFinishedState(inFinishedState.get()).inFailState(inFailedState.get()).processing(inProcState.get()).key(dom[0]).build());
		});

		return domainSummariesPerDomain;
	}

	public List<DepartmentSummary> getDepartmentSummaries(String domain) {
		List<SourcelResponseResultEntity> availables = sourcelResponseResultRepository.findAllByStateIs(SourceDto.State.AVAILABLE.name());
		List<UUID> availKeys = availables.stream().map(SourcelResponseResultEntity::getAccessKey).collect(Collectors.toList());
		Map<String, List<SourceDto>> domainMappings = sourcesProvider.provideSourcesPerDomain(domain).stream().filter(e -> (e.getState() != null && e.getState().equals(SourceDto.State.DEMO_MODE)) || availKeys.contains(UUID.fromString(e.getRequestKey().getKey()))).collect(Collectors.groupingBy(SourceDto::getDepartment));
		Set<String> keyz = new HashSet<>(domainMappings.keySet());
		List<SourceDto> genericSourcesWithNoDomains = new ArrayList<>();
		if (keyz.stream().filter(k -> k.equals(NO_DEPARTMENT)).findFirst().isPresent()) {
			genericSourcesWithNoDomains.addAll(domainMappings.get(NO_DEPARTMENT));
			keyz.remove(NO_DEPARTMENT);
		}
		List<String> keys = new ArrayList<>(keyz);
		Collections.sort(keys);
		List<DepartmentSummary> departmentSummaries = new ArrayList<>();
		keys.stream().forEach(k -> {
			List<SourceDto> res = domainMappings.get(k);
			res.stream().forEach(source -> {
				DepartmentSummary.DepartmentSummaryBuilder builder = DepartmentSummary.builder().domain(source.getDomain()).department(source.getDepartment());

				Optional<DepartmentSummary> departmentSummary = departmentSummaries.stream().filter(d -> d.getDepartment().equals(source.getDepartment())).findFirst();
				DepartmentSummary summary = null;
				if (departmentSummary.isPresent()) {
					summary = departmentSummary.get();
					departmentSummaries.remove(summary);
				}

				Optional<Histogram> h;
				if (sourcesProvider.isInDemoMode()) {
					h = Optional.of(DemoHistogramMaker.generate(source.getRequestKey().getKey(), source.getDomain(), source.getDepartment(), source.getProcessName(), false, false));
				} else {
					h = Optional.of(histogramFetcher.fetchHistogram(UUID.fromString(source.getRequestKey().getKey()), true).getHistogram());
				}
				if (h.isPresent()) {
					Histogram histogram = h.get();
					if (summary == null) {
						summary = builder.inFailState(histogram.getHistogramz()[0].getNumberOfSubjectsInFailstate()).inFinishedState(histogram.getHistogramz()[0].getActuallyFinished()).processing(histogram.getHistogramz()[0].getSum() - histogram.getHistogramz()[0].getActuallyFinished() - histogram.getHistogramz()[0].getNumberOfSubjectsInFailstate()).department(source.getDepartment()).domainLabel(domainLabel).departmentLabel(departmentLabel).key(source.getDepartment()).build();
					} else {
						long totalFails = summary.getInFailState() + histogram.getHistogramz()[0].getNumberOfSubjectsInFailstate();
						long totalFinished = summary.getInFinishedState() + histogram.getHistogramz()[0].getActuallyFinished();
						long totalProcessing = summary.getProcessing() + histogram.getHistogramz()[0].getSum() - histogram.getHistogramz()[0].getActuallyFinished() - histogram.getHistogramz()[0].getNumberOfSubjectsInFailstate();
						summary = builder.inFailState(totalFails).inFinishedState(totalFinished).processing(totalProcessing).department(source.getDepartment()).build();
					}
					departmentSummaries.add(summary);
				}
			});
		});
		return departmentSummaries;
	}

	public List<DepartmentProcessSummary> getDepartmentProcesses(String domain, String department) {
		List<SourcelResponseResultEntity> availables = sourcelResponseResultRepository.findAllByStateIs(SourceDto.State.AVAILABLE.name());
		List<UUID> availKeys = availables.stream().map(SourcelResponseResultEntity::getAccessKey).collect(Collectors.toList());
		Map<String, List<SourceDto>> domainMappings = sourcesProvider.provideSourcesPerDomainAndDepartment(domain, department).stream().filter(e -> (e.getState() != null && e.getState().equals(SourceDto.State.DEMO_MODE)) || availKeys.contains(UUID.fromString(e.getRequestKey().getKey()))).collect(Collectors.groupingBy(SourceDto::getDepartment));
		List<String> keys = new ArrayList<>(domainMappings.keySet());
		List<SourceDto> genericSourcesWithNoDomains = new ArrayList<>();
		if (keys.stream().filter(k -> k.equals(NO_DEPARTMENT)).findFirst().isPresent()) {
			genericSourcesWithNoDomains.addAll(domainMappings.get(NO_DEPARTMENT));
			keys.remove(NO_DEPARTMENT);
		}
		Collections.sort(keys);
		List<DepartmentProcessSummary> departmentSummaries = new ArrayList<>();
		keys.stream().forEach(k -> {
			List<SourceDto> res = domainMappings.get(k);
			res.stream().forEach(source -> {
				DepartmentProcessSummary.DepartmentProcessSummaryBuilder builder = DepartmentProcessSummary.builder().domain(source.getDomain()).department(source.getDepartment());
				Optional<Histogram> h;
				if (sourcesProvider.isInDemoMode()) {
					h = Optional.of(DemoHistogramMaker.generate(source.getRequestKey().getKey(), source.getDomain(), source.getDepartment(), source.getProcessName(), false, false));
				} else {
					h = Optional.of(histogramFetcher.fetchHistogram(UUID.fromString(source.getRequestKey().getKey()), true).getHistogram());
				}
				if (h.isPresent()) {
					Histogram histogram = h.get();

					DepartmentProcessSummary summary =
                            builder.inFailState(histogram.getHistogramz()[0].getNumberOfSubjectsInFailstate()).inFinishedState(histogram.getHistogramz()[0].getActuallyFinished()).processing(histogram.getHistogramz()[0].getSum() - histogram.getHistogramz()[0].getActuallyFinished() - histogram.getHistogramz()[0].getNumberOfSubjectsInFailstate()).department(source.getDepartment()).domainLabel(domainLabel).departmentLabel(departmentLabel)
							//.processName(histogram.getHistogramz()[0].getLabel())
							.actualProgressInPercent(histogram.getHistogramz()[0].getActualStepProgress()).processName(histogram.getEntityNames()).key(source.getRequestKey().getKey()).build();
					departmentSummaries.add(summary);
				}
			});
		});
		return departmentSummaries;
	}

	private void tryComplementWithState(SourceDto ss) {
		if (!weAreInDemoMode()) {
			Optional<SourcelResponseResultEntity> result = sourcelResponseResultRepository.findFirstByAccessKey(UUID.fromString(ss.getRequestKey().getKey()));
			if (result.isPresent()) {
				ss.setState(SourceDto.State.valueOf(result.get().getState()));
			}
		} else {
			ss.setState(SourceDto.State.DEMO_MODE);
		}
	}
}
