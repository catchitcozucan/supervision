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
import {DNA} from "react-loader-spinner";
import './Spinner.css'

export function SpinnerInline() {
    return (
        <>
            <div className={'overlayInline'}>
                <DNA
                    visible={true}
                    height="91"
                    width="91"
                    ariaLabel="dna-loading"
                    wrapperStyle={{
                        'position': 'relative',
                        'width': '100%',
                        'height': '100%',
                        'justifyContent': 'center',
                        'alignItems': 'center'
                }}
                    wrapperClass="dna-wrapper"
                />
            </div>
        </>
    )
}