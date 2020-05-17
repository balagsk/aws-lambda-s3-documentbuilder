package com.amb.app.core.qiktools.handler;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.util.IOUtils;
import com.amb.app.core.qiktools.component.S3BucketService;
import com.google.common.base.Strings;
import org.docx4j.Docx4J;
import org.docx4j.XmlUtils;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.StAXHandlerAbstract;
import org.docx4j.openpackaging.parts.StAXHandlerInterface;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class StaxHandler extends StAXHandlerAbstract {
    public static HashMap<String, String> mappings = new HashMap<String, String>();

    public StringBuilder replace(String wmlTemplateString, int offset, StringBuilder strB,
                                  java.util.Map<String, ?> mappings) {
        System.out.println("wmlTemplateString : \n" +wmlTemplateString);

        int startKey = wmlTemplateString.indexOf("${", offset);
        if (startKey == -1)
            return strB.append(wmlTemplateString.substring(offset));
        else {
            System.out.println("startKey -int:" +startKey);
            strB.append(wmlTemplateString.substring(offset, startKey));
            int keyEnd = wmlTemplateString.indexOf( "}", startKey);
            System.out.println("keyEnd -int:" +keyEnd);
            if (keyEnd>0) {
                String key = wmlTemplateString.substring(startKey + 2, keyEnd);
                Object val = mappings.get(key);
                if (val==null) {
                    System.out.println("Invalid key '" + key + "' or key not mapped to a value");
                    strB.append(key );
                } else {
                    strB.append(val.toString()  );
                }
                return replace(wmlTemplateString, keyEnd + 1, strB, mappings);
            } else {
                System.out.println("Invalid key: could not find '}' ");
                strB.append("$");
                System.out.println("Invalid key: could not find '}' ");
                return replace(wmlTemplateString, offset + 1, strB, mappings);
            }
        }
    }

    public WordprocessingMLPackage getDocumentTemplate(String inputfilepath, InputStream inputfile) throws Docx4JException {
        WordprocessingMLPackage wordMLPackage;
        if(inputfilepath!=null) {
            wordMLPackage = WordprocessingMLPackage.load(new java.io.File(inputfilepath));
        }else {
            wordMLPackage = WordprocessingMLPackage.load(inputfile);
        }
        return wordMLPackage;
    }

    public File processDocxFile(InputStream template, OutputStream outputStream, String param1, String param2, String param3, boolean uploadToS3Bucket) throws Docx4JException, JAXBException, XMLStreamException, IOException {
        final File generated = File.createTempFile("template_temp", ".docx");
        boolean processDocxFileStatus=false;
        String outputfilepath="class03_out.docx";
        System.out.println("[Print params] template :"+template+",outputfilepath:"+outputfilepath+",param1 : "+param1+",param2 :"+param2+",param3 :"+param3);
        WordprocessingMLPackage wordMLPackage=getDocumentTemplate(null, template);
        MainDocumentPart documentPart = wordMLPackage.getMainDocumentPart();
        mappings.put("qiktoolcolour", "green");
        mappings.put("qiktooltbldegree", "engineering");
        mappings.put("colour", "red");
        mappings.put("icecream", "chocolate");
        documentPart.pipe(new StaxHandler() );
        if (uploadToS3Bucket) {
            if (true) {
                Docx4J.save( wordMLPackage, generated );
                processDocxFileStatus = true;
            } else {
                System.out.println( XmlUtils.marshaltoString( documentPart.getJaxbElement(), true,
                        true ) );
            }
        } else {
            if (true) {
                Docx4J.save( wordMLPackage, new File( outputfilepath ) );
                processDocxFileStatus = true;
            } else {
                System.out.println( XmlUtils.marshaltoString( documentPart.getJaxbElement(), true,
                        true ) );
            }
        }
        return generated;
    }

    public void handleCharacters(XMLStreamReader xmlr, XMLStreamWriter writer) throws XMLStreamException {
        StringBuilder sb = new StringBuilder();
        sb.append(xmlr.getTextCharacters(), xmlr.getTextStart(), xmlr.getTextLength());

        String wmlString = replace(sb.toString(), 0, new StringBuilder(), mappings).toString();
//			System.out.println(wmlString);

        char[] charOut = wmlString.toCharArray();
        writer.writeCharacters(charOut, 0, charOut.length);

//			writer.writeCharacters(xmlr.getTextCharacters(),
//					xmlr.getTextStart(), xmlr.getTextLength());

    }


    public static void main(String[] args) throws Docx4JException, JAXBException, XMLStreamException {

        // Input docx has variables in it: ${colour}, ${icecream}
        //String inputfilepath = "D:\\01_GitHub_workspace_Bala\\GitHubRepository\\qiktools\\qiktools\\file\\resume_templates\\classic_03.docx";
        // String outputfilepath = System.getProperty("user.dir") + "/OUT_VariableReplaceStAX.docx";
        StaxHandler staxHandler = new StaxHandler();
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            S3BucketService s3BucketService = new S3BucketService();
            inputStream = s3BucketService.getS3Object( "S3", "Object");
            File file=staxHandler.processDocxFile( inputStream, outputStream,"data1", "data2", "data3", true );
            s3BucketService.uploadS3Object( "S3","Object",file );
            if(file!=null){
                System.out.println( "File uploaded successfully." );
            }else{
                System.out.println( "File upload failed." );
            }
        } catch (IOException | Docx4JException | JAXBException | XMLStreamException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


}
