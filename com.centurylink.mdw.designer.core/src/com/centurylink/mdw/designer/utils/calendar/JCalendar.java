/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.utils.calendar;
import com.centurylink.mdw.designer.MainFrame;

import java.awt.*;

import javax.swing.*;

import java.awt.event.*;

import javax.swing.text.JTextComponent;

import java.text.DateFormatSymbols;
import java.util.Date;
import java.util.Locale;
import java.util.Calendar;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * This class creates a JDialog with calendar details. User can set/select any date from this which can be populated
 * in DateTextField.
 * @version : 1.0
 * @jdk version tested on : 1.4
 */
public class JCalendar extends JDialog implements PropertyChangeListener
{
	private static final long serialVersionUID = 1L;

    protected JYearChooser yearChooser;
    protected JMonthChooser monthChooser;
    protected JDayChooser dayChooser;

    public static final int RIGHT_SPINNER = 0;
    public static final int LEFT_SPINNER = 1;
    public static final int NO_SPINNER = 2;
    public MainFrame frame;
    protected JButton btnOk;
    protected JButton btnCancel;

    protected boolean okPressed = false;

    private Calendar calendar;

    public JCalendar(Frame parent, String title, boolean modal)
    {
        this(parent, title, modal, 0);
    }

    // monthSpinner can have following values
    // RIGHT_SPINNER, LEFT_SPINNER, NO_SPINNER
    // Default is RIGHT_SPINNER
    public JCalendar(Frame parent, String title, boolean modal, int monthSpinner)
    {
        super(parent, title, modal);
        init(monthSpinner);
    }

    public JCalendar(Dialog parent, String title, boolean modal,MainFrame pFrame)
    {
        super(parent, title, modal);
        frame = pFrame;
        init(0);
    }

    private void init(int monthSpinner)
    {
        okPressed = false;

        getContentPane().setLayout(new BorderLayout());
        calendar = Calendar.getInstance();

        dayChooser = new JDayChooser(calendar.get(Calendar.DAY_OF_MONTH));
        monthChooser = new JMonthChooser(monthSpinner, calendar.get(Calendar.MONTH));
        yearChooser = new JYearChooser(calendar.get(Calendar.YEAR));
        btnOk = new JButton("Ok");
        btnCancel = new JButton("Cancel");
        monthChooser.setYearChooser(yearChooser);
        monthChooser.setDayChooser(dayChooser);
        yearChooser.setDayChooser(dayChooser);

        JPanel pnlTop = new JPanel();
        pnlTop.setLayout(new GridLayout(1, 3));
        pnlTop.add(monthChooser);
        pnlTop.add(yearChooser);
        JPanel pnlBottom = new JPanel();
        pnlBottom.add(btnOk);
        pnlBottom.add(btnCancel);
        getContentPane().add(pnlTop, "North");
        getContentPane().add(dayChooser, "Center");
        getContentPane().add(pnlBottom, "South");

        pack();
        setResizable(false);

        dayChooser.addPropertyChangeListener(this);
        monthChooser.addPropertyChangeListener(this);
        yearChooser.addPropertyChangeListener(this);

        SymAction lSymAction = new SymAction();
        btnOk.addActionListener(lSymAction);
        btnCancel.addActionListener(lSymAction);
    }

    class SymAction implements ActionListener
    {
        public void actionPerformed(ActionEvent event)
        {
            Object object = event.getSource();
            if (object == btnOk)
                btnOk_ActionPerformed(event);
            else if (object == btnCancel)
                btnCancel_ActionPerformed(event);
        }
    }

    private void btnOk_ActionPerformed(ActionEvent event)
    {
        okPressed = true;
        closeWindow();
    }

    private void btnCancel_ActionPerformed(ActionEvent event)
    {
        okPressed = false;
        closeWindow();
    }

    private void closeWindow()
    {
        setVisible(false);
        dispose();
    }

