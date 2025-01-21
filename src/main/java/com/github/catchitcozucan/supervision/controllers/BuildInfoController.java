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

import com.github.catchitcozucan.supervision.api.BuildInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/build")
@Slf4j
public class BuildInfoController {

    private static final String JSON_CHARSET_UTF_8 = "application/json; charset=UTF-8";

    @Autowired
    private BuildProperties buildProperties;

    @GetMapping(value = "/info", produces = JSON_CHARSET_UTF_8)
    public BuildInfo buildinfo() {
        return BuildInfo.builder()
                .version(buildProperties.getVersion())
                .name(buildProperties.getName())
                .group(buildProperties.getGroup())
                .artifact(buildProperties.getArtifact())
                .time(buildProperties.getTime().toString())
                .project_build_sourceEncoding(buildProperties.get("project.build.sourceEncoding"))
                .maven_compiler_source(buildProperties.get("maven.compiler.source"))
                .maven_compiler_target(buildProperties.get("maven.compiler.target"))
                .java_version(buildProperties.get("java.version"))
                .source_version(buildProperties.get("source.version"))
                .java_source(buildProperties.get("java.source"))
                .java_target(buildProperties.get("java.target"))
                .developer_node_version(buildProperties.get("developer.node.version"))
                .developer_npm_version(buildProperties.get("developer.npm.version"))
                .developer_maven_version(buildProperties.get("developer.maven.version"))
                .custom_coder(buildProperties.get("custom.coder"))
                .custom_description(buildProperties.get("custom.description"))
                .custom_copyright(buildProperties.get("custom.copyright"))
                .custom_copyright_link(buildProperties.get("custom.copyright.link"))
                .custom_developer_os(buildProperties.get("custom.developer.os"))
                .build();
    }
}
