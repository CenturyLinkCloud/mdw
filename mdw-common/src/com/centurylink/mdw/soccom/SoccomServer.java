/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.soccom;

import java.net.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.Hashtable;
import java.util.Enumeration;

public class SoccomServer extends ServerSocket {

	private boolean _shutdown;
	private Thread main_thread;
	private SimpleDateFormat df;
	private PrintStream log;
	private int thread_count = 0;
	private Hashtable<String, ServerThread> threads;
	protected int max_threads = 0;

	// when max_threads is 0, there is no thread control
	// when max_threads is 1, use single thread model

	public SoccomServer(String port, PrintStream log) throws IOException {
		super(Integer.parseInt(port));
		this.log = log;
		df = new SimpleDateFormat("HH:mm:ss");
		logline(null, "Server started on port " + port);
	}

	public SoccomServer(String port, String logname) throws IOException {
		super(Integer.parseInt(port));
		log = new PrintStream(new FileOutputStream(logname, true), true);
		df = new SimpleDateFormat("HH:mm:ss");
		logline(null, "Server started on port " + port);
	}

	public synchronized void shutdown() {
		_shutdown = true;
		if (main_thread != null)
		    main_thread.interrupt();
		try {
			this.close();
		} catch (IOException e) {
		}
	}

	synchronized void logline(String tid, String msg) {
		if (log != null) {
			if (tid == null)
				log.println(df.format(new Date()) + ": " + msg);
			else
				log.println(df.format(new Date()) + " " + tid + ": " + msg);
		}
	}

	private synchronized int change_thread_count(boolean add) {
		if (add)
			thread_count++;
		else
			thread_count--;
		return thread_count;
	}

	int add_thread(String thread_id, ServerThread thread) {
		threads.put(thread_id, thread);
		return change_thread_count(true);
	}

	void remove_thread(String thread_id, ServerThread thread) {
		threads.remove(thread_id);
		change_thread_count(false);
	}

	private void shutdown_threads() {
		Enumeration<ServerThread> en = threads.elements();
		ServerThread thread;
		while (en.hasMoreElements()) {
			thread = en.nextElement();
			logline(null, "Interrupt thread " + thread.thread_id);
			thread.interrupt();
		}
	}

	public void start(boolean useNewThread) {
		if (useNewThread) {
			main_thread = new Thread() {
				public void run() {
					start_sub();
				}
			};
			main_thread.start();
		} else {
			main_thread = Thread.currentThread();
			start_sub();
		}
	}

	private void start_sub() {
		ServerThread thread;
		_shutdown = false;
		threads = new Hashtable<String, ServerThread>();
		while (!_shutdown) {
			try {
				setSoTimeout(0);
				Socket socket = accept();
				if (_shutdown) {
					// accept returns a default socket 0.0.0.0 instead of
					// raising an InterruptedIOException, when shutdown()
					// is called. Seems to be a bug in Java 1.3.
					logline(null, "Server shuts down here");
				} else if (connect_proc(socket)) {
					socket.setSoTimeout(120 * 1000);
					if (max_threads == 1) {
						try {
							process_message(socket.getInputStream(), socket
									.getOutputStream(), "main", false);
							socket.close();
						} catch (Exception e) {
						    e.printStackTrace();
							logline("main", "Exception: " + e);
						}
					} else {
						if (max_threads > 1 && thread_count >= max_threads) {
							logline(null, "MAXIMUM NUMBER OF THREADS REACHED");
							thread = new ServerThread(socket, this, true);
						} else {
							thread = new ServerThread(socket, this, false);
						}
						thread.start();
					}
				} else {
					logline(null, "Reject connection request from "
							+ socket.getInetAddress());
					socket.close();
				}
			} catch (InterruptedIOException e) {
				if (_shutdown)
					logline(null, "Server shuts down");
				else
					logline(null, "Exception: " + e);
			} catch (IOException e) {
			    if (_shutdown) {
			        logline(null, "Server shuts down");
			    }
			    else {
    			    e.printStackTrace();
    				logline(null, "Exception: " + e);
			    }
			}
		}
		// call user shutdown processor
		shutdown_proc();
		// need to kill all active thread
		shutdown_threads();
	}

