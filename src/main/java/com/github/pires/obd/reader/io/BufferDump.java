package com.github.pires.obd.reader.io;

import com.github.pires.obd.commands.protocol.ObdProtocolCommand;

/**
 * Created by canbaran on 12/6/17.
 */

public class BufferDump extends ObdProtocolCommand {
    /**
     * <p>Constructor for BufferDump.</p>
     */
    public BufferDump() {
        super("AT BD");
    }

    /**
     * <p>Constructor for EchoOffCommand.</p>
     *
     * @param other a {@link com.github.pires.obd.commands.protocol.EchoOffCommand} object.
     */
    public BufferDump(BufferDump other) {
        super(other);
    }

    /** {@inheritDoc} */
    @Override
    public String getFormattedResult() {
        return getResult();
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "Buffer Dump";
    }

}
