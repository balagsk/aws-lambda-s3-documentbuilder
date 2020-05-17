package com.amb.app.core.qiktools.utils;


import com.amazonaws.util.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.docx4j.Docx4J;
import org.docx4j.Docx4jProperties;
import org.docx4j.TraversalUtil;
import org.docx4j.XmlUtils;
import org.docx4j.dml.wordprocessingDrawing.Inline;
import org.docx4j.finders.RangeFinder;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.AltChunkType;
import org.docx4j.openpackaging.parts.WordprocessingML.AlternativeFormatInputPart;
import org.docx4j.openpackaging.parts.WordprocessingML.BinaryPartAbstractImage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.wml.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Entities;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.SchemaOutputResolver;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

/*
 *
 * Reference:
 * https://www.programcreek.com/java-api-examples/?code=openkm/document-management-system/document-management-system-master/src/main/java/com/openkm/util/MSOUtils.java
 *
 *
 * */
public class Docx4jBuilder {

    public static void replaceHtmlText(InputStream input, HashMap<String, String> model, OutputStream output){

        try {
            WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.load(input);
            MainDocumentPart documentPart = wordMLPackage.getMainDocumentPart();
            System.out.println( "Template wordMLPackage : \n" +documentPart.getXML());
            replaceRichText( wordMLPackage,model );
            System.out.println( "Final wordMLPackage : \n" +documentPart.getXML());
            wordMLPackage.save(output);
        } catch (Docx4JException e) {
            e.printStackTrace();
        } catch (JAXBException e) {
            e.printStackTrace();
        }

    }

    public static void replaceRichText(WordprocessingMLPackage wordMLPackage, Map<String, String> richTextMap) throws Docx4JException, JAXBException {
        MainDocumentPart document = wordMLPackage.getMainDocumentPart();
        Map<String, List<Object>> textNodeMap = new HashMap<String, List<Object>>();
        findRichTextNode(textNodeMap, document.getContents().getBody(), null);
        Iterator<String> iterator = richTextMap.keySet().iterator();
        while (iterator.hasNext()) {
            String textTag = iterator.next();
            List<Object> textNodeList = textNodeMap.get(textTag);
            if (textNodeList != null && richTextMap.containsKey(textTag)) {
                List<Object> textObjList = convertToWmlObject(wordMLPackage, richTextMap.get(textTag));
                for (int i = 0, iSize = textNodeList.size(); i < iSize; i++) {
                    Object nodeObject = textNodeList.get(i);
                    if (nodeObject != null) {
                        //setWmlPprSetting(textNodeList.get(i), textObjList);
                        TraversalUtil.replaceChildren(nodeObject , textObjList);
                    }
                }
            }
        }
    }

    private static void findRichTextNode(Map<String, List<Object>> richTextMap,Object object, ContentAccessor accessor) {
        Object textObj = XmlUtils.unwrap(object);
        if (textObj instanceof Text) {
            String text = ((Text) textObj).getValue();
            if (StringUtils.isNotEmpty(text)) {
                text = text.trim();
                if (text.startsWith("$RH{") && text.endsWith("}")) {
                    String textTag = text.substring("$RH{".length(),
                            text.length() - 1);
                    if (StringUtils.isNotEmpty(textTag) && (accessor != null)) {
                        if (richTextMap.containsKey(textTag)) {
                            richTextMap.get(textTag).add(accessor);
                        } else {
                            List<Object> objList = new ArrayList<Object>();
                            objList.add(accessor);
                            richTextMap.put(textTag, objList);

                        }
                    }
                }
            }
        } else if (object instanceof ContentAccessor) {
            List<Object> objList = ((ContentAccessor) object).getContent();
            for (int i = 0, iSize = objList.size(); i < iSize; i++) {
                findRichTextNode(richTextMap, objList.get(i), (ContentAccessor) object);
            }
        }
    }

