package org.bioinfo.ngs.qc.qualimap.gui.threads;

import java.io.File;
import java.util.HashMap;
import java.util.TimerTask;
import java.util.Timer;

import net.sf.samtools.SAMFormatException;
import org.bioinfo.commons.log.Logger;
import org.bioinfo.ngs.qc.qualimap.beans.BamQCRegionReporter;
import org.bioinfo.ngs.qc.qualimap.gui.panels.BamAnalysisDialog;
import org.bioinfo.ngs.qc.qualimap.gui.utils.Constants;
import org.bioinfo.ngs.qc.qualimap.gui.utils.TabPropertiesVO;
import org.bioinfo.ngs.qc.qualimap.process.BamStatsAnalysis;

import javax.swing.*;

/**
 * Class to manage a thread to do the Bam analysis to the input bam files and
 * calculate resulting charts
 * 
 * @author kokonech
 */
public class BamAnalysisThread extends Thread {

	/** Logger to print information */
	protected Logger logger;

	/** Variable to manage the panel with the progressbar at the init */
	private BamAnalysisDialog bamDialog;

	/** Variables that contains the tab properties loaded in the thread */
	TabPropertiesVO tabProperties;

    private static class UpdateProgressTask extends TimerTask {
        JProgressBar progressBar;
        BamStatsAnalysis bamQC;
        UpdateProgressTask(BamStatsAnalysis bamQC, JProgressBar progressBar) {
            this.bamQC = bamQC;
            this.progressBar = progressBar;
        }

        @Override
        public void run() {
            progressBar.setValue(bamQC.getProgress());
        }
    }

	public BamAnalysisThread(String str, BamAnalysisDialog bamDialog, TabPropertiesVO tabProperties) {
		super(str);
		this.bamDialog = bamDialog;
        this.tabProperties = tabProperties;
		logger = new Logger(this.getClass().getName());
	}

