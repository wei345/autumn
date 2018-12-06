# Autumn

将本地 Markdown 文件目录映射为网页，自己可以看到全部内容，未登录用户只能看到指定内容，还有方便的搜索功能。

## 功能

页面

* 主页面，左侧有树形菜单，目录节点在前面，按照字母顺序排序，已选中当前页面。右侧是页面内容。
* 页面里的图片正常显示。例如 /git/git
* 访问不存在的页面，显示 404 模版页面，响应状态码 404。
* TOC。服务端生成，虽然 JS 也可以生成 TOC，不过我们有缓存，服务端生成不会增加多少开销，TOC 也不会占多大流量，好处是即使客户端禁用 JS，TOC 也是可用的。
* 首页显示最近修改列表
* 在路径后面加 .md 可以查看 Markdown 原文

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
* TODO 开发时 Sprig boot reload 时访问页面会看到 tomcat 500

搜索

  * TODO 页面快速搜索页面名，category, tags
  * 单词完全匹配。例如 word1 word2，匹配既包含 word1 又包含 word2 的页面
  * 句子完全匹配。例如 "word1 word2"，匹配包含 word1 word2 的页面
  * TODO 句子完全匹配支持通配符
  * 组合搜索。例如 word1 OR word2，匹配包含 word1 或 word2 的页面
  * 特定 tag 或 category。例如 tag:tag1 word1，匹配包含 tag1 和 word1 的页面
  * TODO 特定路径。例如 path:/java word1，匹配 数据目录/java 目录下包含 word1 的页面
  * 排除特定字词。例如 -word1，匹配不包含 word1 的页面
  * 排序。路径或标题匹配的在前面，然后按匹配次数排序
  * 高亮 hits
  * 缓存 Page 级结果
  * 限制每个 IP 搜索频率
  * TODO 搜索结果分页？

## 设计

### 数据

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

TODO cache 所有 category 和 tags
TODO 触发 reload 数据的接口，生产环境不需要定时频繁 reload

### Markdown 解析器

* [commonmark-java](https://github.com/atlassian/commonmark-java) 速度较快，大约是 flexmark-java 的 1.5 倍。
* [flexmark-java](https://github.com/vsch/flexmark-java) is a fork of commonmark-java project。插件丰富，速度也够快。

其他解析器速度太慢。

一开始选了 commonmark-java，但是生成 TOC 有点麻烦。

commonmark-java 只是 core 速度比较好，commonmark-java 扩展可能不怎么样，例如 commonmark-ext-heading-anchor 性能上欠考虑。

### 权限

目前通过页面里的属性 published 指定，只分私有和公开。

若要支持更多用户，不同的用户有不同的可见范围，可以在页面增加 allow_role 属性，值为 1 个或多个角色，还可增加一个权限配置文件指定角色与目录或文件的对应关系，页面 allow_role 优先级高于配置文件。当然，要改代码啦。

### 搜索

* input parser
  * 不分词，用户自己会加空格
* 组合逻辑
* 查找匹配
  * 要搜索全文内容，包括代码块，因为我们要找的内容有可能在代码块里
* 缓存中间结果和最终结果
* 排序
* 展示结果
* TODO 搜索结果缓存
* TODO 优化：记录搜索词-点击的页面-次数

Media 没多少，不搜。

比 grep 和 DokuWiki 搜索更好用。

#### hits 高亮问题

搜 markdown，代码块外的内容直接加标签。代码块内的不能直接加标签，因为标签会被原样输出，可以插入自定义标记，markdown 转为 html 后再替换自定义标记为高亮标签，因为没有高亮代码块，所以替换应该不会破坏 html 标签。
用于预览

搜 html? html 代码块没有高亮，标签不多。增加 TOC 后，`id` `href` 里会出现高亮词，图片路径也可能含高亮词
  如果代码块内容是 xml，里面可能有很多转义。代码块之外也可能有一些转义。
  转义问题可以解决，搜索前将 searchStr 进行 html 转义。
用于高亮

`<em>` 加到了 `<h2>` id 里。

用 Jsoup 高亮 HTML 里的 searchStr 成本较高。高亮 38k 长文里的几个位置用时 16 ms，使用我的 indexOfIgnoreCase 用时 6 ms。

### 缓存

还有优化空间。

显示一个页面的内容

page markdown > html > apply template > browser

搜索

page markdown > hits > sort > highlight > highlightString > apply template > browser

点击搜索结果

page markdown > html > highlightString hits > highlight > apply template > browser

### Redis

* 限流使用 script

### Production Ready

* 添加 ctx。nginx 映射到子路径时，不需要调整 Autumn 输出的 url 和跳转路径。

### View

* Controller 设置 model 时确保 HTML 安全，FreeMarker 不用转义，只是原样输出。

### UI

初始 sidebar 和 toc 不显示，当页面加载完成后由 JS 控制是否显示。
因为先显示然后又不显示，不如先不显示然后显示看起来好。

如果客户端禁用了 JS，sidebar 和 toc 将不可用。

* 如果未启用 JS，需要 JS 控制的元素将不显示。
* 使用标准 API。不考虑 IE。

减少请求数，并防止浏览器缓存导致 js, css 不更新。

* 合并 css，并在路径中添加版本号
* 合并 js，并在路径中添加版本号

### Java 10

TODO 现在无法在 Java 9 以上运行，报错找不到类 javax/activation/MimetypesFileTypeMap

java --add-modules java.se.ee -jar myapp.jar

--add-modules java.activation

--add-modules ALL-MODULE-PATH



