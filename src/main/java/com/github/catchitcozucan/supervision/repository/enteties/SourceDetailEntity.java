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
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Builder
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "source_detail")
public class SourceDetailEntity {
    public SourceDetailEntity() {}

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "source_detail_generator")
    @SequenceGenerator(name = "source_detail_generator", sequenceName = "source_detail_seq", allocationSize = 1)
    @Column(name = "id", updatable = false, nullable = false)
    private Integer id;

    @Version
    private Integer version;

    @OneToMany(fetch = FetchType.EAGER, orphanRemoval = true, cascade = CascadeType.ALL)
    @JoinTable(name = "detail_header", joinColumns={@JoinColumn(name="detail_id")},
            inverseJoinColumns={@JoinColumn(name="header_id")})
    private List<SourceHeaderEntity> headers;

    @Column(name = "proxy_host", columnDefinition = "varchar(70)", nullable = true)
    private String proxyHost;

    @Column(name = "proxy_port", columnDefinition = "int", nullable = true)
    private Integer proxyPort;

    @Column(name = "basic_auth_user", columnDefinition = "varchar(100)", nullable = true)
    private String basicAuthUsername;

    @Column(name = "basic_auth_pwd", columnDefinition = "varchar(100)", nullable = true)
    private String basicAuthPassword;

    @OneToOne(optional = false)
    @JoinColumn(nullable = false, updatable = false, name = "source_id", referencedColumnName = "id")
    private SourcelEntity sourcelEntity;

    public void setSourceEntity(SourcelEntity source) {
        this.sourcelEntity = source;
    }
}
