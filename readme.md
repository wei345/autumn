# Autumn

映射 Markdown 文件目录为网站，有简单的访问控制，有好用的搜索功能。

## 环境需求

* Java 8+
  * Java 8 需要启用 256 AES key，下载 [Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files](https://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html) 安装到 ${java.home}/jre/lib/security/。Java 9 及以上默认可以使用 256 AES key，不需要额外设置。
* Maven 3.5
* Redis 4 (可选)

## 安装

```bash
git clone git@github.com:wei345/autumn.git
cd autumn
```

或下载 [master.zip](https://github.com/wei345/autumn/archive/master.zip)，解压缩，进入 autumn-master 目录。

生成密码:

```bash
mvn -q -DskipTests=false -Dtest=xyz.liuw.autumn.service.UserServiceTest#generateUser test
```

输出示例：

```text
1 Username 1B62BCEB123EB08F73BE3970394C23973FADF75CDCEE153A27FD2EC808805ED29BCC77CDCB966E4C775347D55E82753510D9E8154387BB7286D8CBAF9E68324A 75F0FF8B5CF34B050491DBB9F0BBF85F;
password: l3RfN05UhyFm4IYc
```

生成 AES key:

```bash
mvn -q -DskipTests=false -Dtest=xyz.liuw.autumn.AesTest#generateKey test
```

输出示例：

```text
2FDFCEF1DAA8E567549C52C10422BE09A81CC80B0A05BFE8CF75F223BD87DEB6
```

新建文件 src/main/resources/application-local.properties，用生成的密码和 AES key 配置 `autumn.users` 和 `autumn.aes.key`，其中 `Username` 可以随意修改，我这里把它改为 `test`。

配置示例：

```properties
autumn.users=1 test 1B62BCEB123EB08F73BE3970394C23973FADF75CDCEE153A27FD2EC808805ED29BCC77CDCB966E4C775347D55E82753510D9E8154387BB7286D8CBAF9E68324A 75F0FF8B5CF34B050491DBB9F0BBF85F;
autumn.aes.key=2FDFCEF1DAA8E567549C52C10422BE09A81CC80B0A05BFE8CF75F223BD87DEB6
```

启动：

```bash
mvn clean compile exec:java
```

或（后台运行）：

```bash
bin/start.sh
```

可以用浏览器访问首页 http://localhost:7000 。

登录页地址 http://localhost:7000/login ，可以用配置文件里设置的用户名 `test` 和生成的密码登录。（上面输出示例中密码为 `l3RfN05UhyFm4IYc`）

停止：

```bash
bin/autumn.sh stop
```

## 配置

更多配置及默认值见 src/main/resources/application.properties。

最重要的几项配置是：`autumn.data-dir`，`autumn.users` 和 `autumn.aes.key`。

## 数据目录

数据目录（`autumn.data-dir`）里可以有 .md 文件和其他任意格式文件，可以有子目录。

.md 文件会被映射为网页。例如 `${autumn.data-dir}/a/b/c.md` 会被映射为网页 `http://localhost:7000/a/b/c`。

其他文件会被视为静态文件。例如 `${autumn.data-dir}/a/b/c.png` 会被映射为 `http://localhost:7000/a/b/c.png`。

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

如果不符合以上格式，系统会自动设置默认值。`created` 和 `modified` 默认值是文件修改时间，`published` 默认值是 `false`。

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

## 生产环境部署

创建配置文件 src/main/resources/application-prod.properties（也可以将其他文件 link 到这个位置），该文件应该包含以下配置：

```properties
# /path/to/data
autumn.data-dir=<your data directory>
autumn.data.reload-interval-seconds=0
autumn.resource.reload-interval-seconds=0
# id username password salt; id ...
autumn.users=<your users>
autumn.aes.key=<your aes key>
```

其中，autumn.data.reload-interval-seconds=0 禁用周期性扫描数据目录，autumn.resource.reload-interval-seconds=0 禁用周期性扫描 resources 目录。

数据更新时，通过 HTTP 接口触发 reload，例如：

```bash
# push your data to remote git server
git push

ssh your_server 'bash -x -e -s' <<END
cd /path/to/data
git pull
curl --silent -X POST http://localhost:${server.port}${server.servlet.context-path}/manage/data
END
```

若要启用 Redis，设置：

```properties
spring.autoconfigure.exclude=
```

启动/重启：

```bash
bin/start.sh
```

停止：

```bash
bin/autumn.sh stop
```

更新 Autumn：

```bash
cd /path/to/autumn
git pull
bin/start.sh
```

## ...

### 有那么多现成的网站工具，为什么还要自己开发？

因为都不顺手。

* Jekyll 不适合我。我只想公开一部分内容，自己能够看到全部内容，还想要不依赖第三方的好用的搜索功能。
* WordPress 不适合我。我不喜欢在网页的小窗口内编辑文本，不，大窗口也不喜欢，我更喜欢用强大的文本编辑器编辑文本。
* DokuWiki 不适合我。同上。
* 其他，试过一些，也不适合我。

我曾经改造过 DokuWiki，用 git 同步数据，自动刷新索引，把文件扩展名从 .txt 改为 .md，增加 front matter 支持，Sidebar 可折叠等。DokuWiki 有很多我不需要的功能和逻辑，对于改造来说都是负担，改造成本很高，考虑到将来要支持 category、tags、blog，还有很高的改造成本。

不如自己开发，比改造一个现有的东西更可控更省时间，每一处都按照自己的喜好来做，访问速度更快，用起来更顺手。

### 为什么叫 Autumn？

因为，

1. 秋天是我最喜欢的季节，我开发 Autumn 时正是秋天。
2. 我打算长期使用和维护这个工具，所以要起一个没有时效性、久看不厌的名字。
