---
permalink: /docs/help/SubProcessInvocations/
title: MDW Sub-ProcessInvocations
---

### Using InvokeSubprocessActivity to Launch a Sub Process

When launching a sub process using the built-in InvokeSubProcessActivity activity, many factors come into play to determine how that sub process will execute.
Below explains the different behaviors:
 
	1. Service Parent process:
		
		- Launching sub Service process Sync (All perf lvls): Executes in same thread and same engine to maintain cached documents.
		
		- Launching sub Regular process Sync (All perf lvls): Throws ActivityException.
		
		- Launching sub Service/Regular process Async (Perf lvl < 5 OR not binding any document variables to sub): Executes in new thread.
		
		- Launching sub Service/Regular process Async (Perf lvl >= 5 AND binding document variables to sub): Throws ActivityException.
		
		
	2. Regular Parent process:
	
		- Launching sub Service process Sync (Perf lvl < 9 OR not binding any document variables to sub): Executes in same thread but new engine to respect sub perf lvl.
		
		- Launching sub Service/Regular process Sync (Perf lvl 9 AND binding document variables to sub): Executes in same thread and same engine to maintain cached documents.
		
		- Launching sub Regular process Sync (Parent and Sub Perf lvl matches OR sub Perf lvl is 0): Executes in same thread and same engine.
		
		- Launching sub Regular process Sync (Different Parent / Sub Perf lvls AND sub Perf lvl is NOT 0): Executes in new thread.
		
		- Launching sub Service/Regular process Async (Perf lvl < 9 OR not binding any document variables to sub): Executes in new thread.
				
		- Launching sub Service/Regular process Async (Perf lvl 9 AND binding document variables to sub): Throws ActivityException.