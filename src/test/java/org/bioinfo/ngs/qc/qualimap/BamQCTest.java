package org.bioinfo.ngs.qc.qualimap;

import org.bioinfo.ngs.qc.qualimap.beans.BamQCRegionReporter;
import org.bioinfo.ngs.qc.qualimap.gui.threads.SaveZipThread;
import org.bioinfo.ngs.qc.qualimap.process.BamStatsAnalysis;
import org.junit.Test;

import java.io.*;
import java.util.*;

import static org.junit.Assert.assertTrue;

public class BamQCTest {


    List<TestConfig> tests;


    public BamQCTest() throws IOException {
        tests = new ArrayList<TestConfig>();

        tests.add( new TestConfig("/home/kokonech/qualimap-tests/test001.txt") );
        tests.add( new TestConfig("/home/kokonech/qualimap-tests/test002.txt") );
        tests.add( new TestConfig("/home/kokonech/qualimap-tests/test003.txt") );
        tests.add( new TestConfig("/home/kokonech/qualimap-tests/test004.txt") );
        tests.add( new TestConfig("/home/kokonech/qualimap-tests/test005.txt") );
        tests.add( new TestConfig("/home/kokonech/qualimap-tests/test006.txt") );

    }

    @Test
    public void testStats() {


        for (TestConfig test : tests) {

            BamQCRegionReporter bamQcReporter = new BamQCRegionReporter();

            BamStatsAnalysis bamQc = new BamStatsAnalysis(test.getPathToBamFile()) ;
            bamQc.setNumberOfWindows(400);

            String pathToRegionFile = test.getPathToRegionFile();
            if (!pathToRegionFile.isEmpty()) {
                bamQc.setSelectedRegions(pathToRegionFile);
                bamQc.setComputeOutsideStats(test.getComputeOutsideStats());
            }

            try {
                bamQc.run();
                bamQcReporter.loadReportData(bamQc.getBamStats());
                bamQcReporter.computeChartsBuffers(bamQc.getBamStats(), bamQc.getLocator(),bamQc.isPairedData());
            } catch (Exception e) {
                assertTrue("Error calculating stats. " + e.getMessage(), false);
                e.printStackTrace();
                return;
            }

            Properties calculatedProps = new Properties();
            SaveZipThread.generateBamQcProperties(calculatedProps, bamQcReporter);

            /*try {
                calculatedProps.store(new FileOutputStream("/home/kokonech/file.properties"), null);
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }*/

            Properties validProps = new Properties();
            try {
                validProps.load(new BufferedInputStream(new FileInputStream(test.getPathToValiationOptions())));
            } catch (IOException e) {
                assertTrue("Error loading valid stats.", false);
                e.printStackTrace();
                return;
            }

            final List<String> keysToSkip = Arrays.asList("meanMappingQuality", "numWindows", "aPercent");

            for (String key : calculatedProps.stringPropertyNames()) {
                if (keysToSkip.contains(key)) {
                    continue;
                }
                if (validProps.containsKey(key)) {
                    String expectedValue = validProps.getProperty(key);
                    String calculatedValue = calculatedProps.getProperty(key);
                    String errorMessage = "Stats are not equal. Key: " + key +
                            "\nExpected: " + expectedValue + "\nCalculated: " + calculatedValue;
                    assertTrue(errorMessage, expectedValue.equals(calculatedValue));

                }
            }
        }
     }



}