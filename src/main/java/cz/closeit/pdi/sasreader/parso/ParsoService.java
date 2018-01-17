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

package cz.closeit.pdi.sasreader.parso;

import com.epam.parso.Column;
import com.epam.parso.SasFileReader;
import com.epam.parso.impl.SasFileReaderImpl;

import cz.closeit.pdi.sasreader.SasReaderStepMeta;
import cz.closeit.pdi.sasreader.input.SasInputField;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ParsoService {

    private static final int KETTLE_TYPE_ANALYZING_LIMIT_ROWS = 7000;

    private SasFileReader parsoReader = null;
    private InputStream stream = null;
    private boolean isReady = false;

    public ParsoService(InputStream inputStream) {
        init(inputStream);
    }

    /**
     * Get information about columns and save them to provided SasReaderStepMeta
     * object. This function doesn't close provided input stream.
     *
     * @param inputStream
     * @param stepMeta
     * @param preferBigNumber use KettleType BigNumber instead of Number
     */
    public static void getFieldsFromFile(InputStream inputStream, SasReaderStepMeta stepMeta, boolean preferBigNumber) {
        ParsoService service = new ParsoService(inputStream);

    	List<SasInputField> inputFields = new ArrayList<>();

        for (Column column : service.parsoReader.getColumns()) {
            SasInputField field = new SasInputField();

            field.setOriginalId(column.getId());
            field.setName(column.getName());
            field.setSasName(column.getName());
            if (column.getType() == Number.class) {
                field.setSasType(SasInputField.SasType.Numeric);
            } else {
                field.setSasType(SasInputField.SasType.Character);
                field.setKettleType(SasInputField.KettleType.String);
            }
            field.setLength(column.getLength());
            field.setLabel(column.getLabel());

            inputFields.add(field);
        }
        analyzeKettleTypes(inputFields, service.parsoReader, preferBigNumber);
        stepMeta.setInputFields(inputFields);
    }

    private static void analyzeKettleTypes(List<SasInputField> inputFields, SasFileReader parsoReader, boolean preferBigNumber) {

        boolean loadAnother = true;
        int rowCounter = 0;

        while (loadAnother) {
            rowCounter++;
            loadAnother = false;
            Object[] row;
            try {
                row = parsoReader.readNext();
            } catch (IOException ex) {
                break;
            }
            if (row == null) {
                break;
            }

            for (SasInputField inputField : inputFields) {
                if (inputField.getKettleType() == SasInputField.KettleType.NotDefined) {
                    int objectIndex = inputField.getOriginalId() - 1;
                    if (row[objectIndex] == null) {
                        loadAnother = true;
                        continue;
                    }
                    if (row[objectIndex] instanceof Integer) {
                        inputField.setKettleType(SasInputField.KettleType.Integer);
                    } else if (row[objectIndex] instanceof Long) {
                        inputField.setKettleType(SasInputField.KettleType.Integer);
                    } else if (row[objectIndex] instanceof Date) {
                        inputField.setKettleType(SasInputField.KettleType.Date);
                    } else if (row[objectIndex] instanceof Double && preferBigNumber == false) {
                        inputField.setKettleType(SasInputField.KettleType.Number);
                    } else if (row[objectIndex] instanceof Double && preferBigNumber == true) {
                        inputField.setKettleType(SasInputField.KettleType.BigNumber);
                    }
                }
            }
            if (rowCounter == KETTLE_TYPE_ANALYZING_LIMIT_ROWS) {
                loadAnother = false;
            }
        }

    }

    /**
     * Get next row of sas file.
     *
     * @return Array of object, null if there is not any other row or service is
     * not ready
     * @throws IOException if reading input stream is impossible
     */
    public Object[] getNextRow() throws IOException {
        if (isReady) {
            return parsoReader.readNext();
        } else {
            return null;
        }
    }

    /**
     * If you disposed the service but you want to use it again, use this
     * method.
     *
     * @param inputStream
     */
    public void init(InputStream inputStream) {
        if (!isReady) {
            stream = inputStream;
            parsoReader = new SasFileReaderImpl(inputStream);
            isReady = true;
        }
    }

    /**
     * When you're done with processing, dispose this service. It will also
     * close associated input stream.
     *
     * @throws IOException
     */
    public void dispose() throws IOException {
        if (isReady) {
            parsoReader = null;
            stream.close();
            stream = null;
            isReady = false;
        }
    }

    public boolean isReady() {
        return isReady;
    }

    public long getNumberOfRows() {
        if (isReady) {
            return parsoReader.getSasFileProperties().getRowCount();
        }
        return -1;
    }
    
    public long getNumberOfColumns() {
        if (isReady) {
            return parsoReader.getSasFileProperties().getColumnsCount();
        }
        return -1;
    }

    /**
     * Check if sas file has column with specified id.
     *
     * @param fieldId id of field we want to find
     * @return
     */
    public boolean hasFieldId(int fieldId) {
        if (isReady) {
            for (Column column : parsoReader.getColumns()) {
                if (column.getId() == fieldId) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    /**
     * Count the number of columns in file with specified name. This is done in
     * order to find non-existent (or renamed) columns and duplicated columns.
     * The id of last column in file with same name is saved to SasInputField. 
     * If the field isn't there, the original id is set to -1.
     * Search is case insensitive.
     *
     * @param inputField sas input field we want to find
     * @return number of columns in file with name specified in
     * SasInputField.sasName or -1 if service isn't ready
     */
    public int countColumnsWithNameSetOrigId(SasInputField inputField) {
        int counter = -1;
        if (isReady) {
            counter = 0;
            inputField.setOriginalId(-1);
            for (Column column : parsoReader.getColumns()) {
                if (column.getName().equalsIgnoreCase(inputField.getSasName())) {
                    inputField.setOriginalId(column.getId());
                    counter++;
                }
            }
            return counter;
        }
        return counter;
    }

}
