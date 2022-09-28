package com.example.ping;

import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Ping {
    private static final String TAG = "Ping";
    private static Executor mExecutor = Executors.newSingleThreadExecutor();
    private static int sleepTime = 200;

    public interface PingCallback {
        void onPingResult(List<PingResult> pingResult);
    }

    public static void ping(final String ipList, final PingCallback callback) {
        final List<PingResult> results = new ArrayList<>();
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                PingResult pingResult = getPingResult(ipList);
                results.add(pingResult);
                if (callback != null)
                    callback.onPingResult(results);
            }
        });
    }

    private static PingResult getPingResult(String host) {
        return pingCmd(host, 10, 200);
    }

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

    private static String getIp(String host) throws UnknownHostException {
        InetAddress i = InetAddress.getByName(host);
        if (i != null) {
            return i.getHostAddress();
        } else {
            return null;
        }
    }

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

    public static boolean isOpen = true;

    public static void setOpen(boolean open) {
        isOpen = open;
    }


    public static String getIP(String url) {
        int i = url.indexOf(":", 7);
        return url.substring(0, i);
    }

    public static int getPosition(String str, int ciShu) {
        int number = 0;
        char arr[] = str.toCharArray();
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == ':') {
                number++;
            }
            if (number == ciShu) {
                return i + 1;
            }
        }
        return 1;

    }

    public static void startSniffer(String urls) {
        String str = getIP(urls);
        String result = str.substring(8, (getPosition(str, 2) == 1) ? str.length() : (getPosition(str, 2) - 1));
        Log.e("ping", "startSniffer: " + result);
        Log.e("ping", "startSniffer---str: " + str);
        if (isOpen) {
            Ping.ping(result, pingResult -> {
                String result1 = pingResult.toString();
                String s = result1.substring(1, result1.length() - 1);
                Log.d("ping", "onSuccess: " + s);
                //调用PingQuality.getQualityInt(s)获取具体网络质量
                //TODO 结果抛出的回调需要实现
                Log.d("ping", "具体网络质量: " + PingQuality.getQualityInt(s));
                startSniffer(urls);
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public static class PingQuality {

        public static int getQualityInt(String reason) {
            switch (reason) {
                case "none":
                    return -1;
                case "QUALITY_UNKNOWN":
                    return 0;
                case "QUALITY_EXCELLENT":
                    return 1;
                case "QUALITY_GOOD":
                    return 2;
                case "QUALITY_POOR":
                    return 3;
                case "QUALITY_BAD":
                    return 4;
                case "QUALITY_VBAD":
                    return 5;
                case "QUALITY_DOWN":
                    return 6;
                case "QUALITY_DETECTING":
                    return 8;
            }
            return 0;
        }

        public static String getDescription(int reason) {
            switch (reason) {
                case -1:
                    return "none";
                case 0:
                    return "QUALITY_UNKNOWN";
                case 1:
                    return "QUALITY_EXCELLENT";
                case 2:
                    return "QUALITY_GOOD";
                case 3:
                    return "QUALITY_POOR";
                case 4:
                    return "QUALITY_BAD";
                case 5:
                    return "QUALITY_VBAD";
                case 6:
                    return "QUALITY_DOWN";
                case 8:
                    return "QUALITY_DETECTING";
            }
            return "unKnow";
        }
    }
}
