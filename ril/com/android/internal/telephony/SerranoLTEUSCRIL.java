/*
 * Copyright (c) 2014, The CyanogenMod Project. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.telephony;

import static com.android.internal.telephony.RILConstants.*;

import android.content.Context;
import android.telephony.Rlog;
import android.os.AsyncResult;
import android.os.Message;
import android.os.Parcel;
import android.telephony.ModemActivityInfo;
import android.telephony.PhoneNumberUtils;
import android.telephony.SignalStrength;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccCardStatus;
import java.util.ArrayList;
import java.util.Collections;
import com.android.internal.telephony.cdma.CdmaInformationRecords;
import com.android.internal.telephony.cdma.CdmaInformationRecords.CdmaSignalInfoRec;
import com.android.internal.telephony.cdma.SignalToneUtil;
import android.os.SystemProperties;

/**
 * RIL customization for Galaxy S4 Mini (LTE USC) device
 *
 * {@hide}
 */
public class SerranoLTEUSCRIL extends RIL {

    private static final int RIL_REQUEST_DIAL_EMERGENCY = 10016;
    private static final int RIL_UNSOL_RESPONSE_IMS_NETWORK_STATE_CHANGED = 1036;
    private static final int RIL_UNSOL_DEVICE_READY_NOTI = 11008;
    private static final int RIL_UNSOL_AM = 11010;
    private static final int RIL_UNSOL_WB_AMR_STATE = 11017;
    private static final int RIL_UNSOL_RESPONSE_HANDOVER = 11021;

    public SerranoLTEUSCRIL(Context context, int preferredNetworkType, int cdmaSubscription) {
        super(context, preferredNetworkType, cdmaSubscription, null);
    }

    public SerranoLTEUSCRIL(Context context, int preferredNetworkType, int cdmaSubscription, Integer instanceId) {
        super(context, preferredNetworkType, cdmaSubscription, instanceId);
    }

