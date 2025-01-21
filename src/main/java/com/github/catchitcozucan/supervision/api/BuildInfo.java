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
package com.github.catchitcozucan.supervision.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class BuildInfo {

    // defaults
    private String group;
    private String artifact;
    private String name;
    private String version;
    private String time;

    // custom props
    private String project_build_sourceEncoding;
    private String maven_compiler_source;
    private String maven_compiler_target;
    private String java_version;
    private String source_version;
    private String java_source;
    private String java_target;
    private String developer_node_version;
    private String developer_npm_version;
    private String developer_maven_version;
    private String custom_coder;
    private String custom_description;
    private String custom_copyright;
    private String custom_copyright_link;
    private String custom_developer_os;
}
