/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.soccom;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

class SoccomMessage
{
    static final int HEADER_SIZE = 48;
    static final int MSGID_SIZE = 24;
    static final int MSGSIZE_OFFSET = 40;
    
    static synchronized void setHostName() {
    	if (hostname != null) return;
		try {
			InetAddress addr = InetAddress.getLocalHost();
			hostname = addr.getHostName();
			hostname = (hostname+"**********").substring(0,10);
		} catch (UnknownHostException e) {
			hostname = "**********";
		}
    }

    static byte[] makeMessage(String msgbody, String msgid)
    {
	StringBuffer sb = new StringBuffer();
	int length = msgbody.length();
	if (_seqno==0) {
		if (hostname==null) setHostName();
	    Random ran = new Random();
	    _randomId = nf6.format(ran.nextInt(1000000));
	}
	if (msgid!=null) {
	    sb.append(msgid);
	} else {
	    sb.append("#@");
	    sb.append(hostname);
	    sb.append(_randomId);
	    sb.append(nf6.format(nextSeqNo()));
	}
	sb.append(df.format(new Date()));
	sb.append(nf8.format(length));
	sb.append(msgbody);
	return sb.toString().getBytes();
    }

    static byte[] makeMessageSpecial(String special, String msgid)
    {
	StringBuffer sb = new StringBuffer();
	if (_seqno==0) {
	    Random ran = new Random();
	    _randomId = nf6.format(ran.nextInt(1000000));
	}
	if (msgid!=null) {
	    sb.append(msgid);
	} else {
	    sb.append("#@");
	    sb.append(hostname);
	    sb.append(_randomId);
	    sb.append(nf6.format(nextSeqNo()));
	}
	sb.append(df.format(new Date()));
	sb.append(special);
	return sb.toString().getBytes();
    }


    synchronized static int nextSeqNo()
    {
	return _seqno++;
    }

    private static String _randomId;
    private static int _seqno = 0;
    private static SimpleDateFormat df;
    private static DecimalFormat nf6, nf8;
	private static String hostname = null;
    
    static {
	df = new SimpleDateFormat("yyyyMMddHHmmss00");
	nf6 = new DecimalFormat("000000");
	nf8 = new DecimalFormat("00000000");
    }

}
