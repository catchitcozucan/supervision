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
import {formToSourceDto, FormValue} from "./SourceFormDto";
import React, {useEffect, useRef, useState} from "react";
import {FormElement} from "./FormElement";
import {SourceDto, SourceTestResponseDto, State} from "../generated/api";
import {testSource} from "../service/supervisionService";
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome'
import {faThumbsDown, faThumbsUp} from "@fortawesome/free-solid-svg-icons";
import {SpinnerInline} from "../components/spinner/SpinnerInline";
import '../components/dialog/SourceDialog.css'
import './SourceForm.css'


export interface SourceFormProps {
    data: FormValue[];
    utilizedUrls: string[];
    dataKey: string;
    touchedCallback: (touched: SourceDto | undefined) => void
}

export const SourceForm = (props: SourceFormProps): JSX.Element => {
    const formRef = useRef<HTMLFormElement | null>(null);
    const dataKey = props.dataKey;
    const [formData, setFormData] = useState<FormValue[]>([]);
    const [loading, setLoading] = useState<boolean>(false)
    const [testResult, setTestResult] = useState<SourceTestResponseDto | undefined>(undefined)
    const [initialData, setInititalData] = useState<Map<string, string> | undefined>(undefined);

    const urlIsUsedAlready = (url: string) => {
        let used = false;
        props.utilizedUrls.forEach(u => {
            if (!used) {
                used = u.includes(url);
                if (!used && url.includes(u)) {
                    const rest = url.replace(u, '');
                    if (!rest.includes('/')) { // ie - no identical url-prefix without a slash ti differ..
                        used = true;
                    }
                }
            }
        });
        return used;
    }

    const isThereAChange = (elements: HTMLFormControlsCollection) => {
        let thereIsAChange = false;
        if (!initialData) {
            return thereIsAChange;
        }
        let headerIndex = 0;
        let weHaveAUsedUrl: boolean = false;
        Array.from(elements).forEach((input, index) => {
            const inputElem = input as HTMLInputElement;
            if (inputElem.name === 'access url') {
                if (initialData.get('access url') !== inputElem.value && urlIsUsedAlready(inputElem.value)) {
                    weHaveAUsedUrl = true;
                }
            }
            if (!thereIsAChange && inputElem.name) {
                let label = inputElem.name;
                if (inputElem.name === 'name') {
                    headerIndex++;
                    label = label + headerIndex;
                } else if (inputElem.name === 'value') {
                    label = label + headerIndex;
                }
                let valueAsString = '';
                if (inputElem.value) {
                    valueAsString = '' + inputElem.value;
                }
                const valInitial = initialData.get(label);
                thereIsAChange = valueAsString !== valInitial;
            }
        });
        return thereIsAChange && !weHaveAUsedUrl;
    }

    useEffect(() => {
        const loadedData: FormValue[] = [];
        let headerIndex = 0;
        const init: Map<string, string> = new Map<string, string>();
        props.data.forEach(m => {
                if (m.key === dataKey) {
                    loadedData.push(m);
                    let label = m.label;
                    if (m.label === 'name') {
                        headerIndex++;
                        label = label + headerIndex;
                        let valueAsString = '';
                        if (m.value) {
                            valueAsString = '' + m.value;
                        }
                        init.set(label, valueAsString)
                        if (m.chained && m.chained.label === 'value') {
                            label = m.chained.label + headerIndex;
                            if (m.chained.value) {
                                valueAsString = '' + m.chained.value;
                            }
                            init.set(label, valueAsString)
                        }
                    } else {
                        let valueAsString = '';
                        if (m.value) {
                            valueAsString = '' + m.value;
                        }
                        init.set(label, valueAsString)
                    }
                }
            }
        )
        setFormData(loadedData);
        setInititalData(init);
    }, [props.data]);

    const testFormSource = (sourceDto: SourceDto) => {
        if (sourceDto.requestKey) {
            setLoading(true);
            testSource(sourceDto).then(res => {
                setLoading(false);
                setTestResult(res);
                if (res && res.histogram) {
                    let triggerProcNameChange = false;

                    // adjust the processname found
                    const newData: FormValue[] = [];
                    formData.forEach(f => {
                        if (!triggerProcNameChange && f.label === 'process name') {
                            triggerProcNameChange = true;
                        }
                        if (triggerProcNameChange) {
                            f.value = res.histogram.entityNames;
                            triggerProcNameChange = false;
                        }
                        newData.push(f);
                    })
                    setFormData(newData);
                }
            }).catch(e => {
                setLoading(false);
            });
        }
    }

    const getEditedData = (form: HTMLFormElement): FormValue[] => {
        const elements = form.elements as HTMLFormControlsCollection;
        const editedData: FormValue[] = [];
        const alreadyIn: string[] = [];
        Array.from(elements).forEach((input, index) => {
            const inputElem = input as HTMLInputElement;
            const newValue = {
                label: inputElem.name,
                value: inputElem.value,
                key: dataKey,
                error: '',
                type: inputElem.type,
                index: index
            } as FormValue;

            const key = newValue.label + newValue.value;
            if (key.length > 4 && !alreadyIn.includes(key)) {
                editedData.push(newValue);
                alreadyIn.push(key);
            }
        });
        return editedData;
    }

    return (
        <>
            <form ref={formRef}
                  onSubmit={(e: React.SyntheticEvent) => {
                      e.preventDefault();
                      return false;
                  }}
                  onChange={(e: React.SyntheticEvent) => {
                      e.preventDefault();
                      const somethingActullyHasChanged = isThereAChange((e.currentTarget as HTMLFormElement).elements as HTMLFormControlsCollection);
                      if (somethingActullyHasChanged) {
                          const editedData = getEditedData(e.currentTarget as HTMLFormElement);
                          const newData = formToSourceDto(editedData);
                          props.touchedCallback(newData);
                      } else {
                          props.touchedCallback(undefined);
                      }
                      return false;
                  }}
                  onReset={(e: React.SyntheticEvent) => {
                      e.preventDefault();
                      testFormSource(formToSourceDto(getEditedData(e.currentTarget as HTMLFormElement)));
                      return false;
                  }}>
                <div className={'container formPlayground'}>
                    <div className={'container'}>
                        {loading &&
                            <>
                                <div className={'spinnerTesterMobile spinnerTesterMid spinnerTesterLarge'}>
                                    <SpinnerInline/>
                                </div>
                            </>
                        }
                        <span className={'column'} style={{paddingLeft: '1%', textAlign: 'left', maxWidth: '10%'}}>
                            <button type="reset" id={'submitForm'} className="orangeButton dialogButton"
                                    style={{'marginRight': '30px'}} onClick={() => {
                            }}>Test
                            </button>
                        </span>
                        <span className={'column'} style={{paddingLeft: '1%', textAlign: 'left', maxWidth: '10%'}}>
                            <button id={'rezetButton'} className="orangeButton dialogButton"
                                    style={{'marginRight': '5px'}} onClick={(e) => {
                                e.preventDefault();
                                const form: HTMLFormElement | null = formRef.current;
                                if (form != null) {
                                    Array.from(form.elements).forEach(e => {
                                        if (e instanceof HTMLInputElement) {
                                            (e as HTMLInputElement).value = '';
                                        }
                                    });
                                }
                            }}>Reset
                                </button>
                                </span>
                        <span className={'column'} style={!loading ? {
                                marginTop: '0px',
                                paddingLeft: '71.6%',
                                fontSize: '0.7em',
                                textAlign: 'right'
                            } :
                            {
                                marginTop: '0px',
                                paddingLeft: '91%',
                                fontSize: '0.7em',
                                textAlign: 'right',
                                width: 'fit-content'
                            }
                        }>
                            {!loading && testResult &&
                                <>
                                    <span style={{textAlign: 'right', position: 'relative', right: '1%'}}>
                                        <span style={{paddingLeft: '4%', margin: 0}}>
                                            <FontAwesomeIcon
                                                style={testResult.state === State.AVAILABLE ? {'color': '#70ef05'} : {'color': '#ec1919'}}
                                                size={'xl'}
                                                icon={testResult.state === State.AVAILABLE ? faThumbsUp : faThumbsDown}/>
                                               <div style={{
                                                   margin: 0,
                                                   paddingTop: 2,
                                                   paddingBottom: 0,
                                                   textAlign: 'right'
                                               }}>
                                                   <span>State : </span><span style={{
                                                   color: '#08f80c',
                                                   fontWeight: 'normal'
                                               }}>{testResult.state.replaceAll('_', ' ').toLowerCase()}</span><br/>
                                                   <span>Exec time :</span><span style={{
                                                   color: '#08f80c',
                                                   fontWeight: 'normal'
                                               }}>{testResult.execTime} </span>
                                                   {testResult.state === State.AVAILABLE &&
                                                       <>
                                                           <br/>
                                                           <span>Process Name:</span><span style={{
                                                           color: '#08f80c',
                                                           fontWeight: 'normal'
                                                       }}>{testResult.histogram.entityNames} </span>
                                                       </>
                                                   }
                                                </div>
                                        </span>
                                    </span>
                                </>
                            }
                        </span>
                    </div>
                    {formData.map(m => {
                            return (
                                <FormElement formValue={m}/>
                            );
                        }
                    )}
                </div>
            </form>
        </>
    );
}