	/**
	 * Public method to run this thread. Its executed when an user call to
	 * method start over this thread.
	 */
	public void run() {

		// Create the outputDir directory
		StringBuilder outputDirPath = tabProperties.createDirectory();

		BamStatsAnalysis bamQC = new BamStatsAnalysis(bamDialog.getInputFile().getAbsolutePath());

		// Set the number of windows
		bamQC.setNumberOfWindows(bamDialog.getNumberOfWindows());
        bamQC.setNumberOfThreads(bamDialog.getNumThreads());
        bamQC.setNumberOfReadsInBunch(bamDialog.getBunchSize());

		// Set the region file
		if (bamDialog.getRegionFile() != null) {
			bamQC.setSelectedRegions(bamDialog.getRegionFile().getAbsolutePath());
            bamQC.setComputeOutsideStats(bamDialog.getComputeOutsideRegions());
		}

		// Put the gff variable to know if the user has added a region file only
		// if we are analyzing the exome
		tabProperties.setGffSelected(bamDialog.getRegionFile() != null);
        tabProperties.setOutsideStatsAvailable(bamDialog.getComputeOutsideRegions());

		bamQC.setComputeChromosomeStats(true);

		// reporting
		bamQC.activeReporting(outputDirPath.toString());

		Timer timer = new Timer(true);
        timer.schedule( new UpdateProgressTask(bamQC, bamDialog.getProgressBar()), 100, 1000);

        bamDialog.setUiEnabled(false);
        bamDialog.getProgressStream().setText("Running BAM file analysis...");

		try {

            bamQC.run();
	        timer.cancel();

            tabProperties.setPairedData(bamQC.isPairedData());
            tabProperties.setBamStats(bamQC.getBamStats());
            tabProperties.setGenomeLocator(bamQC.getLocator());

			bamDialog.getProgressStream().setText("End of bam qc");
            bamDialog.getProgressBar().setValue(100);
	
			// report
			bamDialog.getProgressStream().setText("Computing report...");
			BamQCRegionReporter reporter = new BamQCRegionReporter();
            prepareInputDescription(reporter,bamQC,bamDialog.getDrawChromosomeLimits());
            reporter.setChromosomeFilePath(outputDirPath  + Constants.NAME_OF_FILE_CHROMOSOMES);

            if (bamDialog.getRegionFile() != null) {
                reporter.setNamePostfix(" (inside of regions)");
            }

			// Draw the Chromosome Limits or not
			reporter.setPaintChromosomeLimits( bamDialog.getDrawChromosomeLimits() );
            if (bamDialog.compareGcContentToPrecalculated()) {
                String genomeName = bamDialog.getGenomeName();
                String path = bamDialog.getQualimapHome()  + File.separator +
                        BamStatsAnalysis.getGcContentFileMap().get(genomeName);
                reporter.setPathToGenomeGCContent(genomeName, path);
            }

			bamDialog.getProgressStream().setText("   text report...");
			reporter.loadReportData(bamQC.getBamStats());
			bamDialog.getProgressStream().setText("OK");

			bamDialog.getProgressStream().setText("   charts...");
			reporter.computeChartsBuffers(bamQC.getBamStats(), bamQC.getLocator(), bamQC.isPairedData());
		    bamDialog.getProgressStream().setText("OK");


			// Set the reporter into the created tab
			tabProperties.setReporter(reporter);

            if (bamDialog.getRegionFile() != null && bamDialog.getComputeOutsideRegions() ) {

                BamQCRegionReporter outsideReporter = new BamQCRegionReporter();

                prepareInputDescription(outsideReporter, bamQC, bamDialog.getDrawChromosomeLimits());
	            outsideReporter.setNamePostfix(" (outside of regions)");
                outsideReporter.setChromosomeFilePath( outputDirPath  + Constants.NAME_OF_FILE_CHROMOSOMES_OUTSIDE);
				// Draw the Chromosome Limits or not
				outsideReporter.setPaintChromosomeLimits(bamDialog.getDrawChromosomeLimits());

                if (bamDialog.compareGcContentToPrecalculated()) {
                    String genomeName = bamDialog.getGenomeName();
                    String path = bamDialog.getQualimapHome()  + File.separator +
                        BamStatsAnalysis.getGcContentFileMap().get(genomeName);
                    outsideReporter.setPathToGenomeGCContent(genomeName, path);
                }

				// save stats
				bamDialog.getProgressStream().setText("   outside text report...");
				outsideReporter.loadReportData(bamQC.getOutsideBamStats());
				bamDialog.getProgressStream().setText("OK");

				// save charts
				bamDialog.getProgressStream().setText("   outside charts...");
				outsideReporter.computeChartsBuffers(bamQC.getOutsideBamStats(), bamQC.getLocator(), bamQC.isPairedData());
				bamDialog.getProgressStream().setText("OK");

				// Set the reporters into the created tab
				tabProperties.setOutsideReporter(outsideReporter);
            }

			// Increment the pogress bar
			bamDialog.getProgressStream().setText("OK");
			bamDialog.getProgressBar().setValue(100);

		} catch( OutOfMemoryError e) {
            JOptionPane.showMessageDialog(bamDialog, "<html><body align=\"center\">Out of memory!" +
                    "<br>Try decreasing the size of the bunch in the Advanced Options" +
                    "<br>or changing Java virtual machine settings.</body></html>",
                    bamDialog.getTitle(), JOptionPane.ERROR_MESSAGE);
            bamDialog.setUiEnabled(true);
            return;
        } catch (SAMFormatException se) {
            System.out.print(se.getMessage());
            JOptionPane.showMessageDialog(null, "Error parsing BAM file! See log for details.",
                    bamDialog.getTitle(), JOptionPane.ERROR_MESSAGE);
            se.printStackTrace();
            bamDialog.setUiEnabled(true);
            timer.cancel();
            return;
        } catch (Exception e) {
		    JOptionPane.showMessageDialog(null, "Analysis is failed. Reason: " + e.getMessage(),
                    bamDialog.getTitle(), JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            bamDialog.setUiEnabled(true);
            timer.cancel();
            return;
		}
		bamDialog.addNewPane(tabProperties);
	}


    private static String boolToStr(boolean yes) {
        return yes ? "yes\n" : "no\n";
    }

    public static void prepareInputDescription(BamQCRegionReporter reporter, BamStatsAnalysis bamQC,
                                         boolean drawChromosomeLimits) {

        HashMap<String,String> alignParams = new HashMap<String, String>();
        alignParams.put("BAM file: ", bamQC.getBamFile());
        alignParams.put("Number of windows: ", Integer.toString(bamQC.getNumberOfWindows()));
        Boolean.toString(true);
        alignParams.put("Draw chromosome limits: ", boolToStr(drawChromosomeLimits));
        if (!bamQC.getPgProgram().isEmpty()) {
            alignParams.put("Program: ", bamQC.getPgProgram());
            alignParams.put("Command line: ", bamQC.getPgCommandString() );
        }
        reporter.addInputDataSection("Alignment", alignParams);

        if ( bamQC.selectedRegionsAvailable() ) {
            HashMap<String,String> regionParams = new HashMap<String, String>();
            regionParams.put("GFF file: ", bamQC.getGffFile());
            regionParams.put("Outside statistics: ", boolToStr(bamQC.getComputeOutsideStats()));
            reporter.addInputDataSection("GFF region", regionParams);
        }

    }


}
