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
import {FormValue} from "./SourceFormDto";
import {useEffect, useState} from "react";
import './FormElement.css'
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome'
import {faMinusCircle, faPlusCircle} from "@fortawesome/free-solid-svg-icons";

export interface FormElementProps {
    formValue: FormValue;
}

export const READ_ONLY = 666;
export const IGNORE = 777;
export const HIDDEN = 999;

export const FormElement = (props: FormElementProps): JSX.Element => {
    const [model, setModel] = useState<FormValue>(props.formValue);
    const [extraModels, setExtraModels] = useState<FormValue[]>([]);

    const removeLastExtra = (index?: number) => {
        if (extraModels && extraModels.length > 1) {
            const last = extraModels.length - 1;
            const newValues: FormValue[] = [];
            extraModels.forEach((e, index) => {
                if (index < last - 1) {
                    newValues.push(e);
                } else {
                    e.value = '';
                }
            });
            setExtraModels(newValues);
        }
    }
    const addOneExtra = (modelNew?: FormValue) => {
        const baseNumber = 11;
        const updatedExtras: FormValue[] = [];
        extraModels.forEach(e => updatedExtras.push(e));

        // then it this is a 'transformer' we've
        // got the model to transform. otherwise
        // it's just 'add-click'
        const extraModelNameRaw: FormValue = {
            label: modelNew ? modelNew.label : 'name',
            value: modelNew ? modelNew.value : '',
            type: modelNew ? modelNew.type : model.type,
            index: modelNew ? modelNew.index : baseNumber + extraModels.length + 1,
            key: modelNew ? modelNew.key : (model.key ? model.key + '_key_' + model.index + 1 : 'key_name' + model.index + 1)
        } as FormValue;
        updatedExtras.push(extraModelNameRaw);
        if (!modelNew) {
            const extraModelValueRaw = {
                label: 'value',
                value: '',
                type: model.type,
                index: baseNumber + extraModels.length + 2,
                key: (model.key ? model.key + '_value_' + model.index + 1 : 'key_value' + model.index + 1)
            } as FormValue;
            updatedExtras.push(extraModelValueRaw);
        } else if (modelNew.chained) {
            const extraModelValueRaw = {
                label: modelNew.chained.label,
                value: modelNew.chained.value,
                type: modelNew.chained.type,
                index: modelNew.chained.index,
                key: modelNew.chained.key
            } as FormValue;
            updatedExtras.push(extraModelValueRaw);
        }
        setExtraModels(updatedExtras);
    }

    useEffect(() => {
            if (props.formValue.chained) {
                if (props.formValue.index === IGNORE) {
                    addOneExtra();
                } else {
                    addOneExtra(props.formValue);
                }
                setModel({
                    ...model,
                    index: HIDDEN
                });
            }
            setModel({
                ...model,
                chained: undefined
            });
        }, [props.formValue]
    );

    if (extraModels.length === 0 && model.index === IGNORE) {
        return <span/>;
    }

    return (
        <>
            <div id={props.formValue.index + 'formElem'} className={'formElementWrapper'}>
                {props.formValue.sectionStarter ?
                    <>
                        <div className={'formWrapper'}>
                            {props.formValue.index > 7 &&
                                <>
                                    <div className={'container col-13 plusBar'}>
                                        <span>headers</span>
                                        <div className={'plusIcon'}>
                                        <span onClick={() => {
                                            addOneExtra();
                                        }} className={'thePlus'}><FontAwesomeIcon size={'xl'}
                                                                                  icon={faPlusCircle}/></span>
                                        </div>
                                    </div>
                                </>
                            }
                            {model.label !== 'name' && (model.index !== HIDDEN && model.index !== IGNORE && props.formValue.index < 9) &&
                                <label>
                                    {model.label}
                                </label>
                            }
                        </div>
                    </> :
                    <>
                        {model.label !== 'name' && model.index !== HIDDEN && props.formValue.index < 9 &&
                            <>
                                {model.label === 'process name' ? <span className={'spacerHeight'}/> : <span/>}
                                <label>
                                    {model.label}
                                    {model.label === 'process name' ? ':' : ''}
                                </label>
                            </>
                        }
                    </>
                }
                <span>
                        {model.label !== 'name' && props.formValue.index < 9 && props.formValue.index !== HIDDEN && props.formValue.index !== READ_ONLY && model.index !== IGNORE && !props.formValue.readonly &&
                            <>
                                <input className={'inputForm'} onChange={(e) => {
                                    setModel({
                                        ...model,
                                        value: e.target.value,
                                    });
                                }
                                } type={model.type} name={model.label} value={model.value ? model.value : undefined}/>
                            </>
                        }
                    {model.label !== 'name' && ((props.formValue.index < 9 && props.formValue.index === READ_ONLY) || props.formValue.readonly) &&
                        <>
                            <span className={'valueText'}>&nbsp;{model.value}</span>
                            {model.label === 'process name' ? <span className={'spacerHeight'}/> : <span/>}
                        </>
                    }
                  </span>
                {extraModels && extraModels.length > 0 && extraModels[0].index !== IGNORE && extraModels.map((extra, index) => {
                    return (<>
                        <br/>
                        <label>
                            {extra.label}
                            {extra.label === 'name' &&
                                <>
                                <span onClick={() => {
                                    removeLastExtra();
                                }} className={'theMinus'}><FontAwesomeIcon
                                    size={'sm'} icon={faMinusCircle}/></span>
                                </>
                            }

                        </label>
                        <input className={'inputForm'} onChange={(e) => {
                            const extraNew = ({
                                ...extra,
                                value: e.target.value,
                            });
                            const newModelz: FormValue[] = [];
                            extraModels.forEach(e => {
                                if (e.index === extraNew.index) {
                                    e.value = extraNew.value;
                                }
                                newModelz.push(e);
                            });
                            setExtraModels(newModelz);
                        }
                        } type={extra.type} name={extra.label}
                               value={extra.value ? extra.value : undefined}/>
                    </>);
                })
                }
            </div>
        </>
    );
}