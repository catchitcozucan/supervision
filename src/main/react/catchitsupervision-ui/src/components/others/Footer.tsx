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
import './Footer.css';

export function Footer() {
    const yearNow = new Date().getFullYear();
    return (
        <>
            <div id="footer" className="footer">
                <table className="tableNoSpace noselect">
                    <tbody>
                    <tr>
                        <td style={{width: "90%"}}>
                            <div className="footerLeft">
                                <div className="theGrid">
                                    <a rel="license" href="https://creativecommons.org/licenses/by/4.0/"><img
                                        alt="Creative Commons License" style={{borderWidth: 0}}
                                        src="https://licensebuttons.net/l/by/4.0/80x15.png"/></a><br/>
                                    This work is licensed under a
                                    <br/>
                                    <a target="_blank;" rel="license"
                                       href="https://creativecommons.org/licenses/by/4.0/">Creative
                                        Commons Attribution 4.0 International (CC BY 4.0)</a>
                                </div>
                            </div>
                        </td>
                        <td className={'logoRightMobile'} style={{width: "10%"}}>
                            <div id="footerRight" className="footerLogo">
                                <div className="nollettnoll nollettnollMobile">
                                    Original work by Ola Aronsson 2025<br/>
                                    Courtesy of nollettnoll AB &#169; 2012 - {yearNow}
                                    <br/>
                                </div>
                            </div>
                        </td>
                    </tr>
                    </tbody>
                </table>
            </div>
        </>
    );
}