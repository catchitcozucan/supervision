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
import './LoginDialog.css';
import {login, setAdminPasswordAndLogin} from "../../service/loginService";
import {Spinner} from "../spinner/Spinner";

export interface LoginDialogProps {
    id: string;
    content: string;
    labelButton1: string;
    labelButton2: string;
    title: string;
    onClickButton1?: () => void;
    onClickButton2?: () => void;
}

export function LoginDialog(props: LoginDialogProps) {
    const dialogRef = useRef<HTMLDialogElement | null>(null);
    const [isAdminPwdOk, setsAdminPwdOk] = useState<boolean>(false);
    const [buttonText, setButtonText] = useState<string>('Set new passwd and login');
    const [pwdValue, setPwdValue] = useState<string>('');
    const [borderColor, setBorderColor] = useState<string>('red');
    const [loading, setLoading] = useState<boolean>(false)

    const saveNewAdminPasswd = (pwd: string) => {
        setLoading(true);
        setAdminPasswordAndLogin(pwd).then(res => {
            setLoading(false);
            if (res) {
                toggleModalVisibility(false);
                if (props.onClickButton1) {
                    props.onClickButton1();
                }
            } else {
                setPwdValue('BAD PASSWORD!');
            }
        });
    }

    const toggleModalVisibility = (open?: boolean) => {

        if (open) {
            dialogRef.current?.showModal();
        } else if (dialogRef.current?.hasAttribute("open")) {
            dialogRef.current?.close();
        } else {
            dialogRef.current?.showModal();
        }
       /*
        if (!dialogRef.current) {
            return;
        } else {
            if (open) {
                dialogRef.current?.showModal();
            } else if (dialogRef.current?.hasAttribute("open")) {
                dialogRef.current?.close();
            } else {
                dialogRef.current?.showModal();
            }
        }
        */
    };
    useEffect(() => {
        setLoading(true);
        toggleModalVisibility(true);
        login('admin').then(res => {
            if (res) {
                setsAdminPwdOk(false);
            } else {
                setsAdminPwdOk(true);
                setButtonText(props.labelButton1);
            }
            setLoading(false);
        })
    }, [props.labelButton1]);

    return (
        <>
            <dialog id={props.id} ref={dialogRef} draggable={false} title={props.title}
                    className={'loginDialogScreen noselect'}>
                <div id={'loginDialogTitle'} className={'loginTitleStylee loginDialogTitle'}>
                    <div className='row'>
                        <div className={'loginDialogTitleLeft col-8'}>&lt;{props.title}&gt;</div>
                        <div className={'loginDialogCancelHeader col-1'} onClick={() => {
                            props.onClickButton2 ? props.onClickButton2() : toggleModalVisibility();
                        }}>x
                        </div>
                    </div>
                </div>
                <div id={'loginDialogContent'} className={'loginDialogContent'}>
                    <>
                        {loading ?
                            <Spinner/>
                            :
                            <div className={'loginTitleStylee'}>
                                {isAdminPwdOk ? 'Provide Admin password' : 'Please enter the new Admin password'}
                                <p/>
                                <div style={{borderColor: borderColor}} className={'passwordInput'}>
                                    <input
                                        type={'password'}
                                        maxLength={38}
                                        className={'styledPasswordInput'}
                                        onChange={event => {
                                            const newText = event.target.value;
                                            setPwdValue(newText);
                                            if (newText !== 'admin' && newText.length > 4) {
                                                setBorderColor('green');
                                            } else {
                                                setBorderColor('red');
                                            }
                                        }}
                                        value={pwdValue}/>
                                </div>
                            </div>
                        }
                        <p/>
                        <div className={'animateflicker'}>{pwdValue}</div>
                    </>
                </div>
                <div id={'dialogButtons'} className={'loginDialogButtons'}>
                    <i className="fa-solid fa-bars"></i>
                    <button disabled={borderColor !== 'green'} id={'loginDialogButtonSave'}
                            className="orangeButton loginDialogButton"
                            onClick={() => {
                                setLoading(true);
                                isAdminPwdOk ? login(pwdValue).then(res => {
                                    setLoading(false);
                                    if (res) {
                                        toggleModalVisibility(false);
                                        if (props.onClickButton1) {
                                            props.onClickButton1();
                                        }
                                    } else {
                                        setPwdValue('* * * BAD PASSWORD * * *');
                                    }
                                }) : saveNewAdminPasswd(pwdValue);
                            }} style={{'marginRight': '30px'}}>{buttonText}</button>
                    <button id={'loginDialogButtonCancel'} className="orangeButton loginDialogButton" onClick={() => {
                        props.onClickButton2 ? props.onClickButton2() : toggleModalVisibility();
                    }}>{props.labelButton2}</button>
                </div>
            </dialog>
        </>
    );
}