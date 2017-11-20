/**
 * *************************************************************************
 * Copyright (C) 2017 CloseIT s.r.o.
 * 
 * This file is part of SAS reader plugin.
 * 
 * This file may be distributed and/or modified under the terms of the
 * GNU General Public License version 3 as published by the Free Software
 * Foundation and appearing in the file LICENSE.GPL included in the
 * packaging of this file.
 * 
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING THE
 * WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 * *************************************************************************
 */

package cz.closeit.pdi.sasreader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.core.Const;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.TransPreviewFactory;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.ui.core.dialog.EnterNumberDialog;
import org.pentaho.di.ui.core.dialog.EnterTextDialog;
import org.pentaho.di.ui.core.dialog.PreviewRowsDialog;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.trans.dialog.TransPreviewProgressDialog;
import org.pentaho.di.ui.trans.step.BaseStepDialog;

import cz.closeit.pdi.sasreader.input.SasInputField;
import cz.closeit.pdi.sasreader.parso.ParsoService;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;

public class SasReaderStepDialog extends BaseStepDialog implements StepDialogInterface {

    // If we input invalid data to table, use these default values
    private static final SasInputField.SasType DEFAULT_SAS_TYPE = SasInputField.SasType.Character;
    private static final SasInputField.KettleType DEFAULT_KETTLE_TYPE = SasInputField.KettleType.NotDefined;
    private static final int DEFAULT_LENGHT = 5;

    private SasReaderStepMeta stepMeta;
    private boolean oldChanged;

    // Browse file
    private Label wlFilename;
    private Button wbbFilename;
    private TextVar wFilename;
    private FormData fdlFilename, fdbFilename, fdFilename;

    // Prefer BigNumber check
    private Label wlBigNumber;
    private Button wbBigNumber;
    private FormData fdlBigNumber, fdbBigNumber;

    // Column table
    private Label wlColumns;
    private TableView wColumns;
    private FormData fdlColumns, fdColumns;
    
    // logo
    private Button logo;

    public SasReaderStepDialog(Shell parent, Object meta, TransMeta transMeta, String stepname) {
        super(parent, (BaseStepMeta) meta, transMeta, stepname);
        stepMeta = (SasReaderStepMeta) meta;
    }

    /**
     * Construct and open the dialog.
     *
     * @return if the dialog is canceled - null, if the dialog is confirmed - new step name
     */
    @Override
    public String open() {
        Shell parent = getParent();
        Display display = parent.getDisplay();

        shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX);
        props.setLook(shell);
        setShellImage(shell, stepMeta);

