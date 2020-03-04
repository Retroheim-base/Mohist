package red.mohist;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import red.mohist.console.log4j.MohistLog;
import red.mohist.util.i18n.Message;

public class MohistThreadCost {
    static ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    {
        threadMXBean.setThreadCpuTimeEnabled(true);
    }

    public static void dumpThreadCpuTime() {
        List<MohistThreadCost.ThreadCpuTime> list = new ArrayList<>();
        long[] ids = threadMXBean.getAllThreadIds();
        for (long id : ids) {
            MohistThreadCost.ThreadCpuTime item = new MohistThreadCost.ThreadCpuTime();
            item.cpuTime = threadMXBean.getThreadCpuTime(id) / 1000000;
            item.userTime = threadMXBean.getThreadUserTime(id) / 1000000;
            item.name = threadMXBean.getThreadInfo(id).getThreadName();
            item.id = id;
            list.add(item);
        }
        list.sort(Comparator.comparingLong(i -> i.id));
        MohistLog.LOGGER.info(Message.getString("mohist.dump.1"));
        for (MohistThreadCost.ThreadCpuTime threadCpuTime : list) {
            MohistLog.LOGGER.info(String.format("%s %s %s %s", threadCpuTime.id, threadCpuTime.name, threadCpuTime.cpuTime, threadCpuTime.userTime));
        }
    }

    public static class ThreadCpuTime {
        private long id;
        private long cpuTime;
        private long userTime;
        private String name;
    }
}
