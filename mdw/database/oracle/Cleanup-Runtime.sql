/* This script does not work if you are using MongoDB because in that DB we do not use document_content, all the document contents are in document table */
/* This script does do not delete from solution and solution_map tables, you have to come up with your own business requirements on how to purge those tables */
DECLARE
   dt                 TIMESTAMP;
   rnum               NUMBER;
   daydiff            NUMBER;
   eldaydiff          NUMBER;
   procid             NUMBER          := 0;
   purgestatusid      NUMBER          := 32;
   commitcnt          NUMBER;
   row_count          NUMBER;
   workstatusids      VARCHAR2 (100);
   dynsql             VARCHAR2 (1000);
   processidclause    VARCHAR2 (1000);
   workstatusclause   VARCHAR2 (1000);
   table_exist        VARCHAR2 (100);
   
BEGIN

--1,Pending Processing
--2,In Progress
--3,Failed
--4,Completed
--5,Cancelled
--6,Hold
--7,Waiting
--32,Purge

   rnum := ?;
   daydiff := ?;
   eldaydiff := ?;
   procid := ?;
   workstatusids := ?;
   commitcnt := ?;

--   rnum := 50000;
--   daydiff := 14;
--   eldaydiff := 14;
--   --procid := 11455;
--   workstatusids := '3,4,5';  
--  commitCount := 10000;

   SELECT SYSDATE
     INTO dt
     FROM DUAL;

   DBMS_OUTPUT.put_line ('Start Purging Process -->' || dt);
   DBMS_OUTPUT.put_line ('Start Marking Process Instances to Purge:' || dt);

   IF workstatusids IS NOT NULL
   THEN
      workstatusclause := 'and status_cd in (' || workstatusids || ') ';
   ELSE
      workstatusclause := 'and status_cd = 4 ';
   END IF;

   IF procid > 0
   THEN
      processidclause := 'and process_id = ' || procid || ' ';
   END IF;

   dynsql :=
         'update process_instance'
      || ' set status_cd = '
      || purgestatusid

      || ' where end_dt < TRUNC(SYSDATE - '
      || daydiff
      || ') '
      || workstatusclause
      || processidclause
      || ' and ROWNUM <= '
      || rnum;
      
   DBMS_OUTPUT.put_line ('Dynamic sql is: ' || dynsql);

   EXECUTE IMMEDIATE dynsql;

   COMMIT;
   
   SELECT SYSDATE
     INTO dt
     FROM DUAL;

   DBMS_OUTPUT.put_line ('Finish Marking Process Instance to Purge:' || dt);
   DBMS_OUTPUT.put_line ('Start Purging Event Records:' || dt);

