/* Formatted on 2011/05/11 16:31 (Formatter Plus v4.8.8) */
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

      || ' where create_dt < TRUNC(SYSDATE - '
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
      DELETE FROM event_instance
         WHERE create_dt < TRUNC(SYSDATE - eldaydiff) AND ROWNUM <= commitcnt
         AND  event_name NOT LIKE 'ScheduledJob%';
     
      EXIT WHEN SQL%NOTFOUND;
      row_count := row_count + SQL%ROWCOUNT;     
      COMMIT;
   END LOOP;
   COMMIT;
 
   /*  Un-comment this block if your application uses Wait Activities in Process Flows 
    *  OR if you get Foreign Key violation errors when deleting from DOCUMENT table *
   LOOP
     DELETE FROM EVENT_INSTANCE
        WHERE document_id in (
            SELECT document_id
            FROM DOCUMENT doc
            WHERE doc.create_dt < TRUNC(sysdate - daydiff)
            AND owner_type = 'INTERNAL_EVENT')
        AND rownum < commitcnt;
        
     EXIT WHEN SQL%NOTFOUND ;
     row_count := row_count + SQL%ROWCOUNT;
     COMMIT;
   END LOOP;
   COMMIT;
   
   */
   
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
    WHERE table_name = 'TASK_INST_INDEX';
    
   IF table_exist IS NOT NULL
   THEN
   
   -- delete all task instance indices
      row_count := 0;
      LOOP
        DELETE      task_inst_index
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
   
      DBMS_OUTPUT.put_line (   'Number of rows deleted from TASK_INST_INDEX: '
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

   DBMS_OUTPUT.put_line ('Start Purging Document Records: ' || dt);

   -- delete DOCUMENT 
   row_count := 0;
   LOOP
      DELETE      document doc
   		 -- 1. all documents with process instance ID populated
         WHERE ((    doc.process_inst_id != 0
                AND EXISTS (
                       SELECT /*+ index(pi PROCESS_INSTANCE_PK) */
                              process_instance_id
                         FROM process_instance pi
                        WHERE pi.process_instance_id = doc.process_inst_id
                          AND pi.status_cd = purgestatusid)
               )
   		 -- 2. all documents with LISTENER_REQUEST/USER as owner type and no process inst ID 
            OR (    doc.create_dt <
                         TRUNC(SYSDATE - daydiff)
                AND doc.process_inst_id = 0
                AND doc.owner_type IN ('LISTENER_REQUEST', 'USER')
               )
   		 -- 3. all documents with TASK_INSTANCE as owner
            OR (    doc.create_dt <
                         TRUNC(SYSDATE - daydiff)
                AND doc.process_inst_id = 0
                AND doc.owner_type = 'TASK_INSTANCE'
               )
   		 -- 4. all documents with LISTENER_RESPONSE/DOCUMENT as owner and owner is deleted
            OR (    doc.owner_type IN ('LISTENER_RESPONSE', 'DOCUMENT')
                AND NOT EXISTS (SELECT *
                                  FROM document doc2
                                 WHERE doc2.document_id = doc.owner_id)
               )
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