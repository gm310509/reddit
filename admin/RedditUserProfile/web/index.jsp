<!DOCTYPE html>
<!--
Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Html.html to edit this template
-->
<html>
    <head>
        <title>Reddit User Activity Profiler</title>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <link rel="stylesheet" href="css/stylesheet.css">
    </head>
    <body>
        <div name="header">
            <h1>Reddit User Activity Profile</h1>
        </div>
        <div name="dataEntry">
            <p>
                Reddit user activity profiler.
            </p>
            <p>
            <form name="queryForm" id="frmQuery" action="RedditQueryUser">
                <label for="target">Reddit user name: u/</label>
                <input type="text" name="targetUserId" id="target" size="30" placeholder="user name" required autofocus/>
                <br><br>
                
                <input type="submit" name="submitButton" id="submit" value="Submit"/>
            </form>
            </p>
        </div>
        <div name="results">
            
        </div>
    </body>
</html>

