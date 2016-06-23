
    var posx = 0;
    var posy = 0;

/*  function populateDateInput
    parameters: 
        id - the id of the text input field of the date
        event - the event id
*/
    function populateDateInput(id, event)
    {
      dateInput = findAnElement(id);            
      g_Calendar.show(event, dateInput, 'mm/dd/yyyy');
    }

    function findAnElement(name)
    {
      for (i = 0; i < document.forms.length; i++)
      {
        form = document.forms[i];
        for (j = 0; j < form.elements.length; j++)
        {
          if (form.elements[j].name.indexOf(name) >= 0)
          {
            return form.elements[j];
          }
        }
      }
      return null;
    }

    var timeoutDelay = 2000; // milliseconds, change this if you like, set to 0 for the calendar to never auto disappear
    var g_startDay = 0; // 0=sunday, 1=monday

    // used by timeout auto hide functions
    var timeoutId = false;

    // the now standard browser sniffer class
    function Browser()
    {
      this.dom = document.getElementById?1:0;
      this.ie4 = (document.all && !this.dom)?1:0;
      this.ns4 = (document.layers && !this.dom)?1:0;
      this.ns6 = (this.dom && !document.all)?1:0;
      this.ie5 = (this.dom && document.all)?1:0; //using ie6 is true for this and dom
      this.ok = this.dom || this.ie4 || this.ns4;
      this.platform = navigator.platform;
      this.ff = (navigator.userAgent.indexOf("Firefox") >= 0)?1:0; //ff is also true for ns6 and dom
      this.ie9 = (navigator.userAgent.indexOf("MSIE 9") >= 0 || navigator.userAgent.indexOf("MSIE 10") >= 0 || (!!navigator.userAgent.match(/Trident.*./)) );      
      this.ie11 = !!navigator.userAgent.match(/Trident.*./);     
      if(this.ie11){    	
    	  this.ns6 =0;
    	  this.ie5 = 1;
    	
      }
      this.chrome = (navigator.userAgent.indexOf("Chrome") >= 0) ? 1 : 0; // Chrome is also true for ns6 and dom
    }

    var browser = new Browser();

    // dom browsers require this written to the HEAD section
    document.writeln('<style>');
    document.writeln('#container {');
    document.writeln('position : absolute;');
    document.writeln('left : 100px;');
    document.writeln('top : 100px;');
    document.writeln('width : 124px;');;
    browser.platform=='Win32'?height=140:height=145;
    document.writeln('height : ' + height +'px;');
    document.writeln('clip:rect(0px 124px ' + height + 'px 0px);');
    //document.writeln('overflow : hidden;');
    document.writeln('z-index : 9999;');
    document.writeln('visibility : hidden;');
    document.writeln('background-color : #ffffff');
    document.writeln('}');
    
    // Popup Calendar styling
    document.writeln('TD.cal { FONT-SIZE: 11px; COLOR: #000000; FONT-FAMILY: Arial,Helvetica,Sans-serif; BACKGROUND-COLOR: #ffffff; }');
    document.writeln('SELECT.month { FONT-SIZE: 11px; WIDTH: 85px; COLOR: #000000; FONT-FAMILY: Arial,Helvetica,Sans-serif }');
    document.writeln('INPUT.year { FONT-SIZE: 11px; WIDTH: 30px; COLOR: #000000; FONT-FAMILY: Arial,Helvetica,Sans-serif }');
    document.writeln('TD.calDaysColor { FONT-SIZE: 11px; COLOR: #ffffff; FONT-FAMILY: Arial,Helvetica,Sans-serif; BACKGROUND-COLOR: #399cce }');
    document.writeln('TD.calWeekend { FONT-SIZE: 11px; COLOR: #ffffff; FONT-FAMILY: Arial,Helvetica,Sans-serif; BACKGROUND-COLOR: #f3f3f3 }');
    document.writeln('TD.calBgColor { FONT-SIZE: 11px; COLOR: #399cce; FONT-FAMILY: Arial,Helvetica,Sans-serif; BACKGROUND-COLOR: #399cce }');
    document.writeln('.calBorderColor { FONT-SIZE: 11px; COLOR: #ffffff; FONT-FAMILY: Arial,Helvetica,Sans-serif; BACKGROUND-COLOR: #a9a9a9 }');
    document.writeln('TD.calHighlightColor { FONT-SIZE: 11px; COLOR: #ffffff; FONT-FAMILY: Arial,Helvetica,Sans-serif; BACKGROUND-COLOR: #ffff7d }');
    document.writeln('A.cal { FONT-SIZE: 11px; COLOR: #000000; FONT-FAMILY: Arial,Helvetica,Sans-serif; TEXT-DECORATION: none; }');
    document.writeln('A.cal:hover { FONT-SIZE: 11px; COLOR: #ff0000; FONT-FAMILY: Arial,Helvetica,Sans-serif; TEXT-DECORATION: none }');
    document.writeln('.disabled { FONT-SIZE: 11px; COLOR: #808080; FONT-FAMILY: Arial,Helvetica,Sans-serif; TEXT-DECORATION: none }');
    document.writeln('.detailsLabelColumnBaselineAlignedDivLine {');
    document.writeln('  font-family: Verdana, Arial, Helvetica, sans-serif;');
    document.writeln('  background-color: #D6E1EE;');
    document.writeln('  vertical-align: baseline;');
    document.writeln('  font-size: 10px;');
    document.writeln('  padding: 2px;');
    document.writeln('  border-right-width: 1px;');
    document.writeln('  border-right-style: solid;');
    document.writeln('  border-right-color: #FFFFFF;');
    document.writeln('}');
    document.writeln('.noteLinePageTop {');
    document.writeln('  font-family: Verdana, Arial, Helvetica, sans-serif;');
    document.writeln('  font-size: 12px;');
    document.writeln('  color: #000000;');
    document.writeln('  clear: left;');
    document.writeln('  margin: 10px;');
    document.writeln('  width: auto;');
    document.writeln('  padding: 2px;');
    document.writeln('}');
    document.writeln('.secondLevelNavItemTextOn {');
    document.writeln('  font-family: Verdana, Arial, Helvetica, sans-serif;');
    document.writeln('  color: #053368;');
    document.writeln('  font-size: 10px;');
    document.writeln('  text-transform: uppercase;');
    document.writeln('  text-decoration: none;');
    document.writeln('}');
    
    document.writeln('</style>');

            
    document.write('<div id="container"');
    if (timeoutDelay) document.write(' onmouseout="calendarTimeout();" onmouseover="if (timeoutId) clearTimeout(timeoutId);"');
    document.write('></div>');

    var g_Calendar;  // global to hold the calendar reference, set by constructor

    function calendarTimeout()
    {
      if (browser.ie4 || browser.ie5)
      {
        if (window.event.srcElement && window.event.srcElement.name!='month') timeoutId=setTimeout('g_Calendar.hide();',timeoutDelay);
      }
      if (browser.ns6 || browser.ns4 || browser.ff || browser.chrome) //adding ff to be safe of future releases
      {
        timeoutId=setTimeout('g_Calendar.hide();',timeoutDelay);
      }
    }

    // constructor for calendar class
    function MdwCalendar()
    {
      g_Calendar = this;
      // some constants needed throughout the program
      this.daysOfWeek = new Array("Su","Mo","Tu","We","Th","Fr","Sa");
      this.months = new Array("January","February","March","April","May","June","July","August","September","October","November","December");
      this.daysInMonth = new Array(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31);

      if (browser.ns4)
      {
        var tmpLayer = new Layer(127);
        if (timeoutDelay)
        {
          tmpLayer.captureEvents(Event.MOUSEOVER | Event.MOUSEOUT);
          tmpLayer.onmouseover = function(event) { if (timeoutId) clearTimeout(timeoutId); };
          tmpLayer.onmouseout = function(event) { timeoutId=setTimeout('g_Calendar.hide()',timeoutDelay);};
        }
        tmpLayer.x = 100;
        tmpLayer.y = 100;
        tmpLayer.bgColor = "#ffffff";
      }

      if (browser.dom || browser.ie4)
      {
        var tmpLayer = browser.dom?document.getElementById('container'):document.all.container;
      }
      this.containerLayer = tmpLayer;
      if (browser.ns4 && browser.platform=='Win32')
      {
        this.containerLayer.clip.height=134;
        this.containerLayer.clip.width=127;
      }

    }

    MdwCalendar.prototype.getFirstDOM = function()
    {
      var thedate = new Date();
      thedate.setDate(1);
      thedate.setMonth(this.month);
      thedate.setFullYear(this.year);
      return thedate.getDay();
    }

    MdwCalendar.prototype.getDaysInMonth = function ()
    {
       if (this.month!=1)
       {
         return this.daysInMonth[this.month]
       }
       else 
       {
         // is it a leap year
         if (Date.isLeapYear(this.year))
         {
          return 29;
         }
         else
         {
          return 28;
         }
       }
    }

    MdwCalendar.prototype.buildString = function()
    {
      var tmpStr = '<form onSubmit="this.year.blur();return false;"><table width="100%" border="0" cellspacing="0" cellpadding="2" class="calBorderColor"><tr><td valign="top"><table width="100%" border="0" cellspacing="0" cellpadding="1" class="calBgColor">';
      tmpStr += '<tr>';
      tmpStr += '<td width="60%" class="cal" align="left">';
      tmpStr += '<table border="0" cellspacing="0" cellpadding="0"><tr><td class="cal"><a href="javascript: g_Calendar.changeMonth(-1);" style="text-decoration:none"><</a></td><td width="100%" align="center" class="cal">' + this.months[this.month] + '</td><td class="cal"><a href="javascript: g_Calendar.changeMonth(+1);" style="text-decoration:none">></a></td><td width="7">&nbsp;</td></tr></table>';
      tmpStr += '</td>';
      /* observation : for some reason if the below event is changed to 'onChange' rather than 'onBlur' it totally crashes IE (4 and 5)! */
      tmpStr += '<td width="40%" align="right" class="cal">';
      tmpStr += '<table border="0" cellspacing="0" cellpadding="0"><tr><td class="cal"><a href="javascript: g_Calendar.changeYear(-1);" style="text-decoration:none"><</a></td><td class="cal" width="100%" align="center">' + this.year + '</td><td class="cal"><a href="javascript: g_Calendar.changeYear(+1);" style="text-decoration:none">></a></td></tr></table>'
      tmpStr += '</td>';
      tmpStr += '</tr>';
      tmpStr += '</table>';
      var iCount = 1;

      var iFirstDOM = (7+this.getFirstDOM()-g_startDay)%7; // to prevent calling it in a loop

      var iDaysInMonth = this.getDaysInMonth(); // to prevent calling it in a loop

      tmpStr += '<table width="100%" border="0" cellspacing="0" cellpadding="1" class="calBgColor">';
      tmpStr += '<tr>';
      
      for (var i=0;i<7;i++)
      {
        tmpStr += '<td align="center" class="calDaysColor">' + this.daysOfWeek[(g_startDay+i)%7] + '</td>';
      }
      
      tmpStr += '</tr>';
      var tmpFrom = parseInt('' + this.dateFromYear + this.dateFromMonth + this.dateFromDay,10);
      var tmpTo = parseInt('' + this.dateToYear + this.dateToMonth + this.dateToDay,10);
      var tmpCompare;
      
      for (var j=1;j<=6;j++)
      {
        tmpStr += '<tr>';
        for (var i=1;i<=7;i++)
        {
          tmpStr += '<td width="16" align="center" '
        
          if ( (7*(j-1) + i)>=iFirstDOM+1  && iCount <= iDaysInMonth)
          {
            if (iCount==this.day && this.year==this.oYear && this.month==this.oMonth) tmpStr += 'class="calHighlightColor"';
            else {
            if (i==7-g_startDay || i==((7-g_startDay)%7)+1) tmpStr += 'class="calWeekend"';
            else tmpStr += 'class="cal"';
          }
         
          tmpStr += '>';
          
          /* could create a date object here and compare that but probably more efficient to convert to a number
            and compare number as numbers are primitives */
          tmpCompare = parseInt('' + this.year + padZero(this.month) + padZero(iCount),10);
          if (tmpCompare >= tmpFrom && tmpCompare <= tmpTo) 
          {
            tmpStr += '<a class="cal" href="javascript: g_Calendar.clickDay(' + iCount + ');">' + iCount + '</a>';
          }
          else
          {
            tmpStr += '<span class="disabled">' + iCount + '</span>';
          }
          iCount++;
          }
          else
          {
           if  (i==7-g_startDay || i==((7-g_startDay)%7)+1) tmpStr += 'class="calWeekend"'; else tmpStr +='class="cal"';
           tmpStr += '>&nbsp;';
          }
          tmpStr += '</td>'
        }
        tmpStr += '</tr>'
      }
      tmpStr += '</table></td></tr></table></form>'
      return tmpStr;
    }

    MdwCalendar.prototype.selectChange = function()
    {
      this.month = (browser.ns6 && !browser.ff)?this.containerLayer.ownerDocument.forms[0].month.selectedIndex:this.containerLayer.document.forms[0].month.selectedIndex;
      this.writeString(this.buildString());
    }

    MdwCalendar.prototype.inputChange = function()
    {
      var tmp = (browser.ns6 && !browser.ff)?this.containerLayer.ownerDocument.forms[0].year:this.containerLayer.document.forms[0].year;
      if (tmp.value >=1900 || tmp.value <=2100)
      {
        this.year = tmp.value;
        this.writeString(this.buildString());
      }
      else
      {
        tmp.value = this.year;
      }
    }
    
    MdwCalendar.prototype.changeYear = function(incr)
    {
      (incr==1)?this.year++:this.year--;
      this.writeString(this.buildString());
    }
    
    MdwCalendar.prototype.changeMonth = function(incr)
    {
      if (this.month==11 && incr==1)
      {
        this.month = 0;
        this.year++;
      }
      else
      {
        if (this.month==0 && incr==-1)
        {
          this.month = 11;
          this.year--;
        }
        else
        {
          (incr==1)?this.month++:this.month--;
        }
      }
      this.writeString(this.buildString());
    }

    MdwCalendar.prototype.clickDay = function(day)
    {
      if (this.dateFormat=='dd-mmm-yyyy' || this.dateFormat=='dd/mmm/yyyy') this.target.value = day + this.dateDelim + this.months[this.month].substr(0,3) + this.dateDelim + this.year;
      if (this.dateFormat=='dd/mm/yyyy' || this.dateFormat=='dd-mm-yyyy') this.target.value = day + this.dateDelim + (this.month+1) + this.dateDelim + this.year;
      if (this.dateFormat=='mm/dd/yyyy' || this.dateFormat=='mm-dd-yyyy') this.target.value = (this.month+1) + this.dateDelim + day + this.dateDelim + this.year;
      if (this.dateFormat=='yyyy-mm-dd') this.target.value = this.year + this.dateDelim + (this.month+1) + this.dateDelim + day;

      if (browser.ns4) this.containerLayer.hidden=true;
      
      if (browser.dom || browser.ie4)
      {
        this.containerLayer.style.visibility='hidden';
      }

      splitTargetName = this.target.name.split(":");
      for (i = 0; i < splitTargetName.length; i++)
      {
        targetName = splitTargetName[i];
      }
      
      if (targetName == 'createTaskDueDate_dateInput2') this.target.onchange();
      
      this.target.focus();
    }

    MdwCalendar.prototype.writeString = function(str)
    {
      if (browser.ns4)
      {
        this.containerLayer.document.open();
        this.containerLayer.document.write(str);
        this.containerLayer.document.close();
      }
      if (browser.dom || browser.ie4)
      {
        this.containerLayer.innerHTML = str;
      }
    }

    MdwCalendar.prototype.show = function(event, target, dateFormat, dateFrom, dateTo)
    {
      // calendar can restrict choices between 2 dates, if however no restrictions
      // are made, let them choose any date between 1900 and 3000
      if (dateFrom)
        this.dateFrom = dateFrom;
      else
        this.dateFrom = new Date(1900,0,1);
        this.dateFromDay = padZero(this.dateFrom.getDate());
        this.dateFromMonth = padZero(this.dateFrom.getMonth());
        this.dateFromYear = this.dateFrom.getFullYear();
        
      if (dateTo) this.dateTo = dateTo; else this.dateTo = new Date(3000,0,1);
      
      this.dateToDay = padZero(this.dateTo.getDate());
      this.dateToMonth = padZero(this.dateTo.getMonth());
      this.dateToYear = this.dateTo.getFullYear();
      
      if (dateFormat) this.dateFormat = dateFormat; else this.dateFormat = 'dd-mmm-yyyy';
      
      switch (this.dateFormat)
      {
        case 'dd-mmm-yyyy':
        case 'dd-mm-yyyy':
        case 'yyyy-mm-dd':
          this.dateDelim = '-';
        break;
        case 'dd/mm/yyyy':
        case 'mm/dd/yyyy':
        case 'dd/mmm/yyyy':
          this.dateDelim = '/';
        break;
      }
      
      if (browser.ns4) 
      {
        if (!this.containerLayer.hidden)
        {
          this.containerLayer.hidden=true;
          return;
        }
      }
       
      if (browser.dom || browser.ie4)
      {
        // closes calendar popup when clicking on calendar icon a second time
        if (this.containerLayer.style.visibility=='visible') 
        {
          this.containerLayer.style.visibility='hidden';
          return;
        }
      }

      if (browser.ie5 || browser.ie4)
      {    
        var event = window.event;
      }
      
      if (browser.ns4)
      {
        this.containerLayer.x = event.x+10;
        this.containerLayer.y = event.y-5;
      }
      
      if (browser.ie5 || browser.ie4)
      {
        var obj = event.srcElement;
        x = 0;
        while (obj.offsetParent != null) 
        {
          x += obj.offsetLeft;
          obj = obj.offsetParent;
        }
        x += obj.offsetLeft;
        y = 0;
        var obj = event.srcElement;

        while (obj.offsetParent != null)
        {
          if (obj.tagName != 'DIV')  // Ignore DIV tag that the QControl portal inserts
          {
            y += obj.offsetTop;
          }
          obj = obj.offsetParent;
        }
        
        y += obj.offsetTop;
        this.containerLayer.style.left = x+35;
        if (browser.ie9)
          this.containerLayer.style.left = (x+18) + "px";
      
        if (event.y > 0)
        {
          this.containerLayer.style.top = y;
          if (browser.ie9)
            this.containerLayer.style.top = (event.y-10) + "px";
        }
      }
      
      if (browser.ns6 && !browser.ff && !browser.chrome)
      {
        this.containerLayer.style.left = event.pageX+10;
        this.containerLayer.style.top = event.pageY-5;
      }
      
      if (browser.ff)
      {
        this.containerLayer.style.left = (event.pageX+10) + 'px';
        this.containerLayer.style.top = (event.pageY-5) + 'px';
      }
      
      if(browser.chrome)
      {  
      this.containerLayer.style.left = (event.pageX+10) + 'px';
      this.containerLayer.style.top = (event.pageY-5) + 'px';
      }
      this.target = target;
      var tmp = this.target.value;

      if (tmp && tmp.value && tmp.value.split(this.dateDelim).length==3 && tmp.value.indexOf('d')==-1)
      {
        var atmp = tmp.value.split(this.dateDelim)
        
        switch (this.dateFormat)
        {
          case 'dd-mmm-yyyy':
          case 'dd/mmm/yyyy':
           for (var i=0;i<this.months.length;i++)
           {
             if (atmp[1].toLowerCase()==this.months[i].substr(0,3).toLowerCase())
             {
               this.month = this.oMonth = i;
               break;
             }
           }
           this.day = parseInt(atmp[0],10);
           this.year = this.oYear = parseInt(atmp[2],10);
           break;
         case 'dd/mm/yyyy':
         case 'dd-mm-yyyy':
           this.month = this.oMonth = parseInt(atmp[1]-1,10);
           this.day = parseInt(atmp[0],10);
           this.year = this.oYear = parseInt(atmp[2],10);
           break;
         case 'mm/dd/yyyy':
         case 'mm-dd-yyyy':
           this.month = this.oMonth = parseInt(atmp[0]-1,10);
           this.day = parseInt(atmp[1],10);
           this.year = this.oYear = parseInt(atmp[2],10);
           break;
         case 'yyyy-mm-dd':
           this.month = this.oMonth = parseInt(atmp[1]-1,10);
           this.day = parseInt(atmp[2],10);
           this.year = this.oYear = parseInt(atmp[0],10);
           break;
        }
    
      }
      else
      { // no date set, default to today
        var theDate = new Date();
         this.year = this.oYear = theDate.getFullYear();
         this.month = this.oMonth = theDate.getMonth();
         this.day = this.oDay = theDate.getDate();
      }

      this.writeString(this.buildString());
      // and then show it!
       if (browser.ns4)
       {
         this.containerLayer.hidden=false;
       }
       if (browser.dom || browser.ie4)
       {
         this.containerLayer.style.visibility='visible';
       }
    }

    MdwCalendar.prototype.hide = function()
    {
      if (browser.ns4) this.containerLayer.hidden = true;
      if (browser.dom || browser.ie4)
      {
        this.containerLayer.style.visibility='hidden';
      }
    }

    function handleDocumentClick(e)
    {   
      if (browser.ie4 || browser.ie5) e = window.event;

      if (browser.ns6 && !browser.ff && !browser.chrome)
      {
        var bTest = (e.pageX > parseInt(g_Calendar.containerLayer.style.left,10) && e.pageX <  (parseInt(g_Calendar.containerLayer.style.left,10)+125) && e.pageY < (parseInt(g_Calendar.containerLayer.style.top,10)+125) && e.pageY > parseInt(g_Calendar.containerLayer.style.top,10));
        if (e.target.name!='imgCalendar' && e.target.name!='month'  && e.target.name!='year' && e.target.name!='calendar' && !bTest)
        {
          g_Calendar.hide();
        }
      }
      
      if (browser.ie4 || browser.ie5)
      {
        if (!g_Calendar)
        {
          return;
        }
        // extra test to see if user clicked inside the calendar but not on a valid date, we don't want it to disappear in this case
        var bTest = (e.x > parseInt(g_Calendar.containerLayer.style.left,10) && e.x <  (parseInt(g_Calendar.containerLayer.style.left,10)+125) && e.y < (parseInt(g_Calendar.containerLayer.style.top,10)+125) && e.y > parseInt(g_Calendar.containerLayer.style.top,10));
        if (e.srcElement.name!='imgCalendar' && e.srcElement.name!='month' && e.srcElement.name!='year' && !bTest & typeof(e.srcElement)!='object')
        {
          g_Calendar.hide();
        }
      }
      
      if (browser.ns4) g_Calendar.hide();
    }

    // utility function
    function padZero(num) 
    {
      return ((num <= 9) ? ("0" + num) : num);
    }
    
    // Finally licked extending  native date object;
    Date.isLeapYear = function(year){ if (year%4==0 && ((year%100!=0) || (year%400==0))) return true; else return false; }
    Date.daysInYear = function(year){ if (Date.isLeapYear(year)) return 366; else return 365;}
    var DAY = 1000*60*60*24;
    Date.prototype.addDays = function(num){ 
      return new Date((num*DAY)+this.valueOf()); 
    }

    // events capturing, careful you don't override this by setting something in the onload event of
    // the body tag
    /*  Commented out because of idiosyncrasies in the portal - added to the body tag.
    window.onload=function()
    {
      new MdwCalendar(new Date());
      if (browser.ns4)
      {
        if (typeof document.NSfix == 'undefined')
        {
          document.NSfix = new Object();
          document.NSfix.initWidth=window.innerWidth;
          document.NSfix.initHeight=window.innerHeight;
        }
      }
    }
    */
  
    if (browser.ns4) window.onresize = function()
    {
      if (document.NSfix.initWidth!=window.innerWidth || document.NSfix.initHeight!=window.innerHeight) window.location.reload(false);
    } // ns4 resize bug workaround
    
    window.document.onclick=handleDocumentClick;
  
    function findPosX(obj)
    {
      var curleft = 0;
      if (obj.offsetParent)
      {
        while (obj.offsetParent)
        {
          curleft += obj.offsetLeft
          obj = obj.offsetParent;
        }
      }
      else if (obj.x)
        curleft += obj.x;
      return curleft;
    }

    function findPosY(obj)
    {
      var curtop = 0;
      if (obj.offsetParent)
      {
        while (obj.offsetParent)
        {
          curtop += obj.offsetTop
          obj = obj.offsetParent;
        }
      }
      else if (obj.y)
        curtop += obj.y;
      return curtop;
    }
  
    function setMouseCoordinates(e)
    {

      if (!e) var e = window.event;
      if (e.pageX || e.pageY)
      {
        posx = e.pageX;
        posy = e.pageY;
      }
      else if (e.clientX || e.clientY)
      {
        posx = e.clientX + document.body.scrollLeft;
        posy = e.clientY + document.body.scrollTop;
      }
      // posx and posy contain the mouse position relative to the document
    }
  
    function getAbsoluteLeft(objectId)
    {
      // Get an object left position from the upper left viewport corner
      // Tested with relative and nested objects
      //o = document.getElementById(objectId);
      o = objectId;
      oLeft = o.offsetLeft;            // Get left position from the parent object
      while(o.offsetParent!=null)
      {   // Parse the parent hierarchy up to the document element
        oParent = o.offsetParent;    // Get parent object reference
        oLeft += oParent.offsetLeft; // Add parent left position
        o = oParent;
      }
      // Return left postion
      return oLeft;
    }

    function getAbsoluteTop(objectId)
    {
      // Get an object top position from the upper left viewport corner
      // Tested with relative and nested objects
      //o = document.getElementById(objectId);
      o = objectId;
      oTop = o.offsetTop;            // Get top position from the parent object
      while(o.offsetParent!=null)
      { // Parse the parent hierarchy up to the document element
        oParent = o.offsetParent;  // Get parent object reference
        oTop += oParent.offsetTop; // Add parent top position
        o = oParent;
      }
      // Return top position
      return oTop;
    }

    new MdwCalendar(new Date());