    private void setCalendar(Calendar date, boolean flag)
    {
        Calendar cDate = calendar;
        calendar = date;

        if(flag)
        {
            yearChooser.setYear(date.get(Calendar.YEAR));
            monthChooser.setMonth(date.get(Calendar.MONTH));
            dayChooser.setDay(date.get(Calendar.DAY_OF_MONTH));
        }

        firePropertyChange("calendar", cDate, calendar);
    }

    public boolean isOkPressed()
    {
        return okPressed;
    }

    public void setCalendar(Calendar date)
    {
        setCalendar(date, true);
    }

    public Calendar getCalendar()
    {
        return calendar;
    }

    public void setDate(Date uDate)
    {
        Calendar cDate = Calendar.getInstance(Locale.getDefault());

        if( uDate != null )
        {
            cDate.clear();
            cDate.setTime(uDate);
        }

        setCalendar(cDate, true);
    }

    public Date getDate()
    {
        return calendar.getTime();
    }

    public void setFont(Font font)
    {
        super.setFont(font);
        if(dayChooser != null)
        {
            dayChooser.setFont(font);
            monthChooser.setFont(font);
            yearChooser.setFont(font);
        }
    }

    public void setForeground(Color color)
    {
        super.setForeground(color);
        if(dayChooser != null)
        {
            dayChooser.setForeground(color);
            monthChooser.setForeground(color);
            yearChooser.setForeground(color);
        }
    }

    public void setBackground(Color color)
    {
        super.setBackground(color);
        if(dayChooser != null)
            dayChooser.setBackground(color);
    }

    public void propertyChange(PropertyChangeEvent propertychangeevent)
    {
        if(calendar != null)
        {
            Calendar cDate = (Calendar)calendar.clone();
            if(propertychangeevent.getPropertyName().equals("day"))
            {
                cDate.set(Calendar.DAY_OF_MONTH, ((Integer)propertychangeevent.getNewValue()).intValue());
                setCalendar(cDate, false);
            }
            else if(propertychangeevent.getPropertyName().equals("month"))
            {
                cDate.set(Calendar.MONTH, ((Integer)propertychangeevent.getNewValue()).intValue());
                setCalendar(cDate, false);
            }
            else if(propertychangeevent.getPropertyName().equals("year"))
            {
                cDate.set(Calendar.YEAR, ((Integer)propertychangeevent.getNewValue()).intValue());
                setCalendar(cDate, false);
            }
        }
    }

    public void setVisible(boolean b)
    {
        if(b)
        {
            Rectangle bounds = getParent().getBounds();
            Rectangle abounds = getBounds();

            setLocation(bounds.x + (bounds.width - abounds.width)/ 2, bounds.y + (bounds.height - abounds.height)/2);

            okPressed = false;
        }
        super.setVisible(b);
    }

    public void setVisible(boolean b, int x, int y) {
    	if (b) {
    		setLocation(x, y);
    		okPressed = false;
    	}
    	super.setVisible(b);
    }

    public void setEnabled(boolean flag)
    {
        super.setEnabled(flag);
        if(dayChooser != null)
        {
            dayChooser.setEnabled(flag);
            monthChooser.setEnabled(flag);
            yearChooser.setEnabled(flag);
        }
    }

    public JDayChooser getDayChooser()
    {
        return dayChooser;
    }

    public JMonthChooser getMonthChooser()
    {
        return monthChooser;
    }

    public JYearChooser getYearChooser()
    {
        return yearChooser;
    }
}

class JDayChooser extends JPanel implements ActionListener, KeyListener, FocusListener
{
	private static final long serialVersionUID = 1L;

    private JButton days[];
    private JButton selectedDay;

    private static final DateFormatSymbols   dfSymb  = new DateFormatSymbols();
    private static final String  DAYS[] = dfSymb.getShortWeekdays();

    private int day;
    private Calendar today;
    private Calendar calendar;