    private static List<Object> convertToWmlObject(
            WordprocessingMLPackage wordMLPackage, String content)
            throws Docx4JException, JAXBException {
        MainDocumentPart document = wordMLPackage.getMainDocumentPart();
        String charsetName = Docx4jProperties.getProperty(Docx4jConstants.DOCX4J_CONVERT_OUT_WMLTEMPLATE_CHARSETNAME, Docx4jConstants.DEFAULT_CHARSETNAME );

        List<Object> wmlObjList = null;
        String templateString = XmlUtils.marshaltoString(document.getContents().getBody());
        System.out.println(templateString);
        Body templateBody = document.getContents().getBody();
        try {
            document.getContents().setBody(XmlUtils.deepCopy(templateBody));
            document.getContent().clear();
            org.jsoup.nodes.Document doc = Jsoup.parse(content);
            doc.outputSettings().syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml).escapeMode( Entities.EscapeMode.xhtml);
            //XHTMLImporterImpl xhtmlImporter = new XHTMLImporterImpl(wordMLPackage);

            AlternativeFormatInputPart part = document.addAltChunk( AltChunkType.Xhtml,doc.html().getBytes( Charset.forName(charsetName)));

            WordprocessingMLPackage tempPackage = document.convertAltChunks();
            File file = new File("d://temp.docx");
            tempPackage.save(file);
            wmlObjList = document.getContent();
            //part.getOwningRelationshipPart().getSourceP().get
            //wmlObjList = xhtmlImporter.convert(doc.html(), doc.baseUri());
        } finally {
            document.getContents().setBody(templateBody);
        }
        return wmlObjList;
    }



    public static void replaceText(InputStream input, HashMap<String, String> model, OutputStream output) throws Docx4JException,
            JAXBException, IOException {
        WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.load(input);
        MainDocumentPart documentPart = wordMLPackage.getMainDocumentPart();
        System.out.println( "Template wordMLPackage : \n" +wordMLPackage.getMainDocumentPart().getXML());

        for (final Map.Entry<String, String> entry : model.entrySet()) {
            new TraversalUtil(documentPart, new TraversalUtil.CallbackImpl() {
                @Override
                public List<Object> apply(Object child) {
                    if (child instanceof org.docx4j.wml.Text) {
                        org.docx4j.wml.Text t = (org.docx4j.wml.Text) child;

                        if (t.getValue().contains(entry.getKey())) {
                            t.setValue(t.getValue().replaceAll(entry.getKey(), entry.getValue()));
                        }
                    }

                    return null;
                }
            });
        }

        System.out.println( "Final wordMLPackage : \n" +wordMLPackage.getMainDocumentPart().getXML());
        // Save it
        wordMLPackage.save(output);
    }

    public static void main(String[] args) throws IOException, JAXBException, Docx4JException {
        String resumeTemplate="D:\\01_GitHub_workspace_Bala\\GitHubRepository\\qiktools\\qiktools\\file\\resume_templates\\classic_03.docx";
        String targetPath="D:\\01_GitHub_workspace_Bala\\GitHubRepository\\qiktools\\qiktools\\file\\output\\classic_03_out.docx";
        HashMap<String, String > hashMap=new HashMap<>();
       // hashMap.put( "${docx.name}","BALA" );
        //hashMap.put( "{docx.name}","BALA" );
        hashMap.put( "qiktoolname","Balakrishnan, Selvaraj1" );
        hashMap.put( "qiktool.designation","Software Developer1" );
        hashMap.put( "qiktool.loc","Singapore1" );

        hashMap.put( "qiktooltbldegree","Engineering" );
        hashMap.put( "qiktooltblschool","Jospesh" );
        hashMap.put( "qiktooltblstate","tamil Nadu" );

        InputStream inputStream=new BufferedInputStream( new FileInputStream( resumeTemplate ) );
        OutputStream outputStream=new BufferedOutputStream( new FileOutputStream(targetPath));

        //replaceText(inputStream,hashMap, outputStream );
        replaceHtmlText(inputStream,hashMap,outputStream );


    }


}
