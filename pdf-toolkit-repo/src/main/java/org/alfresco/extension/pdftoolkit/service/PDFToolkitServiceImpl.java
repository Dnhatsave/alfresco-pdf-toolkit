package org.alfresco.extension.pdftoolkit.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.imageio.ImageIO;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.extension.pdftoolkit.constants.PDFToolkitConstants;
import org.alfresco.extension.pdftoolkit.model.PDFToolkitModel;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.template.FreeMarkerProcessor;
import org.alfresco.repo.template.TemplateNode;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.extensions.surf.util.I18NUtil;

import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.property.TextAlignment;

public class PDFToolkitServiceImpl extends PDFToolkitConstants implements PDFToolkitService 
{
    private final static String MSGID_PAGE_NUMBERING_PATTERN_MULTIPLE="pdftoolkit.split-page-numbering-pattern-multiple";
    private final static String MSGID_PAGE_NUMBERING_PATTERN_SINGLE="pdftoolkit.split-page-numbering-pattern-single";
	
	private ServiceRegistry serviceRegistry;
    private NodeService ns;
    private ContentService cs;
    private FileFolderService ffs;
    private DictionaryService ds;
    private PersonService ps;
    private AuthenticationService as;
    
    private FreeMarkerProcessor freemarkerProcessor = new FreeMarkerProcessor();
    
    private static Log logger = LogFactory.getLog(PDFToolkitServiceImpl.class);
    
    // do we need to apply the encryption aspect when we encrypt?
    private boolean useEncryptionAspect = true;
    
    // do we need to apply the signature aspect when we sign?
    private boolean useSignatureAspect = true;
    
    // when we create a new document, do we actually create a new one, or copy the source?
    private boolean createNew = false;
    

  
	@Override
	public NodeRef watermarkPDF(NodeRef targetNodeRef) 
	{

		NodeRef destinationNode = null;
		
        try
        {
        	ContentReader targetReader = getReader(targetNodeRef);
            // remocao do options/parametros
            destinationNode = this.textAction(targetNodeRef, targetReader);         
        }
        catch (AlfrescoRuntimeException e)
        {
            throw new AlfrescoRuntimeException(e.getMessage(), e);
        }
        
        return destinationNode;
	}


	private ContentReader getReader(NodeRef nodeRef)
    {
		// first, make sure the node exists
		if (ns.exists(nodeRef) == false)
        {
            // node doesn't exist - can't do anything
            throw new AlfrescoRuntimeException("NodeRef: " + nodeRef + " does not exist");
        }
		
        // Next check that the node is a sub-type of content
        QName typeQName = ns.getType(nodeRef);
        if (ds.isSubClass(typeQName, ContentModel.TYPE_CONTENT) == false)
        {
            // it is not content, so can't transform
            throw new AlfrescoRuntimeException("The selected node is not a content node");
        }

        // Get the content reader.  If it is null, can't do anything here
        ContentReader contentReader = cs.getReader(nodeRef, ContentModel.PROP_CONTENT);

        if(contentReader == null)
        {
        	throw new AlfrescoRuntimeException("The content reader for NodeRef: " + nodeRef + "is null");
        }
        
        return contentReader;
    }

    /**
     * @param ruleAction
     * @param filename
     * @return
     */
    private NodeRef createDestinationNode(String filename, NodeRef destinationParent, NodeRef target, boolean inplace)
    {

    	NodeRef destinationNode;
    	
    	// if inplace mode is turned on, the destination for the modified content
    	// is the original node
    	if(inplace)
    	{
    		return target;
    	}
    	
    	if(createNew)
    	{
	    	//create a file in the right location
	        FileInfo fileInfo = ffs.create(destinationParent, filename, ContentModel.TYPE_CONTENT);
	        destinationNode = fileInfo.getNodeRef();
    	}
    	else
    	{
    		try 
    		{
	    		FileInfo fileInfo = ffs.copy(target, destinationParent, filename);
	    		destinationNode = fileInfo.getNodeRef();
    		}
    		catch(FileNotFoundException fnf)
    		{
    			throw new AlfrescoRuntimeException(fnf.getMessage(), fnf);
    		}
    	}

        return destinationNode;
    }
    /**
     * @author Dercio Ink CloudSign
     */
    // Create new node on the same path
    private NodeRef replaceNode(String filename, NodeRef target, boolean inplace)
    {

    	NodeRef destinationNode;
    	
    	// if inplace mode is turned on, the destination for the modified content
    	// is the original node
    	if(inplace)
    	{
    		return target;
    	}
    	
    		try 
    		{
	    		FileInfo fileInfo = ffs.copy(target, target, filename);
	    		destinationNode = fileInfo.getNodeRef();
    		}
    		catch(FileNotFoundException fnf)
    		{
    			throw new AlfrescoRuntimeException(fnf.getMessage(), fnf);
    		}
    	

        return destinationNode;
    }
    
