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
package com.github.catchitcozucan.supervision.repository.enteties;


import com.github.catchitcozucan.supervision.utils.DateUtils;
import com.github.catchitcozucan.supervision.utils.SizeUtils;
import com.github.catchitcozucan.supervision.utils.StringUtils;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Date;
import java.util.UUID;

@lombok.Builder
@AllArgsConstructor
@Getter
@Entity
@Table(name = "source_response")
public class SourcelResponseResultEntity {

    public SourcelResponseResultEntity() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "source_generator")
    @SequenceGenerator(name = "source_generator", sequenceName = "source_response_seq", allocationSize = 1)
    private Integer id;

    @Version
    private Integer version;

    @Column(name = "last_succ", columnDefinition = "date")
    private Date lastSuccessfulFetch;

    @Column(name = "touched", columnDefinition = "date", nullable = false)
    private Date touched;

    @Column(name = "last_exec_time", columnDefinition = "varchar", nullable = false)
    private String lastExecTime;

    @Column(name = "access_key", columnDefinition = "UUID", nullable = false, unique = true)
    private UUID accessKey;

    @Column(name = "state", columnDefinition = "varchar", nullable = false)
    private String state;

    @Column(name = "histogram", columnDefinition = "text")
    private String histogramAsJson;

    public void override(SourcelResponseResultEntity sourceEntity, long execTime, Date newSuccess) {
        lastExecTime = SizeUtils.getFormattedMillisPrintoutFriendly(execTime);
        boolean theOverrideIsOfASuccessfulCall = StringUtils.hasContents(sourceEntity.getHistogramAsJson()) && !sourceEntity.getHistogramAsJson().equals("null");
        if (theOverrideIsOfASuccessfulCall) {
            histogramAsJson = sourceEntity.getHistogramAsJson();
            touched = sourceEntity.getTouched();
            lastSuccessfulFetch = newSuccess;
            if (sourceEntity.getLastSuccessfulFetch() != null) {
                lastSuccessfulFetch = sourceEntity.getLastSuccessfulFetch();
            } else {
                if (newSuccess != null) {
                    lastSuccessfulFetch = newSuccess;
                } else {
                    lastSuccessfulFetch = DateUtils.firstDateEver();
                }
            }
        }
    }

    public void setState(String name) {
        this.state = name;
    }

    public void setAccessKey(UUID uuid) {
        this.accessKey = uuid;
    }
}

