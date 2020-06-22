<!DOCTYPE html>
<html>
<head>
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
    <title>${pageTitle!"No Title"} - ${title!}</title>
    <link rel="icon" href="${faviconUrl!}" type="image/x-icon"/>
    <link rel="stylesheet" href="${ctx!}/css/all.css?${cssVersionKeyValue!}"/>
</head>
<body class="multiple_columns">
<div class="container">