DROP procedure IF EXISTS `mysql_cleanup`;
DELIMITER $$
CREATE PROCEDURE `mysql_cleanup`(IN inputrnum INT , inputdaydiff INT, inputeldaydiff INT,
inputprocid INT, inputworkstatusids VARCHAR (100) , inputcommitcnt INT)
BEGIN
DECLARE dt                 DATETIME;
   DECLARE rnum               INT;
   DECLARE daydiff            INT;
   DECLARE eldaydiff          INT;
   DECLARE procid             INT           DEFAULT  0;
   DECLARE purgestatusid      INT           DEFAULT  32;
   DECLARE commitcnt          INT;
   DECLARE row_count          INT;
   DECLARE workstatusids      VARCHAR (100);
   DECLARE dynsql             VARCHAR (1000);
   DECLARE processidclause    VARCHAR (1000);
   DECLARE workstatusclause   VARCHAR (1000);
   DECLARE table_exist        VARCHAR (100);
	
 SET rnum := inputrnum;
 SET daydiff := inputdaydiff;
 SET eldaydiff := inputeldaydiff;
 SET procid := inputprocid; 
 SET workstatusids := inputworkstatusids;  
 SET commitcnt := inputcommitcnt;  
 
--   rnum := 50000;
--   daydiff := 14;  -- process
--   eldaydiff := 14; -- event
--   --procid := 11455;
--   workstatusids := '3,4,5';  
--  commitCount := 10000;
 
