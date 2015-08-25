package net.adamsmolnik.lambda.setup;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.lambda.model.DeleteFunctionRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.VersionListing;

public class WorkshopCleaner {
	
    private static final AmazonS3 S3 = new AmazonS3Client();

    private static final AmazonDynamoDB DB = new AmazonDynamoDBClient();
    
    private static final AWSLambda LBD = new AWSLambdaClient();
    
	public static void main(String[] args) {
		List<String> students = new ArrayList<>();
        for (int i = 24; i <=24 ; i++) {
        	students.add(WorkshopSetup.format(i));
		}

		
        S3.listBuckets().stream().filter(b -> b.getName().startsWith("0")).forEach(b -> {
            VersionListing vl = S3.listVersions(b.getName(), "");
            vl.getVersionSummaries().forEach(v -> {
                S3.deleteVersion(b.getName(), v.getKey(), v.getVersionId());
            });

            ObjectListing ol = S3.listObjects(b.getName());
            ol.getObjectSummaries().forEach(s -> {
                S3.deleteObject(b.getName(), s.getKey());
            });
            S3.deleteBucket(b.getName());
        });
        
        students.forEach(s->{
            DB.deleteTable(s + "-codepot-photos");
            LBD.deleteFunction(new DeleteFunctionRequest().withFunctionName(s + "-photo-collection-handler"));
            LBD.deleteFunction(new DeleteFunctionRequest().withFunctionName(s + "-upload-photo-handler"));
        });
        
	}

}