    @Override
    public void
    dial(String address, int clirMode, UUSInfo uusInfo, Message result) {
        if (PhoneNumberUtils.isEmergencyNumber(address)) {
            dialEmergencyCall(address, clirMode, result);
            return;
        }

        RILRequest rr = RILRequest.obtain(RIL_REQUEST_DIAL, result);

        rr.mParcel.writeString(address);
        rr.mParcel.writeInt(clirMode);
        rr.mParcel.writeInt(0);     // CallDetails.call_type
        rr.mParcel.writeInt(1);     // CallDetails.call_domain
        rr.mParcel.writeString(""); // CallDetails.getCsvFromExtras

        if (uusInfo == null) {
            rr.mParcel.writeInt(0); // UUS information is absent
        } else {
            rr.mParcel.writeInt(1); // UUS information is present
            rr.mParcel.writeInt(uusInfo.getType());
            rr.mParcel.writeInt(uusInfo.getDcs());
            rr.mParcel.writeByteArray(uusInfo.getUserData());
        }

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    @Override
    protected Object
    responseIccCardStatus(Parcel p) {
        IccCardApplicationStatus appStatus;

        IccCardStatus cardStatus = new IccCardStatus();
        cardStatus.setCardState(p.readInt());
        cardStatus.setUniversalPinState(p.readInt());
        cardStatus.mGsmUmtsSubscriptionAppIndex = p.readInt();
        cardStatus.mCdmaSubscriptionAppIndex = p.readInt();
        cardStatus.mImsSubscriptionAppIndex = p.readInt();

        int numApplications = p.readInt();

        // limit to maximum allowed applications
        if (numApplications > IccCardStatus.CARD_MAX_APPS) {
            numApplications = IccCardStatus.CARD_MAX_APPS;
        }
        cardStatus.mApplications = new IccCardApplicationStatus[numApplications];

        for (int i = 0 ; i < numApplications ; i++) {
            appStatus = new IccCardApplicationStatus();
            appStatus.app_type       = appStatus.AppTypeFromRILInt(p.readInt());
            appStatus.app_state      = appStatus.AppStateFromRILInt(p.readInt());
            appStatus.perso_substate = appStatus.PersoSubstateFromRILInt(p.readInt());
            appStatus.aid            = p.readString();
            appStatus.app_label      = p.readString();
            appStatus.pin1_replaced  = p.readInt();
            appStatus.pin1           = appStatus.PinStateFromRILInt(p.readInt());
            appStatus.pin2           = appStatus.PinStateFromRILInt(p.readInt());
            p.readInt(); // pin1_num_retries
            p.readInt(); // puk1_num_retries
            p.readInt(); // pin2_num_retries
            p.readInt(); // puk2_num_retries
            p.readInt(); // perso_unblock_retries

            cardStatus.mApplications[i] = appStatus;
        }
        return cardStatus;
    }

    @Override
    protected Object
    responseCallList(Parcel p) {
        int num;
        int voiceSettings;
        ArrayList<DriverCall> response;
        DriverCall dc;

        num = p.readInt();
        response = new ArrayList<DriverCall>(num);

        //if (RILJ_LOGV) {
            riljLog("responseCallList: num=" + num +
                    " mEmergencyCallbackModeRegistrant=" + mEmergencyCallbackModeRegistrant +
                    " mTestingEmergencyCall=" + mTestingEmergencyCall.get());
        //}
        for (int i = 0 ; i < num ; i++) {
            dc = new DriverCall();

            dc.state = DriverCall.stateFromCLCC(p.readInt());
            riljLogv("dc.state: " + dc.state);
            dc.index = p.readInt() & 0xff;
            riljLogv("dc.index: " + dc.index);
            dc.TOA = p.readInt();
            riljLogv("dc.TOA: " + dc.TOA);
            dc.isMpty = (0 != p.readInt());
            riljLogv("dc.isMpty: " + dc.isMpty);
            dc.isMT = (0 != p.readInt());
            riljLogv("dc.isMT: " + dc.isMT);
            dc.als = p.readInt();
            riljLogv("dc.als: " + dc.als);
            voiceSettings = p.readInt();
            riljLogv("voiceSettings: " + voiceSettings);
            dc.isVoice = (0 != voiceSettings);
            //boolean isVideo = (0 != p.readInt());    Samsung CallDetails
            //int call_type = p.readInt();             Samsung CallDetails
            //int call_domain = p.readInt();           Samsung CallDetails
            //String csv = p.readString();             Samsung CallDetails
            dc.isVoicePrivacy = (0 != p.readInt());
            riljLogv("dc.isVoicePrivacy: " + dc.isVoicePrivacy);
            dc.number = p.readString();
            riljLogv("dc.number: " + dc.number);
            int np = p.readInt();
            riljLogv("np: " + np);
            dc.numberPresentation = DriverCall.presentationFromCLIP(np);
            riljLogv("dc.numberPresentation: " + dc.numberPresentation);
            dc.name = p.readString();
            riljLogv("dc.name: " + dc.name);
            dc.namePresentation = p.readInt();
            riljLogv("dc.namePresentation: " + dc.namePresentation);
            int uusInfoPresent = p.readInt();
            riljLogv("uusInfoPresent: " + uusInfoPresent);
            if (uusInfoPresent == 1) {
                dc.uusInfo = new UUSInfo();
                dc.uusInfo.setType(p.readInt());
                //riljLogv("dc.uusInfo.Type: " + dc.uusInfo.getType());
                dc.uusInfo.setDcs(p.readInt());
                //riljLogv("dc.uusInfo.Dcs: " + dc.uusInfo.getDcs());
                byte[] userData = p.createByteArray();
                dc.uusInfo.setUserData(userData);
                //riljLogv(String.format("Incoming UUS : type=%d, dcs=%d, length=%d",
                //                dc.uusInfo.getType(), dc.uusInfo.getDcs(),
                //                dc.uusInfo.getUserData().length));
                //riljLogv("Incoming UUS : data (string)="
                //        + new String(dc.uusInfo.getUserData()));
                //riljLogv("Incoming UUS : data (hex): "
                //        + IccUtils.bytesToHexString(dc.uusInfo.getUserData()));
            } else {
                riljLogv("Incoming UUS : NOT present!");
            }

            // Make sure there's a leading + on addresses with a TOA of 145
            dc.number = PhoneNumberUtils.stringFromStringAndTOA(dc.number, dc.TOA);
            riljLogv("dc.number: " + dc.number);

            response.add(dc);

            if (dc.isVoicePrivacy) {
                mVoicePrivacyOnRegistrants.notifyRegistrants();
                riljLog("InCall VoicePrivacy is enabled");
            } else {
                mVoicePrivacyOffRegistrants.notifyRegistrants();
                riljLog("InCall VoicePrivacy is disabled");
            }
        }

        Collections.sort(response);

        if ((num == 0) && mTestingEmergencyCall.getAndSet(false)) {
            if (mEmergencyCallbackModeRegistrant != null) {
                riljLog("responseCallList: call ended, testing emergency call," +
                            " notify ECM Registrants");
                mEmergencyCallbackModeRegistrant.notifyRegistrant();
            }
        }

        return response;
    }

    @Override
    protected Object
    responseSignalStrength(Parcel p) {
        int gsmSignalStrength = p.readInt() & 0xff;
        int gsmBitErrorRate = p.readInt();
        int cdmaDbm = p.readInt();
        int cdmaEcio = p.readInt();
        int evdoDbm = p.readInt();
        int evdoEcio = p.readInt();
        int evdoSnr = p.readInt();
        int lteSignalStrength = p.readInt();
        int lteRsrp = p.readInt();
        int lteRsrq = p.readInt();
        int lteRssnr = p.readInt();
        int lteCqi = p.readInt();
        int tdScdmaRscp = p.readInt();
        // constructor sets default true, makeSignalStrengthFromRilParcel does not set it
        boolean isGsm = true;

        if ((lteSignalStrength & 0xff) == 255 || lteSignalStrength == 99) {
            lteSignalStrength = 99;
            lteRsrp = SignalStrength.INVALID;
            lteRsrq = SignalStrength.INVALID;
            lteRssnr = SignalStrength.INVALID;
            lteCqi = SignalStrength.INVALID;
        } else {
            lteSignalStrength &= 0xff;
        }

        if (RILJ_LOGD)
            riljLog("gsmSignalStrength:" + gsmSignalStrength + " gsmBitErrorRate:" + gsmBitErrorRate +
                    " cdmaDbm:" + cdmaDbm + " cdmaEcio:" + cdmaEcio + " evdoDbm:" + evdoDbm +
                    " evdoEcio: " + evdoEcio + " evdoSnr:" + evdoSnr +
                    " lteSignalStrength:" + lteSignalStrength + " lteRsrp:" + lteRsrp +
                    " lteRsrq:" + lteRsrq + " lteRssnr:" + lteRssnr + " lteCqi:" + lteCqi +
                    " tdScdmaRscp:" + tdScdmaRscp + " isGsm:" + (isGsm ? "true" : "false"));

        return new SignalStrength(gsmSignalStrength, gsmBitErrorRate, cdmaDbm, cdmaEcio, evdoDbm,
                evdoEcio, evdoSnr, lteSignalStrength, lteRsrp, lteRsrq, lteRssnr, lteCqi,
                tdScdmaRscp, isGsm);
    }

    @Override
    protected RILRequest
    processSolicited (Parcel p, int type) {
        int serial, error, request;
        RILRequest rr;
        int dataPosition = p.dataPosition(); // save off position within the Parcel

        serial = p.readInt();
        error = p.readInt();

        rr = mRequestList.get(serial);
        if (rr == null || error != 0 || p.dataAvail() <= 0) {
            p.setDataPosition(dataPosition);
            return super.processSolicited(p, type);
        }

        try { switch (rr.mRequest) {
            case RIL_REQUEST_OPERATOR:
                String operators[] = (String [])responseStrings(p);

                Rlog.v(RILJ_LOG_TAG, "SerranoLTEUSCRIL: Operator response");
       
                if (operators == null || operators.length < 0) {
                   Rlog.v(RILJ_LOG_TAG, "SerranoLTEUSCRIL: operators is empty or null");
                } else {
                   Rlog.v(RILJ_LOG_TAG, "SerranoLTEUSCRIL: length of operators:"+operators.length);
                   for (int i = 0; i < operators.length; i++) {
                      Rlog.v(RILJ_LOG_TAG, "SerranoLTEUSCRIL: operator["+i+"]:"+operators[i]);
                   }
                } 

                Rlog.v(RILJ_LOG_TAG, "SerranoLTEUSCRIL: Forcing operator name using build property ro.cdma.home.operator.alpha");
                operators[0] = SystemProperties.get("ro.cdma.home.operator.alpha");

                if (RILJ_LOGD) riljLog(rr.serialString() + "< " + requestToString(rr.mRequest)
                                + " " + retToString(rr.mRequest, operators));

                if (rr.mResult != null) {
                        AsyncResult.forMessage(rr.mResult, operators, null);
                        rr.mResult.sendToTarget();
                }
                mRequestList.remove(serial);
                break;
            case RIL_REQUEST_VOICE_REGISTRATION_STATE:
                String voiceRegStates[] = (String [])responseStrings(p);

                Rlog.v(RILJ_LOG_TAG, "SerranoLTEUSCRIL: VoiceRegistrationState response");

                if (voiceRegStates == null || voiceRegStates.length < 0) {
                   Rlog.v(RILJ_LOG_TAG, "SerranoLTEUSCRIL: voiceRegStates is empty or null");
                } else {
                   Rlog.v(RILJ_LOG_TAG, "SerranoLTEUSCRIL: length of voiceRegStates:"+voiceRegStates.length);
                   for (int i = 0; i < voiceRegStates.length; i++) {
                      Rlog.v(RILJ_LOG_TAG, "SerranoLTEUSCRIL: voiceRegStates["+i+"]:"+voiceRegStates[i]);
                   }
                }
 
                if (RILJ_LOGD) riljLog(rr.serialString() + "< " + requestToString(rr.mRequest)
                                + " " + retToString(rr.mRequest, voiceRegStates));

                if (rr.mResult != null) {
                        AsyncResult.forMessage(rr.mResult, voiceRegStates, null);
                        rr.mResult.sendToTarget();
                }
                mRequestList.remove(serial);
                break;
             case RIL_REQUEST_DATA_REGISTRATION_STATE:
                String dataRegStates[] = (String [])responseStrings(p);

                Rlog.v(RILJ_LOG_TAG, "SerranoLTEUSCRIL: DataRegistrationState response");
               
                if (dataRegStates == null || dataRegStates.length < 0) {
                   Rlog.v(RILJ_LOG_TAG, "SerranoLTEUSCRIL: dataRegStates is empty or null");
                } else {
                   Rlog.v(RILJ_LOG_TAG, "SerranoLTEUSCRIL: length of dataRegStates:"+dataRegStates.length);
                   for (int i = 0; i < dataRegStates.length; i++) {
                      Rlog.v(RILJ_LOG_TAG, "SerranoLTEUSCRIL: dataRegStates["+i+"]:"+dataRegStates[i]);
                   }
                }
  
                if (RILJ_LOGD) riljLog(rr.serialString() + "< " + requestToString(rr.mRequest)
                                + " " + retToString(rr.mRequest, dataRegStates));

                if (rr.mResult != null) {
                        AsyncResult.forMessage(rr.mResult, dataRegStates, null);
                        rr.mResult.sendToTarget();
                }
                mRequestList.remove(serial);
                break;
            default:
                p.setDataPosition(dataPosition);
                return super.processSolicited(p, type);
        }} catch (Throwable tr) {
                // Exceptions here usually mean invalid RIL responses

                Rlog.w(RILJ_LOG_TAG, rr.serialString() + "< "
                                + requestToString(rr.mRequest)
                                + " exception, possible invalid RIL response", tr);

                if (rr.mResult != null) {
                        AsyncResult.forMessage(rr.mResult, null, tr);
                        rr.mResult.sendToTarget();
                }
                return rr;
        }

        return rr;
    }

    @Override
    protected void
    processUnsolicited (Parcel p, int type) {
        Object ret;
        int dataPosition = p.dataPosition(); // save off position within the Parcel
        int response = p.readInt();

        switch(response) {
            case RIL_UNSOL_RESPONSE_IMS_NETWORK_STATE_CHANGED:
                ret = responseVoid(p);
                break;
            case RIL_UNSOL_DEVICE_READY_NOTI:
                ret = responseVoid(p);
                break;
            case RIL_UNSOL_AM:
                ret = responseString(p);
                break;
            case RIL_UNSOL_WB_AMR_STATE:
                ret = responseInts(p);
                break;
            case RIL_UNSOL_RESPONSE_HANDOVER:
                ret = responseVoid(p);
                break;
            default:
                // Rewind the Parcel
                p.setDataPosition(dataPosition);

                // Forward responses that we are not overriding to the super class
                super.processUnsolicited(p, type);
                return;
        }
    }

    // This call causes ril to crash the socket, stopping further communication
    @Override
    public void
    getHardwareConfig (Message result) {
        riljLog("Ignoring call to 'getHardwareConfig'");
        if (result != null) {
            CommandException ex = new CommandException(
                CommandException.Error.REQUEST_NOT_SUPPORTED);
            AsyncResult.forMessage(result, null, ex);
            result.sendToTarget();
        }
    }

    private void
    dialEmergencyCall(String address, int clirMode, Message result) {
        RILRequest rr;

        rr = RILRequest.obtain(RIL_REQUEST_DIAL_EMERGENCY, result);
        rr.mParcel.writeString(address);
        rr.mParcel.writeInt(clirMode);
        rr.mParcel.writeInt(0);        // CallDetails.call_type
        rr.mParcel.writeInt(3);        // CallDetails.call_domain
        rr.mParcel.writeString("");    // CallDetails.getCsvFromExtra
        rr.mParcel.writeInt(0);        // Unknown

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    // Workaround for Samsung CDMA "ring of death" bug:
    //
    // Symptom: As soon as the phone receives notice of an incoming call, an
    // audible "old fashioned ring" is emitted through the earpiece and
    // persists through the duration of the call, or until reboot if the call
    // isn't answered.
    //
    // Background: The CDMA telephony stack implements a number of "signal info
    // tones" that are locally generated by ToneGenerator and mixed into the
    // voice call path in response to radio RIL_UNSOL_CDMA_INFO_REC requests.
    // One of these tones, IS95_CONST_IR_SIG_IS54B_L, is requested by the
    // radio just prior to notice of an incoming call when the voice call
    // path is muted. CallNotifier is responsible for stopping all signalv
    // tones (by "playing" the TONE_CDMA_SIGNAL_OFF tone) upon receipt of a
    // "new ringing connection", prior to unmuting the voice call path.    
    //
    // Problem: CallNotifier's incoming call path is designed to minimize
    // latency to notify users of incoming calls ASAP. Thus,
    // SignalInfoTonePlayer requests are handled asynchronously by spawning a    
    // one-shot thread for each. Unfortunately the ToneGenerator API does
    // not provide a mechanism to specify an ordering on requests, and thus,
    // unexpected thread interleaving may result in ToneGenerator processing
    // them in the opposite order that CallNotifier intended. In this case,
    // playing the "signal off" tone first, followed by playing the "old
    // fashioned ring" indefinitely.
    //
    // Solution: An API change to ToneGenerator is required to enable    
    // SignalInfoTonePlayer to impose an ordering on requests (i.e., drop any
    // request that's older than the most recent observed). Such a change,
    // or another appropriate fix should be implemented in AOSP first.
    //
    // Workaround: Intercept RIL_UNSOL_CDMA_INFO_REC requests from the radio,
    // check for a signal info record matching IS95_CONST_IR_SIG_IS54B_L, and
    // drop it so it's never seen by CallNotifier. If other signal tones are
    // observed to cause this problem, they should be dropped here as well.

    @Override
    protected void notifyRegistrantsCdmaInfoRec(CdmaInformationRecords infoRec) {

       final int response = RIL_UNSOL_CDMA_INFO_REC;

       if (infoRec.record instanceof CdmaSignalInfoRec) {

          CdmaSignalInfoRec sir = (CdmaSignalInfoRec) infoRec.record;

          if (sir != null
          && sir.isPresent
          && sir.signalType == SignalToneUtil.IS95_CONST_IR_SIGNAL_IS54B
          && sir.alertPitch == SignalToneUtil.IS95_CONST_IR_ALERT_MED
          && sir.signal == SignalToneUtil.IS95_CONST_IR_SIG_IS54B_L) {

             Rlog.d(RILJ_LOG_TAG, "Dropping \"" + responseToString(response) + " "
             + retToString(response, sir)
             + "\" to prevent \"ring of death\" bug.");
 
             return;
          } 
       } 
       super.notifyRegistrantsCdmaInfoRec(infoRec);
   }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getCellInfoList(Message result) {
        riljLog("getCellInfoList: not supported");
        if (result != null) {
            CommandException ex = new CommandException(
                CommandException.Error.REQUEST_NOT_SUPPORTED);
            AsyncResult.forMessage(result, null, ex);
            result.sendToTarget();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCellInfoListRate(int rateInMillis, Message response) {
        riljLog("setCellInfoListRate: not supported");
        if (response != null) {
            CommandException ex = new CommandException(
                CommandException.Error.REQUEST_NOT_SUPPORTED);
            AsyncResult.forMessage(response, null, ex);
            response.sendToTarget();
        }
    }

    @Override
    public void getRadioCapability(Message response) {
       riljLog("getRadioCapability: returning static radio capability");
       if (response != null) {
           Object ret = makeStaticRadioCapability();
           AsyncResult.forMessage(response, ret, null);
           response.sendToTarget();
       }
    }

    protected Object
    responseFailCause(Parcel p) {
        int numInts;
        int response[];

        numInts = p.readInt();
        response = new int[numInts];
        for (int i = 0 ; i < numInts ; i++) {
            response[i] = p.readInt();
        }
        LastCallFailCause failCause = new LastCallFailCause();
        failCause.causeCode = response[0];
        if (p.dataAvail() > 0) {
          failCause.vendorCause = p.readString();
        }
        return failCause;
    }  


    /**
    * @hide
    */
    public void getModemActivityInfo(Message response) {
        riljLog("getModemActivityInfo: not supported");
        if (response != null) {
            CommandException ex = new CommandException(
                CommandException.Error.REQUEST_NOT_SUPPORTED);
            AsyncResult.forMessage(response, null, ex);
            response.sendToTarget();
        }
    }
}

