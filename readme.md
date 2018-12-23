# Autumn

将 Markdown 文件目录映射为网站，有简单的访问控制，有好用的搜索功能。

## 环境

Build 环境

* Maven 3.5
* Java 8+

运行环境

* Java 8+
* Redis 4

## 运行

IDE 运行 main class

```text
xyz.liuw.autumn.Application
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

启动后，可以用浏览器访问 http://localhost:7000

## 配置

只有一个配置文件 `src/main/resources/application.properties`，默认配置可以直接运行。

正常使用的话，需要修改 `autumn.data-dir`，`autumn.users` 和 `autumn.aes.key`。

生成用户密码:

```bash
mvn -q -DskipTests=false -Dtest=xyz.liuw.autumn.service.UserServiceTest#generateUser test
```

生成 AES key:

```bash
mvn -q -DskipTests=false -Dtest=xyz.liuw.autumn.AesTest#generateKey test
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
category: atMostOne
tags: one two three more
published: true
---

# Title
...
```

如果不符合以上格式，系统自动设置默认值。`created` 和 `modified` 默认值是文件修改时间，`published` 默认值是 `false`。

## 访问控制

已登录用户可以看到全部页面，未登录只能看到 published 设为 true 的页面。

## 搜索

### 全文搜索

在搜索框输入文字，按回车，会提交到服务端进行搜索。搜索范围包括页面路径、标题、分类、标签、全文。

* 单词完全匹配。例如 `word1 word2` 匹配包含 `word1` 并且包含 `word2` 的页面。
* 句子完全匹配。例如 `"word1 word2"` 匹配包含 `word1 word2` 的页面。
* 句子完全匹配支持通配符。例如 `"word1 * word2"` 匹配包含 `word1 这里可以有 0 个或多个任意字符 word2` 的页面。
* 排除特定字词。例如 `-word1`，匹配不包含 `word1` 的页面。
* category。例如 `c:cat1` 匹配标记为分类 `cat1` 的页面。
* tag。例如 `t:tag1` 匹配标记为标签 `tag1` 的页面。
* OR。例如 `word1 OR word2` 匹配包含 `word1` 或 `word2` 的页面。

在一次搜索中可以任意组合使用以上功能，例如：`t:tag1 word1 -word2` 匹配标记为 `tag1` 并且包含 `word1` 并且不包含 `word2` 的页面。

### 快速搜索

在搜索框输入文字，下方立刻显示快速搜索结果。搜索范围包括页面路径、标题、分类、标签。

* 单词完全匹配。例如 `word1 word2` 匹配包含 `word1` 并且包含 `word2` 的页面。
* 排除特定字词。例如 `-word1`，匹配不包含 `word1` 的页面。
* category。例如 `c:cat1` 匹配标记为分类 `cat1` 的页面。
* tag。例如 `t:tag1` 匹配标记为标签 `tag1` 的页面。

在一次搜索中可以任意组合使用以上功能，例如：`t:tag1 word1 -word2` 匹配标记为 `tag1` 并且包含 `word1` 并且不包含 `word2` 的页面。
