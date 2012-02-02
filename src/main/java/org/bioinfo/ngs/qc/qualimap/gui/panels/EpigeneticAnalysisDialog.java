package org.bioinfo.ngs.qc.qualimap.gui.panels;

import net.miginfocom.swing.MigLayout;
import org.bioinfo.ngs.qc.qualimap.gui.dialogs.BrowseButtonActionListener;
import org.bioinfo.ngs.qc.qualimap.gui.dialogs.EditEpigeneticsInputDataDialog;
import org.bioinfo.ngs.qc.qualimap.gui.frames.HomeFrame;
import org.bioinfo.ngs.qc.qualimap.gui.threads.EpigeneticsAnalysisThread;
import org.bioinfo.ngs.qc.qualimap.gui.utils.Constants;
import org.bioinfo.ngs.qc.qualimap.gui.utils.PopupKeyListener;
import org.bioinfo.ngs.qc.qualimap.gui.utils.TabPropertiesVO;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * Created by kokonech
 * Date: 1/3/12
 * Time: 4:01 PM
 */
public class EpigeneticAnalysisDialog extends JDialog implements ActionListener{

    JTextField regionsField,  clustersField;
    JSpinner leftOffsetSpinner, rightOffsetSpinner, stepSpinner, smoothingLengthSpinner;
    JButton browseGeneSelectionButton;
    JButton startAnalysisButton, addSampleButton, removeSampleButton, editSampleButton;
    JTextField experimentName;
    JComboBox vizTypeBox;
    JLabel progressStream;
    JTable inputDataTable;
    JPanel buttonPanel, locationPanel, transcriptIdsPanel;
    JProgressBar  progressBar;
    JTextArea logArea;
    SampleDataTableModel sampleTableModel;
    HomeFrame homeFrame;

    static final String COMMAND_ADD_ITEM = "add_item";
    static final String COMMAND_REMOVE_ITEM = "delete_item";
    static final String COMMAND_EDIT_ITEM = "edit_item";
    static final String COMMAND_RUN_ANALYSIS = "run_analysis";


    static final boolean DEBUG = true;

    public String getGeneSelectionPath() {
        return regionsField.getText();
    }

    public String getLeftOffset() {
        return leftOffsetSpinner.getValue().toString();
    }

    public String getRightOffset() {
        return rightOffsetSpinner.getValue().toString();
    }

    public String getStep() {
        return stepSpinner.getValue().toString();
    }


    public List<DataItem> getSampleItems() {
        return sampleTableModel.getItems();
    }

    public String[] getClusterNumbers() {
        return clustersField.getText().trim().split(",");
    }

    public String getInputDataName() {
        return experimentName.getText();
    }

    public String getExperimentName() {
        return experimentName.getText();
    }

    public JTextArea getLogArea() {
        return logArea;
    }

    static public class DataItem {
        public String name;
        public String medipPath;
        public String inputPath;
    }

    static class SampleDataTableModel extends AbstractTableModel {

        List<DataItem> sampleDataList;
        final String[] columnNames = { "Replicate Name", "Sample BAM file", "Control BAM file"};

        public SampleDataTableModel() {
            sampleDataList = new ArrayList<DataItem>();
        }

        public void addItem(DataItem item) {
            sampleDataList.add(item);
            fireTableDataChanged();
        }

        public void replaceItem(int index, DataItem newItem) {
            sampleDataList.set(index, newItem);
            fireTableDataChanged();
        }

