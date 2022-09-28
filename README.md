Ping.java使用说明

通过Ping.startSniffer方法开始调用,urls规则可以看Ping里面的getIp方法，从第7位开始到下一个":"截至，获取具体域名来ping
Ping.startSniffer("https://www.baidu.com:");

在startSniffer中需要自己实现回调等方式把PingQuality.getQualityInt(s)获取的网络质量结果抛出
