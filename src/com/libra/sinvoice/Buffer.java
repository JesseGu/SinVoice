/*
 * Copyright (C) 2013 gujicheng
 * 
 * Licensed under the GPL License Version 2.0;
 * you may not use this file except in compliance with the License.
 * 
 * If you have any question, please contact me.
 * 
 *************************************************************************
 **                   Author information                                **
 *************************************************************************
 ** Email: gujicheng197@126.com                                         **
 ** QQ   : 29600731                                                     **
 ** Weibo: http://weibo.com/gujicheng197                                **
 *************************************************************************
 */
package com.libra.sinvoice;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Buffer {
    private final static String TAG = "Buffer";

    private BlockingQueue<BufferData> mProducerQueue;
    private BlockingQueue<BufferData> mConsumeQueue;
    private int mBufferCount;
    private int mBufferSize;

    // when mData is null, means it is end of input
    public static class BufferData {
        public byte mData[];
        private int mFilledSize;
        private int mMaxBufferSize;
        private static BufferData sEmptyBuffer = new BufferData(0);

        public BufferData(int maxBufferSize) {
            mMaxBufferSize = maxBufferSize;
            reset();

            if (maxBufferSize > 0) {
                mMaxBufferSize = maxBufferSize;
                mData = new byte[mMaxBufferSize];
            } else {
                mData = null;
            }
        }

        public static BufferData getEmptyBuffer() {
            return sEmptyBuffer;
        }

        final public void reset() {
            mFilledSize = 0;
        }

        final public int getMaxBufferSize() {
            return mMaxBufferSize;
        }

        final public void setFilledSize(int size) {
            mFilledSize = size;
        }

        final public int getFilledSize() {
            return mFilledSize;
        }
    }

    public Buffer() {
        this(Common.DEFAULT_BUFFER_COUNT, Common.DEFAULT_BUFFER_SIZE);
    }

    public Buffer(int bufferCount, int bufferSize) {
        mBufferSize = bufferSize;
        mBufferCount = bufferCount;
        mProducerQueue = new LinkedBlockingQueue<BufferData>(mBufferCount);
        // we want to put the end buffer, so need to add 1
        mConsumeQueue = new LinkedBlockingQueue<BufferData>(mBufferCount + 1);

        try {
            for (int i = 0; i < mBufferCount; ++i) {
                mProducerQueue.put(new BufferData(mBufferSize));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            LogHelper.e(TAG, "put buffer data error");
        }
    }

    public void reset() {
        int size = mProducerQueue.size();
        for (int i = 0; i < size; ++i) {
            BufferData data = mProducerQueue.peek();
            if (null == data || null == data.mData) {
                mProducerQueue.poll();
            }
        }

        size = mConsumeQueue.size();
        for (int i = 0; i < size; ++i) {
            BufferData data = mConsumeQueue.poll();
            if (null != data && null != data.mData) {
                mProducerQueue.add(data);
            }
        }

        LogHelper.d(TAG, "reset ProducerQueue Size:" + mProducerQueue.size() + "    ConsumeQueue Size:" + mConsumeQueue.size());
    }

    final public int getEmptyCount() {
        return mProducerQueue.size();
    }

    final public int getFullCount() {
        return mConsumeQueue.size();
    }

    public BufferData getEmpty() {
        return getImpl(mProducerQueue);
    }

    public boolean putEmpty(BufferData data) {
        return putImpl(data, mProducerQueue);
    }

    public BufferData getFull() {
        return getImpl(mConsumeQueue);
    }

    public boolean putFull(BufferData data) {
        return putImpl(data, mConsumeQueue);
    }

    private BufferData getImpl(BlockingQueue<BufferData> queue) {
        if (null != queue) {
            try {
                return queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private boolean putImpl(BufferData data, BlockingQueue<BufferData> queue) {
        if (null != queue && null != data) {
            try {
                queue.put(data);
                return true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

}
