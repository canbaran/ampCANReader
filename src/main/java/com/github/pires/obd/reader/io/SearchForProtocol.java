package com.github.pires.obd.reader.io;

import com.github.pires.obd.commands.protocol.ObdProtocolCommand;

/**
 * Created by canbaran on 11/20/17.
 */

public class SearchForProtocol extends ObdProtocolCommand {
    /**
     * <p>Constructor for CANStatus.</p>
     */
    public SearchForProtocol() {
        super("AT SP6");
    }

    /**
     * <p>Constructor for EchoOffCommand.</p>
     *
     * @param other a {@link com.github.pires.obd.commands.protocol.EchoOffCommand} object.
     */
    public SearchForProtocol(SearchForProtocol other) {
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
        return "Searching for protocol";
    }

}
