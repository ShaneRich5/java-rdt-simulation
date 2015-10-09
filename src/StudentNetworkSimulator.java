import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Created by shane on 10/5/15.
 */
public class StudentNetworkSimulator extends NetworkSimulator
{
    /*
     * Predefined Constants (static member variables):
     *
     *   int MAXDATASIZE : the maximum size of the Message data and
     *                     Packet payload
     *
     *   int A           : a predefined integer that represents entity A
     *   int B           : a predefined integer that represents entity B
     *
     *
     * Predefined Member Methods:
     *
     *  void stopTimer(int entity):
     *       Stops the timer running at "entity" [A or B]
     *  void startTimer(int entity, double increment):
     *       Starts a timer running at "entity" [A or B], which will expire in
     *       "increment" time units, causing the interrupt handler to be
     *       called.  You should only call this with A.
     *  void toLayer3(int callingEntity, Packet p)
     *       Puts the packet "p" into the network from "callingEntity" [A or B]
     *  void toLayer5(int entity, String dataSent)
     *       Passes "dataSent" up to layer 5 from "entity" [A or B]
     *  double getTime()
     *       Returns the current time in the simulator.  Might be useful for
     *       debugging.
     *  void printEventList()
     *       Prints the current event list to stdout.  Might be useful for
     *       debugging, but probably not.
     *
     *
     *  Predefined Classes:
     *
     *  Message: Used to encapsulate a message coming from layer 5
     *    Constructor:
     *      Message(String inputData):
     *          creates a new Message containing "inputData"
     *    Methods:
     *      boolean setData(String inputData):
     *          sets an existing Message's data to "inputData"
     *          returns true on success, false otherwise
     *      String getData():
     *          returns the data contained in the message
     *  Packet: Used to encapsulate a packet
     *    Constructors:
     *      Packet (Packet p):
     *          creates a new Packet that is a copy of "p"
     *      Packet (int seq, int ack, int check, String newPayload)
     *          creates a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and a
     *          payload of "newPayload"
     *      Packet (int seq, int ack, int check)
     *          chreate a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and
     *          an empty payload
     *    Methods:
     *      boolean setSeqnum(int n)
     *          sets the Packet's sequence field to "n"
     *          returns true on success, false otherwise
     *      boolean setAcknum(int n)
     *          sets the Packet's ack field to "n"
     *          returns true on success, false otherwise
     *      boolean setChecksum(int n)
     *          sets the Packet's checksum to "n"
     *          returns true on success, false otherwise
     *      boolean setPayload(String newPayload)
     *          sets the Packet's payload to "newPayload"
     *          returns true on success, false otherwise
     *      int getSeqnum()
     *          returns the contents of the Packet's sequence field
     *      int getAcknum()
     *          returns the contents of the Packet's ack field
     *      int getChecksum()
     *          returns the checksum of the Packet
     *      int getPayload()
     *          returns the Packet's payload
     *
     */

    SenderState senderState;
    ReceiverState receiverState;

    // Add any necessary class variables here.  Remember, you cannot use
    // these variables to send messages error free!  They can only hold
    // state information for A or B.
    // Also add any necessary methods (e.g. checksum of a String)

    // This is the constructor.  Don't touch!
    public StudentNetworkSimulator(int numMessages,
                                   double loss,
                                   double corrupt,
                                   double avgDelay,
                                   int trace,
                                   long seed)
    {
        super(numMessages, loss, corrupt, avgDelay, trace, seed);
    }

    // This routine will be called whenever the upper layer at the sender [A]
    // has a message to send.  It is the job of your protocol to ensure that
    // the data in such a message is delivered in-order, and correctly, to
    // the receiving upper layer.
    protected void aOutput(Message message)
    {
        System.out.println("A -> B (aOutput)");
        Packet newPacket = senderState.createPacket(message.getData()); // creates the next packet
        System.out.println("Sending packet: " + newPacket);

        toLayer3(0, newPacket); // pass it to lower level for transmission
        startTimer(0, 5); // start timer
    }

