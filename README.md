<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <title>Amazon EC2 Account Cost Monitoring FastPack</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta http-equiv="X-UA-Compatible" content="IE=EmulateIE8" />
    <meta content="Scroll Wiki Publisher" name="generator"/>
    <link type="text/css" rel="stylesheet" href="css/blueprint/liquid.css" media="screen, projection"/>
    <link type="text/css" rel="stylesheet" href="css/blueprint/print.css" media="print"/>
    <link type="text/css" rel="stylesheet" href="css/content-style.css" media="screen, projection, print"/>
    <link type="text/css" rel="stylesheet" href="css/screen.css" media="screen, projection"/>
    <link type="text/css" rel="stylesheet" href="css/print.css" media="print"/>
</head>
<body>
    <div class="container" style="min-width: 760px;">
        <div class="header block">
            <div class="header-left column span-6">
                
            </div>
            <div class="column span-18 header-right last">
                <h4>Amazon EC2 Account Cost Monitoring FastPack</h4>
            </div>
        </div>
        <div class="block">
            <div class="toc column span-6 prepend-top">
                <h3>Table of Contents
                                        <span class="small">(<a href="Amazon_EC2_Account_Cost_Monitoring_FastPack.html">Start</a>)</span>
                                    </h3>
<ul class="toc">
</ul>
            </div>
            <div id="64192516" class="content column span-18 last">
                <h1>Amazon EC2 Account Cost Monitoring FastPack</h1>
    <div class="section-2"  id="64192516_AmazonEC2AccountCostMonitoringFastPack-Overview"  >
        <h2>Overview</h2>
    <p>
            <img src="images_community/download/attachments/64192516/icon.png" alt="images_community/download/attachments/64192516/icon.png" class="confluence-embedded-image image-left" />
        The dynaTrace FastPack for Amazon EC2 Account Cost Monitoring monitors EC2 instances and CloudFormations of an Amazon AWS Account. It provides a monitor which retrieves measures for the number of instances/formations as well as cost estimations. The FastPack consists of a custom Monitor, a sample System Profile, and a dashboard.    </p>
    </div>
    
    <div class="section-2"  id="64192516_AmazonEC2AccountCostMonitoringFastPack-FastPackDetails"  >
        <h2>FastPack Details</h2>
    <div class="tablewrap">
        <table>
<thead class=" "></thead><tfoot class=" "></tfoot><tbody class=" ">    <tr>
            <td rowspan="1" colspan="1">
        <p>
Name    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
<strong class=" ">Amazon EC2 Account Cost Monitoring FastPack</strong>    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
Version    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
5.5.0.5227    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
dynaTrace Version    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
dynaTrace 5.5 and higher    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
Author    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
dynaTrace software    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
License    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
<a href="attachments_5275722_2_dynaTraceBSD.txt">dynaTrace BSD</a>    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
Support    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
<a href="https://community/display/DL/Support+Levels#SupportLevels-Community">Not Supported </a>    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
FastPack Contents    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
<a href="attachments_174752520_1_amazonaccountmonitor_5.5.0.5227.dtp">Amazon EC2 Account Cost Monitoring FastPack</a>    </p>
            </td>
        </tr>
</tbody>        </table>
            </div>
    </div>
    
    <div class="section-2"  id="64192516_AmazonEC2AccountCostMonitoringFastPack-Description"  >
        <h2>Description</h2>
    
    <p>
This FastPack extends performance data collection to number of instances on Amazon EC2 and estimated cost. It    </p>
<ul class=" "><li class=" ">    <p>
Captures number of instances and calculates resulting cost    </p>
</li><li class=" ">    <p>
Provides ready made System Profile and Dashboard for visualization of results    </p>
</li><li class=" ">    <p>
Allows alerting on number of instances or running/daily/monthly costs    </p>
</li><li class=" ">    <p>
Configurable region handling and cost estimations    </p>
</li></ul>    <p>
To achieve this, this plugin uses the Amazon Webservices interfaces to periodically retrieve information about the number and types of EC2 instances and CloudFormations that are running in a configured Amazon AWS account and calculates related measures.    </p>
    </div>
    
    <div class="section-2"  id="64192516_AmazonEC2AccountCostMonitoringFastPack-Installation"  >
        <h2>Installation</h2>
    
