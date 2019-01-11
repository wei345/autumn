<!DOCTYPE html>
<html>
<head><#--@formatter:off-->
    <#if googleAnalyticsId?has_content>
    <script>
        (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
            (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
                m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
        })(window,document,'script','https://www.google-analytics.com/analytics.js','ga');
        ga('create', '${googleAnalyticsId}', 'auto');
        ga('send', 'pageview');
    </script>
    </#if><#--@formatter:on-->
    <script>
        var autumn = {
            ctx: '${ctx!}',
            prefix: '${prefix!}',
            treeVersionKeyValue: '${treeVersionKeyValue!}'
        };
    </script>
    <script async src="${ctx!}/js/all.js?${jsVersionKeyValue!}"></script>
    <meta charset="utf-8">
    <meta name="google" content="notranslate">
    <meta http-equiv="Content-Language" content="en">
    <meta name="viewport" content="width=device-width,initial-scale=1"/>
    <title>${title!"No Title"} - ${siteTitle!}</title>
    <link rel="icon" href="${faviconUrl!}" type="image/x-icon"/>
    <link rel="stylesheet" href="${ctx!}/css/all.css?${cssVersionKeyValue!}"/>
</head>
<body class="multiple_columns">
<div class="container">