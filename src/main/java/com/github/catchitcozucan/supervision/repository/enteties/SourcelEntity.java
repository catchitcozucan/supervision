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
package com.github.catchitcozucan.supervision.repository.enteties;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Date;
import java.util.UUID;

@lombok.Builder
@AllArgsConstructor
@Getter
@Entity
@Table(name = "source")
public class SourcelEntity {

    public SourcelEntity() {}

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "source_generator")
    @SequenceGenerator(name = "source_generator", sequenceName = "source_seq", allocationSize = 1)
    @Column(name = "id", updatable = false, nullable = false)
    private Integer id;

    @Version
    private Integer version;

    @Column(name = "touched", columnDefinition = "date", nullable = false)
    private Date touched;

    @Column(name = "domain", columnDefinition = "varchar(100)", nullable = false)
    private String domain;

    @Column(name = "department", columnDefinition = "varchar(100)", nullable = false)
    private String department;

    @Column(name = "process_name", columnDefinition = "varchar(100)", nullable = false)
    private String processName;

    @Column(name = "access_url", nullable = false)
    private String accessUrl;

    @Column(name = "access_key", columnDefinition = "UUID", nullable = false)
    private UUID accessKey;

    @Column(name = "disabled", columnDefinition = "bool", nullable = false)
    private Boolean disabled;

    @OneToOne(optional = false, orphanRemoval = true, cascade = CascadeType.ALL)
    @JoinColumn(nullable = false, updatable = false, name = "id", referencedColumnName = "source_id")
    private SourceDetailEntity sourceDetailEntity;

    public void setTouched(Date date) {
        this.touched = date;
    }

    public void override(SourcelEntity newData) {
        this.domain = newData.getDomain();
        this.department = newData.getDepartment();
        this.processName = newData.getProcessName();
        this.accessUrl = newData.getAccessUrl();
        setTouched(new Date());
        SourceDetailEntity detailToOverride = getSourceDetailEntity();
        SourceDetailEntity incomingDetail = newData.getSourceDetailEntity();
        if (incomingDetail.getHeaders() == null || incomingDetail.getHeaders().isEmpty()) {
            detailToOverride.getHeaders().clear();
        } else {
            detailToOverride.getHeaders().clear();
            incomingDetail.getHeaders().stream().forEach(h -> {
                detailToOverride.getHeaders().add(h);
            });
        }
        detailToOverride.setProxyHost(incomingDetail.getProxyHost());
        detailToOverride.setProxyPort(incomingDetail.getProxyPort());
        detailToOverride.setBasicAuthUsername(incomingDetail.getBasicAuthUsername());
        detailToOverride.setBasicAuthPassword(incomingDetail.getBasicAuthPassword());
        sourceDetailEntity = detailToOverride;
    }
    public void disable() {
        this.disabled = Boolean.TRUE;
    }

    public void removeDetail(){
        this.sourceDetailEntity = null;
    }
}

