/*
 *    TaskManagerTabPanel.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *    @author Manuel Martín (msalvador@bournemouth.ac.uk)
 *    @modified Alberto Verdecia (averdeciac@gmail.com)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *    
 */
package moa.experimenter;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import moa.classifiers.Classifier;
import moa.core.StringUtils;
import moa.experimenter.Tasks.myMainTask;
import moa.gui.ClassOptionSelectionPanel;
import moa.options.AbstractOptionHandler;
import moa.options.ClassOption;
import moa.options.OptionHandler;
import moa.streams.ArffFileStream;
import moa.streams.generators.AgrawalGenerator;
import moa.tasks.MainTask;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import static moa.DoTask.MAX_STATUS_STRING_LENGTH;
import static moa.DoTask.progressAnimSequence;
import moa.core.Globals;
import moa.learners.ChangeDetectorLearner;
import moa.learners.Learner;
import moa.streams.InstanceStream;
import moa.streams.generators.cd.ConceptDriftGenerator;
import moa.streams.generators.cd.GradualChangeGenerator;
import org.apache.commons.io.FilenameUtils;

/**
 * Run online learning algorithms over multiple datasets and save the
 * corresponding experiment results over time: measurements of time, memory, and
 * predictive accuracy.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @modified Alberto Verdecia (averdeciac@gmail.com)
 */
public class TaskManagerTabPanel extends JPanel {

    protected MainTask currentTask = new moa.tasks.EvaluatePrequential();//LearnModel();

    protected Classifier learner = new moa.classifiers.bayes.NaiveBayes();//LearnModel();

    protected AbstractOptionHandler stream = new AgrawalGenerator();

    protected DefaultTableModel algoritmModel;

    protected DefaultTableModel streamModel;

    protected List<TaskThread> taskList = new ArrayList<>();

    protected TaskManagerTabPanel.TaskTableModel taskTableModel;

    protected JTable taskTable = new JTable();

    protected String initialString = "initial";

    protected ChangeDetectorLearner detector = new ChangeDetectorLearner();

    protected ConceptDriftGenerator detectorStream = new GradualChangeGenerator();

    DefaultListModel listModelMonitor = new DefaultListModel();

    public static final int MILLISECS_BETWEEN_REFRESH = 600;

    /**
     * Array of characters to use to animate the progress of tasks running.
     */
    public static final char[] progressAnimSequence = new char[]{'-', '\\',
        '|', '/'};
    /**
     * Maximum length of the status string that shows the progress of tasks
     * running.
     */
    public static final int MAX_STATUS_STRING_LENGTH = 79;

    public SummaryTab summary = new SummaryTab();

    public PlotTab plot = new PlotTab();

    public AnalizeTab analizeTab = new AnalizeTab();

    protected String resultsPath = "";
    private javax.swing.JButton jButtonTask;
    private javax.swing.JButton jButtonAlgorithm;
    private javax.swing.JButton jButtonCancel;
    private javax.swing.JButton jButtonDelAlgoritm;
    private javax.swing.JButton jButtonDelStream;
    private javax.swing.JButton jButtonDelete;
    private javax.swing.JButton jButtonPause;
    private javax.swing.JButton jButtonResume;
    private javax.swing.JButton jButtonRun;
    private javax.swing.JButton jButtonStream;
    private javax.swing.JButton jButtonSaveConfig;
    private javax.swing.JButton jButtonOpenConfig;
    private javax.swing.JButton jButtonReset;
    private javax.swing.JButton jButtonDir;
    private javax.swing.JPanel jPanelConfig;
    private javax.swing.JScrollPane jScrollPaneAlgorithms;
    private javax.swing.JScrollPane jScrollPaneStreams;
    private javax.swing.JScrollPane jScrollPaneTaskTable;
    private javax.swing.JTable jTableAlgorithms;
    private javax.swing.JTable jTableStreams;
    private javax.swing.JTextField jTextFieldProcess;
    private javax.swing.JTextField jTextFieldTask;
    private javax.swing.JTextField jTextFieldDir;

