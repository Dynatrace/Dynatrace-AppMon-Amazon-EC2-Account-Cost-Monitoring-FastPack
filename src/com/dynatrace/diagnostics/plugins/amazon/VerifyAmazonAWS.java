/***************************************************
 * dynaTrace Diagnostics (c) dynaTrace software GmbH
 *
 * @file: VerifyAmazonAWS.java
 * @date: 02.07.2013
 * @author: cwat-dstadler
 */
package com.dynatrace.diagnostics.plugins.amazon;

import java.io.IOException;
import java.util.List;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;


/**
 * Small test-application which performs a small request to Amazon to verify that we can talk to AWS correctly.
 *
 * @author cwat-dstadler
 */
public class VerifyAmazonAWS {
	public static void testAWS() throws IOException {
		//System.setProperty("https.protocols", "TLSv1");
		//System.setProperty("sun.security.ssl.allowLegacyHelloMessages", "false");

		System.out.println("Starting AWS Request-" + System.getProperty("https.protocols") + "-" + System.getProperty("sun.security.ssl.allowLegacyHelloMessages"));

		ClientConfiguration clientConfig = new ClientConfiguration();

		// walk all the different regions that were specified
		{
			String endPoint = "cloudformation.sa-east-1.amazonaws.com";
			AmazonCloudFormation stackbuilder = new AmazonCloudFormationClient(AmazonUtils.getAwsCredentials("dtdev"), clientConfig);
			stackbuilder.setEndpoint(endPoint);

			DescribeStacksRequest stackRequest = new DescribeStacksRequest();
			try {
				List<Stack> stacks = stackbuilder.describeStacks(stackRequest).getStacks();
				System.out.println("Had " + stacks.size() + " stacks");
			} catch (Throwable t) {
				System.out.println("Throwable: " + t);
				t.printStackTrace();
				return;
			}
		}

		{
			String endPoint = "ec2.eu-west-1.amazonaws.com";
			System.out.println("Retrieving ec2 instances for endpoint: " + endPoint);

			AmazonEC2Client client = new AmazonEC2Client(AmazonUtils.getAwsCredentials("dtdev"), clientConfig);
			client.setEndpoint(endPoint);
			DescribeInstancesRequest instanceRequest = new DescribeInstancesRequest();
			try {
				List<Reservation> reservations = client.describeInstances(instanceRequest).getReservations();
				for(Reservation reservation : reservations) {
					for(Instance instance : reservation.getInstances()) {
						String status = instance.getState().getName();
							System.out.println("Having Instance: " + instance.getInstanceId() + ", state: " + status + ", public ip:  " + instance.getPublicDnsName());
					}
				}
			} catch (Throwable t) {
				System.out.println("Throwable: " + t);
				t.printStackTrace();
				return;
			}
		}
	}

	public static void main(String[] args) throws Exception {
		testAWS();
	}
}
