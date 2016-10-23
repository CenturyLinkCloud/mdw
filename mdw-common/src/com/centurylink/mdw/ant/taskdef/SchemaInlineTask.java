/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.ant.taskdef;

//project
import java.io.File;
import java.io.FileOutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/***
 * Ant task to inline all included schemas.
 */
public class SchemaInlineTask extends Task {

    /** The following data members are used for setting attributes */
    private File mOutputFile;
    private File mInputFile;
    private boolean mVerbose;
    private File mSchemaRoot;
    private boolean mOverwrite;

    /** The following data members are used during TASK execution */
    private Hashtable<String,String> mIncludeMap;
    private DocumentBuilder mDb;
    private Hashtable<String,String> mDirList;
    private DirListTask mDirListTask;

    /** static fields */
    private static final String msINCLUDE = "xsd:include";

    /**
     * constructor (required for ANT TASK)
     */
    public SchemaInlineTask()
    {
        // Create tale for storing unique include file names
        mIncludeMap = new Hashtable<String,String>();

        // Create the directory listing task
        mDirListTask = new DirListTask();
    }

    /**
     * Setter to set input 'schema' file name
     * REQUIRED to support setting attribute for input file on ANT TASK
     * @param aInput
     */
    public void setInput(File aInput)
    {
        mInputFile = aInput;
    }

    /**
     * Setter to set output 'schema' file name
     * REQUIRED to support setting attribute for output file on ANT TASK
     * @param aOutput
     */
    public void setOutput(File aOutput)
    {
        mOutputFile = aOutput;
    }

    /**
     * Setter to set verbose attribute
     * REQUIRED to support setting attribute for verbose output
     * @param aVerbose
     */
    public void setVerbose(boolean aVerbose)
    {
        mVerbose = aVerbose;
    }

    /**
     * Setter to set 'schema root'
     * REQUIRED to support setting attribute for schema root on ANT TASK
     * @param aRoot
     */
    public void setSchemaRoot(File aRoot)
    {
        mSchemaRoot = aRoot;
    }

    /**
     * Method to verbose the output
     * @param mOutputString
     */
    public void verbose(String mOutputString)
    {
        if (mVerbose) {
            System.out.println(mOutputString);
        }
    }

    public void setOverwrite(boolean b)
    {
      mOverwrite = b;
    }

    /**
     * TASK execution - REQUIRED for ANT TASK
     * @throws BuildException
     */
    public void execute() throws BuildException {

        try {
            // Invoke 'dirlist' task
            this.invokeTask(mDirListTask);

            // Create the factory
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

            // Get the document builder
            mDb = dbf.newDocumentBuilder();

            // Prepare for INLINE
            this.prepareForInline( mInputFile.getPath() );

            if (!mOverwrite && upToDate())
            {
              System.out.println(mOutputFile.getPath()
                  + " is up-to-date, not inlining");
              return;
            }

            // Output document
            Document resultDoc = null;

            // Create src document
            Document srcDoc = mDb.parse( mInputFile );
            Element srcRoot = srcDoc.getDocumentElement();

            // Do we have any includes?
            if ( mIncludeMap.size() == 0 ) {
                this.verbose( "No schemas to inline..." );
                resultDoc = srcDoc;
            }
            else {
                // Inline the documents
                Element resultRoot = srcRoot;

                for( Enumeration<String> e = mIncludeMap.elements();
                     e.hasMoreElements(); ) {

                    // Get the 'include' file name
                    String fileName = e.nextElement();
                    resultRoot = this.inlineSchemas( resultRoot, fileName );
                }

                resultDoc = resultRoot.getOwnerDocument();
            }

            // Clean the 'include' statements
            this.cleanDocument(resultDoc);

            // Save the 'inlined' document
            this.saveXMLDocument(resultDoc);
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new BuildException(e.toString());
        }
    }

    /**
     * Search for a given TAG starting from the input element
     * @param aElementName
     * @param aStartElement
     * @return NodeList
     * @throws Exception
     */
    private NodeList searchForTag( String aElementName,
        Element aStartElement ) throws Exception {

        NodeList resultNodes = null;

        if ( aStartElement != null ) {
            resultNodes = aStartElement.getElementsByTagName( aElementName );
        }
        else {
            throw new BuildException(
                    "Invalid start element while searching for <" +
                    aElementName + ">" );
        }
        return resultNodes;
    }

    /**
     * Method to populate vector with all include files
     * @param aList
     * @return Vector
     * @throws Exception
     */
    private Vector<String> populateIncludeFiles( NodeList aList ) throws Exception {

        if ( aList == null ) {
            this.verbose( "No external include files" );
            return null;
        }

        Vector<String> v = new Vector<String>();
        int length = aList.getLength();
        for( int i=0; i<length; i++ ) {
            // Get the node
            Node node = aList.item(i);

            // Get all the attributes
            NamedNodeMap attributes = node.getAttributes();

            // Get the schemaLocation attribute (first attribute)
            Node schemaLocation = attributes.item(0);
            String locationValue = this.prepareFileName(
                    schemaLocation.getNodeValue() );

            v.add( locationValue );
        }

        return v;
    }

