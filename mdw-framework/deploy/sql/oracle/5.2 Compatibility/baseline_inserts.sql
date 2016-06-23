spool baseline_inserts.lst;

insert into event_type values (1,'START');      
insert into event_type values (2,'FINISH');     
insert into event_type values (3,'DELAY');      
insert into event_type values (8,'CORRECT');      
insert into event_type values (4,'ERROR');      
insert into event_type values (5,'ABORT');      
insert into event_type values (6,'RESUME');     
insert into event_type values (7,'HOLD');     

insert into TASK_CATEGORY values (1,'ORD','Ordering');              
insert into TASK_CATEGORY values (9,'TST','Test Category 1');           
insert into TASK_CATEGORY values (10,'VAC','Vacation Planning');            
insert into TASK_CATEGORY values (11,'CNT','Customer Contact');           
insert into TASK_CATEGORY values (2,'GEN','General Inquiry');           
insert into TASK_CATEGORY values (3,'BIL','Billing');             
insert into TASK_CATEGORY values (4,'COM','Complaint');             
insert into TASK_CATEGORY values (5,'POR','Portal Support');            
insert into TASK_CATEGORY values (6,'TRN','Training');              
insert into TASK_CATEGORY values (7,'RPR','Repair');              
insert into TASK_CATEGORY values (8,'INV','Inventory');             

insert into task_state values (1,'Open');               
insert into task_state values (2,'Alert');                
insert into task_state values (3,'Jeopardy');               
insert into task_state values (4,'Closed');                           
insert into task_state values (5,'Invalid');

insert into TASK_STATUS values (1,'Open');
insert into TASK_STATUS values (2,'Assigned');
insert into TASK_STATUS values (4,'Completed');
insert into TASK_STATUS values (5,'Cancelled');
insert into TASK_STATUS values (6,'In Progress');

insert into TASK_TYPE values (1,'WORKFLOW');                
insert into TASK_TYPE values (2,'GUI');                 
insert into TASK_TYPE values (3,'TEMPLATE');

insert into VARIABLE_TYPE values (101,'java.lang.String', sysdate,'BASELINE5.5',null,null,null,'com.centurylink.mdw.common.translator.impl.StringTranslator'); 
insert into VARIABLE_TYPE  values (102,'java.lang.Long', sysdate,'BASELINE5.5',null,null,null,'com.centurylink.mdw.common.translator.impl.LongTranslator');     
insert into VARIABLE_TYPE  values (103,'java.lang.Integer', sysdate,'BASELINE5.5',null,null,null,'com.centurylink.mdw.common.translator.impl.IntegerTranslator'); 
insert into VARIABLE_TYPE  values (104,'java.lang.Boolean', sysdate,'BASELINE5.5',null,null,null,'com.centurylink.mdw.common.translator.impl.BooleanTranslator');  
insert into VARIABLE_TYPE  values (105,'java.util.Date', sysdate,'BASELINE5.5',null,null,null,'com.centurylink.mdw.common.translator.impl.DateTranslator');  
insert into VARIABLE_TYPE  values (109,'java.util.Map', sysdate,'BASELINE5.5',null,null,null,'com.centurylink.mdw.common.translator.impl.StringMapTranslator');         
insert into VARIABLE_TYPE  values (110,'java.net.URI', sysdate, 'BASELINE5.5', null, null, null, 'com.centurylink.mdw.common.translator.impl.URITranslator');
insert into VARIABLE_TYPE  values (111,'java.util.List<String>', sysdate, 'BASELINE5.5', null, null, null, 'com.centurylink.mdw.common.translator.impl.StringListTranslator');
insert into VARIABLE_TYPE  values (112,'java.util.List<Integer>', sysdate, 'BASELINE5.5', null, null, null, 'com.centurylink.mdw.common.translator.impl.IntegerListTranslator');
insert into VARIABLE_TYPE  values (113,'java.util.List<Long>', sysdate, 'BASELINE5.5', null, null, null, 'com.centurylink.mdw.common.translator.impl.LongListTranslator');
insert into VARIABLE_TYPE  values (114,'java.util.Map<String,String>', sysdate, 'BASELINE5.5', null, null, null, 'com.centurylink.mdw.common.translator.impl.StringStringMapTranslator');

