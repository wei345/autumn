<#include "inc/head.ftl">
        <div class="login_box">
            <p>${message!}</p>
            <form method="POST" action="">
                <label for="username">Username</label>
                <input type="text" id="username" name="username" value="${username!}" placeholder="Username"<#if !username?has_content> autofocus</#if> autocomplete="off">
                <label for="password">Password</label>
                <input type="password" id="password" name="password" placeholder="Password"<#if username?has_content> autofocus</#if>>
                <input class="button-primary" type="submit" value="Login">
            </form>
        </div>
<#include "inc/foot.ftl">