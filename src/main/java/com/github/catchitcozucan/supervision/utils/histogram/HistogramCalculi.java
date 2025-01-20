/**
 * Original work by Ola Aronsson 2020
 * Courtesy of nollettnoll AB &copy; 2012 - 2020
 * <p>
 * Licensed under the Creative Commons Attribution 4.0 International (the "License")
 * you may not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * <p>
 * https://creativecommons.org/licenses/by/4.0/
 * <p>
 * The software is provided “as is”, without warranty of any kind, express or
 * implied, including but not limited to the warranties of merchantability,
 * fitness for a particular purpose and noninfringement. In no event shall the
 * authors or copyright holders be liable for any claim, damages or other liability,
 * whether in an action of contract, tort or otherwise, arising from, out of or
 * in connection with the software or the use or other dealings in the software.
 */
package com.github.catchitcozucan.supervision.utils.histogram;

import com.github.catchitcozucan.supervision.api.Histogram;
import com.github.catchitcozucan.supervision.api.HistogramData;
import com.github.catchitcozucan.supervision.utils.SizeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.catchitcozucan.supervision.utils.histogram.HistogramCalculi.FailStrategy.*;

public class HistogramCalculi {

    private static final int IS_FAIL = -1;
    private static final int IS_SUCCCESS = 1;
    private static final String COMMA = ",";
    private static final String FAIL = "fail";

    public enum FailStrategy {
        EXPLICIT_NAMING, UNEVEN_IF_FAIL, EVEN_IS_FAIL
    }

    public static HistogramState getState(long[] newData, String[] bucketNames) {
        long sum = 0;
        long actuallyFinished = 0;
        long inFailedState = 0;

        FailStrategy failStrategy = evalFailStrategy(bucketNames);
        int max = newData.length;
        for (int i = 0; i < max; i++) {
            sum = sum + newData[i];
            if (i == max - 1) {
                actuallyFinished = newData[i];
            }
            if (failOfSuccess(failStrategy, i, bucketNames) == IS_FAIL) {
                inFailedState = inFailedState + newData[i];
            }
        }

        long scoreWhenAllHaveFinished = sum * (max - 1);
        long currentScore = 0;
        long ii = 1;
        for (int i = max - 1; i > 0; i--) {
            currentScore = currentScore + (newData[i] * Math.abs(max - ii));
            ii++;
        }
        double percentFinshed = SizeUtils.percent(currentScore, scoreWhenAllHaveFinished, 4, false);
        return HistogramState.builder()
                .sum(sum)
                .actuallyFinished(actuallyFinished)
                .inFailState(inFailedState)
                .percentFinished(percentFinshed)
                .build();
    }

    public static Histogram transForm(Histogram histogram, boolean flipFailures, boolean returnOnlyFailures) {
        long[] data = histogram.getHistogramz()[0].getData();
        String[] bucketNames = histogram.getBucketNames();
        HistogramState state = getState(data, bucketNames);

        if (flipFailures || returnOnlyFailures) {
            HistogramCalculi.FailStrategy failStrategy = evalFailStrategy(bucketNames);
            AtomicInteger i = new AtomicInteger(-1);
            List<Long> newValues = new ArrayList<>();
            List<String> newBuckets = new ArrayList<>();
            Arrays.stream(data).forEach(d -> {

                int index = i.incrementAndGet();
                Optional<Long> possibleSucc = Optional.empty();
                Optional<Long> possibleFail = failOfSuccess(failStrategy, index, bucketNames) == IS_FAIL ? Optional.of(Long.valueOf(d)) : Optional.empty();
                if (!possibleFail.isPresent()) {
                    possibleSucc = Optional.of(Long.valueOf(d));
                }

                if (!returnOnlyFailures && possibleSucc.isPresent()) {
                    newValues.add(possibleSucc.get());
                    newBuckets.add(bucketNames[index]);
                }
                if (possibleFail.isPresent()) {
                    newBuckets.add(bucketNames[index]);
                    if (flipFailures) {
                        long possibleFailueres = possibleFail.get();
                        long negInverted = possibleFailueres - possibleFailueres - possibleFailueres;
                        newValues.add(negInverted);
                    } else {
                        newValues.add(possibleFail.get());
                    }
                }
            });

            String[] newBucks = newBuckets.toArray(new String[0]);
            long[] newDatz = new long[newValues.size()];
            AtomicInteger index = new AtomicInteger(-1);
            newValues.stream().forEach(v -> newDatz[index.incrementAndGet()] = v.longValue());
            StringBuilder dataStr = new StringBuilder();
            Arrays.stream(newDatz).forEach(d -> dataStr.append(d).append(COMMA));

            HistogramData datan = HistogramData.builder()
                    .sum(state.getSum())
                    .data(newDatz)
                    .label(histogram.getHistogramz()[0].getLabel())
                    .actualStepProgress(Double.valueOf(state.getPercentFinished()).toString())
                    .numberOfSubjectsInFailstate(state.getInFailState())
                    .actuallyFinished(state.getActuallyFinished())
                    .build();
            return Histogram.builder()
                    .entityNames(histogram.getEntityNames())
                    .bucketNames(newBucks)
                    .histogramz(new HistogramData[]{datan})
                    .build();
        } else {
            histogram.getHistogramz()[0].setActualStepProgress(Double.valueOf(state.getPercentFinished()).toString());
            return histogram;
        }
    }

    private static int failOfSuccess(FailStrategy failStrategy, int index, String[] bucketNames) {
        if (failStrategy.equals(EXPLICIT_NAMING) && bucketNames[index].toLowerCase().startsWith(FAIL)) {
            return IS_FAIL;
        } else if (failStrategy.equals(UNEVEN_IF_FAIL) && index % 2 != 0) { // even number of states -> uneven=fail
            return IS_FAIL;
        } else if (failStrategy.equals(EVEN_IS_FAIL) && index % 2 == 0) { // uneven number of states -> even=fail (first is init-state)
            return IS_FAIL;
        } else {
            return IS_SUCCCESS;
        }
    }

    private static FailStrategy evalFailStrategy(String[] bucketNames) {
        if (Arrays.stream(bucketNames).filter(name -> name.toLowerCase().startsWith(FAIL)).findFirst().isPresent()) {
            return EXPLICIT_NAMING;
        } else if (bucketNames.length % 2 == 0) { // even number of states -> uneven=fail
            return UNEVEN_IF_FAIL;
        } else {
            return EVEN_IS_FAIL;
        }
    }
}
