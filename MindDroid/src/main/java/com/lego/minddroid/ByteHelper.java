package com.lego.minddroid;

import android.speech.tts.TextToSpeech;

import java.util.Locale;

class ByteHelper {

    static int byteToInt(byte byteValue) {
        int intValue = (byteValue & (byte) 0x7f);

        if ((byteValue & (byte) 0x80) != 0)
            intValue |= 0x80;

        return intValue;
    }

    static String handleResult(TextToSpeech mTts, byte[] textMessage) {
        // evaluate control byte
        byte controlByte = textMessage[2];
        // BIT7: Language
        if ((controlByte & 0x80) == 0x00)
            mTts.setLanguage(Locale.US);
        else
            mTts.setLanguage(Locale.getDefault());
        // BIT6: Pitch
        if ((controlByte & 0x40) == 0x00)
            mTts.setPitch(1.0f);
        else
            mTts.setPitch(0.75f);
        // BIT0-3: Speech Rate
        switch (controlByte & 0x0f) {
            case 0x01:
                mTts.setSpeechRate(1.5f);
                break;
            case 0x02:
                mTts.setSpeechRate(0.75f);
                break;
            default:
                mTts.setSpeechRate(1.0f);
                break;
        }

        String ttsText = new String(textMessage, 3, 19);
        ttsText = ttsText.replaceAll("\0", "");
        return ttsText;
    }

}
