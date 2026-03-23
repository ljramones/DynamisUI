package org.dynamisengine.ui.debug.export;

import org.dynamisengine.ui.debug.builder.DebugViewSnapshot;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * Streams {@link DebugViewSnapshot} over TCP using length-prefixed JSON.
 *
 * <p>Protocol: {@code [int32 big-endian length][UTF-8 JSON payload]} per frame.
 *
 * <p>Runs a server socket on the specified port. Accepts one client at a time.
 * If no client is connected, snapshots are silently dropped (no buffering).
 */
public final class TcpExporter implements DebugSnapshotExporter {

    private static final Logger LOG = Logger.getLogger(TcpExporter.class.getName());

    private final int port;
    private ServerSocket serverSocket;
    private volatile Socket clientSocket;
    private volatile OutputStream clientOut;
    private Thread acceptThread;
    private int snapshotCount;

    public TcpExporter(int port) {
        this.port = port;
    }

    /** Start listening for connections. Non-blocking — accepts in background. */
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        serverSocket.setReuseAddress(true);
        LOG.info("Debug telemetry server listening on port " + port);

        acceptThread = Thread.ofVirtual().name("debug-tcp-accept").start(() -> {
            while (!Thread.currentThread().isInterrupted() && serverSocket != null && !serverSocket.isClosed()) {
                try {
                    Socket socket = serverSocket.accept();
                    LOG.info("Debug telemetry client connected: " + socket.getRemoteSocketAddress());
                    // Replace existing client
                    closeClient();
                    clientSocket = socket;
                    clientOut = socket.getOutputStream();
                } catch (IOException e) {
                    if (!serverSocket.isClosed()) {
                        LOG.warning("Accept failed: " + e.getMessage());
                    }
                }
            }
        });
    }

    @Override
    public synchronized void export(DebugViewSnapshot snapshot) {
        var out = clientOut;
        if (out == null) return;

        try {
            byte[] json = DebugSnapshotJson.toJson(snapshot).getBytes(StandardCharsets.UTF_8);
            byte[] header = ByteBuffer.allocate(4).putInt(json.length).array();
            out.write(header);
            out.write(json);
            snapshotCount++;
        } catch (IOException e) {
            LOG.fine("Client disconnected: " + e.getMessage());
            closeClient();
        }
    }

    @Override
    public synchronized void flush() {
        var out = clientOut;
        if (out == null) return;
        try { out.flush(); } catch (IOException e) { closeClient(); }
    }

    @Override
    public void close() {
        closeClient();
        if (acceptThread != null) acceptThread.interrupt();
        if (serverSocket != null) {
            try { serverSocket.close(); } catch (IOException ignored) {}
        }
        LOG.info("Debug telemetry server closed. " + snapshotCount + " snapshots sent.");
    }

    private void closeClient() {
        clientOut = null;
        var socket = clientSocket;
        clientSocket = null;
        if (socket != null) {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    public int snapshotCount() { return snapshotCount; }
    public boolean hasClient() { return clientOut != null; }
}
