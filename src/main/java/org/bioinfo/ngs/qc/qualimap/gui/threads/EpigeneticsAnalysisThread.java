package org.bioinfo.ngs.qc.qualimap.gui.threads;

import org.bioinfo.ngs.qc.qualimap.beans.BamQCRegionReporter;
import org.bioinfo.ngs.qc.qualimap.gui.panels.EpigeneticAnalysisDialog;
import org.bioinfo.ngs.qc.qualimap.gui.utils.TabPropertiesVO;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Created by kokonech
 * Date: 1/4/12
 * Time: 3:00 PM
 */

public class EpigeneticsAnalysisThread extends Thread {

    EpigeneticAnalysisDialog settingsDialog;
    TabPropertiesVO tabProperties;

    static final String TAG_PARAMETERS = "parameters";
    static final String TAG_EXP_ID = "expId";
    static final String TAG_GENE_SELECTION = "geneSelection";
    static final String TAG_FILE = "file";
    static final String TAG_COLUMN = "column";
    static final String TAG_LOCATION = "location";
    static final String TAG_UP = "up";
    static final String TAG_DOWN = "down";
    static final String TAG_FREQ = "freq";
    static final String TAG_SAMPLES = "samples";
    static final String TAG_REPLICATE = "replicate";
    static final String TAG_MEDIPS = "medips";
    static final String TAG_NAME = "name";
    static final String TAG_INPUT = "input";
    static final String TAG_MICROARRAY = "microarray";
    static final String TAG_THRESHOLD = "threshold";
    static final String TAG_CLUSTERS = "clusters";
    static final String TAG_NUM = "num";
    static final String TAG_FRAGMENT = "fragment";
    static final String TAG_DIR_OUT ="dirOut";


    static class OutputParsingThread extends Thread {

        BufferedReader outputReader;
        EpigeneticAnalysisDialog parentDialog;


        OutputParsingThread(BufferedReader outputReader, EpigeneticAnalysisDialog parentDialog) {
            this.outputReader = outputReader;
            this.parentDialog = parentDialog;

        }

        public void run() {
            String line;
            try {
                while ((line = outputReader.readLine()) != null) {
                    System.out.println(line);
                    if (line.contains("STATUS:")) {
                        parentDialog.setProgressStatus(line.split(":")[1]);
                    }
                }

            } catch (IOException e) {
                System.err.println("Failed to parse output stream.");

            }


        }

    }

    public EpigeneticsAnalysisThread(EpigeneticAnalysisDialog settingsDialog, TabPropertiesVO tabProperties) {
        super("EpigeneticsAnalysisThread");
        this.settingsDialog = settingsDialog;
        this.tabProperties = tabProperties;

    }

    public void run()  {

        settingsDialog.setUiEnabled(false);

        StringBuilder outputDir = tabProperties.createDirectory();

        try {


            settingsDialog.setProgressStatus("Generating configuration file...");
            createConfigFile(outputDir.toString(), "config.xml");


            // Create the command to execute
            String commandString = "Rscript " + settingsDialog.getHomeFrame().getQualimapFolder()
                     + "scripts"+ File.separator + "paintLocation.r";

            commandString += " --fileConfig=" + outputDir.toString() + "config.xml";
            commandString += " --homedir=" + settingsDialog.getHomeFrame().getQualimapFolder() + "scripts";
            commandString += " --vizType=" + settingsDialog.getVisuzliationType();

            System.out.println(commandString);
            settingsDialog.setProgressStatus("Running Rscript command...");
            Process p = Runtime.getRuntime().exec(commandString);

            BufferedReader outputReader = new BufferedReader( new InputStreamReader ( p.getInputStream() ) );
            OutputParsingThread outputParser= new OutputParsingThread( outputReader, settingsDialog ) ;

            outputParser.start();
            p.waitFor();
            outputParser.join();

            settingsDialog.setProgressStatus("Loading images...");
            //if (!loadBufferedImages("/home/kokonech/result") ) {
            if (!loadBufferedImages(outputDir.toString()) ) {
                throw new RuntimeException("No images generated.");
            }



        } catch (Exception e)  {
            JOptionPane.showMessageDialog(settingsDialog, e.getMessage(),
                    "Epigenetics analysis", JOptionPane.ERROR_MESSAGE );
            e.printStackTrace();
            settingsDialog.setUiEnabled(true);
            return;
        }


        String inputFileName = settingsDialog.getInputDataName();
        settingsDialog.getHomeFrame().addNewPane(inputFileName, tabProperties);

        settingsDialog.setVisible(false);

    }


