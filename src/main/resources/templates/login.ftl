<#ftl output_format="HTML">
<#include "inc/head.ftl">
    <div class="row">
        <div class="column column-33 column-offset-33">
            <p>${message!}</p>
            <form method="POST" action="">
                <label for="username">Username</label>
                <input type="text" id="username" name="username" value="${username!?esc}" placeholder="Username"<#if !username?has_content> autofocus</#if>>
                <label for="password">Password</label>
                <input type="password" id="password" name="password" placeholder="Password"<#if username?has_content> autofocus</#if>>
                <input class="button-primary" type="submit" value="Login">
            </form>
        </div>
    </div>
<#include "inc/foot.ftl">