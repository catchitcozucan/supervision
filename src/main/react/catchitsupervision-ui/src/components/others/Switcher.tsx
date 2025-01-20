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
import React, {useState} from "react";
import Switch from "react-switch";
import {UISize} from "../../uiTypes/utils";

export interface SwitcherProps {
    title: string,
    callback: (value: boolean) => void
    defaultValue?: boolean | undefined;
    ref?: React.RefObject<HTMLElement | null>;
    size?: UISize;
}

export function Switcher(props: SwitcherProps) {
    const [isChecked, setIsChecked] = useState<boolean>(props.defaultValue ? props.defaultValue : false);
    let handleDiameter = 32;
    let diameter = 200;
    let height = 30;
    if (props.size === UISize.SMALL) {
        handleDiameter = 16;
        diameter = 115;
        height = 20;
    } else if (props.size === UISize.MINI) {
        handleDiameter = 8;
        diameter = 85;
        height = 12;
    }

    return (
        <label>
            <div>{props.title}</div>
            <Switch boxShadow={'#000000'} offHandleColor={'#914cf8'} onHandleColor={'#914cf8'} offColor={'#1e6b88'}
                    onColor={'#08a83f'} handleDiameter={handleDiameter} height={height} width={diameter}
                    uncheckedIcon={false} checkedIcon={false}
                    onChange={e => {
                        props.callback(!isChecked);
                        setIsChecked(e);
                    }} checked={isChecked}/>
        </label>
    );
}