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
import './SourceDialog.css';
import {SourceDto} from "../../generated/api";
import {deleteTheSource, getSources, saveSource} from "../../service/supervisionService";
import {SourcesTable} from "../others/SourcesTable";
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome'
import {faBars, faEdit, faPlus} from "@fortawesome/free-solid-svg-icons";
import {formToSourceDto, FormValue, newEmpty, toForm} from "../../uiTypes/SourceFormDto";
import {SourceForm} from "../../uiTypes/SourceForm";

export interface SourceDialogProps {
    id: string;
    content: string;
    labelButton1: string;
    labelButton2: string;
    title: string;
    showSources: boolean;
    onSaveSource: (toSave: SourceDto) => void;
    onNoSourcesLeft: () => void;
    onClickButton1?: () => void;
    onClickButton2?: () => void;
}

export function SourceDialog(props: SourceDialogProps) {
    const dialogRef = useRef<HTMLDialogElement | null>(null);
    const [sources, setSources] = useState<SourceDto[]>([]);
    const [utilizedUrls, setUtilizedUrls] = useState<string[]>([]);
    const [formData, setFormData] = useState<FormValue[]>([]);
    const [dataKey, setDataKey] = useState<string>('');
    const [touched, setTouched] = useState<SourceDto | undefined>(undefined);
    const wasCalled = useRef(false);

    enum Mode {
        TABLE = 'TABLE',
        EDIT = 'EXIT'
    }

    const changeTouched = (touchedSrc: SourceDto | undefined) => {
        setTouched(touchedSrc);
    }

    const [mode, setMode] = useState<Mode>(Mode.TABLE);

    const selectedSource = (key: string) => {
        setDataKey(key);
        setMode(Mode.EDIT);
    }

    const toggleModalVisibility = (open?: boolean) => {
        if (open) {
            dialogRef.current?.showModal();
        } else if (dialogRef.current?.hasAttribute("open")) {
            dialogRef.current?.close();
        } else {
            dialogRef.current?.showModal();
        }
    };

    const deleteSource = (key: string) => {
        deleteTheSource(key).then(res => {
            reload();
        })
    }

    const reload = () => {
        getSources().then(resp => {
            if (props.showSources) {
                setSources(resp);
                let usedUrls: string[] = [];
                resp.forEach(r => {
                    usedUrls.push(r.accessUrl);
                })
                setUtilizedUrls(usedUrls);
            } else {
                setMode(Mode.EDIT);
            }
            const formDataRaw: FormValue[] = [];
            resp.forEach(s => {
                toForm(s).forEach(e => {
                    formDataRaw.push(e)
                });
            });

            if (resp.length > 0) {
                // sort it
                const indexedArray = formDataRaw.map((item, index) => ({index, value: item}));
                indexedArray.sort((a, b) => (a.value as FormValue).index - (b.value as FormValue).index);
                const sortedvalues = indexedArray.map((item) => item.value);
                setFormData(sortedvalues);
                if (sortedvalues && sortedvalues.length > 0 && sortedvalues[0].key) {
                    setDataKey(sortedvalues[0].key);
                }
            } else {
                props.onNoSourcesLeft();
            }
        });

    }

    useEffect(() => {
        if (wasCalled.current) {
            return;
        }
        wasCalled.current = true
        toggleModalVisibility(true);
        reload();
    });

    return (
        <>
            <dialog id={props.id} ref={dialogRef} draggable={false} title={props.title}
                    className={'dialogScreen noselect'}>
                <div id={'dialogTitle'} className={'titleStylee dialogTitle'}>
                    <div className='row' style={{'marginBottom': '-8px'}}>
                        <div className={'dialogTitleLeft col-8'}>&lt;{props.title}&gt;</div>
                        <div className={'dialogCancelHeader col-1'} onClick={() => {
                            if (props.onClickButton2) {
                                props.onClickButton2();
                            }
                            toggleModalVisibility();
                        }}>x
                        </div>
                    </div>
                </div>
                <div id={'dialogContent'} className={'dialogContent'}>
                    {mode === Mode.TABLE &&
                        <SourcesTable data={sources} deleteSource={deleteSource} selectedSource={selectedSource}/>
                    }
                    {mode === Mode.EDIT &&
                        <SourceForm utilizedUrls={utilizedUrls} data={formData} dataKey={dataKey}
                                    touchedCallback={changeTouched}/>
                    }
                </div>
                <div id={'dialogButtons'} className={'dialogButtonBar'}>
                    <span className={'dialogIcons'}>
                        <span className={'hamburgerHill'} onClick={() => setMode(Mode.TABLE)}>
                            <FontAwesomeIcon style={mode === Mode.TABLE ? {'color': '#5df605'} : {'color': '#bfc5ba'}}
                                             size={'xl'} icon={faBars}/>
                        </span>
                        <span className={'editHill'} onClick={() => {
                            if (sources.length > 0) {
                                setMode(Mode.EDIT);
                            }
                        }}>
                            <FontAwesomeIcon style={mode === Mode.EDIT ? {'color': '#5df605'} : {'color': '#bfc5ba'}}
                                             size={'xl'} icon={faEdit}/>
                        </span>
                        <span className={'plusHill'} onClick={(e) => {
                            e.preventDefault();
                            const newSource = newEmpty();
                            setFormData(newSource);
                            const srcDto: SourceDto = formToSourceDto(newSource);
                            if (srcDto.requestKey?.key) {
                                setDataKey(srcDto.requestKey?.key)
                            }
                            saveSource(srcDto).then(res => {
                                reload();
                                setMode(Mode.TABLE);
                            })
                        }}>
                            <FontAwesomeIcon size={'xl'} icon={faPlus}/>
                        </span>
                    </span>
                    <span className={'dialogButtons'}>
                        <button disabled={mode === Mode.TABLE || !touched} id={'dialogButtonSave'}
                                className="orangeButton dialogButton"
                                onClick={() => {
                                    if (touched !== undefined) {
                                        saveSource(touched).then(res => {
                                            props.onSaveSource(touched);
                                            if (props.onClickButton2) {
                                                props.onClickButton2();
                                            }
                                            toggleModalVisibility();
                                        })
                                    }
                                }} style={{'marginRight': '30px'}}>{props.labelButton1}</button>
                        <button id={'dialogButtonCancel'} className="orangeButton dialogButton" onClick={() => {
                            if (props.onClickButton2) {
                                props.onClickButton2();
                            }
                            toggleModalVisibility();
                        }}>{props.labelButton2}</button>
                    </span>
                </div>
            </dialog>
        </>
    );
}