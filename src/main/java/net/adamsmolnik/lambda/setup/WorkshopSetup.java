package net.adamsmolnik.lambda.setup;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.LocalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.Projection;
import com.amazonaws.services.dynamodbv2.model.ProjectionType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

public class WorkshopSetup {

	private static final AmazonS3 S3 = new AmazonS3Client();

	private static final AmazonDynamoDB DB = new AmazonDynamoDBClient();

	private static final String POLICY = "{\r\n" + "		  \"Version\":\"2012-10-17\",\r\n" + "		  \"Statement\":[\r\n" + "		    {\r\n"
			+ "		      \"Sid\":\"AddPerm\",\r\n" + "		      \"Effect\":\"Allow\",\r\n" + "		      \"Principal\": \"*\",\r\n"
			+ "		      \"Action\":[\"s3:GetObject\"],\r\n" + "		      \"Resource\":[\"arn:aws:s3:::${bucket}/*\"]\r\n" + "		    }\r\n"
			+ "		  ]\r\n" + "		}";

	public static void main(String[] args) {
		int numberOfStudents = 12;
		for (int i = 22; i <= 22; i++) {
			String studentId = format(i);
			S3.createBucket(studentId + "-upload-photos-ext");
			String targetBucket = studentId + "-codepot-photos";
			S3.createBucket(targetBucket);
			S3.setBucketPolicy(targetBucket, POLICY.replace("${bucket}", targetBucket));
			CreateTableRequest ctr = new CreateTableRequest().withTableName(studentId + "-codepot-photos")
					.withProvisionedThroughput(new ProvisionedThroughput(1L, 1L))
					.withAttributeDefinitions(new AttributeDefinition("userId", ScalarAttributeType.S),
							new AttributeDefinition("photoKey", ScalarAttributeType.S),
							new AttributeDefinition("photoTakenDate", ScalarAttributeType.S))
					.withKeySchema(new KeySchemaElement("userId", KeyType.HASH), new KeySchemaElement("photoKey", KeyType.RANGE))
					.withLocalSecondaryIndexes(new LocalSecondaryIndex().withIndexName("photoTakenDate-index")
							.withKeySchema(new KeySchemaElement("userId", KeyType.HASH), new KeySchemaElement("photoTakenDate", KeyType.RANGE))
							.withProjection(new Projection().withProjectionType(ProjectionType.ALL)));
			DB.createTable(ctr);

		}

	}

	static String format(int i) {
		return String.format("%03d", i);
	}

}
