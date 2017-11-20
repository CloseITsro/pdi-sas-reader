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

import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

import cz.closeit.pdi.sasreader.parso.ParsoService;

public class SasReaderStepData extends BaseStepData implements StepDataInterface{
    
    public RowMetaInterface outputRowMeta = null;
    public ParsoService parsoService = null;
    
    public SasReaderStepData() {
        super();
    }
}
