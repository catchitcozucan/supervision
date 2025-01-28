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
import {
    DemoMode,
    DepartmentProcessSummary,
    DepartmentSummary,
    DomainSummary,
    HierarchyResponse,
    Histogram,
    SourceDto,
    SourceTestResponseDto,
} from "../generated/api";
import {BASE_URL, makeCall} from "../base/basicFetchService";
import {FetchError} from "../base/FetchError";

const URL_PREFIX = BASE_URL + '/supervision/';

export interface SupervisionFetcher {
    getDomainSummary(): Promise<DomainSummary[]>; // get summary regarding all known group-process entities
    getDepartmentSummary(domain: string): Promise<DepartmentSummary[]>; // fetch top-level-oriented response for a named group
    getDepartmentProcessSummary(domain: string, department: string): Promise<DepartmentProcessSummary[]>; // fetch top-level-oriented response, one per group-process
    getHistogram(domain: string, department: string, processName: string, flipFailures: boolean, returnOnlyFailures: boolean): Promise<Histogram>; // get current histogram for a process that is a member os a group of processes
    inDemoMode(): Promise<boolean>

    getSources(): Promise<SourceDto[] | string>

    getHierarchy(): Promise<string>

    testSource(source: SourceDto): Promise<SourceTestResponseDto>

    saveSource(sourceDto: SourceDto): Promise<SourceTestResponseDto>

    deleteTheSource(key: string): Promise<boolean>

    getLatestResult(key: string, flip: boolean, failOnly: boolean): Promise<SourceTestResponseDto>
}

class Supervisionservice implements SupervisionFetcher {
    async getHistogram(domain: string, department: string, processName: string, flipFailures: boolean, returnOnlyFailures: boolean): Promise<Histogram | any> {
        const url = URL_PREFIX + 'histogram/domain/' + encodeURIComponent(domain) + '/department/' + encodeURIComponent(department)
            + '/process/' + encodeURIComponent(processName)
            + '?flipFailures=' + flipFailures
            + '&returnOnlyFailures=' + returnOnlyFailures;
        return makeCall(url).then(resp => {
            if (resp.isOk && resp.response) {
                return JSON.parse(resp.response) as Histogram;
            } else {
                console.error("Request getHistogram() went bad " + resp.toString());
                return '';
            }
        });
    }

    async getDomainSummary(): Promise<DomainSummary[]> {
        const url = URL_PREFIX + 'domainSummaries';
        return makeCall(url).then(resp => {
            if (resp.isOk && resp.response) {
                return JSON.parse(resp.response) as DomainSummary[];
            } else {
                return [];
            }
        });
    }

    async getDepartmentSummary(domain: string): Promise<DepartmentSummary[]> {
        const url = URL_PREFIX + 'departmentSummaries/domain/' + encodeURIComponent(domain);
        return makeCall(url).then(resp => {
            if (resp.isOk && resp.response) {
                return JSON.parse(resp.response) as DepartmentSummary[];
            } else {
                console.error("Request getDepartmentSummary() went bad " + resp.toString());
                return [];
            }
        });
    }

    async getDepartmentProcessSummary(domain: string, department: string): Promise<DepartmentProcessSummary[]> {
        const url = URL_PREFIX + 'departmentProcesses/domain/' + encodeURIComponent(domain) + '/department/' + encodeURIComponent(department);
        return makeCall(url).then(resp => {
            if (resp.isOk && resp.response) {
                return JSON.parse(resp.response) as DepartmentProcessSummary[];
            } else {
                console.error("Request getDepartmentProcessSummary() went bad " + resp.toString());
                return [];
            }
        });
    }

    async inDemoMode(): Promise<boolean> {
        const url = URL_PREFIX + 'demoMode';
        return makeCall(url).then(resp => {
            if (resp.isOk && resp.response) {
                return (JSON.parse(resp.response) as DemoMode).isDemoMode;
            } else {
                console.error("Request in demo mode went bad " + resp.toString());
            }
            return true;
        });
    }

