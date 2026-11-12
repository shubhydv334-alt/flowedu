package com.flowedu;

import java.util.Map;

/** A workflow step implementation. Add a new connector by implementing this and annotating it @Component. */
public interface Action {

    /**
     * dryRun   : render only, send nothing (test run "preview").
     * testMode : recipients are forced to testEmail / testChat, webhooks are skipped ("send test to me").
     */
    record Options(boolean dryRun, boolean testMode, String testEmail, String testChat) {
        public static Options live() { return new Options(false, false, null, null); }
    }

    String type();

    /** cfg is already template-rendered; ctx holds the data of the current student/course unit. */
    Map<String, Object> run(Map<String, Object> cfg, Map<String, Object> ctx, Options options) throws Exception;
}
