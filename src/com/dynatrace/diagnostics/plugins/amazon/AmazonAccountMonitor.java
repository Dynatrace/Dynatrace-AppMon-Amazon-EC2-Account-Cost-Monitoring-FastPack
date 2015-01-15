/***************************************************
 * dynaTrace Diagnostics (c) dynaTrace software GmbH
 *
 * @file: AmazonAccountMonitor.java
 * @date: 28.06.2011
 * @author: dominik.stadler
 */
package com.dynatrace.diagnostics.plugins.amazon;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeRegionsRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.Region;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.dynatrace.diagnostics.pdk.Monitor;
import com.dynatrace.diagnostics.pdk.MonitorEnvironment;
import com.dynatrace.diagnostics.pdk.MonitorMeasure;
import com.dynatrace.diagnostics.pdk.Status;


/**
 * A Monitor which polls an Amazon AWS Account and retrieves the following information
 * * number of cloud formations in each state and overall "active"
 * * number of ec2 instances in each state and overall "active"
 * * estimated costs of the currently running instances since last call (0 on first invocation!)
 *
 * @author dominik.stadler
 */
public class AmazonAccountMonitor implements Monitor {
	private static final Logger log = Logger.getLogger(AmazonAccountMonitor.class.getName());

	/**
	 * One hour in milliseconds
	 */
	private static final double HOUR_IN_MS = 60*60*1000;

	/************************************** Config properties **************************/
	protected static final String ENV_CONFIG_ACCESS_KEY = "accessKeyID";
	protected static final String ENV_CONFIG_SECRET_KEY = "secretAccessKey";
	protected static final String ENV_CONFIG_REGION = "region";
	protected static final String ENV_CONFIG_COST_PROPERTIES = "costProperties";
	protected static final String ENV_CONFIG_TEMP_FOLDER = "tempFolder";
	protected static final String ENV_CONFIG_UNIQUE_ID = "uniqueId";

	protected static final String ENV_CONFIG_PROXY_ENABLED = "proxyEnabled";
	protected static final String ENV_CONFIG_PROXY_HOST = "proxyHost";
	protected static final String ENV_CONFIG_PROXY_PORT = "proxyPort";
	protected static final String ENV_CONFIG_PROXY_USERNAME = "proxyUsername";
	protected static final String ENV_CONFIG_PROXY_PASSWORD = "proxyPassword";
	protected static final String ENV_CONFIG_PROXY_DOMAIN = "proxyDomain";
	protected static final String ENV_CONFIG_PROXY_WORKSTATION = "proxyWorkstation";

	/************************************** Metric Groups **************************/
	protected static final String METRIC_GROUP_CLOUD_FORMATION = "Amazon Cloud Formation";
	protected static final String METRIC_GROUP_INSTANCE = "Amazon EC2 Instance";
	protected static final String METRIC_GROUP_INSTANCE_COST = "Amazon EC2 Instance Cost";
	protected static final String METRIC_GROUP_RDS_INSTANCE = "Amazon RDS Instance";
	protected static final String METRIC_GROUP_RDS_INSTANCE_COST = "Amazon RDS Instance Cost";

	/************************************** Measures **************************/
	protected static final String MSR_CLOUD_FORMATIONS = "ActiveCount";
	protected static final Set<String> MSR_CLOUD_FORMATIONS_EXCLUDE = new HashSet<String>();

	static {
		// exclude all that are not an active state, i.e. FAILED and some COMPLETE
		MSR_CLOUD_FORMATIONS_EXCLUDE.add("CREATE_FAILED");
		MSR_CLOUD_FORMATIONS_EXCLUDE.add("ROLLBACK_FAILED");
		MSR_CLOUD_FORMATIONS_EXCLUDE.add("ROLLBACK_COMPLETE");
		MSR_CLOUD_FORMATIONS_EXCLUDE.add("DELETE_FAILED");
	}

    protected static final String MSR_CLOUD_FORMATIONS_CREATE_IN_PROGRESS = "Count_" + StackStatus.CREATE_IN_PROGRESS.name();
    protected static final String MSR_CLOUD_FORMATIONS_CREATE_FAILED = "Count_" + StackStatus.CREATE_FAILED.name();
    protected static final String MSR_CLOUD_FORMATIONS_CREATE_COMPLETE = "Count_" + StackStatus.CREATE_COMPLETE.name();
    protected static final String MSR_CLOUD_FORMATIONS_ROLLBACK_IN_PROGRESS = "Count_" + StackStatus.ROLLBACK_IN_PROGRESS.name();
    protected static final String MSR_CLOUD_FORMATIONS_ROLLBACK_FAILED = "Count_" + StackStatus.ROLLBACK_FAILED.name();
    protected static final String MSR_CLOUD_FORMATIONS_ROLLBACK_COMPLETE = "Count_" + StackStatus.ROLLBACK_COMPLETE.name();
    protected static final String MSR_CLOUD_FORMATIONS_DELETE_IN_PROGRESS = "Count_" + StackStatus.DELETE_IN_PROGRESS.name();
    protected static final String MSR_CLOUD_FORMATIONS_DELETE_FAILED = "Count_" + StackStatus.DELETE_FAILED.name();