    private int getInteger(Serializable val)
    {
    	if(val == null)
    	{ 
    		return 0;
    	}
    	try
    	{
    		return Integer.parseInt(val.toString());
    	}
    	catch(NumberFormatException nfe)
    	{
    		return 0;
    	}
    }
    
    private File getTempFile(NodeRef nodeRef)
    {
    	File alfTempDir = TempFileProvider.getTempDir();
        File toolkitTempDir = new File(alfTempDir.getPath() + File.separatorChar + nodeRef.getId());
        toolkitTempDir.mkdir();
        File file = new File(toolkitTempDir, ffs.getFileInfo(nodeRef).getName());
        
        return file;
    }
    
    private String getFilename(Map<String, Serializable> params, NodeRef targetNodeRef)
    {
    	Serializable providedName = params.get(PARAM_DESTINATION_NAME);
        String fileName = null;
        if(providedName != null)
        {
        	fileName = String.valueOf(providedName);
        	if(!fileName.endsWith(FILE_EXTENSION))
        	{
        		fileName = fileName + FILE_EXTENSION;
        	}
        }
        else
        {
        	fileName = String.valueOf(ns.getProperty(targetNodeRef, ContentModel.PROP_NAME));
        }
        return fileName;
    }
    
    /**
	 * Parses the list of pages or page ranges to delete and returns a list of page numbers 
	 * 
	 * @param list
	 * @return
	 */
	private List<Integer> parsePageList(String list)
	{
		List<Integer> pages = new ArrayList<Integer>();
		String[] tokens = list.split(",");
		for(String token : tokens)
		{
			//parse each, if one is not an int, log it but keep going
			try 
			{
				pages.add(Integer.parseInt(token));
			}
			catch(NumberFormatException nfe)
			{
				logger.warn("Page list contains non-numeric values");
			}
		}
		return pages;
	}
	
   
    
    /**
     * @param fileName
     * @param extension
     * @return
     */
    private String removeExtension(String fileName, String extension)
    {
        // Does the file have the extension?
        if (fileName != null && fileName.contains(extension))
        {
            // Where does the extension start?
            int extensionStartsAt = fileName.indexOf(extension);
            // Get the Filename sans the extension
            return fileName.substring(0, extensionStartsAt);
        }

        return fileName;
    }

    private String getFilename(NodeRef targetNodeRef)
    {
        FileInfo fileInfo = ffs.getFileInfo(targetNodeRef);
        String filename = fileInfo.getName();

        return filename;
    }

    private String getFilenameSansExt(NodeRef targetNodeRef, String extension)
    {
        String filenameSansExt;
        filenameSansExt = removeExtension(getFilename(targetNodeRef), extension);

        return filenameSansExt;
    }
    

    /**
     * Applies a text watermark (current date, user name, etc, depending on
     * options)
     * 
     * @param reader
     * @param writer
     * @param options
     */

