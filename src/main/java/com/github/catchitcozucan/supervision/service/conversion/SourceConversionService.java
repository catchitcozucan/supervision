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

import com.github.catchitcozucan.supervision.api.RequestKey;
import com.github.catchitcozucan.supervision.api.SourceDetailDto;
import com.github.catchitcozucan.supervision.api.SourceDto;
import com.github.catchitcozucan.supervision.api.SourceHeaderDto;
import com.github.catchitcozucan.supervision.repository.enteties.SourceDetailEntity;
import com.github.catchitcozucan.supervision.repository.enteties.SourceHeaderEntity;
import com.github.catchitcozucan.supervision.repository.enteties.SourcelEntity;
import com.github.catchitcozucan.supervision.utils.StringUtils;
import com.github.catchitcozucan.supervision.utils.encr.Aes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SourceConversionService implements ConversionService<SourcelEntity, SourceDto> {

    public static final String UNTESTED = "UNTESTED";

    @Value("${catchit.basicAuthAesKey}")
    private String basicAuthAesKey;

    @Override
    public Collection<SourceDto> convertForwards(Collection<SourcelEntity> sourcelEntities) {
        List<SourceDto> sourceDtos = new java.util.ArrayList<>();
        sourcelEntities.stream().forEach(sourceEntity -> {
            sourceDtos.add(convertForward(sourceEntity));
        });
        Collections.sort(sourceDtos);
        return sourceDtos;
    }

    @Override
    public Collection<SourcelEntity> convertBackwards(Collection<SourceDto> sourceDtos) {
        List<SourcelEntity> protocolForUis = new java.util.ArrayList<>();
        sourceDtos.stream().forEach(protocolEntity -> {
            protocolForUis.add(convertBackward(protocolEntity));
        });
        return protocolForUis;
    }

    @Override
    public SourceDto convertForward(SourcelEntity sourcelEntity) {
        return SourceDto.builder()
                .domain(sourcelEntity.getDomain())
                .department(sourcelEntity.getDepartment())
                .disabled(sourcelEntity.getDisabled())
                .processName(sourcelEntity.getProcessName())
                .accessUrl(sourcelEntity.getAccessUrl())
                .sourceDetailDto(convertdetailForwad(sourcelEntity.getSourceDetailEntity()))
                .requestKey(RequestKey.builder().key(sourcelEntity.getAccessKey().toString()).build())
                .id(sourcelEntity.getId())
                .version(sourcelEntity.getVersion())
                .build();
    }

    @Override
    public SourcelEntity convertBackward(SourceDto sourceDto) {
        SourceDetailEntity detail = convertdetailBackward(sourceDto.getSourceDetailDto());
        if (sourceDto.getState() == null) {
            sourceDto.setState(SourceDto.State.UNKNOWN);
        }
        SourcelEntity source = SourcelEntity.builder()
                .id(sourceDto.getId())
                .disabled(sourceDto.isDisabled())
                .version(sourceDto.getVersion())
                .domain(sourceDto.getDomain())
                .department(sourceDto.getDepartment())
                .processName(StringUtils.hasContents(sourceDto.getProcessName()) ? sourceDto.getProcessName() : new StringBuilder(UNTESTED).append("_").append(UUID.randomUUID().toString()).toString())
                .accessUrl(sourceDto.getAccessUrl())
                .touched(new Date())
                .sourceDetailEntity(detail)
                .accessKey(sourceDto.getRequestKey() != null ? UUID.fromString(sourceDto.getRequestKey().getKey()) : UUID.randomUUID())
                .build();
        detail.setSourceEntity(source);
        return source;
    }

    private SourceDetailEntity convertdetailBackward(SourceDetailDto sourceDetailDto) {
        List<SourceHeaderEntity> headers = (sourceDetailDto.getHeaders() == null || sourceDetailDto.getHeaders().isEmpty()) ? new ArrayList<>()
                : sourceDetailDto.getHeaders().stream().filter(e -> e != null).map(this::convertHeaderBackward).collect(Collectors.toList());

        String enrcyptedPwd = null;
        if (StringUtils.hasContents(sourceDetailDto.getBasicAuthUsername()) && sourceDetailDto.getBasicAuthPassword() != null && sourceDetailDto.getBasicAuthPassword().length() > 3) {
            enrcyptedPwd = packageToAESSecretBasedBase64(sourceDetailDto.getBasicAuthPassword());
        }

        return SourceDetailEntity.builder()
                .headers(headers)
                .id(sourceDetailDto.getId())
                .version(sourceDetailDto.getVersion())
                .basicAuthUsername(sourceDetailDto.getBasicAuthUsername())
                .basicAuthPassword(enrcyptedPwd)
                .id(sourceDetailDto.getId())
                .proxyHost(sourceDetailDto.getProxyHost())
                .proxyPort(sourceDetailDto.getProxyPort())
                .build();
    }

    private SourceHeaderEntity convertHeaderBackward(SourceHeaderDto sourceHeaderDto) {
        return SourceHeaderEntity.builder()
                .id(sourceHeaderDto.getId())
                .version(sourceHeaderDto.getVersion())
                .value(sourceHeaderDto.getValue())
                .name(sourceHeaderDto.getName())
                .build();
    }

    private SourceDetailDto convertdetailForwad(SourceDetailEntity sourceDetailEntity) {
        List<SourceHeaderDto> headers = sourceDetailEntity.getHeaders() == null ? new ArrayList<>()
                : sourceDetailEntity.getHeaders().stream().filter(e -> e != null).map(this::convertHeaderForward).collect(Collectors.toList());

        String decyptedPwd = null;
        if (StringUtils.hasContents(sourceDetailEntity.getBasicAuthUsername()) && sourceDetailEntity.getBasicAuthPassword() != null && sourceDetailEntity.getBasicAuthPassword().length() > 3) {
            decyptedPwd = extractFromAESSecretBasedBase64(sourceDetailEntity.getBasicAuthPassword());
        }

        return SourceDetailDto.builder()
                .headers(headers)
                .id(sourceDetailEntity.getId())
                .version(sourceDetailEntity.getVersion())
                .basicAuthUsername(sourceDetailEntity.getBasicAuthUsername())
                .basicAuthPassword(decyptedPwd)
                .id(sourceDetailEntity.getId())
                .proxyHost(sourceDetailEntity.getProxyHost())
                .proxyPort(sourceDetailEntity.getProxyPort())
                .build();
    }

    private SourceHeaderDto convertHeaderForward(SourceHeaderEntity sourceHeaderDto) {
        return SourceHeaderDto.builder()
                .id(sourceHeaderDto.getId())
                .version(sourceHeaderDto.getVersion())
                .value(sourceHeaderDto.getValue())
                .name(sourceHeaderDto.getName())
                .build();
    }

    private String packageToAESSecretBasedBase64(String input) {
        byte[] aesEncoded = Aes.getSilent().encrypt(basicAuthAesKey.getBytes(StandardCharsets.US_ASCII), input.getBytes(StandardCharsets.UTF_8));
        return new String(Base64.getEncoder().encode(aesEncoded), StandardCharsets.UTF_8);
    }

    private String extractFromAESSecretBasedBase64(String input) {
        return new String(Aes.getSilent().decrypt(basicAuthAesKey.getBytes(StandardCharsets.US_ASCII), Base64.getDecoder().decode(input)), StandardCharsets.UTF_8);
    }

}
