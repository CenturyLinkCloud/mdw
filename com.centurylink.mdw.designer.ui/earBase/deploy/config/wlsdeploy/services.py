#=======================================================================================
# This Jython script is for configuring an MDW domain
# with all JMS resources in a clustered environment with two managed servers.
#
# The arguments provided on the command line to the script are in accordance with
# ECOM standards that call setup scripts via ant
#
# NOTE: YOU NEED TO CHANGE THE FOLLOWING PARAMETERS ACCORDING
# TO YOUR ECOM OR CLUSTERED ENVIRONMENT PRIOR TO RUNNING THE SCRIPT
#=======================================================================================

import os
import re

username = sys.argv[1]
password = sys.argv[2]
adminurl = sys.argv[3]

#=======================================================================================
# Parameters inherited from env.properties.dev
#=======================================================================================

domainname='@DOMAIN_NAME@'
domaindir='@SERVER_ROOT@'

#=======================================================================================
# Properties set locally
#=======================================================================================
jmsBytesHigh=420000000
jmsBytesLow=420000000
jmsBytesMax=640000000
jmsMessagesHigh=-1
jmsMessagesLow=-1
jmsMessagesMax=-1
jmsBlockingSendPolicy='FIFO'
jmsMaxMessageSize=2147483647
topicRedeliveryLimit=3
topicRedeliveryDelay=1800000
templateRedeliveryLimit=3
templateRedeliveryDelay=1800000
connFactoryFlowInterval=30
## For WorkManagers, the list of MaxThreads that can apply to them.  However, if more
## than one work manager has, say, 15 max threads, that only needs to be here once 
maxThreadsConstraints = [1,2,5,10,15]

#=======================================================================================
# Open a domain template.
#=======================================================================================

## Use this first form for running this script in ECOM, where they have user/password stored in files
connect(userConfigFile=username,userKeyFile=password,url=adminurl)

## Use this form for connecting in non-ECOM dev or test using the MDW build.xml and properties files
## connect(username,password,adminurl)

edit()
startEdit()

#=======================================================================================
# Create a JMS Server and file store
#=======================================================================================

cd('/')
adminserver = cmo.getAdminServerName()
allservers = cmo.getServers()
managedservers = []

for x in allservers:
  if x.getName() != adminserver:
    managedservers.append(x)
    print 'Adding Managed Server for configuration: ' + x.getName()

allServerTargets = jarray.array(managedservers, Class.forName("weblogic.management.configuration.TargetMBean"))

singleServerTargets = []

for x in managedservers:
  singleServerTargets.append(jarray.array([x], Class.forName("weblogic.management.configuration.TargetMBean")))

jmsServers = []
count = 1

for target in singleServerTargets:
  jmsDir = File("JMS")
  jmsDir.mkdir()
  filestore = cmo.lookupFileStore('MDWFileStore_' + str(count))
  if filestore == None:
    filestore=create('MDWFileStore_' + str(count), 'FileStore')
  filestore.setDirectory(domaindir);
  filestore.setTargets(target)
  jmsserver = cmo.lookupJMSServer('MDWJMSServer_' + str(count))
  if jmsserver == None:
      jmsserver=create('MDWJMSServer_' + str(count), 'JMSServer')
  jmsserver.setTargets(target)
  jmsserver.setPersistentStore(filestore)
  jmsserver.setBytesThresholdHigh(jmsBytesHigh)
  jmsserver.setBytesThresholdLow(jmsBytesLow)
  jmsserver.setBytesMaximum(jmsBytesMax)
  jmsserver.setMessagesThresholdHigh(jmsMessagesHigh)
  jmsserver.setMessagesThresholdLow(jmsMessagesLow)
  jmsserver.setMessagesMaximum(jmsMessagesMax)
  jmsserver.setBlockingSendPolicy(jmsBlockingSendPolicy)
  jmsserver.setMaximumMessageSize(jmsMaxMessageSize)
  jmsServers.append(jmsserver)
  count = count + 1

print str(jmsServers)
#=======================================================================================
# Create a JMS System resource (JMS module, subdeployment, template)
#=======================================================================================

cd('/')
jmsmod = cmo.lookupJMSSystemResource('MDWJMSModule')
if jmsmod == None:
    jmsmod=create('MDWJMSModule', 'JMSSystemResource')
jmsmod.setTargets(allServerTargets)
jmsmod.setName('MDWJMSModule')

cd('/JMSSystemResources/MDWJMSModule')
subdeploy = cmo.lookupSubDeployment('MDWSubDeployment')
if subdeploy == None:
    subdeploy=create('MDWSubDeployment', 'SubDeployment')
jmsserverTargets = jarray.array(jmsServers, Class.forName("weblogic.management.configuration.TargetMBean"))
subdeploy.setTargets(jmsserverTargets)

#=======================================================================================
# Create JMS Queues.
#=======================================================================================

def checkDefineQueue(qname,jndi) :
    cd('/JMSSystemResources/MDWJMSModule')
    cd('JMSResource/MDWJMSModule')
    theQueue = cmo.lookupUniformDistributedQueue(qname)
    if theQueue == None:
        theQueue = create(qname,'UniformDistributedQueue')
    theQueue.setJNDIName(jndi)
    theQueue.setSubDeploymentName('MDWSubDeployment')

print "Creating queues ..."

checkDefineQueue('MDWProcessHandler','com.qwest.mdw.workflow.process.handler.queue')
checkDefineQueue('MDWExternalEventController','com.qwest.mdw.workflow.external.event.controller.queue')
checkDefineQueue('MDWWorkflowErrorQueue','com.qwest.mdw.workflow.error.queue')


