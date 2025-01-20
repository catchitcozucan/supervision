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
import {RequestKey, SourceDetailDto, SourceDto, SourceHeaderDto, State} from "../generated/api";
import {HTMLInputTypeAttribute} from "react";
import {IGNORE} from "./FormElement";

const HEADER_DUMMY = 'header_dummy';
const LABEL_HEADER_ID = 'header_id';
const LABEL_DETAIL_ID = 'detail_id';
const LABEL_SOURCE_ID = 'source_id';

export interface FormValue {
    label: string
    value: undefined | string | number,
    id: number | undefined,
    error: undefined | string,
    type: HTMLInputTypeAttribute,
    index: number,
    key?: string
    readonly?: boolean,
    sectionStarter?: boolean,
    chained?: FormValue
}

export const newEmpty = (dataKey?:string): FormValue[] => {
    const headerz: SourceHeaderDto[] = [];
    const header: SourceHeaderDto = {
        name: '',
        value: ''
    } as SourceHeaderDto;

    headerz.push(header);

    const detail: SourceDetailDto = {
        headers: headerz,
        proxyHost: '',
        proxyPort: -1,
        basicAuthUsername: '',
        basicAuthPassword: ''
    } as SourceDetailDto;

    const sourceDto: SourceDto = {
        domain: '',
        department: '',
        domainLabel: '',
        departmentLabel: '',
        processName: '',
        requestKey: {key: dataKey? dataKey : crypto.randomUUID().toString()},
        accessUrl: '',
        sourceDetailDto: detail,
        state: State.UNKNOWN
    } as SourceDto;

    return toForm(sourceDto);
}

export const toForm = (sourceDto: SourceDto): FormValue[] => {

    const itemKey = sourceDto.requestKey?.key;

    const formHeaders: FormValue[] = [];
    if (sourceDto.sourceDetailDto?.headers) {
        sourceDto.sourceDetailDto?.headers.forEach((h, index) => {
            formHeaders.push(
                ({
                    id: h.id,
                    sectionStarter: index === 0 ? true : false,
                    key: itemKey,
                    label: 'name',
                    value: h.name,
                    type: 'text',
                    index: (9 + formHeaders.length - index)
                } as FormValue)
            );
            formHeaders.push(
                ({
                    id: h.id,
                    key: itemKey,
                    label: 'value',
                    value: h.value,
                    type: 'text',
                    index: (9 + formHeaders.length - index)
                } as FormValue)
            );
            formHeaders.push(
                ({
                    id: h.id,
                    key: itemKey,
                    label: LABEL_HEADER_ID,
                    value: h.id ? h.id : undefined,
                    type: 'number',
                    index: IGNORE
                } as FormValue)
            );
        });
    }
    // this is so that we _always_ get the opportunity to
    // add headers even if we had none.
    if (formHeaders.length === 0) {
        formHeaders.push(
            ({
                sectionStarter: true,
                key: itemKey,
                label: HEADER_DUMMY,
                value: '',
                type: 'text',
                index: IGNORE,
                chained: {
                    key: itemKey,
                    label: HEADER_DUMMY,
                    value: '',
                    type: 'text',
                    index: IGNORE,
                } as FormValue
            } as FormValue)
        );
    }

    const formdetails: FormValue[] = [];
    formdetails.push(
        ({
            id: sourceDto.id,
            key: itemKey,
            label: LABEL_DETAIL_ID,
            value: sourceDto.id ? sourceDto.id : undefined,
            index: IGNORE
        } as FormValue)
    );
    formHeaders.forEach((h, index) => {
        if (h.index !== IGNORE && h.label === 'name') {
            h.chained = formHeaders[index + 1];
            formdetails.push(h);
        } else if (h.index === IGNORE) {
            formdetails.push(h);
        }
    });
    formdetails.push(
        ({
            key: itemKey,
            label: 'proxy host',
            value: sourceDto.sourceDetailDto?.proxyHost ? sourceDto.sourceDetailDto.proxyHost : undefined,
            type: 'text',
            index: 5,
            sectionStarter: true
        } as FormValue));
    formdetails.push(
        ({
            key: itemKey,
            label: 'proxy port',
            value: sourceDto.sourceDetailDto?.proxyPort ? sourceDto.sourceDetailDto.proxyPort : undefined,
            type: 'number',
            index: 6
        } as FormValue));
    formdetails.push(
        ({
            key: itemKey,
            label: 'basic auth username',
            value: sourceDto.sourceDetailDto?.basicAuthUsername ? sourceDto.sourceDetailDto.basicAuthUsername : undefined,
            type: 'text',
            index: 7
        } as FormValue));
    formdetails.push(
        ({
            key: itemKey,
            label: 'basic auth password',
            value: sourceDto.sourceDetailDto?.basicAuthPassword ? sourceDto.sourceDetailDto.basicAuthPassword : undefined,
            type: 'password',
            index: 8
        } as FormValue));

    const formSource: FormValue[] = [];
    formSource.push(
        ({
            id: sourceDto.id,
            key: itemKey,
            label: 'domain label',
            value: sourceDto.domainLabel,
            type: 'text',
            index: IGNORE
        } as FormValue)
    );
    formSource.push(
        ({
            key: itemKey,
            label: 'department label',
            value: sourceDto.departmentLabel,
            type: 'text',
            index: IGNORE
        } as FormValue)
    );
    formSource.push(
        ({
            key: itemKey,
            label: 'state',
            value: sourceDto.state,
            type: 'text',
            index: IGNORE,
            readonly: true,
            sectionStarter: true
        } as FormValue)
    );
    formSource.push(
        ({
            sectionStarter: true,
            key: itemKey,
            label: 'domain',
            value: sourceDto.domain,
            type: 'text',
            index: 1
        } as FormValue)
    );
    formSource.push(
        ({key: itemKey, label: 'department', value: sourceDto.department, type: 'text', index: 2} as FormValue)
    );
    formSource.push(
        ({
            key: itemKey,
            label: 'process name',
            value: sourceDto.processName ? sourceDto.processName : 'UNTESTED',
            type: 'text',
            index: IGNORE,
            readonly: true
        } as FormValue)
    );
    formSource.push(
        ({key: itemKey, label: 'access url', value: sourceDto.accessUrl, type: 'text', index: 4} as FormValue)
    );
    formSource.push(
        ({
            id: sourceDto.id,
            key: itemKey,
            label: LABEL_SOURCE_ID,
            value: sourceDto.id ? sourceDto.id : undefined,
            type: 'number',
            index: IGNORE
        } as FormValue)
    );
    formSource.push(
        ({
            key: itemKey,
            label: 'request key',
            value: sourceDto.requestKey ? sourceDto.requestKey.key : undefined,
            type: 'text',
            index: IGNORE
        } as FormValue)
    );
    formdetails.forEach(d => formSource.push(d));
    return formSource;
}

