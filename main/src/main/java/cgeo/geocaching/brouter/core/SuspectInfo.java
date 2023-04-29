package cgeo.geocaching.brouter.core;

import java.util.Map;

public class SuspectInfo {
  public static final int TRIGGER_DEAD_END = 1;
  public static final int TRIGGER_DEAD_START = 2;
  public static final int TRIGGER_NODE_BLOCK = 4;
  public static final int TRIGGER_BAD_ACCESS = 8;
  public static final int TRIGGER_UNK_ACCESS = 16;
  public static final int TRIGGER_SHARP_EXIT = 32;
  public static final int TRIGGER_SHARP_ENTRY = 64;
  public static final int TRIGGER_SHARP_LINK = 128;
  public static final int TRIGGER_BAD_TR = 256;

  public int prio;
  public int triggers;

  public static void addSuspect(Map<Long, SuspectInfo> map, long id, int prio, int trigger) {
    final Long iD = Long.valueOf(id);
    SuspectInfo info = map.get(iD);
    if (info == null) {
      info = new SuspectInfo();
      map.put(iD, info);
    }
    info.prio = Math.max(info.prio, prio);
    info.triggers |= trigger;
  }

  public static String getTriggerText(int triggers) {
    final StringBuilder sb = new StringBuilder();
    addText(sb, "dead-end", triggers, TRIGGER_DEAD_END);
    addText(sb, "dead-start", triggers, TRIGGER_DEAD_START);
    addText(sb, "node-block", triggers, TRIGGER_NODE_BLOCK);
    addText(sb, "bad-access", triggers, TRIGGER_BAD_ACCESS);
    addText(sb, "unkown-access", triggers, TRIGGER_UNK_ACCESS);
    addText(sb, "sharp-exit", triggers, TRIGGER_SHARP_EXIT);
    addText(sb, "sharp-entry", triggers, TRIGGER_SHARP_ENTRY);
    addText(sb, "sharp-link", triggers, TRIGGER_SHARP_LINK);
    addText(sb, "bad-tr", triggers, TRIGGER_BAD_TR);
    return sb.toString();
  }

  private static void addText(StringBuilder sb, String text, int mask, int bit) {
    if ((bit & mask) == 0) {
        return;
    }
    if (sb.length() > 0) {
        sb.append(",");
    }
    sb.append(text);
  }

}