	protected static final String MSR_EC2_INSTANCES = "EC2ActiveCount";

	// Measures for the various EC2 instance states
	protected static final String MSR_EC2_INSTANCES_STOPPED = "EC2CountStopped";
	protected static final String MSR_EC2_INSTANCES_PENDING = "EC2CountPending";
	protected static final String MSR_EC2_INSTANCES_RUNNING = "EC2CountRunning";
	protected static final String MSR_EC2_INSTANCES_SHUTTING_DOWN = "EC2CountShutting-down";
	protected static final String MSR_EC2_INSTANCES_TERMINATED = "EC2CountTerminated";

	// the corresponding states reported by Amazon APIs, unfortunately there is no enum in the API
	protected static final String EC2_STATE_STOPPED = "stopped";
	protected static final String EC2_STATE_PENDING = "pending";
	protected static final String EC2_STATE_RUNNING = "running";
	protected static final String EC2_STATE_SHUTTING_DOWN = "shutting-down";
	protected static final String EC2_STATE_TERMINATED = "terminated";

	protected static final String MSR_EC2_INSTANCE_COST = "CostOverall";

	protected static final String MSR_RDS_INSTANCES = "RDSActiveCount";
	protected static final String MSR_RDS_INSTANCES_BY_CLASS = "RDSCountByClass";

	protected static final String MSR_RDS_INSTANCE_COST = "RDSCostOverall";
	protected static final String MSR_RDS_INSTANCE_COST_BY_CLASS = "RDSCostByClass";

	// TODO: should we use InstanceType directly here?
	// see InstanceType for current list
	protected static final String[][] MSR_EC2_INSTANCE_TYPES = {
		// Standard On-Demand Instances
		{"CostM1Small", "m1.small"},
		{"CostM1Medium", "m1.medium"},
		{"CostM1Large", "m1.large"},
		{"CostM1XLarge", "m1.xlarge"},

		// Second Generation Standard On-Demand Instances
		{"CostM3Medium", "m3.medium"},
		{"CostM3Large", "m3.large"},
		{"CostM3XLarge", "m3.xlarge"},
		{"CostM32XLarge", "m3.2xlarge"},

		// Micro On-Demand Instances
		{"CostT1Micro", "t1.micro"},

		// High-Memory On-Demand Instances
		{"CostM2XLarge", "m2.xlarge"},
		{"CostM22XLarge", "m2.2xlarge"},
		{"CostM24XLarge", "m2.4xlarge"},
		{"CostCR18XLarge", "cr1.8xlarge"},

		// Compute Optimized - Previous Generation
		{"CostC1Medium", "c1.medium"},
		{"CostC1XLarge", "c1.xlarge"},

		// Compute Optimized - Current Generation
		{"CostC3Large", "c3.large"},
		{"CostC3XLarge", "c3.xlarge"},
		{"CostC32XLarge", "c3.2xlarge"},
		{"CostC34XLarge", "c3.4xlarge"},
		{"CostC38XLarge", "c3.8xlarge"},

		// GPU Instances - Current Generation
		{"CostG22XLarge", "g2.2xlarge"},

		// Cluster Compute Instances
		{"CostCC14XLarge", "cc1.4xlarge"},
		{"CostCC28XLarge", "cc2.8xlarge"},

		// GPU Instances - Previous Generation
		{"CostCG14XLarge", "cg1.4xlarge"},

		// Storage Optimized - Current Generation
		{"CostI2XLarge", "i2.xlarge"},
		{"CostI22XLarge", "i2.2xlarge"},
		{"CostI24XLarge", "i2.4xlarge"},
		{"CostI28XLarge", "i2.8xlarge"},
		{"CostHS18XLarge", "hs1.8xlarge"},

		// Storage Optimized - Previous Generation
		{"CostHI14XLarge", "hi1.4xlarge"},
	};

	// see https://github.com/garnaat/missingcloud/blob/master/aws.json and http://aws.amazon.com/articles/3912?_encoding=UTF8&jiveRedirect=1
	private static Map<String,String> CLOUD_FORMATION_ENDPOINTS = new HashMap<String, String>();
	static {
		CLOUD_FORMATION_ENDPOINTS.put("us-east-1", "cloudformation.us-east-1.amazonaws.com");
		CLOUD_FORMATION_ENDPOINTS.put("us-west-1", "cloudformation.us-west-1.amazonaws.com");
		CLOUD_FORMATION_ENDPOINTS.put("us-west-2", "cloudformation.us-west-2.amazonaws.com");
		CLOUD_FORMATION_ENDPOINTS.put("eu-west-1", "cloudformation.eu-west-1.amazonaws.com");
		CLOUD_FORMATION_ENDPOINTS.put("ap-northeast-1", "cloudformation.ap-southeast-1.amazonaws.com");
		CLOUD_FORMATION_ENDPOINTS.put("ap-southeast-1", "cloudformation.ap-northeast-1.amazonaws.com");
		CLOUD_FORMATION_ENDPOINTS.put("ap-southeast-2", "cloudformation.ap-southeast-2.amazonaws.com");
		CLOUD_FORMATION_ENDPOINTS.put("sa-east-1", "cloudformation.sa-east-1.amazonaws.com");
	}

