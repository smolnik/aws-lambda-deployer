package net.adamsmolnik.lambda.setup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

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
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.lambda.model.CreateFunctionRequest;
import com.amazonaws.services.lambda.model.FunctionCode;
import com.amazonaws.services.lambda.model.Runtime;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.BucketWebsiteConfiguration;

public class WorkshopSetup {

	private static final AmazonS3 S3 = new AmazonS3Client();

	private static final AmazonDynamoDB DB = new AmazonDynamoDBClient();

	private static final AWSLambda LBD = new AWSLambdaClient();

	private static final String POLICY = "{\r\n" + "		  \"Version\":\"2012-10-17\",\r\n" + "		  \"Statement\":[\r\n" + "		    {\r\n"
			+ "		      \"Sid\":\"AddPerm\",\r\n" + "		      \"Effect\":\"Allow\",\r\n" + "		      \"Principal\": \"*\",\r\n"
			+ "		      \"Action\":[\"s3:GetObject\"],\r\n" + "		      \"Resource\":[\"arn:aws:s3:::${bucket}/*\"]\r\n" + "		    }\r\n"
			+ "		  ]\r\n" + "		}";

	private static void dbSetup(String studentId) {
		CreateTableRequest ctr = new CreateTableRequest().withTableName(studentId + "-codepot-photos")
				.withProvisionedThroughput(new ProvisionedThroughput(1L, 1L))
				.withAttributeDefinitions(new AttributeDefinition("userId", ScalarAttributeType.S),
						new AttributeDefinition("photoKey", ScalarAttributeType.S), new AttributeDefinition("photoTakenDate", ScalarAttributeType.S))
				.withKeySchema(new KeySchemaElement("userId", KeyType.HASH), new KeySchemaElement("photoKey", KeyType.RANGE))
				.withLocalSecondaryIndexes(new LocalSecondaryIndex().withIndexName("photoTakenDate-index")
						.withKeySchema(new KeySchemaElement("userId", KeyType.HASH), new KeySchemaElement("photoTakenDate", KeyType.RANGE))
						.withProjection(new Projection().withProjectionType(ProjectionType.ALL)));
		DB.createTable(ctr);
	}

	private static void s3Setup(String studentId) {
		S3.createBucket(studentId + "-upload-photos-ext");
		String targetBucket = studentId + "-codepot-photos";
		S3.createBucket(targetBucket);
		S3.setBucketPolicy(targetBucket, POLICY.replace("${bucket}", targetBucket));
		s3WebSetup(targetBucket);
	}

	private static void s3WebSetup(String bucketName) {
		S3.setBucketWebsiteConfiguration(bucketName, new BucketWebsiteConfiguration("index.html"));
		loadHtmlContent(bucketName);
	}

	private static void loadHtmlContent(String bucketName) {
		try {
			Files.walk(Paths.get("./src/main/resources/public_html")).filter(Files::isRegularFile).forEach(p -> {
				String objectKey = p.subpath(5, p.getNameCount()).toString().replaceAll("\\\\", "/");
				S3.putObject(bucketName, objectKey, p.toFile());
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	static String format(int i) {
		return String.format("%03d", i);
	}

	public static void main1(String[] args) throws Exception {
		int numberOfStudents = 12;
		for (int i = 0; i <= 0; i++) {
			String studentId = format(i);
			s3Setup(studentId);
			dbSetup(studentId);
		}
	}

	public static void main(String[] args) throws Exception {
		instalLambda(format(1));
	}

	private static void fallback() throws IOException {
		String photoCollectionHandlersDir = "/workspaces/workspace/exercise-photo-collection-handlers/target";
		checkAndPut(photoCollectionHandlersDir);
		String uploadPotoHandlers = "/workspaces/workspace/exercise-upload-photo-handlers/target";
		checkAndPut(uploadPotoHandlers);
	}

	private static void checkAndPut(String dir) throws IOException {
		Files.newDirectoryStream(Paths.get(dir)).forEach(p -> {
			if (!Files.isRegularFile(p) || !p.getFileName().toString().startsWith("0")) {
				return;
			}
			new Thread(() -> {
				S3.putObject("lambda-jars", "fallback/" + p.getFileName(), p.toFile());
			}).start();
		});
	}

	public static void instalLambda(String s) throws Exception {
		LBD.createFunction(new CreateFunctionRequest()
				.withCode(new FunctionCode().withS3Bucket("lambda-jars").withS3Key("fallback/" + s + "-photo-collection-handlers.jar"))
				.withFunctionName(s + "-photo-collection-handler").withMemorySize(256)
				.withHandler("net.adamsmolnik.handler.PhotoCollectionHandler::handle").withRole("arn:aws:iam::542175458111:role/lambda_exec_role")
				.withTimeout(30).withRuntime(Runtime.Java8));
	}

}