cd('/JMSSystemResources/MDWJMSModule')
cd('JMSResource/MDWJMSModule')
errorqueue = cmo.lookupQueue('MDWWorkflowErrorQueue')

#=======================================================================================
# Create a JMS Topic.
#=======================================================================================

cd('/JMSSystemResources/MDWJMSModule/JMSResource/MDWJMSModule')
myt = cmo.lookupUniformDistributedTopic('MDWConfigHandler')
if myt == None:
  cmo.createUniformDistributedTopic('MDWConfigHandler')

cd('/JMSSystemResources/MDWJMSModule/JMSResource/MDWJMSModule/UniformDistributedTopics/MDWConfigHandler')
cmo.setJNDIName('com.qwest.mdw.workflow.engine.config.topic')
cmo.setLoadBalancingPolicy('Round-Robin')

cd('/SystemResources/MDWJMSModule/SubDeployments/MDWSubDeployment')
set('Targets',jarray.array([ObjectName('com.bea:Name=MDWJMSServer_1,Type=JMSServer'), ObjectName('com.bea:Name=MDWJMSServer_2,Type=JMSServer')], ObjectName))

cd('/JMSSystemResources/MDWJMSModule/JMSResource/MDWJMSModule/UniformDistributedTopics/MDWConfigHandler')
cmo.setSubDeploymentName('MDWSubDeployment')

#=======================================================================================
# Create a JMS Template for use with the queues, and apply it to appropriate queues
#=======================================================================================
cd('/JMSSystemResources/MDWJMSModule')
cd('JMSResource/MDWJMSModule')
template = cmo.lookupTemplate('MDWWorkflowTemplate')
if template == None:
    template = create('MDWWorkflowTemplate', 'Template')

cd('/JMSSystemResources/MDWJMSModule')
cd('JMSResource/MDWJMSModule')
cd('Templates/MDWWorkflowTemplate')
cd('DeliveryParamsOverrides/MDWWorkflowTemplate')
cmo.setRedeliveryDelay(templateRedeliveryDelay)

cd('/JMSSystemResources/MDWJMSModule')
cd('JMSResource/MDWJMSModule')
cd('Templates/MDWWorkflowTemplate')
cd('DeliveryFailureParams/MDWWorkflowTemplate')
cmo.setRedeliveryLimit(templateRedeliveryLimit)
cmo.setErrorDestination(errorqueue)

regularqueues = ['MDWProcessHandler','MDWExternalEventController']
for queue in regularqueues:
    cd('/JMSSystemResources/MDWJMSModule')
    cd('JMSResource/MDWJMSModule')
    cd('UniformDistributedQueues/' + queue)
    set('Template', template)

#=======================================================================================
# Create a JMS Connection Factory.
#=======================================================================================

cd('/JMSSystemResources/MDWJMSModule')
cd('JMSResource/MDWJMSModule')
connectionFactory = cmo.lookupConnectionFactory('MDWConnectionFactory')
if (connectionFactory == None):
    connectionFactory = create('MDWConnectionFactory', 'ConnectionFactory')
connectionFactory.setJNDIName('com.qwest.mdw.jms.ConnectionFactory')
connectionFactory.setSubDeploymentName('MDWSubDeployment')
cd('ConnectionFactories/MDWConnectionFactory/FlowControlParams/MDWConnectionFactory')
cmo.setFlowControlEnabled(true)
cmo.setFlowInterval(connFactoryFlowInterval)
cd('../../LoadBalancingParams/MDWConnectionFactory')
cmo.setLoadBalancingEnabled(true)
cmo.setServerAffinityEnabled(false)


#=======================================================================================
# Configure work managers
#=======================================================================================

## first off we need some MaxThreadsConstraints MBeans.  These are just named according
## to their max thread count.  If you change your thread counts below, you will also need
## to add that count into the below array

for threads in maxThreadsConstraints:
  cd('/SelfTuning/' + domainname)
  threadConstraint = cmo.lookupMaxThreadsConstraint('MaxThreadsConstraint-' + str(threads))
  if threadConstraint == None:
      cmo.createMaxThreadsConstraint('MaxThreadsConstraint-' + str(threads)) 
  cd('/SelfTuning/' + domainname + '/MaxThreadsConstraints/MaxThreadsConstraint-' + str(threads))
  cmo.setTargets(allServerTargets)
  cmo.setCount(threads)

#=======================================================================================
# Configure keystore for Qwest Certificate Agent.
#=======================================================================================

for server in managedservers:
  cd('/Servers/'+ server.getName())
  set('KeyStores','CustomIdentityAndCustomTrust')
  set('CustomIdentityKeyStoreFileName','Qwest/config/QwestCA.jks')
  set('CustomIdentityKeyStorePassPhrase','QwestCA')
  set('CustomIdentityKeyStoreType','JKS')
  set('CustomTrustKeyStoreFileName','Qwest/config/QwestCA.jks')
  set('CustomTrustKeyStorePassPhrase','QwestCA')
  set('CustomTrustKeyStoreType','JKS')

#=======================================================================================
# Set the Cluster to Round-Robin - requires CLUSTER_NAME to be set in env.properties.dev
#=======================================================================================

# cd('/Clusters/' + clustername)
# cmo.setDefaultLoadAlgorithm('round-robin')
  
#=======================================================================================
# Set JTA Timeout.
#=======================================================================================
cd('/JTA/' + domainname)
set('TimeoutSeconds', '300')

#=======================================================================================
# Write the domain and close the domain template.
#=======================================================================================

# updateDomain()
# ls()
try:    
  save()    
  activate()
except:    
  print "Error while trying to save and/or activate."
  dumpStack()
  
#=======================================================================================
# Exit WLST.
#=======================================================================================

disconnect()

