/***************************************************
 * dynaTrace Diagnostics (c) dynaTrace software GmbH
 *
 * @file: AmazonUtils.java
 * @date: 02.07.2013
 * @author: cwat-dstadler
 */
package com.dynatrace.diagnostics.plugins.amazon;

import static com.dynatrace.diagnostics.plugins.amazon.AmazonAccountMonitor.TAG_USAGE;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeRegionsRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Region;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.amazonaws.util.json.JSONTokener;
import com.dynatrace.diagnostics.sdk.MonitorEnvironment30Impl;
import com.dynatrace.diagnostics.sdk.MonitorMeasure30Impl;


/**
 *
 * @author cwat-dstadler
 */
public class AmazonUtils {
	private static final Logger log = Logger.getLogger(AmazonUtils.class.getName());

	private static final String[] URL_JSON_PRICES = new String[] {
		"http://aws.amazon.com/ec2/pricing/pricing-on-demand-instances.json",
		"http://a0.awsstatic.com/pricing/1/ec2/mswin-od.min.js",
		"http://a0.awsstatic.com/pricing/1/ec2/linux-od.min.js",
	};

	public static List<String> getAccountNames() throws IOException {
		Properties prop = AmazonUtils.readProperties();

		List<String> names = new ArrayList<String>();
		Enumeration<?> propertyNames = prop.propertyNames();
		while(propertyNames.hasMoreElements()) {
			String name = (String) propertyNames.nextElement();
			if(name.startsWith("accessKey.")) {
				names.add(name.replace("accessKey.", ""));
			}
		}

		return names;
	}

	public static BasicAWSCredentials getAwsCredentials(String name) throws IOException {
		Properties prop = AmazonUtils.readProperties();

		String accessKey = prop.getProperty("accessKey." + name);
		String secretKey = prop.getProperty("secretKey." + name);

		return new BasicAWSCredentials(accessKey, secretKey);
	}

	public static void init(ClientConfiguration clientConfig) {
		Logger lLog = Logger.getLogger("");
		for (Handler handler : lLog.getHandlers()) {
            handler.setFormatter(new DefaultFormatter());
        }

		/*String proxyHost = "cns-lnz.emea.cpwr.corp";
		int proxyPort = 8001;

		log.info("Using proxy " + proxyHost + ":" + proxyPort);
		clientConfig.setProxyHost(proxyHost);
		clientConfig.setProxyPort(proxyPort);*/

		if(clientConfig != null) {
			clientConfig.setConnectionTimeout(60000);
			clientConfig.setSocketTimeout(60000);
		}
	}

	public static String getInstanceTag(Instance instance, String tagname) {
		for(Tag tag : instance.getTags()) {
			if(tag.getKey().equalsIgnoreCase(tagname)) {
				return tag.getValue();
			}
		}

		log.fine("Instance " + instance + " does not have a tag '" + tagname + "'");
		return AmazonAccountMonitor.TAG_UNKNOWN;
	}

	public static String getUsageTag(Instance instance) {
		String usage = getInstanceTag(instance, AmazonAccountMonitor.TAG_USAGE);

		// try to find the Usage-Tag automatically for some instances until we tag them all correctly
		// changes here should also be reflected in "SetUsageTags"
		if(usage.equals(AmazonAccountMonitor.TAG_UNKNOWN)) {
			String guardian = getInstanceTag(instance, "Client.Guardian");
			// for now we put all untagged instances with "GDN-key" into UEMaaS as this is what most people in Gdansk work on
			if(!guardian.equals(AmazonAccountMonitor.TAG_UNKNOWN) /*|| !stack.equals(TAG_UNKNOWN)*/ ||
					(instance.getKeyName() != null && instance.getKeyName().equals("GDN-key"))) {
				usage = "UEMaaS";
			} else if(getInstanceTag(instance, "Name").contains("CoE") ||
					(instance.getKeyName() != null && instance.getKeyName().equals("coe-demo"))) {
				usage = "CoE";
			} else if(!getInstanceTag(instance, "DemoId").equals(AmazonAccountMonitor.TAG_UNKNOWN)) {
				usage = "CloudDemo";
			} else if(instance.getKeyName() != null && instance.getKeyName().equals("EasyTravelLargeDeployment")) {
				usage = "easyTravelNG";
			} else if(!getInstanceTag(instance, "aws:elasticmapreduce:instance-group-role").equals(AmazonAccountMonitor.TAG_UNKNOWN)) {
				usage = "MapReduce";
			} else if(getInstanceTag(instance, "Name").toLowerCase().contains("puppet")) {
				usage = "Puppet";
			} else if(getInstanceTag(instance, "Name").toLowerCase().contains("cloudera")) {
				usage = "Cloudera";
			}
		}

		return usage;
	}

