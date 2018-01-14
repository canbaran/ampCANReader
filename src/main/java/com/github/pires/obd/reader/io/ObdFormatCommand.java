package com.github.pires.obd.reader.io;

import com.github.pires.obd.commands.protocol.ObdProtocolCommand;

/**
 * Created by canbaran on 1/14/18.
 */

public class ObdFormatCommand extends ObdProtocolCommand {
    /**
     * <p>Constructor for ObdResetCommand.</p>
     */
    public ObdFormatCommand() {
        super("AT CAF0");
    }

    public ObdFormatCommand(ObdFormatCommand other) {
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
        return "Format Off";
    }
}
