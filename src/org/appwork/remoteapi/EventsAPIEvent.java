/**
 * Copyright (c) 2009 - 2011 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.remoteapi
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.remoteapi;

import org.appwork.storage.InvalidTypeException;

/**
 * @author daniel
 * 
 */
public class EventsAPIEvent {

    protected String processID = null;
    protected long   messageID = -1;
    protected Object data      = null;

    public EventsAPIEvent() {
    }

    public EventsAPIEvent(final Object data) {
        this.data = data;
    }

    @Override
    public EventsAPIEvent clone() {
        final EventsAPIEvent ret = new EventsAPIEvent();
        ret.data = this.data;
        ret.messageID = this.messageID;
        ret.processID = this.processID;
        return ret;
    }

    /**
     * @return the data
     */
    public Object getData() {
        return this.data;
    }

    /**
     * @return the messageID
     */
    protected long getMessageID() {
        return this.messageID;
    }

    /**
     * @return the processID
     */
    public String getProcessID() {
        return this.processID;
    }

    /**
     * @param data
     *            the data to set
     * @throws InvalidTypeException
     */
    public void setData(final Object data) {
        this.data = data;
    }

    /**
     * @param messageID
     *            the messageID to set
     */
    protected void setMessageID(final long messageID) {
        this.messageID = messageID;
    }

    /**
     * @param processID
     *            the processID to set
     */
    public void setProcessID(final String processID) {
        this.processID = processID;
    }

}
