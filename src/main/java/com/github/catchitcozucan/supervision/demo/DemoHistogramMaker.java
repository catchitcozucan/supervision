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
package com.github.catchitcozucan.supervision.demo;


import com.github.catchitcozucan.supervision.api.Histogram;
import com.github.catchitcozucan.supervision.api.HistogramData;
import com.github.catchitcozucan.supervision.utils.ArrayRotator;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.github.catchitcozucan.supervision.utils.histogram.HistogramCalculi.getState;

public class DemoHistogramMaker {

    private static final Random RAND = new Random();
    //  @formatter:off
	private static final ArrayRotator<String> STATES = new ArrayRotator<>(new String[] {
            "INITIATED",
            "PREP_SUBJECT",
            "CREATE_RECORD",
            "STORE_RECORD",
            "SEND_NOTIFICATION",
            "RENDER_IMAGE",
            "CLUSTER_SYNC",
            "FETCH_DATA",
            "STORE_DATA",
            "STORED_DATA",
            "INITIATING_TRANSFER",
            "WRITE_RECORDS",
            "BULK_READ_DONE"
    }); public static final String FAILED_ = "FAILED_";
    public static final String FINISHED = "FINISHED";
	//  @formatter:on

    private Map<String, Histogram> demoCache;

    static DemoHistogramMaker INSTANCE;

    private DemoHistogramMaker() {
        demoCache = new HashMap<>();
    }

    public static synchronized Histogram generate(String requestKey, String domain, String department, String processName, boolean flipFailures, boolean returnOnlyFailures) {
        if (INSTANCE == null) {
            INSTANCE = new DemoHistogramMaker();
        }
        return INSTANCE.generateHistogram(requestKey, domain, department, processName, flipFailures, returnOnlyFailures);
    }

    private Histogram generateHistogram(String requestKey, String domain, String department, String processName, boolean flipFailures, boolean returnOnlyFailures) {
        String key = requestKey;
        Histogram histogram = demoCache.get(key);
        if (histogram != null) {
            return histogram.transForm(flipFailures, returnOnlyFailures);
        } else {
            Map<String, Long> histogramData = new LinkedHashMap<>();
            List<String> buckets = new LinkedList<>();
            Long numberOfSubjectsInFailstate = 0l;
            for (int i = 0; i < 7; i++) {
                String goodState = STATES.getRandom();
                String badState = new StringBuilder(FAILED_).append(goodState).toString();
                if (!histogramData.containsKey(goodState)) {
                    buckets.add(badState);
                    buckets.add(goodState);
                    Long fails = Long.valueOf(RAND.nextInt(1000));
                    histogramData.put(badState, fails);
                    numberOfSubjectsInFailstate = numberOfSubjectsInFailstate + fails;
                    histogramData.put(goodState, Long.valueOf(RAND.nextInt(1000)));
                }
            }
            Long finished = Long.valueOf(RAND.nextInt(1000));
            histogramData.put(FINISHED, finished);
            buckets.add(FINISHED);

            AtomicLong sum = new AtomicLong(finished);
            histogramData.values().stream().forEach(numberOfSubjects -> sum.getAndAdd(numberOfSubjects));

            int numberOfSteps = histogramData.size();
            Long allAsFinshedState = numberOfSteps * sum.get();
            Long achievedSteps = 0l;

            AtomicInteger index = new AtomicInteger(-1);
            Map<Integer, Long> valuesPerColumn = histogramData.values().stream().collect(Collectors.toMap(val -> index.incrementAndGet(), Long::longValue));
            for (int i = (numberOfSteps - 1); i > -1; i--) {
                achievedSteps = achievedSteps + valuesPerColumn.get(i);
            }
            long[] data = new long[histogramData.size()];
            int i = 0;
            for (String b : buckets) {
                if (histogramData.get(b) != null && i < data.length) {
                    data[i] = histogramData.get(b);
                    i++;
                }
            }
            double percentFinshed = getState(data, buckets.toArray(new String[0])).getPercentFinished();
            HistogramData histogramD = HistogramData.builder()
                    .sum(sum.get())
                    .actuallyFinished(finished)
                    .numberOfSubjectsInFailstate(numberOfSubjectsInFailstate)
                    .label(processName)
                    .actualStepProgress(Double.valueOf(percentFinshed).toString())
                    .data(data)
                    .build();

            Histogram histo = Histogram.builder()
                    .bucketNames(buckets.toArray(String[]::new))
                    .entityNames(processName)
                    .histogramz(new HistogramData[]{histogramD})
                    .build();
            demoCache.put(key, histo);
            return histo.transForm(flipFailures, returnOnlyFailures);
        }
    }
}
