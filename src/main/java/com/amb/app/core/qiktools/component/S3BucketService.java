package com.amb.app.core.qiktools.component;


import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import org.springframework.stereotype.Component;

import java.io.*;

@Component
public class S3BucketService {
    public static Regions clientRegion = Regions.AP_SOUTHEAST_1;
    public static String bucketName = "qiktool-s3"; //arn:aws:s3:::qiktool-s3
    public static String key = "static/template/classic_03.docx";

    //upload
    String stringObjKeyName = "*** String object key name ***";
    String fileObjKeyName = "static/output/classic_03_out.docx";
    String fileName = "static/output/classic_03_out.docx";

    private S3Object fullObject = null;
    private S3Object objectPortion = null;
    private S3Object headerOverrideObject = null;

    public AmazonS3 S3BucketConnector(String region, String accessKey, String secretKey) throws IOException{
        System.out.println("\nConnecting S3 Bucket :  S3BucketName :"+bucketName+",ObjectKeyName :"+key);
        AmazonS3 s3Client=null;
        try {
            AWSCredentials credentials = new BasicAWSCredentials(
                    "AKIAY2WMWS7J7XOSSRFR",
                    "Q7EvyhOfUe/4i+gaOEOYPsI3EHNf2S2c1rpjEkPH"
            );

            s3Client = AmazonS3ClientBuilder.standard()
                    .withRegion(clientRegion)
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .build();
            System.out.println("S3 bucket connected.");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return s3Client;
    }

    public InputStream getS3Object(String S3BucketName, String ObjectKeyName) throws IOException {
        System.out.println("\nConnecting S3 Bucket :  S3BucketName :"+bucketName+",ObjectKeyName :"+key);

    try {
        AmazonS3 amazonS3Connector=S3BucketConnector("some","dummy","data"  );
        // Get an object and print its contents.
        System.out.println("Downloading an object");
        fullObject = amazonS3Connector.getObject(new GetObjectRequest(bucketName, key));
        System.out.println("Content-Type: " + fullObject.getObjectMetadata().getContentType());
        System.out.println("Content: ");
       // displayTextInputStream(fullObject.getObjectContent());

        // Get a range of bytes from an object and print the bytes.
        GetObjectRequest rangeObjectRequest = new GetObjectRequest(bucketName, key)
                .withRange(0, 9);
        objectPortion = amazonS3Connector.getObject(rangeObjectRequest);
        System.out.println("Printing bytes retrieved.");
        //displayTextInputStream(objectPortion.getObjectContent());

        // Get an entire object, overriding the specified response headers, and print the object's content.
        ResponseHeaderOverrides headerOverrides = new ResponseHeaderOverrides()
                .withCacheControl("No-cache")
                .withContentDisposition("attachment; filename=example.txt");
        GetObjectRequest getObjectRequestHeaderOverride = new GetObjectRequest(bucketName, key)
                .withResponseHeaders(headerOverrides);
        headerOverrideObject = amazonS3Connector.getObject(getObjectRequestHeaderOverride);
        //displayTextInputStream(headerOverrideObject.getObjectContent());
    } catch (SdkClientException e) {
        // The call was transmitted successfully, but Amazon S3 couldn't process
        // it, so it returned an error response.
        e.printStackTrace();
    }// Amazon S3 couldn't be contacted for a response, or the client
// couldn't parse the response from Amazon S3.
    finally {
        // To ensure that the network connection doesn't remain open, close any open input streams.
        if (fullObject != null) {
            fullObject.close();
        }
        if (objectPortion != null) {
            objectPortion.close();
        }
    }
    return headerOverrideObject.getObjectContent();
}

    public void displayTextInputStream(S3ObjectInputStream input) throws IOException {
        // Read the text input stream one line at a time and display each line.
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        String line = null;
        while ((line = reader.readLine()) != null) {
            System.out.println("Printing lines : "+line);
        }
        System.out.println("Display -Done.");
    }


    public void uploadS3Object(String S3BucketName, String ObjectKeyName, File file ) throws IOException {

        AmazonS3 amazonS3Connector=S3BucketConnector("some","dummy","data"  );
        PutObjectRequest request = new PutObjectRequest(bucketName, fileObjKeyName, file);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("plain/text");
        metadata.addUserMetadata("title", "someTitle");
        request.setMetadata(metadata);
        PutObjectResult putObjectResult=amazonS3Connector.putObject(request);
    }

}