-- mdw tables that need to be cleaned up based on date range
   row_count := 0;
   LOOP
      DELETE FROM event_log
         WHERE create_dt < TRUNC(SYSDATE - eldaydiff) AND ROWNUM <= commitcnt;
                    
      EXIT WHEN SQL%NOTFOUND;
      row_count := row_count + SQL%ROWCOUNT;     
      COMMIT;
   END LOOP;
   COMMIT;

   DBMS_OUTPUT.put_line (   'Number of rows deleted from EVENT_LOG: '
                         || row_count
                        );

   row_count := 0;
   LOOP
      DELETE FROM event_instance e1
         WHERE e1.create_dt < TRUNC(SYSDATE - eldaydiff) AND ROWNUM <= commitcnt
         AND  e1.event_name NOT LIKE 'ScheduledJob%';
         
      EXIT WHEN SQL%NOTFOUND;
      row_count := row_count + SQL%ROWCOUNT;     
      COMMIT;
   END LOOP;
   COMMIT;
   
   DBMS_OUTPUT.put_line (   'Number of rows deleted from EVENT_INSTANCE: '
                         || row_count
                        );

                        
   -- delete the event wait instance table
   row_count := 0;
   LOOP
      DELETE      event_wait_instance
         WHERE event_wait_instance_owner = 'ACTIVITY_INSTANCE'
           AND event_wait_instance_owner_id IN (
                  SELECT ai.activity_instance_id
                    FROM activity_instance ai
                   WHERE ai.process_instance_id IN (
                                               SELECT /*+ index(process_instance PI_STATUS_CD_IDX) */
                                                      process_instance_id
                                                 FROM process_instance
                                                WHERE status_cd = purgestatusid
                                                   )
                                                )
          AND ROWNUM <= commitcnt;
               
      EXIT WHEN SQL%NOTFOUND;
      row_count := row_count + SQL%ROWCOUNT;    
      COMMIT;
   END LOOP;
   COMMIT;
   
   DBMS_OUTPUT.put_line
                       (   'Number of rows deleted from EVENT_WAIT_INSTANCE: '
                        || row_count
                       );

   SELECT SYSDATE
     INTO dt
     FROM DUAL;

   DBMS_OUTPUT.put_line ('Start Purging Process Instance Child Records:' || dt);

   
   -- delete all the activity instances
   row_count := 0;
   LOOP
      DELETE      activity_instance ai
         WHERE ai.process_instance_id IN (SELECT /*+ index(process_instance PI_STATUS_CD_IDX) */
                                                 process_instance_id
                                            FROM process_instance
                                           WHERE status_cd = purgestatusid)
          AND ROWNUM <= commitcnt;
               
      EXIT WHEN SQL%NOTFOUND;
      row_count := row_count + SQL%ROWCOUNT;    
      COMMIT;
   END LOOP;
   COMMIT;
   
   DBMS_OUTPUT.put_line (   'Number of rows deleted from ACTIVITY_INSTANCE: '
                         || row_count
                        );

                        
   -- delete all the work transition instances that belong to current process instance and child instanes
   row_count := 0;
   LOOP
      DELETE      work_transition_instance wti
         WHERE wti.process_inst_id IN (SELECT /*+ index(process_instance PI_STATUS_CD_IDX) */
                                              process_instance_id
                                         FROM process_instance
                                        WHERE status_cd = purgestatusid)
          AND ROWNUM <= commitcnt;
               
      EXIT WHEN SQL%NOTFOUND;
      row_count := row_count + SQL%ROWCOUNT;    
      COMMIT;
   END LOOP;
   COMMIT;

   DBMS_OUTPUT.put_line
                  (   'Number of rows deleted from WORK_TRANSITION_INSTANCE: '
                   || row_count
                  );


   -- delete all the variable instances that belong to current process instance and child instanes
   row_count := 0;
   LOOP
      DELETE      variable_instance vi
         WHERE vi.process_inst_id IN (SELECT /*+ index(process_instance PI_STATUS_CD_IDX) */
                                             process_instance_id
                                        FROM process_instance
                                       WHERE status_cd = purgestatusid)
          AND ROWNUM <= commitcnt;
               
      EXIT WHEN SQL%NOTFOUND;
      row_count := row_count + SQL%ROWCOUNT;    
      COMMIT;
   END LOOP;
   COMMIT;

   DBMS_OUTPUT.put_line (   'Number of rows deleted from VARIABLE_INSTANCE: '
                         || row_count
                        );


   SELECT SYSDATE
     INTO dt
     FROM DUAL;

   DBMS_OUTPUT.put_line ('Start Purging Task Instances and Related:' || dt);

