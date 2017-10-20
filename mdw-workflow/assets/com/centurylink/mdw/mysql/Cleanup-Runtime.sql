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
	SELECT 'test data'; 
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
     DELETE FROM event_log
         WHERE create_dt < DATE_SUB(CURDATE(), INTERVAL eldaydiff DAY)
         LIMIT commitcnt;
     SET row_count = row_count + ROW_COUNT();
   UNTIL ROW_COUNT() < 1 END REPEAT;
   
   SELECT ( CONCAT('Number of rows deleted from EVENT_LOG: ', row_count));
   COMMIT;

   SET row_count = 0;
   REPEAT
     DELETE FROM event_instance
         WHERE create_dt < DATE_SUB(CURDATE(), INTERVAL eldaydiff DAY) 
         AND  event_name NOT LIKE 'ScheduledJob%'
		 LIMIT commitcnt;
     SET row_count = row_count + ROW_COUNT();
   UNTIL ROW_COUNT() < 1 END REPEAT;
   
   SELECT ( CONCAT('Number of rows deleted from EVENT_INSTANCE:', row_count));
   COMMIT;                     
   
   -- delete the event wait instance table
   SET row_count = 0;
   REPEAT
     DELETE from   event_wait_instance 
         WHERE event_wait_instance_owner = 'ACTIVITY_INSTANCE'
           AND event_wait_instance_owner_id IN (
                  SELECT activity_instance_id
                    FROM activity_instance 
                   WHERE process_instance_id IN (
                                               SELECT /*+ index(process_instance PI_STATUS_CD_IDX) */
                                                      process_instance_id
                                                 FROM process_instance
                                                WHERE status_cd = purgestatusid))
     LIMIT commitcnt;
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
     DELETE  from  activity_instance 
         WHERE process_instance_id IN (SELECT /*+ index(process_instance PI_STATUS_CD_IDX) */
                                                 process_instance_id
                                            FROM process_instance
                                           WHERE status_cd = purgestatusid)
                                           LIMIT commitcnt;
     SET row_count = row_count + ROW_COUNT();
   UNTIL ROW_COUNT() < 1 END REPEAT;                                               

   SELECT ( CONCAT('Number of rows deleted from ACTIVITY_INSTANCE: ', row_count));
   COMMIT;
   
   -- delete all the work transition instances that belong to current process instance and child instanes
   SET row_count = 0;
   REPEAT
     DELETE  from    work_transition_instance 
         WHERE process_inst_id IN (SELECT /*+ index(process_instance PI_STATUS_CD_IDX) */
                                              process_instance_id
                                         FROM process_instance
                                        WHERE status_cd = purgestatusid)
                                           LIMIT commitcnt;
     SET row_count = row_count + ROW_COUNT();
   UNTIL ROW_COUNT() < 1 END REPEAT;     
   SELECT( CONCAT('Number of rows deleted from WORK_TRANSITION_INSTANCE: ', row_count));           
   COMMIT;


   -- delete all the variable instances that belong to current process instance and child instanes
   SET row_count = 0;
   REPEAT
     DELETE from   variable_instance 
         WHERE process_inst_id IN (SELECT /*+ index(process_instance PI_STATUS_CD_IDX) */
                                             process_instance_id
                                        FROM process_instance
                                       WHERE status_cd = purgestatusid)
                                           LIMIT commitcnt;
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
     DELETE  from instance_note
         WHERE instance_note_owner_id IN (
                  SELECT task_instance_id
                    FROM task_instance ti
                   WHERE ti.task_instance_owner = 'PROCESS_INSTANCE'
                     AND ti.task_instance_owner_id IN (
                                               SELECT /*+ index(process_instance PI_STATUS_CD_IDX) */
                                                      process_instance_id
                                                 FROM process_instance
                                                WHERE status_cd =
                                                                 purgestatusid))                                           
                                                                 LIMIT commitcnt;
     SET row_count = row_count + ROW_COUNT();
   UNTIL ROW_COUNT() < 1 END REPEAT; 
   
   SELECT ( CONCAT('Number of rows deleted from INSTANCE_NOTE: ', row_count));
   COMMIT;
   
  SELECT table_name
     INTO table_exist
     FROM information_schema.tables
    WHERE table_name = 'TASK_INST_INDEX';
    
   IF table_exist IS NOT NULL
   THEN
   
   -- delete all task instance indices
     SET row_count = 0;
	   REPEAT
	     DELETE  from    task_inst_index
         WHERE task_instance_id IN (
                  SELECT task_instance_id
                    FROM task_instance ti
                   WHERE ti.task_instance_owner = 'PROCESS_INSTANCE'
                     AND ti.task_instance_owner_id IN (
                                               SELECT /*+ index(process_instance PI_STATUS_CD_IDX) */
                                                      process_instance_id
                                                 FROM process_instance
                                                WHERE status_cd =
                                                                 purgestatusid))
        LIMIT commitcnt;
        SET row_count = row_count + ROW_COUNT();
     UNTIL ROW_COUNT() < 1 END REPEAT;                                                                  
		 SELECT ( CONCAT('Number of rows deleted from TASK_INST_INDEX:', row_count));             
		 COMMIT;
      
   -- delete all task instance group mappings
     SET row_count = 0;
     REPEAT
       DELETE  from    task_inst_grp_mapp
         WHERE task_instance_id IN (
                  SELECT task_instance_id
                    FROM task_instance ti
                   WHERE ti.task_instance_owner = 'PROCESS_INSTANCE'
                     AND ti.task_instance_owner_id IN (
                                               SELECT /*+ index(process_instance PI_STATUS_CD_IDX) */
                                                      process_instance_id
                                                 FROM process_instance
                                                WHERE status_cd =
                                                                 purgestatusid))
        LIMIT commitcnt;
        SET row_count = row_count + ROW_COUNT();
     UNTIL ROW_COUNT() < 1 END REPEAT;     
	   SELECT ( CONCAT('Number of rows deleted from TASK_INST_GRP_MAPP: ', row_count));            
	   COMMIT;
   END IF;

   -- delete all the taskInstances that belong to current process instance and child instanes
   SET row_count = 0;
   REPEAT
     DELETE   from   task_instance 
         WHERE task_instance_owner = 'PROCESS_INSTANCE'
           AND task_instance_owner_id IN (SELECT /*+ index(process_instance PI_STATUS_CD_IDX) */
                                                    process_instance_id
                                               FROM process_instance
                                              WHERE status_cd = purgestatusid)
      LIMIT commitcnt;
      SET row_count = row_count + ROW_COUNT();
   UNTIL ROW_COUNT() < 1 END REPEAT;  

   SELECT ( CONCAT('Number of rows deleted from TASK_INSTANCE: ', row_count));
   COMMIT;
   
 SELECT SYSDATE()
     INTO dt
     FROM DUAL;

   SELECT ( CONCAT('Start Purging Document Content Records: ' , ifnull(dt, '')));

   
   -- deleting from document_content to avoid the integrity constraint issue
   SET row_count = 0;
   REPEAT
     DELETE from document_content where document_id IN (
      SELECT document_id FROM document doc
       WHERE   ( doc.owner_id != 0 AND doc.OWNER_TYPE = 'PROCESS_INSTANCE'  
               AND EXISTS (
                       SELECT /*+ index(pi PROCESS_INSTANCE_PK) */
                              process_instance_id
                         FROM process_instance pi
                        WHERE pi.process_instance_id = doc.owner_id
                          AND pi.status_cd = purgestatusid)
               )
   		 -- 2. all documents with LISTENER_REQUEST/USER as owner type and no process inst ID 
            OR ( doc.create_dt < DATE_SUB(CURDATE(), INTERVAL daydiff DAY)  
                AND doc.owner_id = 0 
                AND doc.owner_type IN ('LISTENER_REQUEST', 'USER')
               )
   		 -- 3. all documents with TASK_INSTANCE as owner
            OR ( doc.create_dt < DATE_SUB(CURDATE(), INTERVAL daydiff DAY)        
               AND doc.owner_id = 0
                AND doc.owner_type = 'TASK_INSTANCE'
               )
   		 -- 4. all documents with LISTENER_RESPONSE/DOCUMENT as owner and owner is deleted
            OR (    doc.owner_type IN ('LISTENER_RESPONSE', 'DOCUMENT')
                AND NOT EXISTS (SELECT *
                                  FROM document doc2
                                 WHERE doc2.document_id = doc.owner_id)
               ))
      LIMIT commitcnt;
      SET row_count = row_count + ROW_COUNT();
   UNTIL ROW_COUNT() < 1 END REPEAT; 
   
   SELECT
      ( CONCAT('Number of rows deleted from DOCUMENT Content: ', row_count));    
	 COMMIT;

   SELECT SYSDATE()
     INTO dt
     FROM DUAL;

   SELECT ( CONCAT('Start Purging Document Records: ' , ifnull(dt, '')));
   
   -- delete DOCUMENT 
   SET row_count = 0;
   REPEAT
     DELETE from document 
   		 -- 1. all documents with process instance ID populated
         WHERE (
          document.owner_id!= 0 AND document.OWNER_TYPE = 'PROCESS_INSTANCE' 
                AND 
               EXISTS (
                       SELECT /*+ index(pi PROCESS_INSTANCE_PK) */
                              process_instance_id
                         FROM process_instance pi
                         WHERE
                         pi.process_instance_id = document.owner_id    
                          AND 
                          pi.status_cd = purgestatusid)
               )
   		 -- 2. all documents with LISTENER_REQUEST/USER as owner type and no process inst ID 
            OR (    document.create_dt < DATE_SUB(CURDATE(), INTERVAL daydiff DAY)
               AND document.owner_id  = 0
                AND document.owner_type IN ('LISTENER_REQUEST', 'USER')
               )
   		 -- 3. all documents with TASK_INSTANCE as owner
            OR (    document.create_dt < DATE_SUB(CURDATE(), INTERVAL daydiff DAY)
             AND document.owner_id  = 0
                AND document.owner_type = 'TASK_INSTANCE' 
               )
   		--  4. all documents with LISTENER_RESPONSE/DOCUMENT as owner and owner is deleted
            OR (    document.owner_type IN ('LISTENER_RESPONSE', 'DOCUMENT')
                AND NOT EXISTS (SELECT 1 from(select *
                                  FROM document) doc2
                                WHERE doc2.document_id = document.owner_id)
              )
      LIMIT commitcnt;
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
    DELETE  from    process_instance 
         WHERE status_cd = purgestatusid
      LIMIT commitcnt;
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