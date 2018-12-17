# Autumn

将 Markdown 文件目录映射为网站，有简单的访问控制，有好用的搜索功能。

## 环境

Build 环境

* Maven 3.5
* Java 8

运行环境

* Java 8
* Redis 4

## 运行

IDE 运行 main class

```text
xyz.liuw,autumn.Application
```

或 命令行从源码运行

```bash
mvn clean compile exec:java
```

或 命令行打包运行

```bash
mvn clean package
java -jar target/autumn.jar
```

## 数据目录

数据目录里可以有 .md 文件和其他任意格式文件，可以有子目录。

.md 文件 `${autumn.data-dir}/a/b/c.md` 会被映射为网页 `http://localhost:7000/a/b/c`。

图片 `${autumn.data-dir}/a/b/c.png` 会被映射为 `http://localhost:7000/a/b/c.png`。

其他格式文件映射方式同图片，系统会自动设置正确的 Content-Type。

.md 文件格式约定：

```markdown
---
created: 2018-02-20 12:59:50
modified: 2018-12-03 20:18:42
category: onlyOne
tags: one two three more
published: true
---

# Title
...
```

如果不符合以上格式，系统自动设置默认值。`created` 和 `modified` 默认值是文件修改时间，`published` 默认值是 `false`。

## 功能

页面

* 主页面，左侧有树形菜单，目录节点在前面，按照字母顺序排序，已选中当前页面。右侧是页面内容。
* 第一次显示树形菜单时，当前页面对应的节点自动滚动到屏幕中间（竖直方向）。
* 页面里的图片正常显示。例如 /git/git
* 访问不存在的页面，显示 404 模版页面，响应状态码 404。
* TOC。服务端生成，虽然 JS 也可以生成 TOC，不过我们有缓存，服务端生成不会增加多少开销，TOC 也不会占多大流量，好处是即使客户端禁用 JS，TOC 也是可用的。
* 首页显示最近修改列表。
* 在路径后面加 .md 可以查看 Markdown 原文。

登录

* 主页面没有登录按钮，打开 /login，显示登录表单，登录成功跳转到首页。
* 如果一个 IP 一天内登录失败 10 次，那么该 IP 再次登录会被拒绝，提示"稍后再试"，持续一天。
* 登录成功后，浏览器会记住登录状态一年。
* 如果修改了密码，所有设备都需要重新登录。

登出

* /logout 会跳转到首页，cookie `ME` 会被删除，客户端处于未登录状态。

权限

* 已登录用户可以看到全部页面，未登录只能看到 published 设为 true 的页面。
* 未登录状态，树形菜单里不会显示无权限页面。
* 未登录状态访问无权限页面，会跳转到登录页面，登录成功后回到之前的页面。
* 已登录状态，左侧树形菜单显示所有页面。

搜索

* 单词完全匹配。例如 `word1 word2` 匹配包含 `word1` 并且包含 `word2` 的页面。
* 句子完全匹配。例如 `"word1 word2"` 匹配包含 `word1 word2` 的页面。
* 句子完全匹配支持通配符。例如 `"word1 * word2"` 匹配包含 `word1 这里可以有 0 个或多个任意字符 word2` 的页面。
* 排除特定字词。例如 `-word1`，匹配不包含 `word1` 的页面。
* category。例如 `c:cat1` 匹配标记为分类 `cat1` 的页面。
* tag。例如 `t:tag1` 匹配标记为标签 `tag1` 的页面。
* OR。例如 `word1 OR word2` 匹配包含 `word1` 或 `word2` 的页面。
* 排序。路径或标题匹配的在前面，然后按 hit 数排序。
* 高亮 hits。
* 缓存 PageHit。
* 限制每个 IP 搜索频率。
* 前端页面快速搜索路径、标题、category、tags。

## 设计

### 数据

* .md 文件。第一次访问时生成 HTML，md 或 template lastModified 变化或应用重启时清除缓存。
* 其他文件。不大于 1MB，第一次访问时缓存，文件修改时间发生变化时清除缓存。
* css 缓存。应用启动时生成，文件 lastModified 变化时重新生成。
* js 缓存。应用启动时生成，文件 lastModified 变化时重新生成。
* 提供 reload 接口，生产环境不需要定时 reload。

### Markdown 解析器

主要有两个解析器

