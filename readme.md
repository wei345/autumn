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

## 功能

页面

* 主页面，左侧有树形菜单，目录节点在前面，按照字母顺序排序，已选中当前页面。右侧是页面内容。
* 页面里的图片正常显示。例如 /git/git
* 访问不存在的页面，显示 404 模版页面，响应状态码 404。

登录

* 主页面没有登录按钮，打开 /login，显示登录表单，登录成功跳转到首页。
* 如果一个 IP 一天内登录失败 10 次，那么该 IP 再次登录会被拒绝，提示"稍后再试"，持续一天。
* 登录成功后，浏览器会记住登录状态一年。
* 如果修改了密码，session 失效后需要重新登录。

权限

* 未登录状态访问无权限页面，会跳转到登录页面，登录成功后回到之前的页面。
* 已登录状态，左侧树形菜单显示所有页面。

登出

* /logout 会跳转到首页，cookie ME 会被删除，客户端处于未登录状态。

缓存

* 页面缓存。如果页面内容或模版没有变化，那么刷新页面响应 304，即使重启应用，刷新页面也响应 304。
* 开发过程中模版文件变化后，10 秒内页面更新。

异常

* 处理请求发生异常时，显示 templates/error 里和状态码对应的错误页面，用户不会看到 Tomcat 错误页面。

TODO 搜索
  页面快速搜索页面名
  搜索文件内容
  支持 AND
  搜索结果页面名匹配的排在前面，然后按匹配次数排序

TODO 合并 css

TODO 合并 js

