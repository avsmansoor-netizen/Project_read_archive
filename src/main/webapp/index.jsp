<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>Email WebApp - Email Processor</title>
</head>
<body>
<h1>Email WebApp — Email Reader & Archiver</h1>
<p>This will read unseen emails in INBOX for accounts listed in <code>credentials.csv</code> and archive them.</p>
<form action="process" method="get">
    <button type="submit">Run processing now</button>
</form>
<p>Make sure <code>src/main/resources/credentials.csv</code> contains email,password rows.</p>
</body>
</html>
