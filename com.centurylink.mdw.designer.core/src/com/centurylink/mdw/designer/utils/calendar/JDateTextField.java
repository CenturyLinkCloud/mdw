/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.utils.calendar;

import java.awt.*;
import javax.swing.* ;

import java.awt.event.* ;

import java.text.DateFormatSymbols;

import java.util.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * This is Date text field that can be used to show java.util.Calendar or java.util.Date in a text field.
 * The date can be shown in different formats.
 * @version : 1.0
 * @jdk version tested on : 1.4
 */
public class JDateTextField extends JPanel implements ActionListener {

	private static final long serialVersionUID = 1L;

	public static final int US_SHORT_DATE = 11;
    public static final int IN_SHORT_DATE = 12;
    public static final int LONG_DATE = 13;

    private static final char FW_SLASH = '/';
    private static final char DASH = '-';

    private static final String FM_US_SHORT_DATE = "MM/dd/yyyy";
    private static final String FM_IN_SHORT_DATE = "dd/MM/yyyy";
    private static final String FM_LONG_DATE = "dd-MMM-yyyy";

    private static final DateFormatSymbols   dfSymb  = new DateFormatSymbols();
    private static final String  SHORT_MONTHS[] = dfSymb.getShortMonths();

    private static final String NULL_SHORT_DATE = "";
    private static final String NULL_LONG_DATE = "";

    private int format = US_SHORT_DATE;

    private JTextField textField;
	private JButton showCalendar, clearButton;
	private JCalendar calendar=null;
	private ActionListener listener = null;
	private String actionCommand = null;

	private Calendar date = null;

    public JDateTextField()
    {
        this(US_SHORT_DATE);
    }