-- document variables
insert into VARIABLE_TYPE  values (201,'org.w3c.dom.Document',sysdate,'BASELINE5.5',null,null,null,'com.centurylink.mdw.common.translator.impl.DomDocumentTranslator');
insert into VARIABLE_TYPE  values (202,'org.apache.xmlbeans.XmlObject',sysdate,'BASELINE5.5',null,null,null,'com.centurylink.mdw.common.translator.impl.XmlBeanTranslator');
insert into VARIABLE_TYPE  values (203,'java.lang.Object', sysdate, 'BASELINE5.5', null, null, null, 'com.centurylink.mdw.common.translator.impl.JavaObjectTranslator');
insert into VARIABLE_TYPE  values (204,'org.json.JSONObject', sysdate, 'BASELINE5.5', null, null, null, 'com.centurylink.mdw.common.translator.impl.JsonObjectTranslator');
insert into VARIABLE_TYPE  values (205,'groovy.util.Node', sysdate, 'BASELINE5.5', null, null, null, 'com.centurylink.mdw.common.translator.impl.GroovyNodeTranslator');
insert into VARIABLE_TYPE  values (206,'com.centurylink.mdw.xml.XmlBeanWrapper',sysdate,'BASELINE5.5',null,null,null,'com.centurylink.mdw.common.translator.impl.XmlBeanWrapperTranslator');
insert into VARIABLE_TYPE  values (207,'com.centurylink.mdw.model.StringDocument',sysdate,'BASELINE5.5',null,null,null,'com.centurylink.mdw.common.translator.impl.StringDocumentTranslator');
insert into VARIABLE_TYPE  values (208,'com.centurylink.mdw.model.FormDataDocument',sysdate,'BASELINE5.5',null,null,null,'com.centurylink.mdw.common.translator.impl.FormDataDocumentTranslator');
insert into VARIABLE_TYPE  values (209,'com.centurylink.mdw.model.HTMLDocument',sysdate,'BASELINE5.5',null,null,null,'com.centurylink.mdw.common.translator.impl.HtmlDocumentTranslator');
insert into VARIABLE_TYPE  values (210,'javax.xml.bind.JAXBElement',sysdate,'BASELINE5.5',null,null,null,'com.centurylink.mdw.jaxb.JaxbElementTranslator');
-- requires the mdw-camel bundle installed
insert into VARIABLE_TYPE  values (310,'org.apache.camel.component.cxf.CxfPayload',sysdate,'BASELINE5.5',null,null,null,'com.centurylink.mdw.camel.cxf.CxfPayloadTranslator');

insert into WORK_TRANSITION_STATUS values (1,'Initiated');  
insert into WORK_TRANSITION_STATUS values (6,'Completed');  

insert into WORK_STATUS values (1,'Pending Processing');  
insert into WORK_STATUS values (2,'In Progress');   
insert into WORK_STATUS values (3,'Failed');      
insert into WORK_STATUS values (4,'Completed');     
insert into WORK_STATUS values (5,'Cancelled');     
insert into WORK_STATUS values (6,'Hold');  
insert into WORK_STATUS values (7,'Waiting');   
insert into WORK_STATUS values (32,'Purge');