export const formToSourceDto = (formData: FormValue[]): SourceDto => {

    let detailId = -1;
    let proxyHost = '';
    let proxyPort = -1;
    let basicAuthUsername = '';
    let basicAuthPassword = '';
    let sourceId = -1;
    let domain = '';
    let department = '';
    let domainLabel = '';
    let departmentLabel = '';
    let processName = '';
    let requestKey = {key: ''} as RequestKey;
    let accessUrl = '';
    let state = State.UNKNOWN;

    const headersPerId: Map<number, FormValue[] | undefined> = new Map();
    formData.forEach(data => {
        let key = data.id ? data.id : -data.index;
        if (data.label === 'name' || data.label === 'value') {
            if (data.label === 'value') {
                if (key < 0) {
                    key = key + 1;
                } else {
                    key = key - 1;
                }
            }
            let datas: FormValue[] | undefined = headersPerId.get(key);
            if (!datas) {
                datas = [];
            }
            datas.push(data);
            headersPerId.set(key, datas);
        } else {
            switch (data.label) {
                case LABEL_DETAIL_ID :
                    detailId = data.value as number;
                    break;
                case 'proxy host' :
                    proxyHost = data.value as string;
                    break;
                case 'proxy port' :
                    proxyPort = data.value ? data.value as number : -1;
                    break;
                case 'basic auth username' :
                    basicAuthUsername = data.value as string;
                    break;
                case 'basic auth password' :
                    basicAuthPassword = data.value as string;
                    break;
                case LABEL_SOURCE_ID :
                    sourceId = data.value as number;
                    break;
                case 'domain' :
                    domain = data.value as string;
                    requestKey = {key: data.key as string} as RequestKey;
                    break;
                case 'department' :
                    department = data.value as string;
                    break;
                case 'process name' :
                    processName = data.value as string;
                    break;
                case 'access url' :
                    accessUrl = data.value as string;
                    break;
                case 'domain label' :
                    domainLabel = data.value as string;
                    break;
                case 'domadepartment label' :
                    departmentLabel = data.value as string;
                    break;
                case 'state':
                    state = data.value as unknown as State;
                    break;
            }
        }

    });

    // so... now they are grouped proper and we know that if
    // the number if a negative then it was not from the database
    // - ie, it is NOT saved yet
    const headers: SourceHeaderDto[] = [];
    headersPerId.forEach((values, key) => {
        if (values && values.length === 2) {
            let name = '';
            let valueStr = '';
            if (values[0].label === 'name') {
                name = '' + values[0].value;
                valueStr = '' + values[1].value;
            } else {
                name = '' + values[1].value;
                valueStr = '' + values[0].value;
            }
            const header: SourceHeaderDto = {
                id: key && key > 0 ? key : undefined,
                name: name,
                value: valueStr
            } as SourceHeaderDto;
            headers.push(header);
        }
    });

    const detail = {
        id: detailId < 0 ? undefined : detailId,
        headers: headers,
        proxyHost: proxyHost,
        proxyPort: proxyPort < 0 ? undefined : proxyPort,
        basicAuthUsername: basicAuthUsername,
        basicAuthPassword: basicAuthPassword,
    } as SourceDetailDto;

    const source = {
        id: sourceId < 0 ? undefined : sourceId,
        domain: domain,
        department: department,
        domainLabel: domainLabel,
        departmentLabel: departmentLabel,
        processName: processName,
        requestKey: requestKey,
        accessUrl: accessUrl,
        sourceDetailDto: detail,
        state: state,
    } as SourceDto;

    return source;
}
