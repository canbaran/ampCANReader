package com.github.pires.obd.reader.io;

import com.github.pires.obd.commands.protocol.ObdProtocolCommand;

/**
 * Created by canbaran on 11/17/17.
 */

public class MonitorModeOnCommand extends ObdProtocolCommand {

    /**
     * <p>Constructor for CANStatus.</p>
     */
    public MonitorModeOnCommand() {
        super("AT BC001");
    }

    /**
     * <p>Constructor for EchoOffCommand.</p>
     *
     * @param other a {@link com.github.pires.obd.commands.protocol.EchoOffCommand} object.
     */
    public MonitorModeOnCommand(MonitorModeOnCommand other) {
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
        return "Monitor Mode  On";
    }
}
