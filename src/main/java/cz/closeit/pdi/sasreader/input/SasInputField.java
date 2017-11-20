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

package cz.closeit.pdi.sasreader.input;

import java.math.BigDecimal;
import java.util.Date;

import org.pentaho.di.core.exception.KettlePluginException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaFactory;

public class SasInputField {

    public static final String KEY_ID = "originalid";
    public static final String KEY_SAS_NAME = "sasname";
    public static final String KEY_NAME = "name";
    public static final String KEY_SAS_TYPE = "sastype";
    public static final String KEY_KETTLE_TYPE = "kettletype";
    public static final String KEY_LENGTH = "length";
    public static final String KEY_LABEL = "label";

    private int originalId;
    private String sasName = "";
    private String name = "";
    private SasType sasType;
    private KettleType kettleType = KettleType.NotDefined;
    private int length = 0;
    private String label = "";

    public SasInputField() {
    }

    public SasInputField(SasInputField fieldToClone) {
        originalId = fieldToClone.getOriginalId();
        sasName = fieldToClone.getSasName();
        name = fieldToClone.getName();
        sasType = fieldToClone.getSasType();
        kettleType = fieldToClone.getKettleType();
        length = fieldToClone.getLength();
        label = fieldToClone.getLabel();
    }

    public enum SasType {
        Character, Numeric
    }

    public enum KettleType {
        String(ValueMetaInterface.TYPE_STRING),
        Number(ValueMetaInterface.TYPE_NUMBER),
        BigNumber(ValueMetaInterface.TYPE_BIGNUMBER),
        Integer(ValueMetaInterface.TYPE_INTEGER),
        Date(ValueMetaInterface.TYPE_DATE),
        NotDefined(0);

        private KettleType(int type) {
            kettleConst = type;
        }

        private final int kettleConst;

        /**
         * Check if the object needs to be converted. Don't use any other object
         * than the one returned in row from parso library.
         * It will return wrong boolean otherwise.
         *
         * @param object Integer, Long, Double, String or Date
         * @return false if the conversion isn't necessary, true otherwise or for other object than Integer, Long, Double, String or Date
         */
        public boolean needConversion(Object object) {
            if (object instanceof Long && kettleConst == ValueMetaInterface.TYPE_INTEGER) {
                return false;
            } else if (object instanceof Double && kettleConst == ValueMetaInterface.TYPE_NUMBER) {
                return false;
            } else if (object instanceof Date && kettleConst == ValueMetaInterface.TYPE_DATE) {
                return false;
            } else if (object instanceof String && kettleConst == ValueMetaInterface.TYPE_STRING) {
                return false;
            } else {
                return true;
            }
        }

        /**
         * Convert row value object from parso service to this kettle type
         * compatible object.
         *
         * For example, one value in row from sas file is Double but kettle type
         * of field is BigNumber (BigDecimal in Java) so this will convert Double to BigDecimal; The
         * conversion might return null if the input object isn't one of the
         * supported types or the conversion didn't work.
         *
         * @param object Integer, Long, Double, Date or String
         * @return converted object or null, if the conversion didn't work or
         * the input object isn't supported
         */
        public Object convert(Object object) {
            if (object instanceof Integer) {
                switch (kettleConst) {
                    case ValueMetaInterface.TYPE_INTEGER:
                        return ((Integer) object).longValue();
                    case ValueMetaInterface.TYPE_STRING:
                        return ((Integer) object).toString();
                    case ValueMetaInterface.TYPE_NUMBER:
                        return ((Integer) object).doubleValue();
                    case ValueMetaInterface.TYPE_BIGNUMBER:
                        return new BigDecimal((Integer) object);
                    case ValueMetaInterface.TYPE_DATE:
                        return new Date(((Integer) object).longValue());
                    default:
                        return null;
                }
            } else if (object instanceof Long) {
                switch (kettleConst) {
                    case ValueMetaInterface.TYPE_INTEGER:
                        return object;
                    case ValueMetaInterface.TYPE_STRING:
                        return ((Long) object).toString();
                    case ValueMetaInterface.TYPE_NUMBER:
                        return ((Long) object).doubleValue();
                    case ValueMetaInterface.TYPE_BIGNUMBER:
                        return new BigDecimal((Long) object);
                    case ValueMetaInterface.TYPE_DATE:
                        return new Date((Long) object);
                    default:
                        return null;
                }
            } else if (object instanceof Double) {
                switch (kettleConst) {
                    case ValueMetaInterface.TYPE_NUMBER:
                        return object;
                    case ValueMetaInterface.TYPE_BIGNUMBER:
                        return new BigDecimal(((Double) object).toString());
                    case ValueMetaInterface.TYPE_INTEGER:
                        return ((Double) object).longValue();
                    case ValueMetaInterface.TYPE_STRING:
                        return ((Double) object).toString();
                    case ValueMetaInterface.TYPE_DATE:
                        return new Date(((Double) object).longValue());
                    default:
                        return null;
                }
            } else if (object instanceof Date) {
                switch (kettleConst) {
                    case ValueMetaInterface.TYPE_DATE:
                        return object;
                    case ValueMetaInterface.TYPE_NUMBER:
                        return Long.valueOf(((Date) object).getTime()).doubleValue();
                    case ValueMetaInterface.TYPE_BIGNUMBER:
                        return new BigDecimal(((Date) object).getTime());
                    case ValueMetaInterface.TYPE_INTEGER:
                        return ((Date) object).getTime();
                    case ValueMetaInterface.TYPE_STRING:
                        return ((Date) object).toString();
                    default:
                        return null;
                }

            } else if (object instanceof String) {
                try {
                    switch (kettleConst) {
                        case ValueMetaInterface.TYPE_STRING:
                            return object;
                        case ValueMetaInterface.TYPE_INTEGER:
                            return new Long((String) object);
                        case ValueMetaInterface.TYPE_NUMBER:
                            return new Double((String) object);
                        case ValueMetaInterface.TYPE_BIGNUMBER:
                            return new BigDecimal((String) object);
                        case ValueMetaInterface.TYPE_DATE:
                            return new Date(new Long((String) object));
                        default:
                            return null;
                    }
                } catch (Exception ex) {
                    return null;
                }
            }
            return null;
        }

        public int getKettleConstant() {
            return kettleConst;
        }
    }

    public ValueMetaInterface getValueMetaInterface(String originName) throws KettleStepException {
        ValueMetaInterface vmi;
        try {
            vmi = ValueMetaFactory.createValueMeta(name, kettleType.getKettleConstant());
        } catch (KettlePluginException ex) {
            throw new KettleStepException();
        }
        vmi.setLength(length);
        vmi.setOrigin(originName);
        return vmi;
    }

    public void setOriginalId(int id) {
        originalId = id;
    }

    public int getOriginalId() {
        return originalId;
    }

    public void setSasName(String name) {
        if (name == null) {
            this.sasName = "";
        } else {
            this.sasName = name;
        }
    }

    public String getSasName() {
        return sasName;
    }

    public void setName(String name) {
        if (name == null) {
            this.name = "";
        } else {
            this.name = name;
        }
    }

    public String getName() {
        return name;
    }

    public void setSasType(SasType type) {
        this.sasType = type;
    }

    public SasType getSasType() {
        return sasType;
    }

    public void setKettleType(KettleType type) {
        kettleType = type;
    }

    public KettleType getKettleType() {
        return kettleType;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getLength() {
        return length;
    }

    public void setLabel(String label) {
        if (label == null) {
            this.label = "";
        } else {
            this.label = label;
        }
    }

    public String getLabel() {
        return label;
    }

}
