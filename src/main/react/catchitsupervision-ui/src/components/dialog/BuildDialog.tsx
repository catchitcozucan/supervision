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
import React, {useEffect, useRef, useState} from "react";
import './BuildDialog.css';
import {BuildInfo} from "../../generated/api";
import {buildInfo} from "../../service/buildService";

export interface BuildDialogProps {
    id: string;
    onClose: () => void;
}

export function BuildDialog(props: BuildDialogProps) {
    const dialogRef = useRef<HTMLDialogElement | null>(null);
    const [info, setInfo] = useState<BuildInfo>();

    const toggleModalVisibility = (open?: boolean) => {
        if (open) {
            dialogRef.current?.showModal();
        } else if (dialogRef.current?.hasAttribute("open")) {
            dialogRef.current?.close();
        } else {
            dialogRef.current?.showModal();
        }
    };

    const borderColor = 'black';

    useEffect(() => {
        toggleModalVisibility(true);
        buildInfo().then(res => {
            setInfo(res);
        })
    }, [props.id]);

    return (
        <>
            <dialog id={props.id} ref={dialogRef} draggable={false} title={'Build properties'}
                    className={'buildDialogScreen noselect'}>
                <div id={'buildDialogTitle'} className={'buildTitleStylee buildDialogTitle'}>
                    <div className='row'>
                        <div className={'buildDialogTitleLeft col-8'}>&lt;{'Build properties'}&gt;</div>
                        <div className={'buildDialogCancelHeader'} onClick={() => {
                            toggleModalVisibility();
                            props.onClose();
                        }}>x
                        </div>
                    </div>
                </div>
                <div id={'buildDialogContent'} className={'buildDialogContent'}>
                    <>
                        <div className={'buildTitleStylee'}>
                            <p/>
                            <div style={{borderColor: borderColor}} className={'borderConentStyle'}>
                                {info &&
                                    <>
                                        <label title={'Version:'}>Version:&nbsp;{info.version}</label><br/>
                                        <label title={'Build Time:'}>Build Time:&nbsp;{info.time}</label><br/>
                                        <label title={'Artifact:'}>Artifact:&nbsp;{info.artifact}</label><br/>
                                        <label title={'Group:'}>Group:&nbsp;{info.group}</label><br/>
                                        <label
                                            title={'Description:'}>Description:&nbsp;{info.custom_description}</label><br/>
                                        <label title={'Java source:'}>Java source:&nbsp;{info.java_source}</label><br/>
                                        <label title={'Java target:'}>Java target:&nbsp;{info.java_target}</label><br/>
                                        <label title={'Java version:'}>Java
                                            version:&nbsp;{info.java_version}</label><br/>
                                        <label title={'Default source encoding:'}>Default source
                                            encoding:&nbsp;{info.project_build_sourceEncoding}</label><br/>
                                        <label title={'Node version:'}>Node
                                            version:&nbsp;{info.developer_node_version}</label><br/>
                                        <label title={'Npm version:'}>Npm
                                            version:&nbsp;{info.developer_npm_version}</label><br/>
                                        <label title={'Maven version:'}>Maven
                                            version:&nbsp;{info.developer_maven_version}</label><br/>
                                        <label title={'Developer os:'}>Developer
                                            os:&nbsp;{info.custom_developer_os}</label><br/>
                                        <label title={'Developers:'}>Developers:&nbsp;{info.custom_coder}</label><br/>
                                        <div id={'buildLicense'} className={'buildLicense'}>
                                            <a rel="license" href="https://creativecommons.org/licenses/by/4.0/"><img
                                                alt="Creative Commons License" style={{borderWidth: 0}}
                                                src="https://licensebuttons.net/l/by/4.0/80x15.png"/></a><br/>
                                            This work is licensed under a
                                            <br/>
                                            <a className={'licenselink'} target="_blank;" rel="license"
                                               href="https://creativecommons.org/licenses/by/4.0/">Creative
                                                Commons Attribution 4.0 International (CC BY 4.0)</a>
                                        </div>
                                    </>
                                }
                            </div>
                        </div>
                    </>
                </div>
                <div id={'dialogButtons'} className={'buildDialogButtons'}>
                    <i className="fa-solid fa-bars"></i>
                    <button id={'buildDialogButtonCancel'} className="orangeButton buildDialogButton" onClick={() => {
                        toggleModalVisibility();
                        props.onClose();
                    }}>Close
                    </button>
                </div>
            </dialog>
        </>
    );
}