/**
 *    Original work by Ola Aronsson 2020
 *    Courtesy of nollettnoll AB &copy; 2012 - 2020
 *
 *    Licensed under the Creative Commons Attribution 4.0 International (the "License")
 *    you may not use this file except in compliance with the License. You may obtain
 *    a copy of the License at
 *
 *                https://creativecommons.org/licenses/by/4.0/
 *
 *    The software is provided “as is”, without warranty of any kind, express or
 *    implied, including but not limited to the warranties of merchantability,
 *    fitness for a particular purpose and noninfringement. In no event shall the
 *    authors or copyright holders be liable for any claim, damages or other liability,
 *    whether in an action of contract, tort or otherwise, arising from, out of or
 *    in connection with the software or the use or other dealings in the software.
 */
package com.github.catchitcozucan.supervision.api;

import com.github.catchitcozucan.core.internal.util.domain.ToStringBuilder;
import com.github.catchitcozucan.supervision.api.annotation.Optional;
import com.github.catchitcozucan.supervision.utils.CompareUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SourceDto implements Comparable<SourceDto> {

    private static final String DOMAIN = "domain";
    private static final String DEPARTMENT = "department";
    private static final String PROCESS = "process";

    @Optional
    private Integer id;
    @Optional
    private Integer version;
    private String domain;
    private String department;
    private String domainLabel;
    private String departmentLabel;
    private String processName;
    @Optional
    private RequestKey requestKey;
    private String accessUrl;
    @Optional
    private SourceDetailDto sourceDetailDto;
    private boolean disabled;

    public void override(SourceDto sourceDto) {
        this.sourceDetailDto = sourceDto.getSourceDetailDto();
        this.state = sourceDto.getState();
        this.accessUrl = sourceDto.getAccessUrl();
        this.department = sourceDto.getDepartment();
        this.domain = sourceDto.getDomain();
        this.processName = sourceDto.getProcessName();
    }

    public void setRequestKey(RequestKey requestKey) {
        this.requestKey = requestKey;
    }

    public void setProcessName(String entityNames) {
        this.processName = entityNames;
    }

    public enum State {
        DISABLED,
        UNKNOWN,
        INITIALIZED,
        DEMO_MODE,
        POLLING_INITIALIZED,
        AVAILABLE,
        NOT_WORKING_URL_BAD,
        HTTP_CODE_NOT_OK,
        NOT_WORKING_NO_DATA,
        NOT_WORKING_NO_CONTENT_TYPE_IN_RESPONSE,
        NOT_WORKING_CONNECTION_REFUSED,
        NOT_WORKING_BAD_CONTENT_TYPE_IN_RESPONSE,
        NOT_WORKING_REQUEST_GIVES_UNKNOWN_HOST_EXCEPTION,
        NOT_WORKING_REQUEST_GIVES_HTTP_CONECTION_TIMEOUT_EXCEPTION,
        NOT_WORKING_REQUEST_GIVES_UNKNOWN_INTERNAL_ERROR_CODE,
        NOT_WORKING_REQUEST_GIVES_HTTP_TIMEOUT_EXCEPTION,
        NOT_WORKING_FOR_UNKNOWN_REASON,
        NOT_WORKING_JSON_SYNTAX_EXCEPTION_WHEN_PARSING_RESPONSE,
        NOT_WORKING_UNKNOWN_PROBLEM_WHEN_JSON_PARSING_RESPONSE,
        NOT_WORKING_GOT_JSON_RESPONSE_BUT_NOTHING_LIKE_A_HISTOGRAM,
        NOT_WORKING_PROXY_UNREACHABLE_OR_UNKNOWN,
        NOT_WORKING_BAD_RESPONSE_HTTP_CODE
    }

    @Builder.Default
    private State state = State.UNKNOWN;

    public void setState(State state) {
        this.state = state;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public void setDomainLabel(String domainLabel) {
        this.domainLabel = domainLabel;
    }

    public void setDepartmentLabel(String departmentLabel) {
        this.departmentLabel = departmentLabel;
    }

    public boolean isSane() {
        return state.equals(State.AVAILABLE);
    }

    @Override
    public String toString() {
        ToStringBuilder toStringBuilder = new ToStringBuilder();
        toStringBuilder.append(DOMAIN, domain);
        toStringBuilder.append(DEPARTMENT, department);
        toStringBuilder.append(PROCESS, processName);
        return toStringBuilder.toString();
    }

    @Override
    public int compareTo(SourceDto o) {
        int comp = CompareUtils.compareForNulling(this, o);
        if (comp == CompareUtils.BOTH_OBJECTS_ARE_NON_NULL) {
            comp = CompareUtils.compareForNulling(domain, o.domain);
            if (comp == CompareUtils.BOTH_OBJECTS_ARE_NON_NULL) {
                if (domain.equals(o.getDomain())) {
                    comp = CompareUtils.compareForNulling(department, o.department);
                    if (comp == CompareUtils.BOTH_OBJECTS_ARE_NON_NULL) {
                        if (department.equals(o.department)) {
                            comp = CompareUtils.compareForNulling(processName, o.processName);
                            if (comp == CompareUtils.BOTH_OBJECTS_ARE_NON_NULL) {
                                return processName.compareTo(o.processName);
                            }
                        } else {
                            return domain.compareTo(o.getDomain());
                        }
                    }
                } else {
                    return domain.compareTo(o.getDomain());
                }
            }
        }
        return comp;
    }
}