     // remocao de options / parametros
    private NodeRef textAction(NodeRef targetNodeRef, ContentReader actionedUponContentReader)
    {

        File tempDir = null;
        ContentWriter writer = null;
        NodeRef destinationNode = null;
        
        try
        {
            File file = getTempFile(targetNodeRef);

            // get the PDF input stream and create a reader for iText
            // PdfReader reader = new PdfReader(actionedUponContentReader.getContentInputStream());
            // stamp = new PdfStamper(reader, new FileOutputStream(file));
            // PdfContentByte pcb;
            //////////////// iText 5 //////////////////////////////////

            // Certificar o writer
            PdfDocument pdfDoc = new PdfDocument(new PdfReader(actionedUponContentReader.getContentInputStream()),
                                 new PdfWriter(file));

            // get the PDF pages and position
            // String pages = (String)options.get(PARAM_PAGE);
            // String position = (String)options.get(PARAM_POSITION);
            // String depth = (String)options.get(PARAM_WATERMARK_DEPTH);
            // int locationX = getInteger(options.get(PARAM_LOCATION_X));
            // int locationY = getInteger(options.get(PARAM_LOCATION_Y));
            Boolean inplace = true;

            // // create the base font for the text stamp
            // BaseFont bf = BaseFont.createFont((String)options.get(PARAM_WATERMARK_FONT), BaseFont.CP1250, BaseFont.EMBEDDED);

            // // get watermark text and process template with model
            // String templateText = (String)options.get(PARAM_WATERMARK_TEXT);
            // Map<String, Object> model = buildWatermarkTemplateModel(targetNodeRef);
            // StringWriter watermarkWriter = new StringWriter();
            // freemarkerProcessor.processString(templateText, model, watermarkWriter);
            // watermarkText = watermarkWriter.getBuffer().toString();

            // // tokenize watermark text to support multiple lines and copy tokens
            // // to vector for re-use
            // st = new StringTokenizer(watermarkText, "\r\n", false);
            // while (st.hasMoreTokens())
            // {
            //     tokens.add(st.nextToken());
            // }

            // stamp each page
            //int numpages = reader.getNumberOfPages();
            float x, y;

            for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++)
            {
                //Rectangle pageSize = reader.getPageSizeWithRotation(i); iText 5
                Rectangle pageSize = pdfDoc.getPage(i).getPageSize();

                // getting the canvas covering the existing content
                //canvas = stamp.getOverContent(i); iText 5
                PdfCanvas canvas = new PdfCanvas(pdfDoc.getPage(i));

                // adding some lines to the right
                // ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
                //     new Phrase("Documento Assinado electronicamente por: "+ AuthenticationUtil.getRunAsUser() + " | " + new java.util.Date() ),
                //     x - 18, y, 90);
                // //recuperar o url e colocar no Doc
                // ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
                //     new Phrase("validar: http://localhost:8080/sgd/page/site/testes/document-details"),
                //     x - 34, y, 90); 
                ////////////////// iText 5 //////////////////////////////////////////
               
                // Adding sme line on right side
                drawText(canvas, pdfDoc, pageSize, pageSize.getRight() - 18,
                (pageSize.getTop() + pageSize.getBottom()) / 2, 90);

                drawDate(canvas, pdfDoc, pageSize, pageSize.getRight() - 1,
                (pageSize.getTop() + pageSize.getBottom()) / 2, 90);
            }

            pdfDoc.close();

            String fileName = getFilename(targetNodeRef);
            
            // Get a writer and prep it for putting it back into the repo
            //can't use BasePDFActionExecuter.getWriter here need the nodeRef of the destination
            /**
             * @author Dercio Ink
             * Create document on same directory always
             */
            destinationNode = replaceNode(fileName, targetNodeRef, inplace);
            writer = cs.getWriter(destinationNode, ContentModel.PROP_CONTENT, true);
            writer.setEncoding(actionedUponContentReader.getEncoding());
            writer.setMimetype(FILE_MIMETYPE);

            // Put it in the repo
            writer.putContent(file);

            // delete the temp file
            file.delete();
            
            // Add Aspect Of Signed 
            ns.addAspect(destinationNode, PDFToolkitModel.ASPECT_SIGNED, new HashMap<QName, Serializable>());
            ns.setProperty(destinationNode, PDFToolkitModel.PROP_SIGNATUREDATE, new java.util.Date());
		    ns.setProperty(destinationNode, PDFToolkitModel.PROP_SIGNEDBY, AuthenticationUtil.getRunAsUser());
			

        }
        catch (IOException e)
        {
            throw new AlfrescoRuntimeException(e.getMessage(), e);
        }
        // catch (DocumentException e)
        // {
        //     throw new AlfrescoRuntimeException(e.getMessage(), e);
        // }
        finally
        {
            if (tempDir != null)
            {
                try
                {
                    tempDir.delete();
                }
                catch (Exception ex)
                {
                    throw new AlfrescoRuntimeException(ex.getMessage(), ex);
                }
            }

            // if (pdfDoc != null)
            // {
            //     try
            //     {
            //         pdfDoc.close();
            //     }
            //     catch (Exception ex)
            //     {
            //         throw new AlfrescoRuntimeException(ex.getMessage(), ex);
            //     }
            // }
        }
        
