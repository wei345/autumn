## 数据

主要是 notes、templates。
登录用户和访客看到的内容是不一样的。
修改 java 代码也可能导致输出内容变化，例如修改 markdown 解析器配置。所以临时增加了 HelloController#CACHE_EXPIRE。

输出给浏览器的内容

* 页面内容。第一次访问时生成，md 或 template lastModified 变化或应用重启时清除缓存。
* css。应用启动时生成，文件 lastModified 变化时重新生成。
* js，tree 数据。应用启动时生成，文件 lastModified 变化时重新生成。

动态页面内容 cache key
userId:path
  md lastModified
  template lastModified
  md5
  content

静态内容 cache key
path
  file1 lastModified
  file2 lastModified
  md5
  content

有时 Command + R，浏览器不发送请求，Command + Shift + R 会发送请求。

## TODO

登录
显示数据目录里的图片
