Ping.java使用说明
1.复制Ping.java到项目中，调用Ping.startSniffer("https://www.baidu.com:");
urls规则可以看Ping里面的getIp方法，从第7位开始到下一个":"截至，获取具体域名来ping

2.在startSniffer中需要自己实现回调等方式把PingQuality.getQualityInt(s)获取的网络质量结果抛出
