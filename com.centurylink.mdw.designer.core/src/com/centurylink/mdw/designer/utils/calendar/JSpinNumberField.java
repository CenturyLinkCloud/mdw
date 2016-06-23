/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.utils.calendar;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

/**
 * This class creates a spin Number field, where user can either enter valus to the Text Field or values can be
 * increaed or decreased using spinner.
 * in DateTextField.
 * @version : 1.0
 * @jdk version tested on : 1.4
 */
public class JSpinNumberField extends JPanel implements AdjustmentListener, ActionListener, FocusListener
{
	private static final long serialVersionUID = 1L;

    protected JNumberTextField textField;
    protected JScrollBar scrollBar;
    private int min = 0;
    private int max = 0;
    private int value = 0;

    public JSpinNumberField(int min, int max, int value)
    {
        this.max =  max;
        this.min = min;
        this.value = value;

        setLayout(new BorderLayout());

        textField = new JNumberTextField(4);
        textField.setInt(value);
        textField.setHorizontalAlignment(JTextField.RIGHT);
        textField.setPreferredSize(new Dimension((new JTextField(String.valueOf(this.max))).getPreferredSize().width, textField.getPreferredSize().height));
        add(textField, "Center");

        scrollBar = new JScrollBar(JScrollBar.VERTICAL, value, 0, min, max);
        scrollBar.setPreferredSize(new Dimension(scrollBar.getPreferredSize().width, textField.getPreferredSize().height));
        scrollBar.setValue((max + min) - value);
        scrollBar.setVisibleAmount(0);
        add(scrollBar, "East");

        textField.addFocusListener(this);
        textField.addActionListener(this);
        scrollBar.addAdjustmentListener(this);
    }

    public JSpinNumberField(int min, int max)
    {
        this(min,max,0);
    }

    public void setValue(int val)
    {
        int j = value;
        if(val < min)
            value = min;
        else if(val > max)
            value = max;
        else
            value = val;

        textField.setInt(value);
        scrollBar.setValue((max + min) - value);

        firePropertyChange("value", j, value);
    }

    public int getValue()
    {
        return value;
    }

    public void setMinimum(int min)
    {
        if( min < 0 && !textField.isAllowNegative() )
            this.min = 0;

        this.min = min;
    }

    public int getMinimum()
    {
        return min;
    }

    public void setMaximum(int max)
    {
        if( max < 0 && !textField.isAllowNegative() )
            this.max = 0;

        textField.setPreferredSize(new Dimension((new JTextField(Integer.toString(max))).getPreferredSize().width, textField.getPreferredSize().height));

        this.max = max;
    }

    public int getMaximum()
    {
        return max;
    }

    public void setFont(Font font)
    {
        if( textField != null )
            textField.setFont(font);
    }

    public void adjustmentValueChanged(AdjustmentEvent adjustmentevent)
    {
        setValue((max + min) - adjustmentevent.getValue());
    }

    public void actionPerformed(ActionEvent actionevent)
    {
        setValue(textField.getInt());
    }

    public void focusGained(FocusEvent e)
    {
    }

    public void focusLost(FocusEvent e)
    {
        Object object = e.getSource();
        if(object instanceof JTextComponent)
            setValue(textField.getInt());
    }

    public void setEnabled(boolean flag)
    {
        super.setEnabled(flag);
        textField.setEnabled(flag);
        scrollBar.setEnabled(flag);
    }

    public static void main(String args[])
    {
        JFrame jframe = new JFrame("JSpinField");
        jframe.getContentPane().add(new JSpinNumberField(0,100));
        jframe.pack();
        jframe.setVisible(true);
    }
}