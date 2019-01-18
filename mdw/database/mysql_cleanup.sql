DROP procedure IF EXISTS `mysql_cleanup`;
DELIMITER $$
CREATE PROCEDURE `mysql_cleanup`(IN inputrnum INT , inputdaydiff INT, inputeldaydiff INT,
inputprocid INT, inputworkstatusids VARCHAR (100) , inputinterval INT)
BEGIN
DECLARE dt                 DATETIME(6);
   DECLARE rnum               INT;
   DECLARE daydiff            INT;
   DECLARE eldaydiff          INT;
   DECLARE procid             INT           DEFAULT  0;
   DECLARE purgestatusid      INT           DEFAULT  32;
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
      
   SELECT (CONCAT('Dynamic sql is: ' , ifnull(@dynsql, '')));

   PREPARE stmt1 FROM @dynsql; 
  EXECUTE stmt1; 
   COMMIT;
  DEALLOCATE PREPARE stmt1; 

   SELECT SYSDATE()
     INTO dt
     FROM DUAL;

   SELECT (CONCAT('Finish Marking Process Instance to Purge:' , ifnull(dt, '')));
   SELECT (CONCAT('Start Purging Event Records:' , ifnull(dt, '')));

   -- mdw tables that need to be cleaned up based on date range
   DELETE FROM EVENT_LOG
         WHERE create_dt < CURDATE() - eldaydiff 
         LIMIT rnum;

   SELECT (   CONCAT('Number of rows deleted from EVENT_LOG: ',ROW_COUNT()));
                        
    COMMIT;
  
   DELETE FROM EVENT_INSTANCE
         WHERE create_dt < CURDATE() - eldaydiff 
         AND  event_name NOT LIKE 'ScheduledJob%'
     LIMIT rnum;

   SELECT (   CONCAT('Number of rows deleted from EVENT_INSTANCE:',ROW_COUNT()));
                        
   

   -- delete the event wait instance table
   DELETE    from   EVENT_WAIT_INSTANCE
         WHERE event_wait_instance_owner = 'ACTIVITY_INSTANCE'
           AND event_wait_instance_owner_id IN (
                  SELECT ai.activity_instance_id
                    FROM ACTIVITY_INSTANCE ai
                   WHERE ai.process_instance_id IN (
                                               SELECT /*+ index(process_instance PI_STATUS_CD_IDX) */
                                                      process_instance_id
                                                 FROM PROCESS_INSTANCE
                                                WHERE status_cd =
                                                                 purgestatusid));

   SELECT(   CONCAT('Number of rows deleted from EVENT_WAIT_INSTANCE: ',ROW_COUNT()));
    
  COMMIT;                    
   
   SELECT SYSDATE()
     INTO dt
     FROM DUAL;
      SELECT (CONCAT('Start Purging Process Instance Child Records:' , ifnull(dt, '')));

   -- delete all the activity instances
   DELETE    ai from  ACTIVITY_INSTANCE as ai
         WHERE ai.process_instance_id IN (SELECT /*+ index(process_instance PI_STATUS_CD_IDX) */
                                                 process_instance_id
                                            FROM PROCESS_INSTANCE
                                           WHERE status_cd = purgestatusid);

   SELECT (   CONCAT('Number of rows deleted from ACTIVITY_INSTANCE: ',ROW_COUNT()));
                         
 COMMIT;
   

   -- delete all the work transition instances that belong to current process instance and child instanes
   DELETE  wti from    WORK_TRANSITION_INSTANCE as wti
         WHERE wti.process_inst_id IN (SELECT /*+ index(process_instance PI_STATUS_CD_IDX) */
                                              process_instance_id
                                         FROM PROCESS_INSTANCE
                                        WHERE status_cd = purgestatusid);

   SELECT(   CONCAT('Number of rows deleted from WORK_TRANSITION_INSTANCE: ',ROW_COUNT()));
                
    COMMIT;


   -- delete all the variable instances that belong to current process instance and child instanes
   DELETE   vi from   VARIABLE_INSTANCE as  vi
         WHERE vi.process_inst_id IN (SELECT /*+ index(process_instance PI_STATUS_CD_IDX) */
                                             process_instance_id
                                        FROM PROCESS_INSTANCE
                                       WHERE status_cd = purgestatusid);

   SELECT (   CONCAT('Number of rows deleted from VARIABLE_INSTANCE: ',ROW_COUNT()));
                    
 COMMIT;
   SELECT SYSDATE()
     INTO dt
     FROM DUAL;

   SELECT (CONCAT('Start Purging Task Instances and Related:' , ifnull(dt, ''))); 

   -- delete all the instance notes for the task instances
   DELETE  from    INSTANCE_NOTE
         WHERE instance_note_owner_id IN (
                  SELECT task_instance_id
                    FROM TASK_INSTANCE ti
                   WHERE ti.task_instance_owner = 'PROCESS_INSTANCE'
                     AND ti.task_instance_owner_id IN (
                                               SELECT /*+ index(process_instance PI_STATUS_CD_IDX) */
                                                      process_instance_id
                                                 FROM PROCESS_INSTANCE
                                                WHERE status_cd =
                                                                 purgestatusid));

   SELECT (   CONCAT('Number of rows deleted from INSTANCE_NOTE: ',ROW_COUNT()));
                    
           COMMIT;
  SELECT table_name
     INTO table_exist
     FROM information_schema.TABLES
    WHERE table_name = 'INSTANCE_INDEX';
    
   IF table_exist IS NOT NULL
   THEN
   
   -- delete all task instance indices
      DELETE  from    INSTANCE_INDEX
         WHERE owner_type='TASK_INSTANCE' and instance_id IN (
                  SELECT task_instance_id
                    FROM TASK_INSTANCE ti
                   WHERE ti.task_instance_owner = 'PROCESS_INSTANCE'
                     AND ti.task_instance_owner_id IN (
                                               SELECT /*+ index(process_instance PI_STATUS_CD_IDX) */
                                                      process_instance_id
                                                 FROM PROCESS_INSTANCE
                                                WHERE status_cd =
                                                                 purgestatusid));
      SELECT (   CONCAT('Number of rows deleted from INSTANCE_INDEX:',ROW_COUNT()));
                    
       COMMIT;
      
   -- delete all task instance group mappings
      DELETE  from    TASK_INST_GRP_MAPP
         WHERE task_instance_id IN (
                  SELECT task_instance_id
                    FROM TASK_INSTANCE ti
                   WHERE ti.task_instance_owner = 'PROCESS_INSTANCE'
                     AND ti.task_instance_owner_id IN (
                                               SELECT /*+ index(process_instance PI_STATUS_CD_IDX) */
                                                      process_instance_id
                                                 FROM PROCESS_INSTANCE
                                                WHERE status_cd =
                                                                 purgestatusid));
      SELECT (   CONCAT('Number of rows deleted from TASK_INST_GRP_MAPP: ',ROW_COUNT()));
                  
    COMMIT;
   END IF;

   -- delete all the taskInstances that belong to current process instance and child instanes
   DELETE   ti from   TASK_INSTANCE as ti
         WHERE ti.task_instance_owner = 'PROCESS_INSTANCE'
           AND ti.task_instance_owner_id IN (SELECT /*+ index(process_instance PI_STATUS_CD_IDX) */
                                                    process_instance_id
                                               FROM PROCESS_INSTANCE
                                              WHERE status_cd = purgestatusid);

   SELECT (   CONCAT('Number of rows deleted from TASK_INSTANCE: ',ROW_COUNT()));
    COMMIT;
   
 SELECT SYSDATE()
     INTO dt
     FROM DUAL;

   SELECT (CONCAT('Start Purging Document Content Records: ' , ifnull(dt, '')));

   
   --deleting from document_content to avoid the integrity constraint issue
    DELETE from DOCUMENT_CONTENT where document_id IN (
   SELECT document_id FROM DOCUMENT doc
   WHERE   ( doc.owner_id != 0 AND doc.OWNER_TYPE = 'PROCESS_INSTANCE'  
               AND EXISTS (
                       SELECT /*+ index(pi PROCESS_INSTANCE_PK) */
                              process_instance_id
                         FROM PROCESS_INSTANCE pi
                        WHERE pi.process_instance_id = doc.owner_id
                          AND pi.status_cd = purgestatusid)
               )
       -- 2. all documents with LISTENER_REQUEST/USER as owner type and no process inst ID 
            OR ( doc.create_dt < CURDATE() - daydiff 
                AND doc.owner_id = 0 
                AND doc.owner_type IN ('LISTENER_REQUEST', 'USER')
               )
       -- 3. all documents with TASK_INSTANCE as owner
            OR ( doc.create_dt < CURDATE() - daydiff           
               AND doc.owner_id = 0
                AND doc.owner_type = 'TASK_INSTANCE'
               )
       -- 4. all documents with LISTENER_RESPONSE/DOCUMENT as owner and owner is deleted
            OR (    doc.owner_type IN ('LISTENER_RESPONSE', 'DOCUMENT')
                AND NOT EXISTS (SELECT *
                                  FROM document doc2
                                 WHERE doc2.document_id = doc.owner_id)
               ));
   SELECT
      (   CONCAT('Number of rows deleted from DOCUMENT Content: ',ROW_COUNT()));
       
      COMMIT;

   SELECT SYSDATE()
     INTO dt
     FROM DUAL;

   SELECT (CONCAT('Start Purging Document Records: ' , ifnull(dt, '')));
   
   -- delete DOCUMENT 
   DELETE   from doc USING document as doc
       -- 1. all documents with process instance ID populated
         WHERE (
          doc.owner_id!= 0 AND doc.OWNER_TYPE = 'PROCESS_INSTANCE' 
                AND 
               EXISTS (
                       SELECT /*+ index(pi PROCESS_INSTANCE_PK) */
                              process_instance_id
                         FROM PROCESS_INSTANCE pi
                         WHERE
                         pi.process_instance_id = doc.owner_id    
                          AND 
                          pi.status_cd = purgestatusid)
               )
       -- 2. all documents with LISTENER_REQUEST/USER as owner type and no process inst ID 
            OR (    doc.create_dt <
                         CURDATE()
                       - daydiff
               AND doc.owner_id  = 0
                AND doc.owner_type IN ('LISTENER_REQUEST', 'USER')
               )
       -- 3. all documents with TASK_INSTANCE as owner
            OR (    doc.create_dt <
                         CURDATE()
                       - daydiff 
             AND doc.owner_id  = 0
                AND doc.owner_type = 'TASK_INSTANCE' 
               )
      --  4. all documents with LISTENER_RESPONSE/DOCUMENT as owner and owner is deleted
            OR (    doc.owner_type IN ('LISTENER_RESPONSE', 'DOCUMENT')
                AND NOT EXISTS (SELECT 1 from(select *
                                  FROM document) doc2
                                WHERE doc2.document_id = doc.owner_id)
              );

   SELECT
      (   CONCAT('Number of rows deleted from DOCUMENT: ',ROW_COUNT()));
       
      COMMIT;
   
   
   SELECT SYSDATE()
     INTO dt
     FROM DUAL;

   SELECT (CONCAT('Start Purging Process Instances Themselves:' , ifnull(dt, '')));

   -- delete the process instance
   DELETE  pi from    PROCESS_INSTANCE as pi
         WHERE status_cd = purgestatusid;

   SELECT (   CONCAT('Number of rows deleted from PROCESS_INSTANCE: ',ROW_COUNT()));
                   
    COMMIT;
   SELECT SYSDATE()
     INTO dt
     FROM DUAL;
     
   SELECT (CONCAT('Finish Purging Process -->', dt));
   
    
   

END$$

DELIMITER ;