	private static void printUsage() {
		System.err
				.println("Usage: java com.qwest.magic.soccom.SoccomServer [options] port");
		System.err.println("   -l log: log file name, default stdout");
		System.err.println("   -t n: max number of threads, default 3");
		System.err.println("      when n is 0, no thread control applies");
		System.err.println("      when n is 1, use single thread model");
	}

	protected boolean connect_proc(Socket socket) {
		return true;
	}

	protected void disconnect_proc(Socket socket) {
	}

	protected void shutdown_proc() {
	}

	protected void putresp(String thread_id, OutputStream out, String msgid,
			String msg) throws IOException {
		byte msgbytes[] = SoccomMessage.makeMessage(msg, msgid);
		logline(thread_id, "SEND: " + new String(msgbytes));
		out.write(msgbytes);
	}

	protected void putresp_vheader(String thread_id, OutputStream out,
			String msgid, String endIndicator) throws IOException,
			SoccomException {
		if (endIndicator.length() != 4)
			throw new SoccomException(SoccomException.ENDM_LENGTH);
		byte msgbytes[] = SoccomMessage.makeMessageSpecial("ENDM"
				+ endIndicator, msgid);
		logline(thread_id, "SEND: " + new String(msgbytes));
		out.write(msgbytes);
	}

	protected void putresp_vline(String thread_id, OutputStream out, String msg)
			throws IOException, SoccomException {
		int length = msg.length();
		if (msg.charAt(length - 1) == '\n') {
			logline(thread_id, "SEND: " + msg.substring(0, length - 1));
		} else {
			logline(thread_id, "SEND: " + msg);
			msg += "\n";
		}
		byte msgbytes[] = msg.getBytes();
		out.write(msgbytes);
	}

	protected void putresp_vfooter(String thread_id, OutputStream out,
			String endIndicator) throws IOException, SoccomException {
		if (endIndicator.length() != 4)
			throw new SoccomException(SoccomException.ENDM_LENGTH);
		String msg = endIndicator + "\n";
		byte msgbytes[] = msg.getBytes();
		logline(thread_id, "SEND: " + endIndicator);
		out.write(msgbytes);
	}

	protected void request_proc(String thread_id, String msgid, byte[] msg,
			int msgsize, OutputStream out) throws IOException, SoccomException {
		String msgstr = new String(msg, 0, msgsize);
		if (msgstr.length() >= 5 && msgstr.substring(0, 5).equals("SLEEP")) {
			int seconds = Integer.parseInt(msgstr.substring(6));
			try {
				if (seconds >= 0) {
					Thread.sleep(seconds * 1000);
				} else {
					boolean hangup = false;
					while (!hangup) {
						Thread.sleep(-seconds * 1000);
						hangup = client_hangup();
						if (hangup)
							logline(thread_id, "client hang-up");
					}
				}
			} catch (InterruptedException e) {
			}
			putresp(thread_id, out, msgid, msgstr);
		} else if (msgstr.length() >= 8
				&& msgstr.substring(0, 8).equals("BUSYLOOP")) {
			int count = Integer.parseInt(msgstr.substring(9));
			for (int i = 0; i < count; i++) {
				System.out.println(" loop " + i + " of " + count);
				if (Thread.interrupted()) {
					logline(thread_id, "Thread interrupted");
					break;
				}
			}
			putresp(thread_id, out, msgid, msgstr);
		} else if (msgstr.length() >= 12
				&& msgstr.substring(0, 12).equals("_TEST_COMBIN")) {
			/*
			 * test combination of sending/receiving line-by-line or as a whole.
			 * Spec has 4 characters as follows: 1. for client send: W - as a
			 * whole, L - line-by-line 2. for server recv: W - as a whole, L -
			 * not implemented 3. for server send: W - as a whole, L -
			 * line-by-line 4. for client recv: W - as a whole, L - line-by-line
			 */
			msgstr = "_RESP_" + msgstr.substring(6);
			String testspec = msgstr.substring(13, 17);
			if (testspec.charAt(2) == 'L') {
				StringTokenizer st = new StringTokenizer(msgstr, "\n");
				putresp_vheader(thread_id, out, msgid, "****");
				while (st.hasMoreTokens()) {
					putresp_vline(thread_id, out, st.nextToken());
				}
				putresp_vfooter(thread_id, out, "****");
			} else {
				putresp(thread_id, out, msgid, msgstr);
			}
		} else if (msgstr.length() >= 6
				&& msgstr.substring(0, 6).equals("_TEST_")) {
			msgstr = "_RESP_" + msgstr.substring(6);
			putresp(thread_id, out, msgid, msgstr);
		} else {
			putresp(thread_id, out, msgid, msgstr);
		}
	}

