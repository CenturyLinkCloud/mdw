/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.testing;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.qwest.mbeng.MbengDocument;

public class TestFile extends File {

	private ArrayList<TestFileLine> lines;
	private File dir;

	public TestFile(File dir, String filename) {
		super(dir==null?filename:dir.getPath()+"/"+filename);
		this.dir = dir;
		lines = new ArrayList<TestFileLine>();
	}

	public List<TestFileLine> getLines() {
		return lines;
	}

	public String load() throws IOException, ParseException {
	    return load(null);
	}

    public String load(PreFilter prefilter) throws IOException, ParseException {
        FileReader reader = null;
        try {
            reader = new FileReader(this);
            char[] parseBuffer = new char[(int)super.length()];
            reader.read(parseBuffer);
            String filtered = new String(parseBuffer);
            if (prefilter != null) {
                filtered = prefilter.apply(filtered);
                parseBuffer = new char[filtered.length()];
                filtered.getChars(0, filtered.length(), parseBuffer, 0);
            }
            CommandParser parser = new CommandParser(this);
            lines = parser.parse(parseBuffer);
            return filtered;
        } finally {
            if (reader!=null) reader.close();
        }
    }

	public void addLine(TestFileLine line) {
		lines.add(line);
	}

	public void save() throws IOException {
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(this);
			for (TestFileLine line : lines) {
				for (int i=0; i<line.getWordCount(); i++) {
					if (i>0) writer.print(' ');
					saveWord(writer, line.getWord(i));
				}
				writer.print("\r\n");
			}
		} finally {
			if (writer!=null) writer.close();
		}
	}

	private void saveWord(PrintWriter writer, String word) throws IOException {
		int quote_level = 0;
		int n = 0;
		if (word != null)
		    n = word.length();
		if (n==0) quote_level = 1;
		else {
			for (int i=0; i<n && quote_level<2; i++) {
				char ch = word.charAt(i);
				if (ch=='\n' || ch=='\r' || ch=='"') quote_level = 2;
				else if (ch==' ' || ch=='\t' || ch=='<' || ch=='@') quote_level = 1;
			}
		}
		if (quote_level==2) {
			writer.print("<<=====\r\n");
//			writer.print(word);
			boolean lastIsCR = false;
			for (int i=0; i<n; i++) {
				char ch = word.charAt(i);
				if (ch=='\n' && !lastIsCR) writer.print('\r');
				writer.print(ch);
				lastIsCR = (ch=='\r');
			}
			writer.print("\r\n=====>>");
		} else if (quote_level==1) {
			writer.print('"');
			writer.print(word);
			writer.print('"');
		} else writer.print(word);
	}

	public void print() throws IOException {
		System.out.format("===%s===\n", super.getPath());
		FileReader reader = new FileReader(this);
		char[] buf = new char[(int)super.length()];
		reader.read(buf);
		reader.close();
		System.out.print(buf);
	}

	public int firstDiffLine(TestFile expected, Map<String,String> map,
			MbengDocument refdoc, PrintStream log) {
		int i=0, n = lines.size();
		TestFileLine myLine=null;
		for (TestFileLine itsLine : expected.lines) {
			if (itsLine.getCommand().startsWith("#")) continue;
			if (i>=n) return (n==0?1:myLine.getLineNumber());
			myLine = lines.get(i++);
			while (myLine.getCommand().startsWith("#")) {
				if (i>=n) return myLine.getLineNumber();
				myLine = lines.get(i++);
			}
			int m = myLine.getWordCount();
			for (int j=0; j<itsLine.getWordCount(); j++) {
				String itsWord = itsLine.getWord(j);
				if (itsWord.equals("#")) break;
                TestDataFilter filter = new TestDataFilter(itsWord, log, true);
				String myWord = myLine.getWord(j);
                itsWord = filter.applyFilters(map, refdoc);
                itsWord = filter.applyAnyNumberFilters(itsWord, myWord);
				if (j>=m) return myLine.getLineNumber();
				if (!(myWord.equals(itsWord) || TestCompare.matchRegex(itsWord, myWord))) {
				    return myLine.getLineNumber();
				}
			}
		}
		while (i<n) {
			myLine = lines.get(i++);
			if (!myLine.getCommand().startsWith("#")) return myLine.getLineNumber();
		}
		return -1;
	}

	public String readFile(String filename) throws IOException {
		File file;
	    if (dir!=null) {
	        file = new File(dir.getPath()+"/"+filename);
	        if (!file.exists()) file = new File(filename);
	    } else file = new File(filename);
	    char[] parseBuffer;
	    FileReader reader = null;
	    try {
	    	reader = new FileReader(file);
	        parseBuffer = new char[(int)file.length()];
	        reader.read(parseBuffer);
	        reader.close();
	        return new String(parseBuffer);
	    } finally {
	    	if (reader!=null)
				try {
					reader.close();
				} catch (IOException e) {
				}
	    }
	}

}
