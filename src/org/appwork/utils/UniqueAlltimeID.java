package org.appwork.utils;

import java.util.concurrent.atomic.AtomicLong;

public class UniqueAlltimeID {
    private static final AtomicLong ID = new AtomicLong(System.currentTimeMillis());
    private long                    id;

    public UniqueAlltimeID() {
        this.id = createUniqueAlltimeID();
    }

    private static long createUniqueAlltimeID() {
        long id = -1;
        while (true) {
            final long lastID = ID.get();
            id = System.currentTimeMillis();
            if (id < lastID) {
                /* WTF?! timestamp is smaller as previous timestamp */
                id = lastID + 1;
            } else if (id == lastID) {
                /* same timestamp, increase by 1 */
                id = id + 1;
            }
            if (ID.compareAndSet(lastID, id)) {
                return id;
            }
        }
    }

    public UniqueAlltimeID(long id2) {
        this.id = id2;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (this == o) {
            return true;
        }
        if (o instanceof UniqueAlltimeID) {
            if (((UniqueAlltimeID) o).id == id) {
                return true;
            }
        }
        return false;
    }

    public long getID() {
        return id;
    }

    @Override
    public String toString() {
        return Long.toString(id);
    }

    /**
     * WARNING: by manually changing the ID you can break unique state of this Instance!
     *
     * @param ID
     */
    public void setID(long ID) {
        this.id = ID;
    }

    /**
     * WARNING: by manually refreshing the ID you can break unique state of this Instance!
     */
    public void refresh() {
        this.setID(createUniqueAlltimeID());
    }

    public static String create() {
        return Long.toString(createUniqueAlltimeID());
    }
}