        // We have to notify Meta class if we made a change, backup the old flag
        ModifyListener modifyListener = new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent me) {
                stepMeta.setChanged();
            }
        };
        oldChanged = stepMeta.hasChanged();

        FormLayout formLayout = new FormLayout();
        formLayout.marginWidth = Const.FORM_MARGIN;
        formLayout.marginHeight = Const.FORM_MARGIN;

        shell.setLayout(formLayout);
        shell.setText(BaseMessages.getString(SasReaderStepMeta.I18N_CLASS, "Dialog.Title"));

        int middle = props.getMiddlePct();
        int margin = Const.MARGIN;

        /////////////////////
        ///// STEP NAME /////
        /////////////////////
        wlStepname = new Label(shell, SWT.RIGHT);
        wlStepname.setText(BaseMessages.getString(SasReaderStepMeta.I18N_CLASS, "Dialog.Label.StepName"));
        props.setLook(wlStepname);
        fdlStepname = new FormData();
        fdlStepname.left = new FormAttachment(0, 0);
        fdlStepname.right = new FormAttachment(middle, -margin);
        fdlStepname.top = new FormAttachment(0, margin);
        wlStepname.setLayoutData(fdlStepname);

        wStepname = new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        wStepname.setText(stepname);
        props.setLook(wStepname);
        wStepname.addModifyListener(modifyListener);
        fdStepname = new FormData();
        fdStepname.left = new FormAttachment(middle, 0);
        fdStepname.top = new FormAttachment(0, margin);
        fdStepname.right = new FormAttachment(100, 0);
        wStepname.setLayoutData(fdStepname);

        ///////////////////////
        ///// BROWSE FILE /////
        ///////////////////////
        wlFilename = new Label(shell, SWT.RIGHT);
        wlFilename.setText(BaseMessages.getString(SasReaderStepMeta.I18N_CLASS, "Dialog.Label.Browse"));
        props.setLook(wlFilename);
        fdlFilename = new FormData();
        fdlFilename.left = new FormAttachment(0, 0);
        fdlFilename.top = new FormAttachment(wStepname, margin);
        fdlFilename.right = new FormAttachment(middle, -margin);
        wlFilename.setLayoutData(fdlFilename);

        wbbFilename = new Button(shell, SWT.PUSH | SWT.CENTER);
        wbbFilename.setText(BaseMessages.getString(SasReaderStepMeta.I18N_CLASS, "Dialog.Button.Browse"));
        props.setLook(wbbFilename);
        fdbFilename = new FormData();
        fdbFilename.top = new FormAttachment(wStepname, margin);
        fdbFilename.right = new FormAttachment(100, 0);
        wbbFilename.setLayoutData(fdbFilename);

        wFilename = new TextVar(transMeta, shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(wFilename);
        wFilename.addModifyListener(modifyListener);
        fdFilename = new FormData();
        fdFilename.left = new FormAttachment(middle, 0);
        fdFilename.top = new FormAttachment(wStepname, margin);
        fdFilename.right = new FormAttachment(wbbFilename, -margin);
        wFilename.setLayoutData(fdFilename);

        wbbFilename.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                FileDialog dialog = new FileDialog(shell, SWT.OPEN);
                dialog.setFilterExtensions(new String[]{"*.sas7bdat;*.SAS7BDAT", "*"});
                if (wFilename.getText() != null) {
                    String fname = transMeta.environmentSubstitute(wFilename.getText());
                    dialog.setFileName(fname);
                }

                dialog.setFilterNames(new String[]{BaseMessages.getString(SasReaderStepMeta.I18N_CLASS, "Dialog.FileType.Sas"), BaseMessages.getString(SasReaderStepMeta.I18N_CLASS, "Dialog.FileType.All")});

                if (dialog.open() != null) {
                    String str = dialog.getFilterPath() + System.getProperty("file.separator") + dialog.getFileName();
                    wFilename.setText(str);
                    stepMeta.setFileName(str);
                }
            }
        });

        ////////////////////////////
        ///// PREFER BIGNUMBER /////
        ////////////////////////////
        wlBigNumber = new Label(shell, SWT.RIGHT);
        wlBigNumber.setText(BaseMessages.getString(SasReaderStepMeta.I18N_CLASS, "Dialog.Label.BigNumber"));
        props.setLook(wlBigNumber);
        fdlBigNumber = new FormData();
        fdlBigNumber.left = new FormAttachment(0, 0);
        fdlBigNumber.top = new FormAttachment(wFilename, margin);
        fdlBigNumber.right = new FormAttachment(middle, -margin);
        wlBigNumber.setLayoutData(fdlBigNumber);

        wbBigNumber = new Button(shell, SWT.CHECK | SWT.LEFT | SWT.BORDER);
        wbBigNumber.setToolTipText(BaseMessages.getString(SasReaderStepMeta.I18N_CLASS, "Dialog.Tooltip.PreferBigNumber"));
        props.setLook(wbBigNumber);
        // TODO I'll just leave it here in case we want to save this checkbox to stepMeta
        /*wbBigNumber.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent se) {
                stepMeta.setChanged();
            }
            public void widgetDefaultSelected(SelectionEvent se) {}
        });*/
        fdbBigNumber = new FormData();
        fdbBigNumber.left = new FormAttachment(middle, 0);
        fdbBigNumber.top = new FormAttachment(wFilename, margin);
        fdbBigNumber.right = new FormAttachment(100, 0);
        wbBigNumber.setLayoutData(fdbBigNumber);

        ////////////////////////////
        ///// TABLE OF COLUMNS /////
        ////////////////////////////
        wlColumns = new Label(shell, SWT.NONE);
        wlColumns.setText(BaseMessages.getString(SasReaderStepMeta.I18N_CLASS, "Dialog.Label.Columns"));
        props.setLook(wlColumns);
        fdlColumns = new FormData();
        fdlColumns.left = new FormAttachment(0, 0);
        fdlColumns.top = new FormAttachment(wbBigNumber, margin);
        wlColumns.setLayoutData(fdlColumns);

        int numberOfColumns = 7;
        int numberOfRows = 1;

        ColumnInfo[] ci = new ColumnInfo[numberOfColumns];
        ci[0] = new ColumnInfo(BaseMessages.getString(SasReaderStepMeta.I18N_CLASS, "Dialog.Column.Id"), ColumnInfo.COLUMN_TYPE_TEXT);
        ci[0].setToolTip(BaseMessages.getString(SasReaderStepMeta.I18N_CLASS, "Dialog.Tooltip.ColumnId"));
        ci[0].setReadOnly(true);
        ci[1] = new ColumnInfo(BaseMessages.getString(SasReaderStepMeta.I18N_CLASS, "Dialog.Column.SasName"), ColumnInfo.COLUMN_TYPE_TEXT);
        ci[1].setToolTip(BaseMessages.getString(SasReaderStepMeta.I18N_CLASS, "Dialog.Tooltip.ColumnSasName"));
        ci[2] = new ColumnInfo(BaseMessages.getString(SasReaderStepMeta.I18N_CLASS, "Dialog.Column.Name"), ColumnInfo.COLUMN_TYPE_TEXT);
        ci[2].setToolTip(BaseMessages.getString(SasReaderStepMeta.I18N_CLASS, "Dialog.Tooltip.ColumnName"));
        ci[3] = new ColumnInfo(BaseMessages.getString(SasReaderStepMeta.I18N_CLASS, "Dialog.Column.SasType"), ColumnInfo.COLUMN_TYPE_CCOMBO, new String[]{"Character", "Numeric"});
        ci[3].setToolTip(BaseMessages.getString(SasReaderStepMeta.I18N_CLASS, "Dialog.Tooltip.ColumnSasType"));
        ci[4] = new ColumnInfo(BaseMessages.getString(SasReaderStepMeta.I18N_CLASS, "Dialog.Column.Length"), ColumnInfo.COLUMN_TYPE_TEXT);
        ci[4].setToolTip(BaseMessages.getString(SasReaderStepMeta.I18N_CLASS, "Dialog.Tooltip.ColumnSasLength"));
        ci[5] = new ColumnInfo(BaseMessages.getString(SasReaderStepMeta.I18N_CLASS, "Dialog.Column.KettleType"), ColumnInfo.COLUMN_TYPE_CCOMBO, new String[]{"String", "Number", "BigNumber", "Integer", "Date"});
        ci[5].setToolTip(BaseMessages.getString(SasReaderStepMeta.I18N_CLASS, "Dialog.Tooltip.ColumnKettleType"));
        ci[6] = new ColumnInfo(BaseMessages.getString(SasReaderStepMeta.I18N_CLASS, "Dialog.Column.Label"), ColumnInfo.COLUMN_TYPE_TEXT);
        ci[6].setToolTip(BaseMessages.getString(SasReaderStepMeta.I18N_CLASS, "Dialog.Tooltip.ColumnLabel"));

        wColumns = new TableView(transMeta, shell, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL, ci, numberOfRows, modifyListener, props);
        wColumns.setSortable(true);

        fdColumns = new FormData();
        fdColumns.left = new FormAttachment(0, 0);
        fdColumns.top = new FormAttachment(wlColumns, margin);
        fdColumns.right = new FormAttachment(100, 0);
        fdColumns.bottom = new FormAttachment(100, -50);
        wColumns.setLayoutData(fdColumns);

        //////////////////////////
        ///// BOTTOM BUTTONS /////
        //////////////////////////
        wOK = new Button(shell, SWT.PUSH);
        wOK.setText(BaseMessages.getString(SasReaderStepMeta.I18N_CLASS, "Dialog.Button.OK"));
        wGet = new Button(shell, SWT.PUSH);
        wGet.setText(BaseMessages.getString(SasReaderStepMeta.I18N_CLASS, "Dialog.Button.Get"));
        wGet.setToolTipText(BaseMessages.getString(SasReaderStepMeta.I18N_CLASS, "Dialog.Tooltip.GetFields"));
        wPreview = new Button(shell, SWT.PUSH);
        wPreview.setText(BaseMessages.getString(SasReaderStepMeta.I18N_CLASS, "Dialog.Button.Preview"));
        wCancel = new Button(shell, SWT.PUSH);
        wCancel.setText(BaseMessages.getString(SasReaderStepMeta.I18N_CLASS, "Dialog.Button.Cancel"));
        BaseStepDialog.positionBottomButtons(shell, new Button[]{wOK, wGet, wPreview, wCancel}, margin, null);

        //////////////////////////
        ///// LOGO /////
        //////////////////////////
        FormData fdLogo = new FormData();
        fdLogo.bottom = new FormAttachment(100, 0);
        fdLogo.right = new FormAttachment(100, 0);
        
        Image logoIm = new Image(display, this.getClass().getResourceAsStream("/cz/closeit/pdi/sasreader/images/logo.png"));
        logo = new Button(shell, SWT.PUSH);
        logo.setImage(logoIm);
        logo.setLayoutData(fdLogo);
        
        // listener for logo
        
        logo.addMouseListener(new MouseListener() {
            @Override
            public void mouseDoubleClick(MouseEvent me) {
                mouseDown(me);
            }

            @Override
            public void mouseDown(MouseEvent me) {
                org.eclipse.swt.program.Program.launch("http://www.closeit.cz");
            }

            @Override
            public void mouseUp(MouseEvent me) {
            }
        });

        lsOK = new Listener() {
            @Override
            public void handleEvent(Event event) {
                ok();
            }
        };

        lsGet = new Listener() {
            @Override
            public void handleEvent(Event event) {
                getFields();
            }
        };

        lsPreview = new Listener() {
            @Override
            public void handleEvent(Event event) {
                previewData();
            }
        };

        lsCancel = new Listener() {
            @Override
            public void handleEvent(Event event) {
                cancel();
            }
        };

        wOK.addListener(SWT.Selection, lsOK);
        wGet.addListener(SWT.Selection, lsGet);
        wPreview.addListener(SWT.Selection, lsPreview);
        wCancel.addListener(SWT.Selection, lsCancel);

        /////////////////////////////
        ///// GENERAL LISTENERS /////
        /////////////////////////////
        lsDef = new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                ok();
            }
        };
        wStepname.addSelectionListener(lsDef);

        shell.addShellListener(new ShellAdapter() {
            @Override
            public void shellClosed(ShellEvent e) {
                cancel();
            }
        });

        setSize();

        /////////////////////////
        ///// FILL AND OPEN /////
        /////////////////////////
        fillData();
        //data filling is going to invoke modifyListener so it will always look that user made a change
        //so we have to reset the state
        stepMeta.setChanged(oldChanged);
        shell.open();

        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        return stepname;
    }

    /**
     * Fill dialog with data from StepMeta object
     */
    private void fillData() {
        Device device = Display.getCurrent();
        Color gray = new Color(device, 210, 210, 210);

        wFilename.setText(stepMeta.getFileName());
        //TODO fill check prefer BigNumber

        List<SasInputField> inputFields = stepMeta.getInputFields();

        wColumns.table.removeAll();
        wColumns.table.setItemCount(inputFields.size());
        for (int i = 0; i < inputFields.size(); i++) {
            TableItem item = wColumns.table.getItem(i);

            item.setText(1, Integer.toString(inputFields.get(i).getOriginalId()));
            item.setBackground(1, gray);
            item.setText(2, inputFields.get(i).getSasName());
            item.setText(3, inputFields.get(i).getName());
            item.setText(4, inputFields.get(i).getSasType().toString());
            item.setText(5, Integer.toString(inputFields.get(i).getLength()));
            item.setText(6, inputFields.get(i).getKettleType().toString());
            item.setText(7, inputFields.get(i).getLabel());
        }
        wColumns.setRowNums();
        wColumns.optWidth(true);
    }

    /**
     * Save data from dialog to StepMeta object
     */
    private void saveData(SasReaderStepMeta stepMeta) {
        stepMeta.setFileName(wFilename.getText());
        //TODO maybe save checkbox prefer BigNumber over Number

        List<SasInputField> inputFields = new ArrayList<>();

        for (int i = 0; i < wColumns.table.getItemCount(); i++) {
            SasInputField inputField = new SasInputField();

            try {
                inputField.setOriginalId(Integer.valueOf(wColumns.table.getItem(i).getText(1)));
            } catch (NumberFormatException ex) {
                inputField.setOriginalId(-1);
            }

            inputField.setSasName(wColumns.table.getItem(i).getText(2));
            inputField.setName(wColumns.table.getItem(i).getText(3));

            try {
                inputField.setSasType(SasInputField.SasType.valueOf(wColumns.table.getItem(i).getText(4)));
            } catch (IllegalArgumentException ex) {
                inputField.setSasType(DEFAULT_SAS_TYPE);
            }

            try {
                inputField.setLength(Integer.valueOf(wColumns.table.getItem(i).getText(5)));
            } catch (NumberFormatException ex) {
                inputField.setLength(DEFAULT_LENGHT);
            }

            try {
                inputField.setKettleType(SasInputField.KettleType.valueOf(wColumns.table.getItem(i).getText(6)));
            } catch (IllegalArgumentException ex) {
                inputField.setKettleType(DEFAULT_KETTLE_TYPE);
            }

            inputField.setLabel(wColumns.table.getItem(i).getText(7));

            inputFields.add(inputField);
        }
        stepMeta.setInputFields(inputFields);
    }

    private void ok() {
        stepname = wStepname.getText();
        saveData(stepMeta);
        
        dispose();
        
        if (logo != null) {
            logo.dispose();
        }
    }

    private void getFields() {
        File sasFile = new File(stepMeta.getFileName());
        InputStream stream;

        try {
            stream = new FileInputStream(sasFile);
            ParsoService.getFieldsFromFile(stream, stepMeta, wbBigNumber.getSelection());
            fillData();
            stream.close();
        } catch (FileNotFoundException ex) {
            //TODO - better error handling
            return;
        } catch (IOException ex) {
            return;
        }
    }

    private void previewData() {
        SasReaderStepMeta stepMeta = new SasReaderStepMeta();
        saveData(stepMeta);

        TransMeta previewMeta = TransPreviewFactory.generatePreviewTransformation(transMeta, stepMeta, wStepname.getText());
        EnterNumberDialog numberDialog = new EnterNumberDialog(shell, props.getDefaultPreviewSize(), BaseMessages.getString(SasReaderStepMeta.I18N_CLASS, "Dialog.Preview.Title"), BaseMessages.getString(SasReaderStepMeta.I18N_CLASS, "Dialog.Preview.Message"));
        int previewSize = numberDialog.open();

        if (previewSize > 0) {
            TransPreviewProgressDialog progressDialog = new TransPreviewProgressDialog(shell, previewMeta, new String[]{wStepname.getText()}, new int[]{previewSize});
            progressDialog.open();
            Trans trans = progressDialog.getTrans();
            String loggingText = progressDialog.getLoggingText();

            if (!progressDialog.isCancelled()) {
                if (trans.getResult() != null && trans.getResult().getNrErrors() > 0) {
                    EnterTextDialog enterTextDialog = new EnterTextDialog(shell, BaseMessages.getString(SasReaderStepMeta.I18N_CLASS, "Dialog.Preview.Error.Title"), BaseMessages.getString(SasReaderStepMeta.I18N_CLASS, "Dialog.Preview.Error.Message"), loggingText, true);
                    enterTextDialog.setReadOnly();
                    enterTextDialog.open();
                }
            }

            PreviewRowsDialog previewRowsDialog = new PreviewRowsDialog(shell, transMeta, SWT.NONE, wStepname.getText(), progressDialog.getPreviewRowsMeta(wStepname.getText()), progressDialog.getPreviewRows(wStepname.getText()), loggingText);
            previewRowsDialog.open();
        }

    }

    private void cancel() {
        stepname = null;
        stepMeta.setChanged(oldChanged);
        dispose();
        
        if (logo != null) {
            logo.dispose();
        }
    }

}
