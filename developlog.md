# Develop log

2019.01.05

* Release v1.1.0
  * 优化 UI。
  * Redis 改为可选的。
  * 可设置 Google Analytics ID。
  * 提高页面加载速度。包括压缩 JS、异步加载 JS、所有静态资源设置版本号和浏览器缓存。
  * 代码块高亮、行号。
  * 一些其他改进和 bug 修复。

12.31 - 01.05

* 设置 expires header。0.2d。
* 页面里的图片增加版本号并设置浏览器缓存。0.8d。
* 设置 Cookie Secure。0.5d。
* 表格可以横向滚动。0.5d。
* 移动设备上搜索框获得焦点自动滚动到顶；移动设备上点击标签或分类，搜索框不自动获得焦点；在大窗口中，QuickSearch 结果区设置最大高度，可滚动。0.5d。
* 代码块可以显示行号（JS 实现），默认不启用。0.5d。

12.26 - 12.30

* 优化窗口高度较小时（如手机横屏）的 UI。0.5d。
* 更新文档。0.5d。
* Redis 改为可选的。0.3d。
* 可设置 Google Analytics ID 和站点标题。0.5d。
* script 标签移至 head 并添加 async 属性。0.5d。
* 代码块高亮。0.5d。
* 压缩 js。1d。
* 改进 QuickSearch 面板样式。0.5d。

2018.12.25

* Release v1.0.0
  * 映射 Markdown 目录到网站。
  * 对应数据目录结构的树形菜单。
  * 页面内容 TOC。
  * 缓存除搜索结果之外的所有资源，如：页面 html、js、css、图片。
  * 访问控制。
  * 全文搜索。
  * 快速搜索。
  * 分类和标签。
  * 自适应桌面和移动设备的 UI。
  * 文件下载，.md 源码等。

12.17 - 12.24

* 完成 readme.md; 分类标签选择栏自动与搜索框内容同步。1d。
* 优化 Heading 字体样式; 添加快捷键 s 和 t。0.5d。
* 改进错误页; 改进文件下载。0.5d。
* 改进 StaticService、Controller。1d。
* 可切换是否固定 Sidebar、Table of Contents、Header。1d。
* QuickSearch 在本次搜索中记住展开状态; 显示结果总数; QuickSearch 面板添加阴影。1d。
* 移除 actuator; 启动脚本设置 JVM 参数; password hash 改为 sha512; AES 从 128 改为 256。1d。
* 手机横屏也启用左中右布局; 改进 QuickSearch  UI; 加快应用启动速度。1d。

12.10 - 12.16

* tree.json URL 增加版本号; 增加 QuickSearch 功能，浏览器本地快速搜索分类、标签、标题、路径。0.7d (9 号 22 点 - 10 号 7 点)。
* QuickSearch 默认显示前几条结果，点击按钮显示全部。0.3d。
* 优化 QuickSearch 外观; 添加快捷键 g, G, u, d; 改进页面布局; 添加 help 页面。1d。
* 句子完全匹配支持通配符。1d。
* 链接 hover 显示下划线; 重构搜索，添加 AbstractPageHitMatcher。1d。
* 整理 quick_search.js; 改进 QuickSearch 样式; 改进 QuickSearch 分类和标签选择。1d。
* sticky sidebar; sticky toc。1d。
* 第一次显示 tree 时，当前页面对应的节点自动滚动到屏幕中间; 更新 readme。1d。

12.04 - 12.09

* 可以上线了，set context path，Spring MVC 正确跳转 HTTPS，启动脚本。1d。
* 改进日志。0.5d
* 改进搜索结果高亮。0.5d
* js, css URL 增加版本号防止浏览器缓存不更新。1d。
* 添加 MimeTypeUtil; 提高 IntersectionOperator 速度; 文件名或标题与搜索词相等的页面排在前面; 添加 data reload endpoint; 添加 tomcat apr。1d。
* 可以给代码块增加行号（服务端实现）。1d。

11.30 - 12.03

* TOC。1d。
* UI，样式，布局。3d。

11.18 - 11.29

Search

* input parser。2d。
* 搜索逻辑和页面展示。1d。
* 搜索结果排序。0.5d。
* 搜索结果高亮。1d。

页面

* 读取本地目录，解析 Markdown，缓存，Tree，Web 页面。3d。
* 用户，登录和权限。 3d。
