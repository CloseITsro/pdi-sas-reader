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
import org.apache.commons.io.FilenameUtils;

import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

import cz.closeit.pdi.sasreader.input.SasInputField;
import cz.closeit.pdi.sasreader.parso.ParsoService;

public class SasReaderStep extends BaseStep implements StepInterface {

    private static final Class<?> PKG = SasReaderStepMeta.I18N_CLASS;

    //NO FIELDS HERE PLEASE
    public SasReaderStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
        super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
    }

    @Override
    public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
        SasReaderStepData stepData = (SasReaderStepData) sdi;
        try {
            if (stepData.parsoService != null) {
                stepData.parsoService.dispose();
            }
        } catch (IOException ex) {
            //TODO - better error handling
        }
        stepData.parsoService = null;
        stepData.outputRowMeta = null;
        super.dispose(smi, sdi);
    }

    @Override
    public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
        SasReaderStepData stepData = (SasReaderStepData) sdi;
        SasReaderStepMeta stepMeta = (SasReaderStepMeta) smi;

        if (first) {
            first = false;

            if (stepMeta.getFileName().isEmpty()) {
                throw new KettleException(BaseMessages.getString(PKG, "Error.NoFilename"));
            }

            String filename = environmentSubstitute(stepMeta.getFileName());
            filename = FilenameUtils.normalize(filename).replace("file:", "");
            File sasFile = new File(filename);
            if (!sasFile.exists() || !sasFile.canRead()) {
                throw new KettleException(BaseMessages.getString(PKG, "Error.CantRead"));
            }
            InputStream stream = null;
            try {
                stream = new FileInputStream(sasFile);
            } catch (FileNotFoundException ex) {
                throw new KettleException(BaseMessages.getString(PKG, "Error.FileNotFound"));
            }

            stepData.parsoService = new ParsoService(stream);

            //check if names of defined columns are in sas file
            for (SasInputField inputField : stepMeta.getInputFields()) {
                int numberOfFoundColumnsInFile = stepData.parsoService.countColumnsWithNameSetOrigId(inputField);
                if (numberOfFoundColumnsInFile == 0 && !inputField.getOptional()) {
                    throw new KettleException(BaseMessages.getString(PKG, "Error.NoNameFound").replace("$name", inputField.getSasName()));
                } else if (numberOfFoundColumnsInFile > 1) {
                    throw new KettleException(BaseMessages.getString(PKG, "Error.DupliciteColumn")
                            .replace("$name", inputField.getSasName())
                            .replace("$number", String.valueOf(numberOfFoundColumnsInFile)));
                } else if (numberOfFoundColumnsInFile == -1) {
                    throw new KettleException(BaseMessages.getString(PKG, "Error.ParsoServiceInit"));
                }
            }

            stepData.outputRowMeta = new RowMeta();
            //set the description of output rows based on input
            stepMeta.getFields(stepData.outputRowMeta, getStepname(), null, null, this, null, null); //set meta of output
        }

        Object[] rowFileValues;
        try {
            rowFileValues = stepData.parsoService.getNextRow();
        } catch (IOException ex) {
            setErrors(1);
            rowFileValues = null;
        }
        if (rowFileValues == null) {
            setOutputDone();
            return false;
        }

        Object[] rowValues = RowDataUtil.allocateRowData(stepData.outputRowMeta.size());

        int outputIndex = 0;
        for (SasInputField inputField : stepMeta.getInputFields()) {

            int originalIndex = inputField.getOriginalId() - 1;

            if (inputField.getOptional() && ((originalIndex > rowFileValues.length - 1) || originalIndex == -1)) {
                continue;
            }

            Object objectFromSasFile = rowFileValues[originalIndex];

            if (objectFromSasFile == null) {
                rowValues[outputIndex] = null;
            } else if (inputField.getKettleType().needConversion(objectFromSasFile)) {
                try {
                    rowValues[outputIndex] = inputField.getKettleType().convert(objectFromSasFile);
                } catch (ClassCastException e) {
                    String msg = String.format("Converting attribute %s - %s(%s) -> %s(%s) failed [%s]",
                            inputField.getSasName(),
                            inputField.getOriginalId(),
                            objectFromSasFile.getClass().getName(),
                            inputField.getName(),
                            inputField.getKettleType(),
                            e.getMessage());
                    throw new ClassCastException(msg);
                }
            } else {
                rowValues[outputIndex] = rowFileValues[originalIndex];
            }
            outputIndex++;
        }
        putRow(stepData.outputRowMeta, rowValues);

        return true;
    }

}