	// see https://github.com/garnaat/missingcloud/blob/master/aws.json and http://aws.amazon.com/articles/3912?_encoding=UTF8&jiveRedirect=1
	private static Map<String,String> RDS_ENDPOINTS = new HashMap<String, String>();
	static {
		RDS_ENDPOINTS.put("us-east-1", "rds.us-east-1.amazonaws.com");
		RDS_ENDPOINTS.put("us-west-1", "rds.us-west-1.amazonaws.com");
		RDS_ENDPOINTS.put("us-west-2", "rds.us-west-2.amazonaws.com");
		RDS_ENDPOINTS.put("eu-west-1", "rds.eu-west-1.amazonaws.com");
		RDS_ENDPOINTS.put("ap-northeast-1", "rds.ap-southeast-1.amazonaws.com");
		RDS_ENDPOINTS.put("ap-southeast-1", "rds.ap-northeast-1.amazonaws.com");
		RDS_ENDPOINTS.put("ap-southeast-2", "rds.ap-southeast-2.amazonaws.com");
		RDS_ENDPOINTS.put("sa-east-1", "rds.sa-east-1.amazonaws.com");
	}

	//private static final String VIRTUALIZATION_TYPE_PARAVIRTUAL = "paravirtual";	// NON-WINDOWS
	private static final String VIRTUALIZATION_TYPE_HVM = "hvm";					// WINDOWS

	// needs to be the same as in the plugin.xml...
	private static final String REGIONS_ALL = "All";

	// Mapping from the readable regions in the plugin.xml to the Amazon region id
	protected static final Map<String,String> REGIONS_MAP = new HashMap<String, String>();
	static {
		REGIONS_MAP.put("US West (N. California)", "us-west-1");
		REGIONS_MAP.put("US West (Oregon)", "us-west-2");
		REGIONS_MAP.put("US East (Virginia)", "us-east-1");
		REGIONS_MAP.put("Asia Pacific (Tokyo)", "ap-northeast-1");
		REGIONS_MAP.put("EU West (Ireland)", "eu-west-1");
		REGIONS_MAP.put("Asia Pacific (Singapore)", "ap-southeast-1");
		REGIONS_MAP.put("Asia Pacific (Sydney)", "ap-southeast-2");
		REGIONS_MAP.put("S. America (Sao Paulo)", "sa-east-1");
	}

	private static final String PROP_LAST_CALL = "lastCall";

	protected static final String TAG_USAGE = "Usage";
	protected static final String TAG_UNKNOWN = "Unknown";
	protected static final String TAG_STATUS = "Status";
	protected static final String TAG_CLASS = "Class";
	protected static final String TAG_TYPE = "Type";
	protected static final String TAG_OWNER = "Owner";

	/************************************** Variables for Configuration items **************************/

	// access key/secret key
	private BasicAWSCredentials awsCredentials;

	// readable name of region - combo box with available regions
	private String filterRegion;
	// Amazon region id, null if set to "All"
	private String activeRegion;

	// costs per instance type, see http://aws.amazon.com/ec2/pricing/, set to the current values
	private Properties costProperties;

	// When was the last time we got called
	private long lastCall = 0;

	// Where do we store properties accross server restarts
	private String tempFolder = null;
	private String uniqueId = null;

	private ClientConfiguration clientConfig = new ClientConfiguration();