<ul class=" "><li class=" ">    <p>
Download the <a href="attachments_99254374_1_amazonaccountmonitor_4.1.0.2973.dtp">plugin file</a>    </p>
</li><li class=" ">    <p>
On Windows, double click the file to install it in the dynaTrace Client, on Unix systems you can use &quot;Tools - Manage Plugins - Install Plugin&quot; in the dynaTrace Client    </p>
</li><li class=" ">    <p>
This will automatically add a System Profile &quot;AmazonAccount&quot; which contains a Montior configuration    </p>
</li><li class=" ">    <p>
Configure the Monitor properties via &quot;AmazonAccount - Edit System Profile - Monitors&quot; and right choose &quot;Edit&quot; on the &quot;Amazon Account&quot; entry (not &quot;Amazon Account Monitor&quot;!)    </p>
</li><li class=" ">    <p>
Enter values for at least accessKeyID and secretKeyID, you get these values in the Amazon AWS console under &quot;Account&quot;.    </p>
</li><li class=" ">    <p>
Optionally you can choose to monitor only certain regions, default is to include all Amazon regions    </p>
</li><li class=" ">    <p>
Instance Costs should be pre-populated, you can adjust them if Amazon changes these costs.    </p>
</li><li class=" ">    <p>
The setting &quot;Persist accross restarts&quot; allows to make the calculation more accurate in cases where dynaTrace is restarted or is offline for some time. This requires a temporary folder (on the machine where the related dynaTrace Collector executes) and a unique id to work properly.    </p>
</li><li class=" ">    <p>
The setting &quot;Use proxy for connecting Amazon AWS&quot; allows to define proxy settings when the dynaTrace Collector is running inside a corporate network with limited Internet access.    </p>
</li></ul>    <p>
            <img src="images_community/download/attachments/64192516/01_amazon_account_monitor_setup.png" alt="images_community/download/attachments/64192516/01_amazon_account_monitor_setup.png" class="" />
            </p>
    </div>
    
    <div class="section-2"  id="64192516_AmazonEC2AccountCostMonitoringFastPack-AmazonEC2AccountCostDashboard"  >
        <h2>Amazon EC2 Account Cost Dashboard</h2>
    <p>
    </p>
    <div class="tablewrap">
        <table>
<thead class=" "></thead><tfoot class=" "></tfoot><tbody class=" ">    <tr>
            <td rowspan="1" colspan="1">
        <p>
            <img src="images_community/download/attachments/64192516/02_amazon_account_monitor_dashboard.png" alt="images_community/download/attachments/64192516/02_amazon_account_monitor_dashboard.png" class="" />
            </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
            <img src="images_community/download/attachments/64192516/03_amazon_account_monitor_report.png" alt="images_community/download/attachments/64192516/03_amazon_account_monitor_report.png" class="" />
            </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
            <img src="images_community/download/attachments/64192516/icon.png" alt="images_community/download/attachments/64192516/icon.png" class="" />
            </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
Predefined Account Monitoring Dashboard    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Report    </p>
            </td>
                <td rowspan="1" colspan="1">
                </td>
        </tr>
</tbody>        </table>
            </div>
    </div>
    <div class="section-2"  id="64192516_AmazonEC2AccountCostMonitoringFastPack-Measures"  >
        <h2>Measures</h2>
    <div class="tablewrap">
        <table>
<thead class=" ">    <tr>
            <td rowspan="1" colspan="1">
        <p>
<strong class=" ">Amazon Cloud Formations</strong>    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Provides metrics about used cloud formation stacks.    </p>
            </td>
        </tr>
</thead><tfoot class=" "></tfoot><tbody class=" ">    <tr>
            <td rowspan="1" colspan="1">
        <p>