        return destinationNode;
    }

    private void drawText(PdfCanvas canvas, PdfDocument pdfDoc, Rectangle pageSize, float x, float y, double rotation) {
        Canvas canvasDrawText = new Canvas(canvas, pageSize)
                .showTextAligned("Documento assinado electronicamente https://www.sgd.gov.mz/validarDocumento?=test.pdf",
                        x, y, TextAlignment.CENTER, (float) Math.toRadians(rotation));
        canvasDrawText.close();
    }  
    private void drawDate(PdfCanvas canvas, PdfDocument pdfDoc, Rectangle pageSize, float x, float y, double rotation) {
        Canvas canvasDrawText = new Canvas(canvas, pageSize)
                .showTextAligned("Assinado por: " + AuthenticationUtil.getRunAsUser() + " aos " + new java.util.Date() ,
                        x, y, TextAlignment.CENTER, (float) Math.toRadians(rotation));
        canvasDrawText.close();
    }
    
    /**
     * Determines whether or not a watermark should be applied to a given page
     * 
     * @param pages
     * @param current
     * @param numpages
     * @return
     */
    private boolean checkPage(String pages, int current, int numpages)
    {

    	
        boolean markPage = false;

        if (pages.equals(PAGE_EVEN))
        {
            if (current % 2 == 0)
            {
                markPage = true;
            }
        }
        else if (pages.equals(PAGE_ODD))
        {
            if (current % 2 != 0)
            {
                markPage = true;
            }
        }
        else if (pages.equals(PAGE_FIRST))
        {
            if (current == 1)
            {
                markPage = true;
            }
        }
        else if (pages.equals(PAGE_LAST))
        {
            if (current == numpages)
            {
                markPage = true;
            }
        }
        else if (pages.equals(PAGE_ALL))
        {
            markPage = true;
        }
        else
        {
        	// if we get here, a scheme wasn't selected, so we can treat this like a page list
        	List<Integer> pageList = parsePageList(pages);
        	if(pageList.contains(current))
        	{
        		markPage = true;
        	}
        }

        return markPage;
    }



    /**
     * Format the page numbers according to the localized string in messages
     * 
     * @param currentPage
     * @param lastPage
     * @return
     */
    private String formatPageNumbering(int currentPage, int lastPage)
    {
    	String text = "";
    	if (lastPage==0) 
    	{
    		text = I18NUtil.getMessage(MSGID_PAGE_NUMBERING_PATTERN_SINGLE, new Object[]{currentPage});
    	}
    	else
    	{
    		text = I18NUtil.getMessage(MSGID_PAGE_NUMBERING_PATTERN_MULTIPLE, new Object[]{currentPage, lastPage});
    	}
    	return text;
    }
    
    /**
     * @param serviceRegistry
     */
    public void setServiceRegistry(ServiceRegistry serviceRegistry)
    {
        this.serviceRegistry = serviceRegistry;
        ns = serviceRegistry.getNodeService();
        cs = serviceRegistry.getContentService();
        ffs = serviceRegistry.getFileFolderService();
        ds = serviceRegistry.getDictionaryService();
        ps = serviceRegistry.getPersonService();
        as = serviceRegistry.getAuthenticationService();
    }
    
    /**
     * Sets whether a PDF action creates a new empty node or copies the source node, preserving
     * the content type, applied aspects and properties
     * 
     * @param createNew
     */
    public void setCreateNew(boolean createNew)
    {
    	this.createNew = createNew;
    }
    
    public void setUseSignatureAspect(boolean useSignatureAspect)
    {
    	this.useSignatureAspect = useSignatureAspect;
    }
    
    public void setUseEncryptionAspect(boolean useEncryptionAspect)
    {
    	this.useEncryptionAspect = useEncryptionAspect;
    }
}
