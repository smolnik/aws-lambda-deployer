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

public class CodepotSetup {

    private static final AmazonS3 s3 = new AmazonS3Client();

    private static final AmazonDynamoDB db = new AmazonDynamoDBClient();

    public static void main(String[] args) {
        int numberOfStudents = 12;

        for (int i = 1; i <= numberOfStudents; i++) {
            String studentId = format(i);
            s3.createBucket(studentId + "-codepot-photos");
            s3.createBucket(studentId + "-upload-photos-ext");
            s3.createBucket(studentId + ".codepot.pl");
            CreateTableRequest ctr = new CreateTableRequest()
                    .withTableName(studentId + "-codepot-photos")
                    .withProvisionedThroughput(new ProvisionedThroughput(1L, 1L))
                    .withAttributeDefinitions(new AttributeDefinition("userId", ScalarAttributeType.S), new AttributeDefinition("photoKey", ScalarAttributeType.S),
                            new AttributeDefinition("photoTakenDate", ScalarAttributeType.S))
                    .withKeySchema(new KeySchemaElement("userId", KeyType.HASH), new KeySchemaElement("photoKey", KeyType.RANGE))
                    .withLocalSecondaryIndexes(
                            new LocalSecondaryIndex().withIndexName("photoTakenDate-index")
                                    .withKeySchema(new KeySchemaElement("userId", KeyType.HASH), new KeySchemaElement("photoTakenDate", KeyType.RANGE))
                                    .withProjection(new Projection().withProjectionType(ProjectionType.ALL)));
            db.createTable(ctr);

        }

    }

    private static String format(int i) {
        return String.format("%03d", i);
    }

}
