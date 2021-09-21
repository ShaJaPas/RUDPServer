package main.tools.system;

import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;
import java.text.DecimalFormat;

public class SystemInfo {
    private static final OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

    private static final DecimalFormat format = new DecimalFormat("##.##");

    public static String getCpuUsage(){
        return format.format(osBean.getSystemCpuLoad());
    }

    public static String getMemUsage(){
        return format.format((double) (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / osBean.getTotalPhysicalMemorySize() * 100 );
    }

    public static String getMemUsedMB(){
        return format.format((double) (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024);
    }
}
