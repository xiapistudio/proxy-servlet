# proxy-servlet
本代码是针对 [Smiley's HTTP Proxy Servlet](https://github.com/mitre/HTTP-Proxy-Servlet) 的进一步改造和尝试。

使用：

具体参考《webapp》module 中的应用。

测试：

* ProxyServlet：只能提供指定 URL 的一次跳转。
* UriTemplateProxyServlet：只能提供指定 URL 模板（通过 Query 实现）的一次跳转。
* UriTemplateProxyServlet2：支持指定 URL 模板（通过 Path 实现）的多次跳转（通过 Redirect 保留目标信息），类似 www.baidu.com 这样的页面再次搜索有问题。
* UriTemplateProxyServlet3：支持指定 URL 模板（通过 Path 实现）的多次跳转（通过 Cookie 保留目标信息），支持 www.baidu.com 这样的页面再次搜索，但是多 Tab 
同时打开跳转的目标地址，会干扰浏览器中存储的 Cookie 信息。
* UriTemplateProxyServlet4：支持指定 URL 模板（通过 Query 实现）的多次跳转（通过 Redirect 保留目标信息），缺点同 UriTemplateProxyServlet 和 
UriTemplateProxyServlet2 一致。

结论：

在特定的跳转场景下，UriTemplateProxyServlet2 效果会更加，完美跳转则 UriTemplateProxyServlet3 胜出。