insert into RESOURCE_TYPE values('PROCESS', 'process', 0, 'Process Definition');
insert into RESOURCE_TYPE values('IMAGE_JPEG', 'jpg', 1, 'JPEG Image');
insert into RESOURCE_TYPE values('IMAGE_GIF', 'gif', 1, 'GIF Image');
insert into RESOURCE_TYPE values('IMAGE_PNG', 'png', 0, 'PNG Image');
insert into RESOURCE_TYPE values('JAVASCRIPT', 'js', 0, 'JavaScript script');
insert into RESOURCE_TYPE values('MAGICBOX', 'mbr', 0, 'Magic Box Rules');
insert into RESOURCE_TYPE values('FACELET', 'xhtml', 0, 'Facelet XHTML');
insert into RESOURCE_TYPE values('CSS', 'css', 0, 'Cascading Style Sheet');
insert into RESOURCE_TYPE values('HTML', 'html', 0, 'HTML file and fragment');
insert into RESOURCE_TYPE values('MILESTONE_REPORT', 'milestone_report', 0, 'Milestone report definition');
insert into RESOURCE_TYPE values('GROOVY', 'groovy', 0, 'Groovy Script');
insert into RESOURCE_TYPE values('FORM', 'form', 0, 'Pagelet form definition');
insert into RESOURCE_TYPE values('USECASE', 'usecase', 0, 'Use Cases');
insert into RESOURCE_TYPE values('VELOCITY', 'velocity', 0, 'Velocity Templates');
insert into RESOURCE_TYPE values('JSP', 'jsp', 0, 'Java Server Page');
insert into RESOURCE_TYPE values('CONFIG', 'config', 0, 'Configuration file');
insert into RESOURCE_TYPE values('EXCEL', 'xls', 1, 'Microsoft Excel 2003');
insert into RESOURCE_TYPE values('EXCEL_2007', 'xlsx', 1, 'Microsoft Excel 2007');
insert into RESOURCE_TYPE values('MS_WORD', 'docx', 1, 'Microsoft Word');
insert into RESOURCE_TYPE values('CSV', 'csv', 0, 'Excel CSV file');
insert into RESOURCE_TYPE values('BIRT', 'rptdesign', 0, 'BIRT');
insert into RESOURCE_TYPE values('TESTDATA', 'testdata', 0, 'Messages for MDW Tester');
insert into RESOURCE_TYPE values('JAR', 'jar', 1, 'Java JAR Archive');
insert into RESOURCE_TYPE values('JAVA', 'java', 0, 'Dynamic Java code');
insert into RESOURCE_TYPE values('DROOLS', 'drools', 0, 'DROOLS DRL');
insert into RESOURCE_TYPE values('ATTRIBUTE_OVERFLOW', 'n/a', 0, 'for storing large attribute');
insert into resource_type values('XML', 'xml', 0, 'XML Document');
insert into resource_type values('XSD', 'xsd', 0, 'XSD Document');
insert into resource_type values('WSDL', 'wsdl', 0, 'WSDL Document');
insert into resource_type values('TEXT', 'txt', 0, 'Text Document');
insert into resource_type values('CAMEL_ROUTE_SPRING', 'xml', 0, 'Camel Route');
insert into resource_type values('PAGELET', 'xml', 0, 'Pagelet');
insert into resource_type values('JSON', 'json', 0, 'JSON');
insert into resource_type values('SPRING_CONFIG', 'xml', 0, 'Spring Config');

INSERT INTO USER_ROLE (USER_ROLE_ID,USER_ROLE_NAME,CREATE_USR,COMMENTS) 
    values (6,'Supervisor', 'BASELINE5.5', 'reassign tasks, acting as a group member');
INSERT INTO USER_ROLE (USER_ROLE_ID,USER_ROLE_NAME,CREATE_USR,COMMENTS) 
    values (7,'Process Design', 'BASELINE5.5', 'define processes'); 
INSERT INTO USER_ROLE (USER_ROLE_ID,USER_ROLE_NAME,CREATE_USR,COMMENTS) 
    values (8,'Process Execution', 'BASELINE5.5', 'execute processes');
INSERT INTO USER_ROLE (USER_ROLE_ID,USER_ROLE_NAME,CREATE_USR,COMMENTS) 
    values (9,'User Admin', 'BASELINE5.5', 'manage users, groups and roles');
INSERT INTO USER_ROLE (USER_ROLE_ID,USER_ROLE_NAME,CREATE_USR,COMMENTS) 
    values (10,'Task Execution', 'BASELINE5.5','perform manual tasks');
       
insert into user_group (USER_GROUP_ID,GROUP_NAME,CREATE_USR,COMMENTS)
	values (1,'MDW Support','BASELINE5.5','MDW Support Group');
INSERT INTO USER_GROUP (USER_GROUP_ID,GROUP_NAME,CREATE_USR,COMMENTS)
     values (2,'Site Admin','BASELINE5.5','site administrator');

commit;

spool off;
