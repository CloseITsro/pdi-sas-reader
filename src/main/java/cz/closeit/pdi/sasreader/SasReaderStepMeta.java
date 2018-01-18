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
import java.util.Map;
import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Counter;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;

import cz.closeit.pdi.sasreader.input.SasInputField;
import cz.closeit.pdi.sasreader.parso.ParsoService;

@Step(id = "CloseITSasReader",
        i18nPackageName = "cz.closeit.pdi.sasreader",
        image = "cz/closeit/pdi/sasreader/images/SASInput.png",
        name = "CloseIt.SasReader.Step.Name",
        description = "CloseIt.SasReader.Step.Description",
        categoryDescription = "CloseIt.SasReader.Step.CategoryDescription")
public class SasReaderStepMeta extends BaseStepMeta implements StepMetaInterface {

    public static final Class<?> PKG = SasReaderStepMeta.class;
    public static final String KEY_FILENAME = "filename";

    private String fileName = "";
    private List<SasInputField> inputFields = null;

    public SasReaderStepMeta() {
        super();
    }

    /**
     * Inform the Kettle how this step modifies row. Because this step is reader
     * incoming structure should be empty.
     *
     * @param inputRowMeta
     * @param name
     * @param info
     * @param nextStep
     * @param space
     * @param metaStore
     */
    @Override
    public void getFields(RowMetaInterface inputRowMeta, String name, RowMetaInterface[] info, StepMeta nextStep, VariableSpace space, Repository repository, IMetaStore metaStore) throws KettleStepException {
        for (SasInputField inputField : inputFields) {
            if (inputField.getOriginalId() == -1 && inputField.getOptional()) {
                continue; //skip missing optional fields
            }
            inputRowMeta.addValueMeta(inputField.getValueMetaInterface(name));
        }
    }

    public StepDialogInterface getDialog(Shell shell, StepMetaInterface meta, TransMeta transMeta, String name) {
        return new SasReaderStepDialog(shell, meta, transMeta, name);
    }

    @Override
    public StepInterface getStep(StepMeta sm, StepDataInterface sdi, int i, TransMeta tm, Trans trans) {
        return new SasReaderStep(sm, sdi, i, tm, trans);
    }

    @Override
    public StepDataInterface getStepData() {
        return new SasReaderStepData();
    }

    @Override
    public Object clone() {
        SasReaderStepMeta copy = (SasReaderStepMeta) super.clone();
        copy.setDefault();

        copy.setFileName(fileName);

        List<SasInputField> list = new ArrayList<>();
        for (SasInputField inputField : inputFields) {
            list.add(new SasInputField(inputField));
        }
        copy.setInputFields(list);

        return copy;
    }

    @Override
    public void setDefault() {
        inputFields = new ArrayList<>();
    }

    @Override
    public String getXML() {
        StringBuilder xml = new StringBuilder();

        xml.append("    ").append(XMLHandler.addTagValue(KEY_FILENAME, fileName));

        for (SasInputField inputField : inputFields) {
            xml.append("    <inputfield>").append(Const.CR);
            xml.append("        ").append(XMLHandler.addTagValue(SasInputField.KEY_ID, inputField.getOriginalId()));
            xml.append("        ").append(XMLHandler.addTagValue(SasInputField.KEY_SAS_NAME, inputField.getSasName()));
            xml.append("        ").append(XMLHandler.addTagValue(SasInputField.KEY_NAME, inputField.getName()));
            xml.append("        ").append(XMLHandler.addTagValue(SasInputField.KEY_SAS_TYPE, inputField.getSasType().toString()));
            xml.append("        ").append(XMLHandler.addTagValue(SasInputField.KEY_KETTLE_TYPE, inputField.getKettleType().toString()));
            xml.append("        ").append(XMLHandler.addTagValue(SasInputField.KEY_LENGTH, inputField.getLength()));
            xml.append("        ").append(XMLHandler.addTagValue(SasInputField.KEY_LABEL, inputField.getLabel()));
            xml.append("        ").append(XMLHandler.addTagValue(SasInputField.KEY_OPTIONAL, inputField.getOptional()));
            xml.append("    </inputfield>").append(Const.CR);
        }
        return xml.toString();
    }