    // foreground colors
    private static final Color RED_COLOR = new Color(164, 0, 0);
    private static final Color BLUE_COLOR = new Color(0, 0, 164);
    private static final Color TODAY_COLOR = new Color(164, 0, 0);
    // background colors
    private static final Color HEADER_COLOR = new Color(180, 180, 200);
    private static final Color SELECTED_COLOR = new Color(200, 200, 160);
    private static final Color UNSELECTED_COLOR = new Color(230, 230, 188);

    public JDayChooser()
    {
        this((Calendar.getInstance(Locale.getDefault())).get(Calendar.DAY_OF_MONTH));
    }

    public JDayChooser(int iDay)
    {
        days = new JButton[49];
        selectedDay = null;

        today = Calendar.getInstance(Locale.getDefault());
        calendar = (Calendar) today.clone();

        setLayout(new GridLayout(7, 7));

        for(int i=0; i<7; i++)
        {
            for(int j=0; j<7; j++)
            {
                int k = j + 7 * i;
                if(i == 0)
                {
                    days[k] = new JButton()
                              {
                    			  private static final long serialVersionUID = 1L;

                                  public void addMouseListener(MouseListener mouselistener)
                                  {
                                  }

                                  public boolean isFocusable()
                                  {
                                      return false;
                                  }
                              };

                    days[k].setBackground(HEADER_COLOR);
                }
                else
                {
                    days[k] = new JButton("x");
                    days[k].addActionListener(this);
                    days[k].addKeyListener(this);
                    days[k].addFocusListener(this);
                    days[k].setBackground(UNSELECTED_COLOR);
                }

                days[k].setMargin(new Insets(0, 0, 0, 0));
                days[k].setFocusPainted(false);
                days[k].setBorder(BorderFactory.createEtchedBorder());

                add(days[k]);
            }
        }

        init();
        setDay(iDay);
    }

    protected void init()
    {

        for(int i=0; i<7; i++)
        {
            days[i].setText(DAYS[i+1]);

            if((i+1)==1 || (i+1)==7)
                days[i].setForeground(RED_COLOR);
            else
                days[i].setForeground(BLUE_COLOR);
        }

        drawDays();
    }

    protected void drawDays()
    {
        Calendar cDate = (Calendar) calendar.clone();
        cDate.set(Calendar.DAY_OF_MONTH, 1);
        int dayOfWeek = cDate.get(Calendar.DAY_OF_WEEK);

        int i = 7;
        for(int m=1; m<dayOfWeek; m++,i++)
        {
            days[i].setVisible(false);
            days[i].setText("");
        }

        Color color = getForeground();
        int curMonth = cDate.get(Calendar.MONTH);

        int j = 0;
        for(;curMonth == cDate.get(Calendar.MONTH);)
        {
            days[i + j].setText(Integer.toString(j + 1));
            days[i + j].setVisible(true);

            if(cDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
               cDate.get(Calendar.YEAR) == today.get(Calendar.YEAR))
                days[i + j].setForeground(TODAY_COLOR);
            else
                days[i + j].setForeground(color);

            if(j + 1 == day)
            {
                days[i + j].setBackground(SELECTED_COLOR);
                selectedDay = days[i + j];
            }
            else
                days[i + j].setBackground(UNSELECTED_COLOR);

            j++;
            cDate.add(Calendar.DAY_OF_MONTH, 1);
        }

        for(int k = i + j; k<49; k++)
        {
            days[k].setVisible(false);
            days[k].setText("");
        }
    }

    public void setDay(int iDay)
    {
        if(iDay < 1)
            iDay = 1;

        Calendar cDate = (Calendar)calendar.clone();
        cDate.set(Calendar.DAY_OF_MONTH, 1);
        cDate.add(Calendar.MONTH, 1);
        cDate.add(Calendar.DAY_OF_MONTH, -1);
        int lastDay = cDate.get(Calendar.DAY_OF_MONTH);
        if( iDay > lastDay )
            iDay = lastDay;

        int k = day;
        day = iDay;
        if(selectedDay != null)
        {
            selectedDay.setBackground(UNSELECTED_COLOR);
            selectedDay.repaint();
        }

        for(int i=7; i<49; i++)
        {
            if(!days[i].getText().equals(Integer.toString(day)))
                continue;
            selectedDay = days[i];
            selectedDay.setBackground(SELECTED_COLOR);
            break;
        }

        firePropertyChange("day", k, day);
    }