* [commonmark-java](https://github.com/atlassian/commonmark-java) 速度较快，大约是 flexmark-java 的 1.5 倍。core 速度比较好，扩展可能不怎么样，例如 commonmark-ext-heading-anchor 性能上欠考虑。
* [flexmark-java](https://github.com/vsch/flexmark-java) is a fork of commonmark-java project。插件丰富，速度也够快。

两个都试了，commonmark-java 生成 TOC 有点麻烦，最终选择了 flexmark-java。

### 权限

若要支持更多用户，不同的用户有不同的可见范围，可以在页面增加 allow_role 属性，
值为 1 个或多个角色，还可增加一个权限配置文件指定角色与目录或文件的对应关系，
页面 allow_role 优先级高于配置文件。...我没有这样的需求，没有做这些功能。

### 搜索

* input parser
  * 不分词，用户自己会加空格
* 组合逻辑
* 查找匹配
  * 要搜索全文内容，包括代码块，因为我们要找的内容有可能在代码块里
* 缓存中间结果和最终结果
* 排序
* 展示结果
* TODO 搜索结果分页？
* TODO 搜索结果缓存
* TODO 优化：记录搜索词-点击的页面-次数

Media 没多少，不搜。

#### hits 高亮问题

搜索结果显示的 hits 预览，是选取的 .md 原文部分内容，可能会带有一点 Markdown 标记。其实也可以从 html 获取文本内容，更干净。

可以准确地给 html hits 加标签。因为在 html 文件中，只有 html 标签会出现 `<` 和 `>`，所以可以准确地跳过 html 标签。搜索前将 searchStr 进行 html 转义。

利用 Jsoup 查找 HTML 里的 searchStr 成本较高，高亮 38k 长文里的几个位置用时 16 ms，我的 HtmlUtil 用时 6 ms。

### 缓存

* 页面缓存。如果页面内容或模版没有变化，那么刷新页面响应 304，即使重启应用，刷新页面也响应 304。
* 开发过程中模版文件变化后，3 秒内页面更新。

还有优化空间。

显示一个页面的内容

page markdown > html > apply template > browser

搜索

page markdown > hits > sort > highlight > highlightString > apply template > browser

点击搜索结果

page markdown > html > highlightString hits > highlight > apply template > browser

### 异常

* 处理请求发生异常时，显示 templates/error 里和状态码对应的错误页面，用户不会看到 Tomcat 错误页面。
* TODO 开发时 Sprig boot reload 时访问页面会看到 tomcat 500

### Production Ready

* 添加 ctx。nginx 映射到子路径时，不需要调整 Autumn 输出的 url 和跳转路径。

### View

* Controller 设置 model 时确保 HTML 安全，FreeMarker 不用转义，只是原样输出。

### UI

初始 sidebar 和 toc 不显示，当页面加载完成后由 JS 控制是否显示。因为先显示然后又不显示，不如先不显示然后显示看起来好。

* 如果客户端未启用 JS，sidebar 和 toc 将不可用。
* 如果未启用 JS，需要 JS 控制的元素将不显示。
* 使用标准 API。不考虑 IE。

减少请求数，并防止浏览器缓存导致 js, css 不更新。

* 合并 css，并在路径中添加版本号。
* 合并 js，并在路径中添加版本号。

### Java 10

TODO 现在无法在 Java 9 以上运行，报错找不到类 javax/activation/MimetypesFileTypeMap

java --add-modules java.se.ee -jar myapp.jar

--add-modules java.activation

--add-modules ALL-MODULE-PATH

### 安全

TODO

* 记录用户 logout 时间和设备。验证 rememberMe 登录时间 > logout 时间，可退出所有设备。
* 记录用户登录时间和设备

如果只退出特定的设备呢？

* 记录已失效的 token？不好。已失效的数据会一直增长。如果服务器把这些数据丢了，那就变成有效的了。
* 记录有效的 token？优点是强登录验证，用户退出就一定是退出的，即使再收到之前的 rememberMe，服务端也是不认的。缺点是如果服务器把这些数据丢了，所有用户设备需要重新登录。

Someday

* 限制单个 IP 访问次数
* 限制单个 IP rememberMe 解析错误次数
* 限制单个 IP rememberMe 用户验证错误次数
* 限制单个 IP 创建 session 数量


