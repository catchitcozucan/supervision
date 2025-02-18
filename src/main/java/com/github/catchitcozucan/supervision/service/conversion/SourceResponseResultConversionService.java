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
package com.github.catchitcozucan.supervision.service.conversion;

import com.github.catchitcozucan.core.internal.util.domain.StringUtils;
import com.github.catchitcozucan.supervision.api.Histogram;
import com.github.catchitcozucan.supervision.api.RequestKey;
import com.github.catchitcozucan.supervision.api.SourceDto;
import com.github.catchitcozucan.supervision.api.SourceTestResponseDto;
import com.github.catchitcozucan.supervision.exception.CatchitSupervisionRuntimeException;
import com.github.catchitcozucan.supervision.repository.enteties.SourcelResponseResultEntity;
import com.github.catchitcozucan.supervision.utils.DateUtils;
import com.github.catchitcozucan.supervision.utils.json.GsonWrapper;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SourceResponseResultConversionService implements ConversionService<SourcelResponseResultEntity, SourceTestResponseDto> {

	private static final String DATEFORMAT = "yyyy-MM-dd HH:mm:ss";
	private static final SimpleDateFormat SDF = new SimpleDateFormat(DATEFORMAT);

	private final GsonWrapper gson;

	@Override
	public Collection<SourceTestResponseDto> convertForwards(Collection<SourcelResponseResultEntity> sourcelEntities) {
		List<SourceTestResponseDto> sourceResults = new ArrayList<>();
		sourcelEntities.stream().forEach(protocolEntity -> {
			sourceResults.add(convertForward(protocolEntity));
		});
		Collections.sort(sourceResults);
		return sourceResults;
	}

	@Override
	public Collection<SourcelResponseResultEntity> convertBackwards(Collection<SourceTestResponseDto> sourceDtos) {
		List<SourcelResponseResultEntity> protocolForUis = new ArrayList<>();
		sourceDtos.stream().forEach(protocolEntity -> {
			protocolForUis.add(convertBackward(protocolEntity));
		});
		return protocolForUis;
	}

	@Override
	public SourceTestResponseDto convertForward(SourcelResponseResultEntity sourcelEntity) {

		return SourceTestResponseDto.builder().updatedWhen(DateUtils.toString(sourcelEntity.getTouched(), DATEFORMAT)).requestKey(RequestKey.builder().key(sourcelEntity.getAccessKey().toString()).build()).lastSuccessfulFetch(DateUtils.toString(sourcelEntity.getLastSuccessfulFetch(), DATEFORMAT)).execTime(sourcelEntity.getLastExecTime()).histogram(toHistogram(sourcelEntity.getHistogramAsJson())).state(SourceDto.State.valueOf(sourcelEntity.getState())).build();
	}

	@Override
	public SourcelResponseResultEntity convertBackward(SourceTestResponseDto sourceDto) {
		SourcelResponseResultEntity.SourcelResponseResultEntityBuilder builder = SourcelResponseResultEntity.builder().touched(new Date()).lastExecTime(sourceDto.getExecTime()).accessKey(sourceDto.getRequestKey() != null ? UUID.fromString(sourceDto.getRequestKey().getKey()) : UUID.randomUUID()).histogramAsJson(fromHistogram(sourceDto.getHistogram())).state(sourceDto.getState().name());

		if (StringUtils.hasContents(sourceDto.getLastSuccessfulFetch())) {
			try {
				builder.lastSuccessfulFetch(SDF.parse(sourceDto.getLastSuccessfulFetch()));
			} catch (ParseException e) {
				throw new CatchitSupervisionRuntimeException(e);
			}
		}
		return builder.build();
	}

	private Histogram toHistogram(String histogramDataAsJson) {
		return gson.toObject(histogramDataAsJson, Histogram.class);
	}

	private String fromHistogram(Histogram histogramDataAsJson) {
		return gson.toJson(histogramDataAsJson);
	}

}