FormationActiveCount    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Number of cloud formation stacks that are active, i.e. CREATE_COMPLETE or any of the ..._IN_PROGRESS states.    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
Count_CREATE_IN_PROGRESS    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Number of cloud formation stacks that are currently created.    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
Count_CREATE_FAILED    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Number of cloud formation stacks where creation failed.    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
Count_CREATE_COMPLETE    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Number of cloud formation stacks that are fully created.    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
Count_ROLLBACK_IN_PROGRESS    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Number of cloud formation stacks where rollback is underway.    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
Count_ROLLBACK_FAILED    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Number of cloud formation stacks where rollback failed.    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
Count_ROLLBACK_COMPLETE    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Number of cloud formation stacks where rollback is done.    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
Count_DELETE_IN_PROGRESS    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Number of cloud formation stacks where deleting is underway.    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
Count_DELETE_FAILED    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Number of cloud formation stacks where deleting failed.    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
<strong class=" ">Amazon EC2 Instances</strong>    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Provides metrics about used Amazon EC2 instances.    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
EC2ActiveCount    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Number of EC2 Instances that are not terminated and not stopped.    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
EC2CountStopped    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Number of EC2 Instances that are stopped.    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
EC2CountPending    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Number of EC2 Instances that are currently started.    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
EC2CountRunning    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Number of EC2 Instances that are running.    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
EC2CountShutting-down    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Number of EC2 Instances that are currently shutting down.    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
EC2CountTerminated    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Number of EC2 Instances that are terminated.    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
<strong class=" ">Amazon EC2 Instance Costs</strong>    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Provides metrics for approximate costs of the currently active Amazon EC2 instances.    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
CostOverall    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Cost since last invocation for currently resvered instances which are not terminated or stopped. Note that these are estimated costs as instances that are started and stopped within one hour still cause cost. Also the costs do not include additional costs for storage and transfers.    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
CostM1Small    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Cost since last invocation for instances of type m1.small.    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
CostM1Medium    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Cost since last invocation for instances of type m1.medium.    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
CostM1Large    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Cost since last invocation for instances of type m1.large.    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
CostM1XLarge    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Cost since last invocation for instances of type m1.xlarge.    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
CostM3XLarge    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Cost since last invocation for instances of type m3.xlarge.    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
CostM32XLarge    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Cost since last invocation for instances of type m3.2xlarge.    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
CostT1Micro    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Cost since last invocation for instances of type t1.micro.    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
CostM2XLarge    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Cost since last invocation for instances of type m2.xlarge.    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
CostM22XLarge    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Cost since last invocation for instances of type m2.2xlarge.    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
CostM24XLarge    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Cost since last invocation for instances of type m2.4xlarge.    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
CostC1Medium    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Cost since last invocation for instances of type c1.medium.    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
CostC1XLarge    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Cost since last invocation for instances of type c1.xlarge.    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
CostCC14XLarge    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Cost since last invocation for instances of type cc1.4xlarge.    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
CostCC28XLarge    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Cost since last invocation for instances of type cc2.8xlarge.    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
CostCG14XLarge    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Cost since last invocation for instances of type cg1.4xlarge.    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
CostHI14XLarge    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Cost since last invocation for instances of type hi1.4xlarge.    </p>
            </td>
        </tr>
</tbody>        </table>
            </div>
    </div>
    
    <div class="section-2"  id="64192516_AmazonEC2AccountCostMonitoringFastPack-NewfeaturesinVersion5.5.0"  >
        <h2>New features in Version 5.5.0</h2>
    
<ul class=" "><li class=" ">    <p>
Update Cost Values, add new Amazon AWS data center locations    </p>
</li><li class=" ">    <p>
Try to automatically retrieve current costs from Amazon web page, fall back to configured costs if not found or network is not available    </p>
</li><li class=" ">    <p>
Add some measures for costs for RDS database instances: RDSActiveCount, RDSCountByClass, RDSCostOverall, RDSCostByClass    </p>
</li><li class=" ">    <p>
Provide new dynamic measures to avoid having to update the plugin when amazon introduces new instance types in the future    </p>
<ul class=" "><li class=" ">    <p>
CostOverall by Usage, i.e. custom tag &quot;Usage&quot; on the EC2 Instance    </p>
</li><li class=" ">    <p>
CostOverall by Type, i.e. instance type    </p>
</li><li class=" ">    <p>
CostOverall by Owner, i.e. custom tag &quot;Owner&quot; on the EC2 Instance    </p>
</li><li class=" ">    <p>
EC2ActiveCount by Usage    </p>
</li><li class=" ">    <p>
EC2ActiveCount by Owner    </p>
</li><li class=" ">    <p>
RDSCostOverall by Status    </p>
</li><li class=" ">    <p>
RDSCostByClass by Instance Class    </p>
</li><li class=" ">    <p>
RDSActiveCount by Status    </p>
</li><li class=" ">    <p>
RDSActiveCountByClass by Instance Class    </p>
</li></ul></li><li class=" ">    <p>
Update to Amazon SDK 1.7.3    </p>
</li></ul>    </div>
    
    <div class="section-2"  id="64192516_AmazonEC2AccountCostMonitoringFastPack-KnownProblems%2FLimitations"  >
        <h2>Known Problems/Limitations</h2>
    
<ul class=" "><li class=" ">    <p>
As Amazon does not make the actual running costs available, the cost calculation is only an estimate, the actual values billed by Amazon might vary slightly.    </p>
</li><li class=" ">    <p>
The plugin only calculates cost for the EC2 instances, costs for storage, network traffic and other Amazon Services are not taken into account.    </p>
</li></ul>    <p>
<a href="https://community.compuwareapm.com/community/download/attachments/64192516/amazonaccountmonitor_5.5.0.5227.dtp?api=v2">amazonaccountmonitor_5.5.0.5227.dtp </a>    </p>
    </div>
            </div>
        </div>
        <div class="footer">
            Created with <a href="http://k15t.com/display/en/Scroll-Wiki-HTML-Exporter-for-Confluence-Overview">Scroll Wiki HTML Exporter for Confluence</a>.
        </div>
    </div>
</body>
</html>
