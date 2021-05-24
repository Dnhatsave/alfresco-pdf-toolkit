/*
 * Copyright 2008-2012 Alfresco Software Limited.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * This file is part of an unsupported extension to Alfresco.
 */

package org.alfresco.extension.pdftoolkit.repo.action.executer;


import java.util.HashMap;
import java.util.List;

import org.alfresco.extension.pdftoolkit.constants.PDFToolkitConstants;
import org.alfresco.extension.pdftoolkit.service.PDFToolkitService;


import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;

import org.alfresco.extension.pdftoolkit.constraints.MapConstraint;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.ServiceRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class PDFWatermarkActionExecuter extends ActionExecuterAbstractBase
{

    protected PDFToolkitService					pdfToolkitService;
    protected ServiceRegistry     				serviceRegistry;


    /**
     * The logger
     */
    private static Log                    logger                   = LogFactory.getLog(PDFWatermarkActionExecuter.class);

    /**
     * Action constants
     */
    public static final String            NAME                     = "pdf-watermark";

 
    /**
     * Add parameter definitions
     */
    @Override
    protected void addParameterDefinitions(List<ParameterDefinition> paramList)
    {
        paramList.add(new ParameterDefinitionImpl(PDFToolkitConstants.PARAM_INPLACE, DataTypeDefinition.BOOLEAN, false, getParamDisplayLabel(PDFToolkitConstants.PARAM_INPLACE), false));
        paramList.add(new ParameterDefinitionImpl(PDFToolkitConstants.PARAM_CREATE_NEW, DataTypeDefinition.BOOLEAN, false, getParamDisplayLabel(PDFToolkitConstants.PARAM_CREATE_NEW), false));
        // formulario
        paramList.add(new ParameterDefinitionImpl(PDFToolkitConstants.PARAM_KEY_PASSWORD, DataTypeDefinition.TEXT, true, getParamDisplayLabel(PDFToolkitConstants.PARAM_KEY_PASSWORD)));


    }


    public void setPDFToolkitService(PDFToolkitService pdfToolkitService)
    {
    	this.pdfToolkitService = pdfToolkitService;
    }

   
    public void setServiceRegistry(ServiceRegistry serviceRegistry)
    {
        this.serviceRegistry = serviceRegistry;
    }

    /**
     * @see org.alfresco.repo.action.executer.ActionExecuterAbstractBase#executeImpl(org.alfresco.service.cmr.repository.NodeRef,
     * org.alfresco.service.cmr.repository.NodeRef)
     */
    // Remocao do action/parametros
    @Override
    protected void executeImpl(Action action, NodeRef actionedUponNodeRef)
    {
    	NodeRef result = pdfToolkitService.watermarkPDF(actionedUponNodeRef);
        action.setParameterValue(PARAM_RESULT, result);
    }
}
