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


import com.github.catchitcozucan.supervision.api.Histogram;
import com.github.catchitcozucan.supervision.api.SourceDto;
import com.github.catchitcozucan.supervision.api.SourceHeaderDto;
import com.github.catchitcozucan.supervision.api.SourceTestResponseDto;
import com.github.catchitcozucan.supervision.exception.CatchitSupervisionRuntimeException;
import com.github.catchitcozucan.supervision.repository.SourceRepository;
import com.github.catchitcozucan.supervision.repository.SourcelResponseResultRepository;
import com.github.catchitcozucan.supervision.repository.enteties.SourcelResponseResultEntity;
import com.github.catchitcozucan.supervision.service.conversion.SourceConversionService;
import com.github.catchitcozucan.supervision.service.conversion.SourceResponseResultConversionService;
import com.github.catchitcozucan.supervision.service.http.ResponseCodeAndBody;
import com.github.catchitcozucan.supervision.service.http.SimpleHttpGetter;
import com.github.catchitcozucan.supervision.utils.DateUtils;
import com.github.catchitcozucan.supervision.utils.SizeUtils;
import com.github.catchitcozucan.supervision.utils.StringUtils;
import com.github.catchitcozucan.supervision.utils.json.GsonWrapper;
import com.google.gson.JsonSyntaxException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.github.catchitcozucan.supervision.api.SourceDto.State.AVAILABLE;
import static com.github.catchitcozucan.supervision.exception.ErrorCodes.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistogramFetcher {

    private static final String HISTOGRAMSOURCE_S_UTILIZED_VIA_URL_D_HAD_BEEN_EVALUATED_S = "Histogram source %s utilized via URL %s has been evaluated : %s";
    private static final String INTERNAL_UNKNOWN_ERROR_CODE_N = "Internal unknown error code : %n";
    private static final String UNKNOWN_ERROR_N = "Unknown error occured while fetching Histogram";
    private static final String JSON_ISSUE = "Could not parse response - JSON issue";
    private static final String UNKNOWN_JSON_ISSUE = "Could not parse response - unknown issue";
    private static final String DATEFORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final SimpleDateFormat SDF = new SimpleDateFormat(DATEFORMAT);
    private static final String REQUEST_TOWARDS_S_FAILED = "Request towards %s failed";

    private final SourceRepository sourceRepository;
    private final SourcelResponseResultRepository sourcelResponseResultRepository;
    private final SourceResponseResultConversionService sourceResponseResultConversionService;
    private final SourceConversionService sourceConversionService;
    private final GsonWrapper gson;
    private final SimpleHttpGetter simpleHttpGetter;

    private Object fetchLock = new Object();

    public SourceTestResponseDto fetchHistogram(UUID requestKey, boolean readOnly) {
        synchronized (fetchLock) {
            SourceDto sourceDto = sourceConversionService.convertForward(sourceRepository.findFirstByAccessKey(requestKey).get());
            SourcelResponseResultEntity respons = fetchHistogramInner(sourceDto, requestKey, readOnly);
            if (StringUtils.hasContents(respons.getState()) && !respons.getState().equals(AVAILABLE.name())) {
                log.info(String.format(HISTOGRAMSOURCE_S_UTILIZED_VIA_URL_D_HAD_BEEN_EVALUATED_S, requestKey.toString(), sourceDto.getAccessUrl(), respons.getState()));
            }
            return sourceResponseResultConversionService.convertForward(respons);
        }
    }

    public SourceTestResponseDto fetchHistogramViaUrlReadOnly(SourceDto sourceDto) {
        return sourceResponseResultConversionService.convertForward(fetchHistogramInner(sourceDto, UUID.fromString(sourceDto.getRequestKey().getKey()), true));
    }

    private SourcelResponseResultEntity fetchHistogramInner(SourceDto sourceDto, UUID requestKey, boolean readOnly) {
        Histogram histogram = null;
        Optional<SourcelResponseResultEntity> responseEntityOpt = Optional.empty();
        try {
            responseEntityOpt = sourcelResponseResultRepository.findFirstByAccessKey(requestKey);
        } catch (Exception e) {
            log.error(String.format(REQUEST_TOWARDS_S_FAILED, requestKey.toString()), e);
        }
        if (!responseEntityOpt.isPresent()) {
            responseEntityOpt = Optional.of(SourcelResponseResultEntity.builder().accessKey(requestKey).touched(new Date()).state(AVAILABLE.toString()).build());
        }
        SourcelResponseResultEntity responseResultEntity = responseEntityOpt.get();
        responseResultEntity.setAccessKey(requestKey);
        responseResultEntity.setState(SourceDto.State.POLLING_INITIALIZED.name());
        URI uri = null;
        try {
            uri = new URI(sourceDto.getAccessUrl());
            if (!StringUtils.hasContents(uri.getHost()) || !StringUtils.hasContents(uri.getScheme())) {
                responseResultEntity.setState(SourceDto.State.NOT_WORKING_URL_BAD.name());
            }
        } catch (Exception e) {
            responseResultEntity.setState(SourceDto.State.NOT_WORKING_URL_BAD.name());
        }
        if (uri != null && responseResultEntity.getState().equals(SourceDto.State.POLLING_INITIALIZED.name())) {
            long start = System.currentTimeMillis();
            try {
                ResponseCodeAndBody resp = simpleHttpGetter.executeGet(uri, toMap(sourceDto.getSourceDetailDto().getHeaders()), sourceDto.getSourceDetailDto().getProxyHost(), sourceDto.getSourceDetailDto().getProxyPort());
                if (resp.executedOk()) {
                    try {
                        histogram = gson.toObject(resp.getBody(), Histogram.class);
                        responseResultEntity.setState(AVAILABLE.name());
                    } catch (Exception e) {
                        if (e instanceof JsonSyntaxException) {
                            responseResultEntity.setState(SourceDto.State.NOT_WORKING_JSON_SYNTAX_EXCEPTION_WHEN_PARSING_RESPONSE.name());
                            log.warn(JSON_ISSUE, e);
                        } else {
                            responseResultEntity.setState(SourceDto.State.NOT_WORKING_UNKNOWN_PROBLEM_WHEN_JSON_PARSING_RESPONSE.name());
                            log.warn(UNKNOWN_JSON_ISSUE, e);
                        }
                    }
                    if (histogram.getHistogramz() == null || histogram.getHistogramz().length == 0 || histogram.getHistogramz()[0].getData() == null) {
                        if (resp.getBody().length() < 3) {
                            responseResultEntity.setState(SourceDto.State.NOT_WORKING_NO_DATA.name());
                        } else {
                            responseResultEntity.setState(SourceDto.State.NOT_WORKING_GOT_JSON_RESPONSE_BUT_NOTHING_LIKE_A_HISTOGRAM.name());
                        }
                    } else {
                        responseResultEntity.setState(AVAILABLE.name());
                    }
                } else {
                    responseResultEntity.setState(SourceDto.State.HTTP_CODE_NOT_OK.name());
                }
            } catch (Exception e) {
                log.warn(String.format(REQUEST_TOWARDS_S_FAILED, uri.toString()), e);
                if (e instanceof CatchitSupervisionRuntimeException && ((CatchitSupervisionRuntimeException) e).getErrorCode() > -1) {
                    CatchitSupervisionRuntimeException excWithErrorcode = (CatchitSupervisionRuntimeException) e;
                    switch (excWithErrorcode.getErrorCode()) {
                        case BAD_RESPONSE_HTTP_CODE:
                            responseResultEntity.setState(SourceDto.State.NOT_WORKING_BAD_RESPONSE_HTTP_CODE.name());
                            break;
                        case REQUEST_GIVES_CONNECTION_REFUSED:
                            responseResultEntity.setState(SourceDto.State.NOT_WORKING_CONNECTION_REFUSED.name());
                            break;
                        case NO_CONTENT_TYPE_IN_RESPONSE:
                            responseResultEntity.setState(SourceDto.State.NOT_WORKING_NO_CONTENT_TYPE_IN_RESPONSE.name());
                            break;
                        case BAD_CONTENT_TYPE_IN_RESPONSE:
                            responseResultEntity.setState(SourceDto.State.NOT_WORKING_BAD_CONTENT_TYPE_IN_RESPONSE.name());
                            break;
                        case REQUEST_GIVES_UNKNOWN_HOST_EXCEPTION:
                            responseResultEntity.setState(SourceDto.State.NOT_WORKING_REQUEST_GIVES_UNKNOWN_HOST_EXCEPTION.name());
                            break;
                        case REQUEST_GIVES_HTTP_CONECTION_TIMEOUT_EXCEPTION:
                            responseResultEntity.setState(SourceDto.State.NOT_WORKING_REQUEST_GIVES_HTTP_TIMEOUT_EXCEPTION.name());
                            break;
                        case REQUEST_GIVES_HTTP_TIMEOUT_EXCEPTION:
                            responseResultEntity.setState(SourceDto.State.NOT_WORKING_REQUEST_GIVES_HTTP_CONECTION_TIMEOUT_EXCEPTION.name());
                            break;
                        case PROXY_IS_UNKNOWN:
                            responseResultEntity.setState(SourceDto.State.NOT_WORKING_PROXY_UNREACHABLE_OR_UNKNOWN.name());
                            break;
                        default:
                            responseResultEntity.setState(SourceDto.State.NOT_WORKING_REQUEST_GIVES_UNKNOWN_INTERNAL_ERROR_CODE.name());
                            log.warn(String.format(INTERNAL_UNKNOWN_ERROR_CODE_N, excWithErrorcode.getErrorCode()), e);
                    }
                } else {
                    responseResultEntity.setState(SourceDto.State.NOT_WORKING_FOR_UNKNOWN_REASON.name());
                    log.warn(UNKNOWN_ERROR_N, e);
                }
            } finally {
                long execTime = System.currentTimeMillis() - start;
                Date newSucess = responseResultEntity.getState().equals(AVAILABLE.name()) && histogram != null ? new Date() : null;
                SourceTestResponseDto sourceM = SourceTestResponseDto.builder().histogram(histogram).execTime(SizeUtils.getFormattedMillisPrintoutFriendly(execTime)).lastSuccessfulFetch(DateUtils.toString(newSucess, DATEFORMAT)).state(SourceDto.State.valueOf(responseResultEntity.getState())).updatedWhen(newSucess != null ? DateUtils.toString(newSucess, DATEFORMAT) : DateUtils.toString(new Date()
                        , DATEFORMAT)).build();
                SourcelResponseResultEntity sourceEntity = sourceResponseResultConversionService.convertBackward(sourceM);
                responseResultEntity.override(sourceEntity, execTime, newSucess);
                if (!readOnly) {
                    Optional<SourcelResponseResultEntity> possibleMatch = sourcelResponseResultRepository.findFirstByAccessKey(sourceEntity.getAccessKey());
                    if (!possibleMatch.isPresent()) {
                        sourcelResponseResultRepository.saveAndFlush(responseResultEntity);
                    } else {
                        SourcelResponseResultEntity oldEntity = possibleMatch.get();
                        oldEntity.override(sourceEntity, execTime, newSucess);
                        sourcelResponseResultRepository.saveAndFlush(oldEntity);
                    }
                }
            }
        }
        return responseResultEntity;
    }

    private java.util.Map<String, String> toMap(List<SourceHeaderDto> headerDtos) {
        if (headerDtos == null || headerDtos.isEmpty()) {
            return null;
        }
        final java.util.Map<String, String> headers = new HashMap<>();
        headerDtos.stream().forEach(h -> headers.put(h.getName(), h.getValue()));
        return headers;
    }
}
