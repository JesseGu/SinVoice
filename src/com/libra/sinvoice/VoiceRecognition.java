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

import com.libra.sinvoice.Buffer.BufferData;

public class VoiceRecognition {
    private final static String TAG = "Recognition";
    private final static int STATE_START = 1;
    private final static int STATE_STOP = 2;
    private final static int STATE_STEP1 = 1;
    private final static int STATE_STEP2 = 2;
    private final static int INDEX[] = { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 6, -1, -1, -1, -1, 5, -1, -1, -1, 4, -1, -1, 3, -1, -1, 2, -1, -1, 1, -1, -1, 0 };
    private final static int MAX_CIRCLE = 31;
    private final static int MIN_CIRCLE = 10;

    private int mState;
    private Listener mListener;
    private Callback mCallback;

    private int mCirclePointCount = 0;

    private int mSampleRate;
    private int mChannel;
    private int mBits;

    private boolean mIsStartCounting = false;
    private int mStep;
    private boolean mIsBeginning = false;
    private boolean mStartingDet = false;
    private int mStartingDetCount;

    private int mRegValue;
    private int mRegIndex;
    private int mRegCount;
    private int mPreRegCircle;
    private boolean mIsRegStart = false;

    public static interface Listener {
        void onStartRecognition();

        void onRecognition(int index);

        void onStopRecognition();
    }

    public static interface Callback {
        BufferData getRecognitionBuffer();

        void freeRecognitionBuffer(BufferData buffer);
    }

    public VoiceRecognition(Callback callback, int SampleRate, int channel, int bits) {
        mState = STATE_STOP;

        mCallback = callback;
        mSampleRate = SampleRate;
        mChannel = channel;
        mBits = bits;
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void start() {
        if (STATE_STOP == mState) {
            mState = STATE_START;

            if (null != mCallback) {
                mCirclePointCount = 0;

                mIsStartCounting = false;
                mStep = STATE_STEP1;
                mIsBeginning = false;
                mStartingDet = false;
                mStartingDetCount = 0;
                mPreRegCircle = -1;
                if (null != mListener) {
                    mListener.onStartRecognition();
                }
                while (STATE_START == mState) {
                    BufferData data = mCallback.getRecognitionBuffer();
                    if (null != data) {
                        if (null != data.mData) {
                            process(data);

                            mCallback.freeRecognitionBuffer(data);
                        } else {
                            LogHelper.d(TAG, "end input buffer, so stop");
                            break;
                        }
                    } else {
                        LogHelper.d(TAG, "get null recognition buffer");
                        break;
                    }
                }

                if (null != mListener) {
                    mListener.onStopRecognition();
                }
            }
        }
    }

    public void stop() {
        if (STATE_START == mState) {
            mState = STATE_STOP;
        }
    }

    private void process(BufferData data) {
        int size = data.getFilledSize() - 1;
        short sh = 0;
        for (int i = 0; i < size; i++) {
            short sh1 = data.mData[i];
            sh1 &= 0xff;
            short sh2 = data.mData[++i];
            sh2 <<= 8;
            sh = (short) ((sh1) | (sh2));

            if (!mIsStartCounting) {
                if (STATE_STEP1 == mStep) {
                    if (sh < 0) {
                        mStep = STATE_STEP2;
                    }
                } else if (STATE_STEP2 == mStep) {
                    if (sh > 0) {
                        mIsStartCounting = true;
                        mCirclePointCount = 0;
                        mStep = STATE_STEP1;
                    }
                }
            } else {
                ++mCirclePointCount;
                if (STATE_STEP1 == mStep) {
                    if (sh < 0) {
                        mStep = STATE_STEP2;
                    }
                } else if (STATE_STEP2 == mStep) {
                    if (sh > 0) {
                        // preprocess the circle
                        int circleCount = preReg(mCirclePointCount);

                        // recognise voice
                        reg(circleCount);

                        mCirclePointCount = 0;
                        mStep = STATE_STEP1;
                    }
                }
            }
        }
    }

    private int preReg(int circleCount) {
        switch (circleCount) {
        case 8:
        case 9:
        case 10:
        case 11:
        case 12:
            circleCount = 10;
            break;

        case 13:
        case 14:
        case 15:
        case 16:
        case 17:
            circleCount = 15;
            break;

        case 18:
        case 19:
        case 20:
            circleCount = 19;
            break;

        case 21:
        case 22:
        case 23:
            circleCount = 22;
            break;

        case 24:
        case 25:
        case 26:
            circleCount = 25;
            break;

        case 27:
        case 28:
        case 29:
            circleCount = 28;
            break;

        case 30:
        case 31:
        case 32:
            circleCount = 31;
            break;

        default:
            circleCount = 0;
            break;
        }

        return circleCount;
    }

    private void reg(int circleCount) {
        if (!mIsBeginning) {
            if (!mStartingDet) {
                if (MAX_CIRCLE == circleCount) {
                    mStartingDet = true;
                    mStartingDetCount = 0;
                }
            } else {
                if (MAX_CIRCLE == circleCount) {
                    ++mStartingDetCount;

                    if (mStartingDetCount >= MIN_CIRCLE) {
                        mIsBeginning = true;
                        mIsRegStart = false;
                        mRegCount = 0;
                    }
                } else {
                    mStartingDet = false;
                }
            }
        } else {
            if (!mIsRegStart) {
                if (circleCount > 0) {
                    mRegValue = circleCount;
                    mRegIndex = INDEX[circleCount];
                    mIsRegStart = true;
                    mRegCount = 1;
                }
            } else {
                if (circleCount == mRegValue) {
                    ++mRegCount;

                    if (mRegCount >= MIN_CIRCLE) {
                        // ok
                        if (mRegValue != mPreRegCircle) {
                            if (null != mListener) {
                                mListener.onRecognition(mRegIndex);
                            }
                            mPreRegCircle = mRegValue;
                        }

                        mIsRegStart = false;
                    }
                } else {
                    mIsRegStart = false;
                }
            }
        }
    }
}
