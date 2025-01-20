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
import {LoggedInResponse, Login, UserAndHashedPassword,} from "../generated/api";
import {BASE_URL, makeCall} from "../base/basicFetchService";

const URL_PREFIX= BASE_URL + '/login/';

export interface Loginservice {
    login(pwd: string): Promise<UserAndHashedPassword>;

    setAdminPasswordAndLogin(pwd: string): Promise<UserAndHashedPassword>;

    adminIsLoggedIn(): Promise<boolean>;
}

class LoginserviceImpl implements Loginservice {
    async login(pwd: string): Promise<UserAndHashedPassword | any> {
        const url = URL_PREFIX + 'verify';
        const adminUser: Login = {
            userName: 'admin',
            password: pwd
        };
        return makeCall(url, 'POST', JSON.stringify(adminUser)).then(resp => {
            if (resp.isOk && resp.response) {
                return JSON.parse(resp.response) as UserAndHashedPassword;
            } else {
                console.error("Request went bad " + resp.toString());
                return '';
            }
        });
    }

    async setAdminPasswordAndLogin(pwd: string): Promise<UserAndHashedPassword | any> {
        const url = URL_PREFIX + 'set';
        const adminUser: Login = {
            userName: 'admin',
            password: pwd
        };
        return makeCall(url, 'PUT', JSON.stringify(adminUser)).then(resp => {
            if (resp.isOk && resp.response) {
                return JSON.parse(resp.response) as UserAndHashedPassword;
            } else {
                console.error("Request went bad " + resp.toString());
                return '';
            }
        });
    }

    async adminIsLoggedIn(): Promise<boolean | any> {
        const url = URL_PREFIX + 'loggedin';
        return makeCall(url).then(resp => {
            if (resp.isOk && resp.response) {
                const logginIn : LoggedInResponse = (JSON.parse(resp.response) as LoggedInResponse);
                return logginIn.isLoggedIn;
            } else {

                console.error("Request went bad " + resp.toString());
                return '';
            }
        });
    }
}

const loginServiceInstance = new LoginserviceImpl();

export function adminIsLoggedin(): Promise<boolean> {
    return loginServiceInstance.adminIsLoggedIn();
}

export async function login(pwd: string): Promise<UserAndHashedPassword | undefined> {
    const user = await loginServiceInstance.login(pwd);
    if (typeof user === 'string') {
        return undefined;
    }
    return user;
}

export async function setAdminPasswordAndLogin(pwd: string): Promise<UserAndHashedPassword | undefined> {
    const user = await loginServiceInstance.setAdminPasswordAndLogin(pwd);
    if (typeof user === 'string') {
        return undefined;
    }
    return user;
}