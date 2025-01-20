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
import {SourceDto, State} from "../../generated/api";
import './SourcesTable.css'
import {useState} from "react";
import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import {faMinus} from "@fortawesome/free-solid-svg-icons";
import '../../main_style/noSources.css'

export interface SourcesTableProps {
    data: SourceDto[];
    selectedSource: (key: string) => void;
    deleteSource: (key: string) => void;
}

export function SourcesTable(props: SourcesTableProps) {

    const [selectIsDisabled, setSelectIsDisabled] = useState<boolean>(false);

    return (
        <>
            <div>
                <div>
                    {props.data.length === 0 &&
                        <>
                                <div id={'row1wdw'} className={'noSourcesContainer'}>
                                    <div>You have no sources - please use the plus-button to start adding sources!</div>
                                    <div>There are no sources - use the plus-button to start adding sources!</div>
                                    <div>You haven't added any sources - add them now!</div>
                                    <div>Lacking sources - there's nothing to work!</div>
                                    <div>Whooop! No sources! Whooop! No sources! Whooop! No sources!</div>
                                </div>

                        </>
                    }
                    {props.data.map((source, index) => {
                        return (
                            <div className={'container tableOuter'}>
                                {index === 0 &&
                                    <>
                                        <div id={source.processName + 'row1'} className={'row'}>
                                            <div id={source.domain + Math.random()}
                                                 className={'col-1 tableCell tableHeading'}>{source.domainLabel}</div>
                                            <div id={source.domain + Math.random()}
                                                 className={'col-1 tableCell tableHeading'}>{source.departmentLabel}</div>
                                            <div id={source.domain + Math.random()}
                                                 className={'col-1 tableCell tableHeading'}>Process name
                                            </div>
                                            <div id={source.domain + Math.random()}
                                                 className={'col-1 tableCell tableHeading'}>State
                                            </div>
                                            <div id={source.domain + Math.random()}
                                                 className={'col-1 tableCell tableHeading tableCellMinus'}>
                                            </div>
                                        </div>
                                    </>
                                }
                                <div id={source.processName + 'row2'} className={'row selectableRow'} onClick={(e) => {
                                    if (!selectIsDisabled && source.requestKey) {
                                        props.selectedSource(source.requestKey.key);
                                    }
                                }}>
                                    <div id={source.domain + Math.random()}
                                         className={'col-1 tableCell'}>{source.domain}</div>
                                    <div id={source.domain + Math.random()}
                                         className={'col-1 tableCell'}>{source.department}</div>
                                    <div id={source.domain + Math.random()}
                                         className={'col-1 tableCell'}>{source.processName}</div>
                                    <div id={source.domain + Math.random()}
                                         className={'col-1 tableCell'}>{source.state}</div>
                                    <div id={source.domain + Math.random()}
                                         className={'col-1 tableCell tableCellMinus'}>
                                        <span className={!source.state || source.state !== State.DEMO_MODE ? 'minusHill' : 'disabledMinusButtonDisabled'} onClick={(e) => {
                                            e.preventDefault();
                                            if (!source.state || source.state !== State.DEMO_MODE) {
                                                if (source.requestKey) {
                                                    props.deleteSource(source.requestKey.key);
                                                    setSelectIsDisabled(false);
                                                }
                                            }
                                            return
                                        }} onMouseOut={() => {
                                            setSelectIsDisabled(false);
                                        }} onMouseOver={() => {
                                            setSelectIsDisabled(true);
                                        }}>
                                            <FontAwesomeIcon
                                                className={source.state && source.state === State.DEMO_MODE ? 'disabledMinus' : ''}
                                                size={'sm'} icon={faMinus}/>
                                         </span>
                                    </div>
                                </div>
                                {index === props.data.length - 1 &&
                                    <div id={source.processName + 'row2'} className={'row'}>
                                        <div className={'tableLastLine'}></div>
                                        <div className={'tableLastLine'}></div>
                                        <div className={'tableLastLine'}></div>
                                        <div className={'tableLastLine'}></div>
                                        <div className={'minusLine'}></div>
                                    </div>
                                }
                            </div>
                        )
                    })}
                </div>
            </div>
        </>
    );
}