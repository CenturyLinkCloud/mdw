-- NOTICE: Be advised that the MDW 5.2 codebase is not compatible with a 5.5 database.
--         In other words, once you've run this script you must use MDW 5.5 code. 
spool upgrade_52_to_55.lst;

ALTER TABLE RULE_SET ADD (OWNER_ID NUMBER(20), OWNER_TYPE VARCHAR2(30 BYTE));
alter table package drop column JAR_LOCATION;
alter table package add GROUP_NAME	VARCHAR2(80 BYTE);

-- for compatibility with vcs-based assets
alter table process_instance modify (process_id number(16));
alter table task_instance modify (task_id number(16));
alter table task_instance drop constraint task_id_fk;
alter table attribute modify (attribute_owner_id number(16));
alter table attribute modify (ATTRIBUTE_NAME  VARCHAR2(500 CHAR));
alter table task_instance modify (TASK_INSTANCE_REFERRED_AS VARCHAR2(500 BYTE));



CREATE INDEX DOCUMENT_OWNER_ID_IDX ON DOCUMENT (OWNER_ID);
CREATE INDEX EVENTWAITINST_OWNERID_IDX ON EVENT_WAIT_INSTANCE (EVENT_WAIT_INSTANCE_OWNER_ID);

insert into VARIABLE_TYPE  values (111,'java.util.List<String>', sysdate, 'BASELINE5.5', null, null, null, 'com.centurylink.mdw.common.translator.impl.StringListTranslator');
insert into VARIABLE_TYPE  values (112,'java.util.List<Integer>', sysdate, 'BASELINE5.5', null, null, null, 'com.centurylink.mdw.common.translator.impl.IntegerListTranslator');
insert into VARIABLE_TYPE  values (113,'java.util.List<Long>', sysdate, 'BASELINE5.5', null, null, null, 'com.centurylink.mdw.common.translator.impl.LongListTranslator');
insert into VARIABLE_TYPE  values (114,'java.util.Map<String,String>', sysdate, 'BASELINE5.5', null, null, null, 'com.centurylink.mdw.common.translator.impl.StringStringMapTranslator');

insert into VARIABLE_TYPE  values (206,'com.centurylink.mdw.xml.XmlBeanWrapper',sysdate,'UPGRADE5.5',null,null,null,'com.centurylink.mdw.common.translator.impl.XmlBeanWrapperTranslator');
insert into VARIABLE_TYPE  values (207,'com.centurylink.mdw.model.StringDocument',sysdate,'UPGRADE5.5',null,null,null,'com.centurylink.mdw.common.translator.impl.StringDocumentTranslator');
insert into VARIABLE_TYPE  values (208,'com.centurylink.mdw.model.FormDataDocument',sysdate,'UPGRADE5.5',null,null,null,'com.centurylink.mdw.common.translator.impl.FormDataDocumentTranslator');
insert into VARIABLE_TYPE  values (209,'com.centurylink.mdw.model.HTMLDocument',sysdate,'UPGRADE5.5',null,null,null,'com.centurylink.mdw.common.translator.impl.HtmlDocumentTranslator');
insert into VARIABLE_TYPE  values (210,'javax.xml.bind.JAXBElement',sysdate,'UPGRADE5.5',null,null,null,'com.centurylink.mdw.jaxb.JaxbElementTranslator');
-- requires the mdw-camel bundle installed
insert into VARIABLE_TYPE  values (310,'org.apache.camel.component.cxf.CxfPayload',sysdate,'UPGRADE5.5',null,null,null,'com.centurylink.mdw.camel.cxf.CxfPayloadTranslator');

insert into task_state values (5,'Invalid');

ALTER TABLE rule_set DROP CONSTRAINT RULESET_LANGUAGE_FK;
ALTER TABLE rule_set DROP CONSTRAINT RULESET_GROUP_FK;

commit;

spool off;