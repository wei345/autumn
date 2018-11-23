<!DOCTYPE html>

<html>
<head>
    <link rel="stylesheet" href="/css/normalize.css"/>
    <link rel="stylesheet" href="/css/milligram.css"/>
    <link rel="stylesheet" href="/css/style.css"/>
</head>
<body>

<div class="container">
    <div class="row">
        <div class="column column-33 column-offset-33">
            <p>${message!}</p>
            <form method="POST" action="">
                <label for="username">Username</label>
                <input type="text" id="username" name="username" value="${username!}" placeholder="Username"<#if !username?has_content> autofocus</#if>>
                <label for="password">Password</label>
                <input type="password" id="password" name="password" placeholder="Password"<#if username?has_content> autofocus</#if>>
                <input class="button-primary" type="submit" value="Login">
            </form>
        </div>
    </div>
</div>
</body>

</html>