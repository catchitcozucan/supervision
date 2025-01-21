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
import {BuildInfo,} from "../generated/api";
import {BASE_URL, makeCall} from "../base/basicFetchService";

const URL_PREFIX = BASE_URL + '/build/';

export interface Buildservice {
    info(): Promise<BuildInfo>;
}

class BuildserviceImpl implements Buildservice {
    async info(): Promise<BuildInfo | any> {
        const url = URL_PREFIX + 'info';
        return makeCall(url).then(resp => {
            if (resp.isOk && resp.response) {
                return JSON.parse(resp.response) as BuildInfo;
            } else {
                console.error("Request went bad " + resp.toString());
                return '';
            }
        });
    }
}

const buildServiceInstance = new BuildserviceImpl();

export function buildInfo(): Promise<BuildInfo> {
    return buildServiceInstance.info();
}