	public static String getOwnerTag(Instance instance) {
		String usage = getInstanceTag(instance, AmazonAccountMonitor.TAG_OWNER);

		// TODO: try to find the Owner-Tag automatically for some instances until we tag them all correctly
		// changes here should also be reflected in "SetUsageTags"
		/*if(usage.equals(AmazonAccountMonitor.TAG_UNKNOWN)) {
			String guardian = getInstanceTag(instance, "Client.Guardian");
			// for now we put all untagged instances with "GDN-key" into UEMaaS as this is what most people in Gdansk work on
			if(!guardian.equals(AmazonAccountMonitor.TAG_UNKNOWN) || !stack.equals(TAG_UNKNOWN) ||
					(instance.getKeyName() != null && instance.getKeyName().equals("GDN-key"))) {
				usage = "UEMaaS";
			} else if(getInstanceTag(instance, "Name").contains("CoE") ||
					(instance.getKeyName() != null && instance.getKeyName().equals("coe-demo"))) {
				usage = "CoE";
			} else if(!getInstanceTag(instance, "DemoId").equals(AmazonAccountMonitor.TAG_UNKNOWN)) {
				usage = "CloudDemo";
			} else if(instance.getKeyName() != null && instance.getKeyName().equals("EasyTravelLargeDeployment")) {
				usage = "easyTravelNG";
			} else if(!getInstanceTag(instance, "aws:elasticmapreduce:instance-group-role").equals(AmazonAccountMonitor.TAG_UNKNOWN)) {
				usage = "MapReduce";
			} else if(getInstanceTag(instance, "Name").toLowerCase().contains("puppet")) {
				usage = "Puppet";
			} else if(getInstanceTag(instance, "Name").toLowerCase().contains("cloudera")) {
				usage = "Cloudera";
			}
		}*/

		return usage;
	}

	public static void setUsageTag(AmazonEC2Client client, Instance instance, String value) {
		log.info("Setting tag " + value + " on instance " + instance);

		List<Tag> tags = new ArrayList<Tag>();
		tags.add(new Tag(TAG_USAGE, value));

		CreateTagsRequest ctr = new CreateTagsRequest();
		ctr.setTags(tags);
		ctr.withResources(instance.getInstanceId());
		client.createTags(ctr);
	}

	public static String getInstanceDescription(Instance instance) {
		return "Instance: " + instance.getInstanceId() + ": " + AmazonUtils.getInstanceTag(instance,  "Name") +
				": Started at " + instance.getLaunchTime() +
				", Key: " + instance.getKeyName() +
				", Type: " + instance.getInstanceType() +
				", State: " + instance.getState().getName() +
				", Owner: " + AmazonUtils.getOwnerTag(instance) +
				", Tags: " + instance.getTags();
	}

	public static Map<String, String> getEndpoints(ClientConfiguration clientConfig, String name) throws IOException {
		final Map<String, String> endPoints = new HashMap<String, String>();
		AmazonEC2Client client = new AmazonEC2Client(getAwsCredentials(name), clientConfig);
		DescribeRegionsRequest regionsRequest = new DescribeRegionsRequest();
		List<Region> regions = client.describeRegions(regionsRequest).getRegions();
		for(Region region : regions) {
			log.info("Region: " + region.getRegionName() + ", endpoint: " + region.getEndpoint());

			// if should show all regions or this is the region that we are interested in
			endPoints.put(region.getRegionName(), region.getEndpoint());
		}

		return endPoints;
	}


	public static Properties readProperties() throws IOException {
		Properties prop = new Properties();
		/* Template for the properties file in testsrc/AwsCredentials.properties:
# Fill in your AWS Access Key ID and Secret Access Key
# http://aws.amazon.com/security-credentials
accessKey =
secretKey =
		 */
		FileInputStream inStream = new FileInputStream("testsrc/AwsCredentials.properties");
		try {
			prop.load(inStream);
		} finally {
			inStream.close();
		}
		return prop;
	}

	/**
	 *
	 * @param env
	 */
	public static void createMeasure(MonitorEnvironment30Impl env, String group, String name) {
		MonitorMeasure30Impl measure;
		measure = new MonitorMeasure30Impl();
		measure.setMetricGroupName(group);
		measure.setMetricName(name);
		env.internalGetMeasures().add(measure);
	}

