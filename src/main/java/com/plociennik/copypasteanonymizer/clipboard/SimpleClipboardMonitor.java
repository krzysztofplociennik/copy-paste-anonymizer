package com.plociennik.copypasteanonymizer.clipboard;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SimpleClipboardMonitor {

    private static final Logger LOG = Logger.getLogger(SimpleClipboardMonitor.class.getName());

    private static final long DEBOUNCE_MS = 50;
    private static final long DEBOUNCE_NS = DEBOUNCE_MS * 1_000_000L;
    private static final int MAX_ERRORS = 5;
    private static final long ERROR_PAUSE_MS = 3_000;
    private static final long UNSUPPORTED_FLAVOR_RETRY_MS = 50;
    private static final long UNEXPECTED_ERROR_RETRY_MS = 500;
    private static final long JOIN_TIMEOUT_MS = 1_000;
    private static final long PROCESSING_SLEEP_MS = 20;
    private static final long FAST_POLL_MS = 30;
    private static final long MEDIUM_POLL_MS = 100;
    private static final long SLOW_POLL_MS = 200;
    private static final long LOCK_RETRY_BASE_MS = 150;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean processing = new AtomicBoolean(false);

    private final Consumer<String> onClipboardChange;

    private volatile String lastContent = "";
    private volatile long lastChangeTimeNs = 0;

    private Thread monitorThread;

    public SimpleClipboardMonitor(Consumer<String> onClipboardChange) {
        this.onClipboardChange = Objects.requireNonNull(onClipboardChange);
    }

    public void start() {
        boolean compareSuccessful = running.compareAndSet(false, true);
        if (!compareSuccessful) {
            return;
        }
        monitorThread = new Thread(this::runMonitorLoop, "ClipboardMonitor");
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    public void stop() {
        boolean compareSuccessful = running.compareAndSet(true, false);
        if (!compareSuccessful) {
            return;
        }
        Thread thread = monitorThread;
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join(JOIN_TIMEOUT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        monitorThread = null;
    }

    public void setProcessing(boolean processing) {
        this.processing.set(processing);
    }

    private void runMonitorLoop() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        int consecutiveErrors = 0;

        LOG.info("Clipboard monitor started");

        while (running.get()) {
            try {
                if (processing.get()) {
                    if (!interruptibleSleep(PROCESSING_SLEEP_MS)) {
                        break;
                    }
                    continue;
                }
                pollClipboardOnce(clipboard);
                consecutiveErrors = 0;

                long elapsedMs = (System.nanoTime() - lastChangeTimeNs) / 1_000_000L;
                if (!interruptibleSleep(computePollInterval(elapsedMs))) {
                    break;
                }
            } catch (UnsupportedFlavorException | IOException e) {
                if (!interruptibleSleep(UNSUPPORTED_FLAVOR_RETRY_MS)) {
                    break;
                }
            } catch (IllegalStateException e) {
                consecutiveErrors++;
                int finalConsecutiveErrors = consecutiveErrors;
                LOG.fine(() -> "Clipboard temporarily locked (attempt " + finalConsecutiveErrors + ")");

                if (!handleClipboardLocked(consecutiveErrors)) {
                    break;
                }

                if (consecutiveErrors >= MAX_ERRORS) {
                    consecutiveErrors = 0;
                }
            } catch (Exception e) {
                consecutiveErrors++;
                LOG.log(Level.WARNING, "Unexpected clipboard error", e);

                if (consecutiveErrors >= MAX_ERRORS) {
                    LOG.severe("Too many unexpected errors. Stopping monitor.");
                    break;
                }
                if (!interruptibleSleep(UNEXPECTED_ERROR_RETRY_MS)) {
                    break;
                }
            }
        }
        running.set(false);
        LOG.info("Clipboard monitor stopped");
    }

    private void pollClipboardOnce(Clipboard clipboard) throws UnsupportedFlavorException, IOException {
        Transferable contents = clipboard.getContents(null);
        if (contents == null ||
                !contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            return;
        }

        String content = (String) contents.getTransferData(DataFlavor.stringFlavor);
        if (content.equals(lastContent)) {
            return;
        }

        long now = System.nanoTime();
        if (now - lastChangeTimeNs <= DEBOUNCE_NS) {
            return;
        }

        lastContent = content;
        lastChangeTimeNs = now;
        LOG.fine(() -> "Clipboard changed: " + truncate(content));
        onClipboardChange.accept(content);
    }

    private long computePollInterval(long timeSinceLastChangeMs) {
        if (timeSinceLastChangeMs < 5_000) {
            return FAST_POLL_MS;
        }
        if (timeSinceLastChangeMs < 30_000) {
            return MEDIUM_POLL_MS;
        }
        return SLOW_POLL_MS;
    }

    private boolean handleClipboardLocked(int consecutiveErrors) {
        if (consecutiveErrors >= MAX_ERRORS) {
            LOG.warning("Clipboard repeatedly locked. Pausing.");
            return interruptibleSleep(ERROR_PAUSE_MS);
        }
        return interruptibleSleep(LOCK_RETRY_BASE_MS * consecutiveErrors);
    }

    private boolean interruptibleSleep(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static String truncate(String content) {
        final int maxLen = 50;
        return content.length() <= maxLen
                ? content
                : content.substring(0, maxLen) + "...";
    }
}