    /**
     * Method to inline schema from 'aFile' on to the given Element.
     * @param aResult
     * @param aFile
     * @return Element
     * @throws Exception
     */
    private Element inlineSchemas(
            Element aResult, String aFile ) throws Exception {

        System.out.println( "Inlining -->> " + aFile );

        // Create the source document
        Document incDoc = mDb.parse( aFile );
        Element incRoot = incDoc.getDocumentElement();

        // Get the result document
        Document resultDoc = aResult.getOwnerDocument();

        // Import all children under root
        NodeList children = incRoot.getChildNodes();
        for( int i=0; i<children.getLength(); i++ ) {
            Node childToImport = resultDoc.importNode(children.item(i), true);
            verbose("including child element: [" + childToImport + "]");
            if (childToImport.getNodeName() != null && childToImport.getNodeName().equals("xsd:import"))
            {
              aResult.insertBefore(childToImport, aResult.getFirstChild());
              // preserve newline
              Node newline = resultDoc.createTextNode("\n");
              aResult.insertBefore(newline, childToImport);
            }
            else
              aResult.appendChild( childToImport );
        }

        return aResult;
    }

    /**
     * Save the given document to the result file
     * @param aDoc
     * @throws Exception
     */
    private void saveXMLDocument( Document aDoc ) throws Exception {

        // open output stream where XML Document will be saved
        FileOutputStream fos = new FileOutputStream(mOutputFile);

        // Use a Transformer for output
        TransformerFactory transformerFactory =
      TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

        DOMSource source = new DOMSource( aDoc );
        StreamResult result = new StreamResult(fos);

        // transform source into result will do save
        transformer.transform(source, result);

        System.out.println( "Result DOC -->> " + mOutputFile );
    }

     /**
      * Method that prepares the schemas for inline. This can support
      * include statements at any level. After this method, the hashtable
      * will contain all the schema file names that are used for
      * include statements.
      *
      * @param String source file
      * @throws Exception
      */
    private void prepareForInline( String aSrcFile ) throws Exception {

        // Create src document
        Document srcDoc = mDb.parse( aSrcFile );
        Element srcRoot = srcDoc.getDocumentElement();

        // Any includes?
        NodeList includeList = this.searchForTag( msINCLUDE, srcRoot );

        // No includes? Nothing to do
        if ( includeList != null && includeList.getLength() > 0 ) {

            // Parse out include file names
            Vector<String> v = this.populateIncludeFiles( includeList );

            // Populate Hashtable with the filenames
            for (String fileName : v) {
                if ( mIncludeMap.get(fileName) == null ) {
                    // Save the filename
                    mIncludeMap.put( fileName, fileName );
                    // Does this include any?
                    prepareForInline( prepareFileName(fileName) );
                }
            }
        }
    }

    /**
     * Method to resolve the file name and path and return the file name
     * @param String - input file name
     * @return String - resolved file name
     * @throws Exception
     */
    private String prepareFileName( String aInFile ) throws Exception {

      this.verbose("prepareFileName: " + aInFile);

      // Include file name paths can be starting with "." (or) ".." -
      // Get the first two chars of the file name
      String filePath = null;

      // Tokenize the infile
      filePath = mDirListTask.tokenize(aInFile, "/\\");

      String absFilePath = mDirList.get(filePath);
      if (absFilePath == null) {
          throw new Exception(aInFile + ": does not exist");
      }
      return absFilePath;
    }

    /**
     * Method to clean out the include statements
     * @param Docuemnt
     * @throws Exception
     */
    private void cleanDocument( Document aDoc ) throws Exception {

      // Get root
      Element root = aDoc.getDocumentElement();

      // Any includes?
      NodeList includeList = this.searchForTag( msINCLUDE, root );

      int length = includeList.getLength();
      for( int i=0; i<length; i++ ) {
          // Get the node
          Node node = includeList.item(0);

          // Remove this node
          root.removeChild( node );
      }
    }

    /**
     * Method to programmatically invoke 'dirlist' task
     * @param aTask
     * @throws Exception
     */
    private void invokeTask(DirListTask aTask) throws Exception {
        aTask.setProject(this.getProject());
        aTask.setDir(mSchemaRoot);
        aTask.execute();
        mDirList = aTask.getDirList();
    }

    /**
     * Check whether the output file is up-to-date versus the input file
     * and imports.
     *
     * @return true if up-to-date
     */
    private boolean upToDate()
    {
      if (!mOutputFile.exists())
        return false;

      long outFileLastMod = mOutputFile.lastModified();
      if (mInputFile.lastModified() > outFileLastMod)
        return false;

      Enumeration<String> keys = mIncludeMap.keys();
      while (keys.hasMoreElements())
      {
        String filename = keys.nextElement();
        File file = new File(filename);
        if (file.lastModified() > outFileLastMod)
          return false;
      }

      return true; // nothing out-of-date
    }
}