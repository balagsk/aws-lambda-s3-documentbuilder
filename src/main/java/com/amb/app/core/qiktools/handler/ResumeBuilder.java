package com.amb.app.core.qiktools.handler;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amb.app.core.qiktools.component.S3BucketService;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.io.*;
import java.util.HashMap;
import java.util.Map;


public class ResumeBuilder implements RequestHandler<Object, Object> {


    @Override
    public Object handleRequest(Object input, Context context) {
        context.getLogger().log( "contextRequestId:" +context.getAwsRequestId());
        context.getLogger().log( "input:" +input);
        StaxHandler staxHandler=new StaxHandler();
        InputStream inputStream=null;
        try {
            S3BucketService s3BucketService=new S3BucketService();
            //Get template
            inputStream =s3BucketService.getS3Object( "S3","Object");
            //Process template with Docx
            File updateFile= staxHandler.processDocxFile(inputStream,null,"data1","data2","data3", true);
            //Upload Docx file
            s3BucketService.uploadS3Object( "dummy","name",updateFile );
        } catch (IOException | Docx4JException | JAXBException | XMLStreamException e) {
            e.printStackTrace();
        } finally {
           if(inputStream!=null) {
               try {
                   inputStream.close();
               } catch (IOException e) {
                   e.printStackTrace();
               }
           }
        }

        return input;
    }



}
