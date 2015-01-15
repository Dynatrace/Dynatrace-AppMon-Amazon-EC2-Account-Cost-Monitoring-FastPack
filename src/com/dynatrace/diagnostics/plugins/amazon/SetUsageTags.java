/***************************************************
 * dynaTrace Diagnostics (c) dynaTrace software GmbH
 *
 * @file: SetUsageTags.java
 * @date: 21.03.2013
 * @author: cwat-dstadler
 */
package com.dynatrace.diagnostics.plugins.amazon;

import static com.dynatrace.diagnostics.plugins.amazon.AmazonAccountMonitor.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;


/**
 * Small helper application which walks through all instances and checks if they have an "Usage" Tag which
 * is used for monitoring later.
 *
 * Based on some heuristics it tries to determine and set the tag for instances that do not have it set.
 *
 * A list of not-tagged instances is print at the end for further investigation.
 *
 * @author cwat-dstadler
 */
public class SetUsageTags {
	private static final Logger log = Logger.getLogger(SetUsageTags.class.getName());

	private ClientConfiguration clientConfig = new ClientConfiguration();

	/**
	 *
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
        new SetUsageTags().run();
	}

	private void run() throws IOException {
		AmazonUtils.init(clientConfig);

		Map<String, Integer> countPerAccount = new HashMap<String, Integer>();
		for(String name : AmazonUtils.getAccountNames()) {
			countPerAccount.put(name, runForAWSAccount(name));
		}

		log.info("Had missing tag counts: " + countPerAccount);
	}

	private int runForAWSAccount(String name) throws IOException {
		log.info("Start setting tags on Amazon Dev EC2 instances for account " + name);

		int count = 0, missedCount = 0, missedRunningCount = 0;
		StringBuilder missed = new StringBuilder();
		StringBuilder missedRunning = new StringBuilder();
		final Map<String, String> endPoints = AmazonUtils.getEndpoints(clientConfig, name);
		for(Map.Entry<String,String> entry : endPoints.entrySet()) {
			AmazonEC2Client client = new AmazonEC2Client(AmazonUtils.getAwsCredentials(name), clientConfig);
			client.setEndpoint(entry.getValue());
			DescribeInstancesRequest instanceRequest = new DescribeInstancesRequest();
			List<Reservation> reservations = client.describeInstances(instanceRequest).getReservations();

			for(Reservation reservation : reservations) {
				//allReservations.put(reservation, entry.getKey());

				for(Instance instance : reservation.getInstances()) {
					if(!AmazonUtils.getInstanceTag(instance, TAG_USAGE).equals(TAG_UNKNOWN)) {
						continue;
					}

					// check to avoid stopping with an error when trying to set a tag
					if(instance.getTags().size() >= 10) {
						log.warning("Cannot set Usage tag for instance as the maximum number of 10 tags is already set, instance: " + AmazonUtils.getInstanceDescription(instance));
						continue;
					}

					String guardian = AmazonUtils.getInstanceTag(instance, "Client.Guardian");
					//String stack = getInstanceTag(instance, "aws:cloudformation:stack-id");
					// for now we put all untagged instances with "GDN-key" into UEMaaS as this is what most people in Gdansk work on
					if(!guardian.equals(TAG_UNKNOWN) /*|| !stack.equals(TAG_UNKNOWN)*/ ||
							(instance.getKeyName() != null && instance.getKeyName().equals("GDN-key"))) {
						AmazonUtils.setUsageTag(client, instance, "UEMaaS");
						count++;

						continue;
					}

					if(AmazonUtils.getInstanceTag(instance, "Name").contains("CoE") ||
							(instance.getKeyName() != null && instance.getKeyName().equals("coe-demo"))) {
						// if it looks like an instance from Center of Excellence, tag it accordingly
						AmazonUtils.setUsageTag(client, instance, "CoE");
						count++;

						continue;
					}

					if(!AmazonUtils.getInstanceTag(instance, "DemoId").equals(TAG_UNKNOWN)) {
						// if "DemoId" is set we tag it as "CloudDemo"
						AmazonUtils.setUsageTag(client, instance, "CloudDemo");
						count++;

						continue;
					}

					if(instance.getKeyName() != null && instance.getKeyName().equals("EasyTravelLargeDeployment")) {
						// use "easyTravelNG" for deployment tests
						AmazonUtils.setUsageTag(client, instance, "easyTravelNG");
						count++;

						continue;
					}

					if(!AmazonUtils.getInstanceTag(instance, "aws:elasticmapreduce:instance-group-role").equals(TAG_UNKNOWN)) {
						// if mapreduce related, set it as "MapReduce"
						AmazonUtils.setUsageTag(client, instance, "MapReduce");
						count++;

						continue;
					}

					if(AmazonUtils.getInstanceTag(instance, "Name").toLowerCase().contains("puppet")) {
						// if mapreduce related, set it as "MapReduce"
						AmazonUtils.setUsageTag(client, instance, "Puppet");
						count++;

						continue;
					}

					if(AmazonUtils.getInstanceTag(instance, "Name").toLowerCase().contains("cloudera")) {
						// if mapreduce related, set it as "MapReduce"
						AmazonUtils.setUsageTag(client, instance, "Cloudera");
						count++;

						continue;
					}


					missed.append(AmazonUtils.getInstanceDescription(instance)).append("\n");
					missedCount++;
					if(instance.getState().getName().equals(EC2_STATE_RUNNING) ||
							instance.getState().getName().equals(EC2_STATE_PENDING)) {
						missedRunning.append(AmazonUtils.getInstanceDescription(instance)).append("\n");
						missedRunningCount++;
					}
				}
			}
		}

		log.warning("The following " + missedCount + " instances could not be tagged, some of them might not be running, see next log: \n" + missed.toString());
		log.warning("The following " + missedRunningCount + " running instances could not be tagged: \n" + missedRunning.toString());
		log.info("Tagged " + count + " instances, " + missedRunningCount + " untagged instances are running, " + missedCount + " overall untagged instances");

		return missedRunningCount;
	}
}