    @Override
    public void loadXML(Node stepnode, List<DatabaseMeta> databases, Map<String, Counter> counters) throws KettleXMLException {
        setDefault();
        try {
            setFileName(XMLHandler.getTagValue(stepnode, KEY_FILENAME));

            int numberOfInputFields = XMLHandler.countNodes(stepnode, "inputfield");
            for (int i = 0; i < numberOfInputFields; i++) {
                SasInputField inputField = new SasInputField();
                Node fieldNode = XMLHandler.getSubNodeByNr(stepnode, "inputfield", i);

                inputField.setOriginalId(Integer.parseInt(XMLHandler.getTagValue(fieldNode, SasInputField.KEY_ID)));
                inputField.setSasName(XMLHandler.getTagValue(fieldNode, SasInputField.KEY_SAS_NAME));
                inputField.setName(XMLHandler.getTagValue(fieldNode, SasInputField.KEY_NAME));
                inputField.setSasType(SasInputField.SasType.valueOf(XMLHandler.getTagValue(fieldNode, SasInputField.KEY_SAS_TYPE)));
                inputField.setKettleType(SasInputField.KettleType.valueOf(XMLHandler.getTagValue(fieldNode, SasInputField.KEY_KETTLE_TYPE)));
                inputField.setLength(Integer.parseInt(XMLHandler.getTagValue(fieldNode, SasInputField.KEY_LENGTH)));
                inputField.setLabel(XMLHandler.getTagValue(fieldNode, SasInputField.KEY_LABEL));
                inputField.setOptional(XMLHandler.getTagValue(fieldNode, SasInputField.KEY_OPTIONAL));

                inputFields.add(inputField);
            }
        } catch (Exception ex) {
            throw new KettleXMLException(BaseMessages.getString(PKG, "Exception.XML.UnableToRead"), ex);
        }
    }

    @Override
    public void saveRep(Repository rep, ObjectId id_transformation, ObjectId id_step) throws KettleException {
        rep.saveStepAttribute(id_transformation, id_step, KEY_FILENAME, fileName);

        int i = 0;
        for (SasInputField inputField : inputFields) {
            rep.saveStepAttribute(id_transformation, id_step, i, SasInputField.KEY_ID, inputField.getOriginalId());
            rep.saveStepAttribute(id_transformation, id_step, i, SasInputField.KEY_SAS_NAME, inputField.getSasName());
            rep.saveStepAttribute(id_transformation, id_step, i, SasInputField.KEY_NAME, inputField.getName());
            rep.saveStepAttribute(id_transformation, id_step, i, SasInputField.KEY_SAS_TYPE, inputField.getSasType().toString());
            rep.saveStepAttribute(id_transformation, id_step, i, SasInputField.KEY_KETTLE_TYPE, inputField.getKettleType().toString());
            rep.saveStepAttribute(id_transformation, id_step, i, SasInputField.KEY_LENGTH, inputField.getLength());
            rep.saveStepAttribute(id_transformation, id_step, i, SasInputField.KEY_LABEL, inputField.getLabel());
            rep.saveStepAttribute(id_transformation, id_step, i, SasInputField.KEY_OPTIONAL, inputField.getOptional());
            i++;
        }
    }

    @Override
    public void readRep(Repository rep, ObjectId id_step, List<DatabaseMeta> databases, Map<String, Counter> counters) throws KettleException {
        setDefault();
        try {
            setFileName(rep.getStepAttributeString(id_step, KEY_FILENAME));

            int numberOfInputFields = rep.countNrStepAttributes(id_step, SasInputField.KEY_ID);
            for (int i = 0; i < numberOfInputFields; i++) {
                SasInputField inputField = new SasInputField();

                inputField.setOriginalId((int) rep.getStepAttributeInteger(id_step, i, SasInputField.KEY_ID));
                inputField.setSasName(rep.getStepAttributeString(id_step, i, SasInputField.KEY_SAS_NAME));
                inputField.setName(rep.getStepAttributeString(id_step, i, SasInputField.KEY_NAME));
                inputField.setSasType(SasInputField.SasType.valueOf(rep.getStepAttributeString(id_step, i, SasInputField.KEY_SAS_TYPE)));
                inputField.setKettleType(SasInputField.KettleType.valueOf(rep.getStepAttributeString(id_step, i, SasInputField.KEY_KETTLE_TYPE)));
                inputField.setLength((int) rep.getStepAttributeInteger(id_step, i, SasInputField.KEY_LENGTH));
                inputField.setLabel(rep.getStepAttributeString(id_step, i, SasInputField.KEY_LABEL));
                inputField.setOptional(rep.getStepAttributeString(id_step, i, SasInputField.KEY_OPTIONAL));

                inputFields.add(inputField);
            }

        } catch (Exception ex) {
            throw new KettleException(BaseMessages.getString(PKG, "Exception.Rep.UnableToRead") + id_step, ex);
        }
    }

