package net.adamsmolnik.lambda.setup;

import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.lambda.runtime.Context;

public class LambdaExplorer {

	protected class Logger {

		private final String processId = ManagementFactory.getRuntimeMXBean().getName();

		private final Context context;

		protected Logger(Context context) {
			this.context = context;
		}

		protected void log(String message) {
			doLog("[" + getBaseMessage(message) + "]");
		}

		protected void log(Date then, String message) {
			doLog("[duration since then: " + (new Date().getTime() - then.getTime()) + ", " + getBaseMessage(message) + "]");
		}

		private String getBaseMessage(String message) {
			return "processId: " + processId + ", thread: " + Thread.currentThread() + ", " + context.getAwsRequestId() + ", date: " + new Date()
					+ ", message: " + message;
		}

		private void doLog(String message) {
			context.getLogger().log(message);
		}

	}

	public String handle(Context context) {
		Date then = new Date();
		Logger log = new Logger(context);
		log.log(then, "Logger created");
		ExecutorService es = Executors.newFixedThreadPool(5);
		es.submit(() -> {
			try {
				log.log(then, "Job submitted");
				for (int i = 0; i < 8; i++) {
					TimeUnit.SECONDS.sleep(15);
					log.log(then, "Doing job, attempt no. " + (i + 1));
				}
			} catch (Exception e) {
				log.log(then, "Exception during job " + e.getLocalizedMessage());
			} finally {
				es.shutdownNow();
				log.log(then, "Job done");
			}

		});
		log.log(then, "Going back");
		return "OK";
	}

}
