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
import React, {useRef} from "react";
import {
    ArcElement,
    Chart,
    ChartConfiguration,
    ChartData,
    ChartOptions,
    Legend,
    PieController,
    Title,
    Tooltip
} from "chart.js";

export type PieProps = {
    data: number[] | undefined;
    labels: string[];
    title: string;
    titleColor: string;
    chartId: string;
}

const colors = [
    'rgb(255, 99, 132)',
    'rgb(54, 162, 235)',
    'rgb(182,86,19)',
    'rgb(117,255,86)',
    'rgb(244,86,255)',
    'rgb(70,110,213)',
    'rgba(231,243,50,0.5)'
];

export function Piechart(props: PieProps) {

    const chartRef = useRef<HTMLCanvasElement | null>(null);

    if (!props.data) {
        return (<span/>)
    }

    const titleText: string = props.title;
    const titleColor: string = props.titleColor;

    const plugin = {
        id: 'customCanvasBackgroundColor',
        beforeDraw: (chart: Chart, args: any, options: ChartOptions) => {
            const ctx: CanvasRenderingContext2D = chart.ctx;
            ctx.save();
            ctx.globalCompositeOperation = 'destination-over';
            ctx.fillStyle = options.color?.toString() || '#220000';
            //ctx.fillRect(0, 0, chart.width, chart.height);
            ctx.restore();
        }
    };

    let backgroundColors: string[] = [];
    let colorIndex = 0;
    for (var i = 0; i < props.data.length; i++) {
        if (colorIndex > colors.length - 1) {
            colorIndex = 0;
        } else {
            colorIndex++;
        }
        backgroundColors.push(colors[colorIndex]);
    }
    const data = {
        labels: props.labels,
        datasets: [{
            label: props.title,
            data: props.data,
            backgroundColor: backgroundColors,
            hoverOffset: 2
        }]
    } as ChartData;

    const chartsByCanvasId: Map<string, Chart> = new Map<string, Chart>();

    const destroyChartIfNecessary = (canvasId: string) => {
        if (chartsByCanvasId.has(canvasId)) {
            chartsByCanvasId.get(canvasId)?.destroy();
        }
    }

    const registerNewChart = (canvasId: string, chart: Chart) => {
        chartsByCanvasId.set(canvasId, chart);
    }

    Chart.register(
        PieController,
        ArcElement,
        Legend,
        Title,
        Tooltip
    );

    const config: ChartConfiguration = {
        data: data,
        type: 'pie',
        options: {
            plugins: {
                customCanvasBackgroundColor: {
                    color: '#22293b'
                },
                legend: {
                    labels: {
                        font: {
                            size: 12,
                        },
                        textAlign: 'right',
                        padding: 10,
                        color: 'white'
                    },
                    fullSize: true,
                    align: 'start',
                    rtl: true
                },
                tooltip: {
                    titleColor: 'white',
                    titleAlign: 'right',
                    usePointStyle: true,
                    padding: 5,
                    position: 'nearest'
                },
                title: {
                    fullSize: true,
                    text: titleText,
                    display: true,
                    color: titleColor,
                    align: 'end',
                    font: {
                        size: 20,
                        weight: 'lighter',
                        style: 'italic',
                        family: 'DejaVu, sans-serif, Sans, Helvetica, Neue',
                        lineHeight: 1
                    }
                },
            },
            animation: {
                duration: 0,
            },
        } as ChartOptions,
        plugins: [plugin]
    } as ChartConfiguration;

    const drawChart = async () => {
        if (Chart.getChart(props.chartId)) {
            Chart.getChart(props.chartId)?.destroy()
        }

        while (!chartRef.current){
            await new Promise(r => setTimeout(r, 20));
        }

        if (chartRef && chartRef.current) {
            const ctx = chartRef.current.getContext('2d');
            if (ctx) {
                try {
                    const chart = new Chart(
                        ctx,
                        config,
                    );
                    registerNewChart(props.chartId, chart);
                } catch (e) {
                    destroyChartIfNecessary(props.chartId);
                }
            }
        }
    }
    drawChart();

    return (<div className={'simpleBorder stdColors boxShadow graphPadding graphPaddingBig'}>
        <canvas ref={chartRef} width="380px" height="380px" id={props.chartId}></canvas>
    </div>)
}