    /**
     * Implements a function for verifying step. File an error or warning if: -
     * no sas file is defined - sas file cannot be read - sas file is empty -
     * name of column doesn't exist in sas file - file has duplicite columns -
     * KettleType of some column isn't defined - sas file wasn't found - no
     * field is defined for this step - step input is connected to other step -
     * step output isn't connected
     */
    @Override
    public void check(List<CheckResultInterface> remarks, TransMeta transmeta, StepMeta stepMeta, RowMetaInterface prev, String input[], String output[], RowMetaInterface info) {
        CheckResult cr;

        File sasFile = new File(fileName);
        if (fileName.isEmpty()) {
            // No filename defined
            cr = new CheckResult(CheckResult.TYPE_RESULT_WARNING, BaseMessages.getString(PKG, "Error.NoFilename"), stepMeta);
            remarks.add(cr);
        } else if (!sasFile.exists() || !sasFile.canRead()) {
            // File cannot be read
            cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(PKG, "Error.CantRead"), stepMeta);
            remarks.add(cr);
        } else {
            InputStream stream = null;
            try {
                stream = new FileInputStream(sasFile);
                ParsoService parsoService = new ParsoService(stream);

                if (parsoService.getNumberOfRows() == 0) {
                    // Provided sas file is empty
                    cr = new CheckResult(CheckResult.TYPE_RESULT_WARNING, BaseMessages.getString(PKG, "Error.EmptyFile"), stepMeta);
                    remarks.add(cr);
                }

                try {
                    checkColumnPresence(remarks, stepMeta, parsoService);
                } catch (KettleException ex) {
                    cr = new CheckResult(CheckResult.TYPE_RESULT_WARNING, BaseMessages.getString(PKG, "Warning.UnexpectedException"), stepMeta);
                    remarks.add(cr);
                }

                // Information about file
                cr = new CheckResult(CheckResult.TYPE_RESULT_COMMENT, BaseMessages.getString(PKG, "Comment.FileInfo")
                        .replace("$columns", String.valueOf(parsoService.getNumberOfColumns()))
                        .replace("$rows", String.valueOf(parsoService.getNumberOfRows())), stepMeta);
                remarks.add(cr);
            } catch (FileNotFoundException ex) {
                // File wasn't found
                cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(PKG, "Error.FileNotFound"), stepMeta);
                remarks.add(cr);
            } finally {
                try {
                    if (stream != null) {
                        stream.close();
                    }
                } catch (IOException ex) {
                }
            }
        }
        if (inputFields.isEmpty()) {
            // No field is defined
            cr = new CheckResult(CheckResult.TYPE_RESULT_WARNING, BaseMessages.getString(PKG, "Error.NoFields"), stepMeta);
            remarks.add(cr);
        }
        if (input.length != 0) {
            // Step input is connected
            cr = new CheckResult(CheckResult.TYPE_RESULT_WARNING, BaseMessages.getString(PKG, "Error.Inputs"), stepMeta);
            remarks.add(cr);
        }
        if (output.length == 0) {
            // Step output isn't connected
            cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(PKG, "Error.Outputs"), stepMeta);
            remarks.add(cr);
        }
    }

    public void setFileName(String fileName) {
        if (fileName == null) {
            this.fileName = "";
        } else {
            this.fileName = fileName;
        }
    }

    public String getFileName() {
        return fileName;
    }

    public void setInputFields(List<SasInputField> inputFields) {
        this.inputFields = inputFields;
    }

    public List<SasInputField> getInputFields() {
        return inputFields;
    }

    /**
     * Checks if all non-optional defined columns are in SAS data set (also
     * looks for duplicated). If List CheckResultInterface object is passed,
     * it's filled with remarks. If the null is passed, KettleException is
     * thrown instead.
     *
     * @param remarks null if check is executed from processRow
     * @param stepMeta null if check is executed from processRow
     * @param parsoService
     */
    public void checkColumnPresence(List<CheckResultInterface> remarks, StepMeta stepMeta, ParsoService parsoService) throws KettleException {
        CheckResult cr;
        for (SasInputField inputField : inputFields) {
            int numberOfFoundColumnsInFile = parsoService.countColumnsWithNameSetOrigId(inputField);
            if (numberOfFoundColumnsInFile == 0 && !inputField.getOptional()) {
                if (remarks != null) {
                    cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(SasReaderStepMeta.PKG, "Error.NoNameFound").replace("$name", inputField.getSasName()), stepMeta);
                    remarks.add(cr);
                } else {
                    throw new KettleException(BaseMessages.getString(PKG, "Error.NoNameFound").replace("$name", inputField.getSasName()));
                }
            } else if (numberOfFoundColumnsInFile > 1) {
                if (remarks != null) {
                    cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(SasReaderStepMeta.PKG, "Error.DupliciteColumn")
                            .replace("$name", inputField.getSasName())
                            .replace("$number", String.valueOf(numberOfFoundColumnsInFile)), stepMeta);
                    remarks.add(cr);
                } else {
                    throw new KettleException(BaseMessages.getString(PKG, "Error.DupliciteColumn")
                            .replace("$name", inputField.getSasName())
                            .replace("$number", String.valueOf(numberOfFoundColumnsInFile)));
                }
            } else if (numberOfFoundColumnsInFile == -1) {
                if (remarks != null) {
                    cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(SasReaderStepMeta.PKG, "Error.ParsoServiceInit"), stepMeta);
                    remarks.add(cr);
                } else {
                    throw new KettleException(BaseMessages.getString(PKG, "Error.ParsoServiceInit"));
                }
            }

            if (inputField.getKettleType() == SasInputField.KettleType.NotDefined) {
                // KettleType of field is not defined
                if (remarks != null) {
                    cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(PKG, "Error.NoKettleType").replace("$name", inputField.getName()), stepMeta);
                    remarks.add(cr);
                } else {
                    throw new KettleException(BaseMessages.getString(PKG, "Error.NoKettleType").replace("$name", inputField.getName()));
                }
            }
        }
    }

}