    public int getDay()
    {
        return day;
    }

    public void setMonth(int month)
    {
        calendar.set(Calendar.MONTH, month);
        setDay(day);
        drawDays();
    }

    public void setYear(int year)
    {
        calendar.set(Calendar.YEAR, year);
        drawDays();
    }

    public void setCalendar(Calendar cDate)
    {
        calendar = cDate;
        drawDays();
    }

    public void setFont(Font font)
    {
        if(days != null)
        {
            for(int i=0; i<49; i++)
                days[i].setFont(font);
        }
    }

    public void setForeground(Color color)
    {
        super.setForeground(color);
        if(days != null)
        {
            for(int i=7; i<49; i++)
                days[i].setForeground(color);

            drawDays();
        }
    }

    public void actionPerformed(ActionEvent actionevent)
    {
        JButton jbutton = (JButton)actionevent.getSource();
        String s = jbutton.getText();
        int i = (new Integer(s)).intValue();
        setDay(i);
    }

    public void focusGained(FocusEvent focusevent)
    {
        JButton jbutton = (JButton)focusevent.getSource();
        String s = jbutton.getText();
        if(s != null && !s.equals(""))
            actionPerformed(new ActionEvent(focusevent.getSource(), 0, null));
    }

    public void focusLost(FocusEvent focusevent)
    {
    }

    public void keyPressed(KeyEvent keyevent)
    {
        byte byte0 = keyevent.getKeyCode() != 38 ? keyevent.getKeyCode() != 40 ? keyevent.getKeyCode() != 37 ? ((byte) (keyevent.getKeyCode() != 39 ? ((byte) (0)) : 1)) : -1 : 7 : -7;
        if(byte0 != 0)
        {
            for(int i = getComponentCount() - 1; i >= 0; i--)
            {
                if(getComponent(i) != selectedDay)
                    continue;
                i += byte0;
                if(i > 7 && i < days.length && days[i].isVisible())
                    days[i].requestFocus();
                break;
            }
        }
    }

    public void keyTyped(KeyEvent keyevent)
    {
    }

    public void keyReleased(KeyEvent keyevent)
    {
    }

    public void setEnabled(boolean flag)
    {
        super.setEnabled(flag);
        for(short s=0; s<days.length; s++)
            if(days[s] != null)
                days[s].setEnabled(flag);
    }

    public static void main(String args[])
    {
        /*JFrame jframe = new JFrame("JDayChooser");
        jframe.getContentPane().add(new JDayChooser());
        jframe.pack();
        jframe.setVisible(true);*/
    }
}

class JYearChooser extends JSpinNumberField
{
	private static final long serialVersionUID = 1L;

    private JDayChooser dayChooser;

    public JYearChooser(int year)
    {
        super(0,9999,year);
        dayChooser = null;
        addFocusListener(this);
    }

    public JYearChooser()
    {
        this(Calendar.getInstance(Locale.getDefault()).get(Calendar.YEAR));
    }

    public void setValue(int year)
    {
        setYear(year);
    }

    public void setYear(int year)
    {
        int j = super.getValue();

        super.setValue(year);

        if(dayChooser != null)
            dayChooser.setYear(year);

        firePropertyChange("year", j, super.getValue());
    }

    public int getYear()
    {
        return super.getValue();
    }

    public void setDayChooser(JDayChooser jdaychooser)
    {
        dayChooser = jdaychooser;
    }

    public void focusGained(FocusEvent e)
    {
        Object object = e.getSource();
        if(object instanceof JTextComponent)
            ((JTextComponent)object).selectAll();
    }

    public static void main(String args[])
    {
        /*JFrame jframe = new JFrame("JYearChooser");
        jframe.getContentPane().add(new JYearChooser());
        jframe.pack();
        jframe.setVisible(true);*/
    }
}