        public void removeItem(int index) {
            sampleDataList.remove(index);
            fireTableDataChanged();
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public int getRowCount() {
            return sampleDataList.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public Object getValueAt(int i, int j) {
            DataItem data = sampleDataList.get(i);

            if (j == 0) {
                return data.name;
            } else if (j == 1) {
                return data.medipPath;
            } else if (j == 2) {
                return  data.inputPath;
            }   else {
                return "";
            }


        }

        public DataItem getItem(int index) {
            return sampleDataList.get(index);
        }

        public List<DataItem> getItems() {
            return sampleDataList;
        }
    }


    public EpigeneticAnalysisDialog(HomeFrame homeFrame) {

        this.homeFrame = homeFrame;

        KeyListener keyListener = new PopupKeyListener(homeFrame, this, null);
        getContentPane().setLayout(new MigLayout("insets 20"));

        add(new JLabel("Experiment ID: "), "");
        experimentName = new JTextField(20);
        experimentName.setText("Experiment 1");
        add(experimentName, "wrap");

        add(new JLabel("Alignment data:"), "span 2, wrap");

        sampleTableModel = new SampleDataTableModel();
        inputDataTable = new JTable(sampleTableModel);
        JScrollPane scroller = new JScrollPane(inputDataTable);
        scroller.setPreferredSize(new Dimension(700, 100));
        add(scroller, "span, wrap");

        buttonPanel = new JPanel();
        buttonPanel.setLayout(new MigLayout("insets 0"));

        addSampleButton = new JButton("Add");
        addSampleButton.setActionCommand(COMMAND_ADD_ITEM);
        addSampleButton.addActionListener(this);
        buttonPanel.add(addSampleButton, "");
        editSampleButton = new JButton("Edit");
        editSampleButton.setActionCommand(COMMAND_EDIT_ITEM);
        editSampleButton.addActionListener(this);
        buttonPanel.add(editSampleButton, "");
        removeSampleButton = new JButton("Remove");
        removeSampleButton.setActionCommand(COMMAND_REMOVE_ITEM);
        removeSampleButton.addActionListener(this);
        buttonPanel.add(removeSampleButton, "wrap");
        add(buttonPanel, "align right, span, wrap");

        // Gene selection
        add(new JLabel("Regions of interest:"));
        regionsField = new JTextField(40);
        regionsField.setToolTipText("Path to annotation file containing regions of interest");
        add(regionsField, "grow");

        browseGeneSelectionButton = new JButton();
		browseGeneSelectionButton.setText("...");
		browseGeneSelectionButton.addKeyListener(keyListener);
        browseGeneSelectionButton.addActionListener(
                new BrowseButtonActionListener(this, regionsField,"Annotation files", "txt"));
        add(browseGeneSelectionButton, "align center, wrap");

        //add(transcriptIdsPanel, "span, wrap");


        add(new JLabel("Location"), "span 2, wrap");

        locationPanel = new JPanel();
        locationPanel.setLayout(new MigLayout("insets 5"));

        locationPanel.add(new JLabel("Left offset (bp):"));
        leftOffsetSpinner = new JSpinner(new SpinnerNumberModel(2000, 1,1000000,1));
        locationPanel.add(leftOffsetSpinner, "");
        locationPanel.add(new JLabel("Right offset (bp):"));
        rightOffsetSpinner = new JSpinner(new SpinnerNumberModel(500, 1,10000000,1));
        locationPanel.add(rightOffsetSpinner, "");
        locationPanel.add(new JLabel("Bin size (bp):"));
        stepSpinner = new JSpinner(new SpinnerNumberModel(100, 1,10000,1));
        locationPanel.add(stepSpinner, "wrap");
        add(locationPanel, "span, wrap");

        add(new JLabel("Number of clusters:"));
        clustersField = new JTextField((20));
        clustersField.setText("10,15,20,25,30");
        add(clustersField, "wrap");

        add(new JLabel("Fragment length (bp):"));
        smoothingLengthSpinner = new JSpinner(new SpinnerNumberModel(300, 100,500,1));
        add(smoothingLengthSpinner, "wrap");

        String[] vizTypes = { "Heatmap", "Line" };
        vizTypeBox = new JComboBox(vizTypes);
        add(new JLabel("Visualization type:"), "");
        add(vizTypeBox, "wrap");

        add(new JLabel("Log"), "wrap");
        logArea = new JTextArea(5,40);
        logArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setViewportView(logArea);
        add(scrollPane, "span, grow, wrap 30px");

        UIManager.put("ProgressBar.selectionBackground", Color.black);
        UIManager.put("ProgressBar.selectionForeground", Color.black);

        progressStream = new JLabel("Status");
        add(progressStream, "align center");
        progressBar = new JProgressBar(0, 100);
        progressBar.setVisible(true);
        progressBar.setStringPainted(true);
        progressBar.setBorderPainted(true);
        progressBar.setForeground(new Color(244, 200, 120));
        add(progressBar, "grow, wrap 30px");

        startAnalysisButton = new JButton();
        startAnalysisButton.setText(">>> Run Analysis");
        startAnalysisButton.setActionCommand(COMMAND_RUN_ANALYSIS);
        startAnalysisButton.addActionListener(this);
        startAnalysisButton.addKeyListener(keyListener);
        add(startAnalysisButton, "align right, span, wrap");


        pack();

        setTitle("Epigenomics");
        setResizable(false);

        if (DEBUG) {


            regionsField.setText("/home/kokonech/sample_data/clustering_sample/CpGIslandsByTakai.wihtNames.short.bed");

            experimentName.setText("24h-i");

            // add some preliminary data
            DataItem item1 = new DataItem();
            item1.name = "24h-i_1";
            item1.medipPath = "/home/kokonech/qualimapEpi/src/medip/mapping/24h-i-medip_1.uniq.sorted.noDup.bam.small";
            item1.inputPath = "/home/kokonech/qualimapEpi/src/medip/mapping/24h-i-input.uniq.sorted.noDup.bam.small";
            addDataItem(item1);

            DataItem item2 = new DataItem();
            item2.name = "24h-i_2";
            item2.medipPath = "/home/kokonech/qualimapEpi/src/medip/mapping/24h-i-medip_2.uniq.sorted.noDup.bam.small";
            item2.inputPath = "/home/kokonech/qualimapEpi/src/medip/mapping/24h-i-input.uniq.sorted.noDup.bam.small";
            addDataItem(item2);

        }

        updateState();



    }

    void updateState() {

        int numRows = inputDataTable.getRowCount();
        removeSampleButton.setEnabled(numRows > 0);
        editSampleButton.setEnabled(numRows > 0);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        String actionCommand = actionEvent.getActionCommand();

        if ( actionCommand.equals(COMMAND_ADD_ITEM) ) {
            EditEpigeneticsInputDataDialog dlg = new EditEpigeneticsInputDataDialog(this);
            dlg.setModal(true);
            dlg.setLocationRelativeTo(this);
            dlg.setVisible(true);
        } else if ( actionCommand.equals(COMMAND_REMOVE_ITEM) ) {
            int index = inputDataTable.getSelectedRow();
            if (index != -1 ) {
                removeDataItem(index);
            }
        } else if ( actionCommand.equals(COMMAND_EDIT_ITEM) ) {
            int index = inputDataTable.getSelectedRow();
            if (index != -1) {
                EditEpigeneticsInputDataDialog dlg = new EditEpigeneticsInputDataDialog(this, index);
                dlg.setModal(true);
                dlg.setLocationRelativeTo(this);
                dlg.setVisible(true);
            }
        } else if ( actionCommand.equals(COMMAND_RUN_ANALYSIS) ) {

            String errMsg = validateInput();
            if (errMsg.isEmpty()) {
                TabPropertiesVO tabProperties = new TabPropertiesVO();
                tabProperties.setTypeAnalysis(Constants.TYPE_BAM_ANALYSIS_EPI);
                EpigeneticsAnalysisThread t = new EpigeneticsAnalysisThread(this, tabProperties );
                t.start();
            } else {
                JOptionPane.showMessageDialog(this, errMsg, "Validate Input", JOptionPane.ERROR_MESSAGE);
            }
        } else  {
            updateState();
        }
    }

    public void removeDataItem(int index) {
        sampleTableModel.removeItem(index);
        updateState();
    }

    public void addDataItem(DataItem item) {
        sampleTableModel.addItem(item);
        updateState();
    }

    public void replaceDataItem(int itemIndex, DataItem item) {
        sampleTableModel.replaceItem(itemIndex, item);
        updateState();
    }


    public DataItem getDataItem(int index) {
        return sampleTableModel.getItem(index);
    }

    public void setUiEnabled(boolean enabled) {

        for (Component c : getContentPane().getComponents()) {
            c.setEnabled(enabled);
        }

        for (Component c : buttonPanel.getComponents()) {
            c.setEnabled(enabled);
        }

        for (Component c : locationPanel.getComponents())  {
            c.setEnabled(enabled);
        }

        // No matter happends these guys stay enabled:
        progressBar.setEnabled(true);
        progressStream.setEnabled(true);

    }

    public HomeFrame getHomeFrame() {
        return homeFrame;
    }

    String validateInput() {

        String geneSelectionPath = getGeneSelectionPath();

        if (geneSelectionPath.isEmpty()) {
            return "Transcripts id path is not set!";
        }

        if ( !(new File(geneSelectionPath)).exists() ) {
            return "Gene selection path is not valid!";
        }

        if (experimentName.getText().isEmpty()) {
            return "Sample name is not provided!";
        }

        String[] clusterNumbers = getClusterNumbers();

        if (clusterNumbers.length == 0) {
            return "Cluster numbers are not provided!";
        } else {
             for (String clusterNumber : clusterNumbers) {
                    try {
                        Integer.parseInt(clusterNumber);
                    } catch (NumberFormatException e) {
                        return "Can not parse number of clusters: " + clusterNumber;
                    }
                }
        }

        if (sampleTableModel.getRowCount() == 0) {
            return "No MEDIP input data is provided!";
        }

        return "";

    }

    public void setProgressStatus(String message) {
        progressStream.setText(message);
    }

    public String getReadSmoothingLength() {
        return smoothingLengthSpinner.getValue().toString();
    }

    public String getVisuzliationType() {
        return vizTypeBox.getSelectedItem().toString().toLowerCase();
    }

}