    public void createConfigFile(String outDir, String configFileName) throws FileNotFoundException, XMLStreamException {

        FileOutputStream stream = new FileOutputStream(outDir + configFileName);

        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
        XMLStreamWriter xmlWriter = outputFactory.createXMLStreamWriter(stream);

        xmlWriter.writeStartDocument();
        xmlWriter.writeCharacters("\n");

        xmlWriter.writeStartElement(TAG_PARAMETERS);
        xmlWriter.writeCharacters("\n\n\t");


        // dirOut
        xmlWriter.writeStartElement(TAG_DIR_OUT);
        xmlWriter.writeCharacters(outDir);
        xmlWriter.writeEndElement();
        xmlWriter.writeCharacters("\n\n\t");

        // expId
        xmlWriter.writeStartElement(TAG_EXP_ID);
        xmlWriter.writeCharacters("24h-promoters.DMRs");
        xmlWriter.writeEndElement();
        xmlWriter.writeCharacters("\n\n\t");


        // gene selection
        xmlWriter.writeStartElement(TAG_GENE_SELECTION);
        xmlWriter.writeCharacters("\n\t\t");
        xmlWriter.writeStartElement(TAG_FILE);
        xmlWriter.writeCharacters(settingsDialog.getGeneSelectionPath());
        xmlWriter.writeEndElement();
        xmlWriter.writeCharacters("\n\t\t");
        xmlWriter.writeStartElement(TAG_COLUMN);
        xmlWriter.writeCharacters(settingsDialog.getGeneSelectionColumn());
        xmlWriter.writeEndElement();
        xmlWriter.writeCharacters("\n\t");
        xmlWriter.writeEndElement();
        xmlWriter.writeCharacters("\n\n\t");

        //location
        xmlWriter.writeStartElement(TAG_LOCATION);
        xmlWriter.writeCharacters("\n\t\t");
        xmlWriter.writeStartElement(TAG_UP);
        xmlWriter.writeCharacters(settingsDialog.getRightOffset());
        xmlWriter.writeEndElement();
        xmlWriter.writeCharacters("\n\t\t");
        xmlWriter.writeStartElement(TAG_DOWN);
        xmlWriter.writeCharacters(settingsDialog.getLeftOffset());
        xmlWriter.writeEndElement();
        xmlWriter.writeCharacters("\n\t\t");
        xmlWriter.writeStartElement(TAG_FREQ);
        xmlWriter.writeCharacters(settingsDialog.getStep());
        xmlWriter.writeEndElement();
        xmlWriter.writeCharacters("\n\t");
        xmlWriter.writeEndElement();
        xmlWriter.writeCharacters("\n\n\t");

        // samples
        xmlWriter.writeStartElement(TAG_SAMPLES);

            xmlWriter.writeCharacters("\n\t\t");
            xmlWriter.writeStartElement("sample1");
            xmlWriter.writeAttribute("name", settingsDialog.getSampleName());
            List<EpigeneticAnalysisDialog.DataItem> sampleItems = settingsDialog.getSampleItems();
            for (EpigeneticAnalysisDialog.DataItem item : sampleItems) {
                xmlWriter.writeCharacters("\n\t\t\t");
                xmlWriter.writeStartElement(TAG_REPLICATE);

                xmlWriter.writeCharacters("\n\t\t\t\t");
                xmlWriter.writeStartElement(TAG_MEDIPS);
                xmlWriter.writeCharacters(item.medipPath);
                xmlWriter.writeEndElement();
                xmlWriter.writeCharacters("\n\t\t\t\t");
                xmlWriter.writeStartElement(TAG_INPUT);
                xmlWriter.writeCharacters(item.inputPath);
                xmlWriter.writeEndElement();
                xmlWriter.writeCharacters("\n\t\t\t\t");
                xmlWriter.writeStartElement(TAG_NAME);
                xmlWriter.writeCharacters(item.name);
                xmlWriter.writeEndElement();
                xmlWriter.writeCharacters("\n\t\t\t");
                xmlWriter.writeEndElement();
            }
            xmlWriter.writeCharacters("\n\t\t");
            xmlWriter.writeEndElement();



        xmlWriter.writeCharacters("\n\t");
        xmlWriter.writeEndElement();

        // MAYBE WILL BE INCLUDED IN FUTURE VERSIONS
        /*if (settingsDialog.microArrayDataAvailable()) {
            xmlWriter.writeCharacters("\n\n\t");
            xmlWriter.writeStartElement(TAG_MICROARRAY);

            xmlWriter.writeCharacters("\n\t\t");
            xmlWriter.writeStartElement(TAG_FILE);
            xmlWriter.writeCharacters(settingsDialog.getMicroArrayDataPath());
            xmlWriter.writeEndElement();

            String[] thresholds = settingsDialog.getMicroArrayThresholds();

            for (String threshold : thresholds) {
                xmlWriter.writeCharacters("\n\t\t");
                xmlWriter.writeStartElement(TAG_THRESHOLD);
                xmlWriter.writeCharacters(threshold);
                xmlWriter.writeEndElement();
            }
            xmlWriter.writeCharacters("\n\t");
            xmlWriter.writeEndElement();
        }*/

        xmlWriter.writeCharacters("\n\n\t");
        xmlWriter.writeStartElement(TAG_CLUSTERS);

        String[] clusters = settingsDialog.getClusterNumbers();

        for (String numClusters : clusters) {
            xmlWriter.writeCharacters("\n\t\t");
            xmlWriter.writeStartElement(TAG_NUM);
            xmlWriter.writeCharacters(numClusters);
            xmlWriter.writeEndElement();
        }
        xmlWriter.writeCharacters("\n\t");
        xmlWriter.writeEndElement();

        xmlWriter.writeCharacters("\n\n\t");
        xmlWriter.writeStartElement(TAG_FRAGMENT);
        xmlWriter.writeCharacters(settingsDialog.getReadSmoothingLength());

        xmlWriter.writeEndElement();

        xmlWriter.writeCharacters("\n\n");
        xmlWriter.writeEndElement(); // parameters
        xmlWriter.writeCharacters("\n");


        xmlWriter.writeEndDocument();

    }