    /**
     * Class ProgressCellRenderer
     */
    public class ProgressCellRenderer extends JProgressBar implements
            TableCellRenderer {

        private static final long serialVersionUID = 1L;

        /**
         * ProgressCellRenderer Constructor
         */
        public ProgressCellRenderer() {
            super(SwingConstants.HORIZONTAL, 0, 10000);
            setBorderPainted(false);
            setStringPainted(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            double frac = -1.0;
            if (value instanceof Double) {
                frac = ((Double) value).doubleValue();
            }
            if (frac >= 0.0) {
                setIndeterminate(false);
                setValue((int) (frac * 10000.0));
                setString(StringUtils.doubleToString(frac * 100.0, 2, 2));
            } else {
                setValue(0);

            }
            return this;
        }

        @Override
        public void validate() {
        }

        @Override
        public void revalidate() {
        }

        @Override
        protected void firePropertyChange(String propertyName, Object oldValue,
                Object newValue) {
        }

        @Override
        public void firePropertyChange(String propertyName, boolean oldValue,
                boolean newValue) {
        }
    }

    /**
     * Class TaskTableModel
     */
    protected class TaskTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        @Override
        public String getColumnName(int col) {
            switch (col) {
                case 0:
                    return "command";
                case 1:
                    return "status";
                case 2:
                    return "time elapsed";
                case 3:
                    return "current activity";
                case 4:
                    return "% complete";
            }
            return null;
        }

        @Override
        public int getColumnCount() {
            return 5;
        }

        @Override
        public int getRowCount() {
            return TaskManagerTabPanel.this.taskList.size();
        }

        @Override
        public Object getValueAt(int row, int col) {
            TaskThread thread = TaskManagerTabPanel.this.taskList.get(row);
            switch (col) {
                case 0:
                    try {
                        return ((OptionHandler) thread.getTask()).getCLICreationString(MainTask.class);
                    } catch (Exception e) {
                    }
                case 1:
                    return thread.getCurrentStatusString();
                case 2:
                    return StringUtils.secondsToDHMSString(thread.getCPUSecondsElapsed());
                case 3:
                    return thread.getCurrentActivityString();
                case 4:
                    return thread.getCurrentActivityFracComplete();
            }
            return null;
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    }

    /**
     * TaskManagerTabPanel Constructor
     */
    public TaskManagerTabPanel() {
        initComponents();

        this.algoritmModel = (DefaultTableModel) jTableAlgorithms.getModel();
        this.streamModel = (DefaultTableModel) jTableStreams.getModel();
        this.taskTableModel = new TaskManagerTabPanel.TaskTableModel();
        this.taskTable.setModel(this.taskTableModel);
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        this.taskTable.getColumnModel().getColumn(1).setCellRenderer(
                centerRenderer);
        this.taskTable.getColumnModel().getColumn(2).setCellRenderer(
                centerRenderer);
        this.taskTable.getColumnModel().getColumn(4).setCellRenderer(new TaskManagerTabPanel.ProgressCellRenderer());

        javax.swing.Timer updateListTimer = new javax.swing.Timer(
                MILLISECS_BETWEEN_REFRESH, (ActionEvent e) -> {
                    TaskManagerTabPanel.this.taskTable.repaint();
                });
        updateListTimer.start();

    }

    private static void createAndShowGUI() {

        // Create and set up the window.
        JFrame frame = new JFrame("Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create and set up the content pane.
        JPanel panel = new TaskManagerTabPanel();
        panel.setOpaque(true); // content panes must be opaque
        frame.setContentPane(panel);

        // Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    private void initComponents() {

        jPanelConfig = new javax.swing.JPanel();
        jScrollPaneAlgorithms = new javax.swing.JScrollPane();
        jTableAlgorithms = new javax.swing.JTable();
        jScrollPaneStreams = new javax.swing.JScrollPane();
        jTableStreams = new javax.swing.JTable();
        jTextFieldTask = new javax.swing.JTextField();
        jButtonTask = new javax.swing.JButton();
        jButtonAlgorithm = new javax.swing.JButton();
        jButtonStream = new javax.swing.JButton();
        jButtonRun = new javax.swing.JButton();
        jScrollPaneTaskTable = new javax.swing.JScrollPane();
        taskTable = new javax.swing.JTable();
        jTextFieldProcess = new javax.swing.JTextField();
        jTextFieldDir = new javax.swing.JTextField();
        jButtonDelAlgoritm = new javax.swing.JButton();
        jButtonDelStream = new javax.swing.JButton();
        jButtonPause = new javax.swing.JButton();
        jButtonResume = new javax.swing.JButton();
        jButtonCancel = new javax.swing.JButton();
        jButtonDelete = new javax.swing.JButton();
        jButtonSaveConfig = new javax.swing.JButton();
        jButtonOpenConfig = new javax.swing.JButton();
        jButtonReset = new javax.swing.JButton();
        jButtonDir = new javax.swing.JButton();

        jPanelConfig.setBorder(javax.swing.BorderFactory.createTitledBorder(null,
                "Configure", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new java.awt.Font("Tahoma", 0, 12))); // NOI18N

        jScrollPaneAlgorithms.setBorder(javax.swing.BorderFactory.createTitledBorder("Algorithms"));

        jTableAlgorithms.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{},
                new String[]{
                    "Algorithm", "Algorithm ID"
                }
        ));
        jTableAlgorithms.setEditingColumn(1);
        jScrollPaneAlgorithms.setViewportView(jTableAlgorithms);

        jScrollPaneStreams.setBorder(javax.swing.BorderFactory.createTitledBorder("Streams"));

        jTableStreams.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{},
                new String[]{
                    "Stream", "Stream ID"
                }
        ));
        jScrollPaneStreams.setViewportView(jTableStreams);

        jTextFieldTask.setEditable(false);
        //jTextFieldTask.setBorder(javax.swing.BorderFactory.createTitledBorder("Task"));

        jButtonTask.setText("Add Task");
        jButtonTask.addActionListener(this::jButtonTaskActionPerformed);
        jButtonDir.setText("Browse");
        jButtonDir.addActionListener(this::jButtonDirActionPerformed);

        jButtonAlgorithm.setText("Add Algorithm");
        jButtonAlgorithm.addActionListener(this::jButtonAlgorithmActionPerformed);

        jButtonStream.setText("Add Stream");
        jButtonStream.addActionListener(this::jButtonStreamActionPerformed);

        jButtonRun.setText("Run Experiment");
        jButtonRun.setToolTipText("Run task");
        jButtonRun.addActionListener(this::jButtonRunActionPerformed);

        jButtonOpenConfig.setText("Open Experiment");
        jButtonOpenConfig.setToolTipText("Open saved configuration file");
        jButtonOpenConfig.addActionListener(this::jButtonOpenConfigActionPerformed);

        jButtonSaveConfig.setText("Save Experiment");
        jButtonSaveConfig.setToolTipText("Save Configuration to file");
        jButtonSaveConfig.addActionListener(this::jButtonSaveConfigActionPerformed);

        jButtonReset.setText("Reset to Default");
        jButtonReset.setToolTipText("Reset all");
        jButtonReset.addActionListener(this::jButtonResetActionPerformed);

        taskTable.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{},
                new String[]{}
        ));
        jScrollPaneTaskTable.setViewportView(taskTable);

        jTextFieldProcess.setText("1");
        jTextFieldProcess.setBorder(javax.swing.BorderFactory.createTitledBorder("Threads"));

        jTextFieldDir.setText("");
        //jTextFieldDir.setBorder(javax.swing.BorderFactory.createTitledBorder("Results directory"));

        jButtonDelAlgoritm.setText("Delete Algorithm");
        jButtonDelAlgoritm.addActionListener(this::jButtonDelAlgoritmActionPerformed);

        jButtonDelStream.setText("Delete Stream");
        jButtonDelStream.addActionListener(this::jButtonDelStreamActionPerformed);

        jButtonPause.setText("Pause");
        jButtonPause.addActionListener(this::jButtonPauseActionPerformed);

        jButtonResume.setText("Resume");
        jButtonResume.addActionListener(this::jButtonResumeActionPerformed);

        jButtonCancel.setText("Cancel");
        jButtonCancel.addActionListener(this::jButtonCancelActionPerformed);

        jButtonDelete.setText("Delete");
        jButtonDelete.addActionListener(this::jButtonDeleteActionPerformed);

        /*prueba*/
        JPanel jPanel1 = new JPanel();
        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Configuration"));
        JLabel jLabelDirectory = new JLabel("Result folder");
        JLabel jLabel1 = new JLabel("Task");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addContainerGap()
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(jScrollPaneAlgorithms, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                                        .addComponent(jButtonAlgorithm)
                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(jButtonDelAlgoritm)
                                                        .addGap(0, 111, Short.MAX_VALUE)))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(jScrollPaneStreams, javax.swing.GroupLayout.DEFAULT_SIZE, 311, Short.MAX_VALUE)
                                                .addGroup(jPanel1Layout.createSequentialGroup()
                                                        .addComponent(jButtonStream)
                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                        .addComponent(jButtonDelStream))))
                                .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addGap(14, 14, 14)
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                .addComponent(jLabel1)
                                                .addComponent(jLabelDirectory))
                                        .addGap(14, 14, 14)
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(jTextFieldDir)
                                                .addComponent(jTextFieldTask))
                                        .addGap(14, 14, 14)
                                        .addComponent(jButtonDir)))
                        .addGap(16, 16, 16))
                .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(113, 113, 113)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addComponent(jButtonOpenConfig)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jButtonSaveConfig)
                                        .addGap(0, 0, Short.MAX_VALUE))
                                .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addGap(0, 0, Short.MAX_VALUE)
                                        .addComponent(jButtonTask)))
                        .addGap(14, 14, 14))
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jButtonOpenConfig)
                                .addComponent(jButtonSaveConfig))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jTextFieldTask, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jButtonTask)
                                .addComponent(jLabel1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jTextFieldDir, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabelDirectory)
                                .addComponent(jButtonDir))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jScrollPaneAlgorithms, javax.swing.GroupLayout.DEFAULT_SIZE, 288, Short.MAX_VALUE)
                                .addComponent(jScrollPaneStreams, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jButtonDelStream)
                                .addComponent(jButtonDelAlgoritm)
                                .addComponent(jButtonAlgorithm)
                                .addComponent(jButtonStream)))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGap(0, 400, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap()))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGap(0, 393, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );

        JPanel panelSRB = new JPanel();
        jTextFieldProcess.setPreferredSize(new Dimension(80, 40));
        panelSRB.add(jTextFieldProcess);
        panelSRB.add(jButtonRun);
        //Configure task table panel
        jScrollPaneAlgorithms.setPreferredSize(new Dimension(461, 260));
        jScrollPaneStreams.setPreferredSize(new Dimension(461, 260));
        jScrollPaneTaskTable.setPreferredSize(new Dimension(350, 180));
        JPanel panelTaskTable = new JPanel();
        JPanel panelTaskTableBtn = new JPanel();
        panelTaskTableBtn.add(jButtonPause);
        panelTaskTableBtn.add(jButtonResume);
        panelTaskTableBtn.add(jButtonCancel);
        panelTaskTableBtn.add(jButtonDelete);
        panelTaskTableBtn.add(jButtonReset);
        panelTaskTable.setLayout(new BorderLayout());
        panelTaskTable.add(panelSRB, BorderLayout.NORTH);
        panelTaskTable.add(jScrollPaneTaskTable, BorderLayout.CENTER);
        panelTaskTable.add(panelTaskTableBtn, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        this.add(jPanel1, BorderLayout.CENTER);
        this.add(panelTaskTable, BorderLayout.SOUTH);

    }// </editor-fold>   

    private void jButtonSaveConfigActionPerformed(java.awt.event.ActionEvent evt) {
        String path = "";
        JFileChooser propDir = new JFileChooser();
        int selection = propDir.showSaveDialog(this);
        if (selection == JFileChooser.APPROVE_OPTION) {
            path = propDir.getSelectedFile().getAbsolutePath();
            SaveConfig(path);
        }
//        if (!this.jTextFieldDir.getText().equals("")) {
//            path = this.jTextFieldDir.getText() + File.separator + "experiment.properties";
//            SaveConfig(path);
//            
//        } else{
//            JOptionPane.showMessageDialog(this, "The result directory is not specified",
//                    "Error", JOptionPane.ERROR_MESSAGE);
//        }

    }

    private void jButtonOpenConfigActionPerformed(java.awt.event.ActionEvent evt) {
        String path = openDirectory(true);

        if (!path.equals("")) {
            openConfig(path);
        }
    }

    private String openDirectory(boolean flag) {
        String path = "";
        JFileChooser propDir = new JFileChooser();
        if (!flag) {
            propDir.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        }
        int selection = propDir.showOpenDialog(this);
        if (selection == JFileChooser.APPROVE_OPTION) {
            path = propDir.getSelectedFile().getAbsolutePath();
            return path;
        }
        return "";
    }

    private void jButtonResetActionPerformed(java.awt.event.ActionEvent evt) {
        this.jTextFieldTask.setText("");
        this.jTextFieldDir.setText("");
        this.jTextFieldProcess.setText("1");
        cleanTables();
    }

    private void jButtonDirActionPerformed(ActionEvent evt) {

        String path = openDirectory(false);

        if (!path.equals("")) {
            this.jTextFieldDir.setText(path);
            this.resultsPath = jTextFieldDir.getText() + File.separator + "Results";
//            File  file= new File(this.resultsPath);
//            file.mkdir();

        }
    }

    private void jButtonTaskActionPerformed(java.awt.event.ActionEvent evt) {

        String initial = TaskManagerTabPanel.this.currentTask.getCLICreationString(MainTask.class);
        if (initial.split(" ") != null) {
            String split[] = initial.split(" ");
            String temp = initial.split(" ")[0];
            if (split.length >= 3 && split[1].equals("-l") == true) {
                for (int i = 3; i < split.length; i++) {
                    temp += " " + split[i];
                }
                initial = temp;
            }

        }
        String newTaskString = ClassOptionSelectionPanel.showSelectClassDialog(TaskManagerTabPanel.this,
                "Configure task", myMainTask.class,
                initial, null);

        try {
            this.currentTask = (MainTask) ClassOption.cliStringToObject(
                    newTaskString, MainTask.class, null);
        } catch (Exception ex) {
            Logger.getLogger(TaskManagerTabPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.jTextFieldTask.setText(newTaskString);
    }

    private void jButtonAlgorithmActionPerformed(java.awt.event.ActionEvent evt) {
        String newTaskString;
        if (this.currentTask instanceof moa.tasks.EvaluateConceptDrift) {
            newTaskString = ClassOptionSelectionPanel.showSelectClassDialog(TaskManagerTabPanel.this,
                    "Configure learner", ChangeDetectorLearner.class, this.initialString, null);

            if (newTaskString.equals(this.initialString) == true) {
                return;
            }
            this.initialString = newTaskString;
            try {
                this.detector = (ChangeDetectorLearner) ClassOption.cliStringToObject(
                        newTaskString, ChangeDetectorLearner.class, null);
            } catch (Exception ex) {
                Logger.getLogger(TaskManagerTabPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            newTaskString = ClassOptionSelectionPanel.showSelectClassDialog(TaskManagerTabPanel.this,
                    "Configure learner", Learner.class, this.initialString, null);
            if (newTaskString.equals(this.initialString) == true) {
                return;
            }
            this.initialString = newTaskString;
            try {
                this.learner = (Classifier) ClassOption.cliStringToObject(
                        newTaskString, Learner.class, null);
            } catch (Exception ex) {
                Logger.getLogger(TaskManagerTabPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        for (int i = 0; i < this.algoritmModel.getRowCount(); i++) {
            if (this.algoritmModel.getValueAt(i, 0).equals(newTaskString)) {
                JOptionPane.showMessageDialog(this, "The value exist",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        this.algoritmModel.addRow(new Object[]{newTaskString, newTaskString});

    }

    private void jButtonStreamActionPerformed(java.awt.event.ActionEvent evt) {

        boolean arff = false;
        String newTaskString = "";
        String streamOption = "";
        if (this.currentTask instanceof moa.tasks.EvaluateConceptDrift) {
            newTaskString = ClassOptionSelectionPanel.showSelectClassDialog(TaskManagerTabPanel.this,
                    "Configure stream", ConceptDriftGenerator.class, this.initialString, null);

            if (newTaskString.equals(this.initialString) == true) {
                return;
            }
            this.initialString = newTaskString;
            try {
                this.detectorStream = (ConceptDriftGenerator) ClassOption.cliStringToObject(
                        newTaskString, ConceptDriftGenerator.class, null);
            } catch (Exception ex) {
                Logger.getLogger(TaskManagerTabPanel.class.getName()).log(Level.SEVERE, null, ex);
            }

        } else {
            newTaskString = ClassOptionSelectionPanel.showSelectClassDialog(TaskManagerTabPanel.this,
                    "Configure stream", InstanceStream.class, this.initialString, null);

            if (newTaskString.equals(this.initialString) == true) {
                return;
            }
            this.initialString = newTaskString;
            try {
                this.stream = (AbstractOptionHandler) ClassOption.cliStringToObject(
                        newTaskString, InstanceStream.class, null);
            } catch (Exception ex) {
                Logger.getLogger(TaskManagerTabPanel.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (this.stream instanceof ArffFileStream) {
                streamOption = FilenameUtils.getBaseName(((ArffFileStream) this.stream).arffFileOption.getFile().getName());
                arff = true;
            }
        }
        for (int i = 0; i < this.streamModel.getRowCount(); i++) {
            if (this.streamModel.getValueAt(i, 0).equals(newTaskString)) {
                JOptionPane.showMessageDialog(this, "The value exist",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        if (arff == true) {
            this.streamModel.addRow(new Object[]{newTaskString, streamOption});
        } else {
            this.streamModel.addRow(new Object[]{newTaskString, newTaskString});
        }

    }

    private void jButtonRunActionPerformed(java.awt.event.ActionEvent evt) {

        //Validations 
        if (this.jTextFieldTask.getText().equals("")) {

            JOptionPane.showMessageDialog(this, "The task is not specified",
                    "Error", JOptionPane.ERROR_MESSAGE);
        } else if (this.jTextFieldDir.getText().equals("")) {
            JOptionPane.showMessageDialog(this, "The result directory is not specified",
                    "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            if (this.jTableAlgorithms.getRowCount() != 0) {
                if (this.jTableStreams.getRowCount() != 0) {
                    List<String> stream = new ArrayList<>();
                    for (int i = 0; i < this.streamModel.getRowCount(); i++) {
                        if (this.streamModel.getValueAt(i, 0).equals("")
                                || this.streamModel.getValueAt(i, 1).equals("")) {
                            JOptionPane.showMessageDialog(this, "Fields incompleted in Table Stream",
                                    "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        if (i == 0) {
                            stream.add(this.streamModel.getValueAt(i, 1).toString());
                        } else {
                            if (stream.remove(this.streamModel.getValueAt(i, 1).toString())) {
                                stream.add(this.streamModel.getValueAt(i, 1).toString());
                                JOptionPane.showMessageDialog(this, "There are reapeted values in Table Stream",
                                        "Error", JOptionPane.ERROR_MESSAGE);
                                return;
                            } else {
                                stream.add(this.streamModel.getValueAt(i, 1).toString());
                            }
                        }

                    }
                    List<String> algorithm = new ArrayList<>();
                    for (int i = 0; i < this.algoritmModel.getRowCount(); i++) {
                        if (this.algoritmModel.getValueAt(i, 0).equals("")
                                || this.algoritmModel.getValueAt(i, 1).equals("")) {
                            JOptionPane.showMessageDialog(this, "Fields incompleted in Table Algorithm",
                                    "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        if (i == 0) {
                            algorithm.add(this.algoritmModel.getValueAt(i, 1).toString());
                        } else {
                            if (algorithm.remove(this.algoritmModel.getValueAt(i, 1).toString())) {
                                algorithm.add(this.algoritmModel.getValueAt(i, 1).toString());
                                JOptionPane.showMessageDialog(this, "There are reapeted values in Table Algorithm",
                                        "Error", JOptionPane.ERROR_MESSAGE);
                                return;
                            } else {
                                algorithm.add(this.algoritmModel.getValueAt(i, 1).toString());
                            }
                        }

                    }//End Validations
                    runTask();
                } else {
                    JOptionPane.showMessageDialog(this, "You must select at least one dataset",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "You must select at least one algorithm",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }

        }

    }

    private void jButtonDelAlgoritmActionPerformed(java.awt.event.ActionEvent evt) {
        this.algoritmModel.removeRow(this.jTableAlgorithms.getSelectedRow());
    }

    private void jButtonDelStreamActionPerformed(java.awt.event.ActionEvent evt) {
        this.streamModel.removeRow(this.jTableStreams.getSelectedRow());
    }

    private void jButtonPauseActionPerformed(java.awt.event.ActionEvent evt) {
        pauseSelectedTasks();
    }

    private void jButtonResumeActionPerformed(java.awt.event.ActionEvent evt) {
        resumeSelectedTasks();
    }

    private void jButtonCancelActionPerformed(java.awt.event.ActionEvent evt) {
        cancelSelectedTasks();
    }

    private void jButtonDeleteActionPerformed(java.awt.event.ActionEvent evt) {
        deleteSelectedTasks();
    }

    /**
     * Executes the Task
     */
    public void runTask() {
        MainTask tasks[] = new MainTask[jTableAlgorithms.getModel().getRowCount() * jTableStreams.getModel().getRowCount()];
        int taskCount = 0;

        String dir = "";

        try {
            this.currentTask = (MainTask) ClassOption.cliStringToObject(
                    this.jTextFieldTask.getText(), MainTask.class, null);
        } catch (Exception ex) {
            Logger.getLogger(TaskManagerTabPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
        MainTask auxTask = (MainTask) this.currentTask.copy();

        dir += this.resultsPath;

        File f = new File(dir);
        if (f.exists()) {
            Object[] options = {"Yes", "No"};
            String cancel = "NO";
            int resp = JOptionPane.showOptionDialog(this,
                    "The selected folder is not empty. This action may overwrite "
                    + "previous experiment results. Do you want to continue?", "Warning",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, cancel);
            if (resp == JOptionPane.OK_OPTION) {
                ReadFile.deleteDrectory(f);

            } else {
                JOptionPane.showMessageDialog(this, "Please specify another directory", "Message",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
        }
        f.mkdir();

        String algNames = "";
        String streamNames = "";
        for (int i = 0; i < jTableAlgorithms.getModel().getRowCount(); i++) {
            String alg = jTableAlgorithms.getModel().getValueAt(i, 0).toString();
            String algFile = jTableAlgorithms.getModel().getValueAt(i, 1).toString();
            algNames += algFile;
            if (i != jTableAlgorithms.getModel().getRowCount() - 1) {
                algNames += ",";
            }
            for (int j = 0; j < jTableStreams.getModel().getRowCount(); j++) {
                String stream = jTableStreams.getModel().getValueAt(j, 0).toString();
                String streamFile = jTableStreams.getModel().getValueAt(j, 1).toString();
                streamNames += streamFile.split(" ")[0];
                if (j != jTableStreams.getModel().getRowCount() - 1) {
                    streamNames += ",";
                }
                if (i == 0) {
                    String sfile = FilenameUtils.separatorsToSystem(dir + "\\\\" + streamFile);
                    f = new File(sfile);
                    f.mkdir();
                }
                String task = " -l ";
                if (alg.split(" ") != null) {
                    task += "(" + alg + ") -s (" + stream + ")" + " -d (" + dir + File.separator
                            + streamFile.split(" ")[0] + File.separator + algFile + ".txt" + ")";
                } else {
                    task += alg + " -s (" + stream + ")" + " -d (" + dir + File.separator
                            + streamFile.split(" ")[0] + File.separator + algFile + ".txt" + ")";
                }
//                String task = FilenameUtils.separatorsToSystem(" -l (" + alg + ") -s (" + stream + ") " + " -d " + "(" + dir + "\\\\"
//                        + streamFile.split(" ")[0] + "\\\\" + algFile + ".txt" + ")");
                auxTask.getOptions().setViaCLIString(task);

                try {
                    tasks[taskCount] = (MainTask) auxTask.copy();
                } catch (Exception ex) {
                    Logger.getLogger(TaskManagerTabPanel.class.getName()).log(Level.SEVERE, null, ex);
                }

                taskCount++;
            }
        }

        Buffer buffer = new Buffer(tasks);
        int proc = 1;
        if (!this.jTextFieldProcess.getText().equals("")) {
            proc = Integer.parseInt(this.jTextFieldProcess.getText());
        }
        if (proc > tasks.length) {
            proc = tasks.length;
        }
        for (int i = 0; i < proc; i++) {
            TaskThread thread = new TaskThread(buffer);
            thread.start();
            this.taskList.add(0, thread);
            this.taskTableModel.fireTableDataChanged();
            this.taskTable.setRowSelectionInterval(0, 0);

        }
        Thread obs = new Thread() {
            public void run() {
                while (true) {
                    int count = 0;
                    for (TaskThread thread : TaskManagerTabPanel.this.taskList) {
                        if (thread.isCompleted == true) {
                            count++;
                            //System.out.println(count);
                        }
                    }
                    if (count == TaskManagerTabPanel.this.taskList.size()) {
                        TaskManagerTabPanel.this.summary.readData(resultsPath);
                        TaskManagerTabPanel.this.plot.readData(resultsPath);
                        TaskManagerTabPanel.this.analizeTab.readData(resultsPath);
                        break;
                    }
                }
            }
        };
        obs.start();
    }

    public void runTaskCLI(String[] args) {
        ExperimeterCLI expCLI = new ExperimeterCLI(args);
        expCLI.proccesCMD();

        MainTask tasks[] = new MainTask[expCLI.getAlgorithms().length * expCLI.getStreams().length];
        int taskCount = 0;

        String dir = "";

        try {
            this.currentTask = (MainTask) ClassOption.cliStringToObject(
                    expCLI.getTask(), MainTask.class, null);
        } catch (Exception ex) {
            Logger.getLogger(TaskManagerTabPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
        MainTask auxTask = (MainTask) this.currentTask.copy();

        resultsPath = expCLI.getResultsFolder() + File.separator + "Results";
        dir += resultsPath;

        File f = new File(dir);
        if (f.exists()) {
            Object[] options = {"Yes", "No"};
            String cancel = "NO";
            int resp = JOptionPane.showOptionDialog(this,
                    "The selected folder is not empty. This action may overwrite "
                    + "previous experiment results. Do you want to continue?", "Warning",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, cancel);
            if (resp == JOptionPane.OK_OPTION) {
                ReadFile.deleteDrectory(f);

            } else {
                JOptionPane.showMessageDialog(this, "Please specify another directory", "Message",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
        }
        f.mkdir();

        String algNames = "";
        String streamNames = "";
        for (int i = 0; i < expCLI.getAlgorithms().length; i++) {
            String alg = expCLI.getAlgorithms()[i];
            String algFile = expCLI.getAlgorithmsID()[i];
            algNames += algFile;
            if (i != expCLI.getAlgorithms().length - 1) {
                algNames += ",";
            }
            for (int j = 0; j < expCLI.getStreams().length; j++) {
                String stream = expCLI.getStreams()[j];
                String streamFile = expCLI.getStreamsID()[j];
                streamNames += streamFile.split(" ")[0];
                if (j != expCLI.getStreams().length - 1) {
                    streamNames += ",";
                }
                if (i == 0) {
                    String sfile = FilenameUtils.separatorsToSystem(dir + "\\\\" + streamFile);
                    f = new File(sfile);
                    f.mkdir();
                }
                String task = " -l ";
                if (alg.split(" ") != null) {
                    task += "(" + alg + ") -s (" + stream + ")" + " -d (" + dir + File.separator
                            + streamFile.split(" ")[0] + File.separator + algFile + ".txt" + ")";
                } else {
                    task += alg + " -s (" + stream + ")" + " -d (" + dir + File.separator
                            + streamFile.split(" ")[0] + File.separator + algFile + ".txt" + ")";
                }
//                String task = FilenameUtils.separatorsToSystem(" -l (" + alg + ") -s (" + stream + ") " + " -d " + "(" + dir + "\\\\"
//                        + streamFile.split(" ")[0] + "\\\\" + algFile + ".txt" + ")");
                auxTask.getOptions().setViaCLIString(task);

                try {
                    tasks[taskCount] = (MainTask) auxTask.copy();
                } catch (Exception ex) {
                    Logger.getLogger(TaskManagerTabPanel.class.getName()).log(Level.SEVERE, null, ex);
                }

                taskCount++;
            }
        }

        Buffer buffer = new Buffer(tasks);
        int proc = expCLI.getThreads();

        if (proc > tasks.length) {
            proc = tasks.length;
        }
        for (int i = 0; i < proc; i++) {
            TaskThread thread = new TaskThread(buffer);
            thread.start();
            this.taskList.add(0, thread);
            this.taskTableModel.fireTableDataChanged();
            this.taskTable.setRowSelectionInterval(0, 0);

        }

        System.err.println(Globals.getWorkbenchInfoString());
        while (true) {
            int count = 0;
            int progressAnimIndex = 0;
            StringBuilder progressLine = new StringBuilder();
            progressLine.append('\r');

            for (TaskThread thread : this.taskList) {
                //System.out.println(thread.getCurrentActivityFracComplete()*100);

                if (thread.isCompleted == true) {
                    count++;
                    //System.out.println(count);
                }
                progressLine.append(StringUtils.secondsToDHMSString(thread.getCPUSecondsElapsed()));
                progressLine.append(" [");
                progressLine.append(thread.getCurrentStatusString());
                progressLine.append("] ");
                double fracComplete = thread.getCurrentActivityFracComplete();
                if (fracComplete >= 0.0) {
                    progressLine.append(StringUtils.doubleToString(
                            fracComplete * 100.0, 2, 2));
                    progressLine.append("% ");

                }
                progressLine.append(thread.getCurrentActivityString());

            }
            System.out.print(progressLine);

            try {
                Thread.sleep(1000);

            } catch (InterruptedException ignored) {
                // wake up
            }
            if (count == this.taskList.size()) {
                TaskManagerTabPanel.this.summary.readData(resultsPath);
                      // TaskManagerTabPanel.this.plot.readData(resultsPath);
                //  TaskManagerTabPanel.this.analizeTab.readData(resultsPath);
                System.out.println();
                System.out.println("Type one option to perform summaries: <summary> <plot> <Analyze> or <exit> to finish");
                Scanner sc = new Scanner(System.in);
                String option = sc.nextLine();
                while (!option.equals("exit")) {
                    switch (option) {
                        case "summary":
                            System.out.println("Measures:");
                            for (int i = 0; i < TaskManagerTabPanel.this.summary.measures.get(0).split(",").length; i++) {
                                System.out.println("[" + i + "] " + TaskManagerTabPanel.this.summary.measures.get(0).split(",")[i]);
                            }
                            System.out.println("Select Measeures: type -h for help");
                                   while(true){
                                       String arg[] = sc.nextLine().split(" ");
                                       boolean out = expCLI.summary1CMD(arg);
                                      if(expCLI.measures != null){ 
                                       String []measures = new String[expCLI.measures.length];
                                       if(measures.length == expCLI.types.length){
                                       for(int i = 0; i < measures.length; i++){
                                           measures[i] = TaskManagerTabPanel.this.summary.measures.get(0).split(",")[expCLI.measures[i]];
                                       }
                                       TaskManagerTabPanel.this.summary.summaryCMD(measures, expCLI.types);
                                       if(out == true)
                                           break;
                                       }else{
                                           System.out.println("There must be the same number of measures and types, please enter the commands again");
                                       }
                                      }
                                       
                                   }
                            break;
                    }
                    System.out.println("Type one option to perform summaries: <summary> <plot> <Analyze> or <exit> to finish");
                    option = sc.nextLine();
                }
//                       String arg = sc.nextLine();
//                       System.out.println(arg);
                break;
            }
        }

    }

    /**
     *
     * @return a task thread
     */
    public TaskThread[] getSelectedTasks() {
        int[] selectedRows = this.taskTable.getSelectedRows();
        TaskThread[] selectedTasks = new TaskThread[selectedRows.length];
        for (int i = 0; i < selectedRows.length; i++) {
            selectedTasks[i] = this.taskList.get(selectedRows[i]);
        }
        return selectedTasks;
    }

    /**
     * Pause tasks
     */
    public void pauseSelectedTasks() {
        TaskThread[] selectedTasks = getSelectedTasks();
        for (TaskThread thread : selectedTasks) {
            thread.pauseTask();
        }
    }

    /**
     * Reseme task
     */
    public void resumeSelectedTasks() {
        TaskThread[] selectedTasks = getSelectedTasks();
        for (TaskThread thread : selectedTasks) {
            thread.resumeTask();
        }
    }

    /**
     * Cancel task
     */
    public void cancelSelectedTasks() {
        TaskThread[] selectedTasks = getSelectedTasks();
        for (TaskThread thread : selectedTasks) {
            thread.cancelTask();
        }
    }

    /**
     * Deletes selected tasks
     */
    public void deleteSelectedTasks() {
        TaskThread[] selectedTasks = getSelectedTasks();
        for (TaskThread thread : selectedTasks) {
            thread.cancelTask();
            this.taskList.remove(thread);
        }
        this.taskTableModel.fireTableDataChanged();
    }

    private void SaveConfig(String path) {
        Properties prop = new Properties();
        String algShortNames = "", algCommand = "";
        String streamShortNames = "", streamCommand = "";
        if (jTableAlgorithms.getRowCount() != 0) {
            algCommand += jTableAlgorithms.getModel().getValueAt(0, 0);
            algShortNames += jTableAlgorithms.getModel().getValueAt(0, 1);
        }
        if (jTableStreams.getRowCount() != 0) {
            streamCommand += jTableStreams.getModel().getValueAt(0, 0);
            streamShortNames += jTableStreams.getModel().getValueAt(0, 1);
        }
        for (int i = 1; i < jTableAlgorithms.getRowCount(); i++) {
            algCommand += "," + jTableAlgorithms.getModel().getValueAt(i, 0);
            algShortNames += "," + jTableAlgorithms.getModel().getValueAt(i, 1);
        }
        for (int j = 1; j < jTableStreams.getRowCount(); j++) {
            streamCommand += "," + jTableStreams.getModel().getValueAt(j, 0);
            streamShortNames += "," + jTableStreams.getModel().getValueAt(j, 1);
        }
        prop.setProperty("task", jTextFieldTask.getText());

        prop.setProperty("processors", jTextFieldProcess.getText());

        prop.setProperty("algorithmCommand", algCommand);
        prop.setProperty("algorithmShortNames", algShortNames);

        prop.setProperty("streamCommand", streamCommand);
        prop.setProperty("streamShortNames", streamShortNames);
        prop.setProperty("ResultsDir", this.resultsPath);
        path += ".properties";
        FileOutputStream propertiesFile = null;
        File f = new File(path);
        if (!f.exists()) {
            f.delete();
        }
        try {
            propertiesFile = new FileOutputStream(path);
        } catch (FileNotFoundException ex) {
            JOptionPane.showMessageDialog(this, "Problems creating properties file",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
        try {
            prop.store(propertiesFile, "file");
            JOptionPane.showMessageDialog(this, "Experiments saved at " + path,
                    "", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Problems creating properties file",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }

    }

    /**
     * Opens a previously saved configuration
     *
     * @param path
     */
    public void openConfig(String path) {

        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(path));
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Problems reading the properties file",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
        // read datasets
        this.jTextFieldProcess.setText(properties.getProperty("processors"));
        this.jTextFieldTask.setText(properties.getProperty("task"));
        try {
            this.currentTask = (MainTask) ClassOption.cliStringToObject(
                    this.jTextFieldTask.getText(), MainTask.class, null);
        } catch (Exception ex) {
            Logger.getLogger(TaskManagerTabPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.jTextFieldDir.setText(properties.getProperty("ResultsDir"));
        this.resultsPath = this.jTextFieldDir.getText();
        String[] streamShortNames = properties.getProperty("streamShortNames").split(",");
        String[] streamCommand = properties.getProperty("streamCommand").split(",");
        String[] algShortNames = properties.getProperty("algorithmShortNames").split(",");
        String[] algorithmCommand = properties.getProperty("algorithmCommand").split(",");
        cleanTables();
        for (int i = 0; i < streamShortNames.length; i++) {
            this.streamModel.addRow(new Object[]{streamCommand[i], streamShortNames[i]});
        }
        for (int i = 0; i < algShortNames.length; i++) {
            this.algoritmModel.addRow(new Object[]{algorithmCommand[i], algShortNames[i]});
        }

    }

    /**
     * Clean the tables
     */
    public void cleanTables() {
        try {
            DefaultTableModel algModel = (DefaultTableModel) jTableAlgorithms.getModel();
            DefaultTableModel strModel = (DefaultTableModel) jTableStreams.getModel();
            int rows = jTableAlgorithms.getRowCount();
            int srow = jTableStreams.getRowCount();
            int trow = this.taskList.size();
            for (int i = 0; i < rows; i++) {
                algModel.removeRow(0);
            }
            for (int i = 0; i < srow; i++) {
                strModel.removeRow(0);
            }
            for (int i = 0; i < trow; i++) {
                this.taskList.remove(0);
            }
            this.taskTableModel.fireTableDataChanged();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al limpiar la tabla.");
        }
    }

    /**
     * Main method
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            javax.swing.SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    if (args.length != 0) {
                        //System.out.println("OK");
                        TaskManagerTabPanel panel = new TaskManagerTabPanel();
                        panel.runTaskCLI(args);
                        System.exit(0);

                    } else {
                        createAndShowGUI();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
