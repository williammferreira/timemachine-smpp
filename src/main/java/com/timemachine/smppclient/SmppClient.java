package com.timemachine.smppclient;

import java.util.concurrent.Executors;

import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.tlv.Tlv;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppBindException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppSession;

/**
 * @author William Ferreira (@link http://www.github.com/williammferreira)
 */
public class SmppClient {
    private static final String HOST = "localhost";
    private static final int PORT = 2775;
    private static final String PASSWD = "password";

    private SmppSession session;

    public SmppSessionConfiguration sessionConfiguration() {
        SmppSessionConfiguration sessionConfig = new SmppSessionConfiguration();
        sessionConfig.setName("smpp.session");
        sessionConfig.setInterfaceVersion(SmppConstants.VERSION_5_0);
        sessionConfig.setType(SmppBindType.TRANSCEIVER);
        sessionConfig.setHost(HOST);
        sessionConfig.setPort(PORT);
        sessionConfig.setSystemId("test");
        sessionConfig.setPassword(PASSWD);
        sessionConfig.setSystemType(null);
        sessionConfig.getLoggingOptions().setLogBytes(false);
        sessionConfig.getLoggingOptions().setLogPdu(true);

        return sessionConfig;
    }

    /**
     * Set up the smpp session.
     */
    public SmppSession session() throws SmppBindException, SmppTimeoutException, SmppChannelException,
            UnrecoverablePduException, InterruptedException {
        SmppSessionConfiguration config = sessionConfiguration();

        System.out.println("Connecting to server...");
        this.session = clientBootstrap().bind(config, new DefaultSmppSessionHandler());
        System.out.println("Connected to server.");

        return this.session;
    }

    public DefaultSmppClient clientBootstrap() {
        return new DefaultSmppClient(Executors.newCachedThreadPool(), 2);
    }

    /**
     * Send a text message.
     * 
     * @param msg         - The message to send.
     * @param source      - The source address.
     * @param destination - The destination address.
     */
    public void send(String msg, String source, String destination) {
        if (session.isBound()) {
            try {
                // Request Delivery
                boolean requestDlr = true;

                SubmitSm submit = new SubmitSm();

                byte[] textBytes;
                textBytes = CharsetUtil.encode(msg, CharsetUtil.CHARSET_UTF_8);

                submit.setDataCoding(SmppConstants.DATA_CODING_LATIN1);

                if (requestDlr) {
                    submit.setRegisteredDelivery(SmppConstants.REGISTERED_DELIVERY_SMSC_RECEIPT_REQUESTED);
                }

                if (textBytes != null && textBytes.length > 255) {
                    submit.addOptionalParameter(
                            new Tlv(SmppConstants.TAG_MESSAGE_PAYLOAD, textBytes, "message_payload"));
                } else {
                    submit.setShortMessage(textBytes);
                }

                submit.setSourceAddress(new Address((byte) 0x05, (byte) 0x01, source));
                submit.setDestAddress(new Address((byte) 0x01, (byte) 0x01, destination));

                // submit message to SMSC. Timeout: 10 seconds
                SubmitSmResp submitResponse = this.session.submit(submit, 10000);

                if (submitResponse.getCommandStatus() == SmppConstants.STATUS_OK) {
                    System.out.println("SMS sent");
                } else {
                    throw new IllegalStateException(submitResponse.getResultMessage());
                }

            } catch (SmppTimeoutException | SmppChannelException | UnrecoverablePduException
                    | InterruptedException | RecoverablePduException e) {
                System.out.println("Failed to send message: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        SmppClient client = new SmppClient();

        try {
            client.session();
            client.send("Hello World!", "17037857393", "15716090399");
        } catch (SmppTimeoutException | SmppChannelException | UnrecoverablePduException
                | InterruptedException e) {
            System.out.println("Failed to connect to server: " + e.getMessage());
        }
    }
}
