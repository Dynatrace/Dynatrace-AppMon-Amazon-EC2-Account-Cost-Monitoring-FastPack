/***************************************************
 * dynaTrace Diagnostics (c) dynaTrace software GmbH
 *
 * @file: SetUsageTags.java
 * @date: 21.03.2013
 * @author: cwat-dstadler
 */
package com.dynatrace.diagnostics.plugins.amazon;

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
 * Small helper application which lists all instances for the account specified in the properties.
 *
 * @author cwat-dstadler
 */
public class ListInstances {
	private static final Logger log = Logger.getLogger(ListInstances.class.getName());

	private ClientConfiguration clientConfig = new ClientConfiguration();

	public static void main(String[] args) throws IOException {
        new ListInstances().run();
	}

	private void run() throws IOException {
		AmazonUtils.init(clientConfig);

		Map<String, Integer> countPerAccount = new HashMap<String, Integer>();
		for(String name : AmazonUtils.getAccountNames()) {
			countPerAccount.put(name, runForAWSAccount(name));
		}
		log.info("Had instance-counts for acocunts: " + countPerAccount);
	}

	private int runForAWSAccount(String name) throws IOException {
		log.info("Start listing instances on Amazon Dev EC2 instances for account " + name);

		int count = 0;
		final Map<String, String> endPoints = AmazonUtils.getEndpoints(clientConfig, name);
		for(Map.Entry<String,String> entry : endPoints.entrySet()) {
			AmazonEC2Client client = new AmazonEC2Client(AmazonUtils.getAwsCredentials(name), clientConfig);
			client.setEndpoint(entry.getValue());
			DescribeInstancesRequest instanceRequest = new DescribeInstancesRequest();
			List<Reservation> reservations = client.describeInstances(instanceRequest).getReservations();

			for(Reservation reservation : reservations) {
				for(Instance instance : reservation.getInstances()) {
					log.info("Instance: " + AmazonUtils.getInstanceDescription(instance));
					count++;
				}
			}
		}
		log.info("Found " + count + " instances for account " + name);

		return count;
	}
}
