package com.plociennik.copypasteanonymizer.clipboard;

import javafx.application.Platform;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class SimpleClipboardMonitor {

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    private final AtomicLong lastChangeTime = new AtomicLong(0);
    private Consumer<String> onClipboardChange;
    private String lastContent = "";
    private Thread monitorThread;

    public SimpleClipboardMonitor(Consumer<String> onClipboardChange) {
        this.onClipboardChange = onClipboardChange;
    }

    public void start() {
        if (isRunning.get()) {
            return;
        }

        isRunning.set(true);
        startMonitor();
    }

    public void stop() {
        isRunning.set(false);
        if (monitorThread != null) {
            monitorThread.interrupt();
        }
    }

    private void startMonitor() {
        monitorThread = new Thread(() -> {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            int consecutiveErrors = 0;
            final int maxErrors = 5;

            System.out.println("Clipboard monitor started");

            while (isRunning.get()) {
                try {
                    if (isProcessing.get()) {
                        Thread.sleep(20);
                        continue;
                    }

                    Transferable contents = clipboard.getContents(null);

                    if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                        String content = (String) contents.getTransferData(DataFlavor.stringFlavor);

                        if (!content.equals(lastContent)) {

                            long currentTime = System.currentTimeMillis();
                            if (currentTime - lastChangeTime.get() > 50) {
                                lastContent = content;
                                lastChangeTime.set(currentTime);

                                System.out.println("Clipboard changed: " +
                                        content.substring(0, Math.min(50, content.length())) +
                                        (content.length() > 50 ? "..." : ""));

                                handleClipboardChange(content);
                            }
                        }
                    }

                    consecutiveErrors = 0;

                    long timeSinceLastChange = System.currentTimeMillis() - lastChangeTime.get();
                    int sleepTime;
                    if (timeSinceLastChange < 5000) {
                        sleepTime = 30;
                    } else if (timeSinceLastChange < 30000) {
                        sleepTime = 100;
                    } else {
                        sleepTime = 200;
                    }

                    Thread.sleep(sleepTime);

                } catch (UnsupportedFlavorException | IOException e) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ie) {
                        break;
                    }
                } catch (IllegalStateException e) {
                    consecutiveErrors++;
                    System.out.println("Clipboard locked (attempt " + consecutiveErrors + ")");

                    if (consecutiveErrors >= maxErrors) {
                        System.err.println("Too many clipboard access errors, pausing monitor for 3 seconds");
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException ie) {
                            break;
                        }
                        consecutiveErrors = 0;
                    } else {
                        try {
                            Thread.sleep(150 * consecutiveErrors);
                        } catch (InterruptedException ie) {
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    System.out.println("Clipboard monitor interrupted");
                    break;
                } catch (Exception e) {
                    consecutiveErrors++;
                    System.err.println("Unexpected clipboard error: " + e.getMessage());

                    if (consecutiveErrors >= maxErrors) {
                        System.err.println("Too many unexpected errors, stopping monitor");
                        break;
                    }

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ie) {
                        break;
                    }
                }
            }

            System.out.println("Clipboard monitor stopped");
        });

        monitorThread.setName("ClipboardMonitor");
        monitorThread.setDaemon(true);
        monitorThread.setPriority(Thread.NORM_PRIORITY + 1);
        monitorThread.start();
    }

    private void handleClipboardChange(String content) {
        Platform.runLater(() -> {
            if (onClipboardChange != null) {
                onClipboardChange.accept(content);
            }
        });
    }

    public void setProcessing(boolean processing) {
        isProcessing.set(processing);
        if (processing) {
            System.out.println("Clipboard processing started");
        } else {
            System.out.println("Clipboard processing finished");
        }
    }
}