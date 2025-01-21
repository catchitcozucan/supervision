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
import "chart.js/auto";
import {
    BarController,
    BarElement,
    Chart,
    ChartConfiguration,
    ChartData,
    ChartOptions,
    Filler,
    Legend,
    PointElement,
    Title,
    Tooltip
} from "chart.js";
import {SourceTestResponseDto} from "../../generated/api";
import './HistogramGraph.css'

const colors = [
    'rgb(255, 99, 132)',
    'rgb(54, 162, 235)',
    'rgb(182,86,19)',
    'rgb(117,255,86)',
    'rgb(244,86,255)',
    'rgb(70,110,213)',
    'rgba(231,243,50,0.5)'
];

export interface HistograProps {
    result: SourceTestResponseDto;
}

export function HistogramGraph(props: HistograProps) {

    const chartRef = useRef<HTMLCanvasElement | null>(null);

    if (!props.result.histogram.histogramz[0].data) {
        return (<span/>)
    }

    const bigScreen = window.screen.width > 2560;
    const titleText: string = props.result.histogram.entityNames;
    const titleColor: string = '#FDF4B9FF';
    const subTitle: string = 'Result from ' + props.result.updatedWhen + ' fetched within ' + props.result.execTime;

    const plugin = {
        id: 'customCanvasBackgroundColor',
        beforeDraw: (chart: Chart, args: any, options: ChartOptions) => {
            const ctx: CanvasRenderingContext2D = chart.ctx;
            ctx.save();
            ctx.globalCompositeOperation = 'destination-over';
            ctx.fillStyle = options.color?.toString() || '#220000';
            //ctx.fillRect(0, 0, chart.width, chart.height);
            ctx.font = 'Sans';
            ctx.restore();
        }
    };

    let backgroundColors: string[] = [];
    let colorIndex = 0;
    for (var i = 0; i < props.result.histogram.histogramz[0].data.length; i++) {
        if (colorIndex > colors.length - 1) {
            colorIndex = 0;
        } else {
            colorIndex++;
        }
        backgroundColors.push(colors[colorIndex]);
    }
    const data = {
        labels: props.result.histogram.bucketNames,
        datasets: [{
            label: props.result.histogram.entityNames,
            data: props.result.histogram.histogramz[0].data,
            backgroundColor: backgroundColors,
            hoverOffset: 0,
            borderColor: ['black'],
            borderWidth: 1
        }]
    } as ChartData;

// Helper object and functions
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
        BarElement,
        BarController,
        PointElement,
        Filler,
        Legend,
        Title,
        Tooltip
    );

    const config: ChartConfiguration = {
        data: data,
        type: 'bar',
        options: {
            scales: {
                x: {
                    ticks: {
                        color: titleColor
                    }
                },
                y: {
                    ticks: {
                        color: 'white'
                    }
                }
            },
            maintainAspectRatio: true,
            responsive: true,
            plugins: {
                customCanvasBackgroundColor: {
                    color: '#22293b'
                },
                legend: {
                    display: false
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
                    text: 'Process : ' + titleText,
                    display: true,
                    color: titleColor,
                    align: 'end',
                    font: {
                        size: 18,
                        weight: 'lighter',
                        style: 'italic',
                        family: 'DejaVu, sans-serif, Sans, Helvetica, Neue',
                        lineHeight: 1
                    }
                },
            },
            indexAxis: 'x',
            color: 'white',
            animation: {
                duration: 0,
            }
        } as ChartOptions,
        plugins: [plugin]
    } as ChartConfiguration;

    const chartId = props.result.histogram.entityNames.replace(' ', '').toLowerCase();
    const drawChart = async () => {
        if (Chart.getChart(chartId)) {
            Chart.getChart(chartId)?.destroy()
        }

        while (!chartRef.current) {
            await new Promise(r => setTimeout(r, 20));
        }

        if (chartRef && chartRef.current) {
            const ctx = chartRef.current.getContext('2d');
            if (ctx) {
                try {
                    const chart = new Chart(
                        ctx,
                        config
                    );
                    registerNewChart(chartId, chart);
                } catch (e) {
                    destroyChartIfNecessary(chartId);
                }
            }
        }
    }
    drawChart();

    return (
        <>
            {bigScreen ?
                <div className={'simpleBorder stdColors boxShadow graphPaddingBigGraph'}>
                    <canvas ref={chartRef} width="1450px" height="882px" id={chartId}></canvas>
                </div>
                :
                <div className={'simpleBorder stdColors, boxShadow graphPaddingHisto'}>
                    <canvas ref={chartRef} width="840px" height="500px" id={chartId}></canvas>
                </div>
            }
            <div className={'subtitles hideForMobile'}>{subTitle}</div>
        </>
    )
}
