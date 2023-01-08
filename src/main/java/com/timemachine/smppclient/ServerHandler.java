package com.timemachine.smppclient;

import java.lang.ref.WeakReference;

import com.cloudhopper.smpp.SmppServerHandler;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.BaseBind;
import com.cloudhopper.smpp.pdu.BaseBindResp;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.type.SmppProcessingException;

/**
 * @author William Ferreira (@link http://www.github.com/williammferreira)
 */
public class ServerHandler implements SmppServerHandler {

    @Override
    public void sessionBindRequested(Long sessionId, SmppSessionConfiguration sessionConfiguration,
            final BaseBind bindRequest) throws SmppProcessingException {
        // test name change of sessions
        // this name actually shows up as thread context....
        sessionConfiguration.setName("Application.SMPP." + sessionConfiguration.getSystemId());

        // throw new SmppProcessingException(SmppConstants.STATUS_BINDFAIL, null);
    }

    @Override
    public void sessionCreated(Long sessionId, SmppServerSession session, BaseBindResp preparedBindResponse)
            throws SmppProcessingException {
        System.out.println("Session created: " + session);
        // need to do something it now (flag we're ready)

        session.serverReady(new TestSmppSessionHandler(session));
    }

    @Override
    public void sessionDestroyed(Long sessionId, SmppServerSession session) {
        System.out.print("Session destroyed: " + session.toString());
        // print out final stats
        if (session.hasCounters()) {
            System.out.println(". Final session rx-submitSM: " + session.getCounters().getRxSubmitSM());
        }

        // make sure it's really shutdown
        session.destroy();
    }

    public static class TestSmppSessionHandler extends DefaultSmppSessionHandler {

        private WeakReference<SmppSession> sessionRef;

        public TestSmppSessionHandler(SmppSession session) {
            this.sessionRef = new WeakReference<SmppSession>(session);
        }

        @Override
        public PduResponse firePduRequestReceived(PduRequest pduRequest) {
            SmppSession session = sessionRef.get();

            // mimic how long processing could take on a slower smsc
            try {
                // Thread.sleep(50);
            } catch (Exception e) {
            }

            return pduRequest.createResponse();
        }
    }
}