class JMonthChooser extends JPanel implements ItemListener, AdjustmentListener
{
	private static final long serialVersionUID = 1L;

    public static final int RIGHT_SPINNER = 0;
    public static final int LEFT_SPINNER = 1;
    public static final int NO_SPINNER = 2;

    private static final DateFormatSymbols   dfSymb  = new DateFormatSymbols();
//    private static final String  SHORT_MONTHS[] = dfSymb.getShortMonths();
    private static final String  MONTHS[] = dfSymb.getMonths();

    private int oldBarValue = 0;
    private int month = 0;

    private JDayChooser dayChooser;
    private JYearChooser yearChooser;

    private JComboBox comboBox;
    private JScrollBar scrollBar;

    public JMonthChooser()
    {
        this(0, Calendar.getInstance(Locale.getDefault()).get(Calendar.MONTH));
    }

    public JMonthChooser(int spinner, int month)
    {
        dayChooser = null;
        yearChooser = null;

        setLayout(new BorderLayout());

        comboBox = new JComboBox();
        for(int i=0; i <MONTHS.length; i++)
            comboBox.addItem(MONTHS[i]);
        comboBox.addItemListener(this);
        add(comboBox, "Center");

        if(spinner != NO_SPINNER)
        {
            scrollBar = new JScrollBar(JScrollBar.VERTICAL,(12-month),0,-10000, 10000);
            scrollBar.setPreferredSize(new Dimension(scrollBar.getPreferredSize().width, getPreferredSize().height));
            scrollBar.setVisibleAmount(0);
            if(spinner == RIGHT_SPINNER)
                add(scrollBar, "East");
            else
                add(scrollBar, "West");

            scrollBar.addAdjustmentListener(this);
        }

        setMonth(month, true);
    }

    public void itemStateChanged(ItemEvent itemevent)
    {
        int index = comboBox.getSelectedIndex();
        if(index >= 0)
            setMonth(index, false);
    }

    public void adjustmentValueChanged(AdjustmentEvent adjustmentevent)
    {

        boolean flag = true;
        int newBarValue = adjustmentevent.getValue();
        if(newBarValue > oldBarValue)
            flag = false;

        oldBarValue = newBarValue;

        int j = getMonth();
        if(flag)
        {
            if(++j == 12)
            {
                j = 0;
                if(yearChooser != null)
                    yearChooser.setYear(yearChooser.getYear() + 1);
            }
        }
        else if(--j == -1)
        {
            j = 11;
            if(yearChooser != null)
                yearChooser.setYear(yearChooser.getYear() - 1);
        }
        setMonth(j);
    }

    private void setMonth(int month, boolean bCombo)
    {
        int j = this.month;

        if(month < 0)
            this.month = 0;
        else if(month > 11)
            this.month = 11;
        else
            this.month = month;

        if(bCombo)
            comboBox.setSelectedIndex(this.month);

        if(dayChooser != null)
            dayChooser.setMonth(this.month);

        firePropertyChange("month", j, this.month);
    }

    public void setMonth(int i)
    {
        setMonth(i, true);
    }

    public int getMonth()
    {
        return month;
    }

    public void setDayChooser(JDayChooser jdaychooser)
    {
        dayChooser = jdaychooser;
    }

    public void setYearChooser(JYearChooser jyearchooser)
    {
        yearChooser = jyearchooser;
    }

    public void setEnabled(boolean flag)
    {
        super.setEnabled(flag);
        comboBox.setEnabled(flag);
        if(scrollBar != null)
            scrollBar.setEnabled(flag);
    }

    public static void main(String args[])
    {
        /*JFrame jframe = new JFrame("MonthChooser");
        JYearChooser yearChooser = new JYearChooser();
        JMonthChooser monthChooser = new JMonthChooser();

        jframe.getContentPane().add(yearChooser);
        jframe.getContentPane().add(monthChooser);

        monthChooser.setYearChooser(yearChooser);

        jframe.pack();
        jframe.setVisible(true);*/
    }
}

