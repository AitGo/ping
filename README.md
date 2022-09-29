Android实时质量检测

## 传送门：[CSDN](https://blog.csdn.net/ly4900/article/details/127091931?csdn_share_tail=%7B%22type%22%3A%22blog%22%2C%22rType%22%3A%22article%22%2C%22rId%22%3A%22127091931%22%2C%22source%22%3A%22ly4900%22%7D)
# 原理

使用`ping -n -i %f -c %d %s`命令实现网络质量检测，具体参数如下：
-n count 发送 count 指定的 ECHO 数据包数。默认值为 4
-i 生存时间
-c 路由隔离仓标识符

如图ping后返回的最后一行可以看到min/avg/max/stddev，我们主要从avg来判断当前的网络质量
![在这里插入图片描述](https://img-blog.csdnimg.cn/f90ebb45f0b54831b76adb886855adbf.png#pic_center)
## 使用

1. 通过Ping.startSniffer方法开始调用,urls规则可以看Ping里面的getIp方法，从第7位开始到下一个":"截至，获取具体域名来ping

```
Ping.startSniffer("https://www.baidu.com:");
```

2. 在startSniffer中需要自己实现回调等方式把PingQuality.getQualityInt(s)获取的网络质量结果抛出



## 核心代码
使用ping命令获取返回的string，然后解析后获取到avg来判断网络质量。
```
private static PingResult pingCmd(String address, int count, int interval) {
        String ip = null;
        try {
            ip = getIp(address);
        } catch (UnknownHostException e) {
//            Log.e(TAG, "parseResult", e);
        }
        if (ip == null) {
            sleepTime = 2000;
            return new PingResult("", address, "", 0);
        } else {
            sleepTime = 200;
        }
        String cmd = String.format(Locale.US, "ping -n -i %f -c %d %s", ((double) interval / 1000), count, ip);
        Process process = null;
        StringBuilder str = new StringBuilder();
        BufferedReader reader = null;
        BufferedReader errorReader = null;
        try {
            process = Runtime.getRuntime().exec(cmd);
            reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream()));
            String line;
            errorReader = new BufferedReader(new InputStreamReader(
                    process.getErrorStream()));
            while ((line = reader.readLine()) != null) {
                str.append(line).append("\n");
            }
            while ((line = errorReader.readLine()) != null) {
                str.append(line);
            }
        } catch (IOException e) {
            Log.e(TAG, "pingCmd", e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (errorReader != null) {
                    errorReader.close();
                }
                if (process != null) {
                    process.destroy();
                }
            } catch (Exception e) {
                Log.e(TAG, "pingCmd", e);
            }
        }
        return new PingResult(str.toString(), address, ip, interval);
    }
```

```
public static class PingResult {
        public final String result;
        public final String ip;
        public final String address;
        public final int interval;
        private static final String LAST_LINE_PREFIX = "rtt min/avg/max/mdev = ";
        private static final String PACKET_WORDS = " packets transmitted";
        private static final String RECEIVED_WORDS = " received";
        private static final String LOSS_WORDS = "% packet loss";
        public int sent;
        public int packetLoss = -1;
        public int dropped;
        public float max;
        public float min;
        public float avg;
        public float stddev;
        public int count;
        public int avgNumber;
        public int lostNumber;

        PingResult(String result, String address, String ip, int interval) {
            this.result = result;
            this.ip = ip;
            this.interval = interval;
            this.address = address;
            parseResult();
        }

        static String trimNoneDigital(String s) {
            if (s == null || s.length() == 0) {
                return "";
            }
            char[] v = s.toCharArray();
            char[] v2 = new char[v.length];
            int j = 0;
            for (char aV : v) {
                if ((aV >= '0' && aV <= '9') || aV == '.') {
                    v2[j++] = aV;
                }
            }
            return new String(v2, 0, j);
        }

        private void parseRttLine(String s) {
            String s2 = s.substring(LAST_LINE_PREFIX.length(), s.length() - 3);
            String[] l = s2.split("/");
            if (l.length != 4) {
                return;
            }
            min = Float.parseFloat(trimNoneDigital(l[0]));
            avg = Float.parseFloat(trimNoneDigital(l[1]));
            max = Float.parseFloat(trimNoneDigital(l[2]));
            stddev = Float.parseFloat(trimNoneDigital(l[3]));
        }

        private void parsePacketLine(String s) {
            String[] l = s.split(",");
            if (l.length != 4) {
                return;
            }
            if (l[0].length() > PACKET_WORDS.length()) {
                String s2 = l[0].substring(0, l[0].length() - PACKET_WORDS.length());
                count = Integer.parseInt(s2);
            }
            if (l[1].length() > RECEIVED_WORDS.length()) {
                String s3 = l[1].substring(0, l[1].length() - RECEIVED_WORDS.length());
                sent = Integer.parseInt(s3.trim());
            }
            if (l[2].length() > LOSS_WORDS.length()) {
                String s4 = l[2].substring(0, l[2].length() - LOSS_WORDS.length());
                packetLoss = Integer.parseInt(s4.trim());
            }
            dropped = count - sent;
        }

        private void parseResult() {
            String[] rs = result.split("\n");
            try {
                for (String s : rs) {
                    if (s.contains(PACKET_WORDS)) {
                        parsePacketLine(s);
                    } else if (s.contains(LAST_LINE_PREFIX)) {
                        parseRttLine(s);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "parseResult", e);
            }

        }

        @Override
        public String toString() {

            if (avg == 1) {
                avgNumber = 0;
            } else if (avg > 1 && avg <= 15) {
                avgNumber = 1;
            } else if (avg > 15 && avg <= 55) {
                avgNumber = 2;
            } else if (avg > 55 && avg <= 140) {
                avgNumber = 3;
            } else if (avg > 140 && avg <= 420) {
                avgNumber = 4;
            } else if (avg > 420) {
                avgNumber = 5;
            } else {
                avgNumber = 6;
            }
            if (packetLoss == 0) {
                lostNumber = 0;
            } else if (packetLoss > 0 && packetLoss <= 2) {
                lostNumber = 1;
            } else if (packetLoss > 2 && packetLoss <= 5) {
                lostNumber = 2;
            } else if (packetLoss > 5 && packetLoss <= 8) {
                lostNumber = 3;
            } else if (packetLoss > 8 && packetLoss <= 13) {
                lostNumber = 4;
            } else if (packetLoss > 13) {
                lostNumber = 5;
            } else {
                lostNumber = 6;
            }

            Log.e("ping", "avg: " + avg + "  packetLoss: " + packetLoss);

            if (lostNumber > avgNumber) {
                return PingQuality.getDescription(lostNumber);
            } else {
                return PingQuality.getDescription(avgNumber);
            }
//            return "Result{" +
//                    "result='" + result + '\'' +
//                    ", ip='" + ip + '\'' +
//                    ", interval=" + interval +
//                    ", lastLinePrefix='" + LAST_LINE_PREFIX + '\'' +
//                    ", packetWords='" + PACKET_WORDS + '\'' +
//                    ", receivedWords='" + RECEIVED_WORDS + '\'' +
//                    ", sent=" + sent +
//                    ", dropped=" + dropped +
//                    ", max=" + max +
//                    ", min=" + min +
//                    ", avg=" + avg +
//                    ", stddev=" + stddev +
//                    ", count=" + count +
//                    ", packetLoss=" + packetLoss +
//                    '}';
        }
    }
```

编码不易欢迎打赏
![zfb](https://github.com/AitGo/ping/blob/main/zfb.png) 