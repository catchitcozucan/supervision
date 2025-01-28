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
import {WebResponse} from "./WebResponse";

let baseUrl = 'http://' + window.location.hostname + ':8080/catchitsupervision'; // for local DEV
if (['production', 'staging'].includes(process.env.NODE_ENV)) {
    baseUrl = '/catchitsupervision';
}
export const BASE_URL = baseUrl;

export async function makeCall(url: string, method?: string, payLoad?: any) {
    return fetch(url, {
        mode: 'cors',
        headers: {
            'Content-Type': 'application/json; charset=UTF-8',
            'Accept': 'application/json; charset=UTF-8',
        },
        body: payLoad ? payLoad : null,
        credentials: 'include',
        method: method ? method : 'GET',
    }).then(response => {
        if (response.ok) {
            return response.text().then(json => {
                return {
                    response: json,
                    httpCode: response.status,
                    isOk: true
                } as WebResponse<string>
            });
        } else {
            return {
                isOk: false,
                response: '',
                httpCode: response.status,
                errorMessage: response.statusText
            } as WebResponse<string>
        }
    }).catch(err => {
        return {
            isOk: false,
            response: '',
            httpCode: 500,
            errorMessage: err.toString()
        } as WebResponse<string>
    });
}
