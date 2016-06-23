/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.listener.formaction;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;

import com.centurylink.mdw.common.constant.FormConstants;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.model.data.common.InstanceNote;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.dao.user.cache.UserGroupCache;
import com.qwest.mbeng.MbengNode;

public class TaskActions extends FormActionBase {

	public FormDataDocument handleAction(FormDataDocument datadoc, Map<String, String> params) {
		try {
			String subaction = params.get(FormConstants.URLARG_ACTION);
			if ("showNotes".equals(subaction)) {
				// Long taskInstId = datadoc.getTaskInstanceId();
				// loadTaskNotes(taskInstId, datadoc);	changed to pre-load this
				datadoc.setAttribute(FormDataDocument.ATTR_FORM, "html:TaskNotes");
			} else if ("deleteNote".equals(subaction)) {
				int index = Integer.parseInt(params.get("index"));
				TaskManager taskManager = ServiceLocator.getTaskManager();
				MbengNode notesNode = datadoc.setTable(null, "TaskNotes", false);
				int i = 0;
				for (MbengNode row=notesNode.getFirstChild(); row!=null; row=row.getNextSibling()) {
					if (i==index) {
						String id = datadoc.getValue(row, "Id");
						String cuid = datadoc.getMetaValue(FormDataDocument.META_USER);
						Long userId = UserGroupCache.getUser(cuid).getId();
						taskManager.deleteNote(new Long(id), userId);
						notesNode.removeChild(row);
						break;
					}
					i++;
				}
			} else if ("addNote".equals(subaction)) {	// add a blank note
				TaskManager taskManager = ServiceLocator.getTaskManager();
				MbengNode notesNode = datadoc.setTable(null, "TaskNotes", false);
				Long taskInstId = datadoc.getTaskInstanceId();
				String summary = "(summary)";
				String detail = "(details)";
				String cuid = datadoc.getMetaValue(FormDataDocument.META_USER);
				Long noteId = taskManager.addNote(OwnerType.TASK_INSTANCE, taskInstId, summary, detail, cuid);
				MbengNode row = datadoc.addRow(notesNode);
				datadoc.setCell(row, "Id", noteId.toString());
				datadoc.setCell(row, "Summary", summary);
				datadoc.setCell(row, "Detail", detail);
				datadoc.setCell(row, "Creator", cuid);
				datadoc.setCell(row, "Date", StringHelper.dateToString(new Date()));
			} else if ("saveNote".equals(subaction)) {
				int index = Integer.parseInt(params.get("index"));
				TaskManager taskManager = ServiceLocator.getTaskManager();
				MbengNode notesNode = datadoc.setTable(null, "TaskNotes", false);
				int i = 0;
				for (MbengNode row=notesNode.getFirstChild(); row!=null; row=row.getNextSibling()) {
					if (i==index) {
						String id = datadoc.getValue(row, "Id");
						if (id==null || id.length()==0 || id.equals("0")) {	// new note
							Long taskInstId = datadoc.getTaskInstanceId();
							Long noteId = taskManager.addNote(OwnerType.TASK_INSTANCE, taskInstId,
									datadoc.getValue(row, "Summary"), datadoc.getValue(row,"Detail"),
									datadoc.getMetaValue(FormDataDocument.META_USER));
							datadoc.setCell(row, "Id", noteId.toString());
							datadoc.setCell(row, "Summary", datadoc.getValue(row, "Summary"));
							datadoc.setCell(row, "Detail", datadoc.getValue(row,"Detail"));
							datadoc.setCell(row, "Creator", datadoc.getMetaValue(FormDataDocument.META_USER));
							datadoc.setCell(row, "Date", StringHelper.dateToString(new Date()));
						} else {
							taskManager.updateNote(new Long(id),
									datadoc.getValue(row, "Summary"), datadoc.getValue(row,"Detail"),
									datadoc.getMetaValue(FormDataDocument.META_USER));
							datadoc.setCell(row, "Creator", datadoc.getMetaValue(FormDataDocument.META_USER));
							datadoc.setCell(row, "Date", StringHelper.dateToString(new Date()));
						}
						break;
					}
					i++;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			datadoc.addError(e.getMessage());
		}
		return datadoc;
	}

	public static void loadTaskNotes(Long taskInstId, FormDataDocument datadoc) throws Exception {
		TaskManager taskManager = ServiceLocator.getTaskManager();
		Collection<InstanceNote> notes = taskManager.getNotes(OwnerType.TASK_INSTANCE, taskInstId);
		int n = notes.size();
		int i = 0;
		InstanceNote[] sortedNotes = new InstanceNote[n];
		for (InstanceNote note : notes) {
			sortedNotes[i++] = note;
			String who = note.getModifiedBy();
			if (who==null) note.setModifiedBy(note.getCreatedBy());
			Date when = note.getModifiedDate();
			if (when==null) note.setModifiedDate(note.getCreatedDate());
		}
		Arrays.sort(sortedNotes, new Comparator<InstanceNote>() {
			public int compare(InstanceNote o1, InstanceNote o2) {
				return o1.getModifiedDate().compareTo(o2.getModifiedDate());
			}
		});
		MbengNode notesNode = datadoc.setTable(null, "TaskNotes", true);
		for (InstanceNote note : sortedNotes) {
			MbengNode row = datadoc.addRow(notesNode);
			datadoc.setCell(row, "Id", note.getId().toString());
			datadoc.setCell(row, "Summary", note.getNoteName());
			datadoc.setCell(row, "Detail", note.getNoteDetails());
			String who = note.getModifiedBy();
			if (who==null) who = note.getCreatedBy();
			datadoc.setCell(row, "Creator", who);
			Date when = note.getModifiedDate();
			if (when==null) when=note.getCreatedDate();
			datadoc.setCell(row, "Date", StringHelper.dateToString(when));
		}
	}


}
