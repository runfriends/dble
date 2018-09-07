package com.actiontech.dble.cache.impl;

import com.actiontech.dble.cache.CachePool;
import com.actiontech.dble.cache.CachePoolFactory;
import org.rocksdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RocksDBCachePoolFactory extends CachePoolFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(RocksDBCachePoolFactory.class);

    @Override
    public CachePool createCachePool(String poolName, int cacheSize, int expireSeconds) {
        final Options options = new Options();
        options.setAllowMmapReads(true).
                setAllowMmapWrites(true).
                setCreateIfMissing(true).
                setCreateMissingColumnFamilies(true).
                setCreateIfMissing(true).
                setCreateMissingColumnFamilies(true).
                setWalSizeLimitMB(cacheSize).
                setWalTtlSeconds(expireSeconds);
        CompactionOptionsFIFO fifo = new CompactionOptionsFIFO();
        fifo.setMaxTableFilesSize(cacheSize);
        options.setCompactionOptionsFIFO(fifo);
        String path = "rocksdb/" + poolName;
        try {
            final RocksDB db = TtlDB.open(options, path, expireSeconds, false);
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    FlushOptions fo = new FlushOptions();
                    fo.setWaitForFlush(true);
                    try {
                        db.flush(fo);
                    } catch (RocksDBException e) {
                        LOGGER.warn("RocksDB flush error", e);
                    } finally {
                        db.close();
                        fo.close();
                        options.close();
                    }
                }
            });
            return new RocksDBPool(db, poolName, cacheSize);
        } catch (RocksDBException e) {
            throw new InitStoreException(e);
        }
    }

    public static class InitStoreException extends RuntimeException {
        public InitStoreException(Throwable cause) {
            super(cause);
        }
    }
}