	/*
	 * (non-Javadoc)
	 *
	 * @see com.dynatrace.diagnostics.pdk.Monitor#setup(com.dynatrace.diagnostics.pdk.MonitorEnvironment)
	 */
	@Override
	public Status setup(MonitorEnvironment env) throws Exception {
		String accessKeyId = env.getConfigString(ENV_CONFIG_ACCESS_KEY);
		if (accessKeyId == null || accessKeyId.isEmpty())
			throw new IllegalArgumentException(
					"Parameter <accesKeyID> must not be empty");

		String secretAccessKey = env.getConfigString(ENV_CONFIG_SECRET_KEY);
		if (secretAccessKey == null || secretAccessKey.isEmpty())
			throw new IllegalArgumentException(
					"Parameter <secretAccessKey> must not be empty");

		awsCredentials = new BasicAWSCredentials(accessKeyId, secretAccessKey);

		filterRegion = env.getConfigString(ENV_CONFIG_REGION);
		if (filterRegion == null || filterRegion.isEmpty())
			throw new IllegalArgumentException(
					"Parameter <region> must not be empty");

		if(!isShowAllRegions() && !REGIONS_MAP.containsKey(filterRegion)) {
			throw new IllegalArgumentException("Configured region " + filterRegion + " was not found in the list of available regions: " + REGIONS_MAP);
		}
		activeRegion = REGIONS_MAP.get(filterRegion);


		String prop = env.getConfigString(ENV_CONFIG_COST_PROPERTIES);
		if (prop == null) {
			throw new IllegalArgumentException(
					"Parameter <costProperties> must not be empty");
		}

		prop = prop.trim();
		if (prop.isEmpty()) {
			throw new IllegalArgumentException(
					"Parameter <costProperties> must not be empty");
		}

		costProperties = new Properties();
		ByteArrayInputStream inStream = new ByteArrayInputStream(prop.getBytes());
		try {
			costProperties.load(inStream);
		} finally {
			inStream.close();
		}

		// try to use the price-information from Amazon itself, fall back to manually set properties if it fails
		// TODO: this currently only includes Linux, not Windows instances, so it only helps to keep some of the
		// prices up to date, but not all
		Properties override = new Properties();
		try {
			Reader reader = new StringReader(AmazonUtils.readAmazonPriceProperties());
			try {
				override.load(reader);

				// copy all found cost-properties onto the previously loaded map of properties
				for(String name : override.stringPropertyNames()) {
					costProperties.setProperty(name, override.getProperty(name));
				}
			} finally {
				reader.close();
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "Could not read cost-properties from Amazon via JSON, using only manually set properties", e);
		}

		String temp = env.getConfigString(ENV_CONFIG_TEMP_FOLDER);
		if (temp != null && !temp.isEmpty()) {
			if(!new File(temp).canWrite()) {
				log.warning("Cannot write to configured temporary folder '" + temp + "', write access denied. Persisting run times is not possible.");
			} else {
				tempFolder = temp;
			}
		}
		String id = env.getConfigString(ENV_CONFIG_UNIQUE_ID);
		if (id != null && !id.isEmpty()) {
			uniqueId = id;
		} else if (tempFolder != null) {
			log.warning("Had a temporary folder for persisting costs across restarts, but no unqiue id was specified, multiple instances of the Account Monitor will not work correctly across restarts.");
			id = "1";
		}

		// add support for proxy configuration, Boolean.equals to also handle possible null-value
		boolean proxyEnabled = Boolean.TRUE.equals(env.getConfigBoolean(ENV_CONFIG_PROXY_ENABLED));
		if(proxyEnabled) {
			String proxyHost = env.getConfigString(ENV_CONFIG_PROXY_HOST);
			int proxyPort = 0;
			if(env.getConfigLong(ENV_CONFIG_PROXY_PORT) != null) {
				proxyPort = env.getConfigLong(ENV_CONFIG_PROXY_PORT).intValue();
			}
			String proxyUsername = env.getConfigString(ENV_CONFIG_PROXY_USERNAME);
			String proxyPassword = env.getConfigString(ENV_CONFIG_PROXY_PASSWORD);
			String proxyDomain = env.getConfigString(ENV_CONFIG_PROXY_DOMAIN);
			String proxyWorkstation = env.getConfigString(ENV_CONFIG_PROXY_WORKSTATION);

			log.info("Using proxy " + proxyHost + ":" + proxyPort + ", username: " + proxyUsername);
			clientConfig.setProxyHost(proxyHost);
			clientConfig.setProxyPort(proxyPort);
			clientConfig.setProxyUsername(proxyUsername);
			clientConfig.setProxyPassword(proxyPassword);
			clientConfig.setProxyDomain(proxyDomain);
			clientConfig.setProxyWorkstation(proxyWorkstation);

			clientConfig.setConnectionTimeout(60000);
			clientConfig.setSocketTimeout(60000);
		}

		return new Status(Status.StatusCode.Success);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.dynatrace.diagnostics.pdk.Monitor#execute(com.dynatrace.diagnostics.pdk.MonitorEnvironment)
	 */
	@Override
	public Status execute(MonitorEnvironment env) throws Exception {
		log.info("Executing Amazon Account Monitor for region: " + filterRegion);

		try {
			// retrieve measures for cloud formation numbers in each state
			measureStacks(env);

			// retrive measures for ec2 instances in each state
			Map<Reservation, String> reservations = measureInstances(env);

			double adjustmentFactor = getAdjustmentFactor();

			// retrieve measures for cost of ec2 instances per instance type
			measureInstanceCosts(reservations, env, adjustmentFactor);

			// measure RDS instances
			Map<DBInstance, String> instances = measureRDSInstances(env);

			// retrieve measures for cost of rds instances
			measureRDSInstanceCosts(instances, env, adjustmentFactor);
		} catch (Exception e) {
			// Our plugin functionality does not report Exceptions well...
			log.log(Level.WARNING, "Had exception while communicating with Amazon AWS: " + e);
			log.log(Level.WARNING, "Had exception while communicating with Amazon AWS", e);
			throw e;
		} catch (Throwable e) {
			// Our plugin functionality does not report Exceptions well...
			log.log(Level.WARNING, "Had throwable while communicating with Amazon AWS: " + e);
			log.log(Level.WARNING, "Had throwable while communicating with Amazon AWS", e);
			throw new Exception(e);
		}

		// now persist the value
		persistLastCallTimestamp();

		return new Status(Status.StatusCode.Success);
	}

	private void measureStacks(MonitorEnvironment env) {
		final Collection<String> endPoints;
		if(isShowAllRegions()) {
			endPoints = CLOUD_FORMATION_ENDPOINTS.values();
		} else {
			endPoints = Collections.singleton(CLOUD_FORMATION_ENDPOINTS.get(activeRegion));
		}

		// walk all the different regions that were specified
		Measure count = new Measure();
		Map<String, Measure> countPerStatus = new HashMap<String, Measure>();
		for(String endPoint : endPoints) {
			if(log.isLoggable(Level.FINE)) {
				log.fine("Retrieving cloud formation stacks for endpoint: " + endPoint);
			}

			AmazonCloudFormation stackbuilder = new AmazonCloudFormationClient(awsCredentials, clientConfig);
			stackbuilder.setEndpoint(endPoint);

			DescribeStacksRequest stackRequest = new DescribeStacksRequest();
			List<Stack> stacks = stackbuilder.describeStacks(stackRequest).getStacks();

			for (Stack stack : stacks) {
				String status = stack.getStackStatus();
				if(log.isLoggable(Level.FINE)) {
					/* Retrieves the actual template, not the URL-source...
					GetTemplateRequest templateRequest = new GetTemplateRequest();
					templateRequest.setStackName(stack.getStackId());
					String template = stackbuilder.getTemplate(templateRequest).getTemplateBody();*/
					log.fine("Having Stack: " + stack.getStackName() + ", state: " + status + /*", template: " + template +*/ ", parameters: " + stack.getParameters());
				}

				// only count this as "active" if it is none of the excluded states
				if(!MSR_CLOUD_FORMATIONS_EXCLUDE.contains(status)) {
					count.incValue();
				}

				if(!countPerStatus.containsKey(status)) {
					countPerStatus.put(status, new Measure(1));
				} else {
					countPerStatus.get(status).incValue();
				}
			}
		}

		// retrieve and set the measurements
		writeMeasure(METRIC_GROUP_CLOUD_FORMATION, MSR_CLOUD_FORMATIONS, env, count);

		// write measures for all status-values that we found
		for(Map.Entry<String, Measure> entry : countPerStatus.entrySet()) {
			writeMeasure(METRIC_GROUP_CLOUD_FORMATION, "Count_" + entry.getKey(), env, entry.getValue());
		}
	}

	private Map<Reservation, String> measureInstances(MonitorEnvironment env) {
		final Map<String, String> endPoints = getEC2Endpoints();

		// walk all the different regions that were specified
		Measure count = new Measure(TAG_USAGE);
		Measure countByOwner = new Measure(TAG_OWNER);

		Map<String, Measure> countPerStatus = new HashMap<String, Measure>();
		Map<Reservation, String> allReservations = new HashMap<Reservation, String>();
		for(Map.Entry<String,String> entry : endPoints.entrySet()) {
			if(log.isLoggable(Level.FINE)) {
				log.fine("Retrieving ec2 instances for endpoint: " + entry.getValue());
			}

			AmazonEC2Client client = new AmazonEC2Client(awsCredentials, clientConfig);
			client.setEndpoint(entry.getValue());
			DescribeInstancesRequest instanceRequest = new DescribeInstancesRequest();
			List<Reservation> reservations = client.describeInstances(instanceRequest).getReservations();

			for(Reservation reservation : reservations) {
				allReservations.put(reservation, entry.getKey());

				for(Instance instance : reservation.getInstances()) {
					String status = instance.getState().getName();
					if(log.isLoggable(Level.FINE)) {
						log.fine("Having Instance: " + instance.getInstanceId() + ", state: " + status + ", public ip:  " + instance.getPublicDnsName());
					}

					final String usage = AmazonUtils.getUsageTag(instance);
					final String owner = AmazonUtils.getOwnerTag(instance);

					// by definition we exclude stopped and terminated from overall count
					if(!EC2_STATE_STOPPED.equals(status) && !EC2_STATE_TERMINATED.equals(status)) {
						count.incValue();
						count.addDynamicMeasure(usage, 1);
					}

					if(!EC2_STATE_STOPPED.equals(status) && !EC2_STATE_TERMINATED.equals(status)) {
						countByOwner.incValue();
						countByOwner.addDynamicMeasure(owner, 1);
					}

					if(!countPerStatus.containsKey(status)) {
						countPerStatus.put(status, new Measure(TAG_USAGE, 1));
					} else {
						countPerStatus.get(status).incValue();
					}

					countPerStatus.get(status).addDynamicMeasure(usage, 1);
				}
			}
		}

		writeMeasure(METRIC_GROUP_INSTANCE, MSR_EC2_INSTANCES, env, count);
		writeMeasure(METRIC_GROUP_INSTANCE, MSR_EC2_INSTANCES, env, countByOwner);

		// write measures for all status-values that we found
		for(Map.Entry<String, Measure> entry : countPerStatus.entrySet()) {
			String status = entry.getKey();
			Measure value = entry.getValue();
			if(status.equals(EC2_STATE_STOPPED)) {
				writeMeasure(METRIC_GROUP_INSTANCE, MSR_EC2_INSTANCES_STOPPED, env, value);
			} else if(status.equals(EC2_STATE_PENDING)) {
				writeMeasure(METRIC_GROUP_INSTANCE, MSR_EC2_INSTANCES_PENDING, env, value);
			} else if(status.equals(EC2_STATE_RUNNING)) {
				writeMeasure(METRIC_GROUP_INSTANCE, MSR_EC2_INSTANCES_RUNNING, env, value);
			} else if(status.equals(EC2_STATE_SHUTTING_DOWN)) {
				writeMeasure(METRIC_GROUP_INSTANCE, MSR_EC2_INSTANCES_SHUTTING_DOWN, env, value);
			} else if(status.equals(EC2_STATE_TERMINATED)) {
				writeMeasure(METRIC_GROUP_INSTANCE, MSR_EC2_INSTANCES_TERMINATED, env, value);
			} else {
				log.warning("Found unknown instance state: " + status + ", could not report value: " + value + " for measure group " + METRIC_GROUP_INSTANCE);
			}
		}

		return allReservations;
	}

	private Map<DBInstance, String> measureRDSInstances(MonitorEnvironment env) {
		final Map<String, String> endPoints = getRDSEndpoints();

		// walk all the different regions that were specified
		Measure count = new Measure(TAG_STATUS);
		Measure countByClass = new Measure(TAG_CLASS);

		Map<DBInstance, String> allInstances = new HashMap<DBInstance, String>();
		for(Map.Entry<String,String> entry : endPoints.entrySet()) {
			if(log.isLoggable(Level.FINE)) {
				log.fine("Retrieving rds instances for endpoint: " + entry.getValue());
			}

			AmazonRDSClient client = new AmazonRDSClient(awsCredentials, clientConfig);
			client.setEndpoint(entry.getValue());

			DescribeDBInstancesResult instanceRequest = client.describeDBInstances();
			List<DBInstance> dbInstances = instanceRequest.getDBInstances();
			for(DBInstance instance : dbInstances) {
				allInstances.put(instance, entry.getKey());

				String status = instance.getDBInstanceStatus();
				if(log.isLoggable(Level.FINE)) {
					log.fine("Having Instance: " + instance.getDBInstanceIdentifier() + ", state: " + status);
				}

				String instanceClass = instance.getDBInstanceClass();

				count.incValue();
				count.addDynamicMeasure(status, 1);

				countByClass.incValue();
				countByClass.addDynamicMeasure(instanceClass, 1);
			}
		}

		writeMeasure(METRIC_GROUP_RDS_INSTANCE, MSR_RDS_INSTANCES, env, count);
		writeMeasure(METRIC_GROUP_RDS_INSTANCE, MSR_RDS_INSTANCES_BY_CLASS, env, countByClass);

		return allInstances;
	}

	private void measureRDSInstanceCosts(Map<DBInstance, String> instances, MonitorEnvironment env, double adjustmentFactor) {
		Measure overallCosts = new Measure(TAG_STATUS);
		overallCosts.setAdjustmentFactor(adjustmentFactor);
		Measure costsByClass = new Measure(TAG_CLASS);
		costsByClass.setAdjustmentFactor(adjustmentFactor);

		for(DBInstance instance : instances.keySet()) {
			StringBuilder property = new StringBuilder("cost.");

			String instanceClass = instance.getDBInstanceClass();
			/*if(type == null) {
				throw new IllegalArgumentException("Could not determine type of instance: " + instance.getInstanceId() + ", key: " + instance.getKeyName() + ", state: " + instance.getState().getName());
			}*/
			String status = instance.getDBInstanceStatus();

			property.append(instances.get(instance)).append(".").append(instanceClass);

			String costString = costProperties.getProperty(property.toString());
			if(log.isLoggable(Level.FINE)) {
				log.fine("Cost for RDS instance with state: " + status + ": property: " + property.toString() + ": " + costString);
			}

			if(costString == null) {
				throw new IllegalArgumentException("Could not find defined costs for property: " + property.toString() + ", please check the provided properties for 'Amazon Instance Cost'");
			}

			double costsPerHour = Double.parseDouble(costString);

			// sum up costs overall and for this status
			overallCosts.addValue(costsPerHour);
			overallCosts.addDynamicMeasure(status, costsPerHour);

			// sum up costs overall and for this type
			costsByClass.addValue(costsPerHour);
			costsByClass.addDynamicMeasure(instanceClass, costsPerHour);
		}

		// write measures, adjust based on the per-hour-factor that we calculated
		writeMeasure(METRIC_GROUP_RDS_INSTANCE_COST, MSR_RDS_INSTANCE_COST, env, overallCosts);
		writeMeasure(METRIC_GROUP_RDS_INSTANCE_COST, MSR_RDS_INSTANCE_COST_BY_CLASS, env, costsByClass);
	}

	/**
	 *
	 * @return
	 * @author dominik.stadler
	 */
	protected Map<String, String> getEC2Endpoints() {
		final Map<String, String> endPoints = new HashMap<String, String>();
		AmazonEC2Client client = new AmazonEC2Client(awsCredentials, clientConfig);
		DescribeRegionsRequest regionsRequest = new DescribeRegionsRequest();
		List<Region> regions = client.describeRegions(regionsRequest).getRegions();
		for(Region region : regions) {
			if(log.isLoggable(Level.FINE)) {
				log.fine("Region: " + region.getRegionName() + ", endpoint: " + region.getEndpoint());
			}

			// if should show all regions or this is the region that we are interested in
			if(isShowAllRegions() ||
					(region.getRegionName().equals(activeRegion))) {
				endPoints.put(region.getRegionName(), region.getEndpoint());
			}
		}

		return endPoints;
	}

	protected Map<String, String> getRDSEndpoints() {
		final Map<String, String> endPoints;
		if(isShowAllRegions()) {
			endPoints = RDS_ENDPOINTS;
		} else {
			endPoints = Collections.singletonMap(activeRegion, RDS_ENDPOINTS.get(activeRegion));
		}

		return endPoints;
	}

	/**
	 *
	 * @return
	 * @author dominik.stadler
	 */
	protected boolean isShowAllRegions() {
		return REGIONS_ALL.equals(filterRegion);
	}

	/**
	 *
	 * @param env
	 * @param value
	 * @param dynamicMeasures
	 */
	protected void writeMeasure(String group, String name, MonitorEnvironment env, Measure value) {
		Collection<MonitorMeasure> measures = env.getMonitorMeasures(group, name);
		if (measures != null) {
			if (log.isLoggable(Level.INFO)) {
				log.info("Setting measure '" + name + "' to value " + value.getValue() + ", dynamic: " + value.getDynamicMeasureName() + ": " + value.getDynamicMeasures());
			}
			for (MonitorMeasure measure : measures) {
				measure.setValue(value.getValue());

				if(value.getDynamicMeasures() != null) {
				     //for this subscribed measure we want to create a dynamic measure
					for(Map.Entry<String, Double> dynamic : value.getDynamicMeasures().entrySet()) {
					     MonitorMeasure dynamicMeasure = env.createDynamicMeasure(measure, value.getDynamicMeasureName(), dynamic.getKey());
					     dynamicMeasure.setValue(dynamic.getValue());
					}
				}
			}
		} else {
			log.warning("Could not find measure " + name + "@" + group + ", tried to report value: " + value);
		}
	}

	/**
	 *
	 * @param reservations
	 * @param env
	 * @author dominik.stadler
	 * @throws IOException
	 */
	private void measureInstanceCosts(Map<Reservation, String> reservations, MonitorEnvironment env, double adjustmentFactor) {
		/*// read regions
		AmazonEC2Client client = new AmazonEC2Client(awsCredentials, clientConfig);
		DescribeRegionsRequest regionsRequest = new DescribeRegionsRequest();
		List<Region> regions = client.describeRegions(regionsRequest).getRegions();
		for(Region region : regions) {
			log.info("Region: " + region.getRegionName() + ", endpoint: " + region.getEndpoint());
		}*/

		Measure overallCosts = new Measure(TAG_USAGE);
		Measure overallCostsByType = new Measure(TAG_TYPE);
		Measure overallCostsByOwner = new Measure(TAG_OWNER);
		overallCosts.setAdjustmentFactor(adjustmentFactor);

		Map<String, Measure> costsPerType = new HashMap<String, Measure>();

		for(Reservation reservation : reservations.keySet()) {
			for(Instance instance : reservation.getInstances()) {
				StringBuilder property = new StringBuilder("cost.");

				String type = instance.getInstanceType();
				/*if(type == null) {
					throw new IllegalArgumentException("Could not determine type of instance: " + instance.getInstanceId() + ", key: " + instance.getKeyName() + ", state: " + instance.getState().getName());
				}*/

				property.append(reservations.get(reservation)).append(".").append(type);

				String virtType = instance.getVirtualizationType();
				if(VIRTUALIZATION_TYPE_HVM.equals(virtType)) {
					// Windows
					property.append(".windows");
				} else {
					// Other/Linux
					property.append(".linux");
				}

				// There is enum InstanceStateName, but it does not contain "stopped"!?
				// by definition stopped and terminated do not incur costs
				// although Amazon documents only "terminated" does not cause costs, we found out that
				// also stopped instances do not cause costs
				InstanceState state = instance.getState();
				if(!EC2_STATE_STOPPED.equals(state.getName()) && !EC2_STATE_TERMINATED.equals(state.getName())) {
					if(log.isLoggable(Level.FINE)) {
						log.fine("Cost for instance with state: " + state.getName() + ": property: " + property.toString() + ": " + costProperties.getProperty(property.toString()));
					}

					if(costProperties.getProperty(property.toString()) == null) {
						throw new IllegalArgumentException("Could not find defined costs for property: " + property.toString() + ", please check the provided properties for 'Amazon Instance Cost'");
					}

					double costsPerHour = Double.parseDouble(costProperties.getProperty(property.toString()));

					final String usage = AmazonUtils.getUsageTag(instance);
					final String owner = AmazonUtils.getOwnerTag(instance);

					// sum up costs overall and for this type
					overallCosts.addValue(costsPerHour);
					overallCosts.addDynamicMeasure(usage, costsPerHour);
					overallCostsByType.addValue(costsPerHour);
					overallCostsByType.addDynamicMeasure(type, costsPerHour);
					overallCostsByOwner.addValue(costsPerHour);
					overallCostsByOwner.addDynamicMeasure(owner, costsPerHour);

					if(!costsPerType.containsKey(type)) {
						Measure value = new Measure(TAG_USAGE, costsPerHour);
						value.setAdjustmentFactor(adjustmentFactor);
						costsPerType.put(type, value);
					} else {
						costsPerType.get(type).addValue(costsPerHour);
					}

					costsPerType.get(type).addDynamicMeasure(usage, costsPerHour);
				}
			}
		}

		// write measures, adjust based on the per-hour-factor that we calculated
		writeMeasure(METRIC_GROUP_INSTANCE_COST, MSR_EC2_INSTANCE_COST, env, overallCosts);
		writeMeasure(METRIC_GROUP_INSTANCE_COST, MSR_EC2_INSTANCE_COST, env, overallCostsByType);
		writeMeasure(METRIC_GROUP_INSTANCE_COST, MSR_EC2_INSTANCE_COST, env, overallCostsByOwner);
		for(String[] msr : AmazonAccountMonitor.MSR_EC2_INSTANCE_TYPES) {
			if(costsPerType.containsKey(msr[1])) {
				writeMeasure(METRIC_GROUP_INSTANCE_COST, msr[0], env, costsPerType.get(msr[1]));
			}
		}
	}

	private void persistLastCallTimestamp() {
		if(tempFolder != null) {
			Properties prop = new Properties();
			File file = new File(tempFolder, replaceInvalidFileNameCharacters("AmazonAccountMonitor-" + uniqueId + ".properties"));
			try {
				FileOutputStream outStream = new FileOutputStream(file);
				try {
					prop.setProperty(PROP_LAST_CALL, Long.toString(lastCall));
					prop.store(outStream, "Created by AmazonAccountMonitor");
					log.info("Storing " + lastCall + " as persisten time of last execution");
				} finally {
					outStream.close();
				}
			} catch (IOException e) {
				log.log(Level.WARNING, "Could not read properties file: " + file, e);
			}
		}
	}

	private double getAdjustmentFactor() {
		// adjust for last time called, i.e. if we are called multiple times per hour, we divide,
		// if we are not called for some hours, we add it up.
		// default if not called before is to report costs for one hour
		double adjustmentFactor = 1.0;

		// lastCall is zero at first call, try to load from persisted properties
		if(lastCall == 0 && tempFolder != null) {
			File file = new File(tempFolder, replaceInvalidFileNameCharacters("AmazonAccountMonitor-" + uniqueId + ".properties"));
			if(file.exists()) {
				Properties prop = new Properties();
				try {
					FileInputStream inStream = new FileInputStream(file);
					try {
						prop.load(inStream);

						String property = prop.getProperty(PROP_LAST_CALL);
						if(property != null) {
							try {
								lastCall = Long.parseLong(property);
								log.info("Loaded " + lastCall + " as time of last execution");
							} catch (NumberFormatException e) {
								log.log(Level.WARNING, "Could not parse property for 'lastCall' to long: " + property, e);
							}
						}
					} finally {
						inStream.close();
					}
				} catch (IOException e) {
					log.log(Level.WARNING, "Could not read properties file: " + file, e);
				}
			}
		}

		// if we now found a timestamp, we can compute a more accurate adjustment factor
		if(lastCall != 0) {
			long time = System.currentTimeMillis() - lastCall;
			adjustmentFactor = time / HOUR_IN_MS;			// adjustment factor is per hour
			log.info("Adjusting costs for " + time + "ms, adjustment factor: " + adjustmentFactor);
		}
		lastCall = System.currentTimeMillis();
		return adjustmentFactor;
	}

	// Copied from FileTools as this was added post-4.0 but monitor should work on 4.0 as well
    private static final String FORMAT_STRING_ILLEGAL_CHARACTER_SEQUENCE_REGEXP = "[\\\\/?:*\"<>|]";
    private static String replaceInvalidFileNameCharacters(String string) {
    	return string.replaceAll(FORMAT_STRING_ILLEGAL_CHARACTER_SEQUENCE_REGEXP, "-");
    }

	/*
	 * (non-Javadoc)
	 *
	 * @see com.dynatrace.diagnostics.pdk.Monitor#teardown(com.dynatrace.diagnostics.pdk.MonitorEnvironment)
	 */
	@Override
	public void teardown(MonitorEnvironment env) throws Exception {
		// nothing to do here
	}
}