SET SQL_SAFE_UPDATES = 0;
SET foreign_key_checks=0;

 SELECT SYSDATE()
     INTO dt
     FROM DUAL;
   SELECT (CONCAT('Start Purging Process -->' , ifnull(dt, '')));
   SELECT (CONCAT('Start Marking Process Instances to Purge:' , ifnull(dt, '')));
 IF workstatusids IS NOT NULL
   THEN
      SET workstatusclause = Concat('and status_cd in ( ' , ifnull(workstatusids, '') , ') ');
   ELSE
      SET workstatusclause = 'and status_cd = 4 ';
   END IF;

   IF procid > 0
   THEN
      SET processidclause = Concat('and process_id = ' , ifnull(procid, '') , ' ');
   END IF;


   SET @dynsql =
         Concat('update process_instance'
      , ' set status_cd = '
      , ifnull(purgestatusid, '')
      , ' where end_dt < '
      ,'DATE_SUB(DATE(NOW()), ' 
      , 'INTERVAL '
      , ifnull(daydiff, '')
      , ' DAY ) '
      , ifnull(workstatusclause, '')
      , ifnull(processidclause, '')
      ,'limit ' 
      , rnum);
      
   SELECT ( CONCAT('Dynamic sql is: ' , ifnull(@dynsql, '')));

   PREPARE stmt1 FROM @dynsql; 
	EXECUTE stmt1; 
	 COMMIT;
	DEALLOCATE PREPARE stmt1; 

   SELECT SYSDATE()
     INTO dt
     FROM DUAL;

   SELECT ( CONCAT('Finish Marking Process Instance to Purge:' , ifnull(dt, '')));
   SELECT ( CONCAT('Start Purging Event Records:' , ifnull(dt, '')));

   -- mdw tables that need to be cleaned up based on date range   
   SET row_count = 0;
   REPEAT
   	COMMIT;
   	 DELETE e1 FROM event_log e1 JOIN
     	(SELECT e2.event_log_id FROM event_log e2
         	WHERE e2.create_dt < DATE_SUB(CURDATE(), INTERVAL eldaydiff DAY)
         LIMIT commitcnt) e2
     USING (event_log_id);
     SET row_count = row_count + ROW_COUNT();
   UNTIL ROW_COUNT() < 1 END REPEAT;
   
   SELECT ( CONCAT('Number of rows deleted from EVENT_LOG: ', row_count));
   COMMIT;

   SET row_count = 0;
   REPEAT
   	 COMMIT;
   	 DELETE e1 FROM event_instance e1 JOIN 
   	 	(SELECT e2.event_name FROM event_instance e2
         	WHERE e2.create_dt < DATE_SUB(CURDATE(), INTERVAL eldaydiff DAY) 
         	AND e2.event_name NOT LIKE 'ScheduledJob%' 
         	LIMIT commitcnt) e3  
	 USING (event_name);
     SET row_count = row_count + ROW_COUNT();
   UNTIL ROW_COUNT() < 1 END REPEAT;
   
   SELECT ( CONCAT('Number of rows deleted from EVENT_INSTANCE:', row_count));
   COMMIT;                     
   
   -- delete the event wait instance table
   SET row_count = 0;
   REPEAT
     COMMIT;
     DELETE e1 FROM event_wait_instance e1 JOIN
     	(SELECT e2.event_wait_instance_id FROM event_wait_instance e2 
         	WHERE e2.event_wait_instance_owner = 'ACTIVITY_INSTANCE'
           	AND e2.event_wait_instance_owner_id IN (
                  SELECT a1.activity_instance_id
                    FROM activity_instance a1 
                   	WHERE a1.process_instance_id IN (
                 		SELECT p1.process_instance_id
                        	FROM process_instance p1
                            WHERE p1.status_cd = purgestatusid))
            LIMIT commitcnt) e3
        USING (event_wait_instance_id);
     SET row_count = row_count + ROW_COUNT();
   UNTIL ROW_COUNT() < 1 END REPEAT;                                                             

   SELECT(   CONCAT('Number of rows deleted from EVENT_WAIT_INSTANCE: ', row_count));  
	 COMMIT;                    
   
   SELECT SYSDATE()
     INTO dt
     FROM DUAL;
      SELECT ( CONCAT('Start Purging Process Instance Child Records:' , ifnull(dt, '')));

   -- delete all the activity instances
   SET row_count = 0;
   REPEAT
   	 COMMIT;
   	 DELETE a1 FROM activity_instance a1 JOIN
     	(SELECT a2.activity_instance_id FROM activity_instance a2 
         	WHERE a2.process_instance_id IN (SELECT p1.process_instance_id
                                            FROM process_instance p1
                                           WHERE p1.status_cd = purgestatusid)
            LIMIT commitcnt) a3
        USING (activity_instance_id);
     SET row_count = row_count + ROW_COUNT();
   UNTIL ROW_COUNT() < 1 END REPEAT;                                               

   SELECT ( CONCAT('Number of rows deleted from ACTIVITY_INSTANCE: ', row_count));
   COMMIT;
   
   -- delete all the work transition instances that belong to current process instance and child instanes
   SET row_count = 0;
   REPEAT
   	 COMMIT;
   	 DELETE w1 FROM work_transition_instance w1 JOIN
     	(SELECT w2.WORK_TRANS_INST_ID FROM work_transition_instance w2 
         	WHERE w2.process_inst_id IN (SELECT p1.process_instance_id
                                         FROM process_instance p1
                                        WHERE p1.status_cd = purgestatusid)
            LIMIT commitcnt) w3
        USING (WORK_TRANS_INST_ID);
     SET row_count = row_count + ROW_COUNT();
   UNTIL ROW_COUNT() < 1 END REPEAT;     
   SELECT( CONCAT('Number of rows deleted from WORK_TRANSITION_INSTANCE: ', row_count));           
   COMMIT;


   -- delete all the variable instances that belong to current process instance and child instanes
   SET row_count = 0;
   REPEAT
   	 COMMIT;
   	 DELETE v1 FROM variable_instance v1 JOIN
     	(SELECT v2.variable_inst_id FROM variable_instance v2 
         	WHERE v2.process_inst_id IN (SELECT p1.process_instance_id
                                        FROM process_instance p1
                                       WHERE p1.status_cd = purgestatusid)
            LIMIT commitcnt) v3
        USING (variable_inst_id);
     SET row_count = row_count + ROW_COUNT();
   UNTIL ROW_COUNT() < 1 END REPEAT; 
   SELECT ( CONCAT('Number of rows deleted from VARIABLE_INSTANCE: ', row_count));                  
   COMMIT;
   
   SELECT SYSDATE()
     INTO dt
     FROM DUAL;

   SELECT ( CONCAT('Start Purging Task Instances and Related:' , ifnull(dt, ''))); 

   -- delete all the instance notes for the task instances
   SET row_count = 0;
   REPEAT
   	 COMMIT;
   	 DELETE i1 FROM instance_note i1 JOIN
     	(SELECT i2.instance_note_id FROM instance_note i2
         	WHERE i2.instance_note_owner_id IN (
                  SELECT ti.task_instance_id
                    FROM task_instance ti
                   WHERE ti.task_instance_owner = 'PROCESS_INSTANCE'
                     AND ti.task_instance_owner_id IN (
                                               SELECT p1.process_instance_id
                                                 FROM process_instance p1
                                                WHERE p1.status_cd = purgestatusid))                                           
            LIMIT commitcnt) i3
       USING (instance_note_id);
     SET row_count = row_count + ROW_COUNT();
   UNTIL ROW_COUNT() < 1 END REPEAT; 
   
   SELECT ( CONCAT('Number of rows deleted from INSTANCE_NOTE: ', row_count));
   COMMIT;
   
  SELECT table_name
     INTO table_exist
     FROM information_schema.tables
    WHERE table_name = 'INSTANCE_INDEX';
    
   IF table_exist IS NOT NULL
   THEN
   
   -- delete all task instance indices
     SET row_count = 0;
	   REPEAT
	   	 COMMIT;
	     DELETE i1 FROM instance_index i1 JOIN
	     	(SELECT INSTANCE_ID,OWNER_TYPE,INDEX_KEY FROM instance_index i2
         		WHERE i2.owner_type='TASK_INSTANCE' and i2.instance_id IN (
                  SELECT ti.task_instance_id
                    FROM task_instance ti
                   WHERE ti.task_instance_owner = 'PROCESS_INSTANCE'
                     AND ti.task_instance_owner_id IN (
                                               SELECT p1.process_instance_id
                                                 FROM process_instance p1
                                                WHERE p1.status_cd = purgestatusid))
             	LIMIT commitcnt) i3
        	USING (INSTANCE_ID,OWNER_TYPE,INDEX_KEY);
        SET row_count = row_count + ROW_COUNT();
     UNTIL ROW_COUNT() < 1 END REPEAT;                                                                  
		 SELECT ( CONCAT('Number of rows deleted from INSTANCE_INDEX:', row_count));             
		 COMMIT;
      
   -- delete all task instance group mappings
     SET row_count = 0;
     REPEAT
       COMMIT;
       DELETE tg1 FROM task_inst_grp_mapp tg1 JOIN
       	(SELECT tg2.TASK_INSTANCE_ID, tg2.USER_GROUP_ID FROM task_inst_grp_mapp tg2
         	WHERE tg2.task_instance_id IN (
                  SELECT ti.task_instance_id
                    FROM task_instance ti
                   WHERE ti.task_instance_owner = 'PROCESS_INSTANCE'
                     AND ti.task_instance_owner_id IN (
                                               SELECT p1.process_instance_id
                                                 FROM process_instance p1
                                                WHERE p1.status_cd = purgestatusid))
        	LIMIT commitcnt) tg3
        USING (TASK_INSTANCE_ID,USER_GROUP_ID);
        SET row_count = row_count + ROW_COUNT();
     UNTIL ROW_COUNT() < 1 END REPEAT;     
	   SELECT ( CONCAT('Number of rows deleted from TASK_INST_GRP_MAPP: ', row_count));            
	   COMMIT;
   END IF;

   -- delete all the taskInstances that belong to current process instance and child instanes
   SET row_count = 0;
   REPEAT
   	 COMMIT;
   	 DELETE t1 FROM task_instance t1 JOIN
     	(SELECT t2.task_instance_id FROM task_instance t2 
         	WHERE t2.task_instance_owner = 'PROCESS_INSTANCE'
           	AND t2.task_instance_owner_id IN (SELECT p1.process_instance_id
                                               FROM process_instance p1
                                              WHERE p1.status_cd = purgestatusid)
      		LIMIT commitcnt) t3
      	USING (task_instance_id);
      SET row_count = row_count + ROW_COUNT();
   UNTIL ROW_COUNT() < 1 END REPEAT;  

   SELECT ( CONCAT('Number of rows deleted from TASK_INSTANCE: ', row_count));
   COMMIT;
   
 SELECT SYSDATE()
     INTO dt
     FROM DUAL;

   SELECT ( CONCAT('Start Purging Document_Content Records: ' , ifnull(dt, '')));

   
   -- deleting from document_content to avoid the integrity constraint issue
   SET row_count = 0;
   REPEAT
   	 COMMIT;
   	 DELETE dc1 FROM document_content dc1 JOIN 
      ( SELECT a.document_id FROM 
		( SELECT doc.document_id FROM document doc JOIN document_content dc USING (document_id)
       -- 1. all documents with owner_id being a process instance ID marked for deletion
       		WHERE  ( ( doc.owner_id != 0 AND doc.OWNER_TYPE IN ('PROCESS_INSTANCE', 'PROCESS_RUN','LISTENER_REQUEST')  
               AND EXISTS (
                       SELECT
                              process_instance_id
                         FROM process_instance pi
                        WHERE pi.process_instance_id = doc.owner_id
                          AND pi.status_cd = purgestatusid)
               )
           -- 2. all documents with owner type of VARIABLE_INSTANCE where row in VARIABLE_INSTANCE table has been deleted
            OR ( doc.owner_id != 0 AND doc.OWNER_TYPE = 'VARIABLE_INSTANCE'
            	AND NOT EXISTS (
            				SELECT variable_inst_id
            				FROM variable_instance
            				WHERE variable_inst_id = doc.owner_id)
               )
           -- 3. all documents with owner type of TASK_INSTANCE where row in TASK_INSTANCE table has been deleted
            OR ( doc.owner_id != 0 AND doc.OWNER_TYPE = 'TASK_INSTANCE'
            	AND NOT EXISTS (
            				SELECT task_instance_id
            				FROM task_instance
            				WHERE task_instance_id = doc.owner_id)
               )   
           -- 4. all documents with owner type of ACTIVITY_INSTANCE/ADAPTER_REQUEST/ADAPTER_RESPONSE/INTERNAL_EVENT where row 
           --		in ACTIVITY_INSTANCE table has been deleted
            OR ( doc.owner_id != 0 AND doc.OWNER_TYPE IN('ACTIVITY_INSTANCE','ADAPTER_REQUEST','ADAPTER_RESPONSE','INTERNAL_EVENT')
            	AND NOT EXISTS (
            				SELECT activity_instance_id
            				FROM activity_instance
            				WHERE activity_instance_id = doc.owner_id)
               )    
       	   -- 5. all documents with LISTENER_REQUEST/USER/TASK_INSTANCE/LISTENER_REQUEST_META/LISTENER_RESPONSE/VARIABLE_INSTANCE/INTERNAL_EVENT
       	   --    as owner type and no owner id meeting DATE criteria 
            OR ( doc.create_dt < DATE_SUB(CURDATE(), INTERVAL daydiff DAY)  
                AND doc.owner_id = 0 
                AND doc.owner_type IN ('LISTENER_REQUEST', 'USER', 'TASK_INSTANCE','LISTENER_REQUEST_META','LISTENER_RESPONSE','VARIABLE_INSTANCE','INTERNAL_EVENT')
               )
       		-- 6. all documents with LISTENER_RESPONSE/DOCUMENT/ *_META as owner type and owner is deleted
            OR ( doc.owner_id != 0 AND doc.owner_type IN 
            	('LISTENER_RESPONSE','LISTENER_RESPONSE_META','LISTENER_REQUEST_META','DOCUMENT','ADAPTER_REQUEST_META','ADAPTER_RESPONSE_META')
                AND NOT EXISTS (SELECT document_id
                                FROM document_content doc2
                                WHERE doc2.document_id = doc.owner_id)
               ) 
             )
             -- 7. the document (event message) is not tied to an event_instance
             AND NOT EXISTS (SELECT e1.document_id 
             					FROM event_instance e1 
             					WHERE e1.document_id = doc.document_id)
		   ) a 
         LIMIT commitcnt
      ) dc2 USING (document_id);    
      SET row_count = row_count + ROW_COUNT();
   UNTIL ROW_COUNT() < 1 END REPEAT; 
   
   SELECT
      ( CONCAT('Number of rows deleted from DOCUMENT_CONTENT: ', row_count));    
	 COMMIT;
   
   SELECT SYSDATE()
     INTO dt
     FROM DUAL;

   SELECT ( CONCAT('Start Purging Document Records: ' , ifnull(dt, '')));
   
   -- delete DOCUMENT 
   SET row_count = 0;
   REPEAT
   	 COMMIT;
   	 DELETE dc1 FROM document dc1 JOIN     
		( SELECT doc.document_id FROM document doc
       -- 1. all documents with owner_id being a process instance ID marked for deletion
       		WHERE  ( ( doc.owner_id != 0 AND doc.OWNER_TYPE IN ('PROCESS_INSTANCE', 'PROCESS_RUN','LISTENER_REQUEST')  
               AND EXISTS (
                       SELECT
                              process_instance_id
                         FROM process_instance pi
                        WHERE pi.process_instance_id = doc.owner_id
                          AND pi.status_cd = purgestatusid)
               )
           -- 2. all documents with owner type of VARIABLE_INSTANCE where row in VARIABLE_INSTANCE table has been deleted
            OR ( doc.owner_id != 0 AND doc.OWNER_TYPE = 'VARIABLE_INSTANCE'
            	AND NOT EXISTS (
            				SELECT variable_inst_id
            				FROM variable_instance
            				WHERE variable_inst_id = doc.owner_id)
               )
           -- 3. all documents with owner type of TASK_INSTANCE where row in TASK_INSTANCE table has been deleted
            OR ( doc.owner_id != 0 AND doc.OWNER_TYPE = 'TASK_INSTANCE'
            	AND NOT EXISTS (
            				SELECT task_instance_id
            				FROM task_instance
            				WHERE task_instance_id = doc.owner_id)
               )   
           -- 4. all documents with owner type of ACTIVITY_INSTANCE/ADAPTER_REQUEST/ADAPTER_RESPONSE/INTERNAL_EVENT where row 
           --		in ACTIVITY_INSTANCE table has been deleted
            OR ( doc.owner_id != 0 AND doc.OWNER_TYPE IN('ACTIVITY_INSTANCE','ADAPTER_REQUEST','ADAPTER_RESPONSE','INTERNAL_EVENT')
            	AND NOT EXISTS (
            				SELECT activity_instance_id
            				FROM activity_instance
            				WHERE activity_instance_id = doc.owner_id)
               )    
       	   -- 5. all documents with LISTENER_REQUEST/USER/TASK_INSTANCE/LISTENER_REQUEST_META/LISTENER_RESPONSE/VARIABLE_INSTANCE/INTERNAL_EVENT
       	   --    as owner type and no owner id meeting DATE criteria 
            OR ( doc.create_dt < DATE_SUB(CURDATE(), INTERVAL daydiff DAY)  
                AND doc.owner_id = 0 
                AND doc.owner_type IN ('LISTENER_REQUEST', 'USER', 'TASK_INSTANCE','LISTENER_REQUEST_META','LISTENER_RESPONSE','VARIABLE_INSTANCE','INTERNAL_EVENT')
               )
       		-- 6. all documents with LISTENER_RESPONSE/DOCUMENT/ *_META as owner type and owner is deleted
            OR ( doc.owner_id != 0 AND doc.owner_type IN 
            	('LISTENER_RESPONSE','LISTENER_RESPONSE_META','LISTENER_REQUEST_META','DOCUMENT','ADAPTER_REQUEST_META','ADAPTER_RESPONSE_META')
                AND NOT EXISTS (SELECT document_id
                                  FROM document doc2
                                 WHERE doc2.document_id = doc.owner_id)
               ) 
			)
			-- 7. the document doesn't exists in document_content
			AND NOT EXISTS (SELECT document_id
                                  FROM document_content dc
                                 WHERE dc.document_id = doc.document_id)
            -- 8. the document (event message) is not tied to an event_instance
            AND NOT EXISTS (SELECT e1.document_id 
             					FROM event_instance e1 
             					WHERE e1.document_id = doc.document_id)
         LIMIT commitcnt
      ) dc2 USING (document_id);
      SET row_count = row_count + ROW_COUNT();
   UNTIL ROW_COUNT() < 1 END REPEAT;           

   SELECT
      ( CONCAT('Number of rows deleted from DOCUMENT: ', row_count));
	 COMMIT;
   
   SELECT SYSDATE()
     INTO dt
     FROM DUAL;

   SELECT ( CONCAT('Start Purging Process Instances Themselves:' , ifnull(dt, '')));

   -- delete the process instance
   SET row_count = 0;
   REPEAT
   	COMMIT;
   	DELETE p1 FROM process_instance p1 JOIN
    	(SELECT p2.process_instance_id FROM process_instance p2 
         	WHERE p2.status_cd = purgestatusid
      		LIMIT commitcnt) p3
      	USING (process_instance_id);
      SET row_count = row_count + ROW_COUNT();
   UNTIL ROW_COUNT() < 1 END REPEAT; 
   
   SELECT ( CONCAT('Number of rows deleted from PROCESS_INSTANCE: ', row_count));            
   COMMIT;
   
   SELECT SYSDATE()
     INTO dt
     FROM DUAL;
     
   SELECT ( CONCAT('Finish Purging Process -->', dt));
   
END$$

DELIMITER ;