	private int readLine(InputStream in, byte buffer[], int offset, int maxbytes)
			throws IOException {
		int n = 0;
		boolean line_read = false;
		while (!line_read && n < maxbytes) {
			in.read(buffer, offset + n, 1);
			if (buffer[offset + n] == '\n')
				line_read = true;
			n++;
		}
		return n;
	}

	boolean process_message(InputStream in, OutputStream out, String thread_id,
			boolean excess_thread) throws SoccomException, Exception {
		byte[] _header = new byte[SoccomMessage.HEADER_SIZE];
		int n;
		n = in.read(_header, 0, SoccomMessage.HEADER_SIZE);
		if (n != SoccomMessage.HEADER_SIZE) {
			if (n == -1)
				return true;
			else
				throw new Exception("Header imcomplete: " + n);
		}
		String sizestr = new String(_header, SoccomMessage.MSGSIZE_OFFSET, 8);
		if (sizestr.equals("SHUTDOWN")) {
			logline(thread_id, "request shut down");
			shutdown();
			return true;
		}
		byte[] buffer;
		int size;
		if (sizestr.startsWith("ENDM")) {
			String endm = sizestr.substring(4, 8);
			boolean line_read = false;
			buffer = new byte[1024];
			size = 0;
			do {
				n = readLine(in, buffer, size, 1024);
				if (n == 5 && endm.equals(new String(buffer, size, 4))) {
					line_read = true;
				} else if (n > 0) {
					size += n;
				} else
					break;
			} while (!line_read);
		} else {
			size = Integer.parseInt(sizestr);
			buffer = new byte[size];
			int got = 0;
			while (got < size) {
				n = in.read(buffer, got, size - got);
				if (n > 0)
					got += n;
				else if (n == -1)
					throw new SoccomException(SoccomException.SOCKET_CLOSED);
				else
					throw new SoccomException(SoccomException.RECV_ERROR);
			}
		}
		String msgid = new String(_header, 0, SoccomMessage.MSGID_SIZE);
		logline(thread_id, "RECV: " + new String(_header));
		logline(thread_id, " msg: " + new String(buffer, 0, size));
		if (excess_thread) {
			putresp(thread_id, out, msgid, "ERROR: NO MORE CONNECTION ALLOWED");
			return true;
		} else {
			request_proc(thread_id, msgid, buffer, size, out);
			return false;
		}
	}

	protected boolean client_hangup() {
		ServerThread thread = (ServerThread) Thread.currentThread();
		boolean hangup;
		try {
			thread._socket.setSoTimeout(1);
			int n = thread.in.read();
			hangup = (n == -1);
		} catch (SocketTimeoutException e) {
			hangup = false;
		} catch (IOException e) {
			hangup = true;
		}
		return hangup;
	}

	public static void main(String argv[]) throws Exception {
		int i;
		String port = null;
		String logname = null;
		int max_threads = 3;
		for (i = 0; i < argv.length; i++) {
			if (argv[i].charAt(0) == '-') {
				switch (argv[i].charAt(1)) {
				case 'l':
					i++;
					logname = argv[i];
					break;
				case 't':
					i++;
					max_threads = Integer.parseInt(argv[i]);
					break;
				default:
					printUsage();
					System.exit(1);
				}
			} else if (port == null) {
				port = argv[i];
			} else {
				printUsage();
				System.exit(1);
			}
		}
		if (port == null) {
			printUsage();
			System.exit(1);
		}
		SoccomServer server;
		if (logname != null)
			server = new SoccomServer(port, logname);
		else
			server = new SoccomServer(port, System.out);
		server.max_threads = max_threads;
		server.start(false);
	}

}
