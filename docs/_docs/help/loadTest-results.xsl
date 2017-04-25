<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
  xmlns:lxslt="http://xml.apache.org/xslt"
  xmlns:stringutils="xalan://org.apache.tools.ant.util.StringUtils">
<xsl:output method="html" indent="yes" encoding="US-ASCII"
  doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN" />
<xsl:decimal-format decimal-separator="." grouping-separator="," />

<xsl:param name="TITLE">MDW Load Test Report</xsl:param>

<xsl:template match="LoadTestResults">
    <html>
        <head>
            <title><xsl:value-of select="$TITLE"/></title>
    <style type="text/css">
      body {
        font:normal 68% verdana,arial,helvetica;
        color:#000000;
      }
      table tr td, table tr th {
          font-size: 68%;
      }
      table.details tr th{
        font-weight: bold;
        text-align:left;
        background:#a6caf0;
      }
      table.details tr td{
        background:#eeeee0;
      }

      p {
        line-height:1.5em;
        margin-top:0.5em; margin-bottom:1.0em;
      }
      h1 {
        margin: 0px 0px 5px; font: 165% verdana,arial,helvetica
      }
      h2 {
        margin-top: 1em; margin-bottom: 0.5em; font: bold 125% verdana,arial,helvetica
      }
      h3 {
        margin-bottom: 0.5em; font: bold 115% verdana,arial,helvetica
      }
      h4 {
        margin-bottom: 0.5em; font: bold 100% verdana,arial,helvetica
      }
      h5 {
        margin-bottom: 0.5em; font: bold 100% verdana,arial,helvetica
      }
      h6 {
        margin-bottom: 0.5em; font: bold 100% verdana,arial,helvetica
      }
      .Error {
        font-weight:bold; color:red;
      }
      .Failure {
        font-weight:bold; color:purple;
      }
      .Properties {
        text-align:right;
      }
      </style>
        </head>
        <body>
            <a name="top"></a>
            <xsl:call-template name="pageHeader"/>
			
			<!-- Summary part -->
            <xsl:call-template name="summary"/>
            <hr size="1" width="95%" align="left"/>
			
			<!-- Results  part -->
            <xsl:call-template name="results"/>
			
        </body>
    </html>
</xsl:template>

<!-- Page HEADER -->
<xsl:template name="pageHeader">
    <h2><xsl:value-of select="$TITLE"/></h2>
    <hr size="1"/>
</xsl:template>

    <xsl:template name="summary">
        <h3 style="margin-top:0px;padding-top:0px;"><xsl:value-of select="testsuite/@name"/></h3>
        
        <xsl:variable name="testCount" select="sum(testsuite/@tests)"/>
        <table class="details" border="0" cellpadding="5" cellspacing="2" width="95%">
        <tr valign="top">
            <th>Tests</th>
        </tr>
        <tr valign="top">
            <td><xsl:value-of select="$testCount"/></td>
        </tr>
        </table>
        <table border="0" width="95%">
        <tr>
        <td style="text-align: justify;">
        </td>
        </tr>
        </table>
    </xsl:template>
	
    <xsl:template name="results">
             <table class="details" border="0" cellpadding="5" cellspacing="2" width="95%">
              <xsl:call-template name="testcase.test.header"/>
			  <xsl:call-template name="testcase.test.results"/> 
            </table>
            <p/>
    </xsl:template>

<!-- method header -->
<xsl:template name="testcase.test.header">
    <tr valign="top">
        <th>Name</th>
        <th>Results</th>
    </tr>
</xsl:template>

<xsl:template name="testcase.test.results">
    <tr valign="top">
        <td>Total number of cases prepared</td>
        <td><xsl:value-of select="/testsuites/LoadTestResults/testsuite/CasesPrepared"/></td>
	</tr>
	<tr valign="top">
        <td>Total number of cases completed</td>
        <td><xsl:value-of select="/testsuites/LoadTestResults/testsuite/CasesCompleted"/></td>
    </tr>
	<tr valign="top">
        <td>Number of processes started</td>
        <td><xsl:value-of select="/testsuites/LoadTestResults/testsuite/ProcessesStarted"/></td>
    </tr>
	 <tr valign="top">
        <td>Total number of activities started</td>
        <td><xsl:value-of select="/testsuites/LoadTestResults/testsuite/ActivitiesStarted"/></td>
    </tr>
	<tr valign="top">
        <td>Total number of activities completed</td>
        <td><xsl:value-of select="/testsuites/LoadTestResults/testsuite/ActivitiesCompleted"/></td>
    </tr>
	<tr valign="top">
        <td>Start Time</td>
        <td><xsl:value-of select="/testsuites/LoadTestResults/testsuite/StartTime"/></td>
    </tr>
	<tr valign="top">
        <td>End Time</td>
        <td><xsl:value-of select="/testsuites/LoadTestResults/testsuite/EndTime"/></td>
    </tr>
	<tr valign="top">
        <td>Activities Per Hour</td>
        <td><xsl:value-of select="/testsuites/LoadTestResults/testsuite/ActivitiesPerHour"/></td>
    </tr>
</xsl:template>
	
</xsl:stylesheet>