    public JDateTextField(int iFormat)
    {
        super(null);
        textField = new JTextField();
        textField.setBounds(0,0,75,20);
        add(textField);
        setFormat(iFormat);
		showCalendar = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("images/date.gif")));
        add(showCalendar);
        showCalendar.setBounds(80, 0, 25, 20);
        showCalendar.setActionCommand("show");
        showCalendar.addActionListener(this);
		clearButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("images/reset.gif")));
        add(clearButton);
        clearButton.setBounds(105, 0, 25, 20);
        clearButton.setActionCommand("clear");
        clearButton.addActionListener(this);

        textField.setEditable(false);
    }

    public void setText(String text)
    {
        try
        {
            setDate(text);
        }
        catch(ParseException pEx)
        {
            validateNull(null);
        }
    }

    public String getText()
    {
        return getText(format);
    }

    public String getText(int iFormat)
    {
        return formateDate(date,iFormat);
    }

    public void setDate(String text)
    	throws ParseException
    {
        if( text == null )
            validateNull(null);
        else
            validateDate(text);
    }

    public void setEnabled(boolean enable)
    {
        super.setEnabled(enable);

        if( enable )
        {
            setBackground(Color.white);
            setForeground(Color.black);
        }
        else
        {
            setBackground(Color.lightGray);
            setForeground(Color.darkGray);
        }
    }

    public void setEditable(boolean enable)
    {
        if( enable )
        {
            textField.setBackground(Color.white);
            textField.setForeground(Color.black);
        }
        else
        {
        	textField.setBackground(Color.lightGray);
        	textField.setForeground(Color.darkGray);
        }
    }

    public void setCalendar(Calendar cDate)
    {
        this.date = cDate;

        if(cDate == null)
             validateNull(null);
        else
        	textField.setText(formateDate(cDate,format));
    }

    public Calendar getCalendar()
    {
        return this.date;
    }

    public void setDate(Date uDate)
    {
        if(uDate == null)
        {
            validateNull(null);
            return;
        }

        Calendar cDate = Calendar.getInstance();
        cDate.clear();
        cDate.setTime(uDate);

        setCalendar(cDate);
    }

    public Date getDate()
    {
        if( this.date == null )
            return null;
        else
            return this.date.getTime();
    }

    public void setFormat(int iFormat)
    {
        switch(iFormat)
        {
            case US_SHORT_DATE:
            default:
            	textField.setText(NULL_SHORT_DATE);
                format = US_SHORT_DATE;
                break;

            case IN_SHORT_DATE:
                textField.setText(NULL_SHORT_DATE);
                format = IN_SHORT_DATE;
                break;

            case LONG_DATE:
            	textField.setText(NULL_LONG_DATE);
                format = LONG_DATE;
                break;
        }
    }

    private void validateNull(String text)
    {
        if( format == US_SHORT_DATE && text == null )
            textField.setText(NULL_SHORT_DATE);
        else if( format == IN_SHORT_DATE && text == null )
        	textField.setText(NULL_SHORT_DATE);
        else if( format == LONG_DATE && text == null )
        	textField.setText(NULL_LONG_DATE);

        this.date = null;
    }

    private void validateDate(String text)
    throws ParseException
    {
        int textLength = text.length();

        if( format == US_SHORT_DATE )
        {
            if( textLength > 10 )
                throw new ParseException("Length of passed date ("+text+") must be 10 atmost : "+FM_US_SHORT_DATE, textLength);

            validateUSShortDate(text);
        }
        else if( format == IN_SHORT_DATE )
        {
            if( textLength > 10 )
                throw new ParseException("Length of passed date ("+text+") must be 10 atmost : "+FM_IN_SHORT_DATE, textLength);

            validateINShortDate(text);
        }
        else if( format == LONG_DATE )
        {
            if( textLength > 11 )
                throw new ParseException("Length of passed date ("+text+") must be 11 atmost : "+FM_LONG_DATE, textLength);

            validateLongDate(text);
        }
    }

    private void validateUSShortDate(String text)
    throws ParseException
    {
        int iFirst = -1, iLast = -1, tokens = 0;
        for(int i=0; i<text.length(); i++)
        {
            char c = text.charAt(i);
            if( c == FW_SLASH )
            {
                tokens++;
                if( iFirst == -1 )
                    iFirst = i;
                else
                    iLast = i;
            }
        }

        if( tokens != 2 )
            throw new ParseException( "Insufficient fields ("+text+"), need 3 fields in format: "+FM_US_SHORT_DATE, 0);

        String  sMonth  = text.substring(0,iFirst);
        String  sDate   = text.substring(iFirst+1,iLast);
        String  sYear   = text.substring(iLast+1);

        int iMonth = -1, iDate = -1, iYear = -1;

        try
        {
            iMonth  = Integer.parseInt(sMonth);
            iDate   = Integer.parseInt(sDate);
            iYear   = Integer.parseInt(sYear);
        }
        catch(NullPointerException npEx)
        {
            throw new ParseException(npEx.getMessage(),0);
        }
        catch(NumberFormatException nfEx)
        {
            throw new ParseException(nfEx.getMessage(),0);
        }

        iMonth--;
        if( iMonth < 0 || iMonth > 11 )
            throw   new ParseException( "Month field should be between 1 and 12.", iMonth);

        int days = getDaysInMonth(iYear,iMonth);
        if( iDate < 1 || iDate > days )
            throw new ParseException( "Date field should be between 1 and "+days+".", iDate);

        if( iYear < 0 )
            throw new ParseException( "Year field can't be negative.", iYear);

        // If only two digits of year are specified ...
        if( iYear < 100 )
        {
            // Use the current year ...
            iYear += Calendar.getInstance().get(Calendar.YEAR) / 100 * 100;
        }

        // Create the Calendar object with the converted values ...
        Calendar cDate = Calendar.getInstance();
        cDate.clear();
        cDate.set( iYear, iMonth, iDate );

        this.date = cDate;
        textField.setText(formateDate(cDate, format));
    }

    private void validateINShortDate(String text)
    throws ParseException
    {
        int iFirst = -1, iLast = -1, tokens = 0;
        for(int i=0; i<text.length(); i++)
        {
            char c = text.charAt(i);
            if( c == FW_SLASH )
            {
                tokens++;
                if( iFirst == -1 )
                    iFirst = i;
                else
                    iLast = i;
            }
        }

        if( tokens != 2 )
            throw new ParseException( "Insufficient fields ("+text+"), need 3 fields in format: "+FM_IN_SHORT_DATE, 0);

        String  sDate   = text.substring(0,iFirst);
        String  sMonth  = text.substring(iFirst+1,iLast);
        String  sYear   = text.substring(iLast+1);

        int iDate = -1, iMonth = -1, iYear = -1;

        try
        {
            iDate   = Integer.parseInt(sDate);
            iMonth  = Integer.parseInt(sMonth);
            iYear   = Integer.parseInt(sYear);
        }
        catch(NullPointerException npEx)
        {
            throw new ParseException(npEx.getMessage(),0);
        }
        catch(NumberFormatException nfEx)
        {
            throw new ParseException(nfEx.getMessage(),0);
        }

        int days = getDaysInMonth(iYear,iMonth);
        if( iDate < 1 || iDate > days )
            throw new ParseException( "Date field should be between 1 and "+days+".", iDate);

        iMonth--;
        if( iMonth < 0 || iMonth > 11 )
            throw   new ParseException( "Month field should be between 1 and 12.", iMonth);

        if( iYear < 0 )
            throw new ParseException( "Year field can't be negative.", iYear);

        // If only two digits of year are specified ...
        if( iYear < 100 )
        {
            // Use the current year ...
            iYear += Calendar.getInstance().get(Calendar.YEAR) / 100 * 100;
        }

        // Create the Calendar object with the converted values ...
        Calendar cDate = Calendar.getInstance();
        cDate.clear();
        cDate.set( iYear, iMonth, iDate );

        this.date = cDate;
        textField.setText(formateDate(cDate, format));
    }

    private void validateLongDate(String text)
    throws ParseException
    {
        int iFirst = -1, iLast = -1, tokens = 0;
        for(int i=0; i<text.length(); i++)
        {
            char c = text.charAt(i);
            if( c == DASH )
            {
                tokens++;
                if( iFirst == -1 )
                    iFirst = i;
                else
                    iLast = i;
            }
        }

        if( tokens != 2 )
            throw new ParseException( "Insufficient fields ("+text+"), need 3 fields in format: "+FM_LONG_DATE, 0);

        String  sDate   = text.substring(0,iFirst);
        String  sMonth  = text.substring(iFirst+1,iLast).toUpperCase();
        String  sYear   = text.substring(iLast+1);

        int iDate = -1, iMonth = -1, iYear = -1;

        try
        {
            iDate   = Integer.parseInt(sDate);
            iYear   = Integer.parseInt(sYear);
        }
        catch(NullPointerException npEx)
        {
            throw new ParseException(npEx.getMessage(),0);
        }
        catch(NumberFormatException nfEx)
        {
            throw new ParseException(nfEx.getMessage(),0);
        }

        int days = getDaysInMonth(iYear,iMonth);
        if( iDate < 1 || iDate > days )
            throw new ParseException( "Date field should be between 1 and "+days+".", iDate);

        for( iMonth=0; iMonth<SHORT_MONTHS.length && !SHORT_MONTHS[iMonth].toUpperCase().equals(sMonth); iMonth++);
        if( iMonth >= SHORT_MONTHS.length )
            throw   new ParseException( "Month field doesn't start with 'Jan' - 'Dec' :"+sMonth, 0);

        if( iYear < 0 )
            throw new ParseException( "Year field can't be negative.", iYear);

        // If only two digits of year are specified ...
        if( iYear < 100 )
        {
            // Use the current year ...
            iYear += Calendar.getInstance().get(Calendar.YEAR) / 100 * 100;
        }

        // Create the Calendar object with the converted values ...
        Calendar cDate = Calendar.getInstance();
        cDate.clear();
        cDate.set( iYear, iMonth, iDate );

        this.date = cDate;
        textField.setText(formateDate(cDate, format));
    }

    private static int getDaysInMonth(int iYear, int iMonth)
    {
        switch( iMonth )
        {
            case 3:
            case 5:
            case 8:
            case 10:
            return  30;

            case 1:     // February
                return ((iYear%4)==0 || (iYear%200)==0) ? 29 : 28;

            default:
                return  31;
        }
    }

    public String formateDate(Calendar cDate, int iFormat)
    {
        String sReturn = null;
        if( date == null )
            return sReturn;

        SimpleDateFormat xlsDateFormater;

        if( iFormat == US_SHORT_DATE )
        {
            xlsDateFormater = new SimpleDateFormat(FM_US_SHORT_DATE);
            sReturn = xlsDateFormater.format(cDate.getTime());
        }
        else if( iFormat == IN_SHORT_DATE )
        {
            xlsDateFormater = new SimpleDateFormat(FM_IN_SHORT_DATE);
            sReturn = xlsDateFormater.format(cDate.getTime());
        }
        else if( iFormat == LONG_DATE )
        {
            xlsDateFormater = new SimpleDateFormat(FM_LONG_DATE);
            sReturn = xlsDateFormater.format(cDate.getTime());
        }

        return sReturn;
    }

	public void actionPerformed(ActionEvent event) {
		if (event.getActionCommand().equals("show")) {
			if (calendar==null) {
		        Container frame = this;
		        while (frame!=null && ! (frame instanceof Frame)) {
		        	frame = frame.getParent();
		        }
		        calendar = new JCalendar((Frame)frame, "Select Date", true);
			}
			Calendar cDate = calendar.getCalendar();
	        if( cDate == null ) cDate = Calendar.getInstance();
	        calendar.setCalendar(cDate);
	        Container frame = this;
	        int cal_x = showCalendar.getX();
	        int cal_y = showCalendar.getY();
	        while (frame!=null && ! (frame instanceof Frame)) {
	        	cal_x += frame.getX();
	        	cal_y += frame.getY();
	        	frame = frame.getParent();
	        }
	        calendar.setVisible(true, cal_x, cal_y);
	        if( calendar.isOkPressed() ) {
	        	setDate(roundDate(calendar.getCalendar(), false));
	        	if (listener!=null) {
	        		ActionEvent e = new ActionEvent(this, 0, actionCommand);
	        		listener.actionPerformed(e);
	        	}
	        }
	        calendar.setVisible(false);
		} else {
			setDate((Date)null);
		}
	}

    private Date roundDate(Calendar calendar, boolean startOfDay) {
        calendar.set(Calendar.HOUR_OF_DAY, startOfDay?0:23);
        calendar.set(Calendar.MINUTE, startOfDay?0:59);
        calendar.set(Calendar.SECOND, startOfDay?0:59);
        return calendar.getTime();
    }

    @Override
    public void setBounds(Rectangle r) {
    	super.setBounds(r);
    	textField.setBounds(0, 0, r.width-55, r.height);
    	showCalendar.setLocation(r.width-50, 0);
    	clearButton.setLocation(r.width-25, 0);
    }

    @Override
    public void setBounds(int x, int y, int w, int h) {
    	super.setBounds(x, y, w, h);
    	textField.setBounds(0, 0, w-55, h);
    	showCalendar.setLocation(w-50, 0);
    	clearButton.setLocation(w-25, 0);
    }

    public void addFocusListener(FocusListener listener) {
    	textField.addFocusListener(listener);
    }

    public void addActionListener(ActionListener listener) {
    	this.listener = listener;
    }

    public void setActionCommand(String cmd) {
    	actionCommand = cmd;
    }

}