    getSources(): Promise<SourceDto[] | string> {
        const url = URL_PREFIX + 'sources';
        return makeCall(url).then(resp => {
            if (resp.isOk && resp.response) {
                return JSON.parse(resp.response) as SourceDto[];
            } else if (resp.httpCode === 403) {
                return "NOT ALLOWED - YOU ARE NOT LOGGED IN!";
            } else {
                throw new FetchError("getSources() FAILED!", resp);
            }
        });
    }

    getHierarchy(): Promise<string> {
        const url = URL_PREFIX + 'hierarchy';
        return makeCall(url).then(resp => {
            if (resp.isOk && resp.response) {
                return (JSON.parse(resp.response) as HierarchyResponse).hierarchy;
            } else {
                console.error("Request get hiearchy went bad " + resp.toString());
            }
            return 'domain.department';
        });
    }

    testSource(source: SourceDto): Promise<SourceTestResponseDto> {
        const url = URL_PREFIX + 'testsource';
        return makeCall(url, 'POST', JSON.stringify(source)).then(resp => {
            if (resp.isOk && resp.response) {
                return JSON.parse(resp.response) as SourceTestResponseDto;
            } else {
                console.error("Request test source went bad " + JSON.stringify(resp))
                throw new FetchError("testSource() FAILED!", resp);
            }
        });
    }

    saveSource(sourceDto: SourceDto): Promise<SourceTestResponseDto> {
        const url = URL_PREFIX + 'savesource';
        return makeCall(url, 'POST', JSON.stringify(sourceDto)).then(resp => {
            if (resp.isOk && resp.response) {
                return JSON.parse(resp.response) as SourceTestResponseDto;
            } else {
                console.error("Request save the source" + JSON.stringify(resp))
                throw new FetchError("saveSource() FAILED!", resp);
            }
        });
    }

    deleteTheSource(key: string): Promise<boolean> {
        const url = URL_PREFIX + 'deletesource/' + key;
        return makeCall(url, 'DELETE').then(resp => {
            if (resp.isOk && resp.response) {
                return true;
            } else {
                console.error("Request delete source went bad" + JSON.stringify(resp))
                return false;
            }
        });
    }

    getLatestResult(key: string, flip: boolean, failOnly: boolean): Promise<SourceTestResponseDto> {
        const url = URL_PREFIX + 'getLastestResult/requestKey/' + key + '/flip/'
            + flip
            + '/failOnly/'
            + failOnly;
        return makeCall(url).then(resp => {
            if (resp.isOk && resp.response) {
                return JSON.parse(resp.response) as SourceTestResponseDto;
            } else {
                throw new FetchError("getLatestResult() FAILED!", resp);
            }
        });
    }
}


const statisticServiceInstance = new Supervisionservice();

export function getHistogram(domain: string, groupName: string, processName: string, flipFailures: boolean, returnOnlyFailures: boolean): Promise<Histogram> {
    return statisticServiceInstance.getHistogram(domain, groupName, processName, flipFailures, returnOnlyFailures);
}

export function getDomainSummary(): Promise<DomainSummary[]> {
    return statisticServiceInstance.getDomainSummary();
}

export function getDepartmentSummary(domain: string): Promise<DepartmentSummary[]> {
    return statisticServiceInstance.getDepartmentSummary(domain);
}

export function getDepartmentProcessSummary(domain: string, department: string): Promise<DepartmentProcessSummary[]> {
    return statisticServiceInstance.getDepartmentProcessSummary(domain, department);
}

export function inDemoMode(): Promise<boolean> {
    return statisticServiceInstance.inDemoMode();
}

export function getSources(): Promise<SourceDto[] | string> {
    return statisticServiceInstance.getSources();
}

export function getHierarchy(): Promise<string> {
    return statisticServiceInstance.getHierarchy();
}

export function testSource(source: SourceDto): Promise<SourceTestResponseDto> {
    return statisticServiceInstance.testSource(source);
}

export function saveSource(sourceDto: SourceDto): Promise<SourceTestResponseDto> {
    return statisticServiceInstance.saveSource(sourceDto);
}

export function deleteTheSource(key: string): Promise<boolean> {
    return statisticServiceInstance.deleteTheSource(key);
}

export function getLatestResult(key: string, flip: boolean, failOnly: boolean): Promise<SourceTestResponseDto> {
    return statisticServiceInstance.getLatestResult(key, flip, failOnly);
}
