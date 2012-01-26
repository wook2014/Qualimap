package org.bioinfo.ngs.qc.qualimap.gui.dialogs;

import net.miginfocom.swing.MigLayout;
import org.bioinfo.ngs.qc.qualimap.gui.utils.Constants;
import org.bioinfo.ngs.qc.qualimap.process.CountReadsAnalysis;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by kokonech
 * Date: 1/26/12
 * Time: 12:26 PM
 */
public class ExportGeneListDialog extends JDialog implements ActionListener {

    JTextField outputPathField;
    JTextArea genesTextArea;
    JButton okButton, cancelButton;
    JComboBox clusterNumberBox;
    JScrollPane scrollPane;
    HashMap<Integer, String[]> geneListMap;


    public ExportGeneListDialog(String exprName, String dataName) throws IOException {

        geneListMap = new HashMap<Integer, String[]>();
        loadDataFile(dataName);

        getContentPane().setLayout(new MigLayout("insets 20"));

        add(new JLabel("Select cluster:"), "");
        clusterNumberBox = new JComboBox();
        for (int geneNum : geneListMap.keySet()) {
            clusterNumberBox.addItem(geneNum);
        }
        add(clusterNumberBox, "wrap");

        add(new JLabel("Output:"), "");

        outputPathField = new JTextField(40);
        add(outputPathField, "grow");

        JButton browseOutputPathButton = new JButton("...");
        browseOutputPathButton.addActionListener(new BrowseButtonActionListener(this,
                outputPathField, "Counts file") );
        add(browseOutputPathButton, "wrap");

        add(new JLabel("Preview"), "wrap");
        genesTextArea = new JTextArea(10,40);
        genesTextArea.setEditable(false);

        scrollPane = new JScrollPane();
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        clusterNumberBox.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                int index = Integer.parseInt(clusterNumberBox.getSelectedItem().toString());
                String[] genes = geneListMap.get(index);
                genesTextArea.setText("");
                for (String gene : genes) {
                    genesTextArea.append(gene + "\n");
                }
                JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
                verticalScrollBar.setValue(verticalScrollBar.getMinimum());
            }
        });
        clusterNumberBox.setSelectedIndex(0);

        scrollPane.setViewportView(genesTextArea);
        add(scrollPane, "span, wrap");

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new MigLayout("insets 20"));

        okButton = new JButton("OK");
        okButton.setActionCommand(Constants.OK_COMMAND);
        okButton.addActionListener(this);
        buttonPanel.add(okButton);
        cancelButton = new JButton("Cancel");
        cancelButton.setActionCommand(Constants.CANCEL_COMMAND);
        cancelButton.addActionListener(this);
        buttonPanel.add(cancelButton);

        add(buttonPanel, "span, align right, wrap");

        pack();

        setResizable(false);
        setTitle("Export gene list: " + exprName);


    }


    private void loadDataFile(String fileName) throws IOException {

        BufferedReader br = new BufferedReader(new FileReader(fileName));

        String line;
        int lineCount = 0;
        int curClusterNumber = 0;

        while ( (line = br.readLine()) != null) {
            if (line.isEmpty()) {
                continue;
            }

            if (line.startsWith("START_CLUSTER=")) {
                lineCount = 1;
                String value = line.split("=")[1].trim();
                curClusterNumber = Integer.parseInt(value);
                continue;
            }

            if (lineCount == 3 && curClusterNumber != 0) {
                String[] genes = line.split("\\s");
                geneListMap.put(curClusterNumber, genes);
            }

            lineCount++;

        }



    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getActionCommand().equals(Constants.OK_COMMAND)) {
            String errMsg = validateInput();
            if (!errMsg.isEmpty()) {
                JOptionPane.showMessageDialog(this, errMsg, "Calculate counts", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                genesTextArea.write(new FileWriter(outputPathField.getText()));
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Failed writing to output file.",
                        "Calculate counts", JOptionPane.ERROR_MESSAGE);
                return;
            }


        }

        setVisible(false);



    }



    private String validateInput() {

        File outputFile = new File(outputPathField.getText());
        try {
           outputFile.createNewFile();
        } catch (IOException e) {
            return "Output file path is not valid!";
        }
        outputFile.delete();

        return "";
    }



}