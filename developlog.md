# Develop log

11.18 - 11.29

页面

* 读取本地目录，解析 Markdown，缓存，Tree，Web 页面。3d。
* 用户，登录和权限，花了 3d。

Search

* input parser。2d。
* 搜索逻辑和页面展示。1d。
* 搜索结果排序。0.5d。
* 搜索结果高亮。1d。

11.30 - 12.03

* TOC。1d。
* UI，样式，布局。3d。

12.04 - 12.09

* 可以上线了，set context path，Spring MVC 正确跳转 HTTPS，启动脚本。1d。
* 改进日志。0.5d
* 改进搜索结果高亮。0.5d
* js, css URL 增加版本号防止浏览器缓存不更新。1d。
* 添加 MimeTypeUtil; 提高 IntersectionOperator 速度; 文件名或标题与搜索词相等的页面排在前面; 添加 data reload endpoint; 添加 tomcat apr。1d。
* 可以给代码块增加行号。1d。

12.10 - 12.16

* tree.json URL 增加版本号; 增加 QuickSearch 功能，浏览器本地快速搜索分类、标签、标题、路径。0.7d (9 号 22 点 - 10 号 7 点)。
* QuickSearch 默认显示前几条结果，点击按钮显示全部。0.3d。
* 优化 QuickSearch 外观; 添加快捷键 g, G, u, d; 改进页面布局; 添加 help 页面。1d。
* 句子完全匹配支持通配符。1d。
* 链接 hover 显示下划线; 重构搜索，添加 AbstractPageHitMatcher。1d。
* 整理 quick_search.js; 改进 QuickSearch 样式; 改进 QuickSearch 分类和标签选择。1d。
* sticky sidebar; sticky toc。1d。
* 第一次显示 tree 时，当前页面对应的节点自动滚动到屏幕中间; 更新 readme。1d。

12.17 - 12.24

* 完成 readme.md; 分类标签选择栏自动与搜索框内容同步。1d。
* 优化 Heading 字体样式; 添加快捷键 s 和 t。0.5d。
* 改进错误页; 改进文件下载。0.5d。
* 改进 StaticService、Controller。1d。
* 可切换是否固定 Sidebar、Table of Contents、Header。1d。
* QuickSearch 在本次搜索中记住展开状态; 显示结果总数; QuickSearch 面板添加阴影。1d。
* 移除 actuator; 启动脚本设置 JVM 参数; password hash 改为 sha512; AES 从 128 改为 256。1d。
* 手机横屏也启用左中右布局; 改进 QuickSearch  UI; 加快应用启动速度。1d。

12.25

* Release v1.0.0
