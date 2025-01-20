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
import React, {useEffect, useRef, useState} from 'react';
import './main_style/App.css';
import logo_transperent_smaller from './images/logo_transperent_smaller_invert_supervision3.png'
import {DepartmentProcessSummary, DepartmentSummary, DomainSummary, SourceTestResponseDto} from "./generated/api";
import {
    getDepartmentProcessSummary,
    getDepartmentSummary,
    getDomainSummary,
    getHierarchy,
    getLatestResult,
    inDemoMode,
} from "./service/supervisionService";
import {Piechart, PieProps} from "./components/graph/Pie";
import {Footer} from "./components/others/Footer";
import {HistogramGraph} from "./components/graph/HistogramGraph";
import {SourceDialog} from "./components/dialog/SourceDialog";
import {adminIsLoggedin} from "./service/loginService";
import {LoginDialog} from "./components/dialog/LoginDialog";
import {Switcher} from "./components/others/Switcher";
import {doUntil, Runner, UISize} from "./uiTypes/utils";
import './main_style/noSources.css'

function App() {

    const fgColorBody = '#FDF4B9FF';

    const [error, setError] = useState<string | undefined>(undefined);
    const [summary, setSummary] = useState<DomainSummary[]>([]);
    const [summaryDomain, setSummaryDomain] = useState<DepartmentSummary[]>([]);
    const [histogramResult, setHistogramResult] = useState<SourceTestResponseDto | undefined>(undefined);
    const [title, setTitle] = useState<string>('DOMAINS');
    const [summaryDomainDepartment, setSummaryDomainDepartment] = useState<DepartmentProcessSummary[]>([]);
    const [activePiePropsFail, setActivePiePropsFail] = useState<PieProps | undefined>(undefined);
    const [activePiePropsProc, setActivePiePropsProc] = useState<PieProps | undefined>(undefined);
    const [activePiePropsFin, setActivePiePropsFin] = useState<PieProps | undefined>(undefined);
    const [dialogIsVisible, setDialogIsVisible] = useState<boolean>(false);
    const [isAdminLoggedIn, setIsAdminLoggedIn] = useState<boolean>(false);
    const [loginDialogIsVisible, setLoginDialogIsVisible] = useState<boolean>(false);
    const [buttonText, setButtonText] = useState<string>('');
    const [selectedProcess, setSelectedProcess] = useState<DepartmentProcessSummary | undefined>(undefined)
    const [flipFailures, setFlipFailures] = useState<boolean>(false)
    const [returnOnlyFailures, setReturnOnlyFailures] = useState<boolean>(false)
    const [hierarchy, setHierarchy] = useState<string>('domain.department');
    const [autoPlayer, setAutoPlayer] = useState<Runner | undefined>(undefined);
    const playerRef = useRef<HTMLElement | null>(null);
    const [noSources, setNoSources] = useState<boolean>(false);

    const scrollUp = () => {
        const scrollable = document.getElementById('scrollable');
        if (scrollable) {
            scrollable.scrollTo({top: 0, left: 0, behavior: 'smooth'});
        }
    }
    const isDepartmentSummary = (thing: DomainSummary | DepartmentSummary | DepartmentProcessSummary): boolean => { //magic happens here
        return (thing as DepartmentSummary).department !== undefined && (thing as DepartmentProcessSummary).processName === undefined;
    }
    const isDomainSummary = (thing: DomainSummary | DepartmentSummary | DepartmentProcessSummary): boolean => { //magic happens here
        return (thing as DepartmentSummary).department === undefined;
    }

    const handleSummaries = (resp: DomainSummary[] | DepartmentSummary[] | DepartmentProcessSummary[]) => {
        let key = '';
        let level = '';
        if (resp.length > 0 && resp[0]) {
            setNoSources(false);
            const elem: DomainSummary | DepartmentSummary | DepartmentProcessSummary = resp[0]
            if (isDomainSummary(elem)) {
                key = (resp[0] as DomainSummary).domain;
                setSummary(resp);
                setTitle(hierarchy.substring(0, hierarchy.indexOf('.')).toUpperCase());
                level = 'domain';
            } else if (isDepartmentSummary(elem)) {
                key = (elem as DepartmentSummary).department;
                setSummary([]);
                setTitle(hierarchy.substring(hierarchy.indexOf('.') + 1).toUpperCase());
                setSummaryDomain(resp as DepartmentSummary[]);
                level = 'department';
            } else {
                key = (elem as DepartmentProcessSummary).processName;
                setSummary([]);
                setSummaryDomain([]);
                setSummaryDomainDepartment(resp as DepartmentProcessSummary[]);
                setTitle('PROCESSES : ' + (elem as DepartmentProcessSummary).department);
                level = 'department-process';
            }
        }

        if (resp.length > 1) {
            const pieDataFails: number[] = [];
            const pieDataFin: number[] = [];
            const pieDataProc: number[] = [];
            const pieLabels: string[] = [];
            resp.forEach(d => {
                pieDataFails.push(d.inFailState);
                pieDataFin.push(d.inFinishedState);
                pieDataProc.push(d.processing);
                if (level === 'domain') {
                    pieLabels.push(d.domain)
                } else if (level === 'department') {
                    pieLabels.push((d as DepartmentSummary).department)
                } else {
                    pieLabels.push((d as DepartmentProcessSummary).processName)
                }
            })
            const activePieFailz = {
                data: pieDataFails,
                labels: pieLabels,
                title: 'IN FAIL STATE',
                titleColor: fgColorBody,
                chartId: 'pie1'
            } as PieProps;
            const activePieFinz = {
                data: pieDataFin,
                labels: pieLabels,
                title: 'IN FINISHED STATE',
                titleColor: fgColorBody,
                chartId: 'pie2'
            } as PieProps;
            const activeProcz = {
                data: pieDataProc,
                labels: pieLabels,
                title: 'IN PROCESSING STATE',
                titleColor: fgColorBody,
                chartId: 'pie3'
            } as PieProps;
            setActivePiePropsFail(activePieFailz);
            setActivePiePropsProc(activeProcz);
            setActivePiePropsFin(activePieFinz);
        } else if (resp.length === 1) {
            setActivePiePropsProc(undefined);
            setActivePiePropsFin(undefined);
            const pieOverall: number[] = [];
            const pieLabels: string[] = [];
            pieOverall.push(resp[0].processing)
            pieOverall.push(resp[0].inFailState)
            pieOverall.push(resp[0].inFinishedState)
            pieLabels.push('PROCESSING');
            pieLabels.push('IN FAIL STATE');
            pieLabels.push('FINISHED');
            const activePieFailz = {
                data: pieOverall,
                labels: pieLabels,
                title: key + ' states',
                titleColor: fgColorBody,
                chartId: 'pie1'
            } as PieProps;
            setActivePiePropsFail(activePieFailz);
        }
        scrollUp();
    }

    const resetStates = () => {
        checkAdminIsLoggedIn();
        setTitle('');
        setNoSources(false);
        setActivePiePropsFail(undefined);
        setActivePiePropsProc(undefined);
        setActivePiePropsFin(undefined);
        if (autoPlayer) {
            autoPlayer.doStuff = () => {
                // nuthin
            }
        } else {
            const runner: Runner = {
                doStuff: () => {
                    // nuthin for now
                },
                finished: false,
                sleepInBetween: 5000
            } as Runner;
            doUntil(runner);
            setAutoPlayer(runner);
        }
        setHistogramResult(undefined);
        setSelectedProcess(undefined);
        setSummary([]);
        setSummaryDomainDepartment([]);
        setSummaryDomain([]);
        getDomainSummary().then(resp => {
            handleSummaries(resp);
            //setLoading(false);
            if (resp.length === 0) {
                setNoSources(true);
            }
        })
        inDemoMode().then(isDemo => {
            if ('' + isDemo === 'true') { // WTF . do we HAVE to do this...?!
                setError('We are currently in demo mode. Please setup some sources!')
            } else {
                setError(undefined);
            }
        });
        resetAutoplayer();
    }

    const onSaveResources = () => {
        resetStates();
    }

    const checkAdminIsLoggedIn = () => {
        adminIsLoggedin().then(isLoggedIn => {
            if (isLoggedIn.toString() === 'false') {
                setButtonText('Login');
                setIsAdminLoggedIn(false);
            } else {
                setIsAdminLoggedIn(true);
                setButtonText('Sources');
            }
        })
    }

    const resetAutoplayer = () => {
        if (autoPlayer) {
            autoPlayer.doStuff = () => {
                getDomainSummary().then(resp => {
                    handleSummaries(resp);
                })
            };
        }
    }

    const loadHistogram = (depermantProcessSummary: DepartmentProcessSummary | undefined, flipFailuresNew?: boolean | undefined, returnOnlyFailuresNew?: boolean | undefined) => {
        if (depermantProcessSummary) {
            let flipToUse = flipFailures;
            let returnOlyFailuresToUse = returnOnlyFailures;
            if (typeof flipFailuresNew !== 'undefined') {
                setFlipFailures(flipFailuresNew);
                flipToUse = flipFailuresNew;
            }
            if (typeof returnOnlyFailuresNew !== 'undefined') {
                setReturnOnlyFailures(returnOnlyFailuresNew);
                returnOlyFailuresToUse = returnOnlyFailuresNew;
            }
            setSelectedProcess(depermantProcessSummary);
            getLatestResult(depermantProcessSummary.key, flipToUse, returnOlyFailuresToUse).then(resp => {
                setSummary([]);
                setSummaryDomain([]);
                setSummaryDomainDepartment([]);
                setHistogramResult(resp);
                setTitle('Histogram');

                if (selectedProcess) {
                    const histogramData = resp.histogram.histogramz[0];
                    selectedProcess.inFinishedState = histogramData.actuallyFinished;
                    selectedProcess.inFailState = Math.abs(histogramData.numberOfSubjectsInFailstate);
                    selectedProcess.processing = histogramData.sum - (selectedProcess.inFinishedState + selectedProcess.inFailState);
                    selectedProcess.actualProgressInPercent = histogramData.actualStepProgress;
                }
            });
        }
    }

    useEffect(() => {
        getHierarchy().then(resp => {
            setHierarchy(resp);
        });
        checkAdminIsLoggedIn();
        resetStates();
    }, [hierarchy]);

    return (
        <div id={'main'} className={'stdColors gradient root'}>
            <>
                <div id="containermain">
                    <div id="logo" className="noselect d-inline-block align-top logo">
                        <img alt='logo' draggable="false" style={{height: '170px'}} src={logo_transperent_smaller}
                             onClick={() => {
                                 resetStates();
                             }} className={'slotter'}/>
                        <span className={'hierarchy'}>Hierarchy: {hierarchy}.process</span>
                        {error ?
                            <span className={'errorBox'}>{error}</span>
                            :
                            <span className={'errorBoxEmpty'}>Live</span>
                        }
                        <span className={'configBox'}>
                    {buttonText && buttonText.length > 0 &&
                        <button onClick={() => {
                            checkAdminIsLoggedIn();
                            if (!isAdminLoggedIn) {
                                setLoginDialogIsVisible(true);
                            } else {
                                setDialogIsVisible(true);
                            }
                        }} className="orangeButton hideForMobile">{buttonText}</button>
                    }
                            <div>
                            <div id={'colCH666'}
                                 className={'noselect'}>
                                <div id={'boxC' + 669} className={'textBoxAutoplay'}>
                                    <Switcher ref={playerRef} size={UISize.MINI} defaultValue={false}
                                              title={'Autoplay'} callback={onOrOff => {
                                        if (autoPlayer) {
                                            autoPlayer.finished = !onOrOff;
                                        }
                                    }}/>
                                </div>
                            </div>
                        </div>
                </span>
                    </div>

                    {noSources &&
                        <>
                            <div id={'row1wdw'} className={'noSourcesContainer'}>
                                <div>You have no working sources - please use the plus-button to start adding sources!
                                </div>
                                <div>There are no working sources - use the plus-button to start adding sources!</div>
                                <div>You haven't added any sources that work - add them now!</div>
                                <div>Lacking a working source - there's nothing to work!</div>
                                <div>Whooop! No working sources! Whooop! No sources! Whooop! No sources!</div>
                            </div>
                        </>
                    }

                    <div id={'bggrow'} className='row styledBg'>
                        <div id={'bgcol'} className="col-md-3">
                            <div id={'titleCol'} className={'noselect titleStyleeMain'}>{title}</div>
                            <div id={'scrollable'} className="column-left">
                                <div id={'leftWrap_' + title} className={'roundz rounded-8'}>
                                    {!histogramResult && !selectedProcess && summary && summary.length > 0 &&
                                        <>
                                            {summary.map((row, index) => (
                                                <>
                                                    <div id={'colA' + index}
                                                         className={'textColumn noselect hoverme'}
                                                         onClick={() => {
                                                             getDepartmentSummary(row.domain).then(resp => {
                                                                 handleSummaries(resp);
                                                             })
                                                             if (autoPlayer) {
                                                                 autoPlayer.doStuff = () => {
                                                                     getDepartmentSummary(row.domain).then(resp => {
                                                                         handleSummaries(resp);
                                                                     })
                                                                 };
                                                             }
                                                         }}>
                                                        <div id={'boxA' + index} className={'textBox'}>
                                                            In the <span style={{
                                                            fontSize: '2em',
                                                            color: '#FAF10AFF'
                                                        }}>{row.domain}</span> {title.toLowerCase()}&nbsp;
                                                            we currently have <span
                                                            className={'processingNumber'}>{row.processing}</span> subjects
                                                            in
                                                            processing. There are <span
                                                            className={'failedNumber'}>{row.inFailState}</span> subjects
                                                            stuck in fail state and <span
                                                            className={'finishedNumber'}>{row.inFinishedState}</span> have
                                                            reached
                                                            finalization.
                                                        </div>
                                                    </div>
                                                </>
                                            ))}
                                        </>
                                    }
                                    {!histogramResult && !selectedProcess && summaryDomain && summaryDomain.length > 0 &&
                                        <>
                                            {summaryDomain.map((row, index) => (
                                                <>
                                                    <div id={'colB' + index}
                                                         className={'textColumn noselect hoverme'}
                                                         onClick={() => {
                                                             getDepartmentProcessSummary(row.domain, row.department).then(resp => {
                                                                 handleSummaries(resp);
                                                             });
                                                             if (autoPlayer) {
                                                                 autoPlayer.doStuff = () => {
                                                                     getDepartmentProcessSummary(row.domain, row.department).then(resp => {
                                                                         handleSummaries(resp);
                                                                     });
                                                                 };
                                                             }
                                                         }}>
                                                        <div id={'boxB' + index} className={'textBox'}>
                                                            In the <span style={{
                                                            fontSize: '2em',
                                                            color: '#FAF10AFF'
                                                        }}>{row.department}</span> {title.toLowerCase()}&nbsp;
                                                            we currently have <span
                                                            className={'processingNumber'}>{row.processing}</span> subjects
                                                            in
                                                            processing. There are <span
                                                            className={'failedNumber'}>{row.inFailState}</span> subjects
                                                            stuck in fail state and <span
                                                            className={'finishedNumber'}>{row.inFinishedState}</span> have
                                                            reached finalization.
                                                        </div>
                                                    </div>
                                                </>
                                            ))}
                                        </>
                                    }
                                    {!histogramResult && !selectedProcess && summaryDomainDepartment && summaryDomainDepartment.length > 0 &&
                                        <>
                                            {summaryDomainDepartment.map((row, index) => (
                                                <>
                                                    <div id={'colC' + index}
                                                         className={'textColumn noselect hoverme'}
                                                         onClick={() => {
                                                             loadHistogram(row, false, false);
                                                             if (autoPlayer) {
                                                                 autoPlayer.doStuff = () => {
                                                                     loadHistogram(row, false, false);
                                                                 };
                                                             }
                                                         }}>

                                                        <div id={'boxC' + index} className={'textBox'}>
                                                            In the <span style={{
                                                            fontSize: '2em',
                                                            color: '#FAF10AFF'
                                                        }}>{row.processName}</span> process
                                                            we currently have <span
                                                            className={'processingNumber'}>{row.processing}</span> subjects
                                                            in
                                                            processing. There are <span
                                                            className={'failedNumber'}>{row.inFailState}</span> subjects
                                                            stuck in fail state and <span
                                                            className={'finishedNumber'}>{row.inFinishedState}</span> have
                                                            reached
                                                            finalization.
                                                        </div>
                                                    </div>
                                                </>
                                            ))}
                                        </>
                                    }
                                    {histogramResult && selectedProcess &&
                                        <>
                                            <div id={'colCH1'} className={'textColumn noselect hoverme'}>
                                                <div id={'boxCH7'} className={'textBox'}>
                                                    In the <span style={{
                                                    fontSize: '2em',
                                                    color: '#FAF10AFF'
                                                }}>{histogramResult.histogram.entityNames}</span> process
                                                    we currently have <span
                                                    className={'processingNumber'}>{(histogramResult.histogram.histogramz[0].sum - histogramResult.histogram.histogramz[0].actuallyFinished - histogramResult.histogram.histogramz[0].numberOfSubjectsInFailstate)}</span> subjects
                                                    in
                                                    processing. There are <span
                                                    className={'failedNumber'}>{histogramResult.histogram.histogramz[0].numberOfSubjectsInFailstate}</span> subjects
                                                    stuck in fail state and <span
                                                    className={'finishedNumber'}>{histogramResult.histogram.histogramz[0].actuallyFinished}</span> have
                                                    reached
                                                    finalization. This means actual progress is :
                                                    <span
                                                        className={'finishedPercent'}> {histogramResult.histogram.histogramz[0].actualStepProgress}</span> %.
                                                </div>
                                            </div>
                                            <div>
                                                <div id={'colCH3'} className={'textColumn noselect hoverme'}>
                                                    <div id={'boxC' + 666} className={'textBox hideForMobile'}>
                                                        <Switcher defaultValue={flipFailures}
                                                                  title={'Invert Failures'} callback={onOrOff => {
                                                            loadHistogram(selectedProcess, onOrOff, undefined);
                                                            if (autoPlayer) {
                                                                autoPlayer.doStuff = () => {
                                                                    loadHistogram(selectedProcess, onOrOff, undefined);
                                                                };
                                                            }
                                                        }}/>
                                                    </div>
                                                </div>
                                                <div id={'colCH4'}
                                                     className={'textColumn noselect hoverme'}>
                                                    <div id={'boxC' + 666} className={'textBox hideForMobile'}>
                                                        <Switcher defaultValue={returnOnlyFailures}
                                                                  title={'Return Only Failures'}
                                                                  callback={onOrOff => {
                                                                      loadHistogram(selectedProcess, undefined, onOrOff);
                                                                      if (autoPlayer) {
                                                                          autoPlayer.doStuff = () => {
                                                                              loadHistogram(selectedProcess, undefined, onOrOff);
                                                                          };
                                                                      }
                                                                  }}/>
                                                    </div>
                                                </div>
                                            </div>
                                        </>
                                    }
                                </div>
                            </div>
                        </div>
                        {!histogramResult && (activePiePropsFail || activePiePropsFin || activePiePropsProc) &&
                            <>
                                {activePiePropsProc &&
                                    <div id={'pai1Wrapper'} className={'col-3 stdColorsPie hideForMobile'}>
                                        <Piechart chartId={activePiePropsProc.chartId}
                                                  titleColor={activePiePropsProc.titleColor}
                                                  data={activePiePropsProc.data} labels={activePiePropsProc.labels}
                                                  title={activePiePropsProc.title}/>
                                    </div>
                                }
                                {activePiePropsFail &&
                                    <div id={'pai2Wrapper'} className={'col-3 stdColorsPie hideForMobile'}>
                                        <Piechart chartId={activePiePropsFail.chartId}
                                                  titleColor={activePiePropsFail.titleColor}
                                                  data={activePiePropsFail.data} labels={activePiePropsFail.labels}
                                                  title={activePiePropsFail.title}/>
                                    </div>
                                }
                                {activePiePropsFin &&
                                    <div id={'pai3Wrapper'} className={'col-3 stdColorsPie hideForMobile'}>
                                        <Piechart chartId={activePiePropsFin.chartId}
                                                  titleColor={activePiePropsFin.titleColor}
                                                  data={activePiePropsFin.data} labels={activePiePropsFin.labels}
                                                  title={activePiePropsFin.title}/>
                                    </div>
                                }
                            </>
                        }
                        {histogramResult &&
                            <>
                                <div id={histogramResult.histogram.entityNames}
                                     className={'col-md-3 stdColorsPie stdColorsBigGraph hideForMobile'}>
                                    <HistogramGraph result={histogramResult}/>
                                </div>
                            </>
                        }
                        {dialogIsVisible &&
                            <SourceDialog id={'dialog'} onClickButton2={() => {
                                resetStates();
                                setDialogIsVisible(false);
                            }}
                                          title={'Sources'}
                                          content={'Please enter your sources'} labelButton1={'Save'}
                                          labelButton2={'Cancel'} showSources={true}
                                          onSaveSource={onSaveResources} onNoSourcesLeft={() => {
                                if (autoPlayer) {
                                    autoPlayer.doStuff = () => {}
                                }
                                playerRef.current?.click();
                            }}/>
                        }
                        {loginDialogIsVisible &&
                            <LoginDialog id={'dialogLogin'} onClickButton2={() => setLoginDialogIsVisible(false)}
                                         title={'Login Admin'}
                                         onClickButton1={checkAdminIsLoggedIn}
                                         content={'Password :'} labelButton1={'Login'} labelButton2={'Cancel'}/>
                        }
                    </div>
                    <Footer/>
                </div>
            </>
        </div>
    );
}

export default App;