    boolean loadBufferedImages(String outDir) throws IOException {

        HashMap<String,BufferedImage> imageMap = new HashMap<String, BufferedImage>();
        int imageCount = 0;

        File dir = new File(outDir);
        if (!dir.exists() || !dir.isDirectory())  {
            return false;
        }
        for (File child : dir.listFiles()) {
            String fileName = child.getName();
            if (fileName.endsWith(".jpg")) {
                String imageName = fileName.subSequence(0,  fileName.length() - 4).toString();
                BufferedImage image = ImageIO.read(new FileInputStream( child ) );
                imageMap.put(imageName, image);
                imageCount++;
            }
        }

        if (imageCount == 0) {
            return false;
        }


        BamQCRegionReporter reporter = tabProperties.getReporter();
        reporter.setImageMap(imageMap);

        reporter.setInputDescription(prepareInputDescription());


        return true;

    }

    private String prepareInputDescription() {
        StringBuffer inputDesc = new StringBuffer();

        inputDesc.append("<h2 align=center>Input data & parameters</h2>\n");
        inputDesc.append("<h3>Gene selection</h3>\n");
        inputDesc.append("<br>Gene selection file: ").append(settingsDialog.getGeneSelectionPath()).append("\n");
        inputDesc.append("<br>Gene selection file column: ").append(settingsDialog.getGeneSelectionColumn()).append("\n");
        inputDesc.append("<br><b>Relative location</b>\n");
        inputDesc.append("<br>Left offset: ").append(settingsDialog.getLeftOffset()).append("\n");
        inputDesc.append("<br>Right offset: ").append(settingsDialog.getRightOffset()).append("\n");
        inputDesc.append("<br>Step offset: ").append(settingsDialog.getStep()).append("\n");

        inputDesc.append("<br>Analyzed genomic region: ");
        inputDesc.append("<h3>Samples</h3>\n");


        /*for (String sampleId : sampleIds) {
            inputDesc.append("<h5>").append(sampleId).append("</h5>\n");
            List<EpigeneticAnalysisDialog.DataItem> sampleItems = settingsDialog.getSampleItems(sampleId);
            for (EpigeneticAnalysisDialog.DataItem item : sampleItems) {
                inputDesc.append("<br>Sample name: ").append(item.name).append("\n");
                inputDesc.append("<br>Input path: ").append(item.inputPath).append("\n");
                inputDesc.append("<br>Medip path: ").append(item.medipPath).append("\n");
            }
        }*/


        return inputDesc.toString();
    }


}