package net.adamsmolnik.lambda.setup;

import java.io.File;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.lambda.model.UpdateFunctionCodeRequest;
import com.amazonaws.services.lambda.model.UpdateFunctionCodeResult;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;

public class Deployer {

	private static class Config {

		private static final String JARS_BUCKET = "lambda-jars";

		private String jar, jarSrcDir, functionName;

	}

	private static final AmazonS3 S3 = new AmazonS3Client();

	private static final TransferManager TM = new TransferManager(S3);

	private static final AWSLambda LBD = new AWSLambdaClient();

	public static void main(String[] args) throws Exception {
		try {
			Config collectionConfig = new Config();
			collectionConfig.jar = "photo-collection-handlers.jar";
			collectionConfig.jarSrcDir = "/workspaces/workspace/photo-collection-handlers/target/";
			collectionConfig.functionName = "photo-collection-handler";
			updateAll(collectionConfig);

			collectionConfig.functionName = "photo-zipper";
			updateLambda(collectionConfig);

			collectionConfig.functionName = "s3-data-push-handler";
			updateLambda(collectionConfig);

			/*
			 * Config uploadConfig = new Config(); uploadConfig.jar =
			 * "upload-photo-handlers.jar"; uploadConfig.jarSrcDir =
			 * "/workspaces/workspace/upload-photo-handlers/target/";
			 * uploadConfig.functionName = "upload-photo-handler";
			 * updateAll(uploadConfig);
			 */
		} finally {
			TM.shutdownNow();
		}

	}

	public static void updateAll(Config c) throws Exception {
		Upload result = TM.upload(Config.JARS_BUCKET, c.jar, new File(c.jarSrcDir + c.jar));
		result.waitForUploadResult();
		System.out.println("Upload completed");
		TimeUnit.SECONDS.sleep(2);
		updateLambda(c);
	}

	public static void updateLambda(Config c) throws Exception {
		UpdateFunctionCodeRequest request = new UpdateFunctionCodeRequest().withFunctionName(c.functionName).withS3Bucket(Config.JARS_BUCKET)
				.withS3Key(c.jar);
		UpdateFunctionCodeResult lambdaResult = LBD.updateFunctionCode(request);
		System.out.println("Lambda update completed for " + lambdaResult.getFunctionName());
	}

}
