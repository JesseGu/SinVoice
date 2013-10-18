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

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.libra.sinvoice.Buffer.BufferData;

public class Record {
    private final static String TAG = "Record";
    private final static int STATE_START = 1;
    private final static int STATE_STOP = 2;

    public final static int BITS_8 = 1;
    public final static int BITS_16 = 2;

    public final static int CHANNEL_1 = 1;
    public final static int CHANNEL_2 = 2;

    private int mState;

    private int mFrequence = 8000;
    private int mChannel = CHANNEL_1;
    private int mBits = BITS_8;
    private int mBufferSize;

    private int mChannelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int mAudioEncoding = AudioFormat.ENCODING_PCM_16BIT;

    private Listener mListener;
    private Callback mCallback;

    public static interface Listener {
        void onStartRecord();

        void onStopRecord();
    }

    public static interface Callback {
        BufferData getRecordBuffer();

        void freeRecordBuffer(BufferData buffer);
    }

    public Record(Callback callback, int frequence, int channel, int bits, int bufferSize) {
        mState = STATE_STOP;

        mCallback = callback;
        mFrequence = frequence;
        mChannel = channel;
        mBits = bits;
        mBufferSize = bufferSize;
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void start() {
        if (STATE_STOP == mState) {
            switch (mChannel) {
            case CHANNEL_1:
                mChannelConfig = AudioFormat.CHANNEL_IN_MONO;
                break;
            case CHANNEL_2:
                mChannelConfig = AudioFormat.CHANNEL_IN_STEREO;
                break;
            }

            switch (mBits) {
            case BITS_8:
                mAudioEncoding = AudioFormat.ENCODING_PCM_8BIT;
                break;

            case BITS_16:
                mAudioEncoding = AudioFormat.ENCODING_PCM_16BIT;
                break;
            }

            int minBufferSize = AudioRecord.getMinBufferSize(mFrequence, mChannelConfig, mAudioEncoding);
            LogHelper.d(TAG, "minBufferSize:" + minBufferSize);
            if ( mBufferSize >= minBufferSize ) {
                AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC, mFrequence, mChannelConfig, mAudioEncoding, mBufferSize);
                if (null != record) {
                    try {
                        mState = STATE_START;
                        record.startRecording();
                        LogHelper.d(TAG, "record start");

                        if (null != mCallback) {
                            if (null != mListener) {
                                mListener.onStartRecord();
                            }

                            while (STATE_START == mState) {
                                BufferData data = mCallback.getRecordBuffer();
                                if (null != data) {
                                    if (null != data.mData) {
                                        int bufferReadResult = record.read(data.mData, 0, mBufferSize);
                                        data.setFilledSize(bufferReadResult);

                                        mCallback.freeRecordBuffer(data);
                                    } else {
                                        // end of input
                                        LogHelper.d(TAG, "get end input data, so stop");
                                        break;
                                    }
                                } else {
                                    LogHelper.e(TAG, "get null data");
                                    break;
                                }
                            }

                            if (null != mListener) {
                                mListener.onStopRecord();
                            }
                        }

                        record.stop();
                        record.release();

                        LogHelper.d(TAG, "record stop");
                    } catch ( IllegalStateException e) {
                        e.printStackTrace();
                        LogHelper.e(TAG, "start record error");
                    }
                    mState = STATE_STOP;
                }
            } else {
                LogHelper.e(TAG, "bufferSize is too small");
            }
        }
    }

    public int getState() {
        return mState;
    }

    public void stop() {
        if (STATE_START == mState) {
            mState = STATE_STOP;
        }
    }
}
