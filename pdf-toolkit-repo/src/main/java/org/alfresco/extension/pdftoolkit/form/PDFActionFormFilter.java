package org.alfresco.extension.pdftoolkit.form;

import java.util.List;
import java.util.Map;

import org.alfresco.extension.pdftoolkit.constants.PDFToolkitConstants;
import org.alfresco.repo.action.ActionDefinitionImpl;
import org.alfresco.repo.forms.Form;
import org.alfresco.repo.forms.FormData;
import org.alfresco.repo.forms.FormData.FieldData;
import org.alfresco.repo.forms.processor.AbstractFilter;
import org.alfresco.repo.forms.processor.action.ActionFormResult;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PDFActionFormFilter extends AbstractFilter<Object, ActionFormResult> {
	
	private static Log 		logger 						= LogFactory.getLog(PDFActionFormFilter.class);
	private String 			WATERMARK_IMAGE_FIELD 		= "assoc_watermark-image_added";
	private String			DESTINATION_FOLDER_FIELD 	= "assoc_destination-folder_added";
	private String			INPLACE_PARAM				= "prop_" + PDFToolkitConstants.PARAM_INPLACE;
	
	private ServiceRegistry serviceRegistry;		
	private Repository repositoryHelper;
	
	@Override
	public void afterGenerate(Object obj, List<String> fields,
			List<String> forcedFields, Form form, Map<String, Object> context) {
		logger.debug("afterGenerate");
		//NTM - nothing to do here at the moment
	}

	@Override
	public void afterPersist(Object obj, FormData formData, ActionFormResult result) {
		logger.debug("afterPersist");
		//NTM - nothing to do here at the moment.
	}
	
	@Override
	public void beforeGenerate(Object obj, List<String> fields,
			List<String> forcedFields, Form form, Map<String, Object> context) {
		logger.debug("beforeGenerate");
		//NTM - nothing to do here at the moment
	}

	@Override
	public void beforePersist(Object obj, FormData formData) {
		logger.debug("beforePersist");
		
	}

	public void setServiceRegistry(ServiceRegistry serviceRegistry)
	{
		this.serviceRegistry = serviceRegistry;
	}
	
	public void setRepositoryHelper(Repository repositoryHelper)
	{
		this.repositoryHelper = repositoryHelper;
	}
}