--   --  delete all the slaInstances that belong to task instances of current process instances
--   DELETE      sla_instance si
--         WHERE si.sla_inst_owner = 'TASK_INSTANCE'
--           AND si.sla_inst_owner_id IN (
--                  SELECT task_instance_id
--                    FROM task_instance ti
--                   WHERE ti.task_instance_owner = 'PROCESS_INSTANCE'
--                     AND ti.task_instance_owner_id IN (
--                                               SELECT /*+ index(process_instance PI_STATUS_CD_IDX) */
--                                                      process_instance_id
--                                                 FROM process_instance
--                                                WHERE status_cd =
--                                                                 purgestatusid));
--
--   DBMS_OUTPUT.put_line
--               (   'Number of rows deleted from SLA_INSTANCE(TASK_INSTANCE): '
--                || SQL%ROWCOUNT
--               );
--
--   COMMIT;

   -- delete all the instance notes for the task instances
   row_count := 0;
   LOOP
      DELETE      instance_note
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
          AND ROWNUM <= commitcnt;
               
      EXIT WHEN SQL%NOTFOUND;
      row_count := row_count + SQL%ROWCOUNT;    
      COMMIT;
   END LOOP;
   COMMIT;

   DBMS_OUTPUT.put_line (   'Number of rows deleted from INSTANCE_NOTE: '
                         || row_count
                        );

   
   SELECT table_name
     INTO table_exist
     FROM all_tables
    WHERE table_name = 'INSTANCE_INDEX';
    
   IF table_exist IS NOT NULL
   THEN
   
   -- delete all task instance indices
      row_count := 0;
      LOOP
        DELETE      instance_index
         WHERE owner_type='TASK_INSTANCE' and instance_id IN (
                  SELECT task_instance_id
                    FROM task_instance ti
                   WHERE ti.task_instance_owner = 'PROCESS_INSTANCE'
                     AND ti.task_instance_owner_id IN (
                                               SELECT /*+ index(process_instance PI_STATUS_CD_IDX) */
                                                      process_instance_id
                                                 FROM process_instance
                                                WHERE status_cd =
                                                                 purgestatusid))
          AND ROWNUM <= commitcnt;
               
        EXIT WHEN SQL%NOTFOUND;
        row_count := row_count + SQL%ROWCOUNT;    
        COMMIT;
      END LOOP;
      COMMIT;
   
      DBMS_OUTPUT.put_line (   'Number of rows deleted from INSTANCE_INDEX: '
                         || row_count
                        );
    
                        
   -- delete all task instance group mappings
      row_count := 0;
      LOOP
        DELETE      task_inst_grp_mapp
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
          AND ROWNUM <= commitcnt;
               
        EXIT WHEN SQL%NOTFOUND;
        row_count := row_count + SQL%ROWCOUNT;    
        COMMIT;
      END LOOP;
      COMMIT;
      
      DBMS_OUTPUT.put_line (   'Number of rows deleted from TASK_INST_GRP_MAPP: '
                         || row_count
                        );
   
   END IF;

   -- delete all the taskInstances that belong to current process instance and child instanes
   row_count := 0;
   LOOP
      DELETE      task_instance ti
         WHERE ti.task_instance_owner = 'PROCESS_INSTANCE'
           AND ti.task_instance_owner_id IN (SELECT /*+ index(process_instance PI_STATUS_CD_IDX) */
                                                    process_instance_id
                                               FROM process_instance
                                              WHERE status_cd = purgestatusid)
           AND ROWNUM <= commitcnt;
               
      EXIT WHEN SQL%NOTFOUND;
      row_count := row_count + SQL%ROWCOUNT;    
      COMMIT;
   END LOOP;
   COMMIT;

   DBMS_OUTPUT.put_line (   'Number of rows deleted from TASK_INSTANCE: '
                         || row_count
                        );
   
   SELECT SYSDATE
     INTO dt
     FROM DUAL;

   DBMS_OUTPUT.put_line ('Start Purging Document Content Records: ' || dt);

   -- delete DOCUMENT_CONTENT
   row_count := 0;
   LOOP
     
 DELETE document_content where document_id IN (
     SELECT document_id FROM document doc 
       -- 1. all documents with process instance ID populated, LISTENER_REQUEST -- Test inserts with non zero owner_id
         WHERE ((    doc.owner_id!= 0 AND doc.OWNER_TYPE IN ('PROCESS_INSTANCE', 'PROCESS_RUN', 'LISTENER_REQUEST') 
                AND EXISTS (
                       SELECT /*+ index(pi PROCESS_INSTANCE_PK) */
                              process_instance_id
                         FROM process_instance pi
                        WHERE pi.process_instance_id = doc.owner_id
                          AND pi.status_cd = purgestatusid)
            )
       -- 2. all documents with LISTENER_REQUEST/USER/TASK_INSTANCE/LISTENER_REQUEST_META/LISTENER_RESPONSE/VARIABLE_INSTANCE/INTERNAL_EVENT
       --    as owner type and no owner id meeting DATE criteria
            OR (    doc.create_dt <
                         TRUNC(SYSDATE - daydiff)
                AND doc.owner_id = 0
                AND doc.owner_type IN ('LISTENER_REQUEST', 'USER', 'TASK_INSTANCE','LISTENER_REQUEST_META','LISTENER_RESPONSE','VARIABLE_INSTANCE','INTERNAL_EVENT')  --empty Get request will have owner_id = 0 
            )
        -- 3. all documents with owner type of ACTIVITY_INSTANCE/ADAPTER_REQUEST/ADAPTER_RESPONSE/INTERNAL_EVENT where row 
        --		in ACTIVITY_INSTANCE table has been deleted
            OR (doc.owner_id!= 0 AND doc.owner_type IN ('ACTIVITY_INSTANCE','ADAPTER_REQUEST','ADAPTER_RESPONSE','INTERNAL_EVENT')
               AND NOT EXISTS (SELECT ACTIVITY_INSTANCE_ID FROM ACTIVITY_INSTANCE act where act.activity_instance_id=doc.owner_id)
            )
        -- 4. all documents with owner type of VARIABLE_INSTANCE where row in VARIABLE_INSTANCE table has been deleted
            OR (doc.owner_id != 0 AND doc.owner_type IN ('VARIABLE_INSTANCE')
               AND NOT EXISTS (SELECT VARIABLE_INST_ID FROM VARIABLE_INSTANCE var where var.variable_inst_id=doc.owner_id)
            )
        -- 5. all documents with owner type of TASK_INSTANCE where row in TASK_INSTANCE table has been deleted
            OR ( doc.owner_id != 0 AND doc.OWNER_TYPE = 'TASK_INSTANCE'
            	AND NOT EXISTS (SELECT task_instance_id	FROM task_instance WHERE task_instance_id = doc.owner_id)
            )   
       	-- 6. all documents with LISTENER_RESPONSE/DOCUMENT/ *_META as owner type and owner is deleted 
            OR (doc.owner_id != 0 AND doc.owner_type IN 
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
         )
         AND ROWNUM <= commitcnt;
               
      EXIT WHEN SQL%NOTFOUND;
      row_count := row_count + SQL%ROWCOUNT;    
      COMMIT;
   END LOOP;
   COMMIT;

   DBMS_OUTPUT.put_line
      (   'Number of rows deleted from DOCUMENT_CONTENT: '
       || row_count
      );
      
    SELECT SYSDATE
     INTO dt
     FROM DUAL;     
   DBMS_OUTPUT.put_line ('Start Purging Document Records: ' || dt);

   -- delete DOCUMENT
   row_count := 0;
   LOOP   
   	DELETE FROM document where document_id IN (
     SELECT document_id FROM document doc 
       -- 1. all documents with process instance ID populated, LISTENER_REQUEST -- Test inserts with non zero owner_id
         WHERE ((    doc.owner_id!= 0 AND doc.OWNER_TYPE IN ('PROCESS_INSTANCE', 'PROCESS_RUN', 'LISTENER_REQUEST') 
                AND EXISTS (
                       SELECT /*+ index(pi PROCESS_INSTANCE_PK) */
                              process_instance_id 
                         FROM process_instance pi
                        WHERE pi.process_instance_id = doc.owner_id
                          AND pi.status_cd = purgestatusid)
            )
       -- 2. all documents with LISTENER_REQUEST/USER/TASK_INSTANCE/LISTENER_REQUEST_META/LISTENER_RESPONSE/VARIABLE_INSTANCE/INTERNAL_EVENT
       --    as owner type and no owner id meeting DATE criteria
            OR (    doc.create_dt <
                         TRUNC(SYSDATE - daydiff)
                AND doc.owner_id = 0
                AND doc.owner_type IN ('LISTENER_REQUEST', 'USER', 'TASK_INSTANCE','LISTENER_REQUEST_META','LISTENER_RESPONSE','VARIABLE_INSTANCE','INTERNAL_EVENT')  --empty Get request will have owner_id = 0 
            )
        -- 3. all documents with owner type of ACTIVITY_INSTANCE/ADAPTER_REQUEST/ADAPTER_RESPONSE/INTERNAL_EVENT where row 
        --		in ACTIVITY_INSTANCE table has been deleted
            OR (doc.owner_id!= 0 AND doc.owner_type IN ('ACTIVITY_INSTANCE','ADAPTER_REQUEST','ADAPTER_RESPONSE','INTERNAL_EVENT')
               AND NOT EXISTS (SELECT ACTIVITY_INSTANCE_ID FROM ACTIVITY_INSTANCE act where act.activity_instance_id=doc.owner_id)
            )
        -- 4. all documents with owner type of VARIABLE_INSTANCE where row in VARIABLE_INSTANCE table has been deleted
            OR (doc.owner_id != 0 AND doc.owner_type IN ('VARIABLE_INSTANCE')
               AND NOT EXISTS (SELECT VARIABLE_INST_ID FROM VARIABLE_INSTANCE var where var.variable_inst_id=doc.owner_id)
            )
        -- 5. all documents with owner type of TASK_INSTANCE where row in TASK_INSTANCE table has been deleted
            OR ( doc.owner_id != 0 AND doc.OWNER_TYPE = 'TASK_INSTANCE'
            	AND NOT EXISTS (SELECT task_instance_id	FROM task_instance WHERE task_instance_id = doc.owner_id)
            )   
       	-- 6. all documents with LISTENER_RESPONSE/DOCUMENT/ *_META as owner type and owner is deleted 
            OR (doc.owner_id != 0 AND doc.owner_type IN 
            	('LISTENER_RESPONSE','LISTENER_RESPONSE_META','LISTENER_REQUEST_META','DOCUMENT','ADAPTER_REQUEST_META','ADAPTER_RESPONSE_META')
                AND NOT EXISTS (SELECT document_id 
                                  FROM document doc2
                                 WHERE doc2.document_id = doc.owner_id)
               )
            )
         -- 7. the document (event message) is not tied to an event_instance
            AND NOT EXISTS (SELECT e1.document_id
         					FROM event_instance e1
         					WHERE e1.document_id = doc.document_id)
         )
         AND ROWNUM <= commitcnt;
     
      EXIT WHEN SQL%NOTFOUND;
      row_count := row_count + SQL%ROWCOUNT;    
      COMMIT;
   END LOOP;
   COMMIT;

   DBMS_OUTPUT.put_line
      (   'Number of rows deleted from DOCUMENT: '
       || row_count
      );


   SELECT SYSDATE
     INTO dt
     FROM DUAL;

   DBMS_OUTPUT.put_line ('Start Purging Process Instances Themselves:' || dt);

   -- delete the process instance
   row_count := 0;
   LOOP
      DELETE      process_instance pi
         WHERE status_cd = purgestatusid AND ROWNUM <= commitcnt;
               
      EXIT WHEN SQL%NOTFOUND;
      row_count := row_count + SQL%ROWCOUNT;    
      COMMIT;
   END LOOP;
   COMMIT;

   DBMS_OUTPUT.put_line (   'Number of rows deleted from PROCESS_INSTANCE: '
                         || row_count
                        );
   
   SELECT SYSDATE
     INTO dt
     FROM DUAL;
     
   DBMS_OUTPUT.put_line ('Finish Purging Process -->' || dt);
   
EXCEPTION
   WHEN OTHERS
   THEN
      DBMS_OUTPUT.put_line ('Error Purge Process :' || ' Error:' || SQLERRM);
      
      rollback;
END;
/