    // This routine will be called whenever a packet sent from the B-side
    // (i.e. as a result of a toLayer3() being done by a B-side procedure)
    // arrives at the A-side.  "packet" is the (possibly corrupted) packet
    // sent from the B-side.
    protected void aInput(Packet packet) {
        System.out.println("B -> A (aInput)");
        System.out.println("Received ACK (aInput)");

        if (senderState.checkIntegrity(packet) || senderState.getUnackPacket().getSeqnum() != packet.getAcknum()) {
            System.out.println("Corrupt");
            startTimer(0, 20);
            toLayer3(0, senderState.getUnackPacket());
        } else {
            System.out.println("Not corrupt <-->" + packet);
//            toLayer5(0, packet.getPayload());
        }

//        startTimer(0, 5);
    }

    // This routine will be called when A's timer expires (thus generating a
    // timer interrupt). You'll probably want to use this routine to control
    // the retransmission of packets. See startTimer() and stopTimer(), above,
    // for how the timer is started and stopped.
    protected void aTimerInterrupt()
    {
        Packet p = senderState.getUnackPacket();
        System.out.println("\nTimer(aTimerInterrupt)\n" + p + "\nRetransmitting...\n");

        toLayer3(0, p);
    }

    // This routine will be called once, before any of your other A-side
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity A).
    protected void aInit()
    {
        senderState = new SenderState();
    }

    // This routine will be called whenever a packet sent from the B-side
    // (i.e. as a result of a toLayer3() being done by an A-side procedure)
    // arrives at the B-side.  "packet" is the (possibly corrupted) packet
    // sent from the A-side.
    protected void bInput(Packet packet) {
        System.out.println("A -> B (bInput)");
        System.out.println("Received packet(bInput) " + packet);

        // receives packet from A
        // measures
        toLayer3(1, receiverState
                .createPacket(packet.getSeqnum(), (receiverState
                        .checkIntegrity(packet)) ? "ACK" : "NACK"));
    }

    // This routine will be called once, before any of your other B-side
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity B).
    protected void bInit()
    {
        receiverState = new ReceiverState();
    }

    public static void main(String []args) {

        StudentNetworkSimulator S = new StudentNetworkSimulator(10, 0.1, 0.1, 1000, 2, 123);
        S.runSimulator();

    }


    private class ReceiverState extends State {

        public ReceiverState() {
            seqnum = 0;
        }

        public int getExpectedSeqNum() {
            return seqnum;
        }

        public void setExpectedSeqNum(int expectedSeqNum) {
            this.seqnum = expectedSeqNum;
        }

        @Override
        public Packet createPacket(String payload) {
            return new Packet(0, seqnum, calculateChecksum(), payload);
        }

        public Packet createPacket(int seqnum, String payload){
            return new Packet(0, seqnum, calculateChecksum(), payload);
        }
    }

    private class SenderState extends State {

        public SenderState() {
            seqnum = 1; // to produce seq 1 on he first packet
        }

        private Packet unackPacket;

        public int updateSeqNum(){
            return seqnum += 1;
        }

        public Packet getUnackPacket(){
            return unackPacket;
        }

        @Override
        public Packet createPacket(String payload) {
            Packet newPacket = new Packet(flipSeqBit(), 0, calculateChecksum(payload), payload);
            unackPacket = newPacket;
            return newPacket;
        }


    }





    private abstract class State {

        protected int checksum = 0;
        protected int seqnum = 0;
        protected int acknum = 0;

        public abstract Packet createPacket(String payload);

        public int calculatePayloadSize(String payload) {
            int payloadSize = 0;

            for (int i = 0; i< payload.length(); i++)
                payloadSize++;

            return payloadSize;
        }

        public boolean checkIntegrity(Packet packet){
            int encodedPayload = encodePayload(packet.getPayload());
            return packet.getChecksum() == encodedPayload + packet.getAcknum() + packet.getSeqnum();
        }

        public int calculateChecksum(String payload) {
            return checksum += seqnum + encodePayload(payload);
        }

        public int calculateChecksum(){
            return checksum += seqnum;
        }

        private int encodePayload(String payload){
            int total = 0;

            for (int i = 0; i < payload.length(); i++)
                total += payload.charAt(i);

            return total;
        }

        protected int flipSeqBit(){
            return seqnum = 1 - seqnum;
        }
    }
}
