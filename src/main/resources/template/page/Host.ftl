<#--
 #%L
 ActivityInfo Server
 %%
 Copyright (C) 2009 - 2013 UNICEF
 %%
 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as
 published by the Free Software Foundation, either version 3 of the 
 License, or (at your option) any later version.
 
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 
 You should have received a copy of the GNU General Public 
 License along with this program.  If not, see
 <http://www.gnu.org/licenses/gpl-3.0.html>.
 #L%
-->
<#-- @ftlvariable name="" type="org.activityinfo.server.bootstrap.model.HostPageModel" -->
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<#if appCacheEnabled>
<html manifest="ActivityInfo/ActivityInfo.appcache">
<#else>
<html>
</#if>
<head>
    <meta http-equiv="content-type" content="text/html; charset=UTF-8">
    <meta name="application-name" content="ActivityInfo"/>
    <meta name="description" content="ActivityInfo"/>
    <meta name="application-url" content="${appUrl}"/>
    <meta http-equiv="X-UA-Compatible" content="IE=9">
 
    <link rel="icon" href="ActivityInfo/desktopicons/16x16.png" sizes="16x16"/>
    <link rel="icon" href="ActivityInfo/desktopicons/32x32.png" sizes="32x32"/>
    <link rel="icon" href="ActivityInfo/desktopicons/48x48.png" sizes="48x48"/>
    <link rel="icon" href="ActivityInfo/desktopicons/64x64.png" sizes="64x64"/>
    <link rel="icon" href="ActivityInfo/desktopicons/128x128.png" sizes="128x128"/>

    <title>${domain.title}</title>
    <style type="text/css">
        #loading-box {
            position: absolute;
            left: 45%;
            top: 40%;
            padding: 2px;
            margin-left: -45px;
            z-index: 20001;
            border: 1px solid #ccc;
        }

        #loading-box .loading-indicator {
            background: #eef;
            font: bold 13px tahoma, arial, helvetica;
            padding: 10px;
            margin: 0;
            height: auto;
            color: #444;
        }

        #loading-box .loading-indicator img {
            margin-right: 8px;
            float: left;
            vertical-align: top;
        }

        #loading-msg {
            font: normal 10px tahoma, arial, sans-serif;
        }

        #loading-options {
            position: absolute;
            right: 10px;
            bottom: 10px;
            font: normal 13px tahoma, arial, sans-serif;
            text-align: right;
        }
        <#include "Application.css">
    </style>
    <script type="text/javascript">
        if(document.cookie.indexOf('authToken=') == -1 || 
            document.cookie.indexOf('userId') == -1 ||
            document.cookie.indexOf('email') == -1) {
            window.location = "/login" + window.location.hash;
        }
        var VersionInfo = {
            display: '$[display.version]',
            commitId: '$[git.commit.id]'
        };
    </script>    

    <script language='javascript' src='ActivityInfo/gxt224/flash/swfobject.js'></script>
    <script type="text/javascript" language="javascript" src="ActivityInfo/ActivityInfo.nocache.js"></script>
    <script type="text/javascript">
    

      var _gaq = _gaq || [];
      _gaq.push(['_setAccount', 'UA-11567120-1']);
      _gaq.push(['_setDomainName', '${domain.host}']);
      _gaq.push(['_setCustomVar', 2, 'Existing User', 'Yes', 1]);
               
      (function() {
        var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
        ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
        var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
      })();
    </script>
</head>
<body>
<iframe id="__printingFrame" style="width:0;height:0;border:0"></iframe>
<div id="loading">
    <div id="loading-box">
        <div class="loading-indicator">
            <img src="ActivityInfo/gxt224/images/default/shared/large-loading.gif" alt=""/>
            ${label.activityInfo} $[display.version]<br/>
            <span id="loading-msg">${label.loading}</span>

        </div>
    </div>
</div>
<iframe src="javascript:''" id="__gwt_historyFrame" tabIndex='-1'
        style="position:absolute;width:0;height:0;border:0"></iframe>
<iframe src="javascript:''" id="_downloadFrame" name="_downloadFrame" tabIndex='-1'
        style="position:absolute;width:0;height:0;border:0"></iframe>
<script type="text/javascript">
    var gaJsHost = (("https:" == document.location.protocol) ? "https://ssl." : "http://www.");
    document.write(unescape("%3Cscript src='" + gaJsHost + "google-analytics.com/ga.js' type='text/javascript'%3E%3C/script%3E"));
</script>