	private static final Map<String,String> JSON_REGION_MAP = new HashMap<String, String>();
	static {
	    JSON_REGION_MAP.put("us-east", "us-east-1");
	    JSON_REGION_MAP.put("us-west-2", "us-west-2");
	    JSON_REGION_MAP.put("us-west", "us-west-1");
	    JSON_REGION_MAP.put("eu-ireland", "eu-west-1");
	    JSON_REGION_MAP.put("apac-sin", "ap-southeast-1");
	    JSON_REGION_MAP.put("apac-tokyo", "ap-northeast-1");
	    JSON_REGION_MAP.put("apac-syd", "ap-southeast-2");
	    JSON_REGION_MAP.put("sa-east-1", "sa-east-1");
	}

	public static String readAmazonPriceProperties() throws IOException {
		StringBuffer pricing = new StringBuffer();
		for(String urlJson : URL_JSON_PRICES) {
			final URI url;
			try {
				url = new URI(urlJson);
			} catch (URISyntaxException e) {
				throw new IOException(e);
			}

			HttpClient httpclient = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(url);
			HttpResponse response = httpclient.execute(httpGet);

			try {
				int statusCode = response.getStatusLine().getStatusCode();
				if(statusCode != 200) {
					log.warning("Had HTTP StatusCode " + statusCode + " for request: " + url);
					return null;
				}
			    HttpEntity entity = response.getEntity();

			    // do conversion to string manually, avoid dependency on commons-io for now
			    String jsonTxt = readToString(entity.getContent());

			    // adjust jsonp to json
			    if(jsonTxt.startsWith("/*")) {
			    	int endPos = jsonTxt.indexOf("*/");
			    	jsonTxt = jsonTxt.substring(endPos+2);
			    }
			    jsonTxt = jsonTxt.replace("callback(", "");
			    if(jsonTxt.trim().endsWith(");")) {
			    	jsonTxt = jsonTxt.substring(0, jsonTxt.length()-2);
			    }

			    // use JSONObject to walk the tree of json-elements
		        JSONTokener tokener = new JSONTokener(jsonTxt);
		        JSONObject json = new JSONObject(tokener);
		        JSONObject config = json.getJSONObject("config");

		        JSONArray regions = config.getJSONArray("regions");
		        for(int i = 0;i < regions.length();i++) {
		        	JSONObject regionMap = regions.getJSONObject(i);
		        	String region = (String) regionMap.get("region");
					log.info("Found region in Amazon Pricing json file: " + region);

					JSONArray instanceTypes = regionMap.getJSONArray("instanceTypes");
					for(int j = 0;j < instanceTypes.length();j++) {
						JSONObject instanceTypeMap = instanceTypes.getJSONObject(j);
						JSONArray sizes = instanceTypeMap.getJSONArray("sizes");
						for(int k = 0;k < sizes.length();k++) {
							JSONObject sizeMap = sizes.getJSONObject(k);
							String size = sizeMap.getString("size");
							JSONArray values = sizeMap.getJSONArray("valueColumns");
							for(int l = 0;l < values.length();l++) {
								JSONObject valueMap = values.getJSONObject(l);
								String osName = valueMap.getString("name");
								if(osName.equals("mswin")) {
									osName = "windows";
								} else if (osName.equals("os")) {
									if(urlJson.contains("mswin-")) {
										osName = "windows";
									} else {
										osName = "linux";
									}
								}
								JSONObject prices = valueMap.getJSONObject("prices");
								String price = prices.getString("USD");

								String mappedRegion = JSON_REGION_MAP.get(region);
								if(mappedRegion == null) {
									throw new IllegalStateException("Did not find a mapping for region " + region + ", size: " + size + ", osName: " + osName + ", price: " + price + ", having mappings: " + JSON_REGION_MAP);
								}
								// add to properties
								// cost.us-east-1.m1.small.linux=0.060
								pricing.append("cost." + mappedRegion + "." + size + "." + osName + "=" + price + "\n");
							}
						}
					}
		        }

			    // ensure all content is taken out to free resources
			    EntityUtils.consume(entity);
			} catch (JSONException e) {
				throw new IOException(e);
			} finally {
			    httpclient.getConnectionManager().shutdown();
			}
		}

		return pricing.toString();
	}

	protected static String readToString(InputStream stream) throws IOException {
		StringBuilder inputStringBuilder = new StringBuilder();
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
		try {
			String line = bufferedReader.readLine();
			while(line != null){
			    inputStringBuilder.append(line).append('\n');
			    line = bufferedReader.readLine();
			}
		} finally {
			bufferedReader.close();
		}

		return inputStringBuilder.toString();
	}
}
