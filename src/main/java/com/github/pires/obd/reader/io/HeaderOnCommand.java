package com.github.pires.obd.reader.io;

import com.github.pires.obd.commands.protocol.ObdProtocolCommand;

/**
 * Created by canbaran on 11/17/17.
 */

public class HeaderOnCommand extends ObdProtocolCommand {
    /**
     * <p>Constructor for CANStatus.</p>
     */
    public HeaderOnCommand() {
        super("AT H1");

    }

    /**
     * <p>Constructor for EchoOffCommand.</p>
     *
     * @param other a {@link com.github.pires.obd.commands.protocol.EchoOffCommand} object.
     */
    public HeaderOnCommand(HeaderOnCommand other) {
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
        return "Header On";
    }
}
